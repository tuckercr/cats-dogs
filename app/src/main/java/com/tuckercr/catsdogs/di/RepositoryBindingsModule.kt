package com.tuckercr.catsdogs.di

import com.tuckercr.catsdogs.data.CitySearchRepository
import com.tuckercr.catsdogs.data.GeocodingRepository
import com.tuckercr.catsdogs.data.PreferencesRepository
import com.tuckercr.catsdogs.data.UserPreferences
import com.tuckercr.catsdogs.data.WeatherDataRepository
import com.tuckercr.catsdogs.data.WeatherRepository
import com.tuckercr.catsdogs.util.ContextWeatherUnitsProvider
import com.tuckercr.catsdogs.util.WeatherUnitsProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryBindingsModule {

    @Binds
    abstract fun bindUserPreferences(repository: PreferencesRepository): UserPreferences

    @Binds
    abstract fun bindCitySearchRepository(repository: GeocodingRepository): CitySearchRepository

    @Binds
    abstract fun bindWeatherDataRepository(repository: WeatherRepository): WeatherDataRepository

    @Binds
    abstract fun bindWeatherUnitsProvider(provider: ContextWeatherUnitsProvider): WeatherUnitsProvider
}
