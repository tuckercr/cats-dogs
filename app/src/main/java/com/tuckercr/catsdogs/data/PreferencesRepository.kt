package com.tuckercr.catsdogs.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.tuckercr.catsdogs.domain.SavedLocation
import com.tuckercr.catsdogs.domain.UnitOverride
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
    @ApplicationContext private val context: Context,
    private val json: Json,
) {

    private val dataStore get() = context.weatherDataStore

    val hasSeenWelcome: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_HAS_SEEN_WELCOME] == true
    }

    val locationOnboardingDone: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_LOCATION_ONBOARDING_DONE] == true
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

    suspend fun setLastCity(cityName: String) {
        dataStore.edit { it[KEY_LAST_CITY] = cityName }
    }

    suspend fun setSavedLocations(locations: List<SavedLocation>) {
        dataStore.edit { it[KEY_SAVED_LOCATIONS] = json.encodeToString(locations) }
    }

    suspend fun setActiveLocationIndex(index: Int) {
        dataStore.edit { it[KEY_ACTIVE_LOCATION_INDEX] = index }
    }

    val cachedCurrentWeatherJson: Flow<String?> = dataStore.data.map { prefs ->
        prefs[KEY_CACHED_CURRENT_WEATHER]
    }

    val cachedForecastJson: Flow<String?> = dataStore.data.map { prefs ->
        prefs[KEY_CACHED_FORECAST]
    }

    suspend fun setCachedCurrentWeather(json: String) {
        dataStore.edit { it[KEY_CACHED_CURRENT_WEATHER] = json }
    }

    suspend fun setCachedForecast(json: String) {
        dataStore.edit { it[KEY_CACHED_FORECAST] = json }
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

    suspend fun clearCache() {
        dataStore.edit {
            it.remove(KEY_CACHED_CURRENT_WEATHER)
            it.remove(KEY_CACHED_FORECAST)
        }
    }

    suspend fun hasSeenWelcomeOnce(): Boolean = hasSeenWelcome.first()

    suspend fun locationOnboardingDoneOnce(): Boolean = locationOnboardingDone.first()

    companion object {
        private val KEY_HAS_SEEN_WELCOME = booleanPreferencesKey("has_seen_welcome")
        private val KEY_LOCATION_ONBOARDING_DONE = booleanPreferencesKey("location_onboarding_done")
        private val KEY_LAST_CITY = stringPreferencesKey("last_city")
        private val KEY_SAVED_LOCATIONS = stringPreferencesKey("saved_locations")
        private val KEY_ACTIVE_LOCATION_INDEX = intPreferencesKey("active_location_index")
        private val KEY_CACHED_CURRENT_WEATHER = stringPreferencesKey("cached_current_weather")
        private val KEY_CACHED_FORECAST = stringPreferencesKey("cached_forecast")
        private val KEY_UNIT_OVERRIDE = stringPreferencesKey("unit_override")
    }
}
