package com.tuckercr.catsdogs.util

import android.content.Context
import android.icu.util.LocaleData
import android.icu.util.ULocale
import android.os.Build
import androidx.core.os.ConfigurationCompat
import androidx.core.text.util.LocalePreferences
import androidx.core.text.util.LocalePreferences.TemperatureUnit
import com.tuckercr.catsdogs.domain.UnitOverride
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

    val temperatureUnit = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        LocalePreferences.getTemperatureUnit()
    } else {
        null
    }
    return resolveWeatherUnits(locale, temperatureUnit)
}

fun Context.resolveWeatherUnits(override: UnitOverride): WeatherUnits =
    when (override) {
        UnitOverride.METRIC -> WeatherUnits.METRIC
        UnitOverride.IMPERIAL -> WeatherUnits.IMPERIAL
        UnitOverride.SYSTEM -> resolveWeatherUnits()
    }

internal fun resolveWeatherUnits(
    locale: Locale,
    temperatureUnit: String?,
): WeatherUnits =
    when (temperatureUnit) {
        TemperatureUnit.CELSIUS -> WeatherUnits.METRIC
        TemperatureUnit.FAHRENHEIT -> WeatherUnits.IMPERIAL
        else -> WeatherUnits.fromLocale(locale)
    }

/**
 * Whether distances (visibility) read in miles: the app's unit choice, or for "Use system setting"
 * the system's measurement system, where the US and UK use miles.
 */
fun Context.usesMilesForDistance(override: UnitOverride): Boolean =
    usesMilesForDistance(override) {
        val locale = ConfigurationCompat.getLocales(resources.configuration).get(0) ?: Locale.getDefault()
        measurementSystemUsesMiles(locale)
    }

internal fun usesMilesForDistance(
    override: UnitOverride,
    systemUsesMiles: () -> Boolean,
): Boolean =
    when (override) {
        UnitOverride.METRIC -> false
        UnitOverride.IMPERIAL -> true
        UnitOverride.SYSTEM -> systemUsesMiles()
    }

private fun measurementSystemUsesMiles(locale: Locale): Boolean =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        val system = LocaleData.getMeasurementSystem(ULocale.forLocale(locale))
        system == LocaleData.MeasurementSystem.US || system == LocaleData.MeasurementSystem.UK
    } else {
        // No measurement-system API before 28: the same country rule as the temperature units.
        WeatherUnits.fromLocale(locale) == WeatherUnits.IMPERIAL
    }
