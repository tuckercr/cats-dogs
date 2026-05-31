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
import org.junit.Test
import java.time.ZoneOffset

class WeatherRepositoryTest {

    @Test
    fun `fetchCurrentWeather with coordinates sends coordinates and preserves location label`() =
        runBlocking {
            val api = FakeOpenWeatherApi()
            val repository = WeatherRepository(api, " test-key ", ZoneOffset.UTC, Json)

            val result = repository.fetchCurrentWeather(
                units = WeatherUnits.IMPERIAL,
                locationLabel = "Austin, Texas, US",
                cityQuery = "Austin",
                latitude = 30.2672,
                longitude = -97.7431,
            )
            val weather = result.getOrThrow()

            assertEquals("Austin, Texas, US", weather.cityName)
            assertEquals("Clear sky", weather.description)
            assertEquals(WeatherUnits.IMPERIAL, weather.units)
            assertNull(api.lastCurrentCityQuery)
            assertEquals(30.2672, api.lastCurrentLatitude ?: 0.0, 0.0001)
            assertEquals(-97.7431, api.lastCurrentLongitude ?: 0.0, 0.0001)
            assertEquals("test-key", api.lastCurrentApiKey)
            assertEquals("imperial", api.lastCurrentUnits)
        }

    @Test
    fun `fetchCurrentWeather with blank city query fails before calling api`() =
        runBlocking {
            val api = FakeOpenWeatherApi()
            val repository = WeatherRepository(api, "test-key", ZoneOffset.UTC, Json)

            val result = repository.fetchCurrentWeather(
                units = WeatherUnits.METRIC,
                cityQuery = "   ",
            )

            val error = result.exceptionOrNull()
            assertEquals("empty_query", error?.message)
            assertEquals(0, api.currentWeatherCallCount)
        }

    @Test
    fun `fetchForecast with missing api key fails before calling api`() =
        runBlocking {
            val api = FakeOpenWeatherApi()
            val repository = WeatherRepository(api, "   ", ZoneOffset.UTC, Json)

            val result = repository.fetchForecast(
                units = WeatherUnits.METRIC,
                cityQuery = "Austin",
            )

            val error = result.exceptionOrNull()
            assertEquals("missing_api_key", error?.message)
            assertEquals(0, api.forecastCallCount)
        }

    private class FakeOpenWeatherApi : OpenWeatherApi {
        var currentWeatherCallCount = 0
        var forecastCallCount = 0
        var lastCurrentCityQuery: String? = null
        var lastCurrentLatitude: Double? = null
        var lastCurrentLongitude: Double? = null
        var lastCurrentApiKey: String? = null
        var lastCurrentUnits: String? = null

        override suspend fun currentWeather(
            cityQuery: String?,
            latitude: Double?,
            longitude: Double?,
            apiKey: String,
            units: String,
        ): CurrentWeatherResponse {
            currentWeatherCallCount += 1
            lastCurrentCityQuery = cityQuery
            lastCurrentLatitude = latitude
            lastCurrentLongitude = longitude
            lastCurrentApiKey = apiKey
            lastCurrentUnits = units
            return CurrentWeatherResponse(
                name = "OpenWeather Austin",
                weather = listOf(
                    WeatherDescDto(
                        main = "Clear",
                        description = "clear sky",
                        icon = "01d",
                    ),
                ),
                main = MainDto(
                    temp = 72.5,
                    feelsLike = 70.0,
                    humidity = 42,
                ),
                wind = WindDto(speed = 5.5),
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
            throw AssertionError("Forecast API should not be called by this test")
        }
    }
}
