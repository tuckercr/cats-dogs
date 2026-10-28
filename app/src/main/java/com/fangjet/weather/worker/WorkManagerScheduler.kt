package com.fangjet.weather.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.fangjet.weather.data.RemoteConfigRepository
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

    /**
     * The 8:30 AM run (briefing, and planning the walk-time nudge), daily. Rain alerts ride on the
     * background refresh instead, which runs every half hour or so.
     */
    private fun scheduleNotifications(wm: WorkManager) {
        // The old 9 AM / 1 PM / 7 PM briefings.
        LEGACY_NOTIFICATION_HOURS.forEach { wm.cancelUniqueWork("weather_notification_${it}h") }
        val request = PeriodicWorkRequestBuilder<WeatherNotificationWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(millisUntilNext(BRIEFING_HOUR, BRIEFING_MINUTE), TimeUnit.MILLISECONDS)
            .setConstraints(networkConstraints())
            .build()
        wm.enqueueUniquePeriodicWork(MORNING_WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    /** The walk-time nudge at [epochSeconds]; replaces any already planned. */
    fun scheduleWalkTime(epochSeconds: Long) {
        val delay = (epochSeconds * 1000 - System.currentTimeMillis()).coerceAtLeast(0)
        val request = OneTimeWorkRequestBuilder<WalkTimeWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setConstraints(networkConstraints())
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(WALK_WORK_NAME, ExistingWorkPolicy.REPLACE, request)
    }

    private fun networkConstraints() =
        Constraints
            .Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

    private fun millisUntilNext(
        hour: Int,
        minute: Int,
    ): Long {
        val now = ZonedDateTime.now()
        var target = now
            .withHour(hour)
            .withMinute(minute)
            .withSecond(0)
            .withNano(0)
        if (!target.isAfter(now)) target = target.plusDays(1)
        return target.toInstant().toEpochMilli() - now.toInstant().toEpochMilli()
    }

    companion object {
        private const val WORK_NAME = "weather_background_update"
        private const val MIN_INTERVAL_MINUTES = 15L // WorkManager platform minimum
        private const val MORNING_WORK_NAME = "notification_morning"
        private const val WALK_WORK_NAME = "notification_walk_time"
        private const val BRIEFING_HOUR = 8
        private const val BRIEFING_MINUTE = 30
        private val LEGACY_NOTIFICATION_HOURS = listOf(9, 13, 19)
    }
}
