package com.tuckercr.catsdogs.data.remote.dto

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class OpenWeatherParsingTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `parses current weather payload`() {
        val jsonStr =
            """
            {
              "name": "Austin",
              "weather": [
                { "main": "Clear", "description": "clear sky", "icon": "01n" }
              ],
              "main": { "temp": 21.3, "feels_like": 20.1, "humidity": 55 },
              "wind": { "speed": 4.2 }
            }
            """.trimIndent()

        val parsed = json.decodeFromString<CurrentWeatherResponse>(jsonStr)

        assertEquals("Austin", parsed.name)
        assertEquals("Clear", parsed.weather.single().main)
        assertEquals(21.3, parsed.main.temp, 0.0001)
        assertEquals(20.1, parsed.main.feelsLike, 0.0001)
        assertEquals(55, parsed.main.humidity)
        assertEquals(4.2, parsed.wind.speed, 0.0001)
    }

    @Test
    fun `parses forecast payload list`() {
        val jsonStr =
            """
            {
              "list": [
                {
                  "dt": 1700000000,
                  "main": { "temp": 5.0, "feels_like": 4.0, "humidity": 80 },
                  "weather": [ { "main": "Rain", "description": "light rain", "icon": "10d" } ],
                  "wind": { "speed": 2.0 }
                }
              ],
              "city": { "name": "Denver" }
            }
            """.trimIndent()

        val parsed = json.decodeFromString<ForecastResponse>(jsonStr)

        assertEquals(1, parsed.list.size)
        assertEquals(1700000000L, parsed.list.single().dt)
        assertEquals(
            "Rain",
            parsed.list
                .single()
                .weather
                .single()
                .main,
        )
        assertEquals("Denver", parsed.city?.name)
    }
}
