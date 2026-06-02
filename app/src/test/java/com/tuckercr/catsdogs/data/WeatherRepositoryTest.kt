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
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException
import java.time.ZoneOffset

class WeatherRepositoryTest {

    @Test
    fun `fetchCurrentWeather with coordinates sends coordinates and preserves location label`() =
        runBlocking {
            val api = FakeOpenWeatherApi()
            val repository = WeatherRepository(api, " test-key ", ZoneOffset.UTC, Json)

            val result = repository.fetchCurrentWeather(
                units = WeatherUnits.IMPERIAL,
                locationLabel = "Austin, TX, US",
                cityQuery = "Austin",
                latitude = 30.2672,
                longitude = -97.7431,
            )
            val weather = result.getOrThrow()

            assertEquals("Austin, TX, US", weather.cityName)
            assertEquals("Clear sky", weather.description)
            assertEquals(WeatherUnits.IMPERIAL, weather.units)
            assertNull(api.lastCurrentCityQuery)
            assertEquals(30.2672, api.lastCurrentLatitude ?: 0.0, 0.0001)
            assertEquals(-97.7431, api.lastCurrentLongitude ?: 0.0, 0.0001)
            assertEquals("test-key", api.lastCurrentApiKey)
            assertEquals("imperial", api.lastCurrentUnits)
        }

