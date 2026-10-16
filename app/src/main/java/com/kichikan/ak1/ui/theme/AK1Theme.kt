package com.kichikan.ak1.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.time.LocalTime

private enum class DayPhase { MORNING, DAY, SUNSET, NIGHT }

/**
 * Premium Abotorab visual system: deep navy surfaces, gold highlights and emerald
 * accents. Time changes the accent subtly, not the entire layout or readability.
 */
@Composable
fun AK1Theme(content: @Composable () -> Unit) {
    var minuteOfDay by remember {
        val now = LocalTime.now()
        mutableIntStateOf(now.hour * 60 + now.minute)
    }
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000)
            val now = LocalTime.now()
            minuteOfDay = now.hour * 60 + now.minute
        }
    }

    val phase = when (minuteOfDay) {
        in (5 * 60)..<(10 * 60) -> DayPhase.MORNING
        in (10 * 60)..<(16 * 60) -> DayPhase.DAY
        in (16 * 60)..<(20 * 60) -> DayPhase.SUNSET
        else -> DayPhase.NIGHT
    }
    val accent = when (phase) {
        DayPhase.MORNING -> Color(0xFF38EF7D)
        DayPhase.DAY -> Color(0xFFFFD166)
        DayPhase.SUNSET -> Color(0xFFFFB36B)
        DayPhase.NIGHT -> Color(0xFF9AA7FF)
    }
    val scheme = darkColorScheme(
        primary = accent,
        onPrimary = Color(0xFF101A2E),
        primaryContainer = Color(0xFF263A50),
        onPrimaryContainer = Color(0xFFF5F7FB),
        secondary = Color(0xFFFFD166),
        onSecondary = Color(0xFF28200A),
        secondaryContainer = Color(0xFF4A3A19),
        onSecondaryContainer = Color(0xFFFFE9A6),
        tertiary = Color(0xFF38EF7D),
        onTertiary = Color(0xFF092415),
        tertiaryContainer = Color(0xFF174B35),
        onTertiaryContainer = Color(0xFFB8FFD0),
        background = Color(0xFF0B1323),
        onBackground = Color(0xFFF2F5FA),
        surface = Color(0xFF121F33),
        onSurface = Color(0xFFF2F5FA),
        surfaceVariant = Color(0xFF1C2B42),
        onSurfaceVariant = Color(0xFFB7C5D9),
        outline = Color(0xFF526781),
        error = Color(0xFFFF6B75),
        onError = Color(0xFF2A060A),
        errorContainer = Color(0xFF5A1D28),
        onErrorContainer = Color(0xFFFFD9DC)
    )
    val baseTypography = Typography()
    val typography = baseTypography.copy(
        headlineMedium = baseTypography.headlineMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.4).sp),
        headlineSmall = baseTypography.headlineSmall.copy(fontWeight = FontWeight.Bold),
        titleLarge = baseTypography.titleLarge.copy(fontWeight = FontWeight.Bold),
        titleMedium = baseTypography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        labelLarge = baseTypography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
    )
    val shapes = Shapes(
        small = RoundedCornerShape(12.dp),
        medium = RoundedCornerShape(18.dp),
        large = RoundedCornerShape(26.dp)
    )
    MaterialTheme(colorScheme = scheme, typography = typography, shapes = shapes, content = content)
}
