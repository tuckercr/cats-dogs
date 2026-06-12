package com.tuckercr.catsdogs.model

import com.tuckercr.catsdogs.domain.CitySuggestion
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
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
    fun `short input clears suggestions without searching`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val calls = mutableListOf<String>()
            val viewModel = viewModel(
                searchCities = { query ->
                    calls += query
                    Result.success(listOf(austinSuggestion))
                },
            )

            viewModel.onCityInputChange(" A ")
            mainDispatcherRule.testDispatcher.scheduler.runCurrent()

            assertTrue(viewModel.citySuggestions.value.isEmpty())
            assertFalse(viewModel.citySuggestLoading.value)
            assertTrue(calls.isEmpty())
        }

    @Test
    fun `latest city input wins when earlier search is still running`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val firstSearch = CompletableDeferred<Result<List<CitySuggestion>>>()
            val secondSearch = CompletableDeferred<Result<List<CitySuggestion>>>()
            val calls = mutableListOf<String>()
            val viewModel = viewModel(
                searchCities = { query ->
                    calls += query
                    when (query) {
                        "Au" -> firstSearch.await()
                        "Austin" -> secondSearch.await()
                        else -> error("Unexpected query $query")
                    }
                },
            )

            viewModel.onCityInputChange("Au")
            mainDispatcherRule.testDispatcher.scheduler.advanceTimeBy(DEBOUNCE_MILLIS)
            mainDispatcherRule.testDispatcher.scheduler.runCurrent()

            assertEquals(listOf("Au"), calls)
            assertTrue(viewModel.citySuggestLoading.value)

            viewModel.onCityInputChange("Austin")
            firstSearch.complete(Result.success(listOf(staleSuggestion)))
            mainDispatcherRule.testDispatcher.scheduler.runCurrent()

            assertTrue(viewModel.citySuggestions.value.isEmpty())
            assertTrue(viewModel.citySuggestLoading.value)

            mainDispatcherRule.testDispatcher.scheduler.advanceTimeBy(DEBOUNCE_MILLIS)
            mainDispatcherRule.testDispatcher.scheduler.runCurrent()

            assertEquals(listOf("Au", "Austin"), calls)

            secondSearch.complete(Result.success(listOf(austinSuggestion)))
            mainDispatcherRule.testDispatcher.scheduler.runCurrent()

            assertEquals(listOf(austinSuggestion), viewModel.citySuggestions.value)
            assertFalse(viewModel.citySuggestLoading.value)
        }

    @Test
    fun `choosing suggestion pins coordinates and manual edits clear the pin`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = viewModel()

            viewModel.onCitySuggestionChosen(austinSuggestion)

            assertEquals("Austin, TX, US", viewModel.cityInput.value)
            assertTrue(viewModel.citySuggestions.value.isEmpty())
            assertFalse(viewModel.citySuggestLoading.value)
            assertEquals(30.2672, viewModel.pinnedLatitude() ?: 0.0, 0.0001)
            assertEquals(-97.7431, viewModel.pinnedLongitude() ?: 0.0, 0.0001)

            viewModel.onCityInputChange("Austin")

            assertNull(viewModel.pinnedLatitude())
            assertNull(viewModel.pinnedLongitude())
        }

    @Test
    fun `restore saved city updates input only once`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val lastCity = MutableStateFlow<String?>("Denver")
            val viewModel = viewModel(lastCity = lastCity)

            val restored = viewModel.restoreSavedCityOnce()

            assertEquals("Denver", restored)
            assertEquals("Denver", viewModel.cityInput.value)

            lastCity.value = "Austin"

            assertNull(viewModel.restoreSavedCityOnce())
            assertEquals("Denver", viewModel.cityInput.value)
        }

    private fun viewModel(
        lastCity: Flow<String?> = MutableStateFlow(null),
        searchCities: suspend (String) -> Result<List<CitySuggestion>> = { Result.success(emptyList()) },
    ) = GeoLocationViewModel(
        lastCity = lastCity,
        searchCities = searchCities,
    )

    private companion object {
        const val DEBOUNCE_MILLIS = 280L

        val austinSuggestion = CitySuggestion(
            label = "Austin, TX, US",
            weatherLat = 30.2672,
            weatherLon = -97.7431,
        )

        val staleSuggestion = CitySuggestion(
            label = "Aurora, CO, US",
            weatherLat = 39.7294,
            weatherLon = -104.8319,
        )
    }
}
