package com.tuckercr.catsdogs.data

import org.junit.Assert.assertEquals
import org.junit.Test

class WeatherHelpersTest {

    @Test
    fun `wmo codes map to app conditions with day and night icons`() {
        assertEquals(WmoWeatherCodes.Condition("Clear", "clear sky", "01d"), WmoWeatherCodes.condition(0, true))
        assertEquals("01n", WmoWeatherCodes.condition(0, false).iconCode)
        assertEquals("Thunderstorm", WmoWeatherCodes.condition(95, true).main)
        assertEquals("Snow", WmoWeatherCodes.condition(73, true).main)
        assertEquals("Drizzle", WmoWeatherCodes.condition(53, true).main)
        assertEquals("Fog", WmoWeatherCodes.condition(45, true).main)
        assertEquals("Clouds", WmoWeatherCodes.condition(999, true).main)
    }

    @Test
    fun `pavement matches air at night or without sun`() {
        assertEquals(20.0, PavementHeat.estimateC(20.0, 900.0, isDay = false), 0.0001)
        assertEquals(20.0, PavementHeat.estimateC(20.0, null, isDay = true), 0.0001)
    }

    @Test
    fun `pavement heats with sunlight up to a cap`() {
        assertEquals(48.0, PavementHeat.estimateC(20.0, 800.0, isDay = true), 0.0001)
        assertEquals(55.0, PavementHeat.estimateC(20.0, 5000.0, isDay = true), 0.0001)
    }
}
