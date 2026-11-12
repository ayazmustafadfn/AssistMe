package com.artsistem.assistme.ui

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
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
import androidx.compose.material.icons.filled.VolumeOff
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
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import java.util.Calendar

/**
 * Hatırlatma zamanı geldiğinde dolu ekran açılan alarm ekranı.
 *
 * Kilit ekranının üstünde gösterilir, ekranı uyandırır ve [AlarmPlayer] ile
 * alarm sesini döngülü çalar. Butonlar: Tamamlandı / 15 dk / 1 saat /
 * (son özel süre) / Diğer… / Kapat.
 */
class AlarmActivity : ComponentActivity(), SensorEventListener {

    private val autoDismiss = Handler(Looper.getMainLooper())
    private var reminderId: Long = -1L

    private val muted = mutableStateOf(false)
    private var sensorManager: SensorManager? = null
    /** Alçak geçirgen filtreyle süzülmüş yerçekimi vektörü (titreşim gürültüsünü eler). */
    private val gravity = FloatArray(3)
    private var baseline: FloatArray? = null
    private var sensorStartedAt = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showOverLockScreen()

        reminderId = intent.getLongExtra(ReminderScheduler.EXTRA_REMINDER_ID, -1L)
        val title = intent.getStringExtra(EXTRA_TITLE) ?: getString(
            com.artsistem.assistme.R.string.app_name
        )
        val note = intent.getStringExtra(EXTRA_NOTE).orEmpty()
        val lastCustom = Settings.getLastCustomSnoozeMinutes(this)
        val vibrateOnly = intent.getBooleanExtra(EXTRA_VIBRATE_ONLY, false)
        val bgArgb = Settings.getAlarmBgColor(this)

        // Normalde bildirimle birlikte zaten başlamıştır; değilse burada başlar.
        AlarmPlayer.start(this, vibrateOnly)
        autoDismiss.postDelayed({ finishAlarm() }, AUTO_DISMISS_MS)
        if (Settings.isPickupMuteEnabled(this)) startPickupDetection()

