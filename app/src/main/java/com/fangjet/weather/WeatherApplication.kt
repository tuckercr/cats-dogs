package com.fangjet.weather

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import coil.ImageLoader
import com.fangjet.weather.analytics.AnalyticsCollectionController
import com.fangjet.weather.worker.WorkManagerScheduler
import com.google.firebase.Firebase
import com.google.firebase.crashlytics.crashlytics
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class WeatherApplication :
    Application(),
    Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var workManagerScheduler: WorkManagerScheduler

    @Inject
    lateinit var coilImageLoader: ImageLoader

    @Inject
    lateinit var analyticsCollectionController: AnalyticsCollectionController

    /** Lives as long as the process; for app-wide observers such as [analyticsCollectionController]. */
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override val workManagerConfiguration: Configuration
        get() = Configuration
            .Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) Timber.plant(Timber.DebugTree())
        coil.Coil.setImageLoader(coilImageLoader)
        workManagerScheduler.schedule()
        runCatching {
            Firebase.crashlytics.setCrashlyticsCollectionEnabled(!BuildConfig.DEBUG)
        }
        // Applies "Share usage statistics" + the analytics_enabled kill switch to the SDK now,
        // and again whenever either changes.
        runCatching { analyticsCollectionController.start(appScope) }
        createNotificationChannels()
    }

    /** One channel per notification type, so each can be silenced on its own in system settings. */
    private fun createNotificationChannels() {
        val manager = getSystemService(NotificationManager::class.java)
        // The 9 AM / 1 PM / 7 PM briefings' channel, replaced by these.
        manager.deleteNotificationChannel(LEGACY_CHANNEL_ID)
        listOf(
            Triple(CHANNEL_MORNING_BRIEFING, R.string.notif_channel_briefing_name, R.string.notif_channel_briefing_description),
            Triple(CHANNEL_WALK_TIME, R.string.notif_channel_walk_name, R.string.notif_channel_walk_description),
            Triple(CHANNEL_RAIN, R.string.notif_channel_rain_name, R.string.notif_channel_rain_description),
        ).forEach { (id, name, description) ->
            val channel = NotificationChannel(id, getString(name), NotificationManager.IMPORTANCE_DEFAULT).apply {
                this.description = getString(description)
            }
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_MORNING_BRIEFING = "morning_briefing"
        const val CHANNEL_WALK_TIME = "walk_time"
        const val CHANNEL_RAIN = "rain_alerts"
        private const val LEGACY_CHANNEL_ID = "weather_daily"
    }
}
