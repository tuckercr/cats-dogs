package com.tuckercr.catsdogs.data

import com.tuckercr.catsdogs.data.remote.GeocodingApi
import com.tuckercr.catsdogs.data.remote.dto.GeocodingDirectDto
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GeocodingRepositoryTest {

    @Test
    fun `searchCities trims query and maps city labels`() =
        runBlocking {
            val api = RecordingGeocodingApi(
                response = listOf(
                    GeocodingDirectDto(
                        name = "London",
                        lat = 51.5074,
                        lon = -0.1278,
                        country = "GB",
                        state = "England",
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
            val repository = GeocodingRepository(api, " test-key ")

            val result = repository.searchCities("  Lon  ")

            assertTrue(result.isSuccess)
            assertEquals("Lon", api.lastQuery)
            assertEquals(8, api.lastLimit)
            assertEquals("test-key", api.lastApiKey)
            assertEquals("London, England, GB", result.getOrThrow()[0].label)
            assertEquals(51.5074, result.getOrThrow()[0].weatherLat, 0.0001)
            assertEquals(-0.1278, result.getOrThrow()[0].weatherLon, 0.0001)
            assertEquals("Paris, FR", result.getOrThrow()[1].label)
        }

    @Test
    fun `searchCities returns empty result for short trimmed queries without calling api`() =
        runBlocking {
            val api = RecordingGeocodingApi()
            val repository = GeocodingRepository(api, "test-key")

            val result = repository.searchCities(" a ")

            assertTrue(result.isSuccess)
            assertEquals(0, api.callCount)
            assertTrue(result.getOrThrow().isEmpty())
        }

    @Test
    fun `searchCities fails before api call when api key is missing`() =
        runBlocking {
            val api = RecordingGeocodingApi()
            val repository = GeocodingRepository(api, " ")

            val result = repository.searchCities("Austin")

            assertTrue(result.isFailure)
            assertEquals("missing_api_key", result.exceptionOrNull()?.message)
            assertEquals(0, api.callCount)
        }

    private class RecordingGeocodingApi(
        private val response: List<GeocodingDirectDto> = emptyList(),
    ) : GeocodingApi {
        var callCount = 0
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
            callCount += 1
            lastQuery = query
            lastLimit = limit
            lastApiKey = apiKey
            return response
        }
    }
}
