package com.fangjet.weather.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.fangjet.weather.domain.SavedLocation
import com.fangjet.weather.domain.UnitOverride
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

private val Context.weatherDataStore: DataStore<Preferences> by preferencesDataStore(name = "weather_prefs")

@Singleton
class PreferencesRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val json: Json,
) {

    private val dataStore get() = context.weatherDataStore

    val hasSeenWelcome: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_HAS_SEEN_WELCOME] == true
    }

    val locationOnboardingDone: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_LOCATION_ONBOARDING_DONE] == true
    }

    val notificationOnboardingDone: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_NOTIFICATION_ONBOARDING_DONE] == true ||
            (prefs[KEY_NOTIFICATION_ONBOARDING_DONE] == null && prefs[KEY_LOCATION_ONBOARDING_DONE] == true)
    }

    val lastCity: Flow<String?> = dataStore.data.map { prefs ->
        prefs[KEY_LAST_CITY]?.takeIf { it.isNotBlank() }
    }

    val savedLocations: Flow<List<SavedLocation>> = dataStore.data.map { prefs ->
        val raw = prefs[KEY_SAVED_LOCATIONS] ?: return@map emptyList()
        runCatching { json.decodeFromString<List<SavedLocation>>(raw) }.getOrElse { emptyList() }
    }

    val activeLocationIndex: Flow<Int> = dataStore.data.map { prefs ->
        prefs[KEY_ACTIVE_LOCATION_INDEX] ?: 0
    }

    suspend fun setHasSeenWelcome(value: Boolean) {
        dataStore.edit { it[KEY_HAS_SEEN_WELCOME] = value }
    }

    suspend fun setLocationOnboardingDone() {
        dataStore.edit { it[KEY_LOCATION_ONBOARDING_DONE] = true }
    }

    suspend fun setNotificationOnboardingDone() {
        dataStore.edit { it[KEY_NOTIFICATION_ONBOARDING_DONE] = true }
    }

    suspend fun setLastCity(cityName: String) {
        dataStore.edit { it[KEY_LAST_CITY] = cityName }
    }

    suspend fun setSavedLocations(locations: List<SavedLocation>) {
        dataStore.edit { it[KEY_SAVED_LOCATIONS] = json.encodeToString(locations) }
    }

    suspend fun setActiveLocationIndex(index: Int) {
        dataStore.edit { it[KEY_ACTIVE_LOCATION_INDEX] = index }
    }

    suspend fun getCachedWeatherFor(locationKey: String): String? {
        val raw = dataStore.data.map { it[KEY_WEATHER_CACHE] }.first() ?: return null
        return runCatching { json.decodeFromString<Map<String, String>>(raw) }
            .getOrNull()
            ?.get(locationKey)
    }

    suspend fun setCachedWeatherFor(
        locationKey: String,
        weatherJson: String,
    ) {
        dataStore.edit { prefs ->
            val existing = prefs[KEY_WEATHER_CACHE]
                ?.let { runCatching { json.decodeFromString<Map<String, String>>(it) }.getOrNull() }
                ?: emptyMap()
            prefs[KEY_WEATHER_CACHE] = json.encodeToString(existing + (locationKey to weatherJson))
        }
    }

    suspend fun getCachedForecastFor(locationKey: String): String? {
        val raw = dataStore.data.map { it[KEY_FORECAST_CACHE] }.first() ?: return null
        return runCatching { json.decodeFromString<Map<String, String>>(raw) }
            .getOrNull()
            ?.get(locationKey)
    }

    suspend fun setCachedForecastFor(
        locationKey: String,
        forecastJson: String,
    ) {
        dataStore.edit { prefs ->
            val existing = prefs[KEY_FORECAST_CACHE]
                ?.let { runCatching { json.decodeFromString<Map<String, String>>(it) }.getOrNull() }
                ?: emptyMap()
            prefs[KEY_FORECAST_CACHE] =
                json.encodeToString(existing + (locationKey to forecastJson))
        }
    }

    val unitOverride: Flow<UnitOverride> = dataStore.data.map { prefs ->
        when (prefs[KEY_UNIT_OVERRIDE]) {
            UnitOverride.METRIC.name -> UnitOverride.METRIC
            UnitOverride.IMPERIAL.name -> UnitOverride.IMPERIAL
            else -> UnitOverride.SYSTEM
        }
    }

    suspend fun setUnitOverride(override: UnitOverride) {
        dataStore.edit { it[KEY_UNIT_OVERRIDE] = override.name }
    }

    /** "Share usage statistics" — the user's analytics opt-out. On unless they turn it off. */
    val shareUsageStatistics: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_SHARE_USAGE_STATISTICS] ?: true
    }

    suspend fun setShareUsageStatistics(enabled: Boolean) {
        dataStore.edit { it[KEY_SHARE_USAGE_STATISTICS] = enabled }
    }

    /** Whether the main screen shows "Best walk time". On unless they turn it off (no dog, say). */
    val showWalkCard: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_SHOW_WALK_CARD] ?: true
    }

    suspend fun setShowWalkCard(show: Boolean) {
        dataStore.edit { it[KEY_SHOW_WALK_CARD] = show }
    }

    /** The 8:30 AM weather briefing. On unless they turn it off. */
    val notifyMorningBriefing: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_NOTIFY_MORNING_BRIEFING] ?: true
    }

    suspend fun setNotifyMorningBriefing(enabled: Boolean) {
        dataStore.edit { it[KEY_NOTIFY_MORNING_BRIEFING] = enabled }
    }

    /**
     * A nudge at the day's best walk time. Until they choose, it follows the dog question (the walk
     * card's switch): on with a dog at home, off without.
     */
    val notifyWalkTime: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_NOTIFY_WALK_TIME] ?: prefs[KEY_SHOW_WALK_CARD] ?: true
    }

    suspend fun setNotifyWalkTime(enabled: Boolean) {
        dataStore.edit { it[KEY_NOTIFY_WALK_TIME] = enabled }
    }

    /** An alert when rain or snow is about to start. Off unless they turn it on. */
    val notifyRain: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_NOTIFY_RAIN] ?: false
    }

    suspend fun setNotifyRain(enabled: Boolean) {
        dataStore.edit { it[KEY_NOTIFY_RAIN] = enabled }
    }

    /** When the rain last alerted about starts, epoch seconds, so the same shower isn't alerted twice. */
    suspend fun lastRainAlertStart(): Long? = dataStore.data.first()[KEY_LAST_RAIN_ALERT_START]

    suspend fun setLastRainAlertStart(epochSeconds: Long) {
        dataStore.edit { it[KEY_LAST_RAIN_ALERT_START] = epochSeconds }
    }

    suspend fun clearCache() {
        dataStore.edit {
            it.remove(KEY_WEATHER_CACHE)
            it.remove(KEY_FORECAST_CACHE)
        }
    }

    suspend fun hasSeenWelcomeOnce(): Boolean = hasSeenWelcome.first()

    suspend fun locationOnboardingDoneOnce(): Boolean = locationOnboardingDone.first()

    suspend fun notificationOnboardingDoneOnce(): Boolean = notificationOnboardingDone.first()

    companion object {
        private val KEY_HAS_SEEN_WELCOME = booleanPreferencesKey("has_seen_welcome")
        private val KEY_LOCATION_ONBOARDING_DONE = booleanPreferencesKey("location_onboarding_done")
        private val KEY_NOTIFICATION_ONBOARDING_DONE = booleanPreferencesKey("notification_onboarding_done")
        private val KEY_LAST_CITY = stringPreferencesKey("last_city")
        private val KEY_SAVED_LOCATIONS = stringPreferencesKey("saved_locations")
        private val KEY_ACTIVE_LOCATION_INDEX = intPreferencesKey("active_location_index")
        private val KEY_WEATHER_CACHE = stringPreferencesKey("weather_cache")
        private val KEY_FORECAST_CACHE = stringPreferencesKey("forecast_cache")
        private val KEY_UNIT_OVERRIDE = stringPreferencesKey("unit_override")
        private val KEY_SHARE_USAGE_STATISTICS = booleanPreferencesKey("share_usage_statistics")
        private val KEY_SHOW_WALK_CARD = booleanPreferencesKey("show_walk_card")
        private val KEY_NOTIFY_MORNING_BRIEFING = booleanPreferencesKey("notify_morning_briefing")
        private val KEY_NOTIFY_WALK_TIME = booleanPreferencesKey("notify_walk_time")
        private val KEY_NOTIFY_RAIN = booleanPreferencesKey("notify_rain")
        private val KEY_LAST_RAIN_ALERT_START = longPreferencesKey("last_rain_alert_start")
    }
}
