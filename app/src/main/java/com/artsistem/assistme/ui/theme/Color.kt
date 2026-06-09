package com.artsistem.assistme.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * "Sıcak & Sakin" tasarım kimliği: krem/kum arka plan, kiremit (terracotta)
 * birincil, koyu yeşil ikincil, sıcak amber üçüncül. Açık + koyu tema.
 */

// --- Açık tema ---
val LightColorScheme = lightColorScheme(
    primary = Color(0xFFC8553D),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFDBD0),
    onPrimaryContainer = Color(0xFF3B0A02),

    secondary = Color(0xFF2F5D50),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFB9E5D6),
    onSecondaryContainer = Color(0xFF08231B),

    tertiary = Color(0xFFB07D3B),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFBE0BA),
    onTertiaryContainer = Color(0xFF3A2A0A),

    background = Color(0xFFFBF7F0),
    onBackground = Color(0xFF2A2622),
    surface = Color(0xFFFBF7F0),
    onSurface = Color(0xFF2A2622),
    surfaceVariant = Color(0xFFECE3D6),
    onSurfaceVariant = Color(0xFF574F45),
    surfaceContainer = Color(0xFFF3ECE0),
    surfaceContainerHigh = Color(0xFFEEE6D9),
    surfaceContainerHighest = Color(0xFFE8E0D2),
    surfaceContainerLow = Color(0xFFF7F1E7),
    surfaceContainerLowest = Color(0xFFFFFFFF),

    outline = Color(0xFF897F71),
    outlineVariant = Color(0xFFD8CDBE),

    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),

    inverseSurface = Color(0xFF36322C),
    inverseOnSurface = Color(0xFFFBEFE2),
    inversePrimary = Color(0xFFFFB4A1)
)

// --- Koyu tema ---
val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFFFB4A1),
    onPrimary = Color(0xFF5E1606),
    primaryContainer = Color(0xFF9C3F2A),
    onPrimaryContainer = Color(0xFFFFDBD0),

    secondary = Color(0xFFA0D0C0),
    onSecondary = Color(0xFF073529),
    secondaryContainer = Color(0xFF214E41),
    onSecondaryContainer = Color(0xFFBCECDC),

    tertiary = Color(0xFFE6C18E),
    onTertiary = Color(0xFF422C06),
    tertiaryContainer = Color(0xFF5C421B),
    onTertiaryContainer = Color(0xFFFBE0BA),

    background = Color(0xFF1A1714),
    onBackground = Color(0xFFECE1D4),
    surface = Color(0xFF1A1714),
    onSurface = Color(0xFFECE1D4),
    surfaceVariant = Color(0xFF51463B),
    onSurfaceVariant = Color(0xFFD5C5B4),
    surfaceContainer = Color(0xFF272320),
    surfaceContainerHigh = Color(0xFF322D29),
    surfaceContainerHighest = Color(0xFF3D3833),
    surfaceContainerLow = Color(0xFF221E1B),
    surfaceContainerLowest = Color(0xFF140F0D),

    outline = Color(0xFFA0937F),
    outlineVariant = Color(0xFF51463B),

    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),

    inverseSurface = Color(0xFFECE1D4),
    inverseOnSurface = Color(0xFF36322C),
    inversePrimary = Color(0xFFC8553D)
)
