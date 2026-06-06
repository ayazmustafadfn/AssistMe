package com.artsistem.assistme.reminder

import com.artsistem.assistme.data.Reminder
import com.artsistem.assistme.data.RepeatType
import java.util.Calendar

/**
 * Tekrarlı hatırlatmalar için bir sonraki tetiklenme zamanını hesaplar.
 *
 * Sabit aralıklılar (HOURLY/CUSTOM) milisaniye eklenerek; takvim tabanlılar
 * (DAILY/WEEKLY/MONTHLY) Calendar ile ilerletilir ki yaz saati / ay uzunluğu
 * gibi durumlarda saat sabit kalsın.
 */
object Recurrence {

    /**
     * [reminder] son [from] zamanında tetiklendikten sonra, [now] anından
     * kesinlikle sonra olan bir sonraki tetiklenme zamanını döndürür.
     * Tekrarsız hatırlatmalar için null döner.
     */
    fun nextTrigger(reminder: Reminder, now: Long = System.currentTimeMillis()): Long? {
        if (!reminder.isRepeating) return null

        var next = reminder.triggerAtMillis

        return when (reminder.repeatType) {
            RepeatType.HOURLY, RepeatType.CUSTOM -> {
                val stepMs = reminder.repeatIntervalMinutes() * 60_000L
                if (stepMs <= 0) return null
                // Geçmişteki tetiklemeleri atla, gelecekteki ilk zamana git.
                if (next <= now) {
                    val missed = ((now - next) / stepMs) + 1
                    next += missed * stepMs
                }
                next
            }

            RepeatType.DAILY, RepeatType.WEEKLY, RepeatType.MONTHLY -> {
                val cal = Calendar.getInstance().apply { timeInMillis = next }
                while (cal.timeInMillis <= now) {
                    when (reminder.repeatType) {
                        RepeatType.DAILY -> cal.add(Calendar.DAY_OF_YEAR, 1)
                        RepeatType.WEEKLY -> cal.add(Calendar.WEEK_OF_YEAR, 1)
                        RepeatType.MONTHLY -> cal.add(Calendar.MONTH, 1)
                        else -> Unit
                    }
                }
                cal.timeInMillis
            }

            RepeatType.NONE -> null
        }
    }
}
