package com.fangjet.weather.di

import com.fangjet.weather.BuildConfig
import com.fangjet.weather.data.ForecastSourcePolicy
import com.fangjet.weather.data.RemoteConfigRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.ZoneId
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppConfigModule {

    /**
     * Remote Config `openweather_api_key` when set, otherwise the key built into the app. Read once
     * per process, so a rotated key takes effect from the next app start.
     */
    @Provides
    @Singleton
    @Named(WeatherNetworkModule.OWM_API_KEY)
    fun provideOwmApiKey(remoteConfig: RemoteConfigRepository): String =
        resolveOwmApiKey(remoteConfig.openWeatherApiKey(), BuildConfig.OWM_API_KEY)

    internal fun resolveOwmApiKey(
        remoteKey: String?,
        bundledKey: String,
    ): String = remoteKey?.trim()?.takeIf { it.isNotEmpty() } ?: bundledKey.trim()

    @Provides
    @Singleton
    fun provideDefaultZoneId(): ZoneId = ZoneId.systemDefault()

    /**
     * Remote Config `open_meteo_enabled` and `air_quality_enabled`, read on every forecast fetch
     * (foreground and workers).
     */
    @Provides
    @Singleton
    fun provideForecastSourcePolicy(remoteConfig: RemoteConfigRepository): ForecastSourcePolicy =
        object : ForecastSourcePolicy {
            override fun openMeteoEnabled() = remoteConfig.openMeteoEnabled()

            override fun airQualityEnabled() = remoteConfig.airQualityEnabled()
        }
}
