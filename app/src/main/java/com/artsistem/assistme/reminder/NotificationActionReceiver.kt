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
 * - Ertele: bildirimi kapatır, alarmı [snoozeMinutes] dakika sonraya kurar.
 *   (Tekrarlı hatırlatmanın asıl tekrar zamanı korunur; erteleme tek seferlik
 *   ek bir alarmdır ve aynı reminder id'si üzerinden yeniden kurulur.)
 * - Tamam: bildirimi kapatır, ek işlem yapmaz.
 */
class NotificationActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getLongExtra(ReminderScheduler.EXTRA_REMINDER_ID, -1L)
        if (reminderId == -1L) return

        val appContext = context.applicationContext
        Notifications.cancel(appContext, reminderId)

        when (intent.action) {
            ACTION_SNOOZE -> {
                val minutes = intent.getIntExtra(EXTRA_SNOOZE_MINUTES, Settings.DEFAULT_SNOOZE_MINUTES)
                val pending = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val repo = ReminderRepository(AppDatabase.get(appContext).reminderDao())
                        val reminder = repo.getById(reminderId) ?: return@launch
                        val snoozeAt = System.currentTimeMillis() + minutes * 60_000L
                        ReminderScheduler.schedule(
                            appContext,
                            reminder.copy(triggerAtMillis = snoozeAt, enabled = true)
                        )
                        Log.d("NotifAction", "Hatırlatma #$reminderId $minutes dk ertelendi")
                    } finally {
                        pending.finish()
                    }
                }
            }

            ACTION_DONE -> {
                Log.d("NotifAction", "Hatırlatma #$reminderId kapatıldı")
            }
        }
    }

    companion object {
        const val ACTION_SNOOZE = "com.artsistem.assistme.ACTION_SNOOZE"
        const val ACTION_DONE = "com.artsistem.assistme.ACTION_DONE"
        const val EXTRA_SNOOZE_MINUTES = "extra_snooze_minutes"
    }
}
