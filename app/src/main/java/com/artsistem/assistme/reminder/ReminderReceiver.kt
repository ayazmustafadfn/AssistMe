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
                val db = AppDatabase.get(appContext)
                val repo = ReminderRepository(db.reminderDao(), db.groupDao(), db.historyDao())
                val reminder = repo.getById(reminderId)
                if (reminder == null || !reminder.enabled) return@launch

                Notifications.show(appContext, reminder)

                if (reminder.isRepeating) {
                    val next = Recurrence.nextTrigger(reminder)
                    val newRemaining = reminder.repeatCount?.minus(1)
                    val reachedCount = newRemaining != null && newRemaining <= 0
                    val end = reminder.repeatEndMillis
                    val reachedDate = end != null && (next == null || next > end)

                    if (next == null || reachedCount || reachedDate) {
                        // Tekrar bitti -> pasifleştir.
                        if (reminder.repeatCount != null) repo.updateRepeatCount(reminder.id, 0)
                        repo.setEnabled(reminder.id, false)
                    } else {
                        if (newRemaining != null) repo.updateRepeatCount(reminder.id, newRemaining)
                        repo.updateTriggerTime(reminder.id, next)
                        ReminderScheduler.schedule(
                            appContext,
                            reminder.copy(triggerAtMillis = next, repeatCount = newRemaining)
                        )
                    }
                } else {
                    // Tek seferlik hatırlatma tetiklendi -> tamamlandı say, geçmişe yaz, pasifleştir.
                    repo.addHistory(reminder, "fired", System.currentTimeMillis())
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
