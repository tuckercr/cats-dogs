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
    fun `initial state is null before prefs read`() =
        runTest {
            val viewModel = viewModel(
                hasSeenWelcomeOnce = { true },
                locationOnboardingDoneOnce = { false },
            )

            assertNull(viewModel.onboardingState.value)
        }

    @Test
    fun `initial state reflects stored flags`() =
        runTest {
            val viewModel = viewModel(
                hasSeenWelcomeOnce = { true },
                locationOnboardingDoneOnce = { true },
            )

            mainDispatcherRule.testDispatcher.scheduler.runCurrent()

            assertEquals(
                OnboardingState(
                    hasSeenWelcome = true,
                    notificationOnboardingDone = false,
                    locationOnboardingDone = true,
                ),
                viewModel.onboardingState.value,
            )
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
            assertEquals(false, viewModel.onboardingState.value?.hasSeenWelcome)

            viewModel.completeWelcome()
            mainDispatcherRule.testDispatcher.scheduler.runCurrent()

            assertEquals(true, viewModel.onboardingState.value?.hasSeenWelcome)
            assertEquals(listOf(true), persistedValues)
        }

    @Test
    fun `complete notification onboarding marks state done and persists`() =
        runTest {
            val persisted = mutableListOf<Unit>()
            val viewModel = viewModel(
                notificationOnboardingDoneOnce = { false },
                setNotificationOnboardingDone = { persisted += Unit },
            )
            mainDispatcherRule.testDispatcher.scheduler.runCurrent()
            assertEquals(false, viewModel.onboardingState.value?.notificationOnboardingDone)

            viewModel.completeNotificationOnboarding()
            mainDispatcherRule.testDispatcher.scheduler.runCurrent()

            assertEquals(true, viewModel.onboardingState.value?.notificationOnboardingDone)
            assertEquals(1, persisted.size)
        }

    @Test
    fun `complete location onboarding marks state done and persists`() =
        runTest {
            val persisted = mutableListOf<Unit>()
            val viewModel = viewModel(
                locationOnboardingDoneOnce = { false },
                setLocationOnboardingDone = { persisted += Unit },
            )
            mainDispatcherRule.testDispatcher.scheduler.runCurrent()
            assertEquals(false, viewModel.onboardingState.value?.locationOnboardingDone)

            viewModel.completeLocationOnboarding()
            mainDispatcherRule.testDispatcher.scheduler.runCurrent()

            assertEquals(true, viewModel.onboardingState.value?.locationOnboardingDone)
            assertEquals(1, persisted.size)
        }

    private fun viewModel(
        hasSeenWelcomeOnce: suspend () -> Boolean = { false },
        notificationOnboardingDoneOnce: suspend () -> Boolean = { false },
        locationOnboardingDoneOnce: suspend () -> Boolean = { false },
        setHasSeenWelcome: suspend (Boolean) -> Unit = {},
        setNotificationOnboardingDone: suspend () -> Unit = {},
        setLocationOnboardingDone: suspend () -> Unit = {},
    ) = WelcomeViewModel(
        hasSeenWelcomeOnce = hasSeenWelcomeOnce,
        notificationOnboardingDoneOnce = notificationOnboardingDoneOnce,
        locationOnboardingDoneOnce = locationOnboardingDoneOnce,
        setHasSeenWelcome = setHasSeenWelcome,
        setNotificationOnboardingDone = setNotificationOnboardingDone,
        setLocationOnboardingDone = setLocationOnboardingDone,
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
