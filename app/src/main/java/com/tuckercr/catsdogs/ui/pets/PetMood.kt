package com.tuckercr.catsdogs.ui.pets

import com.tuckercr.catsdogs.domain.PetWeatherThresholds

/** How the cat and dog react on the home screen, derived from the current conditions. */
enum class PetMood { SUNNY, CLOUDY, RAIN, STORM, SNOW, HOT, COLD, NIGHT }

/**
 * Picks the pet mood. Precipitation and storms win over temperature, temperature extremes win over
 * time of day, and an OpenWeatherMap icon ending in "n" means night. The hot / cold cut-offs come
 * from [thresholds] (Remote Config `pet_hot_threshold_c` / `pet_cold_threshold_c`).
 */
fun petMoodFor(
    conditionMain: String,
    iconCode: String,
    temperatureC: Double,
    thresholds: PetWeatherThresholds = PetWeatherThresholds.DEFAULT,
): PetMood =
    when (conditionMain.lowercase()) {
        "thunderstorm" -> PetMood.STORM
        "snow" -> PetMood.SNOW
        "rain", "drizzle" -> PetMood.RAIN
        else -> when {
            temperatureC >= thresholds.petHotC -> PetMood.HOT
            temperatureC <= thresholds.petColdC -> PetMood.COLD
            iconCode.endsWith("n") -> PetMood.NIGHT
            conditionMain.equals("clear", ignoreCase = true) -> PetMood.SUNNY
            else -> PetMood.CLOUDY
        }
    }
