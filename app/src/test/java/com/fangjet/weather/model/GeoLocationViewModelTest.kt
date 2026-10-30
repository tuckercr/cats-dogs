package com.fangjet.weather.model

import com.fangjet.weather.data.GeocodingRepository
import com.fangjet.weather.domain.CitySuggestion
import com.fangjet.weather.domain.SavedLocation
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
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
class GeoLocationViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val geocodingRepository = mockk<GeocodingRepository>(relaxed = true)

    @Test
    fun `short input clears suggestions without searching`() =
        runTest {
            val calls = mutableListOf<String>()
            coEvery { geocodingRepository.searchCities(any()) } answers {
                calls += firstArg<String>()
                Result.success(listOf(austinSuggestion))
            }
            val viewModel = GeoLocationViewModel(geocodingRepository)

            viewModel.onCityInputChange(" A ")
            mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

            assertTrue(viewModel.citySuggestions.value.isEmpty())
            assertFalse(viewModel.citySuggestLoading.value)
            assertTrue(calls.isEmpty())
        }

    @Test
    fun `latest city input wins when an earlier search is still running`() =
        runTest {
            val firstSearch = CompletableDeferred<Result<List<CitySuggestion>>>()
            val secondSearch = CompletableDeferred<Result<List<CitySuggestion>>>()
            val calls = mutableListOf<String>()

            coEvery { geocodingRepository.searchCities(any()) } coAnswers {
                val query = firstArg<String>()
                calls += query
                when (query) {
                    "Au" -> firstSearch.await()
                    "Austin" -> secondSearch.await()
                    else -> error("Unexpected query $query")
                }
            }
            val viewModel = GeoLocationViewModel(geocodingRepository)

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
        runTest {
            val viewModel = GeoLocationViewModel(geocodingRepository)

            viewModel.onCitySuggestionChosen(austinSuggestion)

            assertEquals("Austin, TX, US", viewModel.cityInput.value)
            assertTrue(viewModel.citySuggestions.value.isEmpty())
            assertFalse(viewModel.citySuggestLoading.value)
            assertEquals(austinSuggestion, viewModel.selectedSuggestion.value)

            viewModel.onCityInputChange("Austin")

            assertNull(viewModel.selectedSuggestion.value)
        }

    @Test
    fun `dismissSuggestions clears suggestions and loading but preserves input and selection`() =
        runTest {
            val viewModel = GeoLocationViewModel(geocodingRepository)
            viewModel.onCitySuggestionChosen(austinSuggestion)

            coEvery { geocodingRepository.searchCities(any()) } returns Result.success(listOf(austinSuggestion))
            viewModel.onCityInputChange("Austin")
            mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

            viewModel.dismissSuggestions()

            assertEquals("Austin", viewModel.cityInput.value)
            assertTrue(viewModel.citySuggestions.value.isEmpty())
            assertFalse(viewModel.citySuggestLoading.value)
        }

    @Test
    fun `reset clears all state`() =
        runTest {
            val viewModel = GeoLocationViewModel(geocodingRepository)
            viewModel.onCitySuggestionChosen(austinSuggestion)

            viewModel.reset()

            assertEquals("", viewModel.cityInput.value)
            assertTrue(viewModel.citySuggestions.value.isEmpty())
            assertFalse(viewModel.citySuggestLoading.value)
            assertNull(viewModel.selectedSuggestion.value)
        }

    // --- locationToAdd ---

    @Test
    fun `add city takes the top suggestion when none is picked`() =
        runTest {
            coEvery { geocodingRepository.searchCities(any()) } returns
                Result.success(listOf(londonGb, londonOntario))
            val viewModel = GeoLocationViewModel(geocodingRepository)

            viewModel.onCityInputChange("London")
            mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

            assertEquals(
                SavedLocation(label = "London, GB", latitude = 51.51, longitude = -0.13),
                viewModel.locationToAdd(),
            )
        }

    @Test
    fun `add city takes the picked suggestion over the top one`() =
        runTest {
            val viewModel = GeoLocationViewModel(geocodingRepository)

            viewModel.onCitySuggestionChosen(londonOntario)

            assertEquals(
                SavedLocation(label = "London, Ontario, CA", latitude = 42.98, longitude = -81.25),
                viewModel.locationToAdd(),
            )
        }

    @Test
    fun `add city falls back to the typed name without suggestions`() =
        runTest {
            coEvery { geocodingRepository.searchCities(any()) } returns Result.failure(java.io.IOException())
            val viewModel = GeoLocationViewModel(geocodingRepository)

            viewModel.onCityInputChange("  London ")
            mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

            assertEquals(SavedLocation(label = "London", latitude = null, longitude = null), viewModel.locationToAdd())
        }

    @Test
    fun `add city has nothing to add with nothing typed`() {
        assertNull(GeoLocationViewModel(geocodingRepository).locationToAdd())
    }

    private val londonGb = CitySuggestion("London, GB", 51.51, -0.13)
    private val londonOntario = CitySuggestion("London, Ontario, CA", 42.98, -81.25)

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
