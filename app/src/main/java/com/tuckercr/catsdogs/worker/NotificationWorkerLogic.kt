package com.tuckercr.catsdogs.worker

import androidx.work.ListenableWorker.Result
import com.tuckercr.catsdogs.domain.CurrentWeather
import com.tuckercr.catsdogs.domain.DayForecast
import com.tuckercr.catsdogs.domain.SavedLocation
import com.tuckercr.catsdogs.domain.WeatherUnits
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
    private val postNotification: (title: String, body: String) -> Unit,
) {
    suspend fun doWork(): Result {
        val locations = getSavedLocations()
        if (locations.isEmpty()) return Result.success()

        val activeIndex = getActiveIndex()
        val location = locations.getOrNull(activeIndex) ?: return Result.success()

        val units = getUnits()

        val weather = fetchCurrentWeather(
            units,
            location.label,
            location.latitude,
            location.longitude,
        ).getOrNull() ?: return Result.retry()

        val forecast = fetchForecast(
            units,
            location.label,
            location.latitude,
            location.longitude,
        ).getOrNull()
        val todayForecast = forecast?.firstOrNull()

        if (!hasNotificationPermission()) return Result.success()

        val (title, body) = buildContent(weather, todayForecast)
        postNotification(title, body)
        return Result.success()
    }
}

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
