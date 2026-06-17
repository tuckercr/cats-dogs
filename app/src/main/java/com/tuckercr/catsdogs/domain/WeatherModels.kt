package com.tuckercr.catsdogs.domain

data class CurrentWeather(
    /** Shown in the UI: matches what the user searched (when provided), not necessarily OpenWeather’s `name`. */
    val cityName: String,
    val conditionMain: String,
    val description: String,
    val iconCode: String,
    val temperature: Double,
    val feelsLike: Double,
    val tempMin: Double,
    val tempMax: Double,
    val humidityPercent: Int,
    val pressureHpa: Int,
    val windSpeed: Double,
    val windDeg: Int,
    val visibilityMeters: Int?,
    val cloudPercent: Int,
    val units: WeatherUnits,
)

/** A single 3-hour forecast slot used in the day-detail hourly breakdown. */
data class HourlySlot(
    val timeLabel: String,
    val iconCode: String,
    val description: String,
    val temperature: Double,
    val feelsLike: Double,
    val windSpeed: Double,
    val windDeg: Int,
    val humidity: Int,
    val pressure: Int,
    val units: WeatherUnits,
)

data class DayForecast(
    val dateLabel: String,
    val conditionMain: String,
    val description: String,
    val iconCode: String,
    val temperature: Double,
    val feelsLike: Double,
    val tempMin: Double,
    val tempMax: Double,
    val units: WeatherUnits,
    /** All 3-hour slots for this calendar day, ordered chronologically. */
    val hourlySlots: List<HourlySlot> = emptyList(),
)
