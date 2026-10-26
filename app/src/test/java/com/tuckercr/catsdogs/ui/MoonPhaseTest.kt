package com.tuckercr.catsdogs.ui

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class MoonPhaseTest {

    private fun phaseAt(iso: String) = moonPhaseAt(Instant.parse(iso).epochSecond)

    @Test
    fun `matches the published phases of January 2024`() {
        assertEquals(MoonPhase.LAST_QUARTER, phaseAt("2024-01-04T03:30:00Z"))
        assertEquals(MoonPhase.NEW, phaseAt("2024-01-11T11:57:00Z"))
        assertEquals(MoonPhase.FIRST_QUARTER, phaseAt("2024-01-18T03:53:00Z"))
        assertEquals(MoonPhase.FULL, phaseAt("2024-01-25T17:54:00Z"))
    }

    @Test
    fun `steps through the phases in order across a cycle`() {
        val newMoon = Instant.parse("2024-01-11T11:57:00Z").epochSecond
        val eighth = (29.530588853 * 86_400 / 8).toLong()
        MoonPhase.entries.forEachIndexed { i, phase ->
            assertEquals(phase, moonPhaseAt(newMoon + i * eighth))
        }
    }

    @Test
    fun `dates before the reference new moon still work`() {
        assertEquals(MoonPhase.FULL, phaseAt("1999-12-22T17:31:00Z"))
    }
}
