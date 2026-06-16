package com.tuckercr.catsdogs.util

import androidx.core.text.util.LocalePreferences.TemperatureUnit
import com.tuckercr.catsdogs.domain.WeatherUnits
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class LocaleWeatherUnitsTest {

    @Test
    fun `explicit celsius preference uses metric even in imperial locale`() {
        val units = resolveWeatherUnits(
            locale = Locale.US,
            temperatureUnit = TemperatureUnit.CELSIUS,
        )

        assertEquals(WeatherUnits.METRIC, units)
    }

    @Test
    fun `explicit fahrenheit preference uses imperial even in metric locale`() {
        val units = resolveWeatherUnits(
            locale = Locale.UK,
            temperatureUnit = TemperatureUnit.FAHRENHEIT,
        )

        assertEquals(WeatherUnits.IMPERIAL, units)
    }

    @Test
    fun `unknown temperature preference falls back to locale`() {
        val units = resolveWeatherUnits(
            locale = Locale.CANADA,
            temperatureUnit = TemperatureUnit.DEFAULT,
        )

        assertEquals(WeatherUnits.METRIC, units)
    }
}
