package com.tuckercr.catsdogs.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.weatherDataStore: DataStore<Preferences> by preferencesDataStore(name = "weather_prefs")

@Singleton
class PreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) : UserPreferences {

    private val dataStore get() = context.weatherDataStore

    override val hasSeenWelcome: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_HAS_SEEN_WELCOME] == true
    }

    override val lastCity: Flow<String?> = dataStore.data.map { prefs ->
        prefs[KEY_LAST_CITY]?.takeIf { it.isNotBlank() }
    }

    override suspend fun setHasSeenWelcome(value: Boolean) {
        dataStore.edit { it[KEY_HAS_SEEN_WELCOME] = value }
    }

    override suspend fun setLastCity(cityName: String) {
        dataStore.edit { it[KEY_LAST_CITY] = cityName }
    }

    override suspend fun hasSeenWelcomeOnce(): Boolean = hasSeenWelcome.first()

    companion object {
        private val KEY_HAS_SEEN_WELCOME = booleanPreferencesKey("has_seen_welcome")
        private val KEY_LAST_CITY = stringPreferencesKey("last_city")
    }
}
