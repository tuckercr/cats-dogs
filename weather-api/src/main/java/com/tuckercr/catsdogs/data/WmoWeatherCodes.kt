package com.tuckercr.catsdogs.data

/**
 * Maps Open-Meteo's WMO weather codes onto the OpenWeatherMap-style condition, description, and
 * icon code the rest of the app (icons, pet moods) already understands.
 * Reference: https://open-meteo.com/en/docs (WMO Weather interpretation codes).
 */
internal object WmoWeatherCodes {

    data class Condition(
        val main: String,
        val description: String,
        val iconCode: String,
    )

    fun condition(
        code: Int,
        isDay: Boolean,
    ): Condition {
        val (main, description, icon) = when (code) {
            0 -> Triple("Clear", "clear sky", "01")
            1 -> Triple("Clouds", "mainly clear", "02")
            2 -> Triple("Clouds", "partly cloudy", "03")
            3 -> Triple("Clouds", "overcast", "04")
            45, 48 -> Triple("Fog", "fog", "50")
            51, 53, 55 -> Triple("Drizzle", "drizzle", "09")
            56, 57 -> Triple("Drizzle", "freezing drizzle", "09")
            61 -> Triple("Rain", "light rain", "10")
            63 -> Triple("Rain", "moderate rain", "10")
            65 -> Triple("Rain", "heavy rain", "10")
            66, 67 -> Triple("Rain", "freezing rain", "13")
            71 -> Triple("Snow", "light snow", "13")
            73 -> Triple("Snow", "moderate snow", "13")
            75 -> Triple("Snow", "heavy snow", "13")
            77 -> Triple("Snow", "snow grains", "13")
            80, 81 -> Triple("Rain", "rain showers", "09")
            82 -> Triple("Rain", "violent rain showers", "09")
            85, 86 -> Triple("Snow", "snow showers", "13")
            95 -> Triple("Thunderstorm", "thunderstorm", "11")
            96, 99 -> Triple("Thunderstorm", "thunderstorm with hail", "11")
            else -> Triple("Clouds", "cloudy", "03")
        }
        return Condition(main, description, icon + if (isDay) "d" else "n")
    }
}
