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
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description

@OptIn(ExperimentalCoroutinesApi::class)
class WeatherForecastViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `blank current weather search fails before fetching`() =
        runTest {
            var fetchCalls = 0
            val viewModel = viewModel(
                fetchCurrentWeather = { _, _, _, _, _ ->
                    fetchCalls += 1
                    Result.success(currentWeather())
                },
            )

            viewModel.refreshCurrent(city = "   ", latitude = null, longitude = null)

            assertEquals(
                LoadingState.Error("Please enter a city name.", canRetry = false),
                viewModel.currentWeather.value,
            )
            assertEquals(0, fetchCalls)
        }

    @Test
    fun `current weather with pinned coordinates ignores city query and saves resolved city`() =
        runTest {
            val requests = mutableListOf<CurrentRequest>()
            val savedCities = mutableListOf<String>()
            val viewModel = viewModel(
                resolveUnits = { WeatherUnits.IMPERIAL },
                fetchCurrentWeather = { units, locationLabel, cityQuery, latitude, longitude ->
                    requests += CurrentRequest(units, locationLabel, cityQuery, latitude, longitude)
                    Result.success(currentWeather(cityName = "Austin, TX, US"))
                },
                saveLastCity = { savedCities += it },
            )

            viewModel.refreshCurrent(
                city = "  Austin, TX, US  ",
                latitude = 30.2672,
                longitude = -97.7431,
            )
            mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

            assertEquals(
                listOf(
                    CurrentRequest(
                        units = WeatherUnits.IMPERIAL,
                        locationLabel = "Austin, TX, US",
                        cityQuery = null,
                        latitude = 30.2672,
                        longitude = -97.7431,
                    ),
                ),
                requests,
            )
            assertEquals("Austin, TX, US", viewModel.resolvedCity.value)
            assertEquals(listOf("Austin, TX, US"), savedCities)
            assertEquals(
                LoadingState.Success(currentWeather(cityName = "Austin, TX, US")),
                viewModel.currentWeather.value,
            )
        }

    @Test
    fun `latest current weather request wins when older response finishes last`() =
        runTest {
            val firstSearch = CompletableDeferred<Result<CurrentWeather>>()
            val secondSearch = CompletableDeferred<Result<CurrentWeather>>()
            val requests = mutableListOf<String?>()
            val savedCities = mutableListOf<String>()
            val viewModel = viewModel(
                fetchCurrentWeather = { _, _, cityQuery, _, _ ->
                    requests += cityQuery
                    when (cityQuery) {
                        "Austin" -> firstSearch.await()
                        "Denver" -> secondSearch.await()
                        else -> error("Unexpected city query $cityQuery")
                    }
                },
                saveLastCity = { savedCities += it },
            )

            viewModel.refreshCurrent(city = "Austin", latitude = null, longitude = null)
            mainDispatcherRule.testDispatcher.scheduler.runCurrent()
            assertEquals(listOf("Austin"), requests)
            assertEquals(LoadingState.Loading, viewModel.currentWeather.value)

            viewModel.refreshCurrent(city = "Denver", latitude = null, longitude = null)
            firstSearch.complete(Result.success(currentWeather(cityName = "Austin")))
            mainDispatcherRule.testDispatcher.scheduler.runCurrent()

            assertEquals(listOf("Austin", "Denver"), requests)
            assertEquals(LoadingState.Loading, viewModel.currentWeather.value)
            assertNull(viewModel.resolvedCity.value)
            assertEquals(emptyList<String>(), savedCities)

            secondSearch.complete(Result.success(currentWeather(cityName = "Denver")))
            mainDispatcherRule.testDispatcher.scheduler.runCurrent()

            assertEquals("Denver", viewModel.resolvedCity.value)
            assertEquals(listOf("Denver"), savedCities)
            assertEquals(
                LoadingState.Success(currentWeather(cityName = "Denver")),
                viewModel.currentWeather.value,
            )
        }

    @Test
    fun `blank forecast search fails before fetching`() =
        runTest {
            var fetchCalls = 0
            val viewModel = viewModel(
                fetchForecast = { _, _, _, _ ->
                    fetchCalls += 1
                    Result.success(listOf(dayForecast()))
                },
            )

            viewModel.refreshForecast(resolvedCityName = "   ", latitude = null, longitude = null)

            assertEquals(
                LoadingState.Error("Load current weather first to pick a location.", canRetry = false),
                viewModel.forecast.value,
            )
            assertEquals(0, fetchCalls)
        }

    @Test
    fun `forecast errors are mapped to retryable user messages`() =
        runTest {
            val requests = mutableListOf<ForecastRequest>()
            val viewModel = viewModel(
                resolveUnits = { WeatherUnits.METRIC },
                fetchForecast = { units, cityQuery, latitude, longitude ->
                    requests += ForecastRequest(units, cityQuery, latitude, longitude)
                    Result.failure(IllegalStateException("network"))
                },
            )

            viewModel.refreshForecast(
                resolvedCityName = "  Austin  ",
                latitude = null,
                longitude = null,
            )
            mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

            assertEquals(
                listOf(
                    ForecastRequest(
                        units = WeatherUnits.METRIC,
                        cityQuery = "Austin",
                        latitude = null,
                        longitude = null,
                    ),
                ),
                requests,
            )
            assertEquals(
                LoadingState.Error(
                    message = "We could not reach the weather service. Check your connection and try again.",
                    canRetry = true,
                ),
                viewModel.forecast.value,
            )
        }

    private fun viewModel(
        resolveUnits: () -> WeatherUnits = { WeatherUnits.METRIC },
        fetchCurrentWeather: suspend (
            units: WeatherUnits,
            locationLabel: String,
            cityQuery: String?,
            latitude: Double?,
            longitude: Double?,
        ) -> Result<CurrentWeather> = { _, _, _, _, _ -> Result.success(currentWeather()) },
        fetchForecast: suspend (
            units: WeatherUnits,
            cityQuery: String?,
            latitude: Double?,
            longitude: Double?,
        ) -> Result<List<DayForecast>> = { _, _, _, _ -> Result.success(listOf(dayForecast())) },
        saveLastCity: suspend (String) -> Unit = {},
    ) = WeatherForecastViewModel(
        resolveUnits = resolveUnits,
        fetchCurrentWeather = fetchCurrentWeather,
        fetchForecast = fetchForecast,
        saveLastCity = saveLastCity,
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
        fun currentWeather(cityName: String = "Austin") =
            CurrentWeather(
                cityName = cityName,
                conditionMain = "Clear",
                description = "Clear sky",
                iconCode = "01d",
                temperature = 72.5,
                feelsLike = 70.0,
                humidityPercent = 42,
                windSpeed = 5.5,
                units = WeatherUnits.METRIC,
            )

        fun dayForecast() =
            DayForecast(
                dateLabel = "Mon, Jan 1",
                conditionMain = "Clear",
                description = "Clear sky",
                iconCode = "01d",
                temperature = 72.5,
                feelsLike = 70.0,
                units = WeatherUnits.METRIC,
            )
    }
}
