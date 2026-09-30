package com.tuckercr.catsdogs.ui.pets

import com.tuckercr.catsdogs.domain.WeatherUnits
import org.junit.Assert.assertEquals
import org.junit.Test

class PetMoodTest {

    @Test
    fun `precipitation and storms map to their moods regardless of temperature`() {
        assertEquals(PetMood.STORM, petMoodFor("Thunderstorm", "11d", 35.0))
        assertEquals(PetMood.SNOW, petMoodFor("Snow", "13d", -5.0))
        assertEquals(PetMood.RAIN, petMoodFor("Rain", "10d", 32.0))
        assertEquals(PetMood.RAIN, petMoodFor("Drizzle", "09n", 12.0))
    }

    @Test
    fun `temperature extremes win over sky and time of day`() {
        assertEquals(PetMood.HOT, petMoodFor("Clear", "01d", 30.0))
        assertEquals(PetMood.COLD, petMoodFor("Clouds", "04n", 0.0))
    }

    @Test
    fun `night icon means night, otherwise clear is sunny and anything else is cloudy`() {
        assertEquals(PetMood.NIGHT, petMoodFor("Clear", "01n", 15.0))
        assertEquals(PetMood.SUNNY, petMoodFor("Clear", "01d", 20.0))
        assertEquals(PetMood.CLOUDY, petMoodFor("Clouds", "03d", 20.0))
        assertEquals(PetMood.CLOUDY, petMoodFor("Mist", "50d", 12.0))
    }

    @Test
    fun `fahrenheit converts to celsius`() {
        assertEquals(0.0, toCelsius(32.0, WeatherUnits.IMPERIAL), 0.001)
        assertEquals(30.0, toCelsius(86.0, WeatherUnits.IMPERIAL), 0.001)
        assertEquals(21.5, toCelsius(21.5, WeatherUnits.METRIC), 0.001)
    }
}
