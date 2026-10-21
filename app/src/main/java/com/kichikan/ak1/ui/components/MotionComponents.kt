package com.kichikan.ak1.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import com.kichikan.ak1.ui.theme.Motion
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/**
 * Counts up to [value] instead of jumping to it.
 *
 * Used for the ring summary (members, XP, points, diamonds) so a refresh reads as a
 * deliberate update rather than a silent text swap.
 */
@Composable
fun AnimatedCounter(
    value: Int,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.headlineSmall,
    color: Color = MaterialTheme.colorScheme.primary,
    durationMillis: Int = 650
) {
    val animated = remember { Animatable(0f) }
    LaunchedEffect(value) {
        animated.animateTo(
            targetValue = value.toFloat(),
            animationSpec = tween(durationMillis = durationMillis, easing = Motion.Decelerate)
        )
    }
    Text(
        text = animated.value.roundToInt().toString(),
        modifier = modifier,
        style = style,
        color = color,
        maxLines = 1
    )
}

/**
 * Fades/slides one block in after [index] * [stepMillis].
 *
 * Wrapping individual rows (not the whole screen) keeps the existing Column spacing
 * intact while giving the dashboard a soft top-to-bottom entrance.
 */
@Composable
fun StaggeredItem(
    index: Int,
    stepMillis: Long = 45L,
    content: @Composable () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(index.coerceAtLeast(0) * stepMillis)
        visible = true
    }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(Motion.Medium, easing = Motion.Standard)) +
            slideInVertically(tween(Motion.Slow, easing = Motion.Emphasized)) { height -> height / 8 }
    ) {
        Column { content() }
    }
}
