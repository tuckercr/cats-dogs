package com.tuckercr.catsdogs.di

import com.tuckercr.catsdogs.data.CitySearchRepository
import com.tuckercr.catsdogs.data.GeocodingRepository
import com.tuckercr.catsdogs.data.PreferencesRepository
import com.tuckercr.catsdogs.data.SavedCityRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryBindingModule {

    @Binds
    abstract fun bindCitySearchRepository(repository: GeocodingRepository): CitySearchRepository

    @Binds
    abstract fun bindSavedCityRepository(repository: PreferencesRepository): SavedCityRepository
}
