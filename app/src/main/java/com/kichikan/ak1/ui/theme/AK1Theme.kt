package com.kichikan.ak1.ui.theme

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * AK1 visual system, built from the "4 icon styles" sheet:
 *  - Style 2 (neon gradient)  -> theme, background and buttons (this file + [Button]).
 *  - Style 1 (minimal vector) -> bottom-menu icons, now the hand-illustrated PNG set ([AppIcon]).
 *  - Style 3 (clay 3D)        -> service icons, now the hand-illustrated PNG set ([AppIcon]).
 *  - Style 4 (layered glass)  -> cards, panels and metric icons ([Card], [GlassIcon]).
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
    // Slow aurora drift behind every screen: the gradient never sits still, but the
    // movement is gentle enough not to pull attention away from the content.
    val aurora = rememberInfiniteTransition(label = "aurora")
    val drift by aurora.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(14000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "aurora-drift"
    )
    MaterialTheme(
        colorScheme = NeonScheme,
        typography = AK1Typography,
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
                    // Drifting neon glows: three soft light sources whose centres move with
                    // the aurora animation, so the background breathes behind the glass cards.
                    drawRect(
                        Brush.radialGradient(
                            listOf(Neon.Cyan.copy(alpha = 0.20f), Color.Transparent),
                            center = Offset(size.width * (0.94f - 0.10f * drift), size.height * (0.04f + 0.06f * drift)),
                            radius = size.width * 0.95f
                        )
                    )
                    drawRect(
                        Brush.radialGradient(
                            listOf(Neon.Violet.copy(alpha = 0.24f), Color.Transparent),
                            center = Offset(size.width * (0.06f + 0.10f * drift), size.height * (0.94f - 0.06f * drift)),
                            radius = size.width * 1.05f
                        )
                    )
                    drawRect(
                        Brush.radialGradient(
                            listOf(Neon.Sky.copy(alpha = 0.12f), Color.Transparent),
                            center = Offset(size.width * (0.35f + 0.30f * drift), size.height * 0.45f),
                            radius = size.width * 0.65f
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
