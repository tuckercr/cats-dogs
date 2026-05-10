package com.tuckercr.catsdogs.di

import com.tuckercr.catsdogs.BuildConfig
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

    const val OWM_API_KEY: String = "owm_api_key"

    @Provides
    @Singleton
    @Named(OWM_API_KEY)
    fun provideOwmApiKey(): String = BuildConfig.OWM_API_KEY.trim()

    @Provides
    @Singleton
    fun provideDefaultZoneId(): ZoneId = ZoneId.systemDefault()
}
