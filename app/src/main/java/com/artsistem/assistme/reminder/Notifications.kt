package com.artsistem.assistme.reminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.artsistem.assistme.R
import com.artsistem.assistme.data.Reminder
import com.artsistem.assistme.ui.AlarmActivity

/**
 * Bildirim kanalı oluşturma ve hatırlatma bildirimini (ertele / tamam
 * aksiyonlarıyla) gösterme işlerini toplar.
 *
 * Hatırlatma zamanı gelince ALARM seviyesinde bir bildirim basılır:
 * tam ekran niyet (full-screen intent) ile kilit ekranının üstünde
 * [AlarmActivity] açılır ve alarm sesi çalar.
 */
object Notifications {

    /** Eski (normal) hatırlatma kanalı — geriye dönük uyumluluk için tutulur. */
    const val CHANNEL_ID = "reminders"

    /** Alarm seviyesindeki bildirimler için kanal (alarm sesi + yüksek önem). */
    const val CHANNEL_ALARM_ID = "alarms"

    private const val GROUP_KEY = "com.artsistem.assistme.REMINDERS"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)

        if (manager.getNotificationChannel(CHANNEL_ID) == null) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.channel_reminders_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.channel_reminders_desc)
                enableVibration(true)
                setShowBadge(true)
            }
            manager.createNotificationChannel(channel)
        }

        if (manager.getNotificationChannel(CHANNEL_ALARM_ID) == null) {
            val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val attrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            val channel = NotificationChannel(
                CHANNEL_ALARM_ID,
                context.getString(R.string.channel_alarms_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.channel_alarms_desc)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 1000, 1000)
                setShowBadge(true)
                setBypassDnd(true)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                if (alarmUri != null) setSound(alarmUri, attrs)
            }
            manager.createNotificationChannel(channel)
        }
    }

    fun show(context: Context, reminder: Reminder, snoozeMinutes: Int) {
        ensureChannel(context)

        val notificationId = reminder.id.toInt()

        // Tam ekran alarm ekranını açacak niyet (kilit ekranı üstünde + ekranı uyandırır).
        val alarmActivityIntent = Intent(context, AlarmActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(ReminderScheduler.EXTRA_REMINDER_ID, reminder.id)
            putExtra(AlarmActivity.EXTRA_TITLE, reminder.title)
            putExtra(AlarmActivity.EXTRA_NOTE, reminder.note)
            putExtra(AlarmActivity.EXTRA_SNOOZE_MINUTES, snoozeMinutes)
            putExtra(AlarmActivity.EXTRA_REPEATING, reminder.isRepeating)
        }
        val fullScreenIntent = PendingIntent.getActivity(
            context,
            notificationId,
            alarmActivityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeIntent = actionPendingIntent(
            context,
            reminder.id,
            NotificationActionReceiver.ACTION_SNOOZE,
            requestCode = notificationId * 10 + 1,
            snoozeMinutes = snoozeMinutes
        )
        val doneIntent = actionPendingIntent(
            context,
            reminder.id,
            NotificationActionReceiver.ACTION_DONE,
            requestCode = notificationId * 10 + 2
        )

        val snoozeLabel = context.getString(R.string.action_snooze_minutes, snoozeMinutes)

        val builder = NotificationCompat.Builder(context, CHANNEL_ALARM_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(reminder.title)
            .setContentText(reminder.note.ifBlank { context.getString(R.string.notification_default_body) })
            .setStyle(NotificationCompat.BigTextStyle().bigText(reminder.note))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            // Bildirime dokununca da tam ekran alarm açılsın.
            .setContentIntent(fullScreenIntent)
            // Asıl alarm davranışı: tam ekran niyet.
            .setFullScreenIntent(fullScreenIntent, true)
            .setGroup(GROUP_KEY)
            .addAction(R.drawable.ic_snooze, snoozeLabel, snoozeIntent)
            .addAction(R.drawable.ic_done, context.getString(R.string.action_done), doneIntent)

        if (reminder.note.isBlank()) {
            builder.setStyle(null)
        }

        try {
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
        } catch (_: SecurityException) {
            // POST_NOTIFICATIONS izni verilmemiş; sessizce geç.
        }
    }

    fun cancel(context: Context, reminderId: Long) {
        NotificationManagerCompat.from(context).cancel(reminderId.toInt())
    }

    private fun actionPendingIntent(
        context: Context,
        reminderId: Long,
        action: String,
        requestCode: Int,
        snoozeMinutes: Int = 0
    ): PendingIntent {
        val intent = Intent(context, NotificationActionReceiver::class.java).apply {
            this.action = action
            putExtra(ReminderScheduler.EXTRA_REMINDER_ID, reminderId)
            putExtra(NotificationActionReceiver.EXTRA_SNOOZE_MINUTES, snoozeMinutes)
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
