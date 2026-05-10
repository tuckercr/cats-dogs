package com.tuckercr.catsdogs.domain

import com.tuckercr.catsdogs.data.remote.OpenWeatherApi.Companion.UNITS_IMPERIAL
import com.tuckercr.catsdogs.data.remote.OpenWeatherApi.Companion.UNITS_METRIC
import java.util.Locale

enum class WeatherUnits {
    METRIC, IMPERIAL;

    val units: String
        get() = when (this) {
            METRIC -> UNITS_METRIC
            IMPERIAL -> UNITS_IMPERIAL
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
