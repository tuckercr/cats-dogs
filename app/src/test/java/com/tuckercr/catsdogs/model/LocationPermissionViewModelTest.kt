package com.tuckercr.catsdogs.model

import android.app.Application
import com.tuckercr.catsdogs.domain.SavedLocation
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description

@OptIn(ExperimentalCoroutinesApi::class)
class LocationPermissionViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `fetchLocation without permission denies immediately and does not query location`() =
        runTest {
            val fetcher = FakeLocationFetcher(
                lastLocation = DeviceLocation(latitude = 30.2672, longitude = -97.7431),
            )
            val viewModel = viewModel(
                hasLocationPermission = false,
                locationFetcher = fetcher,
            )

            viewModel.fetchLocation()
            mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

            assertEquals(LocationFetchState.PermissionDenied, viewModel.state.value)
            assertEquals(0, fetcher.lastLocationCallCount)
            assertEquals(0, fetcher.currentLocationCallCount)
        }

    @Test
    fun `fetchLocation uses cached last location when available`() =
        runTest {
            val fetcher = FakeLocationFetcher(
                lastLocation = DeviceLocation(latitude = 30.2672, longitude = -97.7431),
                currentLocation = DeviceLocation(latitude = 39.7392, longitude = -104.9903),
            )
            val viewModel = viewModel(locationFetcher = fetcher)

            viewModel.fetchLocation()
            mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

            assertEquals(
                LocationFetchState.Located(
                    SavedLocation(
                        label = "My Location",
                        latitude = 30.2672,
                        longitude = -97.7431,
                        isCurrentLocation = true,
                    ),
                ),
                viewModel.state.value,
            )
            assertEquals(1, fetcher.lastLocationCallCount)
            assertEquals(0, fetcher.currentLocationCallCount)
        }

    @Test
    fun `fetchLocation falls back to current location when cached location is missing`() =
        runTest {
            val fetcher = FakeLocationFetcher(
                lastLocation = null,
                currentLocation = DeviceLocation(latitude = 39.7392, longitude = -104.9903),
            )
            val viewModel = viewModel(locationFetcher = fetcher)

            viewModel.fetchLocation()
            mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

            assertEquals(
                LocationFetchState.Located(
                    SavedLocation(
                        label = "My Location",
                        latitude = 39.7392,
                        longitude = -104.9903,
                        isCurrentLocation = true,
                    ),
                ),
                viewModel.state.value,
            )
            assertEquals(1, fetcher.lastLocationCallCount)
            assertEquals(1, fetcher.currentLocationCallCount)
        }

    @Test
    fun `fetchLocation fails when no location source returns coordinates`() =
        runTest {
            val fetcher = FakeLocationFetcher(
                lastLocation = null,
                currentLocation = null,
            )
            val viewModel = viewModel(locationFetcher = fetcher)

            viewModel.fetchLocation()
            mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

            assertEquals(LocationFetchState.Failed, viewModel.state.value)
            assertEquals(1, fetcher.lastLocationCallCount)
            assertEquals(1, fetcher.currentLocationCallCount)
        }

    @Test
    fun `onPermissionDenied records denied state`() {
        val viewModel = viewModel()

        viewModel.onPermissionDenied()

        assertEquals(LocationFetchState.PermissionDenied, viewModel.state.value)
    }

    private fun viewModel(
        hasLocationPermission: Boolean = true,
        locationFetcher: LocationFetcher = FakeLocationFetcher(),
    ) = LocationPermissionViewModel(
        application = mockk<Application>(relaxed = true),
        hasLocationPermissionProvider = { hasLocationPermission },
        locationFetcher = locationFetcher,
    )

    private class FakeLocationFetcher(
        private val lastLocation: DeviceLocation? = null,
        private val currentLocation: DeviceLocation? = null,
    ) : LocationFetcher {
        var lastLocationCallCount = 0
        var currentLocationCallCount = 0

        override suspend fun tryLastLocation(): DeviceLocation? {
            lastLocationCallCount += 1
            return lastLocation
        }

        override suspend fun tryCurrentLocation(): DeviceLocation? {
            currentLocationCallCount += 1
            return currentLocation
        }
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
