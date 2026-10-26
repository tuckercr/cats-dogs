package com.tuckercr.catsdogs.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CalmWindTest {

    @Test
    fun `a speed that would show as 0_0 is calm`() {
        assertTrue(isCalmWind(0.0))
        assertTrue(isCalmWind(0.04))
    }

    @Test
    fun `anything that shows as 0_1 or more is not`() {
        assertFalse(isCalmWind(0.05))
        assertFalse(isCalmWind(3.2))
    }

    @Test
    fun `visibility at OpenWeather's 10 km cap reads as clear`() {
        assertTrue(isClearVisibility(10_000))
        assertFalse(isClearVisibility(9_999))
        assertFalse(isClearVisibility(800))
    }
}
