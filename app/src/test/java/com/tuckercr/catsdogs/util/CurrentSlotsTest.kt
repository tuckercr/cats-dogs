package com.tuckercr.catsdogs.util

import com.tuckercr.catsdogs.domain.HourlySlot
import com.tuckercr.catsdogs.domain.WeatherUnits
import org.junit.Assert.assertEquals
import org.junit.Test

class CurrentSlotsTest {

    // 2026-01-01T12:00:00Z
    private val noon = 1_767_268_800L
    private val hour = 3_600L

    @Test
    fun `hourly slots that have passed are dropped, keeping the hour in progress`() {
        val slots = listOf(-5, -2, -1, 0, 1, 2).map { slot(noon + it * hour) }

        val current = slots.fromCurrentSlot(nowEpochSeconds = noon + 20 * 60) // 12:20

        assertEquals(listOf(noon, noon + hour, noon + 2 * hour), current.map { it.epochSeconds })
    }

    @Test
    fun `three-hourly slots keep the slot in progress`() {
        val slots = listOf(-6, -3, 0, 3, 6).map { slot(noon + it * hour) }

        // 14:00 is inside the 12:00-15:00 slot.
        val current = slots.fromCurrentSlot(nowEpochSeconds = noon + 2 * hour)

        assertEquals(listOf(noon, noon + 3 * hour, noon + 6 * hour), current.map { it.epochSeconds })
    }

    @Test
    fun `a slot starting exactly now is the current one`() {
        val slots = listOf(-1, 0, 1).map { slot(noon + it * hour) }

        assertEquals(listOf(noon, noon + hour), slots.fromCurrentSlot(noon).map { it.epochSeconds })
    }

    @Test
    fun `half-hour time zone slots are compared as instants`() {
        // India (+05:30): local whole hours start at hh:30 UTC.
        val slots = listOf(-90, -30, 30, 90).map { slot(noon + it * 60L) }

        val current = slots.fromCurrentSlot(nowEpochSeconds = noon) // 17:30 local is in progress

        assertEquals(listOf(noon - 30 * 60, noon + 30 * 60, noon + 90 * 60), current.map { it.epochSeconds })
    }

    @Test
    fun `a forecast entirely in the future is unchanged`() {
        val slots = listOf(1, 2, 3).map { slot(noon + it * hour) }

        assertEquals(slots, slots.fromCurrentSlot(noon))
    }

    @Test
    fun `a forecast entirely in the past keeps only its last slot`() {
        val slots = listOf(-30, -29, -28).map { slot(noon + it * hour) }

        assertEquals(listOf(noon - 28 * hour), slots.fromCurrentSlot(noon).map { it.epochSeconds })
    }

    @Test
    fun `slots without a timestamp are kept`() {
        val untimed = listOf(slot(null), slot(null))
        assertEquals(untimed, untimed.fromCurrentSlot(noon))

        val mixed = listOf(slot(noon - 2 * hour), slot(null), slot(noon - hour), slot(noon + hour))
        assertEquals(
            listOf(null, noon - hour, noon + hour),
            mixed.fromCurrentSlot(noon).map { it.epochSeconds },
        )
    }

    @Test
    fun `an empty forecast stays empty`() {
        assertEquals(emptyList<HourlySlot>(), emptyList<HourlySlot>().fromCurrentSlot(noon))
    }

    private fun slot(epochSeconds: Long?) =
        HourlySlot(
            timeLabel = epochSeconds?.toString() ?: "?",
            iconCode = "01d",
            description = "clear",
            temperature = 18.0,
            feelsLike = 18.0,
            windSpeed = 1.0,
            windDeg = 0,
            humidity = 50,
            pressure = 1013,
            units = WeatherUnits.METRIC,
            epochSeconds = epochSeconds,
        )
}
