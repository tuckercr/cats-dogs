package com.tuckercr.catsdogs.data

import com.tuckercr.catsdogs.data.remote.GeocodingApi
import com.tuckercr.catsdogs.data.remote.dto.GeocodingDirectDto
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine

class GeocodingRepositoryTest {

    @Test
    fun `searchCities validates api key before calling api`() {
        val api = FakeGeocodingApi()
        val repository = GeocodingRepository(api, " ")

        val result = runSuspend { repository.searchCities("Austin") }

        assertEquals("missing_api_key", result.exceptionOrNull()?.message)
        assertEquals(0, api.calls.size)
    }

    @Test
    fun `searchCities returns empty result for trimmed query shorter than two characters`() {
        val api = FakeGeocodingApi()
        val repository = GeocodingRepository(api, "test-key")

        val result = runSuspend { repository.searchCities(" A ") }

        assertEquals(emptyList<Any>(), result.getOrThrow())
        assertEquals(0, api.calls.size)
    }

    @Test
    fun `searchCities trims query and maps suggestion labels`() {
        val api = FakeGeocodingApi(
            response = listOf(
                GeocodingDirectDto(
                    name = "Austin",
                    lat = 30.2672,
                    lon = -97.7431,
                    country = "US",
                    state = "TX",
                ),
                GeocodingDirectDto(
                    name = "London",
                    lat = 51.5072,
                    lon = -0.1276,
                    country = "GB",
                    state = "",
                ),
                GeocodingDirectDto(
                    name = "Paris",
                    lat = 48.8566,
                    lon = 2.3522,
                    country = "FR",
                    state = null,
                ),
            ),
        )
        val repository = GeocodingRepository(api, " test-key ")

        val result = runSuspend { repository.searchCities(" Austin ") }

        val suggestions = result.getOrThrow()
        assertEquals(GeocodingCall(query = "Austin", limit = 8, apiKey = "test-key"), api.calls.single())
        assertEquals("Austin, TX, US", suggestions[0].label)
        assertEquals(30.2672, suggestions[0].weatherLat, 0.0001)
        assertEquals(-97.7431, suggestions[0].weatherLon, 0.0001)
        assertEquals("London, GB", suggestions[1].label)
        assertEquals("Paris, FR", suggestions[2].label)
    }

    private class FakeGeocodingApi(
        private val response: List<GeocodingDirectDto> = emptyList(),
    ) : GeocodingApi {
        val calls = mutableListOf<GeocodingCall>()

        override suspend fun directSearch(
            query: String,
            limit: Int,
            apiKey: String,
        ): List<GeocodingDirectDto> {
            calls += GeocodingCall(query, limit, apiKey)
            return response
        }
    }

    private data class GeocodingCall(
        val query: String,
        val limit: Int,
        val apiKey: String,
    )

    private companion object {
        private val unset = Any()

        private fun <T> runSuspend(block: suspend () -> T): T {
            var value: Any? = unset
            var failure: Throwable? = null
            block.startCoroutine(
                object : Continuation<T> {
                    override val context = EmptyCoroutineContext

                    override fun resumeWith(result: Result<T>) {
                        result.fold(
                            onSuccess = { value = it },
                            onFailure = { failure = it },
                        )
                    }
                },
            )
            failure?.let { throw it }
            if (value === unset) {
                error("Coroutine did not complete synchronously")
            }
            @Suppress("UNCHECKED_CAST")
            return value as T
        }
    }
}
