package com.tuckercr.catsdogs.data

import com.tuckercr.catsdogs.data.remote.GeocodingApi
import com.tuckercr.catsdogs.data.remote.dto.GeocodingDirectDto
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GeocodingRepositoryTest {

    @Test
    fun `searchCities trims input and formats suggestion labels`() = runBlocking {
        val api = FakeGeocodingApi(
            response = listOf(
                GeocodingDirectDto(
                    name = "Austin",
                    lat = 30.2672,
                    lon = -97.7431,
                    country = "US",
                    state = "",
                ),
            ),
        )
        val repository = GeocodingRepository(api, " test-key ")

        val suggestions = repository.searchCities("  Austin  ").getOrThrow()

        assertEquals("Austin", api.lastQuery)
        assertEquals(8, api.lastLimit)
        assertEquals("test-key", api.lastApiKey)
        assertEquals(1, suggestions.size)
        assertEquals("Austin, US", suggestions.single().label)
        assertEquals(30.2672, suggestions.single().weatherLat, 0.0001)
        assertEquals(-97.7431, suggestions.single().weatherLon, 0.0001)
    }

    @Test
    fun `searchCities returns empty result for short query without calling api`() = runBlocking {
        val api = FakeGeocodingApi()
        val repository = GeocodingRepository(api, "test-key")

        val suggestions = repository.searchCities(" a ").getOrThrow()

        assertTrue(suggestions.isEmpty())
        assertEquals(0, api.callCount)
    }

    private class FakeGeocodingApi(
        private val response: List<GeocodingDirectDto> = emptyList(),
    ) : GeocodingApi {
        var callCount = 0
        var lastQuery: String? = null
        var lastLimit: Int? = null
        var lastApiKey: String? = null

        override suspend fun directSearch(
            query: String,
            limit: Int,
            apiKey: String,
        ): List<GeocodingDirectDto> {
            callCount += 1
            lastQuery = query
            lastLimit = limit
            lastApiKey = apiKey
            return response
        }
    }
}
