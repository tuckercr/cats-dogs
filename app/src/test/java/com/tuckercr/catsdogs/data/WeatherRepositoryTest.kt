package com.tuckercr.catsdogs.data

import com.tuckercr.catsdogs.data.remote.OpenWeatherApi
import com.tuckercr.catsdogs.data.remote.dto.CityDto
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneOffset

class WeatherRepositoryTest {

    @Test
    fun `current weather uses selected coordinates and display label`() = runBlocking {
        val api = FakeOpenWeatherApi()
        val repository = WeatherRepository(
            api = api,
            apiKey = " weather-key ",
            zoneId = ZoneOffset.UTC,
            json = Json { ignoreUnknownKeys = true },
        )

        val weather = repository.fetchCurrentWeather(
            units = WeatherUnits.IMPERIAL,
            locationLabel = " London, ON, CA ",
            cityQuery = "London",
            latitude = 42.9849,
            longitude = -81.2453,
        ).getOrThrow()

        assertEquals("London, ON, CA", weather.cityName)
        assertEquals("Broken clouds", weather.description)
        assertEquals(WeatherUnits.IMPERIAL, weather.units)

        val call = api.currentWeatherCalls.single()
        assertNull(call.cityQuery)
        assertEquals(42.9849, call.latitude!!, 0.0001)
        assertEquals(-81.2453, call.longitude!!, 0.0001)
        assertEquals("weather-key", call.apiKey)
        assertEquals("imperial", call.units)
    }

    @Test
    fun `forecast uses selected coordinates instead of ambiguous city query`() = runBlocking {
        val api = FakeOpenWeatherApi(
            forecastResponse = ForecastResponse(
                list = listOf(
                    forecastItem(
                        epochSeconds = 1_704_110_400L,
                        conditionMain = "Clouds",
                        description = "scattered clouds",
                    ),
                ),
                city = CityDto("London"),
            ),
        )
        val repository = WeatherRepository(
            api = api,
            apiKey = "weather-key",
            zoneId = ZoneOffset.UTC,
            json = Json { ignoreUnknownKeys = true },
        )

        val forecast = repository.fetchForecast(
            units = WeatherUnits.METRIC,
            cityQuery = "London",
            latitude = 42.9849,
            longitude = -81.2453,
        ).getOrThrow()

        assertEquals(1, forecast.size)
        assertEquals("Clouds", forecast.single().conditionMain)
        assertEquals("Scattered clouds", forecast.single().description)

        val call = api.forecastCalls.single()
        assertNull(call.cityQuery)
        assertEquals(42.9849, call.latitude!!, 0.0001)
        assertEquals(-81.2453, call.longitude!!, 0.0001)
        assertEquals("weather-key", call.apiKey)
        assertEquals("metric", call.units)
    }

    @Test
    fun `missing api key fails before calling weather api`() = runBlocking {
        val api = FakeOpenWeatherApi()
        val repository = WeatherRepository(
            api = api,
            apiKey = " ",
            zoneId = ZoneOffset.UTC,
            json = Json { ignoreUnknownKeys = true },
        )

        val currentError = repository.fetchCurrentWeather(
            units = WeatherUnits.METRIC,
            cityQuery = "London",
        ).exceptionOrNull()
        val forecastError = repository.fetchForecast(
            units = WeatherUnits.METRIC,
            cityQuery = "London",
        ).exceptionOrNull()

        assertEquals("missing_api_key", currentError?.message)
        assertEquals("missing_api_key", forecastError?.message)
        assertTrue(api.currentWeatherCalls.isEmpty())
        assertTrue(api.forecastCalls.isEmpty())
    }

    private class FakeOpenWeatherApi(
        private val currentWeatherResponse: CurrentWeatherResponse = sampleCurrentWeatherResponse(),
        private val forecastResponse: ForecastResponse = ForecastResponse(
            list = emptyList(),
            city = null,
        ),
    ) : OpenWeatherApi {

        val currentWeatherCalls = mutableListOf<WeatherCall>()
        val forecastCalls = mutableListOf<WeatherCall>()

        override suspend fun currentWeather(
            cityQuery: String?,
            latitude: Double?,
            longitude: Double?,
            apiKey: String,
            units: String,
        ): CurrentWeatherResponse {
            currentWeatherCalls += WeatherCall(cityQuery, latitude, longitude, apiKey, units)
            return currentWeatherResponse
        }

        override suspend fun forecast(
            cityQuery: String?,
            latitude: Double?,
            longitude: Double?,
            apiKey: String,
            units: String,
        ): ForecastResponse {
            forecastCalls += WeatherCall(cityQuery, latitude, longitude, apiKey, units)
            return forecastResponse
        }
    }

    private data class WeatherCall(
        val cityQuery: String?,
        val latitude: Double?,
        val longitude: Double?,
        val apiKey: String,
        val units: String,
    )

    companion object {
        private fun sampleCurrentWeatherResponse() = CurrentWeatherResponse(
            name = "London",
            weather = listOf(
                WeatherDescDto(
                    main = "Clouds",
                    description = "broken clouds",
                    icon = "04d",
                ),
            ),
            main = MainDto(
                temp = 15.0,
                feelsLike = 14.1,
                humidity = 72,
            ),
            wind = WindDto(speed = 4.2),
        )

        private fun forecastItem(
            epochSeconds: Long,
            conditionMain: String,
            description: String,
        ) = ForecastListItemDto(
            dt = epochSeconds,
            main = MainDto(
                temp = 12.0,
                feelsLike = 10.5,
                humidity = 68,
            ),
            weather = listOf(
                WeatherDescDto(
                    main = conditionMain,
                    description = description,
                    icon = "03d",
                ),
            ),
            wind = WindDto(speed = 3.0),
        )
    }
}
