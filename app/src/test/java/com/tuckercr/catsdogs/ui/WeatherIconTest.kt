package com.tuckercr.catsdogs.ui

import com.tuckercr.catsdogs.R
import org.junit.Assert.assertEquals
import org.junit.Test

class WeatherIconTest {

    @Test
    fun `night icons show the moon's phase`() {
        assertEquals(R.drawable.wx_01d, weatherIconRes("01d", MoonPhase.NEW))
        assertEquals(R.drawable.wx_01n_full, weatherIconRes("01n", MoonPhase.FULL))
        assertEquals(R.drawable.wx_01n_new, weatherIconRes("01n", MoonPhase.NEW))
        assertEquals(R.drawable.wx_02n_first_quarter, weatherIconRes("02n", MoonPhase.FIRST_QUARTER))
        assertEquals(R.drawable.wx_10n_waning_crescent, weatherIconRes("10n", MoonPhase.WANING_CRESCENT))
    }

    @Test
    fun `the southern hemisphere's moon is lit from the other side, except new and full`() {
        assertEquals(R.drawable.wx_01n_waxing_crescent_s, weatherIconRes("01n", MoonPhase.WAXING_CRESCENT, southernHemisphere = true))
        assertEquals(R.drawable.wx_10n_last_quarter_s, weatherIconRes("10n", MoonPhase.LAST_QUARTER, southernHemisphere = true))
        assertEquals(R.drawable.wx_01n_full, weatherIconRes("01n", MoonPhase.FULL, southernHemisphere = true))
        assertEquals(R.drawable.wx_02n_new, weatherIconRes("02n", MoonPhase.NEW, southernHemisphere = true))
    }

    @Test
    fun `other conditions share one icon by day and night`() {
        listOf("03", "04", "09", "11", "13", "50").forEach { code ->
            assertEquals(code, weatherIconRes("${code}d"), weatherIconRes("${code}n"))
        }
    }

    @Test
    fun `an unknown code falls back to a cloud`() {
        assertEquals(R.drawable.wx_03d, weatherIconRes(""))
        assertEquals(R.drawable.wx_03d, weatherIconRes("99d"))
    }
}
