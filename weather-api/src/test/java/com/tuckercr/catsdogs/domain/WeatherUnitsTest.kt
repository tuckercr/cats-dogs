package com.tuckercr.catsdogs.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class WeatherUnitsTest {

    @Test
    fun `converts to and from celsius`() {
        assertEquals(0.0, WeatherUnits.IMPERIAL.toCelsius(32.0), 0.001)
        assertEquals(30.0, WeatherUnits.IMPERIAL.toCelsius(86.0), 0.001)
        assertEquals(21.5, WeatherUnits.METRIC.toCelsius(21.5), 0.001)
        assertEquals(212.0, WeatherUnits.IMPERIAL.fromCelsius(100.0), 0.001)
        assertEquals(-4.0, WeatherUnits.METRIC.fromCelsius(-4.0), 0.001)
    }

    @Suppress("DEPRECATION")
    @Test
    fun `US and territories use imperial`() {
        assertEquals(WeatherUnits.IMPERIAL, WeatherUnits.fromLocale(Locale.US))
        assertEquals(WeatherUnits.IMPERIAL, WeatherUnits.fromLocale(Locale("en", "PR")))
        assertEquals(WeatherUnits.IMPERIAL, WeatherUnits.fromLocale(Locale("en", "GU")))
    }

    @Test
    fun `UK and others use metric`() {
        assertEquals(WeatherUnits.METRIC, WeatherUnits.fromLocale(Locale.UK))
        assertEquals(WeatherUnits.METRIC, WeatherUnits.fromLocale(Locale.CANADA))
        assertEquals(WeatherUnits.METRIC, WeatherUnits.fromLocale(Locale.GERMANY))
    }
}
