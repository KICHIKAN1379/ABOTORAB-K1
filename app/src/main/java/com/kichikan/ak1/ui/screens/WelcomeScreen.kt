package com.kichikan.ak1.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.kichikan.ak1.domain.model.*
import androidx.compose.foundation.Canvas
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.kichikan.ak1.ui.theme.Button
import com.kichikan.ak1.ui.theme.Neon
import com.kichikan.ak1.data.AppRepository

/**
 * First-run flow: a welcome screen, then a short setup step that names the ring.
 *
 * It reports success through [onReady] instead of mutating a counter, because the caller's
 * early return used to mean the counter was never read, so the first tap looked like it did
 * nothing and the second one crashed on `check(ring == null)`.
 */
@Composable
internal fun WelcomeScreen(repo: AppRepository, onReady: () -> Unit) {
    var step by remember { mutableIntStateOf(0) }
    var ringName by remember { mutableStateOf("ابوتراب") }
    var username by remember { mutableStateOf("مربی") }
    var error by remember { mutableStateOf<String?>(null) }

    Box(Modifier.fillMaxSize()) {
        WelcomeBackdrop()
        Column(
            Modifier.fillMaxSize().padding(horizontal = 28.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (step == 0) {
                Surface(
                    shape = androidx.compose.foundation.shape.CircleShape,
                    color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.06f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Neon.Cyan.copy(alpha = 0.55f)),
                    modifier = Modifier.size(112.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("K1", style = MaterialTheme.typography.headlineLarge, color = Neon.Cyan)
                    }
                }
                Spacer(Modifier.height(28.dp))
                Text(
                    "خوش آمدید",
                    style = MaterialTheme.typography.displaySmall,
                    color = androidx.compose.ui.graphics.Color.White
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    "مجموعه فرهنگی ابوتراب علیه السلام",
                    style = MaterialTheme.typography.titleMedium,
                    color = Neon.Gold,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(Modifier.height(44.dp))
                Button(
                    onClick = { step = 1 },
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    Text("ورود", style = MaterialTheme.typography.titleLarge)
                }
            } else {
                Text("مشخصات حلقه", style = MaterialTheme.typography.headlineSmall, color = androidx.compose.ui.graphics.Color.White)
                Spacer(Modifier.height(6.dp))
                Text(
                    "این اطلاعات فقط روی همین گوشی ذخیره می‌شود.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.7f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(Modifier.height(20.dp))
                OutlinedTextField(ringName, { ringName = it }, label = { Text("نام حلقه") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(username, { username = it }, label = { Text("نام کاربری محلی مربی") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                error?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, color = MaterialTheme.colorScheme.error)
                }
                Spacer(Modifier.height(22.dp))
                Button(
                    onClick = {
                        try {
                            repo.createRing(ringName, username)
                            onReady()
                        } catch (e: Exception) {
                            error = e.message ?: "شروع کار انجام نشد"
                        }
                    },
                    enabled = ringName.isNotBlank() && username.isNotBlank(),
                    modifier = Modifier.fillMaxWidth().height(54.dp)
                ) {
                    Text("شروع کار آفلاین", style = MaterialTheme.typography.titleMedium)
                }
                TextButton(onClick = { step = 0; error = null }) { Text("→ بازگشت") }
            }
        }
    }
}

/** Soft night-sky background for the welcome step, drawn with the app palette. */
@Composable
private fun WelcomeBackdrop() {
    Canvas(Modifier.fillMaxSize()) {
        drawRect(
            Brush.verticalGradient(
                listOf(
                    androidx.compose.ui.graphics.Color(0xFF02132B),
                    Neon.Navy,
                    androidx.compose.ui.graphics.Color(0xFF07395E)
                )
            )
        )
        val upperGlow = Offset(size.width * 0.82f, size.height * 0.14f)
        drawCircle(
            Brush.radialGradient(
                listOf(Neon.Cyan.copy(alpha = 0.26f), androidx.compose.ui.graphics.Color.Transparent),
                center = upperGlow,
                radius = size.width * 0.85f
            ),
            radius = size.width * 0.85f,
            center = upperGlow
        )
        val lowerGlow = Offset(size.width * 0.12f, size.height * 0.88f)
        drawCircle(
            Brush.radialGradient(
                listOf(Neon.Violet.copy(alpha = 0.22f), androidx.compose.ui.graphics.Color.Transparent),
                center = lowerGlow,
                radius = size.width * 0.75f
            ),
            radius = size.width * 0.75f,
            center = lowerGlow
        )
        val arcCenter = Offset(size.width / 2f, size.height * 0.30f)
        for (index in 1..4) {
            drawCircle(
                color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.055f),
                radius = size.width * (0.30f + index * 0.19f),
                center = arcCenter,
                style = Stroke(1.2f)
            )
        }
    }
}
