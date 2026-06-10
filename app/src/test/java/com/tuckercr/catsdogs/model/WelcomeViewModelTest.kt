package com.tuckercr.catsdogs.model

import com.tuckercr.catsdogs.data.WeatherPreferences
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WelcomeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `init exposes stored welcome state`() =
        runTest(mainDispatcherRule.dispatcher) {
            val preferences = FakeWeatherPreferences(hasSeenWelcome = MutableStateFlow(true))
            val viewModel = WelcomeViewModel(preferences)

            runCurrent()

            assertTrue(viewModel.welcomeDone.value == true)
        }

    @Test
    fun `completeWelcome marks welcome done and persists preference`() =
        runTest(mainDispatcherRule.dispatcher) {
            val preferences = FakeWeatherPreferences(hasSeenWelcome = MutableStateFlow(false))
            val viewModel = WelcomeViewModel(preferences)
            runCurrent()

            assertFalse(viewModel.welcomeDone.value == true)

            viewModel.completeWelcome()
            runCurrent()

            assertTrue(viewModel.welcomeDone.value == true)
            assertTrue(preferences.hasSeenWelcome.value)
        }

    private class FakeWeatherPreferences(
        override val hasSeenWelcome: MutableStateFlow<Boolean>,
    ) : WeatherPreferences {
        override val lastCity = MutableStateFlow<String?>(null)

        override suspend fun setHasSeenWelcome(value: Boolean) {
            hasSeenWelcome.value = value
        }

        override suspend fun setLastCity(cityName: String) = Unit

        override suspend fun hasSeenWelcomeOnce(): Boolean = hasSeenWelcome.value
    }
}
