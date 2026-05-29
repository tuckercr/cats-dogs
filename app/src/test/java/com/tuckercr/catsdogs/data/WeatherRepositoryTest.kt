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
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneOffset

class WeatherRepositoryTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `fetch current weather trims city query and uses searched location label`() {
        runBlocking {
            val api = FakeOpenWeatherApi(
                currentWeatherResponse = currentWeatherResponse(
                    name = "OpenWeather Name",
                    description = "light rain",
                ),
            )
            val repository = WeatherRepository(api, apiKey = " api-key ", zoneId = ZoneOffset.UTC, json = json)

            val result = repository.fetchCurrentWeather(
                units = WeatherUnits.METRIC,
                locationLabel = " Paris, FR ",
                cityQuery = " Paris ",
            )

            val weather = result.getOrThrow()
            assertEquals("Paris, FR", weather.cityName)
            assertEquals("Light rain", weather.description)
            assertEquals(WeatherUnits.METRIC, weather.units)
            assertEquals(
                CurrentWeatherCall(
                    cityQuery = "Paris",
                    latitude = null,
                    longitude = null,
                    apiKey = "api-key",
                    units = "metric",
                ),
                api.currentWeatherCalls.single(),
            )
        }
    }

    @Test
    fun `fetch forecast uses coordinates when available instead of city query`() {
        runBlocking {
            val api = FakeOpenWeatherApi(
                forecastResponse = ForecastResponse(
                    list = listOf(
                        forecastItem(
                            epochSeconds = 1_704_110_400L, // 2024-01-01T12:00:00Z
                            conditionMain = "Clear",
                        ),
                    ),
                    city = null,
                ),
            )
            val repository = WeatherRepository(api, apiKey = "api-key", zoneId = ZoneOffset.UTC, json = json)

            val result = repository.fetchForecast(
                units = WeatherUnits.IMPERIAL,
                cityQuery = "Springfield",
                latitude = 39.7817,
                longitude = -89.6501,
            )

            val forecast = result.getOrThrow()
            assertEquals("Clear", forecast.single().conditionMain)
            assertEquals(
                ForecastCall(
                    cityQuery = null,
                    latitude = 39.7817,
                    longitude = -89.6501,
                    apiKey = "api-key",
                    units = "imperial",
                ),
                api.forecastCalls.single(),
            )
        }
    }

    @Test
    fun `missing API key fails current weather before calling API`() {
        runBlocking {
            val api = FakeOpenWeatherApi()
            val repository = WeatherRepository(api, apiKey = "  ", zoneId = ZoneOffset.UTC, json = json)

            val result = repository.fetchCurrentWeather(
                units = WeatherUnits.METRIC,
                cityQuery = "Paris",
            )

            assertEquals("missing_api_key", result.exceptionOrNull()?.message)
            assertTrue(api.currentWeatherCalls.isEmpty())
        }
    }

    @Test
    fun `blank city query without coordinates fails forecast before calling API`() {
        runBlocking {
            val api = FakeOpenWeatherApi()
            val repository = WeatherRepository(api, apiKey = "api-key", zoneId = ZoneOffset.UTC, json = json)

            val result = repository.fetchForecast(
                units = WeatherUnits.METRIC,
                cityQuery = "  ",
            )

            assertEquals("empty_query", result.exceptionOrNull()?.message)
            assertTrue(api.forecastCalls.isEmpty())
        }
    }

    @Test
    fun `invalid current weather payload returns failure`() {
        runBlocking {
            val api = FakeOpenWeatherApi(
                currentWeatherResponse = currentWeatherResponse(weather = emptyList()),
            )
            val repository = WeatherRepository(api, apiKey = "api-key", zoneId = ZoneOffset.UTC, json = json)

            val result = repository.fetchCurrentWeather(
                units = WeatherUnits.METRIC,
                cityQuery = "Paris",
            )

            assertEquals("invalid_payload", result.exceptionOrNull()?.message)
        }
    }

    private class FakeOpenWeatherApi(
        private val currentWeatherResponse: CurrentWeatherResponse = currentWeatherResponse(),
        private val forecastResponse: ForecastResponse = ForecastResponse(list = emptyList(), city = null),
    ) : OpenWeatherApi {
        val currentWeatherCalls = mutableListOf<CurrentWeatherCall>()
        val forecastCalls = mutableListOf<ForecastCall>()

        override suspend fun currentWeather(
            cityQuery: String?,
            latitude: Double?,
            longitude: Double?,
            apiKey: String,
            units: String,
        ): CurrentWeatherResponse {
            currentWeatherCalls += CurrentWeatherCall(cityQuery, latitude, longitude, apiKey, units)
            return currentWeatherResponse
        }

        override suspend fun forecast(
            cityQuery: String?,
            latitude: Double?,
            longitude: Double?,
            apiKey: String,
            units: String,
        ): ForecastResponse {
            forecastCalls += ForecastCall(cityQuery, latitude, longitude, apiKey, units)
            return forecastResponse
        }
    }

    private data class CurrentWeatherCall(
        val cityQuery: String?,
        val latitude: Double?,
        val longitude: Double?,
        val apiKey: String,
        val units: String,
    )

    private data class ForecastCall(
        val cityQuery: String?,
        val latitude: Double?,
        val longitude: Double?,
        val apiKey: String,
        val units: String,
    )

    private companion object {
        fun currentWeatherResponse(
            name: String = "Paris",
            description: String = "clear sky",
            weather: List<WeatherDescDto> = listOf(
                WeatherDescDto(main = "Clear", description = description, icon = "01d"),
            ),
        ): CurrentWeatherResponse =
            CurrentWeatherResponse(
                name = name,
                weather = weather,
                main = MainDto(temp = 21.3, feelsLike = 20.1, humidity = 55),
                wind = WindDto(speed = 4.2),
            )

        fun forecastItem(
            epochSeconds: Long,
            conditionMain: String,
        ): ForecastListItemDto =
            ForecastListItemDto(
                dt = epochSeconds,
                main = MainDto(temp = 21.3, feelsLike = 20.1, humidity = 55),
                weather = listOf(WeatherDescDto(main = conditionMain, description = "clear sky", icon = "01d")),
                wind = WindDto(speed = 4.2),
            )
    }
}
