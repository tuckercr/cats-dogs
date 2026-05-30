package com.tuckercr.catsdogs.data

import com.tuckercr.catsdogs.data.remote.OpenWeatherApi
import com.tuckercr.catsdogs.data.remote.dto.CurrentWeatherResponse
import com.tuckercr.catsdogs.data.remote.dto.ForecastResponse
import com.tuckercr.catsdogs.data.remote.dto.MainDto
import com.tuckercr.catsdogs.data.remote.dto.WeatherDescDto
import com.tuckercr.catsdogs.data.remote.dto.WindDto
import com.tuckercr.catsdogs.domain.WeatherUnits
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.time.ZoneOffset

class WeatherRepositoryTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `fetchCurrentWeather with coordinates uses lat lon instead of city query`() = runBlocking {
        val api = RecordingOpenWeatherApi()
        val repository = WeatherRepository(
            api = api,
            apiKey = " test-key ",
            zoneId = ZoneOffset.UTC,
            json = json,
        )

        val result = repository.fetchCurrentWeather(
            units = WeatherUnits.IMPERIAL,
            locationLabel = "Springfield, IL, US",
            cityQuery = "Springfield",
            latitude = 39.7817,
            longitude = -89.6501,
        )

        assertTrue(result.isSuccess)
        assertNull(api.lastCurrentCityQuery)
        assertEquals(39.7817, api.lastCurrentLatitude ?: 0.0, 0.0001)
        assertEquals(-89.6501, api.lastCurrentLongitude ?: 0.0, 0.0001)
        assertEquals("test-key", api.lastCurrentApiKey)
        assertEquals("imperial", api.lastCurrentUnits)
        assertEquals("Springfield, IL, US", result.getOrThrow().cityName)
        assertEquals("Scattered clouds", result.getOrThrow().description)
    }

    @Test
    fun `fetchCurrentWeather maps io failures to network message`() = runBlocking {
        val api = RecordingOpenWeatherApi(currentFailure = IOException("socket closed"))
        val repository = WeatherRepository(
            api = api,
            apiKey = "test-key",
            zoneId = ZoneOffset.UTC,
            json = json,
        )

        val result = repository.fetchCurrentWeather(
            units = WeatherUnits.METRIC,
            cityQuery = "Austin",
        )

        assertTrue(result.isFailure)
        assertEquals("Austin", api.lastCurrentCityQuery)
        assertEquals("network", result.exceptionOrNull()?.message)
    }

    @Test
    fun `fetchForecast fails before api call when query is blank`() = runBlocking {
        val api = RecordingOpenWeatherApi()
        val repository = WeatherRepository(
            api = api,
            apiKey = "test-key",
            zoneId = ZoneOffset.UTC,
            json = json,
        )

        val result = repository.fetchForecast(
            units = WeatherUnits.METRIC,
            cityQuery = " ",
        )

        assertTrue(result.isFailure)
        assertEquals("empty_query", result.exceptionOrNull()?.message)
        assertEquals(0, api.forecastCallCount)
    }

    private class RecordingOpenWeatherApi(
        private val currentFailure: Throwable? = null,
    ) : OpenWeatherApi {
        var lastCurrentCityQuery: String? = null
            private set
        var lastCurrentLatitude: Double? = null
            private set
        var lastCurrentLongitude: Double? = null
            private set
        var lastCurrentApiKey: String? = null
            private set
        var lastCurrentUnits: String? = null
            private set
        var forecastCallCount = 0
            private set

        override suspend fun currentWeather(
            cityQuery: String?,
            latitude: Double?,
            longitude: Double?,
            apiKey: String,
            units: String,
        ): CurrentWeatherResponse {
            lastCurrentCityQuery = cityQuery
            lastCurrentLatitude = latitude
            lastCurrentLongitude = longitude
            lastCurrentApiKey = apiKey
            lastCurrentUnits = units
            currentFailure?.let { throw it }
            return CurrentWeatherResponse(
                name = "OpenWeather Springfield",
                weather = listOf(
                    WeatherDescDto(
                        main = "Clouds",
                        description = "scattered clouds",
                        icon = "03d",
                    ),
                ),
                main = MainDto(
                    temp = 68.0,
                    feelsLike = 66.0,
                    humidity = 45,
                ),
                wind = WindDto(speed = 9.5),
            )
        }

        override suspend fun forecast(
            cityQuery: String?,
            latitude: Double?,
            longitude: Double?,
            apiKey: String,
            units: String,
        ): ForecastResponse {
            forecastCallCount += 1
            return ForecastResponse(list = emptyList(), city = null)
        }
    }
}
