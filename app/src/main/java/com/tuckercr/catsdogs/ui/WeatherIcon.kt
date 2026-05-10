package com.tuckercr.catsdogs.ui

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest

@Composable
fun WeatherIcon(
    iconCode: String,
    contentDescription: String,
    sizeDp: Int,
    modifier: Modifier = Modifier
) {
    // These aren't very pretty but they do the job
    val url = "https://openweathermap.org/img/wn/${iconCode}@2x.png"
    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(url)
            .crossfade(true)
            .build(),
        contentDescription = contentDescription,
        modifier = modifier.size(sizeDp.dp),
        contentScale = ContentScale.Fit,
    )
}
