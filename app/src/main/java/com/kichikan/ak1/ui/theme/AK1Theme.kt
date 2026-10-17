package com.kichikan.ak1.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * AK1 visual system, built from the "4 icon styles" sheet:
 *  - Style 2 (neon gradient)  -> theme, background and buttons (this file + [Button]).
 *  - Style 1 (minimal vector) -> bottom-menu icons ([MinimalIcon]).
 *  - Style 3 (clay 3D)        -> service icons ([ClayIcon]).
 *  - Style 4 (layered glass)  -> cards, panels and everything else ([Card]).
 */
object Neon {
    val Navy = Color(0xFF021E43)
    val Indigo = Color(0xFF012C71)
    val Deep = Color(0xFF011D59)
    val Cyan = Color(0xFF1EFCE5)
    val Sky = Color(0xFF5BB5FF)
    val Violet = Color(0xFFB197FF)
    val Gold = Color(0xFFFEC856)
    val Coral = Color(0xFFE0684F)
    val Green = Color(0xFF3CAEA3)
    val Ice = Color(0xFFE8F0FF)

    val ButtonBrush = Brush.horizontalGradient(listOf(Cyan, Sky, Violet))
    val BackgroundBrush = Brush.verticalGradient(listOf(Navy, Indigo, Deep))
}

private val NeonScheme = darkColorScheme(
    primary = Neon.Cyan,
    onPrimary = Neon.Navy,
    primaryContainer = Color(0xFF0B4A86),
    onPrimaryContainer = Color(0xFFC9FFF8),
    secondary = Neon.Violet,
    onSecondary = Color(0xFF2A1A66),
    secondaryContainer = Color(0xFF3A2D86),
    onSecondaryContainer = Color(0xFFE6DEFF),
    tertiary = Neon.Gold,
    onTertiary = Color(0xFF3A2800),
    tertiaryContainer = Color(0xFF5A4300),
    onTertiaryContainer = Color(0xFFFFE9B0),
    background = Neon.Navy,
    onBackground = Neon.Ice,
    surface = Color(0xFF0B2F73),
    onSurface = Neon.Ice,
    // Translucent: cards painted with surfaceVariant read as glass over the gradient.
    surfaceVariant = Color(0x1FFFFFFF),
    onSurfaceVariant = Color(0xFFA9BEE0),
    surfaceTint = Neon.Cyan,
    outline = Color(0xFF4D6FB0),
    outlineVariant = Color(0xFF2A4A8A),
    error = Color(0xFFFF8A80),
    onError = Color(0xFF4A0A05),
    errorContainer = Color(0xFF7A1F18),
    onErrorContainer = Color(0xFFFFDAD6),
    surfaceContainerLowest = Neon.Navy,
    surfaceContainerLow = Color(0xFF082456),
    surfaceContainer = Color(0xFF0A2B68),
    surfaceContainerHigh = Color(0xFF0C3278),
    surfaceContainerHighest = Color(0xFF123D8F)
)

@Composable
fun AK1Theme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = NeonScheme,
        typography = Typography(),
        shapes = Shapes(
            small = RoundedCornerShape(12.dp),
            medium = RoundedCornerShape(18.dp),
            large = RoundedCornerShape(24.dp)
        )
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Neon.BackgroundBrush)
                .drawBehind {
                    // Soft neon glows, like the light bloom in the sheet's gradient tiles.
                    drawRect(
                        Brush.radialGradient(
                            listOf(Neon.Cyan.copy(alpha = 0.16f), Color.Transparent),
                            center = Offset(size.width * 0.95f, size.height * 0.04f),
                            radius = size.width * 0.95f
                        )
                    )
                    drawRect(
                        Brush.radialGradient(
                            listOf(Neon.Violet.copy(alpha = 0.20f), Color.Transparent),
                            center = Offset(size.width * 0.05f, size.height * 0.96f),
                            radius = size.width * 1.0f
                        )
                    )
                }
        ) {
            CompositionLocalProvider(LocalContentColor provides NeonScheme.onBackground) {
                content()
            }
        }
    }
}
