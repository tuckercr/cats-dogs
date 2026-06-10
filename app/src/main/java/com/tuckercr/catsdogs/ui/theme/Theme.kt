package com.tuckercr.catsdogs.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = SkyBlue,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    primaryContainer = SkyBlueLight,
    onPrimaryContainer = SkyBlueDark,
    secondary = TealAccent,
    background = SurfaceLight,
    surface = SurfaceLight,
    surfaceVariant = SkyBlueLight,
)

private val DarkColorScheme = darkColorScheme(
    primary = SkyBlueNight,
    onPrimary = SurfaceDark,
    primaryContainer = SurfaceContainerDark,
    onPrimaryContainer = SkyBlueNight,
    secondary = TealAccentNight,
    background = SurfaceDark,
    surface = SurfaceDark,
    surfaceVariant = SurfaceContainerDark,
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
