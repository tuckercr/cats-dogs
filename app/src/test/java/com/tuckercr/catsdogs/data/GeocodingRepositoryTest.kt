package com.tuckercr.catsdogs.data

import com.tuckercr.catsdogs.data.remote.GeocodingApi
import com.tuckercr.catsdogs.data.remote.dto.GeocodingDirectDto
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GeocodingRepositoryTest {

    @Test
    fun `short queries return empty suggestions without calling API`() {
        runBlocking {
            val api = FakeGeocodingApi()
            val repository = GeocodingRepository(api, apiKey = "api-key")

            val result = repository.searchCities(" a ")

            assertTrue(result.getOrThrow().isEmpty())
            assertTrue(api.calls.isEmpty())
        }
    }

    @Test
    fun `missing API key fails before calling API`() {
        runBlocking {
            val api = FakeGeocodingApi()
            val repository = GeocodingRepository(api, apiKey = "  ")

            val result = repository.searchCities("Paris")

            assertEquals("missing_api_key", result.exceptionOrNull()?.message)
            assertTrue(api.calls.isEmpty())
        }
    }

    @Test
    fun `search trims query and maps labels without blank state`() {
        runBlocking {
            val api = FakeGeocodingApi(
                responses = listOf(
                    GeocodingDirectDto(name = "Austin", lat = 30.2672, lon = -97.7431, country = "US", state = "TX"),
                    GeocodingDirectDto(name = "Paris", lat = 48.8566, lon = 2.3522, country = "FR", state = " "),
                ),
            )
            val repository = GeocodingRepository(api, apiKey = " api-key ")

            val result = repository.searchCities("  Par ")

            val suggestions = result.getOrThrow()
            assertEquals(GeocodingCall(query = "Par", limit = 8, apiKey = "api-key"), api.calls.single())
            assertEquals("Austin, TX, US", suggestions.first().label)
            assertEquals(30.2672, suggestions.first().weatherLat, 0.0001)
            assertEquals(-97.7431, suggestions.first().weatherLon, 0.0001)
            assertEquals("Paris, FR", suggestions.last().label)
        }
    }

    private class FakeGeocodingApi(
        private val responses: List<GeocodingDirectDto> = emptyList(),
    ) : GeocodingApi {
        val calls = mutableListOf<GeocodingCall>()

        override suspend fun directSearch(
            query: String,
            limit: Int,
            apiKey: String,
        ): List<GeocodingDirectDto> {
            calls += GeocodingCall(query, limit, apiKey)
            return responses
        }
    }

    private data class GeocodingCall(
        val query: String,
        val limit: Int,
        val apiKey: String,
    )
}
