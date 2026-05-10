package com.tuckercr.catsdogs.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * These are very minimal color schemes, a production app with proper branding
 * would specify more.
 */
private val LightColorScheme = lightColorScheme(
    primary = PrimaryColor,
    background = LightBackground
)

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryColorDark,
    background = DarkBackground
)

@Composable
fun CatsDogsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        content = content,
    )
}
