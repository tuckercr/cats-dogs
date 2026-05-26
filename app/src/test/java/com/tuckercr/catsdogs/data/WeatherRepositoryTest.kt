package com.tuckercr.catsdogs.data

import com.tuckercr.catsdogs.data.remote.OpenWeatherApi
import com.tuckercr.catsdogs.data.remote.dto.CurrentWeatherResponse
import com.tuckercr.catsdogs.data.remote.dto.ForecastListItemDto
import com.tuckercr.catsdogs.data.remote.dto.ForecastResponse
import com.tuckercr.catsdogs.data.remote.dto.MainDto
import com.tuckercr.catsdogs.data.remote.dto.WeatherDescDto
import com.tuckercr.catsdogs.data.remote.dto.WindDto
import com.tuckercr.catsdogs.domain.WeatherUnits
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException
import java.time.ZoneOffset
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine

class WeatherRepositoryTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `fetchCurrentWeather validates api key before calling api`() {
        val api = FakeOpenWeatherApi()
        val repository = WeatherRepository(api, "   ", ZoneOffset.UTC, json)

        val result = runSuspend {
            repository.fetchCurrentWeather(WeatherUnits.METRIC, cityQuery = "Austin")
        }

        assertEquals("missing_api_key", result.exceptionOrNull()?.message)
        assertEquals(0, api.currentCalls.size)
    }

    @Test
    fun `fetchCurrentWeather trims city query and maps response with location label`() {
        val api = FakeOpenWeatherApi()
        val repository = WeatherRepository(api, " test-key ", ZoneOffset.UTC, json)

        val result = runSuspend {
            repository.fetchCurrentWeather(
                units = WeatherUnits.IMPERIAL,
                locationLabel = " Austin, TX, US ",
                cityQuery = " Austin ",
            )
        }

        val weather = result.getOrThrow()
        assertEquals("Austin, TX, US", weather.cityName)
        assertEquals("Clear", weather.conditionMain)
        assertEquals("Clear sky", weather.description)
        assertEquals("01d", weather.iconCode)
        assertEquals(72.5, weather.temperature, 0.0001)
        assertEquals(71.0, weather.feelsLike, 0.0001)
        assertEquals(45, weather.humidityPercent)
        assertEquals(6.2, weather.windSpeed, 0.0001)
        assertEquals(WeatherUnits.IMPERIAL, weather.units)
        assertEquals(
            WeatherCall(
                cityQuery = "Austin",
                latitude = null,
                longitude = null,
                apiKey = "test-key",
                units = "imperial",
            ),
            api.currentCalls.single(),
        )
    }

    @Test
    fun `fetchCurrentWeather uses coordinates instead of city query when both are present`() {
        val api = FakeOpenWeatherApi()
        val repository = WeatherRepository(api, "test-key", ZoneOffset.UTC, json)

        val result = runSuspend {
            repository.fetchCurrentWeather(
                units = WeatherUnits.METRIC,
                cityQuery = "Austin",
                latitude = 30.2672,
                longitude = -97.7431,
            )
        }

        result.getOrThrow()
        assertEquals(
            WeatherCall(
                cityQuery = null,
                latitude = 30.2672,
                longitude = -97.7431,
                apiKey = "test-key",
                units = "metric",
            ),
            api.currentCalls.single(),
        )
    }

    @Test
    fun `fetchCurrentWeather rejects blank query when coordinates are incomplete`() {
        val api = FakeOpenWeatherApi()
        val repository = WeatherRepository(api, "test-key", ZoneOffset.UTC, json)

        val result = runSuspend {
            repository.fetchCurrentWeather(
                units = WeatherUnits.METRIC,
                cityQuery = " ",
                latitude = 30.2672,
                longitude = null,
            )
        }

        assertEquals("empty_query", result.exceptionOrNull()?.message)
        assertEquals(0, api.currentCalls.size)
    }

    @Test
    fun `fetchCurrentWeather maps empty weather list to invalid payload`() {
        val api = FakeOpenWeatherApi(
            currentResponse = currentWeatherResponse(weather = emptyList()),
        )
        val repository = WeatherRepository(api, "test-key", ZoneOffset.UTC, json)

        val result = runSuspend {
            repository.fetchCurrentWeather(WeatherUnits.METRIC, cityQuery = "Austin")
        }

        assertEquals("invalid_payload", result.exceptionOrNull()?.message)
    }

    @Test
    fun `fetchCurrentWeather maps network and http failures to stable messages`() {
        val api = FakeOpenWeatherApi(currentThrowable = IOException("timeout"))
        val repository = WeatherRepository(api, "test-key", ZoneOffset.UTC, json)

        val networkResult = runSuspend {
            repository.fetchCurrentWeather(WeatherUnits.METRIC, cityQuery = "Austin")
        }

        assertEquals("network", networkResult.exceptionOrNull()?.message)

        api.currentThrowable = httpException(
            code = 401,
            body = """{"cod":"401","message":"Invalid API key"}""",
        )
        val httpResult = runSuspend {
            repository.fetchCurrentWeather(WeatherUnits.METRIC, cityQuery = "Austin")
        }

        assertEquals("Invalid API key", httpResult.exceptionOrNull()?.message)
    }

    @Test
    fun `fetchForecast routes coordinates and aggregates forecast slots`() {
        val api = FakeOpenWeatherApi()
        val repository = WeatherRepository(api, " test-key ", ZoneOffset.UTC, json)

        val result = runSuspend {
            repository.fetchForecast(
                units = WeatherUnits.METRIC,
                cityQuery = "Austin",
                latitude = 30.2672,
                longitude = -97.7431,
            )
        }

        val forecast = result.getOrThrow()
        assertEquals(1, forecast.size)
        assertEquals("Clouds", forecast.single().conditionMain)
        assertEquals("Few clouds", forecast.single().description)
        assertEquals(18.0, forecast.single().temperature, 0.0001)
        assertEquals(WeatherUnits.METRIC, forecast.single().units)
        assertEquals(
            WeatherCall(
                cityQuery = null,
                latitude = 30.2672,
                longitude = -97.7431,
                apiKey = "test-key",
                units = "metric",
            ),
            api.forecastCalls.single(),
        )
    }

    @Test
    fun `fetchForecast maps slot without weather to invalid payload`() {
        val api = FakeOpenWeatherApi(
            forecastResponse = forecastResponse(weather = emptyList()),
        )
        val repository = WeatherRepository(api, "test-key", ZoneOffset.UTC, json)

        val result = runSuspend {
            repository.fetchForecast(WeatherUnits.METRIC, cityQuery = "Austin")
        }

        assertEquals("invalid_payload", result.exceptionOrNull()?.message)
    }

    private class FakeOpenWeatherApi(
        var currentResponse: CurrentWeatherResponse = currentWeatherResponse(),
        var forecastResponse: ForecastResponse = forecastResponse(),
        var currentThrowable: Throwable? = null,
    ) : OpenWeatherApi {
        val currentCalls = mutableListOf<WeatherCall>()
        val forecastCalls = mutableListOf<WeatherCall>()

        override suspend fun currentWeather(
            cityQuery: String?,
            latitude: Double?,
            longitude: Double?,
            apiKey: String,
            units: String,
        ): CurrentWeatherResponse {
            currentCalls += WeatherCall(cityQuery, latitude, longitude, apiKey, units)
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

    private companion object {
        private val unset = Any()

        private fun currentWeatherResponse(
            weather: List<WeatherDescDto> = listOf(
                WeatherDescDto(main = "Clear", description = "clear sky", icon = "01d"),
            ),
        ) = CurrentWeatherResponse(
            name = "OpenWeather Austin",
            weather = weather,
            main = MainDto(temp = 72.5, feelsLike = 71.0, humidity = 45),
            wind = WindDto(speed = 6.2),
        )

        private fun forecastResponse(
            weather: List<WeatherDescDto> = listOf(
                WeatherDescDto(main = "Clouds", description = "few clouds", icon = "02d"),
            ),
        ) = ForecastResponse(
            list = listOf(
                ForecastListItemDto(
                    dt = 1_704_110_400L, // 2024-01-01T12:00:00Z
                    main = MainDto(temp = 18.0, feelsLike = 17.0, humidity = 60),
                    weather = weather,
                    wind = WindDto(speed = 3.0),
                ),
            ),
            city = null,
        )

        private fun httpException(
            code: Int,
            body: String,
        ): HttpException =
            HttpException(
                Response.error<Any>(
                    code,
                    body.toResponseBody("application/json".toMediaType()),
                ),
            )

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
