package com.fangjet.weather.worker

import androidx.work.ListenableWorker.Result
import com.fangjet.weather.domain.CurrentWeather
import com.fangjet.weather.domain.DayForecast
import com.fangjet.weather.domain.PetWeatherThresholds
import com.fangjet.weather.domain.SavedLocation
import com.fangjet.weather.domain.WeatherUnits
import com.fangjet.weather.ui.pets.PetMood
import com.fangjet.weather.ui.pets.petMoodFor
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

/**
 * Pure logic extracted from [WeatherNotificationWorker] so it can be unit-tested
 * without Android framework dependencies (Context, NotificationManager, etc.).
 */
internal class NotificationWorkerLogic(
    private val getSavedLocations: suspend () -> List<SavedLocation>,
    private val getActiveIndex: suspend () -> Int,
    private val getUnits: suspend () -> WeatherUnits,
    private val fetchCurrentWeather: suspend (
        units: WeatherUnits,
        label: String,
        latitude: Double?,
        longitude: Double?,
    ) -> kotlin.Result<CurrentWeather>,
    private val fetchForecast: suspend (
        units: WeatherUnits,
        label: String,
        latitude: Double?,
        longitude: Double?,
    ) -> kotlin.Result<List<DayForecast>>,
    private val hasNotificationPermission: () -> Boolean,
    private val postNotification: (WeatherNotification) -> Unit,
    // Remote Config daily_weather_notifications_enabled. Only gates posting: the fetch still runs.
    private val isDailyWeatherNotificationEnabled: () -> Boolean = { true },
    // Remote Config pet-mood thresholds, for the pets picture: the same mood as the app's top card.
    private val petWeatherThresholds: () -> PetWeatherThresholds = { PetWeatherThresholds.DEFAULT },
    // Same label format as ForecastAggregator produces, in the city's local date (its UTC offset)
    // so it matches the city-local forecast labels. Overridable so tests are date-independent.
    private val todayLabel: (utcOffsetSeconds: Int) -> String = { offset ->
        OffsetDateTime.now(ZoneOffset.ofTotalSeconds(offset)).format(DAY_LABEL_FORMATTER)
    },
) {
    suspend fun doWork(): Result {
        val locations = getSavedLocations()
        if (locations.isEmpty()) return Result.success()

        val activeIndex = getActiveIndex()
        val location = locations.getOrNull(activeIndex) ?: return Result.success()

        val units = getUnits()

        val weather = fetchCurrentWeather(
            units,
            // "My Location" has no city name of its own: an empty label lets the API's name for
            // the place show ("57° in Mountain View"), as on the app's own screen.
            if (location.isCurrentLocation) "" else location.label,
            location.latitude,
            location.longitude,
        ).getOrNull() ?: return Result.retry()

        val forecast = fetchForecast(
            units,
            location.label,
            location.latitude,
            location.longitude,
        ).getOrNull()
        // /forecast only returns upcoming 3-hour slots, so once today's last slot has passed the
        // first day is tomorrow. Only an entry that is actually today may supply the high/low.
        val todayForecast = forecast?.firstOrNull { it.dateLabel == todayLabel(weather.offsetSeconds) }

        if (!hasNotificationPermission()) return Result.success()
        if (!runCatching { isDailyWeatherNotificationEnabled() }.getOrDefault(true)) return Result.success()

        val (title, body) = buildContent(weather, todayForecast)
        val mood = petMoodFor(
            weather.conditionMain,
            weather.iconCode,
            weather.units.toCelsius(weather.feelsLike),
            runCatching { petWeatherThresholds() }.getOrDefault(PetWeatherThresholds.DEFAULT),
        )
        postNotification(WeatherNotification(title, body, mood, southernHemisphere = (location.latitude ?: 0.0) < 0))
        return Result.success()
    }

    private companion object {
        val DAY_LABEL_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE, MMM d")
    }
}

/** A daily weather notification: its text, and the pets' mood for its picture. */
internal data class WeatherNotification(
    val title: String,
    val body: String,
    val mood: PetMood,
    val southernHemisphere: Boolean,
)

internal fun buildNotificationContent(
    weather: CurrentWeather,
    todayForecast: DayForecast?,
): Pair<String, String> {
    val tempHigh = (todayForecast?.tempMax ?: weather.tempMax).roundToInt()
    val tempLow = (todayForecast?.tempMin ?: weather.tempMin).roundToInt()
    val description = weather.description
        .split(" ")
        .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
    val title = "${weather.temperature.roundToInt()}° in ${weather.cityName}"
    val body = "$tempHigh°/$tempLow° • $description"
    return title to body
}

private fun buildContent(
    weather: CurrentWeather,
    todayForecast: DayForecast?,
) = buildNotificationContent(weather, todayForecast)
