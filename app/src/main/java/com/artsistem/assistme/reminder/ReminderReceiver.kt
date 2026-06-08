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
 * Alarm tetiklendiğinde çalışır: bildirimi gösterir ve hatırlatma tekrarlıysa
 * bir sonraki tetiklenmeyi kurar.
 */
class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getLongExtra(ReminderScheduler.EXTRA_REMINDER_ID, -1L)
        if (reminderId == -1L) return

        val pending = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repo = ReminderRepository(AppDatabase.get(appContext).reminderDao())
                val reminder = repo.getById(reminderId)
                if (reminder == null || !reminder.enabled) return@launch

                Notifications.show(appContext, reminder)

                if (reminder.isRepeating) {
                    val next = Recurrence.nextTrigger(reminder)
                    if (next != null) {
                        repo.updateTriggerTime(reminder.id, next)
                        ReminderScheduler.schedule(appContext, reminder.copy(triggerAtMillis = next))
                    }
                } else {
                    // Tek seferlik hatırlatma tetiklendi -> pasifleştir.
                    repo.setEnabled(reminder.id, false)
                }
            } catch (e: Exception) {
                Log.e("ReminderReceiver", "Hatırlatma işlenemedi", e)
            } finally {
                pending.finish()
            }
        }
    }
}
