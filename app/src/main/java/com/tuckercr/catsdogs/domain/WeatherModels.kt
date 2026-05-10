package com.tuckercr.catsdogs.domain

data class CurrentWeather(
    /** Shown in the UI: matches what the user searched (when provided), not necessarily OpenWeather’s `name`. */
    val cityName: String,
    val conditionMain: String,
    val description: String,
    val iconCode: String,
    val temperature: Double,
    val feelsLike: Double,
    val humidityPercent: Int,
    val windSpeed: Double,
    val units: WeatherUnits,
)

data class DayForecast(
    val dateLabel: String,
    val conditionMain: String,
    val description: String,
    val iconCode: String,
    val temperature: Double,
    val feelsLike: Double,
    val units: WeatherUnits,
)
