package com.tuckercr.catsdogs.model

import com.tuckercr.catsdogs.data.CitySearchRepository
import com.tuckercr.catsdogs.data.WeatherPreferences
import com.tuckercr.catsdogs.domain.CitySuggestion
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GeoLocationViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `short query clears suggestions and does not search`() =
        runTest(mainDispatcherRule.dispatcher) {
            val searcher = RecordingCitySearcher()
            val viewModel = viewModel(citySearchRepository = searcher)

            viewModel.onCitySuggestionChosen(austin)
            viewModel.onCityInputChange(" a ")
            advanceUntilIdle()

            assertTrue(viewModel.citySuggestions.value.isEmpty())
            assertFalse(viewModel.citySuggestLoading.value)
            assertNull(viewModel.pinnedLatitude())
            assertNull(viewModel.pinnedLongitude())
            assertTrue(searcher.queries.isEmpty())
        }

    @Test
    fun `selecting suggestion pins coordinates and typing clears them`() =
        runTest(mainDispatcherRule.dispatcher) {
            val viewModel = viewModel()

            viewModel.onCitySuggestionChosen(austin)

            assertEquals("Austin, TX, US", viewModel.cityInput.value)
            assertEquals(30.2672, viewModel.pinnedLatitude() ?: 0.0, 0.0001)
            assertEquals(-97.7431, viewModel.pinnedLongitude() ?: 0.0, 0.0001)
            assertTrue(viewModel.citySuggestions.value.isEmpty())
            assertFalse(viewModel.citySuggestLoading.value)

            viewModel.onCityInputChange("Austin")
            runCurrent()

            assertNull(viewModel.pinnedLatitude())
            assertNull(viewModel.pinnedLongitude())
        }

    @Test
    fun `stale search result is ignored after input changes`() =
        runTest(mainDispatcherRule.dispatcher) {
            val searcher = DeferredCitySearcher()
            val viewModel = viewModel(citySearchRepository = searcher)

            viewModel.onCityInputChange("Austin")
            advanceTimeBy(280)
            runCurrent()

            assertEquals(listOf("Austin"), searcher.queries)
            assertTrue(viewModel.citySuggestLoading.value)

            viewModel.onCityInputChange("Boston")
            searcher.complete("Austin", listOf(austin))
            runCurrent()

            assertTrue(viewModel.citySuggestions.value.isEmpty())

            advanceTimeBy(280)
            runCurrent()
            assertEquals(listOf("Austin", "Boston"), searcher.queries)

            searcher.complete("Boston", listOf(boston))
            runCurrent()

            assertEquals(listOf(boston), viewModel.citySuggestions.value)
            assertFalse(viewModel.citySuggestLoading.value)
        }

    @Test
    fun `restoreSavedCityOnce returns saved city only once`() =
        runTest(mainDispatcherRule.dispatcher) {
            val savedCity = MutableStateFlow<String?>("Austin, TX, US")
            val viewModel = viewModel(preferences = FakeWeatherPreferences(lastCity = savedCity))

            val firstRestore = viewModel.restoreSavedCityOnce()
            savedCity.value = "Boston, MA, US"
            val secondRestore = viewModel.restoreSavedCityOnce()

            assertEquals("Austin, TX, US", firstRestore)
            assertNull(secondRestore)
            assertEquals("Austin, TX, US", viewModel.cityInput.value)
        }

    @Test
    fun `restoreSavedCityOnce ignores blank saved city`() =
        runTest(mainDispatcherRule.dispatcher) {
            val viewModel = viewModel(preferences = FakeWeatherPreferences(lastCity = MutableStateFlow("   ")))

            val restored = viewModel.restoreSavedCityOnce()

            assertNull(restored)
            assertEquals("", viewModel.cityInput.value)
        }

    private fun viewModel(
        preferences: WeatherPreferences = FakeWeatherPreferences(lastCity = MutableStateFlow(null)),
        citySearchRepository: CitySearchRepository = RecordingCitySearcher(),
    ) = GeoLocationViewModel(
        preferences = preferences,
        citySearchRepository = citySearchRepository,
    )

    private class FakeWeatherPreferences(
        override val lastCity: Flow<String?>,
    ) : WeatherPreferences {
        override val hasSeenWelcome = MutableStateFlow(false)

        override suspend fun setHasSeenWelcome(value: Boolean) {
            hasSeenWelcome.value = value
        }

        override suspend fun setLastCity(cityName: String) = Unit

        override suspend fun hasSeenWelcomeOnce(): Boolean = hasSeenWelcome.value
    }

    private class RecordingCitySearcher : CitySearchRepository {
        val queries = mutableListOf<String>()

        override suspend fun searchCities(query: String): Result<List<CitySuggestion>> {
            queries += query
            return Result.success(emptyList())
        }
    }

    private class DeferredCitySearcher : CitySearchRepository {
        val queries = mutableListOf<String>()
        private val responses = mutableMapOf<String, CompletableDeferred<Result<List<CitySuggestion>>>>()

        override suspend fun searchCities(query: String): Result<List<CitySuggestion>> {
            queries += query
            val response = CompletableDeferred<Result<List<CitySuggestion>>>()
            responses[query] = response
            return response.await()
        }

        fun complete(
            query: String,
            suggestions: List<CitySuggestion>,
        ) {
            responses.getValue(query).complete(Result.success(suggestions))
        }
    }

    private companion object {
        val austin = CitySuggestion(
            label = "Austin, TX, US",
            weatherLat = 30.2672,
            weatherLon = -97.7431,
        )

        val boston = CitySuggestion(
            label = "Boston, MA, US",
            weatherLat = 42.3601,
            weatherLon = -71.0589,
        )
    }
}
