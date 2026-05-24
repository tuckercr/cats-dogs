package com.tuckercr.catsdogs.data

import com.tuckercr.catsdogs.data.remote.GeocodingApi
import com.tuckercr.catsdogs.data.remote.dto.GeocodingDirectDto
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GeocodingRepositoryTest {

    @Test
    fun `searchCities rejects blank api key before calling api`() = runBlocking {
        val api = FakeGeocodingApi()
        val repository = GeocodingRepository(api = api, apiKey = " ")

        val result = repository.searchCities("London")

        assertTrue(result.isFailure)
        assertEquals("missing_api_key", result.exceptionOrNull()?.message)
        assertEquals(0, api.calls)
    }

    @Test
    fun `searchCities ignores short trimmed queries without calling api`() = runBlocking {
        val api = FakeGeocodingApi()
        val repository = GeocodingRepository(api = api, apiKey = "test-key")

        val result = repository.searchCities(" A ")

        assertTrue(result.isSuccess)
        assertEquals(emptyList<Any>(), result.getOrThrow())
        assertEquals(0, api.calls)
    }

    @Test
    fun `searchCities trims query and maps labels with optional state`() = runBlocking {
        val api = FakeGeocodingApi(
            response = listOf(
                GeocodingDirectDto(
                    name = "London",
                    state = "Ontario",
                    country = "CA",
                    lat = 42.9849,
                    lon = -81.2453,
                ),
                GeocodingDirectDto(
                    name = "Paris",
                    state = "",
                    country = "FR",
                    lat = 48.8566,
                    lon = 2.3522,
                ),
            ),
        )
        val repository = GeocodingRepository(api = api, apiKey = "test-key")

        val result = repository.searchCities("  Lon  ")

        assertTrue(result.isSuccess)
        val suggestions = result.getOrThrow()
        assertEquals(2, suggestions.size)
        assertEquals("London, Ontario, CA", suggestions[0].label)
        assertEquals(42.9849, suggestions[0].weatherLat, 0.0001)
        assertEquals(-81.2453, suggestions[0].weatherLon, 0.0001)
        assertEquals("Paris, FR", suggestions[1].label)
        assertEquals("Lon", api.lastQuery)
        assertEquals(8, api.lastLimit)
        assertEquals("test-key", api.lastApiKey)
    }

    private class FakeGeocodingApi(
        private val response: List<GeocodingDirectDto> = emptyList(),
    ) : GeocodingApi {
        var calls = 0
            private set
        var lastQuery: String? = null
            private set
        var lastLimit: Int? = null
            private set
        var lastApiKey: String? = null
            private set

        override suspend fun directSearch(
            query: String,
            limit: Int,
            apiKey: String,
        ): List<GeocodingDirectDto> {
            calls += 1
            lastQuery = query
            lastLimit = limit
            lastApiKey = apiKey
            return response
        }
    }
}
