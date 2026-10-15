package com.kichikan.ak1.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.delay
import java.time.LocalTime

private enum class DayPhase { MORNING, DAY, SUNSET, NIGHT }

/**
 * AK1's shared visual system.
 *
 * The palette follows the device's local time, without changing screen copy or
 * user data. The clock is refreshed once a minute so the theme changes naturally
 * when a phase boundary is crossed while the app remains open.
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

    val scheme = when (phase) {
        DayPhase.MORNING -> lightColorScheme(
            primary = Color(0xFF426B50),
            onPrimary = Color(0xFFFFFFFF),
            primaryContainer = Color(0xFFDDEBDD),
            onPrimaryContainer = Color(0xFF183B27),
            secondary = Color(0xFFB56A39),
            onSecondary = Color(0xFFFFFFFF),
            secondaryContainer = Color(0xFFFBE7D7),
            onSecondaryContainer = Color(0xFF512B12),
            tertiary = Color(0xFF5977A6),
            onTertiary = Color(0xFFFFFFFF),
            background = Color(0xFFF6F8F1),
            onBackground = Color(0xFF1D3024),
            surface = Color(0xFFFFFFFF),
            onSurface = Color(0xFF1D3024),
            surfaceVariant = Color(0xFFE8EEE4),
            onSurfaceVariant = Color(0xFF526458),
            outline = Color(0xFF9CAD98)
        )
        DayPhase.DAY -> lightColorScheme(
            primary = Color(0xFF087F8C),
            onPrimary = Color(0xFFFFFFFF),
            primaryContainer = Color(0xFFC9F0EE),
            onPrimaryContainer = Color(0xFF073B40),
            secondary = Color(0xFFCA5B3F),
            onSecondary = Color(0xFFFFFFFF),
            secondaryContainer = Color(0xFFFFE0D5),
            onSecondaryContainer = Color(0xFF592014),
            tertiary = Color(0xFF6554A4),
            onTertiary = Color(0xFFFFFFFF),
            background = Color(0xFFF6F7F3),
            onBackground = Color(0xFF172D35),
            surface = Color(0xFFFFFFFF),
            onSurface = Color(0xFF172D35),
            surfaceVariant = Color(0xFFE7F0EE),
            onSurfaceVariant = Color(0xFF52686C),
            outline = Color(0xFF9CB3B2)
        )
        DayPhase.SUNSET -> lightColorScheme(
            primary = Color(0xFF9B4F2F),
            onPrimary = Color(0xFFFFFFFF),
            primaryContainer = Color(0xFFF6DDCE),
            onPrimaryContainer = Color(0xFF512414),
            secondary = Color(0xFF8A6416),
            onSecondary = Color(0xFFFFFFFF),
            secondaryContainer = Color(0xFFF6E7B9),
            onSecondaryContainer = Color(0xFF342500),
            tertiary = Color(0xFF6554A4),
            onTertiary = Color(0xFFFFFFFF),
            background = Color(0xFFFBF6F0),
            onBackground = Color(0xFF35251F),
            surface = Color(0xFFFFFFFF),
            onSurface = Color(0xFF35251F),
            surfaceVariant = Color(0xFFF2E8DF),
            onSurfaceVariant = Color(0xFF6B574C),
            outline = Color(0xFFC8AA97)
        )
        DayPhase.NIGHT -> darkColorScheme(
            primary = Color(0xFF8ED9D0),
            onPrimary = Color(0xFF062B2D),
            primaryContainer = Color(0xFF164B4A),
            onPrimaryContainer = Color(0xFFD1F6F1),
            secondary = Color(0xFFF4B08B),
            onSecondary = Color(0xFF47200D),
            secondaryContainer = Color(0xFF693B25),
            onSecondaryContainer = Color(0xFFFFDBCA),
            tertiary = Color(0xFFC7B8FF),
            onTertiary = Color(0xFF30245C),
            background = Color(0xFF0D1726),
            onBackground = Color(0xFFE3EAF4),
            surface = Color(0xFF152235),
            onSurface = Color(0xFFE3EAF4),
            surfaceVariant = Color(0xFF24354A),
            onSurfaceVariant = Color(0xFFB6C5D8),
            outline = Color(0xFF52677F)
        )
    }

    MaterialTheme(
        colorScheme = scheme,
        typography = Typography(),
        content = content
    )
}