        setContent {
            AssistMeTheme {
                AlarmScreen(
                    title = title,
                    note = note,
                    lastCustomMinutes = lastCustom,
                    muted = muted.value,
                    bgArgb = bgArgb,
                    onSnooze = { minutes -> snooze(minutes) },
                    onSnoozeAt = { at -> snooze(0, at) },
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

    // --- Ele alınca sessize al ---

    private fun startPickupDetection() {
        val sm = getSystemService(Context.SENSOR_SERVICE) as? SensorManager ?: return
        val accel = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) ?: return
        sensorManager = sm
        sensorStartedAt = System.currentTimeMillis()
        sm.registerListener(this, accel, SensorManager.SENSOR_DELAY_UI)
    }

    private fun stopPickupDetection() {
        sensorManager?.unregisterListener(this)
        sensorManager = null
    }

    override fun onSensorChanged(event: SensorEvent) {
        for (i in 0..2) gravity[i] = LOW_PASS * gravity[i] + (1 - LOW_PASS) * event.values[i]
        // Filtre otursun diye ilk saniyeyi bekle, sonra duruşu referans al.
        if (System.currentTimeMillis() - sensorStartedAt < SETTLE_MS) return
        val base = baseline ?: gravity.copyOf().also { baseline = it; return }
        if (angleBetween(base, gravity) > PICKUP_ANGLE_DEG) muteAlarm()
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    private fun angleBetween(a: FloatArray, b: FloatArray): Double {
        val dot = a[0] * b[0] + a[1] * b[1] + a[2] * b[2]
        val na = Math.sqrt((a[0] * a[0] + a[1] * a[1] + a[2] * a[2]).toDouble())
        val nb = Math.sqrt((b[0] * b[0] + b[1] * b[1] + b[2] * b[2]).toDouble())
        if (na < 1e-3 || nb < 1e-3) return 0.0
        return Math.toDegrees(Math.acos((dot / (na * nb)).coerceIn(-1.0, 1.0)))
    }

    /** Sesi/titreşimi keser; ekran açık kalır, karar kullanıcıda. */
    private fun muteAlarm() {
        if (muted.value) return
        muted.value = true
        AlarmPlayer.mute()
        stopPickupDetection()
    }

    /** Ses tuşlarına basmak da alarmı sessize alır (yaygın alarm davranışı). */
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            muteAlarm()
            return true
        }
        return super.onKeyDown(keyCode, event)
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

    /** [minutes] sonra ertele; [atMillis] > 0 ise o kesin zamana ertele. */
    private fun snooze(minutes: Int, atMillis: Long = 0L) {
        if (reminderId != -1L) {
            sendBroadcast(
                Intent(this, NotificationActionReceiver::class.java).apply {
                    action = NotificationActionReceiver.ACTION_SNOOZE
                    putExtra(ReminderScheduler.EXTRA_REMINDER_ID, reminderId)
                    putExtra(NotificationActionReceiver.EXTRA_SNOOZE_MINUTES, minutes.coerceAtLeast(1))
                    if (atMillis > 0L) putExtra(NotificationActionReceiver.EXTRA_SNOOZE_AT, atMillis)
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
        stopPickupDetection()
        AlarmPlayer.stop()
        if (reminderId != -1L) Notifications.cancel(this, reminderId)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopPickupDetection()
        AlarmPlayer.stop()
    }

    companion object {
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_NOTE = "extra_note"
        const val EXTRA_SNOOZE_MINUTES = "extra_snooze_minutes"
        const val EXTRA_REPEATING = "extra_repeating"
        const val EXTRA_VIBRATE_ONLY = "extra_vibrate_only"

        /** Kimse dokunmazsa 2 dakika sonra otomatik sustur. */
        private const val AUTO_DISMISS_MS = 2 * 60 * 1000L

        /** Duruş bu açıdan fazla değişirse "ele alındı" say. */
        private const val PICKUP_ANGLE_DEG = 25.0
        private const val SETTLE_MS = 1000L
        private const val LOW_PASS = 0.8f
    }
}

/** Dakika sayısını "15 dk" / "1 saat" / "1 sa 30 dk" biçiminde gösterir. */
fun formatSnoozeLabel(minutes: Int): String = when {
    minutes < 60 -> "$minutes dk"
    minutes % 60 == 0 -> "${minutes / 60} saat"
    else -> "${minutes / 60} sa ${minutes % 60} dk"
}

/** Alarm ekranı zemin seçenekleri (Ayarlar). 0 = tema kırmızısı (varsayılan). */
val AlarmBgPalette: List<Pair<Int, String>> = listOf(
    0 to "Kırmızı",
    0xFFC8553D.toInt() to "Kiremit",
    0xFFE0A030.toInt() to "Amber",
    0xFF2E7D5B.toInt() to "Yeşil",
    0xFF1E6FA8.toInt() to "Mavi",
    0xFF5E3A8C.toInt() to "Mor",
    0xFFD86A8A.toInt() to "Pembe",
    0xFF263238.toInt() to "Gece",
    0xFFF3EEE6.toInt() to "Krem"
)

/** Zemin rengine göre okunaklı yazı/ikon rengi. */
fun alarmContentColor(bg: Color): Color =
    if (bg.luminance() > 0.5f) Color(0xFF1C1B1F) else Color.White

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AlarmScreen(
    title: String,
    note: String,
    lastCustomMinutes: Int,
    muted: Boolean,
    bgArgb: Int,
    onSnooze: (Int) -> Unit,
    onSnoozeAt: (Long) -> Unit,
    onDone: () -> Unit,
    onDismiss: () -> Unit
) {
    var showPicker by remember { mutableStateOf(false) }

    val showLastCustom = lastCustomMinutes > 0 &&
        lastCustomMinutes != Settings.SNOOZE_SHORT_MINUTES &&
        lastCustomMinutes != Settings.SNOOZE_LONG_MINUTES

    val bg = if (bgArgb == 0) MaterialTheme.colorScheme.errorContainer else Color(bgArgb)
    val fg = if (bgArgb == 0) MaterialTheme.colorScheme.onErrorContainer else alarmContentColor(bg)
    val tonal = ButtonDefaults.filledTonalButtonColors(
        containerColor = fg.copy(alpha = 0.14f),
        contentColor = fg
    )

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = bg
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
                tint = fg
            )
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                color = fg,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 24.dp)
            )
            if (note.isNotBlank()) {
                Text(
                    text = note,
                    style = MaterialTheme.typography.bodyLarge,
                    color = fg,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }
            if (muted) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 16.dp)
                ) {
                    Icon(
                        Icons.Filled.VolumeOff,
                        contentDescription = null,
                        tint = fg,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        "  Sessize alındı",
                        style = MaterialTheme.typography.labelLarge,
                        color = fg
                    )
                }
            }

