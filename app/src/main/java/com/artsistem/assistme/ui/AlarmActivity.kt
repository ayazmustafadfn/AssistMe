package com.artsistem.assistme.ui

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.artsistem.assistme.reminder.AlarmPlayer
import com.artsistem.assistme.reminder.NotificationActionReceiver
import com.artsistem.assistme.reminder.Notifications
import com.artsistem.assistme.reminder.ReminderScheduler
import com.artsistem.assistme.reminder.Settings
import com.artsistem.assistme.ui.theme.AssistMeTheme

/**
 * Hatırlatma zamanı geldiğinde dolu ekran açılan alarm ekranı.
 *
 * Kilit ekranının üstünde gösterilir, ekranı uyandırır ve [AlarmPlayer] ile
 * alarm sesini döngülü çalar. Butonlar: Tamamlandı / 15 dk / 1 saat /
 * (son özel süre) / Diğer… / Kapat.
 */
class AlarmActivity : ComponentActivity() {

    private val autoDismiss = Handler(Looper.getMainLooper())
    private var reminderId: Long = -1L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showOverLockScreen()

        reminderId = intent.getLongExtra(ReminderScheduler.EXTRA_REMINDER_ID, -1L)
        val title = intent.getStringExtra(EXTRA_TITLE) ?: getString(
            com.artsistem.assistme.R.string.app_name
        )
        val note = intent.getStringExtra(EXTRA_NOTE).orEmpty()
        val lastCustom = Settings.getLastCustomSnoozeMinutes(this)

        AlarmPlayer.start(this)
        autoDismiss.postDelayed({ finishAlarm() }, AUTO_DISMISS_MS)

        setContent {
            AssistMeTheme {
                AlarmScreen(
                    title = title,
                    note = note,
                    lastCustomMinutes = lastCustom,
                    onSnooze = { minutes -> snooze(minutes) },
                    onDone = { done() },
                    onDismiss = { dismiss() }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    private fun showOverLockScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val km = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
            km?.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun snooze(minutes: Int) {
        if (reminderId != -1L) {
            sendBroadcast(
                Intent(this, NotificationActionReceiver::class.java).apply {
                    action = NotificationActionReceiver.ACTION_SNOOZE
                    putExtra(ReminderScheduler.EXTRA_REMINDER_ID, reminderId)
                    putExtra(NotificationActionReceiver.EXTRA_SNOOZE_MINUTES, minutes)
                }
            )
        }
        finishAlarm()
    }

    private fun done() {
        sendAction(NotificationActionReceiver.ACTION_DONE)
        finishAlarm()
    }

    private fun dismiss() {
        sendAction(NotificationActionReceiver.ACTION_DISMISS)
        finishAlarm()
    }

    private fun sendAction(action: String) {
        if (reminderId == -1L) return
        sendBroadcast(
            Intent(this, NotificationActionReceiver::class.java).apply {
                this.action = action
                putExtra(ReminderScheduler.EXTRA_REMINDER_ID, reminderId)
            }
        )
    }

    private fun finishAlarm() {
        autoDismiss.removeCallbacksAndMessages(null)
        AlarmPlayer.stop()
        if (reminderId != -1L) Notifications.cancel(this, reminderId)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        AlarmPlayer.stop()
    }

    companion object {
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_NOTE = "extra_note"
        const val EXTRA_SNOOZE_MINUTES = "extra_snooze_minutes"
        const val EXTRA_REPEATING = "extra_repeating"

        /** Kimse dokunmazsa 2 dakika sonra otomatik sustur. */
        private const val AUTO_DISMISS_MS = 2 * 60 * 1000L
    }
}

/** Dakika sayısını "15 dk" / "1 saat" / "1 sa 30 dk" biçiminde gösterir. */
fun formatSnoozeLabel(minutes: Int): String = when {
    minutes < 60 -> "$minutes dk"
    minutes % 60 == 0 -> "${minutes / 60} saat"
    else -> "${minutes / 60} sa ${minutes % 60} dk"
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AlarmScreen(
    title: String,
    note: String,
    lastCustomMinutes: Int,
    onSnooze: (Int) -> Unit,
    onDone: () -> Unit,
    onDismiss: () -> Unit
) {
    var showPicker by remember { mutableStateOf(false) }

    val showLastCustom = lastCustomMinutes > 0 &&
        lastCustomMinutes != Settings.SNOOZE_SHORT_MINUTES &&
        lastCustomMinutes != Settings.SNOOZE_LONG_MINUTES

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.errorContainer
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Alarm,
                contentDescription = null,
                modifier = Modifier.size(96.dp),
                tint = MaterialTheme.colorScheme.onErrorContainer
            )
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 24.dp)
            )
            if (note.isNotBlank()) {
                Text(
                    text = note,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }

            // Tamamlandı (vurgulu)
            Button(
                onClick = onDone,
                modifier = Modifier.fillMaxWidth().padding(top = 40.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.onErrorContainer,
                    contentColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(20.dp))
                Text("  Tamamlandı", style = MaterialTheme.typography.titleMedium)
            }

            // Erteleme seçenekleri
            FlowRow(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalButton(onClick = { onSnooze(Settings.SNOOZE_SHORT_MINUTES) }) {
                    Text("15 dk")
                }
                FilledTonalButton(onClick = { onSnooze(Settings.SNOOZE_LONG_MINUTES) }) {
                    Text("1 saat")
                }
                if (showLastCustom) {
                    FilledTonalButton(onClick = { onSnooze(lastCustomMinutes) }) {
                        Text(formatSnoozeLabel(lastCustomMinutes))
                    }
                }
                FilledTonalButton(onClick = { showPicker = true }) {
                    Text("Diğer…")
                }
            }

            // Kapat (sade)
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.padding(top = 24.dp)
            ) {
                Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(20.dp))
                Text(
                    "  Kapat",
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }

    if (showPicker) {
        SnoozePickerDialog(
            initialMinutes = if (lastCustomMinutes > 0) lastCustomMinutes else 30,
            onConfirm = { minutes ->
                showPicker = false
                onSnooze(minutes)
            },
            onDismiss = { showPicker = false }
        )
    }
}

@Composable
private fun SnoozePickerDialog(
    initialMinutes: Int,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var isHour by remember { mutableStateOf(initialMinutes >= 60 && initialMinutes % 60 == 0) }
    var amount by remember {
        mutableIntStateOf(if (initialMinutes >= 60 && initialMinutes % 60 == 0) initialMinutes / 60 else initialMinutes)
    }

    fun minutes(): Int = if (isHour) amount * 60 else amount

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ne kadar ertelensin?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Birim seçimi
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = !isHour,
                        onClick = { if (isHour) { isHour = false; amount = 15 } },
                        label = { Text("Dakika") }
                    )
                    FilterChip(
                        selected = isHour,
                        onClick = { if (!isHour) { isHour = true; amount = 1 } },
                        label = { Text("Saat") }
                    )
                }
                // Adımlayıcı
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        amount = if (isHour) (amount - 1).coerceAtLeast(1)
                        else (amount - 5).coerceAtLeast(5)
                    }) {
                        Icon(Icons.Filled.Remove, contentDescription = "Azalt")
                    }
                    Text(
                        text = if (isHour) "$amount saat" else "$amount dk",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    IconButton(onClick = {
                        amount = if (isHour) amount + 1 else amount + 5
                    }) {
                        Icon(Icons.Filled.Add, contentDescription = "Artır")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(minutes()) }) { Text("Ertele") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("İptal") }
        }
    )
}
