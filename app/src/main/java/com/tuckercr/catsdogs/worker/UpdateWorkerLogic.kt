package com.tuckercr.catsdogs.worker

import androidx.work.ListenableWorker.Result
import com.tuckercr.catsdogs.domain.CurrentWeather
import com.tuckercr.catsdogs.domain.DayForecast
import com.tuckercr.catsdogs.domain.SavedLocation
import com.tuckercr.catsdogs.domain.WeatherUnits

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
    private val cacheCurrentWeather: suspend (cacheKey: String, weather: CurrentWeather) -> Unit,
    private val cacheForecast: suspend (cacheKey: String, forecast: List<DayForecast>) -> Unit,
) {
    suspend fun doWork(): Result {
        val locations = getSavedLocations()
        val activeIndex = getActiveIndex()
        val location = locations.getOrNull(activeIndex) ?: return Result.success()

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

        return if (weatherOk || forecastOk) Result.success() else Result.retry()
    }
}
