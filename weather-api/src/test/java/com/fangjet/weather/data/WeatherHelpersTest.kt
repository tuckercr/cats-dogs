package com.fangjet.weather.data

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

    @Test
    fun `each hour uses the sun of the hour ahead, since Open-Meteo averages the hour before`() {
        // Radiation stamped 01:00 is the 00:00-01:00 average, so it belongs to the 00:00 hour.
        val pavement = PavementHeat.series(listOf(20.0, 20.0), listOf(0.0, 800.0), listOf(true, true))

        assertEquals(48.0, pavement[0]!!, 0.0001)
        // The last hour has nothing after it and keeps its own value.
        assertEquals(48.0, PavementHeat.series(listOf(20.0), listOf(800.0), listOf(true)).single()!!, 0.0001)
    }

    @Test
    fun `pavement warms through the morning and keeps some heat after the sun goes`() {
        // 10 C air all day; sun for the middle two hours settles the pavement at 27.5 C.
        val pavement = PavementHeat.series(
            airC = List(5) { 10.0 },
            shortwaveRadiationWm2 = listOf(0.0, 0.0, 500.0, 500.0, 0.0),
            isDay = List(5) { true },
        )

        assertEquals(10.0, pavement[0]!!, 0.0001)
        assertEquals(20.5, pavement[1]!!, 0.0001) // 60% of the way up
        assertEquals(24.7, pavement[2]!!, 0.0001)
        assertEquals(18.82, pavement[3]!!, 0.0001) // 40% of the way back down
    }

    @Test
    fun `an hour without an air temperature has no pavement and doesn't break the series`() {
        val pavement = PavementHeat.series(listOf(20.0, null, 20.0), listOf(0.0, 0.0, 0.0), listOf(false, false, false))

        assertEquals(listOf(20.0, null, 20.0), pavement)
    }
}
