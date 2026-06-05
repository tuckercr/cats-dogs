package com.tuckercr.catsdogs.di

import com.tuckercr.catsdogs.data.CitySearchDataSource
import com.tuckercr.catsdogs.data.GeocodingRepository
import com.tuckercr.catsdogs.data.PreferencesRepository
import com.tuckercr.catsdogs.data.WeatherDataSource
import com.tuckercr.catsdogs.data.WeatherPreferences
import com.tuckercr.catsdogs.data.WeatherRepository
import com.tuckercr.catsdogs.util.AndroidWeatherUnitsProvider
import com.tuckercr.catsdogs.util.WeatherUnitsProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryBindingsModule {
    @Binds
    @Singleton
    abstract fun bindWeatherPreferences(repository: PreferencesRepository): WeatherPreferences

    @Binds
    @Singleton
    abstract fun bindWeatherDataSource(repository: WeatherRepository): WeatherDataSource

    @Binds
    @Singleton
    abstract fun bindCitySearchDataSource(repository: GeocodingRepository): CitySearchDataSource

    @Binds
    @Singleton
    abstract fun bindWeatherUnitsProvider(provider: AndroidWeatherUnitsProvider): WeatherUnitsProvider
}
