package com.fangjet.weather

import com.fangjet.weather.worker.NotificationKind
import org.junit.Assert.assertEquals
import org.junit.Test

class NotificationChannelsTest {
    @Test
    fun `the Walk time channel is only made with a dog`() {
        assertEquals(
            listOf(NotificationKind.MORNING_BRIEFING, NotificationKind.RAIN),
            WeatherApplication.channelsNeeded(hasDog = false),
        )
        assertEquals(NotificationKind.entries.toList(), WeatherApplication.channelsNeeded(hasDog = true))
    }
}
