package com.tuckercr.catsdogs.model

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WelcomeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `init publishes stored welcome state`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = WelcomeViewModel(
                hasSeenWelcomeOnce = { true },
                setHasSeenWelcome = {},
            )

            mainDispatcherRule.testDispatcher.scheduler.runCurrent()

            assertEquals(true, viewModel.welcomeDone.value)
        }

    @Test
    fun `completeWelcome updates state and persists preference`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val savedValues = mutableListOf<Boolean>()
            val viewModel = WelcomeViewModel(
                hasSeenWelcomeOnce = { false },
                setHasSeenWelcome = { savedValues += it },
            )
            mainDispatcherRule.testDispatcher.scheduler.runCurrent()

            viewModel.completeWelcome()
            mainDispatcherRule.testDispatcher.scheduler.runCurrent()

            assertEquals(true, viewModel.welcomeDone.value)
            assertEquals(listOf(true), savedValues)
        }
}
