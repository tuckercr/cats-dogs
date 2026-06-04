package com.tuckercr.catsdogs.model

import com.tuckercr.catsdogs.data.CitySearchRepository
import com.tuckercr.catsdogs.data.SavedCityRepository
import com.tuckercr.catsdogs.domain.CitySuggestion
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GeoLocationViewModelTest {

    private lateinit var dispatcher: TestDispatcher

    @Before
    fun setUp() {
        dispatcher = StandardTestDispatcher()
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `city suggestions are fetched only after debounce`() =
        runTest(dispatcher) {
            val repository = FakeCitySearchRepository(
                results = mapOf("Au" to Result.success(listOf(suggestion("Austin, TX, US")))),
            )
            val viewModel = viewModel(citySearchRepository = repository)

            viewModel.onCityInputChange(" Au ")
            runCurrent()

            assertTrue(viewModel.citySuggestLoading.value)
            assertTrue(repository.queries.isEmpty())

            advanceTimeBy(279)
            runCurrent()

            assertTrue(repository.queries.isEmpty())
            assertTrue(viewModel.citySuggestLoading.value)

            advanceTimeBy(1)
            runCurrent()

            assertEquals(listOf("Au"), repository.queries)
            assertEquals(listOf(suggestion("Austin, TX, US")), viewModel.citySuggestions.value)
            assertFalse(viewModel.citySuggestLoading.value)
        }

    @Test
    fun `short input cancels pending city search and clears loading`() =
        runTest(dispatcher) {
            val repository = FakeCitySearchRepository()
            val viewModel = viewModel(citySearchRepository = repository)

            viewModel.onCityInputChange("Au")
            runCurrent()
            viewModel.onCityInputChange("A")
            runCurrent()
            advanceTimeBy(280)
            runCurrent()

            assertEquals("A", viewModel.cityInput.value)
            assertTrue(repository.queries.isEmpty())
            assertTrue(viewModel.citySuggestions.value.isEmpty())
            assertFalse(viewModel.citySuggestLoading.value)
        }

    @Test
    fun `stale city search result does not replace latest input suggestions`() =
        runTest(dispatcher) {
            val firstResult = CompletableDeferred<Result<List<CitySuggestion>>>()
            val secondResult = CompletableDeferred<Result<List<CitySuggestion>>>()
            val repository = FakeCitySearchRepository(
                resultProvider = { query ->
                    when (query) {
                        "Au" -> firstResult.await()
                        "Austin" -> secondResult.await()
                        else -> Result.success(emptyList())
                    }
                },
            )
            val viewModel = viewModel(citySearchRepository = repository)

            viewModel.onCityInputChange("Au")
            runCurrent()
            advanceTimeBy(280)
            runCurrent()
            viewModel.onCityInputChange("Austin")
            runCurrent()

            firstResult.complete(Result.success(listOf(suggestion("Auburn, AL, US"))))
            runCurrent()

            assertTrue(viewModel.citySuggestions.value.isEmpty())
            assertTrue(viewModel.citySuggestLoading.value)

            advanceTimeBy(280)
            runCurrent()
            secondResult.complete(Result.success(listOf(suggestion("Austin, TX, US"))))
            runCurrent()

            assertEquals(listOf("Au", "Austin"), repository.queries)
            assertEquals(listOf(suggestion("Austin, TX, US")), viewModel.citySuggestions.value)
            assertFalse(viewModel.citySuggestLoading.value)
        }

    @Test
    fun `choosing suggestion pins coordinates and typing clears them`() =
        runTest(dispatcher) {
            val viewModel = viewModel()
            val selected = suggestion("Austin, TX, US", latitude = 30.2672, longitude = -97.7431)

            viewModel.onCitySuggestionChosen(selected)

            assertEquals("Austin, TX, US", viewModel.cityInput.value)
            assertEquals(30.2672, viewModel.pinnedLatitude() ?: 0.0, 0.0001)
            assertEquals(-97.7431, viewModel.pinnedLongitude() ?: 0.0, 0.0001)
            assertTrue(viewModel.citySuggestions.value.isEmpty())
            assertFalse(viewModel.citySuggestLoading.value)

            viewModel.onCityInputChange("Austin")

            assertNull(viewModel.pinnedLatitude())
            assertNull(viewModel.pinnedLongitude())

            viewModel.dismissSuggestions()
        }

    @Test
    fun `saved city is restored into input only once`() =
        runTest(dispatcher) {
            val viewModel = viewModel(savedCityRepository = FakeSavedCityRepository("Denver"))

            val firstRestore = viewModel.restoreSavedCityOnce()
            val secondRestore = viewModel.restoreSavedCityOnce()

            assertEquals("Denver", firstRestore)
            assertEquals("Denver", viewModel.cityInput.value)
            assertNull(secondRestore)
        }

    private fun viewModel(
        savedCityRepository: SavedCityRepository = FakeSavedCityRepository(),
        citySearchRepository: CitySearchRepository = FakeCitySearchRepository(),
    ) = GeoLocationViewModel(savedCityRepository, citySearchRepository)

    private class FakeSavedCityRepository(
        initialCity: String? = null,
    ) : SavedCityRepository {
        override val lastCity = MutableStateFlow(initialCity)
    }

    private class FakeCitySearchRepository(
        private val results: Map<String, Result<List<CitySuggestion>>> = emptyMap(),
        private val resultProvider: suspend (String) -> Result<List<CitySuggestion>> = { query ->
            results[query] ?: Result.success(emptyList())
        },
    ) : CitySearchRepository {
        val queries = mutableListOf<String>()

        override suspend fun searchCities(query: String): Result<List<CitySuggestion>> {
            queries += query
            return resultProvider(query)
        }
    }

    private companion object {
        fun suggestion(
            label: String,
            latitude: Double = 30.2672,
            longitude: Double = -97.7431,
        ) = CitySuggestion(
            label = label,
            weatherLat = latitude,
            weatherLon = longitude,
        )
    }
}
