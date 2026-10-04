@file:OptIn(kotlinx.serialization.InternalSerializationApi::class)

package com.tuckercr.catsdogs.domain

import kotlinx.serialization.Serializable

@Serializable
data class CurrentWeather(
    /** Shown in the UI: matches what the user searched (when provided), not necessarily OpenWeather's `name`. */
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
    val sunriseEpoch: Long? = null,
    val sunsetEpoch: Long? = null,
    /**
     * The location's offset from UTC, in seconds; null in data cached before it was recorded.
     * Read [offsetSeconds] instead, which falls back sensibly.
     */
    val utcOffsetSeconds: Int? = null,
) {
    /**
     * Offset for rendering the location's local time. Unknown offsets use the device's own zone,
     * which is right for "My Location" and closer than UTC for most saved cities.
     */
    val offsetSeconds: Int
        get() = utcOffsetSeconds
            ?: java.time.ZoneId
                .systemDefault()
                .rules
                .getOffset(java.time.Instant.now())
                .totalSeconds
}

/** A single 3-hour forecast slot used in the day-detail hourly breakdown. */
@Serializable
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
    /** Probability of precipitation as a percentage, 0..100. */
    val precipitationChance: Int = 0,
    /** UV index (0..11+); null when the source doesn't provide it. */
    val uvIndex: Double? = null,
    /** Estimated sun-baked pavement temperature in [units]; null when unknown. */
    val pavementTemperature: Double? = null,
    /** Hour of day (0..23) in the location's own timezone; null in data cached before it existed. */
    val localHour: Int? = null,
)

@Serializable
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
    /** Highest probability of precipitation across the day's slots, as a percentage, 0..100. */
    val precipitationChance: Int = 0,
    /** Peak UV index across the day's slots; null when the source doesn't provide UV. */
    val uvIndexMax: Double? = null,
)
