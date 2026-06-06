package com.tuckercr.catsdogs.model

import com.tuckercr.catsdogs.data.UserPreferences
import com.tuckercr.catsdogs.data.WeatherDataRepository
import com.tuckercr.catsdogs.domain.CurrentWeather
import com.tuckercr.catsdogs.domain.DayForecast
import com.tuckercr.catsdogs.domain.WeatherUnits
import com.tuckercr.catsdogs.util.WeatherUnitsProvider
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WeatherForecastViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `refreshCurrent with coordinates ignores city query and saves resolved city`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val preferences = FakeUserPreferences()
            val weatherRepository = FakeWeatherDataRepository()
            val viewModel = WeatherForecastViewModel(
                preferencesRepository = preferences,
                weatherRepository = weatherRepository,
                weatherUnitsProvider = FakeWeatherUnitsProvider(WeatherUnits.IMPERIAL),
            )

            viewModel.refreshCurrent(
                city = " Austin, TX, US ",
                latitude = 30.2672,
                longitude = -97.7431,
            )
            runCurrent()

            val request = weatherRepository.currentRequests.single()
            val state = viewModel.currentWeather.value as LoadingState.Success
            assertEquals(WeatherUnits.IMPERIAL, request.units)
            assertEquals("Austin, TX, US", request.locationLabel)
            assertEquals(null, request.cityQuery)
            assertEquals(30.2672, request.latitude ?: 0.0, 0.0001)
            assertEquals(-97.7431, request.longitude ?: 0.0, 0.0001)
            assertEquals("Austin, TX, US", state.data.cityName)
            assertEquals("Austin, TX, US", viewModel.resolvedCity.value)
            assertEquals(listOf("Austin, TX, US"), preferences.savedCities)
        }

    @Test
    fun `refreshCurrent ignores stale response from older request`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val preferences = FakeUserPreferences()
            val weatherRepository = FakeWeatherDataRepository()
            val firstResponse = CompletableDeferred<Result<CurrentWeather>>()
            var callCount = 0
            weatherRepository.currentHandler = {
                callCount += 1
                if (callCount == 1) {
                    firstResponse.await()
                } else {
                    Result.success(currentWeather(cityName = "New City"))
                }
            }
            val viewModel = WeatherForecastViewModel(
                preferencesRepository = preferences,
                weatherRepository = weatherRepository,
                weatherUnitsProvider = FakeWeatherUnitsProvider(WeatherUnits.METRIC),
            )

            viewModel.refreshCurrent(city = "Old City", latitude = null, longitude = null)
            runCurrent()
            viewModel.refreshCurrent(city = "New City", latitude = null, longitude = null)
            runCurrent()

            assertEquals("New City", successWeather(viewModel).cityName)

            firstResponse.complete(Result.success(currentWeather(cityName = "Old City")))
            runCurrent()

            assertEquals("New City", successWeather(viewModel).cityName)
            assertEquals("New City", viewModel.resolvedCity.value)
            assertEquals(listOf("New City"), preferences.savedCities)
        }

    @Test
    fun `refreshForecast with coordinates ignores resolved city query`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val weatherRepository = FakeWeatherDataRepository()
            val forecast = dayForecast(dateLabel = "Today")
            weatherRepository.forecastHandler = { Result.success(listOf(forecast)) }
            val viewModel = WeatherForecastViewModel(
                preferencesRepository = FakeUserPreferences(),
                weatherRepository = weatherRepository,
                weatherUnitsProvider = FakeWeatherUnitsProvider(WeatherUnits.IMPERIAL),
            )

            viewModel.refreshForecast(
                resolvedCityName = "Austin",
                latitude = 30.2672,
                longitude = -97.7431,
            )
            runCurrent()

            val request = weatherRepository.forecastRequests.single()
            val state = viewModel.forecast.value as LoadingState.Success
            assertEquals(WeatherUnits.IMPERIAL, request.units)
            assertEquals(null, request.cityQuery)
            assertEquals(30.2672, request.latitude ?: 0.0, 0.0001)
            assertEquals(-97.7431, request.longitude ?: 0.0, 0.0001)
            assertEquals(listOf(forecast), state.data)
        }

    @Test
    fun `refreshForecast with blank location fails before repository call`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val weatherRepository = FakeWeatherDataRepository()
            val viewModel = WeatherForecastViewModel(
                preferencesRepository = FakeUserPreferences(),
                weatherRepository = weatherRepository,
                weatherUnitsProvider = FakeWeatherUnitsProvider(WeatherUnits.METRIC),
            )

            viewModel.refreshForecast(resolvedCityName = "   ", latitude = null, longitude = null)

            assertEquals(
                LoadingState.Error("Load current weather first to pick a location.", canRetry = false),
                viewModel.forecast.value,
            )
            assertTrue(weatherRepository.forecastRequests.isEmpty())
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

    private class FakeWeatherDataRepository : WeatherDataRepository {
        val currentRequests = mutableListOf<CurrentRequest>()
        val forecastRequests = mutableListOf<ForecastRequest>()
        var currentHandler: suspend (CurrentRequest) -> Result<CurrentWeather> = { request ->
            Result.success(currentWeather(cityName = request.locationLabel.ifBlank { request.cityQuery.orEmpty() }))
        }
        var forecastHandler: suspend (ForecastRequest) -> Result<List<DayForecast>> = {
            Result.success(emptyList())
        }

        override suspend fun fetchCurrentWeather(
            units: WeatherUnits,
            locationLabel: String,
            cityQuery: String?,
            latitude: Double?,
            longitude: Double?,
        ): Result<CurrentWeather> {
            val request = CurrentRequest(
                units = units,
                locationLabel = locationLabel,
                cityQuery = cityQuery,
                latitude = latitude,
                longitude = longitude,
            )
            currentRequests += request
            return currentHandler(request)
        }

        override suspend fun fetchForecast(
            units: WeatherUnits,
            cityQuery: String?,
            latitude: Double?,
            longitude: Double?,
        ): Result<List<DayForecast>> {
            val request = ForecastRequest(
                units = units,
                cityQuery = cityQuery,
                latitude = latitude,
                longitude = longitude,
            )
            forecastRequests += request
            return forecastHandler(request)
        }
    }

    private class FakeUserPreferences : UserPreferences {
        val savedCities = mutableListOf<String>()
        override val hasSeenWelcome: Flow<Boolean> = MutableStateFlow(false)
        override val lastCity: Flow<String?> = MutableStateFlow(null)

        override suspend fun setHasSeenWelcome(value: Boolean) = Unit

        override suspend fun setLastCity(cityName: String) {
            savedCities += cityName
        }

        override suspend fun hasSeenWelcomeOnce(): Boolean = false
    }

    private class FakeWeatherUnitsProvider(
        private val units: WeatherUnits,
    ) : WeatherUnitsProvider {
        override fun resolve(): WeatherUnits = units
    }

    private companion object {
        fun successWeather(viewModel: WeatherForecastViewModel): CurrentWeather =
            (viewModel.currentWeather.value as LoadingState.Success).data

        fun currentWeather(cityName: String) =
            CurrentWeather(
                cityName = cityName,
                conditionMain = "Clear",
                description = "Clear sky",
                iconCode = "01d",
                temperature = 72.0,
                feelsLike = 70.0,
                humidityPercent = 50,
                windSpeed = 5.0,
                units = WeatherUnits.METRIC,
            )

        fun dayForecast(dateLabel: String) =
            DayForecast(
                dateLabel = dateLabel,
                conditionMain = "Clouds",
                description = "broken clouds",
                iconCode = "04d",
                temperature = 80.0,
                feelsLike = 79.0,
                units = WeatherUnits.IMPERIAL,
            )
    }
}
