package com.tuckercr.catsdogs.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.tuckercr.catsdogs.ui.theme.SkyBlueSurface

@Composable
fun WeatherIcon(
    iconCode: String,
    contentDescription: String,
    sizeDp: Int,
    modifier: Modifier = Modifier,
) {
    // These aren't very pretty but they do the job
    val url = "https://openweathermap.org/img/wn/$iconCode@2x.png"
    // OpenWeather's night icons are a dark grey disc (1.5:1 on the dark theme's cards), so on a dark
    // surface each icon sits on the light theme's card colour, where every icon reads clearly.
    val backdrop = if (MaterialTheme.colorScheme.surface.luminance() < 0.5f) {
        Modifier.background(SkyBlueSurface, CircleShape)
    } else {
        Modifier
    }
    AsyncImage(
        model = ImageRequest
            .Builder(LocalContext.current)
            .data(url)
            .crossfade(true)
            .build(),
        contentDescription = contentDescription,
        modifier = modifier.size(sizeDp.dp).then(backdrop),
        contentScale = ContentScale.Fit,
    )
}