            // Tamamlandı (vurgulu)
            Button(
                onClick = onDone,
                modifier = Modifier.fillMaxWidth().padding(top = 40.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = fg,
                    contentColor = bg
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
                FilledTonalButton(onClick = { onSnooze(Settings.SNOOZE_SHORT_MINUTES) }, colors = tonal) {
                    Text("15 dk")
                }
                FilledTonalButton(onClick = { onSnooze(Settings.SNOOZE_LONG_MINUTES) }, colors = tonal) {
                    Text("1 saat")
                }
                if (showLastCustom) {
                    FilledTonalButton(onClick = { onSnooze(lastCustomMinutes) }, colors = tonal) {
                        Text(formatSnoozeLabel(lastCustomMinutes))
                    }
                }
                FilledTonalButton(onClick = { showPicker = true }, colors = tonal) {
                    Text("Diğer…")
                }
            }

            // Kapat (sade)
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.padding(top = 24.dp)
            ) {
                Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(20.dp), tint = fg)
                Text("  Kapat", color = fg)
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
            onConfirmAt = { at ->
                showPicker = false
                onSnoozeAt(at)
            },
            onDismiss = { showPicker = false }
        )
    }
}

private enum class SnoozeMode { MINUTE, HOUR, DATE }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SnoozePickerDialog(
    initialMinutes: Int,
    onConfirm: (Int) -> Unit,
    onConfirmAt: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val startsInHours = initialMinutes >= 60 && initialMinutes % 60 == 0
    var mode by remember { mutableStateOf(if (startsInHours) SnoozeMode.HOUR else SnoozeMode.MINUTE) }
    var amount by remember {
        mutableIntStateOf(if (startsInHours) initialMinutes / 60 else initialMinutes)
    }
    // Tarih modu: varsayılan yarın aynı saat (5 dk'ya yuvarlı).
    var targetAt by remember {
        mutableLongStateOf(
            Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, 1)
                set(Calendar.MINUTE, (get(Calendar.MINUTE) / 5) * 5)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
        )
    }
    var showDate by remember { mutableStateOf(false) }

    fun minutes(): Int = if (mode == SnoozeMode.HOUR) amount * 60 else amount
    val isPast = mode == SnoozeMode.DATE && targetAt <= System.currentTimeMillis()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ne kadar ertelensin?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Birim seçimi
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = mode == SnoozeMode.MINUTE,
                        onClick = { if (mode != SnoozeMode.MINUTE) { mode = SnoozeMode.MINUTE; amount = 15 } },
                        label = { Text("Dakika") }
                    )
                    FilterChip(
                        selected = mode == SnoozeMode.HOUR,
                        onClick = { if (mode != SnoozeMode.HOUR) { mode = SnoozeMode.HOUR; amount = 1 } },
                        label = { Text("Saat") }
                    )
                    FilterChip(
                        selected = mode == SnoozeMode.DATE,
                        onClick = { mode = SnoozeMode.DATE },
                        label = { Text("Tarih") }
                    )
                }
                if (mode == SnoozeMode.DATE) {
                    // Belirli gün + saat (ör. yarın 09:00).
                    OutlinedButton(onClick = { showDate = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(formatDateTime(targetAt))
                    }
                    val cal = Calendar.getInstance().apply { timeInMillis = targetAt }
                    TimeWheel(
                        hour = cal.get(Calendar.HOUR_OF_DAY),
                        minute = cal.get(Calendar.MINUTE),
                        onChange = { h, m -> targetAt = withTime(targetAt, h, m) }
                    )
                    if (isPast) {
                        Text(
                            "Geçmiş bir zaman seçilemez",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                } else {
                    // Adımlayıcı
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = {
                            amount = if (mode == SnoozeMode.HOUR) (amount - 1).coerceAtLeast(1)
                            else (amount - 5).coerceAtLeast(5)
                        }) {
                            Icon(Icons.Filled.Remove, contentDescription = "Azalt")
                        }
                        Text(
                            text = if (mode == SnoozeMode.HOUR) "$amount saat" else "$amount dk",
                            style = MaterialTheme.typography.headlineSmall
                        )
                        IconButton(onClick = {
                            amount = if (mode == SnoozeMode.HOUR) amount + 1 else amount + 5
                        }) {
                            Icon(Icons.Filled.Add, contentDescription = "Artır")
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !isPast,
                onClick = {
                    if (mode == SnoozeMode.DATE) onConfirmAt(targetAt) else onConfirm(minutes())
                }
            ) { Text("Ertele") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("İptal") }
        }
    )

    if (showDate) {
        val dateState = rememberDatePickerState(
            initialSelectedDateMillis = toPickerMillis(targetAt),
            selectableDates = TodayOrLaterDates
        )
        DatePickerDialog(
            onDismissRequest = { showDate = false },
            confirmButton = {
                TextButton(onClick = {
                    dateState.selectedDateMillis?.let { targetAt = combinePickedDate(it, targetAt) }
                    showDate = false
                }) { Text("Tamam") }
            },
            dismissButton = {
                TextButton(onClick = { showDate = false }) { Text("İptal") }
            }
        ) {
            DatePicker(state = dateState)
        }
    }
}
