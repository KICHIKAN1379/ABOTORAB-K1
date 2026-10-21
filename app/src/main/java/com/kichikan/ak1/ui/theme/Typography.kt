package com.kichikan.ak1.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.kichikan.ak1.R

/**
 * Persian-first type system.
 *
 * The platform default (Roboto) has no proper Persian shaping/line metrics, so every
 * text style is built on Vazirmatn with extra line height: Persian ascenders,
 * descenders and diacritics need more room than Latin text at the same size.
 * Letter spacing is forced to 0 because tracking breaks Arabic-script joining.
 */
val Vazirmatn = FontFamily(
    Font(R.font.vazirmatn_regular, FontWeight.Normal),
    Font(R.font.vazirmatn_medium, FontWeight.Medium),
    Font(R.font.vazirmatn_semibold, FontWeight.SemiBold),
    Font(R.font.vazirmatn_bold, FontWeight.Bold)
)

private fun ak1Style(size: Int, lineHeight: Int, weight: FontWeight) = TextStyle(
    fontFamily = Vazirmatn,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = 0.sp
)

val AK1Typography = Typography(
    displayLarge = ak1Style(57, 70, FontWeight.Bold),
    displayMedium = ak1Style(45, 58, FontWeight.Bold),
    displaySmall = ak1Style(36, 48, FontWeight.SemiBold),
    headlineLarge = ak1Style(32, 44, FontWeight.Bold),
    headlineMedium = ak1Style(28, 40, FontWeight.SemiBold),
    headlineSmall = ak1Style(24, 36, FontWeight.SemiBold),
    titleLarge = ak1Style(22, 32, FontWeight.SemiBold),
    titleMedium = ak1Style(16, 26, FontWeight.Medium),
    titleSmall = ak1Style(14, 24, FontWeight.Medium),
    bodyLarge = ak1Style(16, 28, FontWeight.Normal),
    bodyMedium = ak1Style(14, 24, FontWeight.Normal),
    bodySmall = ak1Style(12, 20, FontWeight.Normal),
    labelLarge = ak1Style(14, 22, FontWeight.Medium),
    labelMedium = ak1Style(12, 18, FontWeight.Medium),
    labelSmall = ak1Style(11, 16, FontWeight.Medium)
)
