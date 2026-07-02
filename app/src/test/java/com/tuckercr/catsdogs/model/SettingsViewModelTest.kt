package com.tuckercr.catsdogs.model

import com.tuckercr.catsdogs.data.PreferencesRepository
import com.tuckercr.catsdogs.domain.UnitOverride
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
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
class SettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `unitOverride reflects value from repository`() =
        runTest {
            val viewModel = viewModel(unitOverride = UnitOverride.IMPERIAL)
            val collected = mutableListOf<UnitOverride>()
            val job = backgroundScope.launch { viewModel.unitOverride.collect { collected += it } }

            mainDispatcherRule.testDispatcher.scheduler.runCurrent()

            assertEquals(UnitOverride.IMPERIAL, collected.last())
            job.cancel()
        }

    @Test
    fun `setUnitOverride delegates to repository`() =
        runTest {
            val repo = mockk<PreferencesRepository>(relaxed = true)
            every { repo.unitOverride } returns flowOf(UnitOverride.SYSTEM)
            val viewModel = SettingsViewModel(repo)

            viewModel.setUnitOverride(UnitOverride.METRIC)
            mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

            coVerify { repo.setUnitOverride(UnitOverride.METRIC) }
        }

    @Test
    fun `clearCache delegates to repository`() =
        runTest {
            val repo = mockk<PreferencesRepository>(relaxed = true)
            every { repo.unitOverride } returns flowOf(UnitOverride.SYSTEM)
            val viewModel = SettingsViewModel(repo)

            viewModel.clearCache()
            mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

            coVerify { repo.clearCache() }
        }

    private fun viewModel(unitOverride: UnitOverride = UnitOverride.SYSTEM): SettingsViewModel {
        val repo = mockk<PreferencesRepository>(relaxed = true)
        every { repo.unitOverride } returns flowOf(unitOverride)
        return SettingsViewModel(repo)
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
