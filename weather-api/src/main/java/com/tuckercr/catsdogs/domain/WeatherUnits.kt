package com.tuckercr.catsdogs.domain

import com.tuckercr.catsdogs.data.remote.OpenWeatherApi.Companion.UNITS_IMPERIAL
import com.tuckercr.catsdogs.data.remote.OpenWeatherApi.Companion.UNITS_METRIC
import java.util.Locale

enum class WeatherUnits {
    METRIC,
    IMPERIAL,
    ;

    val units: String
        get() = when (this) {
            METRIC -> UNITS_METRIC
            IMPERIAL -> UNITS_IMPERIAL
        }

    /** Converts a temperature in these units to Celsius. */
    fun toCelsius(value: Double): Double =
        when (this) {
            METRIC -> value
            IMPERIAL -> (value - 32.0) * 5.0 / 9.0
        }

    /** Converts a Celsius temperature to these units. */
    fun fromCelsius(celsius: Double): Double =
        when (this) {
            METRIC -> celsius
            IMPERIAL -> celsius * 9.0 / 5.0 + 32.0
        }

    companion object {
        /**
         * Fallback when regional temperature preference is unavailable (pre-API 34) or not
         * Celsius/Fahrenheit (e.g. Kelvin, default): United States and its territories → imperial;
         * otherwise metric. Uses [Locale.getCountry] from the app’s primary locale.
         */
        fun fromLocale(locale: Locale): WeatherUnits {
            val c = locale.country.uppercase(Locale.ROOT)
            val imperialCountries = setOf("US", "PR", "GU", "VI", "AS", "MP", "UM")
            return if (c in imperialCountries) IMPERIAL else METRIC
        }
    }
}
