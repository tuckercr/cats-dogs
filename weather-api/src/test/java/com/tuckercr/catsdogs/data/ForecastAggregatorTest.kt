package com.tuckercr.catsdogs.data

import com.tuckercr.catsdogs.domain.WeatherUnits
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId
import java.time.ZoneOffset

class ForecastAggregatorTest {
    private val utc = ZoneOffset.UTC

    @Test
    fun `aggregate picks noon slot per day`() {
        val dayStart = 1_704_067_200L // 2024-01-01T00:00:00Z
        val noon = dayStart + 12 * 3600
        val slots = listOf(
            ForecastAggregator.Slot(
                epochSeconds = dayStart + 3600,
                temperature = 1.0,
                feelsLike = 0.0,
                conditionMain = "Morning",
                description = "morning",
                iconCode = "01d",
            ),
            ForecastAggregator.Slot(
                epochSeconds = noon,
                temperature = 10.0,
                feelsLike = 9.0,
                conditionMain = "Noon",
                description = "noon",
                iconCode = "02d",
            ),
            ForecastAggregator.Slot(
                epochSeconds = dayStart + 18 * 3600,
                temperature = 3.0,
                feelsLike = 2.0,
                conditionMain = "Evening",
                description = "evening",
                iconCode = "03d",
            ),
        )

        val days = ForecastAggregator.aggregate(slots, utc, WeatherUnits.METRIC)

        assertEquals(1, days.size)
        assertEquals("Noon", days.first().conditionMain)
        assertEquals(10.0, days.first().temperature, 0.0001)
    }

    @Test
    fun `aggregate splits multiple calendar days`() {
        val day1 = 1_704_067_200L // 2024-01-01T00:00:00Z
        val day2 = day1 + 86_400L // 2024-01-02T00:00:00Z
        val slots = listOf(
            ForecastAggregator.Slot(
                epochSeconds = day1,
                temperature = 1.0,
                feelsLike = 1.0,
                conditionMain = "A",
                description = "a",
                iconCode = "01d",
            ),
            ForecastAggregator.Slot(
                epochSeconds = day2,
                temperature = 2.0,
                feelsLike = 2.0,
                conditionMain = "B",
                description = "b",
                iconCode = "02d",
            ),
        )

        val days = ForecastAggregator.aggregate(slots, utc, WeatherUnits.METRIC)

        assertEquals(2, days.size)
        assertEquals("B", days.last().conditionMain)
    }

    @Test
    fun `aggregate groups slots by provided local time zone instead of UTC`() {
        val losAngeles = ZoneId.of("America/Los_Angeles")
        val slots = listOf(
            // Both timestamps are Jan 2 in UTC, but the first is Jan 1 at 11 PM locally.
            slot(
                epochSeconds = 1_704_177_600L, // 2024-01-02T07:00:00Z
                conditionMain = "LateNight",
            ),
            slot(
                epochSeconds = 1_704_224_000L, // 2024-01-02T20:00:00Z
                conditionMain = "Noon",
            ),
        )

        val days = ForecastAggregator.aggregate(slots, losAngeles, WeatherUnits.METRIC)

        assertEquals(2, days.size)
        assertEquals("LateNight", days[0].conditionMain)
        assertEquals("Noon", days[1].conditionMain)
    }

    @Test
    fun `aggregate returns empty list for empty input`() {
        val days = ForecastAggregator.aggregate(emptyList(), utc, WeatherUnits.METRIC)
        assertTrue(days.isEmpty())
    }

    @Test
    fun `aggregate produces hourly slots for each slot in the day`() {
        val dayStart = 1_704_067_200L // 2024-01-01T00:00:00Z (Monday)
        val slots = listOf(
            slot(epochSeconds = dayStart, conditionMain = "A", iconCode = "01n"),
            slot(epochSeconds = dayStart + 3 * 3600, conditionMain = "B", iconCode = "02d"),
            slot(epochSeconds = dayStart + 6 * 3600, conditionMain = "C", iconCode = "03d"),
        )

        val day = ForecastAggregator.aggregate(slots, utc, WeatherUnits.METRIC).single()

        assertEquals(3, day.hourlySlots.size)
        assertEquals("01n", day.hourlySlots[0].iconCode)
        assertEquals("02d", day.hourlySlots[1].iconCode)
        assertEquals("03d", day.hourlySlots[2].iconCode)
    }

