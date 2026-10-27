package com.fangjet.weather.analytics

import com.fangjet.weather.data.PreferencesRepository
import com.fangjet.weather.data.RemoteConfigRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class AnalyticsCollectionControllerTest {

    private val backend = mockk<AnalyticsBackend>(relaxed = true)

    @Test
    fun `kill switch on and user opted in enables SDK collection`() =
        runTest {
            val controller = controller(shareUsageStatistics = flowOf(true), killSwitch = flowOf(true))

            startAndSettle(controller)

            verify(exactly = 1) { backend.setCollectionEnabled(true) }
            verify(exactly = 0) { backend.setCollectionEnabled(false) }
            assertEquals(true, controller.shareUsageStatistics)
        }

    @Test
    fun `user opted out disables SDK collection`() =
        runTest {
            val controller = controller(shareUsageStatistics = flowOf(false), killSwitch = flowOf(true))

            startAndSettle(controller)

            verify(exactly = 1) { backend.setCollectionEnabled(false) }
            verify(exactly = 0) { backend.setCollectionEnabled(true) }
            assertEquals(false, controller.shareUsageStatistics)
        }

    @Test
    fun `kill switch off disables SDK collection even when the user opted in`() =
        runTest {
            val controller = controller(shareUsageStatistics = flowOf(true), killSwitch = flowOf(false))

            startAndSettle(controller)

            verify(exactly = 1) { backend.setCollectionEnabled(false) }
            verify(exactly = 0) { backend.setCollectionEnabled(true) }
        }

    @Test
    fun `toggling the setting re-applies the SDK flag`() =
        runTest {
            val setting = MutableStateFlow(true)
            val controller = controller(shareUsageStatistics = setting, killSwitch = flowOf(true))
            startAndSettle(controller)

            setting.value = false
            runCurrent()
            setting.value = true
            runCurrent()

            verify(exactly = 2) { backend.setCollectionEnabled(true) }
            verify(exactly = 1) { backend.setCollectionEnabled(false) }
            assertEquals(true, controller.shareUsageStatistics)
        }

    @Test
    fun `a Remote Config activation flipping the kill switch re-applies the SDK flag`() =
        runTest {
            val killSwitch = MutableStateFlow(true)
            val controller = controller(shareUsageStatistics = flowOf(true), killSwitch = killSwitch)
            startAndSettle(controller)

            killSwitch.value = false
            runCurrent()

            verify(exactly = 1) { backend.setCollectionEnabled(true) }
            verify(exactly = 1) { backend.setCollectionEnabled(false) }
        }

    @Test
    fun `a failing preferences read never crashes and leaves events gated off`() =
        runTest {
            val controller = controller(
                shareUsageStatistics = flow { throw IOException("DataStore unreadable") },
                killSwitch = flowOf(true),
            )

            startAndSettle(controller)

            verify(exactly = 0) { backend.setCollectionEnabled(any()) }
            assertNull(controller.shareUsageStatistics)
        }

    private fun TestScope.startAndSettle(controller: AnalyticsCollectionController) {
        controller.start(backgroundScope)
        runCurrent()
    }

    private fun controller(
        shareUsageStatistics: Flow<Boolean>,
        killSwitch: Flow<Boolean>,
    ): AnalyticsCollectionController {
        val preferences = mockk<PreferencesRepository>()
        every { preferences.shareUsageStatistics } returns shareUsageStatistics
        val remoteConfig = mockk<RemoteConfigRepository>()
        every { remoteConfig.analyticsEnabledUpdates } returns killSwitch
        return AnalyticsCollectionController(preferences, remoteConfig, backend)
    }
}
