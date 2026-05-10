package com.tuckercr.catsdogs.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class WeatherUnitsTest {

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
