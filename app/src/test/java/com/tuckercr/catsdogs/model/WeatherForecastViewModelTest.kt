package com.tuckercr.catsdogs.model

import com.tuckercr.catsdogs.domain.CurrentWeather
import com.tuckercr.catsdogs.domain.DayForecast
import com.tuckercr.catsdogs.domain.WeatherUnits
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WeatherForecastViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `blank current and forecast requests fail before fetching`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = viewModel()

            viewModel.refreshCurrent(city = "   ", latitude = null, longitude = null)
            viewModel.refreshForecast(resolvedCityName = "   ", latitude = null, longitude = null)

            assertEquals(
                LoadingState.Error("Please enter a city name.", canRetry = false),
                viewModel.currentWeather.value,
            )
            assertEquals(
                LoadingState.Error("Load current weather first to pick a location.", canRetry = false),
                viewModel.forecast.value,
            )

            viewModel.clearCurrentError()
            viewModel.clearForecastError()

            assertEquals(LoadingState.Idle, viewModel.currentWeather.value)
            assertEquals(LoadingState.Idle, viewModel.forecast.value)
        }

    @Test
    fun `refreshCurrent with coordinates fetches by coordinates and saves resolved city`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val requests = mutableListOf<CurrentRequest>()
            val savedCities = mutableListOf<String>()
            val weather = currentWeather(cityName = "Austin")
            val viewModel = viewModel(
                resolveWeatherUnits = { WeatherUnits.IMPERIAL },
                fetchCurrentWeather = { units, locationLabel, cityQuery, latitude, longitude ->
                    requests += CurrentRequest(units, locationLabel, cityQuery, latitude, longitude)
                    Result.success(weather)
                },
                setLastCity = { savedCities += it },
            )

            viewModel.refreshCurrent(
                city = "  Austin, TX, US  ",
                latitude = 30.2672,
                longitude = -97.7431,
            )
            mainDispatcherRule.testDispatcher.scheduler.runCurrent()

            val state = viewModel.currentWeather.value
            assertTrue(state is LoadingState.Success)
            assertEquals(weather, (state as LoadingState.Success).data)
            assertEquals("Austin", viewModel.resolvedCity.value)
            assertEquals(listOf("Austin"), savedCities)
            assertEquals(
                CurrentRequest(
                    units = WeatherUnits.IMPERIAL,
                    locationLabel = "Austin, TX, US",
                    cityQuery = null,
                    latitude = 30.2672,
                    longitude = -97.7431,
                ),
                requests.single(),
            )
        }

    @Test
    fun `latest current weather response wins when earlier request completes last`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val firstResponse = CompletableDeferred<Result<CurrentWeather>>()
            val secondResponse = CompletableDeferred<Result<CurrentWeather>>()
            val savedCities = mutableListOf<String>()
            val viewModel = viewModel(
                fetchCurrentWeather = { _, _, cityQuery, _, _ ->
                    when (cityQuery) {
                        "Austin" -> firstResponse.await()
                        "Denver" -> secondResponse.await()
                        else -> error("Unexpected query $cityQuery")
                    }
                },
                setLastCity = { savedCities += it },
            )

            viewModel.refreshCurrent(city = "Austin", latitude = null, longitude = null)
            mainDispatcherRule.testDispatcher.scheduler.runCurrent()
            viewModel.refreshCurrent(city = "Denver", latitude = null, longitude = null)
            mainDispatcherRule.testDispatcher.scheduler.runCurrent()

            firstResponse.complete(Result.success(currentWeather(cityName = "Austin")))
            mainDispatcherRule.testDispatcher.scheduler.runCurrent()

            assertEquals(LoadingState.Loading, viewModel.currentWeather.value)
            assertNull(viewModel.resolvedCity.value)
            assertTrue(savedCities.isEmpty())

            val denverWeather = currentWeather(cityName = "Denver")
            secondResponse.complete(Result.success(denverWeather))
            mainDispatcherRule.testDispatcher.scheduler.runCurrent()

            val state = viewModel.currentWeather.value
            assertTrue(state is LoadingState.Success)
            assertEquals(denverWeather, (state as LoadingState.Success).data)
            assertEquals("Denver", viewModel.resolvedCity.value)
            assertEquals(listOf("Denver"), savedCities)
        }

    @Test
    fun `refreshForecast with pinned coordinates ignores stale city name`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val requests = mutableListOf<ForecastRequest>()
            val forecast = listOf(dayForecast("Tue, Jan 2"))
            val viewModel = viewModel(
                resolveWeatherUnits = { WeatherUnits.METRIC },
                fetchForecast = { units, cityQuery, latitude, longitude ->
                    requests += ForecastRequest(units, cityQuery, latitude, longitude)
                    Result.success(forecast)
                },
            )

            viewModel.refreshForecast(
                resolvedCityName = "Austin",
                latitude = 30.2672,
                longitude = -97.7431,
            )
            mainDispatcherRule.testDispatcher.scheduler.runCurrent()

            val state = viewModel.forecast.value
            assertTrue(state is LoadingState.Success)
            assertEquals(forecast, (state as LoadingState.Success).data)
            assertEquals(
                ForecastRequest(
                    units = WeatherUnits.METRIC,
                    cityQuery = null,
                    latitude = 30.2672,
                    longitude = -97.7431,
                ),
                requests.single(),
            )
        }

    private fun viewModel(
        resolveWeatherUnits: () -> WeatherUnits = { WeatherUnits.METRIC },
        fetchCurrentWeather: suspend (
            units: WeatherUnits,
            locationLabel: String,
            cityQuery: String?,
            latitude: Double?,
            longitude: Double?,
        ) -> Result<CurrentWeather> = { _, _, _, _, _ -> Result.success(currentWeather("Austin")) },
        fetchForecast: suspend (
            units: WeatherUnits,
            cityQuery: String?,
            latitude: Double?,
            longitude: Double?,
        ) -> Result<List<DayForecast>> = { _, _, _, _ -> Result.success(emptyList()) },
        setLastCity: suspend (String) -> Unit = {},
    ) = WeatherForecastViewModel(
        resolveWeatherUnits = resolveWeatherUnits,
        fetchCurrentWeather = fetchCurrentWeather,
        fetchForecast = fetchForecast,
        setLastCity = setLastCity,
    )

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
        fun currentWeather(cityName: String) =
            CurrentWeather(
                cityName = cityName,
                conditionMain = "Clear",
                description = "Clear sky",
                iconCode = "01d",
                temperature = 72.0,
                feelsLike = 70.0,
                humidityPercent = 40,
                windSpeed = 5.0,
                units = WeatherUnits.METRIC,
            )

        fun dayForecast(dateLabel: String) =
            DayForecast(
                dateLabel = dateLabel,
                conditionMain = "Clouds",
                description = "Few clouds",
                iconCode = "02d",
                temperature = 68.0,
                feelsLike = 66.0,
                units = WeatherUnits.METRIC,
            )
    }
}
