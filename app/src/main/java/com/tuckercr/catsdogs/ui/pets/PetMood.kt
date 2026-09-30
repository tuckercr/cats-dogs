package com.tuckercr.catsdogs.ui.pets

import com.tuckercr.catsdogs.domain.WeatherUnits

/** How the cat and dog react on the home screen, derived from the current conditions. */
enum class PetMood { SUNNY, CLOUDY, RAIN, STORM, SNOW, HOT, COLD, NIGHT }

internal const val HOT_THRESHOLD_C = 30.0
internal const val COLD_THRESHOLD_C = 0.0

internal fun toCelsius(
    value: Double,
    units: WeatherUnits,
): Double =
    when (units) {
        WeatherUnits.METRIC -> value
        WeatherUnits.IMPERIAL -> (value - 32.0) * 5.0 / 9.0
    }

/**
 * Picks the pet mood. Precipitation and storms win over temperature, temperature extremes win over
 * time of day, and an OpenWeatherMap icon ending in "n" means night.
 */
fun petMoodFor(
    conditionMain: String,
    iconCode: String,
    temperatureC: Double,
): PetMood =
    when (conditionMain.lowercase()) {
        "thunderstorm" -> PetMood.STORM
        "snow" -> PetMood.SNOW
        "rain", "drizzle" -> PetMood.RAIN
        else -> when {
            temperatureC >= HOT_THRESHOLD_C -> PetMood.HOT
            temperatureC <= COLD_THRESHOLD_C -> PetMood.COLD
            iconCode.endsWith("n") -> PetMood.NIGHT
            conditionMain.equals("clear", ignoreCase = true) -> PetMood.SUNNY
            else -> PetMood.CLOUDY
        }
    }
