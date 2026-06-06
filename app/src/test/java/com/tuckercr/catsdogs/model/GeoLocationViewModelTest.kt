package com.tuckercr.catsdogs.model

import com.tuckercr.catsdogs.data.CitySearchRepository
import com.tuckercr.catsdogs.data.UserPreferences
import com.tuckercr.catsdogs.domain.CitySuggestion
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceTimeBy
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
    fun `restoreSavedCityOnce copies saved city only once`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val preferences = FakeUserPreferences(lastCity = "Denver")
            val viewModel = GeoLocationViewModel(preferences, FakeCitySearchRepository())

            val restored = viewModel.restoreSavedCityOnce()
            preferences.lastCityValue.value = "Austin"
            val restoredAgain = viewModel.restoreSavedCityOnce()

            assertEquals("Denver", restored)
            assertNull(restoredAgain)
            assertEquals("Denver", viewModel.cityInput.value)
        }

    @Test
    fun `typing clears pinned location and searches only after debounce`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val suggestion = CitySuggestion(
                label = "Austin, TX, US",
                weatherLat = 30.2672,
                weatherLon = -97.7431,
            )
            val citySearch = FakeCitySearchRepository(result = listOf(suggestion))
            val viewModel = GeoLocationViewModel(FakeUserPreferences(), citySearch)

            viewModel.onCitySuggestionChosen(suggestion)
            viewModel.onCityInputChange("  Au  ")
            runCurrent()

            assertNull(viewModel.pinnedLatitude())
            assertNull(viewModel.pinnedLongitude())
            assertTrue(viewModel.citySuggestLoading.value)
            assertTrue(viewModel.citySuggestions.value.isEmpty())
            assertTrue(citySearch.queries.isEmpty())

            advanceTimeBy(279)
            runCurrent()

            assertTrue(citySearch.queries.isEmpty())

            advanceTimeBy(1)
            runCurrent()

            assertEquals(listOf("Au"), citySearch.queries)
            assertFalse(viewModel.citySuggestLoading.value)
            assertEquals(listOf(suggestion), viewModel.citySuggestions.value)
        }

    private class FakeCitySearchRepository(
        private val result: List<CitySuggestion> = emptyList(),
    ) : CitySearchRepository {
        val queries = mutableListOf<String>()

        override suspend fun searchCities(query: String): Result<List<CitySuggestion>> {
            queries += query
            return Result.success(result)
        }
    }

    private class FakeUserPreferences(
        lastCity: String? = null,
    ) : UserPreferences {
        val lastCityValue = MutableStateFlow(lastCity)
        override val hasSeenWelcome: Flow<Boolean> = MutableStateFlow(false)
        override val lastCity: Flow<String?> = lastCityValue

        override suspend fun setHasSeenWelcome(value: Boolean) = Unit

        override suspend fun setLastCity(cityName: String) = Unit

        override suspend fun hasSeenWelcomeOnce(): Boolean = false
    }
}
