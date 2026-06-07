package com.tuckercr.catsdogs.model

import com.tuckercr.catsdogs.data.CitySearch
import com.tuckercr.catsdogs.data.UserPreferences
import com.tuckercr.catsdogs.domain.CitySuggestion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description

@OptIn(ExperimentalCoroutinesApi::class)
class GeoLocationViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var preferences: FakeUserPreferences
    private lateinit var citySearch: FakeCitySearch
    private lateinit var viewModel: GeoLocationViewModel

    @Before
    fun setUp() {
        preferences = FakeUserPreferences()
        citySearch = FakeCitySearch()
        viewModel = GeoLocationViewModel(preferences, citySearch)
    }

    @Test
    fun `search waits for debounce and only requests latest input`() =
        runTest {
            val boston = CitySuggestion("Boston, MA, US", 42.3601, -71.0589)
            citySearch.resultsByQuery["Boston"] = listOf(boston)

            viewModel.onCityInputChange("Austin")
            runCurrent()
            assertTrue(viewModel.citySuggestLoading.value)

            advanceTimeBy(279)
            runCurrent()
            assertTrue(citySearch.queries.isEmpty())

            viewModel.onCityInputChange("Boston")
            runCurrent()
            advanceTimeBy(280)
            runCurrent()

            assertEquals(listOf("Boston"), citySearch.queries)
            assertFalse(viewModel.citySuggestLoading.value)
            assertEquals(listOf(boston), viewModel.citySuggestions.value)
        }

    @Test
    fun `choosing suggestion pins coordinates and manual typing clears stale pin`() {
        val suggestion = CitySuggestion("Austin, TX, US", 30.2672, -97.7431)

        viewModel.onCitySuggestionChosen(suggestion)

        assertEquals("Austin, TX, US", viewModel.cityInput.value)
        assertEquals(30.2672, viewModel.pinnedLatitude() ?: 0.0, 0.0001)
        assertEquals(-97.7431, viewModel.pinnedLongitude() ?: 0.0, 0.0001)
        assertTrue(viewModel.citySuggestions.value.isEmpty())
        assertFalse(viewModel.citySuggestLoading.value)

        viewModel.onCityInputChange("Austin")

        assertNull(viewModel.pinnedLatitude())
        assertNull(viewModel.pinnedLongitude())
    }

    @Test
    fun `restoreSavedCityOnce copies saved city only once`() =
        runTest {
            preferences.setStoredLastCity("Seattle")

            val restored = viewModel.restoreSavedCityOnce()
            preferences.setStoredLastCity("Portland")
            val restoredAgain = viewModel.restoreSavedCityOnce()

            assertEquals("Seattle", restored)
            assertEquals("Seattle", viewModel.cityInput.value)
            assertNull(restoredAgain)
        }

    @Test
    fun `restoreSavedCityOnce ignores blank saved city`() =
        runTest {
            preferences.setStoredLastCity("   ")

            val restored = viewModel.restoreSavedCityOnce()

            assertNull(restored)
            assertEquals("", viewModel.cityInput.value)
        }

    private class FakeCitySearch : CitySearch {
        val queries = mutableListOf<String>()
        val resultsByQuery = mutableMapOf<String, List<CitySuggestion>>()

        override suspend fun searchCities(query: String): Result<List<CitySuggestion>> {
            queries += query
            return Result.success(resultsByQuery[query].orEmpty())
        }
    }

    private class FakeUserPreferences : UserPreferences {
        private val hasSeenWelcomeFlow = MutableStateFlow(false)
        private val lastCityFlow = MutableStateFlow<String?>(null)

        override val hasSeenWelcome: Flow<Boolean> = hasSeenWelcomeFlow
        override val lastCity: Flow<String?> = lastCityFlow

        override suspend fun setHasSeenWelcome(value: Boolean) {
            hasSeenWelcomeFlow.value = value
        }

        override suspend fun setLastCity(cityName: String) {
            lastCityFlow.value = cityName
        }

        override suspend fun hasSeenWelcomeOnce(): Boolean = hasSeenWelcome.first()

        fun setStoredLastCity(cityName: String?) {
            lastCityFlow.value = cityName
        }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
private class MainDispatcherRule(
    private val testDispatcher: TestDispatcher = StandardTestDispatcher(),
) : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(testDispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}
