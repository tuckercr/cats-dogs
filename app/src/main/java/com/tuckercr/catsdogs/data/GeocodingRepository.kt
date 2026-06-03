package com.tuckercr.catsdogs.data

import com.tuckercr.catsdogs.data.remote.GeocodingApi
import com.tuckercr.catsdogs.data.remote.dto.GeocodingDirectDto
import com.tuckercr.catsdogs.di.AppConfigModule
import com.tuckercr.catsdogs.domain.CitySuggestion
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class GeocodingRepository @Inject constructor(
    private val api: GeocodingApi,
    @param:Named(AppConfigModule.OWM_API_KEY) private val apiKey: String,
) : CitySearchDataSource {

    override suspend fun searchCities(query: String): Result<List<CitySuggestion>> {
        val key = apiKey.trim()
        if (key.isEmpty()) {
            return Result.failure(IllegalStateException("missing_api_key"))
        }
        val q = query.trim()
        if (q.length < 2) {
            return Result.success(emptyList())
        }
        return runCatching {
            api.directSearch(query = q, limit = 8, apiKey = key).map { it.toSuggestion() }
        }
    }

    private fun GeocodingDirectDto.toSuggestion(): CitySuggestion {
        val labelParts = listOfNotNull(
            name,
            state?.takeIf { it.isNotBlank() },
            country,
        )
        val label = labelParts.joinToString(", ")

        return CitySuggestion(
            label = label,
            weatherLat = lat,
            weatherLon = lon,
        )
    }
}
