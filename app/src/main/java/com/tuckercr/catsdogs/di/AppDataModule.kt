package com.tuckercr.catsdogs.di

import com.tuckercr.catsdogs.data.CitySearch
import com.tuckercr.catsdogs.data.GeocodingRepository
import com.tuckercr.catsdogs.data.PreferencesRepository
import com.tuckercr.catsdogs.data.UserPreferences
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppDataModule {

    @Binds
    @Singleton
    abstract fun bindCitySearch(repository: GeocodingRepository): CitySearch

    @Binds
    @Singleton
    abstract fun bindUserPreferences(repository: PreferencesRepository): UserPreferences
}
