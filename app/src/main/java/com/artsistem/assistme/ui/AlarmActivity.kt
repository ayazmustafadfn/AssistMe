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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.artsistem.assistme.data.RepeatType
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
 * alarm sesini döngülü çalar. Kullanıcı "Ertele" ya da "Tamam" diyene veya
 * zaman aşımına ([AUTO_DISMISS_MS]) kadar çalmaya devam eder.
 */
class AlarmActivity : ComponentActivity() {

    private val autoDismiss = Handler(Looper.getMainLooper())
    private var reminderId: Long = -1L
    private var snoozeMinutes: Int = Settings.DEFAULT_SNOOZE_MINUTES

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showOverLockScreen()

        reminderId = intent.getLongExtra(ReminderScheduler.EXTRA_REMINDER_ID, -1L)
        val title = intent.getStringExtra(EXTRA_TITLE) ?: getString(
            com.artsistem.assistme.R.string.app_name
        )
        val note = intent.getStringExtra(EXTRA_NOTE).orEmpty()
        snoozeMinutes = intent.getIntExtra(EXTRA_SNOOZE_MINUTES, Settings.DEFAULT_SNOOZE_MINUTES)
        val repeating = intent.getBooleanExtra(EXTRA_REPEATING, false)

        // Sesi başlat (zaten çalıyorsa tekrar etmez).
        AlarmPlayer.start(this)

        // Güvenlik: kullanıcı hiç dokunmazsa belli süre sonra sustur.
        autoDismiss.postDelayed({ finishAlarm() }, AUTO_DISMISS_MS)

        setContent {
            AssistMeTheme {
                AlarmScreen(
                    title = title,
                    note = note,
                    snoozeMinutes = snoozeMinutes,
                    showSnooze = true,
                    onSnooze = { onSnooze() },
                    onDone = { onDone() }
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

    private fun onSnooze() {
        sendAction(NotificationActionReceiver.ACTION_SNOOZE)
        finishAlarm()
    }

    private fun onDone() {
        sendAction(NotificationActionReceiver.ACTION_DONE)
        finishAlarm()
    }

    /** Mevcut bildirim aksiyon mantığını yeniden kullanır. */
    private fun sendAction(action: String) {
        if (reminderId == -1L) return
        val intent = Intent(this, NotificationActionReceiver::class.java).apply {
            this.action = action
            putExtra(ReminderScheduler.EXTRA_REMINDER_ID, reminderId)
            putExtra(NotificationActionReceiver.EXTRA_SNOOZE_MINUTES, snoozeMinutes)
        }
        sendBroadcast(intent)
    }

    private fun finishAlarm() {
        autoDismiss.removeCallbacksAndMessages(null)
        AlarmPlayer.stop()
        if (reminderId != -1L) Notifications.cancel(this, reminderId)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Ekran her şekilde kapanırsa ses kalmasın.
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

@Composable
private fun AlarmScreen(
    title: String,
    note: String,
    snoozeMinutes: Int,
    showSnooze: Boolean,
    onSnooze: () -> Unit,
    onDone: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.errorContainer
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
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

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 48.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onDone,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.onErrorContainer,
                        contentColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text("Tamam", style = MaterialTheme.typography.titleMedium)
                }
                if (showSnooze) {
                    OutlinedButton(
                        onClick = onSnooze,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "$snoozeMinutes dk ertele",
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }
    }
}
