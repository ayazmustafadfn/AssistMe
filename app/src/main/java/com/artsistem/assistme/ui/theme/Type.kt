package com.artsistem.assistme.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Rafine tipografi ölçeği. (Şimdilik sistem fontu; ileride Plus Jakarta Sans
 * gömülebilir.) Başlıklar daha dolgun ve sıkı, gövde rahat okunur.
 */
private val Display = FontFamily.Default

val AppTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = Display, fontWeight = FontWeight.Bold,
        fontSize = 32.sp, lineHeight = 40.sp, letterSpacing = (-0.5).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = Display, fontWeight = FontWeight.Bold,
        fontSize = 26.sp, lineHeight = 33.sp, letterSpacing = (-0.3).sp
    ),
    headlineSmall = TextStyle(
        fontFamily = Display, fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp, lineHeight = 28.sp, letterSpacing = (-0.2).sp
    ),
    titleLarge = TextStyle(
        fontFamily = Display, fontWeight = FontWeight.SemiBold,
        fontSize = 21.sp, lineHeight = 27.sp, letterSpacing = (-0.2).sp
    ),
    titleMedium = TextStyle(
        fontFamily = Display, fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp, lineHeight = 22.sp, letterSpacing = 0.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = Display, fontWeight = FontWeight.Normal,
        fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.1.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = Display, fontWeight = FontWeight.Normal,
        fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp
    ),
    bodySmall = TextStyle(
        fontFamily = Display, fontWeight = FontWeight.Normal,
        fontSize = 12.5.sp, lineHeight = 17.sp, letterSpacing = 0.2.sp
    ),
    labelLarge = TextStyle(
        fontFamily = Display, fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.2.sp
    ),
    labelMedium = TextStyle(
        fontFamily = Display, fontWeight = FontWeight.Medium,
        fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.4.sp
    )
)
