package com.artsistem.assistme.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.artsistem.assistme.data.AppDatabase
import com.artsistem.assistme.data.ReminderRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Bildirim üzerindeki "Ertele" ve "Tamam" aksiyonlarını işler.
 *
 * - Ertele: bildirimi kapatır, [snoozeMinutes] dakika sonraya AYRI bir erteleme
 *   alarmı kurar ve zamanı `snoozedUntilMillis`'e yazar. Tekrarlı hatırlatmanın
 *   asıl tekrar alarmı korunur.
 * - Tamam: bildirimi kapatır, ek işlem yapmaz.
 */
class NotificationActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getLongExtra(ReminderScheduler.EXTRA_REMINDER_ID, -1L)
        if (reminderId == -1L) return

        val appContext = context.applicationContext
        Notifications.cancel(appContext, reminderId)
        // Hangi yoldan gelirse gelsin alarm sesini durdur (güvenlik ağı).
        AlarmPlayer.stop()

        when (intent.action) {
            ACTION_SNOOZE -> {
                val minutes = intent.getIntExtra(EXTRA_SNOOZE_MINUTES, Settings.SNOOZE_SHORT_MINUTES)
                    .coerceAtLeast(1)
                // "Diğer… → Tarih" ile seçilen kesin zaman (0 = yok, süre kullanılır).
                val snoozeAtExtra = intent.getLongExtra(EXTRA_SNOOZE_AT, 0L)
                val useAbsolute = snoozeAtExtra > System.currentTimeMillis()
                // "Diğer…" ile seçilen özel süreyi hatırla (sabit 15/60 ve tarih seçimi hariç).
                if (!useAbsolute && minutes != Settings.SNOOZE_SHORT_MINUTES && minutes != Settings.SNOOZE_LONG_MINUTES) {
                    Settings.setLastCustomSnoozeMinutes(appContext, minutes)
                }
                val pending = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val repo = ReminderRepository(AppDatabase.get(appContext).reminderDao())
                        val reminder = repo.getById(reminderId) ?: return@launch
                        val snoozeAt = if (useAbsolute) snoozeAtExtra
                        else System.currentTimeMillis() + minutes * 60_000L
                        // Ertelemeyi DB'ye yaz (listede görünsün, yeniden başlatmada kurulsun).
                        // Tek seferlik hatırlatma tetiklenince pasifleşmişti; erteleme sürdükçe aktif.
                        repo.setSnoozedUntil(reminder.id, snoozeAt)
                        if (!reminder.isRepeating) repo.setEnabled(reminder.id, true)
                        ReminderScheduler.scheduleSnooze(appContext, reminder.id, snoozeAt)
                        Log.d("NotifAction", "Hatırlatma #$reminderId $minutes dk ertelendi")
                    } finally {
                        pending.finish()
                    }
                }
            }

            ACTION_DONE -> {
                // Tamamlandı: geçmişe yaz. (Tekrarlıysa bir sonraki tekrar zaten kuruludur.)
                val pending = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val db = AppDatabase.get(appContext)
                        val repo = ReminderRepository(db.reminderDao(), db.groupDao(), db.historyDao())
                        repo.getById(reminderId)?.let {
                            repo.addHistory(it, "done", System.currentTimeMillis())
                        }
                    } finally {
                        pending.finish()
                    }
                }
            }

            ACTION_DISMISS -> {
                // Kapat: sadece alarmı sustur/kapat, başka bir şey yapma.
                Log.d("NotifAction", "Hatırlatma #$reminderId kapatıldı")
            }
        }
    }

    companion object {
        const val ACTION_SNOOZE = "com.artsistem.assistme.ACTION_SNOOZE"
        const val ACTION_DONE = "com.artsistem.assistme.ACTION_DONE"
        const val ACTION_DISMISS = "com.artsistem.assistme.ACTION_DISMISS"
        const val EXTRA_SNOOZE_MINUTES = "extra_snooze_minutes"
        /** Kesin erteleme zamanı (epoch ms); verilirse dakika yerine kullanılır. */
        const val EXTRA_SNOOZE_AT = "extra_snooze_at"
    }
}
