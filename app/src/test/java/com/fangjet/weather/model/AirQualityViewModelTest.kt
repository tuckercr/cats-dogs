package com.fangjet.weather.model

import com.fangjet.weather.data.AirQualitySource
import com.fangjet.weather.data.ForecastSourcePolicy
import com.fangjet.weather.domain.AirQuality
import com.fangjet.weather.domain.SavedLocation
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class AirQualityViewModelTest {
    @get:Rule
    val mainDispatcherRule = CityListViewModelTest.MainDispatcherRule()

    private val london = SavedLocation(label = "London", latitude = 51.51, longitude = -0.13)
    private val reading = AirQuality(42, AirQuality.Scale.US, null)
    private val source = mockk<AirQualitySource>()

    @Test
    fun `loads a reading per city and does not refetch while fresh`() =
        runTest {
            coEvery { source.fetch(any(), any()) } returns Result.success(reading)
            var clock = 0L
            val viewModel = AirQualityViewModel(source, ForecastSourcePolicy.OpenMeteoPreferred).apply { now = { clock } }

            viewModel.load(london)
            mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()
            viewModel.load(london)
            mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

            assertEquals(reading, viewModel.readings.value[london.cacheKey])
            coVerify(exactly = 1) { source.fetch(51.51, -0.13) }

            clock += AirQualityViewModel.STALE_AFTER_MS
            viewModel.load(london)
            mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()
            coVerify(exactly = 2) { source.fetch(51.51, -0.13) }
        }

    @Test
    fun `cities without coordinates are not fetched`() =
        runTest {
            val viewModel = AirQualityViewModel(source, ForecastSourcePolicy.OpenMeteoPreferred)

            viewModel.load(SavedLocation(label = "London", latitude = null, longitude = null))
            mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

            coVerify(exactly = 0) { source.fetch(any(), any()) }
        }

    @Test
    fun `nothing is fetched while Open-Meteo is switched off`() =
        runTest {
            val viewModel = AirQualityViewModel(source) { false }

            viewModel.load(london)
            mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

            coVerify(exactly = 0) { source.fetch(any(), any()) }
        }

    @Test
    fun `a failed fetch leaves no reading`() =
        runTest {
            coEvery { source.fetch(any(), any()) } returns Result.failure(IOException())
            val viewModel = AirQualityViewModel(source, ForecastSourcePolicy.OpenMeteoPreferred)

            viewModel.load(london)
            mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

            assertTrue(viewModel.readings.value.isEmpty())
        }
}
