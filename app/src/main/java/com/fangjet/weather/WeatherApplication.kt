package com.fangjet.weather

import android.app.Application
import android.app.NotificationManager
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import coil.ImageLoader
import com.fangjet.weather.analytics.AnalyticsCollectionController
import com.fangjet.weather.data.PreferencesRepository
import com.fangjet.weather.worker.NotificationKind
import com.fangjet.weather.worker.WorkManagerScheduler
import com.fangjet.weather.worker.createNotificationChannel
import com.google.firebase.Firebase
import com.google.firebase.crashlytics.crashlytics
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
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

    @Inject
    lateinit var preferencesRepository: PreferencesRepository

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

    /**
     * One channel per notification type, so each can be silenced on its own in system settings. The
     * briefing and rain channels are made now; Walk time only once there's a dog (the onboarding
     * answer or Settings), so Android's list doesn't offer it to people without one. A channel is
     * never deleted: turning the dog off later leaves it, with the user's choices for it.
     */
    private fun createNotificationChannels() {
        // The 9 AM / 1 PM / 7 PM briefings' channel, replaced by these.
        getSystemService(NotificationManager::class.java).deleteNotificationChannel(LEGACY_CHANNEL_ID)
        channelsNeeded(hasDog = false).forEach(::createNotificationChannel)
        preferencesRepository.showWalkCard
            .filter { it }
            .onEach { channelsNeeded(hasDog = true).forEach(::createNotificationChannel) }
            .launchIn(appScope)
    }

    companion object {
        /** The channels there should be: Walk time only with a dog. */
        internal fun channelsNeeded(hasDog: Boolean): List<NotificationKind> =
            NotificationKind.entries.filter { it != NotificationKind.WALK_TIME || hasDog }

        const val CHANNEL_MORNING_BRIEFING = "morning_briefing"
        const val CHANNEL_WALK_TIME = "walk_time"
        const val CHANNEL_RAIN = "rain_alerts"
        private const val LEGACY_CHANNEL_ID = "weather_daily"
    }
}
