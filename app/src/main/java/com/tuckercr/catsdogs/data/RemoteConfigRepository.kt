package com.tuckercr.catsdogs.data

import android.content.Context
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfigSettings
import com.tuckercr.catsdogs.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RemoteConfigRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    // Firebase.remoteConfig requires FirebaseApp to be initialized (needs google-services.json).
    // We catch any failure and fall through to local defaults so the app works without Firebase.
    private val remoteConfig = runCatching {
        Firebase.remoteConfig.also { rc ->
            rc.setDefaultsAsync(R.xml.remote_config_defaults)
            rc.setConfigSettingsAsync(
                remoteConfigSettings {
                    // Fetch new values at most every hour in production.
                    minimumFetchIntervalInSeconds = 3600
                },
            )
            rc.fetchAndActivate()
        }
    }.getOrNull()

    fun refreshIntervalMinutes(): Long =
        remoteConfig?.getLong(KEY_REFRESH_INTERVAL) ?: DEFAULT_REFRESH_INTERVAL_MINUTES

    companion object {
        const val KEY_REFRESH_INTERVAL = "weather_refresh_interval_minutes"
        const val DEFAULT_REFRESH_INTERVAL_MINUTES = 30L
    }
}
