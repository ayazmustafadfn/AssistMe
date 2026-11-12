package com.artsistem.assistme.ui

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableDates
import java.util.Calendar
import java.util.TimeZone

/**
 * DatePicker gün değerlerini UTC gece yarısı olarak verir. Yerel "bugün"ün
 * UTC gece yarısı karşılığı; bundan önceki günler seçilemez.
 */
private fun todayUtcMidnight(): Long {
    val local = Calendar.getInstance()
    return Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        clear()
        set(local.get(Calendar.YEAR), local.get(Calendar.MONTH), local.get(Calendar.DAY_OF_MONTH))
    }.timeInMillis
}

/** Yalnızca bugün ve sonrası seçilebilir (geriye dönük hatırlatma/erteleme engeli). */
@OptIn(ExperimentalMaterial3Api::class)
object TodayOrLaterDates : SelectableDates {
    override fun isSelectableDate(utcTimeMillis: Long): Boolean = utcTimeMillis >= todayUtcMidnight()
    override fun isSelectableYear(year: Int): Boolean = year >= Calendar.getInstance().get(Calendar.YEAR)
}

/** DatePicker'dan gelen günü (UTC gece yarısı) [timeOf]'un yerel saat/dakikasıyla birleştirir. */
fun combinePickedDate(pickedUtcMillis: Long, timeOf: Long): Long {
    val d = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = pickedUtcMillis }
    val t = Calendar.getInstance().apply { timeInMillis = timeOf }
    return Calendar.getInstance().apply {
        clear()
        set(
            d.get(Calendar.YEAR), d.get(Calendar.MONTH), d.get(Calendar.DAY_OF_MONTH),
            t.get(Calendar.HOUR_OF_DAY), t.get(Calendar.MINUTE), 0
        )
    }.timeInMillis
}

/** Yerel saat/dakikayı [base] gününe uygular. */
fun withTime(base: Long, hour: Int, minute: Int): Long = Calendar.getInstance().apply {
    timeInMillis = base
    set(Calendar.HOUR_OF_DAY, hour)
    set(Calendar.MINUTE, minute)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}.timeInMillis

/** DatePicker başlangıç değeri: yerel günün UTC gece yarısı karşılığı. */
fun toPickerMillis(localMillis: Long): Long {
    val l = Calendar.getInstance().apply { timeInMillis = localMillis }
    return Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        clear()
        set(l.get(Calendar.YEAR), l.get(Calendar.MONTH), l.get(Calendar.DAY_OF_MONTH))
    }.timeInMillis
}
