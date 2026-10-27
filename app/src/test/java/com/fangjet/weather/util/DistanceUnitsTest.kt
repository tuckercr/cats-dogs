package com.fangjet.weather.util

import com.fangjet.weather.domain.UnitOverride
import org.junit.Assert.assertEquals
import org.junit.Test

class DistanceUnitsTest {

    @Test
    fun `an explicit unit choice wins over the system`() {
        assertEquals(false, usesMilesForDistance(UnitOverride.METRIC) { true })
        assertEquals(true, usesMilesForDistance(UnitOverride.IMPERIAL) { false })
    }

    @Test
    fun `use system setting follows the measurement system`() {
        assertEquals(true, usesMilesForDistance(UnitOverride.SYSTEM) { true })
        assertEquals(false, usesMilesForDistance(UnitOverride.SYSTEM) { false })
    }
}
