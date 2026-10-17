package com.kichikan.ak1.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val AbotorabNavy = Color(0xFF0D1119)
private val AbotorabPanel = Color(0xFF171E29)
private val AbotorabPanelRaised = Color(0xFF222C3A)
private val AbotorabGold = Color(0xFFD8B56D)
private val AbotorabIvory = Color(0xFFF4F0E7)
private val AbotorabGreen = Color(0xFF39836A)
private val AbotorabMuted = Color(0xFFB6BECA)

@Composable
fun AK1Theme(content: @Composable () -> Unit) {
    val scheme = darkColorScheme(
        primary = AbotorabGold,
        onPrimary = Color(0xFF211A0C),
        primaryContainer = Color(0xFF3A311F),
        onPrimaryContainer = AbotorabIvory,
        secondary = AbotorabGreen,
        onSecondary = Color.White,
        secondaryContainer = Color(0xFF203C35),
        onSecondaryContainer = Color(0xFFD6F2E7),
        tertiary = Color(0xFF8DA7D6),
        onTertiary = Color(0xFF101A2C),
        tertiaryContainer = Color(0xFF293750),
        onTertiaryContainer = Color(0xFFE4ECFF),
        background = AbotorabNavy,
        onBackground = AbotorabIvory,
        surface = AbotorabPanel,
        onSurface = AbotorabIvory,
        surfaceVariant = AbotorabPanelRaised,
        onSurfaceVariant = AbotorabMuted,
        surfaceTint = AbotorabGold,
        outline = Color(0xFF596474),
        outlineVariant = Color(0xFF343E4D),
        error = Color(0xFFFF7777),
        onError = Color(0xFF330B0B),
        errorContainer = Color(0xFF562323),
        onErrorContainer = Color(0xFFFFDEDE)
    )

    // Compact type scale: keeps cards and forms readable without letting headings
    // consume most of a phone screen.
    val base = Typography()
    val typography = base.copy(
        displayLarge = base.displayLarge.copy(fontSize = 34.sp, lineHeight = 40.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.6).sp),
        displayMedium = base.displayMedium.copy(fontSize = 30.sp, lineHeight = 36.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.4).sp),
        displaySmall = base.displaySmall.copy(fontSize = 26.sp, lineHeight = 32.sp, fontWeight = FontWeight.Bold),
        headlineLarge = base.headlineLarge.copy(fontSize = 25.sp, lineHeight = 31.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.4).sp),
        headlineMedium = base.headlineMedium.copy(fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.2).sp),
        headlineSmall = base.headlineSmall.copy(fontSize = 19.sp, lineHeight = 25.sp, fontWeight = FontWeight.Bold),
        titleLarge = base.titleLarge.copy(fontSize = 18.sp, lineHeight = 24.sp, fontWeight = FontWeight.Bold),
        titleMedium = base.titleMedium.copy(fontSize = 15.sp, lineHeight = 21.sp, fontWeight = FontWeight.SemiBold),
        titleSmall = base.titleSmall.copy(fontSize = 14.sp, lineHeight = 19.sp, fontWeight = FontWeight.SemiBold),
        bodyLarge = base.bodyLarge.copy(fontSize = 14.sp, lineHeight = 20.sp),
        bodyMedium = base.bodyMedium.copy(fontSize = 13.sp, lineHeight = 18.sp),
        bodySmall = base.bodySmall.copy(fontSize = 11.sp, lineHeight = 15.sp),
        labelLarge = base.labelLarge.copy(fontSize = 13.sp, lineHeight = 17.sp, fontWeight = FontWeight.SemiBold),
        labelMedium = base.labelMedium.copy(fontSize = 11.sp, lineHeight = 15.sp, fontWeight = FontWeight.Medium),
        labelSmall = base.labelSmall.copy(fontSize = 10.sp, lineHeight = 14.sp, fontWeight = FontWeight.Medium)
    )
    val shapes = Shapes(
        extraSmall = RoundedCornerShape(5.dp),
        small = RoundedCornerShape(9.dp),
        medium = RoundedCornerShape(13.dp),
        large = RoundedCornerShape(18.dp),
        extraLarge = RoundedCornerShape(24.dp)
    )
    MaterialTheme(colorScheme = scheme, typography = typography, shapes = shapes, content = content)
}
