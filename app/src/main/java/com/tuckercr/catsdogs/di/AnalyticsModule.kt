package com.tuckercr.catsdogs.di

import com.tuckercr.catsdogs.analytics.AnalyticsBackend
import com.tuckercr.catsdogs.analytics.FirebaseAnalyticsBackend
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
