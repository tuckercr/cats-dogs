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
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException
import java.time.ZoneOffset

class WeatherRepositoryTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `fetchCurrentWeather rejects blank api key before calling api`() =
        runBlocking {
            val api = FakeOpenWeatherApi()
            val repository = repository(api = api, apiKey = "  ")

            val result = repository.fetchCurrentWeather(
                units = WeatherUnits.METRIC,
                cityQuery = "Austin",
            )

            assertTrue(result.isFailure)
            assertEquals("missing_api_key", result.exceptionOrNull()?.message)
            assertEquals(0, api.currentCalls)
        }

    @Test
    fun `fetchCurrentWeather uses coordinates instead of city query and preserves selected label`() =
        runBlocking {
            val api = FakeOpenWeatherApi(
                currentResponse = currentWeatherResponse(name = "London"),
            )
            val repository = repository(api = api)

            val result = repository.fetchCurrentWeather(
                units = WeatherUnits.IMPERIAL,
                locationLabel = "London, ON, CA",
                cityQuery = "London",
                latitude = 42.9849,
                longitude = -81.2453,
            )

            assertTrue(result.isSuccess)
            assertEquals("London, ON, CA", result.getOrThrow().cityName)
            assertEquals(WeatherUnits.IMPERIAL, result.getOrThrow().units)
            assertNull(api.lastCurrentCityQuery)
            assertEquals(42.9849, api.lastCurrentLatitude!!, 0.0001)
            assertEquals(-81.2453, api.lastCurrentLongitude!!, 0.0001)
            assertEquals("imperial", api.lastCurrentUnits)
        }

    @Test
    fun `fetchCurrentWeather fails invalid payload when weather descriptions are absent`() =
        runBlocking {
            val api = FakeOpenWeatherApi(
                currentResponse = currentWeatherResponse(weather = emptyList()),
            )
            val repository = repository(api = api)

            val result = repository.fetchCurrentWeather(
                units = WeatherUnits.METRIC,
                cityQuery = "Austin",
            )

            assertTrue(result.isFailure)
            assertEquals("invalid_payload", result.exceptionOrNull()?.message)
        }

    @Test
    fun `fetchForecast maps forecast slots into day forecasts`() =
        runBlocking {
            val api = FakeOpenWeatherApi(
                forecastResponse = ForecastResponse(
                    list = listOf(
                        forecastItem(
                            dt = 1_704_067_200L + 1 * 3600,
                            temp = 2.0,
                            condition = "Morning",
                        ),
                        forecastItem(
                            dt = 1_704_067_200L + 12 * 3600,
                            temp = 10.0,
                            condition = "Noon",
                        ),
                    ),
                    city = null,
                ),
            )
            val repository = repository(api = api)

            val result = repository.fetchForecast(
                units = WeatherUnits.METRIC,
                cityQuery = "Austin",
            )

            assertTrue(result.isSuccess)
            val days = result.getOrThrow()
            assertEquals(1, days.size)
            assertEquals("Noon", days.single().conditionMain)
            assertEquals(10.0, days.single().temperature, 0.0001)
            assertEquals("Austin", api.lastForecastCityQuery)
            assertEquals("metric", api.lastForecastUnits)
        }

    @Test
    fun `fetchCurrentWeather maps io failures to network`() =
        runBlocking {
            val api = FakeOpenWeatherApi(currentThrowable = IOException("timeout"))
            val repository = repository(api = api)

            val result = repository.fetchCurrentWeather(
                units = WeatherUnits.METRIC,
                cityQuery = "Austin",
            )

            assertTrue(result.isFailure)
            val error = result.exceptionOrNull()
            assertTrue(error is IOException)
            assertEquals("network", error?.message)
            assertEquals("timeout", error?.cause?.message)
        }

    @Test
    fun `fetchCurrentWeather maps openweather error response message`() =
        runBlocking {
            val errorBody = """{"cod":"404","message":"city not found"}"""
                .toResponseBody("application/json".toMediaType())
            val response = Response.error<CurrentWeatherResponse>(404, errorBody)
            val api = FakeOpenWeatherApi(currentThrowable = HttpException(response))
            val repository = repository(api = api)

            val result = repository.fetchCurrentWeather(
                units = WeatherUnits.METRIC,
                cityQuery = "Missing City",
            )

            assertTrue(result.isFailure)
            assertEquals("city not found", result.exceptionOrNull()?.message)
        }

    private fun repository(
        api: OpenWeatherApi,
        apiKey: String = "test-key",
    ) = WeatherRepository(
        api = api,
        apiKey = apiKey,
        zoneId = ZoneOffset.UTC,
        json = json,
    )

    private class FakeOpenWeatherApi(
        private val currentResponse: CurrentWeatherResponse = currentWeatherResponse(),
        private val forecastResponse: ForecastResponse = ForecastResponse(list = emptyList(), city = null),
        private val currentThrowable: Throwable? = null,
    ) : OpenWeatherApi {
        var currentCalls = 0
            private set
        var lastCurrentCityQuery: String? = null
            private set
        var lastCurrentLatitude: Double? = null
            private set
        var lastCurrentLongitude: Double? = null
            private set
        var lastCurrentUnits: String? = null
            private set
        var lastForecastCityQuery: String? = null
            private set
        var lastForecastUnits: String? = null
            private set

        override suspend fun currentWeather(
            cityQuery: String?,
            latitude: Double?,
            longitude: Double?,
            apiKey: String,
            units: String,
        ): CurrentWeatherResponse {
            currentCalls += 1
            lastCurrentCityQuery = cityQuery
            lastCurrentLatitude = latitude
            lastCurrentLongitude = longitude
            lastCurrentUnits = units
            currentThrowable?.let { throw it }
            return currentResponse
        }

        override suspend fun forecast(
            cityQuery: String?,
            latitude: Double?,
            longitude: Double?,
            apiKey: String,
            units: String,
        ): ForecastResponse {
            lastForecastCityQuery = cityQuery
            lastForecastUnits = units
            assertFalse(apiKey.isBlank())
            return forecastResponse
        }
    }

    private companion object {
        fun currentWeatherResponse(
            name: String = "Austin",
            weather: List<WeatherDescDto> = listOf(
                WeatherDescDto(
                    main = "Clear",
                    description = "clear sky",
                    icon = "01d",
                ),
            ),
        ) = CurrentWeatherResponse(
            name = name,
            weather = weather,
            main = MainDto(temp = 22.0, feelsLike = 21.0, humidity = 60),
            wind = WindDto(speed = 3.0),
        )

        fun forecastItem(
            dt: Long,
            temp: Double,
            condition: String,
        ) = ForecastListItemDto(
            dt = dt,
            main = MainDto(temp = temp, feelsLike = temp - 1.0, humidity = 70),
            weather = listOf(
                WeatherDescDto(
                    main = condition,
                    description = condition.lowercase(),
                    icon = "01d",
                ),
            ),
            wind = WindDto(speed = 4.0),
        )
    }
}
