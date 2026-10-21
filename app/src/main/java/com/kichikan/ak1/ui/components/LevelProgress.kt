package com.kichikan.ak1.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.kichikan.ak1.domain.model.LevelRules
import com.kichikan.ak1.ui.theme.Motion
import com.kichikan.ak1.ui.theme.Neon

/**
 * XP bar for one member.
 *
 * Uses [LevelRules] so the bar and the level badge can never disagree: level 0 needs
 * 20 XP, every later level needs 30. The fill animates from its previous value, which
 * makes awarding points feel like progress instead of a number swap.
 */
@Composable
fun LevelProgress(
    xp: Int,
    level: Int,
    modifier: Modifier = Modifier
) {
    val start = LevelRules.xpForLevel(level)
    val next = LevelRules.xpForLevel(level + 1)
    val span = (next - start).coerceAtLeast(1)
    val fraction = ((xp - start).toFloat() / span).coerceIn(0f, 1f)
    val fill by animateFloatAsState(
        targetValue = fraction,
        animationSpec = tween(900, easing = Motion.Decelerate),
        label = "xp-progress"
    )
    val remaining = (next - xp).coerceAtLeast(0)
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(50))
                .background(Color.White.copy(alpha = 0.10f))
        ) {
            Box(
                Modifier
                    .fillMaxWidth(fill)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(50))
                    .background(Brush.horizontalGradient(listOf(Neon.Cyan, Neon.Sky, Neon.Violet)))
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                "XP $xp / $next",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                if (remaining == 0) "آمادهٔ سطح بعد" else "تا سطح بعد $remaining XP",
                style = MaterialTheme.typography.labelSmall,
                color = Neon.Cyan
            )
        }
    }
}
