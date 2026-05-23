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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.time.ZoneOffset

class WeatherRepositoryTest {

    @Test
    fun `fetchCurrentWeather returns missing api key without calling api`() = runBlocking {
        val api = FakeOpenWeatherApi()
        val repository = repository(api, apiKey = "   ")

        val result = repository.fetchCurrentWeather(
            units = WeatherUnits.METRIC,
            cityQuery = "Austin",
        )

        assertTrue(result.isFailure)
        assertEquals("missing_api_key", result.exceptionOrNull()?.message)
        assertTrue(api.currentWeatherCalls.isEmpty())
    }

    @Test
    fun `fetchCurrentWeather trims request values and prefers provided location label`() = runBlocking {
        val api = FakeOpenWeatherApi()
        val repository = repository(api, apiKey = "  test-key  ")

        val result = repository.fetchCurrentWeather(
            units = WeatherUnits.IMPERIAL,
            locationLabel = "  Austin, TX  ",
            cityQuery = "  Austin  ",
        )

        val weather = result.getOrThrow()
        val call = api.currentWeatherCalls.single()
        assertEquals("Austin", call.cityQuery)
        assertNull(call.latitude)
        assertNull(call.longitude)
        assertEquals("test-key", call.apiKey)
        assertEquals(WeatherUnits.IMPERIAL.units, call.units)
        assertEquals("Austin, TX", weather.cityName)
        assertEquals("Clear sky", weather.description)
        assertEquals(72.5, weather.temperature, 0.0001)
        assertEquals(WeatherUnits.IMPERIAL, weather.units)
    }

    @Test
    fun `fetchForecast uses coordinates instead of city query when provided`() = runBlocking {
        val api = FakeOpenWeatherApi()
        val repository = repository(api, apiKey = "test-key")

        val result = repository.fetchForecast(
            units = WeatherUnits.METRIC,
            cityQuery = "Austin",
            latitude = 30.2672,
            longitude = -97.7431,
        )

        val forecast = result.getOrThrow()
        val call = api.forecastCalls.single()
        assertNull(call.cityQuery)
        assertEquals(30.2672, call.latitude!!, 0.0001)
        assertEquals(-97.7431, call.longitude!!, 0.0001)
        assertEquals("test-key", call.apiKey)
        assertEquals(WeatherUnits.METRIC.units, call.units)
        assertEquals(1, forecast.size)
        assertEquals("Noon", forecast.single().conditionMain)
        assertEquals("Light rain", forecast.single().description)
    }

    @Test
    fun `fetchForecast maps OpenWeather error body to failure message`() = runBlocking {
        val api = FakeOpenWeatherApi().apply {
            forecastError = httpException(
                code = 404,
                body = """{"cod":"404","message":"city not found"}""",
            )
        }
        val repository = repository(api, apiKey = "test-key")

        val result = repository.fetchForecast(
            units = WeatherUnits.METRIC,
            cityQuery = "Missing City",
        )

        assertTrue(result.isFailure)
        assertEquals("city not found", result.exceptionOrNull()?.message)
    }

    private fun repository(
        api: OpenWeatherApi,
        apiKey: String,
    ): WeatherRepository =
        WeatherRepository(
            api = api,
            apiKey = apiKey,
            zoneId = ZoneOffset.UTC,
            json = Json { ignoreUnknownKeys = true },
        )

    private inner class FakeOpenWeatherApi : OpenWeatherApi {
        val currentWeatherCalls = mutableListOf<ApiCall>()
        val forecastCalls = mutableListOf<ApiCall>()
        var currentWeatherError: Throwable? = null
        var forecastError: Throwable? = null

        override suspend fun currentWeather(
            cityQuery: String?,
            latitude: Double?,
            longitude: Double?,
            apiKey: String,
            units: String,
        ): CurrentWeatherResponse {
            currentWeatherCalls += ApiCall(cityQuery, latitude, longitude, apiKey, units)
            currentWeatherError?.let { throw it }
            return currentWeatherResponse()
        }

        override suspend fun forecast(
            cityQuery: String?,
            latitude: Double?,
            longitude: Double?,
            apiKey: String,
            units: String,
        ): ForecastResponse {
            forecastCalls += ApiCall(cityQuery, latitude, longitude, apiKey, units)
            forecastError?.let { throw it }
            return forecastResponse()
        }
    }

    private data class ApiCall(
        val cityQuery: String?,
        val latitude: Double?,
        val longitude: Double?,
        val apiKey: String,
        val units: String,
    )

    private fun currentWeatherResponse(): CurrentWeatherResponse =
        CurrentWeatherResponse(
            name = "Austin",
            weather = listOf(
                WeatherDescDto(
                    main = "Clear",
                    description = "clear sky",
                    icon = "01d",
                ),
            ),
            main = MainDto(
                temp = 72.5,
                feelsLike = 71.0,
                humidity = 45,
            ),
            wind = WindDto(speed = 7.2),
        )

    private fun forecastResponse(): ForecastResponse =
        ForecastResponse(
            list = listOf(
                forecastItem(
                    epochSeconds = 1_704_071_600L,
                    conditionMain = "Morning",
                    description = "mist",
                ),
                forecastItem(
                    epochSeconds = 1_704_110_400L,
                    conditionMain = "Noon",
                    description = "light rain",
                ),
                forecastItem(
                    epochSeconds = 1_704_132_000L,
                    conditionMain = "Evening",
                    description = "cloudy",
                ),
            ),
            city = null,
        )

    private fun forecastItem(
        epochSeconds: Long,
        conditionMain: String,
        description: String,
    ): ForecastListItemDto =
        ForecastListItemDto(
            dt = epochSeconds,
            main = MainDto(
                temp = 60.0,
                feelsLike = 58.0,
                humidity = 70,
            ),
            weather = listOf(
                WeatherDescDto(
                    main = conditionMain,
                    description = description,
                    icon = "10d",
                ),
            ),
            wind = WindDto(speed = 4.0),
        )

    private fun httpException(
        code: Int,
        body: String,
    ): HttpException {
        val responseBody = body.toResponseBody("application/json".toMediaType())
        return HttpException(Response.error<Any>(code, responseBody))
    }
}
