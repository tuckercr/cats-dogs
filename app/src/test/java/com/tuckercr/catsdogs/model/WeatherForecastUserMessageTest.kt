package com.tuckercr.catsdogs.model

import org.junit.Assert.assertEquals
import org.junit.Test

class WeatherForecastUserMessageTest {

    @Test
    fun `maps repository sentinel errors to user-facing copy`() {
        val expectedMessages = mapOf(
            "missing_api_key" to "Weather API key is missing. Add OWM_API_KEY to local.properties and rebuild.",
            "empty_query" to "Please enter a city name.",
            "network" to "We could not reach the weather service. Check your connection and try again.",
        )

        expectedMessages.forEach { (errorMessage, userMessage) ->
            assertEquals(
                userMessage,
                resolveWeatherForecastUserMessage(IllegalStateException(errorMessage)),
            )
        }
    }

    @Test
    fun `preserves non-blank service errors for weather failures`() {
        val message = resolveWeatherForecastUserMessage(IllegalStateException("city not found"))

        assertEquals("city not found", message)
    }

    @Test
    fun `uses generic fallback for blank weather failures`() {
        val expected = "Weather data is not available right now. Please try again later."

        assertEquals(expected, resolveWeatherForecastUserMessage(IllegalStateException("   ")))
        assertEquals(expected, resolveWeatherForecastUserMessage(Throwable()))
    }
}
