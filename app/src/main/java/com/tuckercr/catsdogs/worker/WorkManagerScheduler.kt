package com.tuckercr.catsdogs.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.tuckercr.catsdogs.data.RemoteConfigRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkManagerScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val remoteConfigRepository: RemoteConfigRepository,
) {
    fun schedule() {
        val intervalMinutes = remoteConfigRepository
            .refreshIntervalMinutes()
            .coerceAtLeast(MIN_INTERVAL_MINUTES)

        val request = PeriodicWorkRequestBuilder<WeatherUpdateWorker>(
            intervalMinutes,
            TimeUnit.MINUTES,
        ).setConstraints(
            Constraints
                .Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build(),
        ).build()

        val wm = WorkManager.getInstance(context)
        wm.enqueueUniquePeriodicWork(
            WORK_NAME,
            // UPDATE replaces any existing schedule if the interval changed.
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
        scheduleNotifications(wm)
    }

    private fun scheduleNotifications(wm: WorkManager) {
        val networkConstraints = Constraints
            .Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()

        NOTIFICATION_HOURS.forEach { hour ->
            val delay = millisUntilNextOccurrenceOf(hour)
            val request = PeriodicWorkRequestBuilder<WeatherNotificationWorker>(
                1,
                TimeUnit.DAYS,
            ).setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .setConstraints(networkConstraints)
                .build()
            wm.enqueueUniquePeriodicWork(
                "weather_notification_${hour}h",
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }
    }

    private fun millisUntilNextOccurrenceOf(hour: Int): Long {
        val now = ZonedDateTime.now()
        var target = now
            .withHour(hour)
            .withMinute(0)
            .withSecond(0)
            .withNano(0)
        if (!target.isAfter(now)) target = target.plusDays(1)
        return target.toInstant().toEpochMilli() - now.toInstant().toEpochMilli()
    }

    companion object {
        private const val WORK_NAME = "weather_background_update"
        private const val MIN_INTERVAL_MINUTES = 15L // WorkManager platform minimum
        private val NOTIFICATION_HOURS = listOf(9, 13, 19) // 9 AM, 1 PM, 7 PM
    }
}
