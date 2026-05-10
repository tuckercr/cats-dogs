package com.tuckercr.catsdogs.util

import android.content.Context
import android.os.Build
import androidx.core.os.ConfigurationCompat
import androidx.core.text.util.LocalePreferences
import androidx.core.text.util.LocalePreferences.TemperatureUnit
import com.tuckercr.catsdogs.domain.WeatherUnits
import java.util.Locale

/**
 * Resolves [WeatherUnits] for OpenWeather (`metric` vs `imperial`).
 *
 * On API 34+ we usee [LocalePreferences.getTemperatureUnit]
 * Below API 34, we use [WeatherUnits.fromLocale] for a sensible default
 */
fun Context.resolveWeatherUnits(): WeatherUnits {
    val locale: Locale =
        ConfigurationCompat.getLocales(resources.configuration).get(0) ?: Locale.getDefault()

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        when (LocalePreferences.getTemperatureUnit()) {
            TemperatureUnit.CELSIUS -> return WeatherUnits.METRIC
            TemperatureUnit.FAHRENHEIT -> return WeatherUnits.IMPERIAL
            else -> return WeatherUnits.fromLocale(locale)
        }
    }
    return WeatherUnits.fromLocale(locale)
}
