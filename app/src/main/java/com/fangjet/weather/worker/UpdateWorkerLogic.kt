package com.fangjet.weather.worker

import androidx.work.ListenableWorker.Result
import com.fangjet.weather.domain.CurrentWeather
import com.fangjet.weather.domain.DayForecast
import com.fangjet.weather.domain.SavedLocation
import com.fangjet.weather.domain.WeatherUnits
import com.fangjet.weather.domain.notificationLocation

/**
 * Pure logic extracted from [WeatherUpdateWorker] so it can be unit-tested without Android
 * framework dependencies (Context, DataStore, WorkManager runtime, etc.).
 *
 * The units used for both fetches come from [getUnits], which the worker wires to honor the user's
 * unit override, so the background refresh never writes a different unit system into the cache than
 * a foreground refresh would.
 */
internal class UpdateWorkerLogic(
    private val getSavedLocations: suspend () -> List<SavedLocation>,
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
    private val cacheCurrentWeather: suspend (cacheKey: String, weather: CurrentWeather) -> Unit,
    private val cacheForecast: suspend (cacheKey: String, forecast: List<DayForecast>) -> Unit,
    // After a refresh, with whatever came back: the rain alert checks it.
    private val afterRefresh: suspend (
        location: SavedLocation,
        weather: CurrentWeather?,
        forecast: List<DayForecast>?,
    ) -> Unit = { _, _, _ -> },
) {
    suspend fun doWork(): Result {
        val location = getSavedLocations().notificationLocation() ?: return Result.success()

        val units = getUnits()

        val currentResult = fetchCurrentWeather(
            units,
            location.label,
            location.latitude,
            location.longitude,
        )
        val forecastResult = fetchForecast(
            units,
            location.label,
            location.latitude,
            location.longitude,
        )

        val cacheKey = location.cacheKey

        val weatherOk = currentResult
            .onSuccess { weather -> cacheCurrentWeather(cacheKey, weather) }
            .isSuccess

        val forecastOk = forecastResult
            .onSuccess { forecast -> cacheForecast(cacheKey, forecast) }
            .isSuccess

        afterRefresh(location, currentResult.getOrNull(), forecastResult.getOrNull())
        return if (weatherOk || forecastOk) Result.success() else Result.retry()
    }
}
