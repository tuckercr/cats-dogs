package com.tuckercr.catsdogs.worker

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.tuckercr.catsdogs.MainActivity
import com.tuckercr.catsdogs.R
import com.tuckercr.catsdogs.WeatherApplication.Companion.NOTIF_CHANNEL_ID
import com.tuckercr.catsdogs.data.PreferencesRepository
import com.tuckercr.catsdogs.data.WeatherRepository
import com.tuckercr.catsdogs.util.resolveWeatherUnits
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

@HiltWorker
class WeatherNotificationWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    preferencesRepository: PreferencesRepository,
    weatherRepository: WeatherRepository,
) : CoroutineWorker(context, workerParams) {

    private val logic = NotificationWorkerLogic(
        getSavedLocations = { preferencesRepository.savedLocations.first() },
        getActiveIndex = { preferencesRepository.activeLocationIndex.first() },
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
        hasNotificationPermission = {
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS,
                ) == PackageManager.PERMISSION_GRANTED
        },
        postNotification = { title, body -> post(title, body) },
    )

    override suspend fun doWork() = logic.doWork()

    private fun post(
        title: String,
        body: String,
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val tapPendingIntent = PendingIntent.getActivity(
            context,
            RC_TAP,
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat
            .Builder(context, NOTIF_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_weather)
            .setContentTitle(title)
            .setContentText(body)
            .setContentIntent(tapPendingIntent)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(NOTIF_ID, notification)
    }

    companion object {
        const val EXTRA_LOCATION_INDEX = "extra_location_index"
        private const val NOTIF_ID = 1000
        private const val RC_TAP = 0
    }
}
