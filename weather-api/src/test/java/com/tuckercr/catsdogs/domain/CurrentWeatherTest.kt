package com.tuckercr.catsdogs.domain

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class CurrentWeatherTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `known offset is used as is`() {
        assertEquals(-14_400, weather(utcOffsetSeconds = -14_400).offsetSeconds)
    }

    @Test
    fun `cache written before the offset existed falls back to the device zone, not UTC`() {
        val old = json.encodeToString(CurrentWeather.serializer(), weather()).replace(",\"utcOffsetSeconds\":null", "")
        val decoded = json.decodeFromString(CurrentWeather.serializer(), old)

        assertNull(decoded.utcOffsetSeconds)
        val deviceOffset = ZoneId
            .systemDefault()
            .rules
            .getOffset(Instant.now())
            .totalSeconds
        assertEquals(deviceOffset, decoded.offsetSeconds)
    }

    private fun weather(utcOffsetSeconds: Int? = null) =
        CurrentWeather(
            cityName = "New York",
            conditionMain = "Clear",
            description = "clear sky",
            iconCode = "01d",
            temperature = 20.0,
            feelsLike = 20.0,
            tempMin = 18.0,
            tempMax = 22.0,
            humidityPercent = 50,
            pressureHpa = 1013,
            windSpeed = 3.0,
            windDeg = 90,
            visibilityMeters = 10_000,
            cloudPercent = 0,
            units = WeatherUnits.METRIC,
            utcOffsetSeconds = utcOffsetSeconds,
        )
}
