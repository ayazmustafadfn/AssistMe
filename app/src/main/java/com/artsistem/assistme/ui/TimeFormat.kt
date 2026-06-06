package com.artsistem.assistme.ui

import com.artsistem.assistme.data.RepeatType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val dateTimeFormat = SimpleDateFormat("d MMM yyyy, HH:mm", Locale("tr"))

fun formatDateTime(millis: Long): String = dateTimeFormat.format(Date(millis))

fun repeatLabel(repeatType: RepeatType, customMinutes: Long): String = when (repeatType) {
    RepeatType.NONE -> "Tekrar yok"
    RepeatType.HOURLY -> "Her saat"
    RepeatType.DAILY -> "Her gün"
    RepeatType.WEEKLY -> "Her hafta"
    RepeatType.MONTHLY -> "Her ay"
    RepeatType.CUSTOM -> "Her $customMinutes dakikada"
}
