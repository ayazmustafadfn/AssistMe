package com.artsistem.assistme.ui

import com.artsistem.assistme.data.Reminder
import com.artsistem.assistme.data.ReminderHistory
import com.artsistem.assistme.data.RepeatType
import java.util.Calendar

/** Bir güne düşen tek hatırlatma anı (tekrarlılar her tekrar için ayrı). */
data class DayItem(val atMillis: Long, val title: String, val snoozed: Boolean)

/** Ana ekran özet kartının bir gün için içeriği. */
data class DaySummary(
    val dayStart: Long,
    /** Bugün ve sonrası: o gün çalacak hatırlatmalar (saat sırasıyla). */
    val upcoming: List<DayItem>,
    /** Geçmiş günler: geçmişe yazılmış çalma/tamamlama kayıtları. */
    val history: List<ReminderHistory>
)

fun startOfDay(millis: Long): Long = Calendar.getInstance().apply {
    timeInMillis = millis
    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
}.timeInMillis

fun addDays(millis: Long, days: Int): Long = Calendar.getInstance().apply {
    timeInMillis = millis
    add(Calendar.DAY_OF_YEAR, days)
}.timeInMillis

/**
 * [dayOffset] gün (0 = bugün) için özet. Geçmiş günler DB'de yalnızca geçmiş
 * tablosunda iz bırakır; bugün ve sonrası aktif hatırlatmalardan (tekrarlar
 * açılarak) hesaplanır.
 */
fun summarizeDay(
    dayOffset: Int,
    reminders: List<Reminder>,
    history: List<ReminderHistory>,
    now: Long = System.currentTimeMillis()
): DaySummary {
    val dayStart = startOfDay(addDays(now, dayOffset))
    val dayEnd = addDays(dayStart, 1)

    if (dayOffset < 0) {
        val past = history.filter { it.completedAtMillis in dayStart until dayEnd }
            .sortedBy { it.completedAtMillis }
        return DaySummary(dayStart, emptyList(), past)
    }

    val items = mutableListOf<DayItem>()
    for (r in reminders) {
        r.snoozedUntilMillis?.let { if (it in dayStart until dayEnd) items += DayItem(it, r.title, true) }
        if (!r.enabled) continue
        occurrences(r, dayStart, dayEnd).forEach { items += DayItem(it, r.title, false) }
    }
    return DaySummary(dayStart, items.sortedBy { it.atMillis }, emptyList())
}

/** [r]'nin [from, to) aralığına düşen tetiklenme anları; tekrar bitişine uyar. */
private fun occurrences(r: Reminder, from: Long, to: Long): List<Long> {
    if (!r.isRepeating) return if (r.triggerAtMillis in from until to) listOf(r.triggerAtMillis) else emptyList()

    val out = mutableListOf<Long>()
    val cal = Calendar.getInstance().apply { timeInMillis = r.triggerAtMillis }
    val stepMs = r.repeatIntervalMinutes() * 60_000L
    var remaining = r.repeatCount ?: Int.MAX_VALUE
    var guard = 0
    while (cal.timeInMillis < to && remaining > 0 && guard++ < 5000) {
        val t = cal.timeInMillis
        val end = r.repeatEndMillis
        if (end != null && t > end) break
        if (t >= from) out += t
        remaining--
        when (r.repeatType) {
            RepeatType.HOURLY, RepeatType.CUSTOM -> {
                if (stepMs <= 0) break
                cal.timeInMillis = t + stepMs
            }
            RepeatType.DAILY -> cal.add(Calendar.DAY_OF_YEAR, 1)
            RepeatType.WEEKLY -> cal.add(Calendar.WEEK_OF_YEAR, 1)
            RepeatType.MONTHLY -> cal.add(Calendar.MONTH, 1)
            RepeatType.NONE -> break
        }
    }
    return out
}
