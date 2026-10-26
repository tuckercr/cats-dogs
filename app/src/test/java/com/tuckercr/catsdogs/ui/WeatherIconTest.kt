package com.tuckercr.catsdogs.ui

import com.tuckercr.catsdogs.R
import org.junit.Assert.assertEquals
import org.junit.Test

class WeatherIconTest {

    @Test
    fun `clear, few clouds and rain have night versions`() {
        assertEquals(R.drawable.wx_01d, weatherIconRes("01d"))
        assertEquals(R.drawable.wx_01n, weatherIconRes("01n"))
        assertEquals(R.drawable.wx_02n, weatherIconRes("02n"))
        assertEquals(R.drawable.wx_10n, weatherIconRes("10n"))
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
