package com.fangjet.weather.analytics

import com.fangjet.weather.data.PreferencesRepository
import com.fangjet.weather.data.RemoteConfigRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Keeps Firebase Analytics collection in step with the user's "Share usage statistics"
 * setting and the Remote Config `analytics_enabled` kill switch: collection is on only
 * when both are on.
 *
 * Done at the SDK level (`setAnalyticsCollectionEnabled`) rather than only by filtering our
 * own events, so Firebase's automatic events (first_open, session_start, screen_view…) stop
 * too — an opt-out that only silenced our custom events would be a half-truth.
 * Started once from the Application; re-applied whenever either input changes.
 */
@Singleton
class AnalyticsCollectionController @Inject constructor(
    private val preferences: PreferencesRepository,
    private val remoteConfig: RemoteConfigRepository,
    private val backend: AnalyticsBackend,
) {
    /**
     * The user's latest "Share usage statistics" choice, or null until the preference has been
     * read. [AnalyticsRepository] treats null as "don't send" so an opted-out user's choice is
     * honoured even for an event fired in the first moments after launch.
     */
    @Volatile
    var shareUsageStatistics: Boolean? = null
        private set

    fun start(scope: CoroutineScope) {
        scope.launch {
            combine(
                preferences.shareUsageStatistics.onEach { shareUsageStatistics = it },
                remoteConfig.analyticsEnabledUpdates,
            ) { userOptedIn, killSwitchOn -> userOptedIn && killSwitchOn }
                .distinctUntilChanged()
                .catch { Timber.w(it, "Analytics collection state unavailable; leaving SDK flag as is") }
                .collect { enabled -> backend.setCollectionEnabled(enabled) }
        }
    }
}