    @Test
    fun `fetchCurrentWeather trims city query when coordinates are absent`() =
        runBlocking {
            val api = FakeOpenWeatherApi()
            val repository = WeatherRepository(api, "test-key", ZoneOffset.UTC, Json)

            val result = repository.fetchCurrentWeather(
                units = WeatherUnits.METRIC,
                cityQuery = "  Denver  ",
            )
            val weather = result.getOrThrow()

            assertEquals("OpenWeather City", weather.cityName)
            assertEquals("Denver", api.lastCurrentCityQuery)
            assertNull(api.lastCurrentLatitude)
            assertNull(api.lastCurrentLongitude)
            assertEquals("metric", api.lastCurrentUnits)
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

    @Test
    fun `fetchForecast with blank city query fails before calling api`() =
        runBlocking {
            val api = FakeOpenWeatherApi()
            val repository = WeatherRepository(api, "test-key", ZoneOffset.UTC, Json)

            val result = repository.fetchForecast(
                units = WeatherUnits.METRIC,
                cityQuery = "   ",
            )

            val error = result.exceptionOrNull()
            assertEquals("empty_query", error?.message)
            assertEquals(0, api.forecastCallCount)
        }

    @Test
    fun `fetchForecast maps forecast slots through aggregator`() =
        runBlocking {
            val api = FakeOpenWeatherApi(
                forecastResponse = ForecastResponse(
                    list = listOf(
                        forecastItem(
                            epochSeconds = 1_704_067_200L + 3600,
                            temperature = 1.0,
                            main = "Morning",
                        ),
                        forecastItem(
                            epochSeconds = 1_704_067_200L + 12 * 3600,
                            temperature = 10.0,
                            main = "Noon",
                        ),
                    ),
                    city = null,
                ),
            )
            val repository = WeatherRepository(api, "test-key", ZoneOffset.UTC, Json)

            val result = repository.fetchForecast(
                units = WeatherUnits.METRIC,
                latitude = 30.2672,
                longitude = -97.7431,
            )
            val forecast = result.getOrThrow()

            assertEquals(1, forecast.size)
            assertEquals("Noon", forecast.single().conditionMain)
            assertEquals("Noon description", forecast.single().description)
            assertEquals(10.0, forecast.single().temperature, 0.0001)
            assertEquals(WeatherUnits.METRIC, forecast.single().units)
            assertNull(api.lastForecastCityQuery)
            assertEquals(30.2672, api.lastForecastLatitude ?: 0.0, 0.0001)
            assertEquals(-97.7431, api.lastForecastLongitude ?: 0.0, 0.0001)
        }

    @Test
    fun `fetchForecast maps network failures to network message`() =
        runBlocking {
            val api = FakeOpenWeatherApi(forecastThrowable = IOException("timeout"))
            val repository = WeatherRepository(api, "test-key", ZoneOffset.UTC, Json)

            val result = repository.fetchForecast(
                units = WeatherUnits.METRIC,
                cityQuery = "Austin",
            )

            val error = result.exceptionOrNull()
            assertEquals("network", error?.message)
        }

    @Test
    fun `fetchForecast maps http failures without error message to status code`() =
        runBlocking {
            val api = FakeOpenWeatherApi(
                forecastThrowable = httpException(
                    code = 500,
                    body = """{"cod":"500","message":"   "}""",
                ),
            )
            val repository = WeatherRepository(api, "test-key", ZoneOffset.UTC, Json)

            val result = repository.fetchForecast(
                units = WeatherUnits.METRIC,
                cityQuery = "Austin",
            )

            val error = result.exceptionOrNull()
            assertEquals("http_500", error?.message)
        }

    @Test
    fun `fetchForecast maps missing weather entry to invalid payload`() =
        runBlocking {
            val api = FakeOpenWeatherApi(
                forecastResponse = ForecastResponse(
                    list = listOf(
                        ForecastListItemDto(
                            dt = 1_704_067_200L,
                            main = MainDto(
                                temp = 21.0,
                                feelsLike = 20.0,
                                humidity = 55,
                            ),
                            weather = emptyList(),
                            wind = WindDto(speed = 4.2),
                        ),
                    ),
                    city = null,
                ),
            )
            val repository = WeatherRepository(api, "test-key", ZoneOffset.UTC, Json)

            val result = repository.fetchForecast(
                units = WeatherUnits.METRIC,
                cityQuery = "Broken City",
            )

            val error = result.exceptionOrNull()
            assertEquals("invalid_payload", error?.message)
        }

    @Test
    fun `fetchCurrentWeather maps network failures to network message`() =
        runBlocking {
            val api = FakeOpenWeatherApi(currentThrowable = IOException("timeout"))
            val repository = WeatherRepository(api, "test-key", ZoneOffset.UTC, Json)

            val result = repository.fetchCurrentWeather(
                units = WeatherUnits.METRIC,
                cityQuery = "Austin",
            )

            val error = result.exceptionOrNull()
            assertEquals("network", error?.message)
        }

    @Test
    fun `fetchCurrentWeather maps OpenWeather error body message`() =
        runBlocking {
            val api = FakeOpenWeatherApi(
                currentThrowable = httpException(
                    code = 404,
                    body = """{"cod":"404","message":"city not found"}""",
                ),
            )
            val repository = WeatherRepository(api, "test-key", ZoneOffset.UTC, Json)

            val result = repository.fetchCurrentWeather(
                units = WeatherUnits.METRIC,
                cityQuery = "Missing City",
            )

            val error = result.exceptionOrNull()
            assertEquals("city not found", error?.message)
        }

    @Test
    fun `fetchCurrentWeather maps missing weather entry to invalid payload`() =
        runBlocking {
            val api = FakeOpenWeatherApi(
                currentResponse = CurrentWeatherResponse(
                    name = "Broken City",
                    weather = emptyList(),
                    main = MainDto(
                        temp = 21.0,
                        feelsLike = 20.0,
                        humidity = 55,
                    ),
                    wind = WindDto(speed = 4.2),
                ),
            )
            val repository = WeatherRepository(api, "test-key", ZoneOffset.UTC, Json)

            val result = repository.fetchCurrentWeather(
                units = WeatherUnits.METRIC,
                cityQuery = "Broken City",
            )

            val error = result.exceptionOrNull()
            assertEquals("invalid_payload", error?.message)
        }

    private class FakeOpenWeatherApi(
        private val currentResponse: CurrentWeatherResponse = currentWeatherResponse(),
        private val forecastResponse: ForecastResponse = ForecastResponse(
            list = emptyList(),
            city = null,
        ),
        private val currentThrowable: Throwable? = null,
        private val forecastThrowable: Throwable? = null,
    ) : OpenWeatherApi {
        var currentWeatherCallCount = 0
        var forecastCallCount = 0
        var lastCurrentCityQuery: String? = null
        var lastCurrentLatitude: Double? = null
        var lastCurrentLongitude: Double? = null
        var lastCurrentApiKey: String? = null
        var lastCurrentUnits: String? = null
        var lastForecastCityQuery: String? = null
        var lastForecastLatitude: Double? = null
        var lastForecastLongitude: Double? = null

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
            forecastCallCount += 1
            lastForecastCityQuery = cityQuery
            lastForecastLatitude = latitude
            lastForecastLongitude = longitude
            forecastThrowable?.let { throw it }
            return forecastResponse
        }
    }

    private companion object {
        fun currentWeatherResponse() =
            CurrentWeatherResponse(
                name = "OpenWeather City",
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

        fun forecastItem(
            epochSeconds: Long,
            temperature: Double,
            main: String,
        ) = ForecastListItemDto(
            dt = epochSeconds,
            main = MainDto(
                temp = temperature,
                feelsLike = temperature - 1.0,
                humidity = 50,
            ),
            weather = listOf(
                WeatherDescDto(
                    main = main,
                    description = "${main.lowercase()} description",
                    icon = "01d",
                ),
            ),
            wind = WindDto(speed = 1.0),
        )

        fun httpException(
            code: Int,
            body: String,
        ) = HttpException(
            Response.error<CurrentWeatherResponse>(
                code,
                body.toResponseBody(),
            ),
        )
    }
}
