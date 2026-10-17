package com.kichikan.ak1.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CardElevation
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/** Style 2: neon-gradient button (cyan -> sky -> violet) with a soft glow. */
@Composable
fun Button(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(16.dp),
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    content: @Composable RowScope.() -> Unit
) {
    val glow = if (enabled) Modifier.shadow(10.dp, shape, ambientColor = Neon.Cyan.copy(alpha = 0.4f), spotColor = Neon.Cyan.copy(alpha = 0.6f)) else Modifier
    Surface(
        onClick = onClick,
        modifier = modifier.then(glow),
        enabled = enabled,
        shape = shape,
        color = Color.Transparent,
        contentColor = if (enabled) Neon.Navy else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
    ) {
        val fill = if (enabled) Neon.ButtonBrush else Brush.linearGradient(listOf(Color(0x1FFFFFFF), Color(0x1FFFFFFF)))
        Box(
            Modifier.background(fill, shape).defaultMinSize(minHeight = 40.dp).padding(contentPadding),
            contentAlignment = Alignment.Center
        ) {
            ProvideTextStyle(MaterialTheme.typography.labelLarge) {
                Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically, content = content)
            }
        }
    }
}

private fun glassFill(accent: Color?) = Brush.linearGradient(
    listOf(
        (accent ?: Color.White).copy(alpha = if (accent != null) 0.24f else 0.15f),
        Color.White.copy(alpha = 0.05f)
    )
)

private fun glassEdge(accent: Color?) = BorderStroke(
    1.dp,
    Brush.linearGradient(
        listOf(Color.White.copy(alpha = 0.55f), (accent ?: Neon.Cyan).copy(alpha = 0.30f), Color.White.copy(alpha = 0.08f))
    )
)

/** Style 4: layered-glass card (translucent gradient fill, bright glass edge). */
@Composable
fun Card(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(22.dp),
    colors: CardColors = CardDefaults.cardColors(containerColor = Color.Transparent),
    elevation: CardElevation = CardDefaults.cardElevation(),
    border: BorderStroke? = null,
    accent: Color? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    androidx.compose.material3.Card(
        modifier = modifier.background(glassFill(accent), shape),
        shape = shape,
        colors = colors,
        elevation = elevation,
        border = border ?: glassEdge(accent),
        content = content
    )
}

@Composable
fun Card(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(22.dp),
    colors: CardColors = CardDefaults.cardColors(containerColor = Color.Transparent),
    elevation: CardElevation = CardDefaults.cardElevation(),
    border: BorderStroke? = null,
    accent: Color? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    androidx.compose.material3.Card(
        onClick = onClick,
        modifier = modifier.background(glassFill(accent), shape),
        shape = shape,
        colors = colors,
        elevation = elevation,
        border = border ?: glassEdge(accent),
        content = content
    )
}
