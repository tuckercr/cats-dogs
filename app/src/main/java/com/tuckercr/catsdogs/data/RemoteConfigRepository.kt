package com.tuckercr.catsdogs.data

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigValue
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfigSettings
import com.tuckercr.catsdogs.R
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Server-tunable values from Firebase Remote Config (project `cats-dogs-weather`; the
 * console parameters live in `firebase/remoteconfig.template.json`).
 *
 * ## Never crash, fall back to defaults
 *
 * Firebase is optional: a checkout without `google-services.json` never initialises
 * [FirebaseApp], and in that case this class is an inert pass-through that returns the
 * compiled-in defaults below. With Firebase present it seeds the in-app defaults from
 * `res/xml/remote_config_defaults.xml`, reads the last activated values immediately on
 * cold start, and refreshes in the background. Every read is guarded: a missing, unparsable
 * or out-of-range value falls back to the default rather than throwing.
 */
@Singleton
class RemoteConfigRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    // Bumped whenever new values become readable (in-app defaults landed, fetch activated).
    // Declared before remoteConfig: its completion listeners write here.
    private val activations = MutableStateFlow(0)

    /** Non-null only when Firebase actually initialised. */
    private val remoteConfig: FirebaseRemoteConfig? = initRemoteConfig()

    fun refreshIntervalMinutes(): Long = long(KEY_REFRESH_INTERVAL) ?: DEFAULT_REFRESH_INTERVAL_MINUTES

    /** Delay between radar animation frames, in milliseconds. Clamped to a sane range. */
    fun radarFrameIntervalMs(): Long =
        long(KEY_RADAR_FRAME_INTERVAL)
            ?.takeIf { it in MIN_RADAR_FRAME_INTERVAL_MS..MAX_RADAR_FRAME_INTERVAL_MS }
            ?: DEFAULT_RADAR_FRAME_INTERVAL_MS

    /**
     * Radar kill switch. False hides the radar section on the current-weather screen and skips
     * the RainViewer timeline and tile fetches — for when RainViewer or OpenStreetMap tiles
     * become unavailable.
     */
    fun radarEnabled(): Boolean = boolean(KEY_RADAR_ENABLED) ?: DEFAULT_RADAR_ENABLED

    /** False hides the "time to walk the dog" card on the current-weather screen. */
    fun walkCardEnabled(): Boolean = boolean(KEY_WALK_CARD_ENABLED) ?: DEFAULT_WALK_CARD_ENABLED

    /**
     * Forecast provider for cities with coordinates: true uses Open-Meteo, false falls back to
     * the OpenWeatherMap 3-hourly forecast by lat/lon.
     */
    fun openMeteoEnabled(): Boolean = boolean(KEY_OPEN_METEO_ENABLED) ?: DEFAULT_OPEN_METEO_ENABLED

    /**
     * Daily 9 / 13 / 19 weather-summary notifications. False skips posting them (the scheduled
     * work still runs). Other notification types will have their own switches.
     */
    fun dailyWeatherNotificationsEnabled(): Boolean =
        boolean(KEY_DAILY_WEATHER_NOTIFICATIONS_ENABLED) ?: DEFAULT_DAILY_WEATHER_NOTIFICATIONS_ENABLED

    /**
     * Minimum minutes between silent foreground-resume refreshes of the same city. 0 always
     * refreshes; values outside 0..1440 fall back to 0. Explicit refreshes are never throttled.
     */
    fun foregroundRefreshMinMinutes(): Long =
        long(KEY_FOREGROUND_REFRESH_MIN_MINUTES)
            ?.takeIf { it in 0L..MAX_FOREGROUND_REFRESH_MIN_MINUTES }
            ?: DEFAULT_FOREGROUND_REFRESH_MIN_MINUTES

    /**
     * Analytics kill switch. False drops every analytics event and turns Firebase Analytics
     * collection off on every install, without an app update.
     */
    fun analyticsEnabled(): Boolean = boolean(KEY_ANALYTICS_ENABLED) ?: DEFAULT_ANALYTICS_ENABLED

    /** [analyticsEnabled], re-emitted whenever newly activated Remote Config values may have changed it. */
    val analyticsEnabledUpdates: Flow<Boolean> =
        activations.map { analyticsEnabled() }.distinctUntilChanged()

    private fun initRemoteConfig(): FirebaseRemoteConfig? {
        if (runCatching { FirebaseApp.getApps(context).isEmpty() }.getOrDefault(true)) {
            Timber.i("Firebase not initialised; using local Remote Config defaults")
            return null
        }
        return runCatching {
            Firebase.remoteConfig.also { rc ->
                rc.setConfigSettingsAsync(
                    remoteConfigSettings {
                        // Fetch new values at most every hour in production.
                        minimumFetchIntervalInSeconds = 3600
                    },
                )
                rc.setDefaultsAsync(R.xml.remote_config_defaults).addOnCompleteListener { onActivated() }
                rc.fetchAndActivate().addOnCompleteListener { task ->
                    if (!task.isSuccessful) {
                        // Network failure, throttling, etc.: keep whatever was already activated.
                        Timber.w(task.exception, "Remote Config fetch failed; keeping current values")
                    }
                    onActivated()
                }
            }
        }.onFailure { Timber.w(it, "Remote Config unavailable; using local defaults") }
            .getOrNull()
    }

    private fun onActivated() = activations.update { it + 1 }

    /**
     * Only values that actually exist (activated from the server or seeded from the in-app
     * defaults). A key with no value reports the STATIC source, where `asBoolean()` would
     * return `false` and `asLong()` `0` — on a cold first launch, before `setDefaultsAsync`
     * has landed, that would switch analytics off. Returning null falls back to the default.
     */
    private fun value(key: String): FirebaseRemoteConfigValue? =
        runCatching {
            remoteConfig?.getValue(key)?.takeIf { it.source != FirebaseRemoteConfig.VALUE_SOURCE_STATIC }
        }.getOrNull()

    private fun long(key: String): Long? = value(key)?.let { runCatching { it.asLong() }.getOrNull() }

    private fun boolean(key: String): Boolean? = value(key)?.let { runCatching { it.asBoolean() }.getOrNull() }

    companion object {
        const val KEY_REFRESH_INTERVAL = "weather_refresh_interval_minutes"
        const val DEFAULT_REFRESH_INTERVAL_MINUTES = 30L

        const val KEY_RADAR_FRAME_INTERVAL = "radar_frame_interval_ms"
        const val DEFAULT_RADAR_FRAME_INTERVAL_MS = 850L
        private const val MIN_RADAR_FRAME_INTERVAL_MS = 100L
        private const val MAX_RADAR_FRAME_INTERVAL_MS = 5000L

        const val KEY_DAILY_WEATHER_NOTIFICATIONS_ENABLED = "daily_weather_notifications_enabled"
        const val DEFAULT_DAILY_WEATHER_NOTIFICATIONS_ENABLED = true

        const val KEY_FOREGROUND_REFRESH_MIN_MINUTES = "foreground_refresh_min_minutes"
        const val DEFAULT_FOREGROUND_REFRESH_MIN_MINUTES = 0L
        private const val MAX_FOREGROUND_REFRESH_MIN_MINUTES = 1440L

        const val KEY_ANALYTICS_ENABLED = "analytics_enabled"
        const val DEFAULT_ANALYTICS_ENABLED = true

        const val KEY_RADAR_ENABLED = "radar_enabled"
        const val DEFAULT_RADAR_ENABLED = true

        const val KEY_WALK_CARD_ENABLED = "walk_card_enabled"
        const val DEFAULT_WALK_CARD_ENABLED = true

        const val KEY_OPEN_METEO_ENABLED = "open_meteo_enabled"
        const val DEFAULT_OPEN_METEO_ENABLED = true
    }
}
