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
 * Cihaz yeniden başladığında (veya saat/zaman dilimi değiştiğinde) tüm aktif
 * hatırlatma alarmlarını yeniden kurar. AlarmManager alarmları reboot sonrası
 * silindiği için bu şarttır.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repo = ReminderRepository(AppDatabase.get(appContext).reminderDao())
                val reminders = repo.getAllEnabled()
                val now = System.currentTimeMillis()
                for (reminder in reminders) {
                    // Geçmişte kalmış tekrarlı hatırlatmaları ileri sar.
                    val target = if (reminder.isRepeating && reminder.triggerAtMillis <= now) {
                        val next = Recurrence.nextTrigger(reminder, now) ?: reminder.triggerAtMillis
                        repo.updateTriggerTime(reminder.id, next)
                        next
                    } else {
                        reminder.triggerAtMillis
                    }
                    if (target > now) {
                        ReminderScheduler.schedule(appContext, reminder.copy(triggerAtMillis = target))
                    }
                }
                Log.d("BootReceiver", "${reminders.size} hatırlatma yeniden kuruldu")
            } catch (e: Exception) {
                Log.e("BootReceiver", "Alarmlar yeniden kurulamadı", e)
            } finally {
                pending.finish()
            }
        }
    }
}
