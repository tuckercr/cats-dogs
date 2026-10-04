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
        zone: String? = null,
        start: Long = dayStart,
        temp: Double = 20.0,
        radiation: Double = 0.0,
        isDay: Int = 1,
    ) = OpenMeteoResponse(
        utcOffsetSeconds = offset,
        timezone = zone,
        hourly = OpenMeteoHourlyDto(
            time = List(hours) { start + it * 3600L },
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
        assertEquals(5.0, day.uvIndexMax ?: -1.0, 0.0001)
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
    fun `keeps the current hour in a half-hour timezone`() {
        // India (+5:30): slots start at xx:30 UTC. At 10:40 local (05:10 UTC) the 10:00 local slot
        // (04:30 UTC) is the current hour and must stay.
        val start = dayStart - 1800 // 23:30 UTC = 05:00 local
        val now = dayStart + 5 * 3600 + 600
        val first = OpenMeteoForecastSource
            .toDayForecasts(response(24, offset = 19_800, zone = "Asia/Kolkata", start = start), WeatherUnits.METRIC, now)
            .first()
            .hourlySlots
            .first()
        assertEquals("10 AM", first.timeLabel)
        assertEquals(10, first.localHour)
    }

    @Test
    fun `labels follow a DST change inside the forecast window`() {
        // London leaves BST at 01:00 UTC on 2026-10-25. Fetched before, with offset +3600, the
        // 12:00 UTC slot after the change must read noon, not 1 PM.
        val noonUtcAfter = 1_792_929_600L // 2026-10-25T12:00:00Z
        val days = OpenMeteoForecastSource.toDayForecasts(
            response(1, offset = 3600, zone = "Europe/London", start = noonUtcAfter),
            WeatherUnits.METRIC,
            noonUtcAfter,
        )
        assertEquals(
            12,
            days
                .single()
                .hourlySlots
                .single()
                .localHour,
        )
    }

    @Test
    fun `missing uv stays unknown rather than zero`() {
        val r = response(2)
        val noUv = r.copy(hourly = r.hourly.copy(uvIndex = emptyList()))
        val day = OpenMeteoForecastSource.toDayForecasts(noUv, WeatherUnits.METRIC, dayStart).single()
        assertEquals(null, day.hourlySlots.first().uvIndex)
        assertEquals(null, day.uvIndexMax)
    }

    @Test
    fun `hours missing a temperature are skipped`() {
        val r = response(3)
        val gappy = r.copy(hourly = r.hourly.copy(temperature = listOf(20.0, null, 21.0)))
        val slots = OpenMeteoForecastSource.toDayForecasts(gappy, WeatherUnits.METRIC, dayStart).single().hourlySlots
        assertEquals(2, slots.size)
    }
}
