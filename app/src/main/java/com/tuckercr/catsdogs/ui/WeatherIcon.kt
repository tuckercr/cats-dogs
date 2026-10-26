package com.tuckercr.catsdogs.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.tuckercr.catsdogs.R

/**
 * A weather condition icon, bundled with the app and drawn in the pet scene's style. Every shape
 * has the pets' navy outline, so the white clouds hold up on the light cards, and the fills carry
 * them on the dark ones.
 */
@Composable
fun WeatherIcon(
    iconCode: String,
    contentDescription: String,
    sizeDp: Int,
    modifier: Modifier = Modifier,
) {
    Image(
        painter = painterResource(weatherIconRes(iconCode)),
        contentDescription = contentDescription,
        modifier = modifier.size(sizeDp.dp),
    )
}

/**
 * Maps an OpenWeatherMap icon code ("01d", "10n"…; Open-Meteo's WMO codes are mapped onto the same
 * set) to its drawable. Only clear, few clouds and rain look different at night; the rest share one.
 */
@DrawableRes
internal fun weatherIconRes(iconCode: String): Int {
    val night = iconCode.endsWith("n")
    return when (iconCode.take(2)) {
        "01" -> if (night) R.drawable.wx_01n else R.drawable.wx_01d
        "02" -> if (night) R.drawable.wx_02n else R.drawable.wx_02d
        "03" -> R.drawable.wx_03d
        "04" -> R.drawable.wx_04d
        "09" -> R.drawable.wx_09d
        "10" -> if (night) R.drawable.wx_10n else R.drawable.wx_10d
        "11" -> R.drawable.wx_11d
        "13" -> R.drawable.wx_13d
        "50" -> R.drawable.wx_50d
        else -> R.drawable.wx_03d
    }
}
