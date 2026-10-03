package com.tuckercr.catsdogs.data

import com.tuckercr.catsdogs.data.remote.dto.OpenMeteoHourlyDto
import com.tuckercr.catsdogs.data.remote.dto.OpenMeteoResponse
import com.tuckercr.catsdogs.domain.WeatherUnits
import org.junit.Assert.assertEquals
import org.junit.Test

class OpenMeteoForecastSourceTest {

    private val dayStart = 1_704_067_200L // 2024-01-01T00:00:00Z

    private fun response(
        hours: Int,
        offset: Int = 0,
        temp: Double = 20.0,
        radiation: Double = 0.0,
        isDay: Int = 1,
    ) = OpenMeteoResponse(
        utcOffsetSeconds = offset,
        hourly = OpenMeteoHourlyDto(
            time = List(hours) { dayStart + it * 3600L },
            temperature = List(hours) { temp },
            weatherCode = List(hours) { 61 },
            isDay = List(hours) { isDay },
            uvIndex = List(hours) { it.toDouble() },
            shortwaveRadiation = List(hours) { radiation },
            precipitationProbability = List(hours) { 40 },
        ),
    )

    @Test
    fun `drops hours before the current hour`() {
        // "Now" is 05:30, so the 00:00-04:00 hours are gone and the first slot is 05:00.
        val now = dayStart + 5 * 3600 + 1800
        val days = OpenMeteoForecastSource.toDayForecasts(response(24), WeatherUnits.METRIC, now)
        assertEquals(19, days.flatMap { it.hourlySlots }.size)
        assertEquals(
            "5 AM",
            days
                .first()
                .hourlySlots
                .first()
                .timeLabel,
        )
    }

    @Test
    fun `groups and labels hours in the city's own timezone`() {
        // UTC+10: midnight UTC is 10 AM local on the same date, and 14:00 UTC is midnight next day.
        val days = OpenMeteoForecastSource.toDayForecasts(response(24, offset = 36_000), WeatherUnits.METRIC, dayStart)
        assertEquals(
            "10 AM",
            days
                .first()
                .hourlySlots
                .first()
                .timeLabel,
        )
        assertEquals(2, days.size)
    }

    @Test
    fun `maps weather code, rain chance, and daily peak uv`() {
        val day = OpenMeteoForecastSource.toDayForecasts(response(6), WeatherUnits.METRIC, dayStart).single()
        assertEquals("Rain", day.conditionMain)
        assertEquals(40, day.precipitationChance)
        assertEquals(5.0, day.uvIndexMax, 0.0001)
        assertEquals("10d", day.hourlySlots.first().iconCode)
    }

    @Test
    fun `pavement heat is estimated in the requested units`() {
        // 86F = 30C air; 800 W/m2 adds 28C -> 58C = 136.4F.
        val slot = OpenMeteoForecastSource
            .toDayForecasts(response(1, temp = 86.0, radiation = 800.0), WeatherUnits.IMPERIAL, dayStart)
            .single()
            .hourlySlots
            .single()
        assertEquals(136.4, slot.pavementTemperature ?: 0.0, 0.01)
    }

    @Test
    fun `hours missing a temperature are skipped`() {
        val r = response(3)
        val gappy = r.copy(hourly = r.hourly.copy(temperature = listOf(20.0, null, 21.0)))
        val slots = OpenMeteoForecastSource.toDayForecasts(gappy, WeatherUnits.METRIC, dayStart).single().hourlySlots
        assertEquals(2, slots.size)
    }
}
