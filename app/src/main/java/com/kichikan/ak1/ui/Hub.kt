package com.kichikan.ak1.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.kichikan.ak1.ui.theme.Card
import com.kichikan.ak1.ui.theme.Neon

/** One entrance card of a hub page (icon + title + short description). */
data class HubEntry(
    val title: String,
    val subtitle: String,
    val accent: Color,
    val icon: @Composable () -> Unit,
    val onClick: () -> Unit
)

/** Top row used by every inner page so the user can always step back to the group page. */
@Composable
fun PageHeader(title: String, onBack: () -> Unit, subtitle: String? = null) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Surface(
            onClick = onBack,
            shape = RoundedCornerShape(50),
            color = Color.White.copy(alpha = 0.06f),
            border = BorderStroke(1.dp, Neon.Cyan.copy(alpha = 0.28f))
        ) {
            Row(
                Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("→", style = MaterialTheme.typography.labelLarge, color = Neon.Cyan)
                Text("بازگشت", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface)
            }
        }
        Text(title, style = MaterialTheme.typography.headlineMedium)
        if (!subtitle.isNullOrBlank()) Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** A page made only of entrance cards: used for "خدمات" and for each service group. */
@Composable
fun HubScreen(
    padding: PaddingValues,
    title: String,
    subtitle: String,
    entries: List<HubEntry>,
    onBack: (() -> Unit)? = null
) {
    Column(
        Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (onBack != null) PageHeader(title, onBack, subtitle)
        else {
            Text(title, style = MaterialTheme.typography.headlineMedium)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        entries.chunked(2).forEach { rowItems ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                rowItems.forEach { entry ->
                    Card(
                        onClick = entry.onClick,
                        modifier = Modifier.weight(1f).heightIn(min = 176.dp),
                        shape = RoundedCornerShape(24.dp),
                        accent = entry.accent
                    ) {
                        Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.SpaceBetween) {
                            entry.icon()
                            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                Text(entry.title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                                Text(entry.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text("ورود به بخش  ←", style = MaterialTheme.typography.labelMedium, color = entry.accent)
                        }
                    }
                }
                if (rowItems.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}
