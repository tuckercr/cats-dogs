package com.tuckercr.catsdogs.data

import com.tuckercr.catsdogs.data.remote.OpenWeatherApi
import com.tuckercr.catsdogs.data.remote.dto.CurrentWeatherResponse
import com.tuckercr.catsdogs.data.remote.dto.ForecastListItemDto
import com.tuckercr.catsdogs.data.remote.dto.ForecastResponse
import com.tuckercr.catsdogs.data.remote.dto.MainDto
import com.tuckercr.catsdogs.data.remote.dto.WeatherDescDto
import com.tuckercr.catsdogs.data.remote.dto.WindDto
import com.tuckercr.catsdogs.domain.WeatherUnits
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.time.ZoneOffset

class WeatherRepositoryTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `fetchCurrentWeather trims query and maps successful city response`() {
        runBlocking {
            val api = FakeOpenWeatherApi()
            val repository = repository(api = api, apiKey = " test-key ")

            val result = repository.fetchCurrentWeather(
                units = WeatherUnits.IMPERIAL,
                cityQuery = " Austin ",
            )

            assertTrue(result.isSuccess)
            val weather = result.getOrThrow()
            assertEquals("Austin", api.currentRequest?.cityQuery)
            assertNull(api.currentRequest?.latitude)
            assertNull(api.currentRequest?.longitude)
            assertEquals("test-key", api.currentRequest?.apiKey)
            assertEquals("imperial", api.currentRequest?.units)
            assertEquals("Remote City", weather.cityName)
            assertEquals("Clear sky", weather.description)
            assertEquals(WeatherUnits.IMPERIAL, weather.units)
        }
    }

    @Test
    fun `fetchCurrentWeather uses coordinates and display label when provided`() {
        runBlocking {
            val api = FakeOpenWeatherApi()
            val repository = repository(api = api)

            val result = repository.fetchCurrentWeather(
                units = WeatherUnits.METRIC,
                locationLabel = " London, GB ",
                cityQuery = "ignored city",
                latitude = 51.5074,
                longitude = -0.1278,
            )

            assertTrue(result.isSuccess)
            val weather = result.getOrThrow()
            assertNull(api.currentRequest?.cityQuery)
            assertEquals(51.5074, api.currentRequest?.latitude ?: 0.0, 0.0001)
            assertEquals(-0.1278, api.currentRequest?.longitude ?: 0.0, 0.0001)
            assertEquals("London, GB", weather.cityName)
        }
    }

    @Test
    fun `fetchCurrentWeather rejects blank query before calling api`() {
        runBlocking {
            val api = FakeOpenWeatherApi()
            val repository = repository(api = api)

            val result = repository.fetchCurrentWeather(
                units = WeatherUnits.METRIC,
                cityQuery = "   ",
            )

            assertFalse(result.isSuccess)
            assertEquals("empty_query", result.exceptionOrNull()?.message)
            assertNull(api.currentRequest)
        }
    }

    @Test
    fun `fetchCurrentWeather rejects blank api key before calling api`() {
        runBlocking {
            val api = FakeOpenWeatherApi()
            val repository = repository(api = api, apiKey = "   ")

            val result = repository.fetchCurrentWeather(
                units = WeatherUnits.METRIC,
                cityQuery = "Austin",
            )

            assertFalse(result.isSuccess)
            assertEquals("missing_api_key", result.exceptionOrNull()?.message)
            assertNull(api.currentRequest)
        }
    }

    @Test
    fun `fetchForecast uses coordinates when provided`() {
        runBlocking {
            val api = FakeOpenWeatherApi()
            val repository = repository(api = api)

            val result = repository.fetchForecast(
                units = WeatherUnits.METRIC,
                cityQuery = "ignored city",
                latitude = 40.7128,
                longitude = -74.0060,
            )

            assertTrue(result.isSuccess)
            assertNull(api.forecastRequest?.cityQuery)
            assertEquals(40.7128, api.forecastRequest?.latitude ?: 0.0, 0.0001)
            assertEquals(-74.0060, api.forecastRequest?.longitude ?: 0.0, 0.0001)
            assertEquals("Clear sky", result.getOrThrow().single().description)
        }
    }

    @Test
    fun `fetchForecast maps io failures to network error`() {
        runBlocking {
            val api = FakeOpenWeatherApi(forecastFailure = IOException("timeout"))
            val repository = repository(api = api)

            val result = repository.fetchForecast(
                units = WeatherUnits.METRIC,
                cityQuery = "Austin",
            )

            assertFalse(result.isSuccess)
            assertEquals("network", result.exceptionOrNull()?.message)
        }
    }

    private fun repository(
        api: OpenWeatherApi,
        apiKey: String = "test-key",
    ): WeatherRepository =
        WeatherRepository(
            api = api,
            apiKey = apiKey,
            zoneId = ZoneOffset.UTC,
            json = json,
        )

    private class FakeOpenWeatherApi(
        private val currentFailure: Throwable? = null,
        private val forecastFailure: Throwable? = null,
    ) : OpenWeatherApi {
        var currentRequest: CurrentRequest? = null
        var forecastRequest: ForecastRequest? = null

        override suspend fun currentWeather(
            cityQuery: String?,
            latitude: Double?,
            longitude: Double?,
            apiKey: String,
            units: String,
        ): CurrentWeatherResponse {
            currentRequest = CurrentRequest(
                cityQuery = cityQuery,
                latitude = latitude,
                longitude = longitude,
                apiKey = apiKey,
                units = units,
            )
            currentFailure?.let { throw it }
            return currentResponse()
        }

        override suspend fun forecast(
            cityQuery: String?,
            latitude: Double?,
            longitude: Double?,
            apiKey: String,
            units: String,
        ): ForecastResponse {
            forecastRequest = ForecastRequest(
                cityQuery = cityQuery,
                latitude = latitude,
                longitude = longitude,
                apiKey = apiKey,
                units = units,
            )
            forecastFailure?.let { throw it }
            return forecastResponse()
        }

        data class CurrentRequest(
            val cityQuery: String?,
            val latitude: Double?,
            val longitude: Double?,
            val apiKey: String,
            val units: String,
        )

        data class ForecastRequest(
            val cityQuery: String?,
            val latitude: Double?,
            val longitude: Double?,
            val apiKey: String,
            val units: String,
        )
    }
}

private fun currentResponse(): CurrentWeatherResponse =
    CurrentWeatherResponse(
        name = "Remote City",
        weather = listOf(
            WeatherDescDto(
                main = "Clear",
                description = "clear sky",
                icon = "01d",
            ),
        ),
        main = MainDto(
            temp = 21.0,
            feelsLike = 20.5,
            humidity = 45,
        ),
        wind = WindDto(speed = 5.0),
    )

private fun forecastResponse(): ForecastResponse =
    ForecastResponse(
        list = listOf(
            ForecastListItemDto(
                dt = 1_704_110_400L, // 2024-01-01T12:00:00Z
                main = MainDto(
                    temp = 16.0,
                    feelsLike = 15.0,
                    humidity = 60,
                ),
                weather = listOf(
                    WeatherDescDto(
                        main = "Clear",
                        description = "clear sky",
                        icon = "01d",
                    ),
                ),
                wind = WindDto(speed = 3.0),
            ),
        ),
        city = null,
    )
