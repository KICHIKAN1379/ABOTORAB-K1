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

private val AbotorabNavy = Color(0xFF10131C)
private val AbotorabPanel = Color(0xFF1A202B)
private val AbotorabPanelRaised = Color(0xFF242D3B)
private val AbotorabGold = Color(0xFFD8B56D)
private val AbotorabIvory = Color(0xFFF5F0E6)
private val AbotorabGreen = Color(0xFF39836A)

@Composable
fun AK1Theme(content: @Composable () -> Unit) {
    val scheme = darkColorScheme(
        primary = AbotorabGold, onPrimary = Color(0xFF211A0C),
        primaryContainer = Color(0xFF40351F), onPrimaryContainer = AbotorabIvory,
        secondary = AbotorabGreen, onSecondary = Color.White,
        secondaryContainer = Color(0xFF203C35), onSecondaryContainer = Color(0xFFD6F2E7),
        tertiary = Color(0xFF8DA7D6), onTertiary = Color(0xFF101A2C),
        tertiaryContainer = Color(0xFF293750), onTertiaryContainer = Color(0xFFE4ECFF),
        background = AbotorabNavy, onBackground = AbotorabIvory,
        surface = AbotorabPanel, onSurface = AbotorabIvory,
        surfaceVariant = AbotorabPanelRaised, onSurfaceVariant = Color(0xFFC0C6D0),
        outline = Color(0xFF596474), error = Color(0xFFFF7777), onError = Color(0xFF330B0B),
        errorContainer = Color(0xFF562323), onErrorContainer = Color(0xFFFFDEDE)
    )
    val base = Typography()
    val typography = base.copy(
        headlineLarge = base.headlineLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
        headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.3).sp),
        headlineSmall = base.headlineSmall.copy(fontWeight = FontWeight.Bold),
        titleLarge = base.titleLarge.copy(fontWeight = FontWeight.Bold),
        titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        labelLarge = base.labelLarge.copy(fontWeight = FontWeight.SemiBold)
    )
    val shapes = Shapes(small = RoundedCornerShape(9.dp), medium = RoundedCornerShape(14.dp), large = RoundedCornerShape(20.dp))
    MaterialTheme(colorScheme = scheme, typography = typography, shapes = shapes, content = content)
}
