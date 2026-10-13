package com.tuckercr.catsdogs.model

import com.tuckercr.catsdogs.data.RadarRepository
import com.tuckercr.catsdogs.data.RemoteConfigRepository
import com.tuckercr.catsdogs.domain.RadarTimeline
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description

@OptIn(ExperimentalCoroutinesApi::class)
class RadarViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val radarRepository = mockk<RadarRepository>()

    @Test
    fun `radar_enabled off disables the radar and never fetches the timeline`() =
        runTest {
            val viewModel = viewModel(radarEnabled = false)

            viewModel.load()
            mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

            assertFalse(viewModel.enabled)
            assertEquals(LoadingState.Idle, viewModel.timeline.value)
            coVerify(exactly = 0) { radarRepository.getTimeline() }
        }

    @Test
    fun `radar_enabled off makes retry a no-op too`() =
        runTest {
            val viewModel = viewModel(radarEnabled = false)

            viewModel.retry()
            mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

            assertEquals(LoadingState.Idle, viewModel.timeline.value)
            coVerify(exactly = 0) { radarRepository.getTimeline() }
        }

    @Test
    fun `radar_enabled on loads the timeline`() =
        runTest {
            val timeline = mockk<RadarTimeline>()
            coEvery { radarRepository.getTimeline() } returns Result.success(timeline)
            val viewModel = viewModel(radarEnabled = true)

            viewModel.load()
            mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

            assertTrue(viewModel.enabled)
            assertEquals(LoadingState.Success(timeline), viewModel.timeline.value)
            coVerify(exactly = 1) { radarRepository.getTimeline() }
        }

    private fun viewModel(radarEnabled: Boolean): RadarViewModel {
        val remoteConfig = mockk<RemoteConfigRepository>()
        every { remoteConfig.radarEnabled() } returns radarEnabled
        every { remoteConfig.radarFrameIntervalMs() } returns RemoteConfigRepository.DEFAULT_RADAR_FRAME_INTERVAL_MS
        return RadarViewModel(radarRepository, remoteConfig)
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
