package com.tuckercr.catsdogs.model

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
class WelcomeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `initial state reflects stored welcome flag`() =
        runTest {
            val viewModel = viewModel(
                hasSeenWelcomeOnce = { true },
            )

            assertNull(viewModel.welcomeDone.value)

            mainDispatcherRule.testDispatcher.scheduler.runCurrent()

            assertEquals(true, viewModel.welcomeDone.value)
        }

    @Test
    fun `complete welcome marks state done and persists flag`() =
        runTest {
            val persistedValues = mutableListOf<Boolean>()
            val viewModel = viewModel(
                hasSeenWelcomeOnce = { false },
                setHasSeenWelcome = { persistedValues += it },
            )
            mainDispatcherRule.testDispatcher.scheduler.runCurrent()
            assertEquals(false, viewModel.welcomeDone.value)

            viewModel.completeWelcome()
            mainDispatcherRule.testDispatcher.scheduler.runCurrent()

            assertEquals(true, viewModel.welcomeDone.value)
            assertEquals(listOf(true), persistedValues)
        }

    private fun viewModel(
        hasSeenWelcomeOnce: suspend () -> Boolean = { false },
        setHasSeenWelcome: suspend (Boolean) -> Unit = {},
    ) = WelcomeViewModel(
        hasSeenWelcomeOnce = hasSeenWelcomeOnce,
        setHasSeenWelcome = setHasSeenWelcome,
    )

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
