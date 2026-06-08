package com.artsistem.assistme.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Bir hatırlatmanın tekrar şekli.
 *
 * [intervalMinutes] DAILY/WEEKLY gibi sabit periyotlar için baz alınır;
 * [CUSTOM] seçildiğinde kullanıcı kendi dakika cinsinden aralığını girer
 * ([Reminder.customIntervalMinutes]).
 */
enum class RepeatType(val intervalMinutes: Long) {
    NONE(0),
    HOURLY(60),
    DAILY(60 * 24),
    WEEKLY(60 * 24 * 7),
    MONTHLY(60 * 24 * 30),
    CUSTOM(0);
}

/**
 * Tek bir hatırlatma kaydı.
 *
 * @param triggerAtMillis Bir sonraki tetiklenme zamanı (epoch ms).
 * @param enabled Kapalıysa alarm kurulmaz.
 */
@Entity(tableName = "reminders")
data class Reminder(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val note: String = "",
    val triggerAtMillis: Long,
    val repeatType: RepeatType = RepeatType.NONE,
    /** [RepeatType.CUSTOM] için dakika cinsinden tekrar aralığı. */
    val customIntervalMinutes: Long = 0,
    val enabled: Boolean = true,
    /** Ait olduğu grubun id'si; null ise "Grupsuz". */
    val groupId: Long? = null,
    val createdAtMillis: Long = System.currentTimeMillis()
) {
    /** Bu hatırlatma tekrarlı mı? */
    val isRepeating: Boolean
        get() = repeatType != RepeatType.NONE

    /** Tekrar aralığını dakika cinsinden döndürür (tekrarsızsa 0). */
    fun repeatIntervalMinutes(): Long = when (repeatType) {
        RepeatType.CUSTOM -> customIntervalMinutes
        else -> repeatType.intervalMinutes
    }
}
