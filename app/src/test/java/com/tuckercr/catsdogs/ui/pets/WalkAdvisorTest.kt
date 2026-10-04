package com.tuckercr.catsdogs.ui.pets

import com.tuckercr.catsdogs.domain.HourlySlot
import com.tuckercr.catsdogs.domain.WeatherUnits
import org.junit.Assert.assertEquals
import org.junit.Test

class WalkAdvisorTest {

    @Test
    fun `great when the next slot is dry and comfortable`() {
        val advice = walkAdviceFor(18.0, listOf(slot("3 PM", 18.0, 10), slot("6 PM", 16.0, 80)))
        assertEquals(WalkAdvice(WalkRating.GREAT, null), advice)
    }

    @Test
    fun `okay with the first good window when it is wet now`() {
        val advice = walkAdviceFor(12.0, listOf(slot("3 PM", 12.0, 90), slot("6 PM", 13.0, 10)))
        assertEquals(WalkAdvice(WalkRating.OKAY, "6 PM"), advice)
    }

    @Test
    fun `poor when there is no good window in the lookahead`() {
        val advice = walkAdviceFor(12.0, List(8) { slot("${it}h", 12.0, 90) })
        assertEquals(WalkAdvice(WalkRating.POOR, null), advice)
    }

    @Test
    fun `paws hot above the heat threshold, pointing at the first cooler slot`() {
        val advice = walkAdviceFor(33.0, listOf(slot("3 PM", 33.0, 0), slot("7 PM", 24.0, 0)))
        assertEquals(WalkAdvice(WalkRating.PAWS_HOT, "7 PM"), advice)
    }

    @Test
    fun `too cold at or below freezing`() {
        assertEquals(WalkRating.TOO_COLD, walkAdviceFor(-3.0, listOf(slot("3 PM", -3.0, 0)))?.rating)
    }

    @Test
    fun `imperial slots are compared in celsius`() {
        // 64F is ~18C: comfortable.
        val advice = walkAdviceFor(18.0, listOf(slot("3 PM", 64.0, 0, WeatherUnits.IMPERIAL)))
        assertEquals(WalkRating.GREAT, advice?.rating)
    }

    @Test
    fun `night slots are skipped in favour of the first daytime window`() {
        val advice = walkAdviceFor(18.0, listOf(slot("2 AM", 18.0, 0), slot("5 AM", 17.0, 0), slot("8 AM", 16.0, 0)))
        assertEquals(WalkAdvice(WalkRating.OKAY, "8 AM"), advice)
    }

    @Test
    fun `no slots means no advice`() {
        assertEquals(null, walkAdviceFor(18.0, emptyList()))
    }

    @Test
    fun `night is judged by local hour, not the label text`() {
        // A label in a locale the old parser couldn't read; the hour still rules it out.
        val advice = walkAdviceFor(18.0, listOf(slot("2 午前", 18.0, 0, hour = 2), slot("8 午前", 16.0, 0, hour = 8)))
        assertEquals("8 午前", advice?.bestTimeLabel)
    }

    @Test
    fun `hot pavement on a mild day means paws hot, pointing at the first cooler hour`() {
        val advice = walkAdviceFor(
            24.0,
            listOf(slot("1 PM", 24.0, 0, pavement = 58.0), slot("6 PM", 22.0, 0, pavement = 30.0)),
        )
        assertEquals(WalkAdvice(WalkRating.PAWS_HOT, "6 PM", 58.0), advice)
    }

    @Test
    fun `imperial pavement is compared in celsius`() {
        // 120F is ~49C: under the 52C paw limit, so a warm afternoon is still walkable.
        val advice = walkAdviceFor(26.0, listOf(slot("2 PM", 79.0, 0, WeatherUnits.IMPERIAL, pavement = 120.0)))
        assertEquals(WalkRating.GREAT, advice?.rating)
    }

    private fun slot(
        label: String,
        temp: Double,
        rainChance: Int,
        units: WeatherUnits = WeatherUnits.METRIC,
        pavement: Double? = null,
        hour: Int? = hourOf(label),
    ) = HourlySlot(
        timeLabel = label,
        iconCode = "01d",
        description = "clear",
        temperature = temp,
        feelsLike = temp,
        windSpeed = 1.0,
        windDeg = 0,
        humidity = 50,
        pressure = 1013,
        units = units,
        precipitationChance = rainChance,
        pavementTemperature = pavement,
        localHour = hour,
    )

    /** Test-only: reads the hour from a "3 PM"-style label. */
    private fun hourOf(label: String): Int? {
        val m = Regex("""^(\d{1,2})\s*([AP])M""").find(label) ?: return null
        val h = m.groupValues[1].toInt() % 12
        return if (m.groupValues[2] == "P") h + 12 else h
    }
}
