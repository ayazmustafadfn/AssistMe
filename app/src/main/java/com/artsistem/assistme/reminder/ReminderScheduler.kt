package com.artsistem.assistme.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.artsistem.assistme.data.Reminder

/**
 * AlarmManager üzerinden hatırlatma alarmlarını kurar / iptal eder.
 *
 * Tam zamanlı (exact) ve Doze modunda da çalışan alarmlar kullanılır ki
 * hatırlatma tam istenen dakikada bildirim olarak ekrana düşsün.
 */
object ReminderScheduler {

    private const val TAG = "ReminderScheduler"
    const val EXTRA_REMINDER_ID = "extra_reminder_id"

    fun schedule(context: Context, reminder: Reminder) {
        if (!reminder.enabled) return
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val pendingIntent = alarmPendingIntent(context, reminder.id)
        val triggerAt = reminder.triggerAtMillis

        // Android 12+ tam alarm izni yoksa yaklaşık alarma düş.
        val canExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else true

        try {
            if (canExact) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAt,
                    pendingIntent
                )
            } else {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAt,
                    pendingIntent
                )
            }
            Log.d(TAG, "Hatırlatma #${reminder.id} kuruldu: $triggerAt (exact=$canExact)")
        } catch (e: SecurityException) {
            Log.e(TAG, "Alarm kurulamadı (izin yok)", e)
        }
    }

    fun cancel(context: Context, reminderId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(alarmPendingIntent(context, reminderId))
        Log.d(TAG, "Hatırlatma #$reminderId iptal edildi")
    }

    private fun alarmPendingIntent(context: Context, reminderId: Long): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = "com.artsistem.assistme.ALARM_FIRE"
            putExtra(EXTRA_REMINDER_ID, reminderId)
        }
        return PendingIntent.getBroadcast(
            context,
            reminderId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
