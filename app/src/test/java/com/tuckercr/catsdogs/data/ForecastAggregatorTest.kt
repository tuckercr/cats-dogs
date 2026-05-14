package com.tuckercr.catsdogs.data

import com.tuckercr.catsdogs.domain.WeatherUnits
import org.junit.Assert.assertEquals
import org.junit.Test
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
}
