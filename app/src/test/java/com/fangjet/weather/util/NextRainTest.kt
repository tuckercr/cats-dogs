package com.fangjet.weather.util

import com.fangjet.weather.domain.QuarterHour
import org.junit.Assert.assertEquals
import org.junit.Test

class NextRainTest {

    private val start = 1_704_067_200L // a quarter-hour boundary

    private fun quarters(
        vararg mm: Double,
        snow: Boolean = false,
    ) = mm.mapIndexed { i, amount -> QuarterHour(start + i * 900L, amount, if (snow && amount > 0) 1.0 else 0.0) }

    @Test
    fun `dry for the next 2 hours says nothing`() {
        assertEquals(null, nextRain(quarters(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0), start))
    }

    @Test
    fun `traces under 0_1 mm don't count`() {
        assertEquals(null, nextRain(quarters(0.0, 0.05, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0), start))
    }

    @Test
    fun `dry now and wet later is when it starts`() {
        assertEquals(NextRain.Starting(start + 3 * 900, false), nextRain(quarters(0.0, 0.0, 0.0, 0.4, 1.0, 0.2, 0.0, 0.0), start))
    }

    @Test
    fun `rain beyond 2 hours isn't mentioned`() {
        assertEquals(null, nextRain(quarters(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0), start))
    }

    @Test
    fun `wet now stops at the first 30 dry minutes, not a short gap`() {
        // A 15-minute gap at quarter 2, then dry from quarter 4.
        assertEquals(NextRain.Stopping(start + 4 * 900, false), nextRain(quarters(0.5, 0.5, 0.0, 0.3, 0.0, 0.0, 0.0, 0.0), start))
    }

    @Test
    fun `wet now and dry only at the last quarter still stops`() {
        assertEquals(NextRain.Stopping(start + 7 * 900, false), nextRain(quarters(0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.0), start))
    }

    @Test
    fun `wet throughout keeps on`() {
        assertEquals(NextRain.Continuing(false), nextRain(quarters(0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5), start))
    }

    @Test
    fun `snow is named as snow`() {
        assertEquals(NextRain.Starting(start + 900, true), nextRain(quarters(0.0, 0.4, 0.4, 0.4, 0.0, 0.0, 0.0, 0.0, snow = true), start))
    }

    @Test
    fun `it reads from the quarter in progress`() {
        // 20 minutes in: the first quarter is over, so the rain in the second is "now".
        val now = start + 20 * 60
        assertEquals(NextRain.Stopping(start + 3 * 900, false), nextRain(quarters(0.0, 0.5, 0.5, 0.0, 0.0, 0.0, 0.0, 0.0), now))
    }

    @Test
    fun `a forecast with under an hour left says nothing`() {
        assertEquals(null, nextRain(quarters(0.0, 0.0, 0.4), start))
    }
}
