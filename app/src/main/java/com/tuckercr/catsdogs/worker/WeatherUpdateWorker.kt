package com.tuckercr.catsdogs.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.tuckercr.catsdogs.data.PreferencesRepository
import com.tuckercr.catsdogs.data.WeatherRepository
import com.tuckercr.catsdogs.domain.CurrentWeather
import com.tuckercr.catsdogs.domain.DayForecast
import com.tuckercr.catsdogs.util.resolveWeatherUnits
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@HiltWorker
class WeatherUpdateWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val preferencesRepository: PreferencesRepository,
    private val weatherRepository: WeatherRepository,
    private val json: Json,
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val locations = preferencesRepository.savedLocations.first()
        val activeIndex = preferencesRepository.activeLocationIndex.first()
        val location = locations.getOrNull(activeIndex) ?: return Result.success()

        val units = context.resolveWeatherUnits()

        val currentResult = if (location.latitude != null && location.longitude != null) {
            weatherRepository.fetchCurrentWeather(
                units = units,
                locationLabel = location.label,
                latitude = location.latitude,
                longitude = location.longitude,
            )
        } else {
            weatherRepository.fetchCurrentWeather(
                units = units,
                locationLabel = location.label,
                cityQuery = location.label,
            )
        }

        val forecastResult = if (location.latitude != null && location.longitude != null) {
            weatherRepository.fetchForecast(
                units = units,
                latitude = location.latitude,
                longitude = location.longitude,
            )
        } else {
            weatherRepository.fetchForecast(units = units, cityQuery = location.label)
        }

        val weatherOk = currentResult.onSuccess { weather ->
            preferencesRepository.setCachedCurrentWeather(json.encodeToString<CurrentWeather>(weather))
        }.isSuccess

        val forecastOk = forecastResult.onSuccess { forecast ->
            preferencesRepository.setCachedForecast(json.encodeToString<List<DayForecast>>(forecast))
        }.isSuccess

        return if (weatherOk || forecastOk) Result.success() else Result.retry()
    }
}
