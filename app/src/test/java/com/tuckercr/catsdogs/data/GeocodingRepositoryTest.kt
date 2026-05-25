package com.tuckercr.catsdogs.data

import com.tuckercr.catsdogs.data.remote.GeocodingApi
import com.tuckercr.catsdogs.data.remote.dto.GeocodingDirectDto
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GeocodingRepositoryTest {

    @Test
    fun `short query returns empty suggestions without calling api`() =
        runBlocking {
            val api = FakeGeocodingApi()
            val repository = GeocodingRepository(api, apiKey = "geocode-key")

            val suggestions = repository.searchCities(" L ").getOrThrow()

            assertTrue(suggestions.isEmpty())
            assertTrue(api.calls.isEmpty())
        }

    @Test
    fun `search trims query and maps city labels`() =
        runBlocking {
            val api = FakeGeocodingApi(
                results = listOf(
                    GeocodingDirectDto(
                        name = "London",
                        lat = 42.9849,
                        lon = -81.2453,
                        country = "CA",
                        state = "ON",
                    ),
                    GeocodingDirectDto(
                        name = "Paris",
                        lat = 48.8566,
                        lon = 2.3522,
                        country = "FR",
                        state = " ",
                    ),
                ),
            )
            val repository = GeocodingRepository(api, apiKey = " geocode-key ")

            val suggestions = repository.searchCities("  Lo  ").getOrThrow()

            assertEquals("London, ON, CA", suggestions[0].label)
            assertEquals(42.9849, suggestions[0].weatherLat, 0.0001)
            assertEquals(-81.2453, suggestions[0].weatherLon, 0.0001)
            assertEquals("Paris, FR", suggestions[1].label)

            val call = api.calls.single()
            assertEquals("Lo", call.query)
            assertEquals(8, call.limit)
            assertEquals("geocode-key", call.apiKey)
        }

    @Test
    fun `missing api key fails without calling api`() =
        runBlocking {
            val api = FakeGeocodingApi()
            val repository = GeocodingRepository(api, apiKey = " ")

            val error = repository.searchCities("London").exceptionOrNull()

            assertEquals("missing_api_key", error?.message)
            assertTrue(api.calls.isEmpty())
        }

    private class FakeGeocodingApi(
        private val results: List<GeocodingDirectDto> = emptyList(),
    ) : GeocodingApi {

        val calls = mutableListOf<SearchCall>()

        override suspend fun directSearch(
            query: String,
            limit: Int,
            apiKey: String,
        ): List<GeocodingDirectDto> {
            calls += SearchCall(query, limit, apiKey)
            return results
        }
    }

    private data class SearchCall(
        val query: String,
        val limit: Int,
        val apiKey: String,
    )
}
