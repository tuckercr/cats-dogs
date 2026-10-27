package com.fangjet.weather.di

import com.fangjet.weather.analytics.AnalyticsBackend
import com.fangjet.weather.analytics.FirebaseAnalyticsBackend
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class AnalyticsModule {

    @Binds
    abstract fun bindAnalyticsBackend(impl: FirebaseAnalyticsBackend): AnalyticsBackend
}
