package com.tuckercr.catsdogs.di

import com.tuckercr.catsdogs.BuildConfig
import com.tuckercr.catsdogs.data.ForecastSourcePolicy
import com.tuckercr.catsdogs.data.RemoteConfigRepository
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

    @Provides
    @Singleton
    @Named(WeatherNetworkModule.OWM_API_KEY)
    fun provideOwmApiKey(): String = BuildConfig.OWM_API_KEY.trim()

    @Provides
    @Singleton
    fun provideDefaultZoneId(): ZoneId = ZoneId.systemDefault()

    /** Remote Config `open_meteo_enabled`, read on every forecast fetch (foreground and workers). */
    @Provides
    @Singleton
    fun provideForecastSourcePolicy(remoteConfig: RemoteConfigRepository): ForecastSourcePolicy =
        ForecastSourcePolicy { remoteConfig.openMeteoEnabled() }
}
