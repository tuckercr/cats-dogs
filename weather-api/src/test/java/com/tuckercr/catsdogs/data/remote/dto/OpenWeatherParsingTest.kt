package com.tuckercr.catsdogs.data.remote.dto

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
        assertEquals(0.0, parsed.main.tempMin, 0.0001)
        assertEquals(0.0, parsed.main.tempMax, 0.0001)
        assertEquals(0, parsed.main.pressure)
        assertEquals(4.2, parsed.wind.speed, 0.0001)
        assertEquals(0, parsed.wind.deg)
        assertNull(parsed.wind.gust)
        assertNull(parsed.visibility)
        assertNull(parsed.clouds)
    }

    @Test
    fun `parses current weather detail fields`() {
        val jsonStr =
            """
            {
              "name": "Portland",
              "weather": [
                { "main": "Rain", "description": "light rain", "icon": "10d" }
              ],
              "main": {
                "temp": 14.0,
                "feels_like": 12.5,
                "temp_min": 10.0,
                "temp_max": 17.0,
                "humidity": 88,
                "pressure": 1005
              },
              "wind": { "speed": 3.6, "deg": 200, "gust": 6.1 },
              "visibility": 8000,
              "clouds": { "all": 90 }
            }
            """.trimIndent()

        val parsed = json.decodeFromString<CurrentWeatherResponse>(jsonStr)

        assertEquals("Portland", parsed.name)
        assertEquals(10.0, parsed.main.tempMin, 0.0001)
        assertEquals(17.0, parsed.main.tempMax, 0.0001)
        assertEquals(1005, parsed.main.pressure)
        assertEquals(3.6, parsed.wind.speed, 0.0001)
        assertEquals(200, parsed.wind.deg)
        assertEquals(6.1, parsed.wind.gust ?: 0.0, 0.0001)
        assertEquals(8000, parsed.visibility)
        assertEquals(90, parsed.clouds?.all)
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
        val item = parsed.list.single()
        assertEquals(1700000000L, item.dt)
        assertEquals(0.0, item.main.tempMin, 0.0001)
        assertEquals(0.0, item.main.tempMax, 0.0001)
        assertEquals(0, item.main.pressure)
        assertEquals(0, item.wind.deg)
        assertNull(item.wind.gust)
        assertEquals(
            "Rain",
            item.weather.single().main,
        )
        assertEquals("Denver", parsed.city?.name)
    }
}
