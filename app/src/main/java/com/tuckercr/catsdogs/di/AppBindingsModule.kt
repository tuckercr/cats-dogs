package com.tuckercr.catsdogs.di

import com.tuckercr.catsdogs.data.CitySearchRepository
import com.tuckercr.catsdogs.data.GeocodingRepository
import com.tuckercr.catsdogs.data.PreferencesRepository
import com.tuckercr.catsdogs.data.WeatherPreferences
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppBindingsModule {

    @Binds
    @Singleton
    abstract fun bindWeatherPreferences(repository: PreferencesRepository): WeatherPreferences

    @Binds
    @Singleton
    abstract fun bindCitySearchRepository(repository: GeocodingRepository): CitySearchRepository
}