    @Test
    fun `hourly slots carry wind, humidity, and pressure from the source slot`() {
        val dayStart = 1_704_067_200L // 2024-01-01T00:00:00Z
        val slots = listOf(
            slot(
                epochSeconds = dayStart + 12 * 3600,
                windSpeed = 5.5,
                windDeg = 270,
                humidity = 65,
                pressure = 1008,
            ),
        )

        val hourly = ForecastAggregator
            .aggregate(slots, utc, WeatherUnits.METRIC)
            .single()
            .hourlySlots
            .single()

        assertEquals(5.5, hourly.windSpeed, 0.001)
        assertEquals(270, hourly.windDeg)
        assertEquals(65, hourly.humidity)
        assertEquals(1008, hourly.pressure)
    }

    @Test
    fun `hourly slots are ordered chronologically within a day`() {
        val dayStart = 1_704_067_200L
        // intentionally out of order
        val slots = listOf(
            slot(epochSeconds = dayStart + 9 * 3600, conditionMain = "Nine"),
            slot(epochSeconds = dayStart + 3 * 3600, conditionMain = "Three"),
            slot(epochSeconds = dayStart + 18 * 3600, conditionMain = "Eighteen"),
        )

        val day = ForecastAggregator.aggregate(slots, utc, WeatherUnits.METRIC).single()

        assertEquals("Three", day.hourlySlots[0].description)
        assertEquals("Nine", day.hourlySlots[1].description)
        assertEquals("Eighteen", day.hourlySlots[2].description)
    }

    @Test
    fun `daily min and max are computed across all slots in the day`() {
        val dayStart = 1_704_067_200L
        val slots = listOf(
            slot(epochSeconds = dayStart + 3 * 3600, tempMin = 8.0, tempMax = 15.0),
            slot(epochSeconds = dayStart + 12 * 3600, tempMin = 14.0, tempMax = 22.0),
            slot(epochSeconds = dayStart + 18 * 3600, tempMin = 10.0, tempMax = 17.0),
        )

        val day = ForecastAggregator.aggregate(slots, utc, WeatherUnits.METRIC).single()

        assertEquals(8.0, day.tempMin, 0.001)
        assertEquals(22.0, day.tempMax, 0.001)
    }

    @Test
    fun `daily min and max fall back to temperature when slot min-max are zero`() {
        val dayStart = 1_704_067_200L
        val slots = listOf(
            slot(
                epochSeconds = dayStart + 6 * 3600,
                temperature = 5.0,
                tempMin = 0.0,
                tempMax = 0.0,
            ),
            slot(
                epochSeconds = dayStart + 12 * 3600,
                temperature = 20.0,
                tempMin = 0.0,
                tempMax = 0.0,
            ),
        )

        val day = ForecastAggregator.aggregate(slots, utc, WeatherUnits.METRIC).single()

        assertEquals(5.0, day.tempMin, 0.001)
        assertEquals(20.0, day.tempMax, 0.001)
    }

    @Test
    fun `description is title-cased in both day representative and hourly slots`() {
        val dayStart = 1_704_067_200L
        val slots = listOf(
            slot(
                epochSeconds = dayStart + 12 * 3600,
                conditionMain = "Rain",
                description = "light rain",
            ),
        )

        val day = ForecastAggregator.aggregate(slots, utc, WeatherUnits.METRIC).single()

        assertEquals("Light rain", day.description)
        assertEquals("Light rain", day.hourlySlots.single().description)
    }

    // --- helpers ---

    private fun slot(
        epochSeconds: Long,
        temperature: Double = 15.0,
        feelsLike: Double = 14.0,
        tempMin: Double = 0.0,
        tempMax: Double = 0.0,
        conditionMain: String = "Clear",
        description: String = conditionMain.lowercase(),
        iconCode: String = "01d",
        windSpeed: Double = 0.0,
        windDeg: Int = 0,
        humidity: Int = 0,
        pressure: Int = 0,
    ) = ForecastAggregator.Slot(
        epochSeconds = epochSeconds,
        temperature = temperature,
        feelsLike = feelsLike,
        tempMin = tempMin,
        tempMax = tempMax,
        conditionMain = conditionMain,
        description = description,
        iconCode = iconCode,
        windSpeed = windSpeed,
        windDeg = windDeg,
        humidity = humidity,
        pressure = pressure,
    )
}
