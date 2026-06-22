package com.tuckercr.catsdogs.analytics

import android.os.Bundle
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnalyticsRepository @Inject constructor() {

    private val analytics = runCatching { Firebase.analytics }.getOrNull()

    fun logOnboardingComplete() = log(EVENT_ONBOARDING_COMPLETE)

    fun logCityAdded() = log(EVENT_CITY_ADDED)

    fun logForecastOpened() = log(EVENT_FORECAST_OPENED)

    fun logSettingsOpened() = log(EVENT_SETTINGS_OPENED)

    fun logWeatherError(errorKey: String) =
        log(
            EVENT_WEATHER_ERROR,
            Bundle().apply { putString(PARAM_ERROR_KEY, errorKey) },
        )

    private fun log(
        event: String,
        params: Bundle? = null,
    ) {
        analytics?.logEvent(event, params)
    }

    companion object {
        const val EVENT_ONBOARDING_COMPLETE = "onboarding_complete"
        const val EVENT_CITY_ADDED = "city_added"
        const val EVENT_FORECAST_OPENED = "forecast_opened"
        const val EVENT_SETTINGS_OPENED = "settings_opened"
        const val EVENT_WEATHER_ERROR = "weather_error"
        const val PARAM_ERROR_KEY = "error_key"
    }
}
