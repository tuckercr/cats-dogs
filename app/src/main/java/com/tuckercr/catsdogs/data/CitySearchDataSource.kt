package com.tuckercr.catsdogs.data

import com.tuckercr.catsdogs.domain.CitySuggestion

interface CitySearchDataSource {
    suspend fun searchCities(query: String): Result<List<CitySuggestion>>
}
