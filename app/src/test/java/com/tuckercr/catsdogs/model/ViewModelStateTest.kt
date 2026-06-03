package com.tuckercr.catsdogs.model

import com.tuckercr.catsdogs.data.CitySearchDataSource
import com.tuckercr.catsdogs.data.WeatherDataSource
import com.tuckercr.catsdogs.data.WeatherPreferences
import com.tuckercr.catsdogs.domain.CitySuggestion
import com.tuckercr.catsdogs.domain.CurrentWeather
import com.tuckercr.catsdogs.domain.DayForecast
import com.tuckercr.catsdogs.domain.WeatherUnits
import com.tuckercr.catsdogs.util.WeatherUnitsProvider
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description

@OptIn(ExperimentalCoroutinesApi::class)
class ViewModelStateTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `refreshCurrent with coordinates stores resolved city and calls weather by coordinates`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val preferences = FakeWeatherPreferences()
            val weatherDataSource = FakeWeatherDataSource()
            val viewModel = WeatherForecastViewModel(
                weatherUnitsProvider = FakeWeatherUnitsProvider(WeatherUnits.IMPERIAL),
                preferencesRepository = preferences,
                weatherDataSource = weatherDataSource,
            )

            viewModel.refreshCurrent(
                city = " Austin, TX, US ",
                latitude = 30.2672,
                longitude = -97.7431,
            )
            advanceUntilIdle()

            val state = viewModel.currentWeather.value
            assertTrue(state is LoadingState.Success)
            assertEquals("Austin, TX, US", (state as LoadingState.Success).data.cityName)
            assertEquals("Austin, TX, US", viewModel.resolvedCity.value)
            assertEquals(listOf("Austin, TX, US"), preferences.lastCityWrites)

            val request = weatherDataSource.currentRequests.single()
            assertEquals(WeatherUnits.IMPERIAL, request.units)
            assertEquals("Austin, TX, US", request.locationLabel)
            assertNull(request.cityQuery)
            assertEquals(30.2672, request.latitude ?: 0.0, 0.0001)
            assertEquals(-97.7431, request.longitude ?: 0.0, 0.0001)
        }

    @Test
    fun `older current weather response cannot replace newer search`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val preferences = FakeWeatherPreferences()
            val weatherDataSource = FakeWeatherDataSource()
            val firstResponse = CompletableDeferred<Result<CurrentWeather>>()
            val secondResponse = CompletableDeferred<Result<CurrentWeather>>()
            weatherDataSource.enqueueCurrent(firstResponse)
            weatherDataSource.enqueueCurrent(secondResponse)
            val viewModel = WeatherForecastViewModel(
                weatherUnitsProvider = FakeWeatherUnitsProvider(WeatherUnits.METRIC),
                preferencesRepository = preferences,
                weatherDataSource = weatherDataSource,
            )

            viewModel.refreshCurrent(city = "First", latitude = null, longitude = null)
            advanceUntilIdle()
            viewModel.refreshCurrent(city = "Second", latitude = null, longitude = null)
            advanceUntilIdle()

            secondResponse.complete(Result.success(currentWeather("Second City")))
            advanceUntilIdle()

            val newerState = viewModel.currentWeather.value
            assertTrue(newerState is LoadingState.Success)
            assertEquals("Second City", (newerState as LoadingState.Success).data.cityName)
            assertEquals(listOf("Second City"), preferences.lastCityWrites)

            firstResponse.complete(Result.success(currentWeather("First City")))
            advanceUntilIdle()

            val finalState = viewModel.currentWeather.value
            assertTrue(finalState is LoadingState.Success)
            assertEquals("Second City", (finalState as LoadingState.Success).data.cityName)
            assertEquals(listOf("Second City"), preferences.lastCityWrites)
        }

    @Test
    fun `refreshForecast without a location fails before repository call`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val weatherDataSource = FakeWeatherDataSource()
            val viewModel = WeatherForecastViewModel(
                weatherUnitsProvider = FakeWeatherUnitsProvider(WeatherUnits.METRIC),
                preferencesRepository = FakeWeatherPreferences(),
                weatherDataSource = weatherDataSource,
            )

            viewModel.refreshForecast(resolvedCityName = "   ", latitude = null, longitude = null)

            assertEquals(
                LoadingState.Error(
                    message = "Load current weather first to pick a location.",
                    canRetry = false,
                ),
                viewModel.forecast.value,
            )
            assertTrue(weatherDataSource.forecastRequests.isEmpty())
        }

    @Test
    fun `city suggestions debounce and chosen suggestion pins coordinates`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val suggestion = CitySuggestion("Austin, TX, US", 30.2672, -97.7431)
            val citySearchDataSource = FakeCitySearchDataSource(
                results = mapOf("Austin" to listOf(suggestion)),
            )
            val viewModel = GeoLocationViewModel(
                preferencesRepository = FakeWeatherPreferences(),
                citySearchDataSource = citySearchDataSource,
            )

            viewModel.onCityInputChange("A")
            runCurrent()
            assertFalse(viewModel.citySuggestLoading.value)
            assertTrue(viewModel.citySuggestions.value.isEmpty())
            assertTrue(citySearchDataSource.queries.isEmpty())

            viewModel.onCityInputChange("Austin")
            runCurrent()
            assertTrue(viewModel.citySuggestLoading.value)
            advanceTimeBy(279)
            runCurrent()
            assertTrue(citySearchDataSource.queries.isEmpty())

            advanceTimeBy(1)
            runCurrent()
            assertEquals(listOf("Austin"), citySearchDataSource.queries)
            assertEquals(listOf(suggestion), viewModel.citySuggestions.value)
            assertFalse(viewModel.citySuggestLoading.value)

            viewModel.onCitySuggestionChosen(suggestion)
            assertEquals("Austin, TX, US", viewModel.cityInput.value)
            assertEquals(30.2672, viewModel.pinnedLatitude() ?: 0.0, 0.0001)
            assertEquals(-97.7431, viewModel.pinnedLongitude() ?: 0.0, 0.0001)
            assertTrue(viewModel.citySuggestions.value.isEmpty())

            viewModel.onCityInputChange("A")
            assertNull(viewModel.pinnedLatitude())
            assertNull(viewModel.pinnedLongitude())
        }

    @Test
    fun `restoreSavedCityOnce restores saved city only once`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = GeoLocationViewModel(
                preferencesRepository = FakeWeatherPreferences(initialLastCity = "Denver"),
                citySearchDataSource = FakeCitySearchDataSource(),
            )

            assertEquals("Denver", viewModel.restoreSavedCityOnce())
            assertEquals("Denver", viewModel.cityInput.value)
            assertNull(viewModel.restoreSavedCityOnce())
        }

    @Test
    fun `welcome ViewModel reads preference and persists completion`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val preferences = FakeWeatherPreferences(initialWelcomeSeen = false)
            val viewModel = WelcomeViewModel(preferences)

            runCurrent()
            assertEquals(false, viewModel.welcomeDone.value)

            viewModel.completeWelcome()
            runCurrent()

            assertEquals(true, viewModel.welcomeDone.value)
            assertEquals(listOf(true), preferences.welcomeWrites)
        }

    private class FakeWeatherPreferences(
        initialWelcomeSeen: Boolean = false,
        initialLastCity: String? = null,
    ) : WeatherPreferences {
        private val hasSeenWelcomeFlow = MutableStateFlow(initialWelcomeSeen)
        private val lastCityFlow = MutableStateFlow(initialLastCity)

        val welcomeWrites = mutableListOf<Boolean>()
        val lastCityWrites = mutableListOf<String>()

        override val hasSeenWelcome: Flow<Boolean> = hasSeenWelcomeFlow
        override val lastCity: Flow<String?> = lastCityFlow

        override suspend fun setHasSeenWelcome(value: Boolean) {
            welcomeWrites += value
            hasSeenWelcomeFlow.value = value
        }

        override suspend fun setLastCity(cityName: String) {
            lastCityWrites += cityName
            lastCityFlow.value = cityName
        }

        override suspend fun hasSeenWelcomeOnce(): Boolean = hasSeenWelcomeFlow.value
    }

    private class FakeCitySearchDataSource(
        private val results: Map<String, List<CitySuggestion>> = emptyMap(),
    ) : CitySearchDataSource {
        val queries = mutableListOf<String>()

        override suspend fun searchCities(query: String): Result<List<CitySuggestion>> {
            queries += query
            return Result.success(results[query].orEmpty())
        }
    }

    private class FakeWeatherDataSource : WeatherDataSource {
        val currentRequests = mutableListOf<CurrentRequest>()
        val forecastRequests = mutableListOf<ForecastRequest>()
        private val currentResponses = ArrayDeque<CompletableDeferred<Result<CurrentWeather>>>()

        fun enqueueCurrent(response: CompletableDeferred<Result<CurrentWeather>>) {
            currentResponses += response
        }

        override suspend fun fetchCurrentWeather(
            units: WeatherUnits,
            locationLabel: String,
            cityQuery: String?,
            latitude: Double?,
            longitude: Double?,
        ): Result<CurrentWeather> {
            currentRequests += CurrentRequest(units, locationLabel, cityQuery, latitude, longitude)
            return if (currentResponses.isEmpty()) {
                Result.success(currentWeather(locationLabel.takeIf { it.isNotBlank() } ?: cityQuery.orEmpty()))
            } else {
                currentResponses.removeFirst().await()
            }
        }

        override suspend fun fetchForecast(
            units: WeatherUnits,
            cityQuery: String?,
            latitude: Double?,
            longitude: Double?,
        ): Result<List<DayForecast>> {
            forecastRequests += ForecastRequest(units, cityQuery, latitude, longitude)
            return Result.success(emptyList())
        }
    }

    private class FakeWeatherUnitsProvider(
        private val units: WeatherUnits,
    ) : WeatherUnitsProvider {
        override fun resolve(): WeatherUnits = units
    }

    data class CurrentRequest(
        val units: WeatherUnits,
        val locationLabel: String,
        val cityQuery: String?,
        val latitude: Double?,
        val longitude: Double?,
    )

    data class ForecastRequest(
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
                temperature = 21.0,
                feelsLike = 20.0,
                humidityPercent = 42,
                windSpeed = 5.5,
                units = WeatherUnits.METRIC,
            )
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
private class MainDispatcherRule(
    val testDispatcher: TestDispatcher = StandardTestDispatcher(),
) : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(testDispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}
