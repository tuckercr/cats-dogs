package com.fangjet.weather.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.fangjet.weather.data.PreferencesRepository
import com.fangjet.weather.data.RemoteConfigRepository
import com.fangjet.weather.data.WeatherRepository
import com.fangjet.weather.util.resolveWeatherUnits
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

/** Runs once at the walk time the morning run picked, and nudges if it's still a good time. */
@HiltWorker
class WalkTimeWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    preferencesRepository: PreferencesRepository,
    weatherRepository: WeatherRepository,
    remoteConfigRepository: RemoteConfigRepository,
) : CoroutineWorker(context, workerParams) {

    private val logic = WalkTimeLogic(
        getSavedLocations = { preferencesRepository.savedLocations.first() },
        getUnits = {
            val override = preferencesRepository.unitOverride.first()
            context.resolveWeatherUnits(override)
        },
        fetchCurrentWeather = { units, label, lat, lon ->
            if (lat != null && lon != null) {
                weatherRepository.fetchCurrentWeather(units, label, latitude = lat, longitude = lon)
            } else {
                weatherRepository.fetchCurrentWeather(units, label, cityQuery = label)
            }
        },
        fetchForecast = { units, label, lat, lon ->
            if (lat != null && lon != null) {
                weatherRepository.fetchForecast(units, latitude = lat, longitude = lon)
            } else {
                weatherRepository.fetchForecast(units, cityQuery = label)
            }
        },
        hasNotificationPermission = context::canPostNotifications,
        postNotification = context::postWeatherNotification,
        isWalkTimeEnabled = { preferencesRepository.notifyWalkTime.first() },
        petWeatherThresholds = remoteConfigRepository::petWeatherThresholds,
    )

    override suspend fun doWork() = logic.doWork()
}
