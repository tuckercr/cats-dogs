@file:OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)

package com.tuckercr.catsdogs.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.tuckercr.catsdogs.R

// Rounded display face that echoes the "Cats & Dogs" wordmark. Bundled variable font (weights
// selected via FontVariation, minSdk 26+). Used for headings only; body stays on the default face.
private val Baloo2 = FontFamily(
    Font(
        R.font.baloo2,
        weight = FontWeight.Medium,
        variationSettings = FontVariation.Settings(FontVariation.weight(500)),
    ),
    Font(
        R.font.baloo2,
        weight = FontWeight.SemiBold,
        variationSettings = FontVariation.Settings(FontVariation.weight(600)),
    ),
    Font(
        R.font.baloo2,
        weight = FontWeight.Bold,
        variationSettings = FontVariation.Settings(FontVariation.weight(700)),
    ),
)

private val default = Typography()

val Typography = Typography(
    displayLarge = default.displayLarge.copy(fontFamily = Baloo2),
    displayMedium = default.displayMedium.copy(fontFamily = Baloo2),
    displaySmall = default.displaySmall.copy(fontFamily = Baloo2),
    headlineLarge = default.headlineLarge.copy(fontFamily = Baloo2),
    headlineMedium = default.headlineMedium.copy(fontFamily = Baloo2),
    headlineSmall = default.headlineSmall.copy(fontFamily = Baloo2),
    titleLarge = default.titleLarge.copy(fontFamily = Baloo2),
    titleMedium = default.titleMedium.copy(fontFamily = Baloo2),
    titleSmall = default.titleSmall.copy(fontFamily = Baloo2),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp,
    ),
)
