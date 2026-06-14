package com.tuckercr.catsdogs.model

import com.tuckercr.catsdogs.domain.CurrentWeather
import com.tuckercr.catsdogs.domain.DayForecast
import com.tuckercr.catsdogs.domain.WeatherUnits
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class WeatherForecastViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `current weather with coordinates ignores city query and saves resolved city`() =
        runTest {
            var request: CurrentRequest? = null
            val savedCities = mutableListOf<String>()
            val viewModel = viewModel(
                resolveWeatherUnits = { WeatherUnits.IMPERIAL },
                fetchCurrentWeather = { units, locationLabel, cityQuery, latitude, longitude ->
                    request = CurrentRequest(
                        units = units,
                        locationLabel = locationLabel,
                        cityQuery = cityQuery,
                        latitude = latitude,
                        longitude = longitude,
                    )
                    Result.success(currentWeather(cityName = "Austin, TX, US", units = units))
                },
                setLastCity = { savedCities += it },
            )

            viewModel.refreshCurrent(
                city = " Austin, TX, US ",
                latitude = 30.2672,
                longitude = -97.7431,
            )
            mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

            assertEquals(
                CurrentRequest(
                    units = WeatherUnits.IMPERIAL,
                    locationLabel = "Austin, TX, US",
                    cityQuery = null,
                    latitude = 30.2672,
                    longitude = -97.7431,
                ),
                request,
            )
            assertEquals("Austin, TX, US", viewModel.resolvedCity.value)
            assertEquals(listOf("Austin, TX, US"), savedCities)
            assertEquals(
                LoadingState.Success(currentWeather(cityName = "Austin, TX, US", units = WeatherUnits.IMPERIAL)),
                viewModel.currentWeather.value,
            )
        }

    @Test
    fun `latest current weather request wins when an earlier request finishes last`() =
        runTest {
            val firstRequest = CompletableDeferred<Result<CurrentWeather>>()
            val secondRequest = CompletableDeferred<Result<CurrentWeather>>()
            val requestedCities = mutableListOf<String>()
            val savedCities = mutableListOf<String>()
            val viewModel = viewModel(
                fetchCurrentWeather = { units, _, cityQuery, _, _ ->
                    val city = cityQuery ?: error("Expected city query")
                    requestedCities += city
                    when (city) {
                        "Austin" -> firstRequest.await()
                        "Denver" -> secondRequest.await()
                        else -> error("Unexpected city $city")
                    }.map { it.copy(units = units) }
                },
                setLastCity = { savedCities += it },
            )

            viewModel.refreshCurrent("Austin", latitude = null, longitude = null)
            mainDispatcherRule.testDispatcher.scheduler.runCurrent()
            viewModel.refreshCurrent("Denver", latitude = null, longitude = null)
            mainDispatcherRule.testDispatcher.scheduler.runCurrent()

            secondRequest.complete(Result.success(currentWeather(cityName = "Denver")))
            mainDispatcherRule.testDispatcher.scheduler.runCurrent()
            firstRequest.complete(Result.success(currentWeather(cityName = "Austin")))
            mainDispatcherRule.testDispatcher.scheduler.runCurrent()

            assertEquals(listOf("Austin", "Denver"), requestedCities)
            assertEquals(LoadingState.Success(currentWeather(cityName = "Denver")), viewModel.currentWeather.value)
            assertEquals("Denver", viewModel.resolvedCity.value)
            assertEquals(listOf("Denver"), savedCities)
        }

    @Test
    fun `blank current weather request fails without fetching and can be cleared`() =
        runTest {
            var fetchCount = 0
            val viewModel = viewModel(
                fetchCurrentWeather = { _, _, _, _, _ ->
                    fetchCount += 1
                    Result.success(currentWeather(cityName = "Austin"))
                },
            )

            viewModel.refreshCurrent("   ", latitude = null, longitude = null)

            assertEquals(0, fetchCount)
            assertEquals(
                LoadingState.Error("Please enter a city name.", canRetry = false),
                viewModel.currentWeather.value,
            )

            viewModel.clearCurrentError()

            assertEquals(LoadingState.Idle, viewModel.currentWeather.value)
        }

    @Test
    fun `forecast with coordinates ignores resolved city query`() =
        runTest {
            var request: ForecastRequest? = null
            val expectedForecast = listOf(dayForecast(units = WeatherUnits.IMPERIAL))
            val viewModel = viewModel(
                resolveWeatherUnits = { WeatherUnits.IMPERIAL },
                fetchForecast = { units, cityQuery, latitude, longitude ->
                    request = ForecastRequest(
                        units = units,
                        cityQuery = cityQuery,
                        latitude = latitude,
                        longitude = longitude,
                    )
                    Result.success(expectedForecast)
                },
            )

            viewModel.refreshForecast(
                resolvedCityName = " Austin, TX, US ",
                latitude = 30.2672,
                longitude = -97.7431,
            )
            mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

            assertEquals(
                ForecastRequest(
                    units = WeatherUnits.IMPERIAL,
                    cityQuery = null,
                    latitude = 30.2672,
                    longitude = -97.7431,
                ),
                request,
            )
            assertEquals(LoadingState.Success(expectedForecast), viewModel.forecast.value)
        }

    @Test
    fun `forecast maps network failure to retryable user message`() =
        runTest {
            val viewModel = viewModel(
                fetchForecast = { _, _, _, _ -> Result.failure(IOException("network")) },
            )

            viewModel.refreshForecast("Austin", latitude = null, longitude = null)
            mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

            assertEquals(
                LoadingState.Error(
                    "We could not reach the weather service. Check your connection and try again.",
                    canRetry = true,
                ),
                viewModel.forecast.value,
            )

            viewModel.clearForecastError()

            assertEquals(LoadingState.Idle, viewModel.forecast.value)
        }

    private fun viewModel(
        resolveWeatherUnits: () -> WeatherUnits = { WeatherUnits.METRIC },
        fetchCurrentWeather: suspend (
            units: WeatherUnits,
            locationLabel: String,
            cityQuery: String?,
            latitude: Double?,
            longitude: Double?,
        ) -> Result<CurrentWeather> = { units, locationLabel, cityQuery, _, _ ->
            Result.success(currentWeather(cityName = cityQuery ?: locationLabel, units = units))
        },
        fetchForecast: suspend (
            units: WeatherUnits,
            cityQuery: String?,
            latitude: Double?,
            longitude: Double?,
        ) -> Result<List<DayForecast>> = { units, _, _, _ ->
            Result.success(listOf(dayForecast(units = units)))
        },
        setLastCity: suspend (String) -> Unit = {},
    ) = WeatherForecastViewModel(
        resolveWeatherUnits = resolveWeatherUnits,
        fetchCurrentWeather = fetchCurrentWeather,
        fetchForecast = fetchForecast,
        setLastCity = setLastCity,
    )

    class MainDispatcherRule(
        val testDispatcher: TestDispatcher = StandardTestDispatcher(),
    ) : TestWatcher() {
        override fun starting(description: Description) {
            Dispatchers.setMain(testDispatcher)
        }

        override fun finished(description: Description) {
            Dispatchers.resetMain()
        }
    }

    private data class CurrentRequest(
        val units: WeatherUnits,
        val locationLabel: String,
        val cityQuery: String?,
        val latitude: Double?,
        val longitude: Double?,
    )

    private data class ForecastRequest(
        val units: WeatherUnits,
        val cityQuery: String?,
        val latitude: Double?,
        val longitude: Double?,
    )

    private companion object {
        fun currentWeather(
            cityName: String,
            units: WeatherUnits = WeatherUnits.METRIC,
        ) = CurrentWeather(
            cityName = cityName,
            conditionMain = "Clear",
            description = "Clear sky",
            iconCode = "01d",
            temperature = 72.5,
            feelsLike = 70.0,
            humidityPercent = 42,
            windSpeed = 5.5,
            units = units,
        )

        fun dayForecast(units: WeatherUnits = WeatherUnits.METRIC) =
            DayForecast(
                dateLabel = "Mon, Jan 1",
                conditionMain = "Clouds",
                description = "cloudy",
                iconCode = "02d",
                temperature = 68.0,
                feelsLike = 67.0,
                units = units,
            )
    }
}
