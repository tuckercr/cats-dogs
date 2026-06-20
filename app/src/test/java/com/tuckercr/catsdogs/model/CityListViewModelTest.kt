package com.tuckercr.catsdogs.model

import com.tuckercr.catsdogs.data.PreferencesRepository
import com.tuckercr.catsdogs.domain.SavedLocation
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
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
class CityListViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val austin = SavedLocation(label = "Austin, TX", latitude = 30.27, longitude = -97.74)
    private val denver = SavedLocation(label = "Denver, CO", latitude = 39.74, longitude = -104.98)
    private val gps = SavedLocation(
        label = "My Location",
        latitude = 37.0,
        longitude = -122.0,
        isCurrentLocation = true,
    )

    // --- init ---

    @Test
    fun `init loads saved locations and active index from DataStore`() =
        runTest {
            val viewModel = viewModel(
                savedLocations = listOf(austin, denver),
                activeIndex = 1,
            )
            mainDispatcherRule.testDispatcher.scheduler.runCurrent()

            assertEquals(listOf(austin, denver), viewModel.locations.value)
            assertEquals(1, viewModel.activeIndex.value)
            assertEquals(denver, viewModel.activeLocation.value)
        }

    @Test
    fun `init with no saved locations migrates from legacy lastCity`() =
        runTest {
            val persisted = mutableListOf<List<SavedLocation>>()
            val viewModel = viewModel(
                savedLocations = emptyList(),
                lastCity = "London",
                onSetSavedLocations = { persisted += it },
            )
            mainDispatcherRule.testDispatcher.scheduler.runCurrent()

            assertEquals(1, viewModel.locations.value.size)
            assertEquals(
                "London",
                viewModel.locations.value
                    .first()
                    .label,
            )
            assertEquals(1, persisted.size)
        }

    @Test
    fun `init with no saved locations and no lastCity leaves locations empty`() =
        runTest {
            val viewModel = viewModel(
                savedLocations = emptyList(),
                lastCity = null,
            )
            mainDispatcherRule.testDispatcher.scheduler.runCurrent()

            assertEquals(emptyList<SavedLocation>(), viewModel.locations.value)
            assertNull(viewModel.activeLocation.value)
        }

    @Test
    fun `init clamps out-of-range active index`() =
        runTest {
            val viewModel = viewModel(
                savedLocations = listOf(austin),
                activeIndex = 99,
            )
            mainDispatcherRule.testDispatcher.scheduler.runCurrent()

            assertEquals(0, viewModel.activeIndex.value)
        }

    // --- addLocation ---

    @Test
    fun `addLocation appends new entry and makes it active`() =
        runTest {
            val persisted = mutableListOf<List<SavedLocation>>()
            val viewModel = viewModel(
                savedLocations = listOf(austin),
                onSetSavedLocations = { persisted += it },
            )
            mainDispatcherRule.testDispatcher.scheduler.runCurrent()

            viewModel.addLocation(denver)
            mainDispatcherRule.testDispatcher.scheduler.runCurrent()

            assertEquals(listOf(austin, denver), viewModel.locations.value)
            assertEquals(1, viewModel.activeIndex.value)
            assertEquals(denver, viewModel.activeLocation.value)
            assertEquals(listOf(austin, denver), persisted.last())
        }

    @Test
    fun `addLocation with duplicate label switches to existing instead of duplicating`() =
        runTest {
            val persisted = mutableListOf<List<SavedLocation>>()
            val viewModel = viewModel(
                savedLocations = listOf(austin, denver),
                onSetSavedLocations = { persisted += it },
            )
            mainDispatcherRule.testDispatcher.scheduler.runCurrent()

            viewModel.addLocation(austin)
            mainDispatcherRule.testDispatcher.scheduler.runCurrent()

            assertEquals(listOf(austin, denver), viewModel.locations.value) // unchanged
            assertEquals(0, viewModel.activeIndex.value) // switched to austin
            assertEquals(austin, viewModel.activeLocation.value)
            assertEquals(true, persisted.isEmpty()) // no persistence on dedup
        }

    @Test
    fun `addLocation fires onCityAdded for new cities`() =
        runTest {
            var callCount = 0
            val viewModel = viewModel(
                savedLocations = listOf(austin),
                onCityAdded = { callCount++ },
            )
            mainDispatcherRule.testDispatcher.scheduler.runCurrent()

            viewModel.addLocation(denver)
            assertEquals(1, callCount)
        }

    @Test
    fun `addLocation does not fire onCityAdded for duplicates`() =
        runTest {
            var callCount = 0
            val viewModel = viewModel(
                savedLocations = listOf(austin, denver),
                onCityAdded = { callCount++ },
            )
            mainDispatcherRule.testDispatcher.scheduler.runCurrent()

            viewModel.addLocation(austin)
            assertEquals(0, callCount)
        }

    // --- removeLocation ---

    @Test
    fun `removeLocation removes entry at index and persists`() =
        runTest {
            val persisted = mutableListOf<List<SavedLocation>>()
            val viewModel = viewModel(
                savedLocations = listOf(austin, denver),
                activeIndex = 0,
                onSetSavedLocations = { persisted += it },
            )
            mainDispatcherRule.testDispatcher.scheduler.runCurrent()

            viewModel.removeLocation(1)
            mainDispatcherRule.testDispatcher.scheduler.runCurrent()

            assertEquals(listOf(austin), viewModel.locations.value)
            assertEquals(listOf(austin), persisted.last())
        }

    @Test
    fun `removeLocation clamps active index when active tab is removed`() =
        runTest {
            val viewModel = viewModel(
                savedLocations = listOf(austin, denver),
                activeIndex = 1,
            )
            mainDispatcherRule.testDispatcher.scheduler.runCurrent()

            viewModel.removeLocation(1) // remove the active (last) entry
            mainDispatcherRule.testDispatcher.scheduler.runCurrent()

            assertEquals(0, viewModel.activeIndex.value)
            assertEquals(austin, viewModel.activeLocation.value)
        }

    @Test
    fun `removeLocation on last entry results in empty list and null activeLocation`() =
        runTest {
            val viewModel = viewModel(
                savedLocations = listOf(austin),
                activeIndex = 0,
            )
            mainDispatcherRule.testDispatcher.scheduler.runCurrent()

            viewModel.removeLocation(0)
            mainDispatcherRule.testDispatcher.scheduler.runCurrent()

            assertEquals(emptyList<SavedLocation>(), viewModel.locations.value)
            assertNull(viewModel.activeLocation.value)
        }

    // --- setActiveIndex ---

    @Test
    fun `setActiveIndex updates index and persists`() =
        runTest {
            val persistedIndices = mutableListOf<Int>()
            val viewModel = viewModel(
                savedLocations = listOf(austin, denver),
                activeIndex = 0,
                onSetActiveIndex = { persistedIndices += it },
            )
            mainDispatcherRule.testDispatcher.scheduler.runCurrent()

            viewModel.setActiveIndex(1)
            mainDispatcherRule.testDispatcher.scheduler.runCurrent()

            assertEquals(1, viewModel.activeIndex.value)
            assertEquals(denver, viewModel.activeLocation.value)
            assertEquals(listOf(1), persistedIndices)
        }

    @Test
    fun `setActiveIndex is a no-op when index is unchanged`() =
        runTest {
            val persistedIndices = mutableListOf<Int>()
            val viewModel = viewModel(
                savedLocations = listOf(austin, denver),
                activeIndex = 0,
                onSetActiveIndex = { persistedIndices += it },
            )
            mainDispatcherRule.testDispatcher.scheduler.runCurrent()

            viewModel.setActiveIndex(0)
            mainDispatcherRule.testDispatcher.scheduler.runCurrent()

            assertEquals(0, viewModel.activeIndex.value)
            assertEquals(true, persistedIndices.isEmpty())
        }

    // --- helpers ---

    private fun viewModel(
        savedLocations: List<SavedLocation> = emptyList(),
        activeIndex: Int = 0,
        lastCity: String? = null,
        onSetSavedLocations: (List<SavedLocation>) -> Unit = {},
        onSetActiveIndex: (Int) -> Unit = {},
        onCityAdded: () -> Unit = {},
    ): CityListViewModel {
        val repo = mockk<PreferencesRepository>(relaxed = true)
        every { repo.savedLocations } returns flowOf(savedLocations)
        every { repo.activeLocationIndex } returns flowOf(activeIndex)
        every { repo.lastCity } returns flowOf(lastCity)
        coEvery { repo.setSavedLocations(any()) } answers {
            onSetSavedLocations(firstArg())
        }
        coEvery { repo.setActiveLocationIndex(any()) } answers {
            onSetActiveIndex(firstArg())
        }
        return CityListViewModel(repo, onCityAdded = onCityAdded)
    }

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
}
