package com.tuckercr.catsdogs.data

import com.tuckercr.catsdogs.domain.WeatherUnits
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.ZoneId
import java.time.ZoneOffset

class ForecastAggregatorTest {
    private val utc = ZoneOffset.UTC

    @Test
    fun `aggregate returns empty forecast for empty slots`() {
        val days = ForecastAggregator.aggregate(emptyList(), utc, WeatherUnits.METRIC)

        assertEquals(0, days.size)
    }

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
    fun `aggregate groups forecast slots by local date`() {
        val dayStart = 1_704_067_200L // 2024-01-01T00:00:00Z
        val losAngeles = ZoneId.of("America/Los_Angeles")
        val slots = listOf(
            ForecastAggregator.Slot(
                epochSeconds = dayStart + 7 * 3600, // 2023-12-31T23:00:00-08:00
                temperature = 1.0,
                feelsLike = 1.0,
                conditionMain = "PriorDay",
                description = "prior day",
                iconCode = "01d",
            ),
            ForecastAggregator.Slot(
                epochSeconds = dayStart + 20 * 3600, // 2024-01-01T12:00:00-08:00
                temperature = 2.0,
                feelsLike = 2.0,
                conditionMain = "LocalNoon",
                description = "local noon",
                iconCode = "02d",
            ),
        )

        val days = ForecastAggregator.aggregate(slots, losAngeles, WeatherUnits.IMPERIAL)

        assertEquals(2, days.size)
        assertEquals("PriorDay", days.first().conditionMain)
        assertEquals("Prior day", days.first().description)
        assertEquals("LocalNoon", days.last().conditionMain)
        assertEquals(WeatherUnits.IMPERIAL, days.last().units)
    }
}
