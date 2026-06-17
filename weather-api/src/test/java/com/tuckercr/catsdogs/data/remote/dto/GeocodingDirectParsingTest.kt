package com.tuckercr.catsdogs.data.remote.dto

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GeocodingDirectParsingTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `parses geocoding result when state is omitted`() {
        val jsonStr =
            """
            [
              {
                "name": "London",
                "lat": 51.5072,
                "lon": -0.1276,
                "country": "GB"
              }
            ]
            """.trimIndent()

        val parsed = json.decodeFromString<List<GeocodingDirectDto>>(jsonStr).single()

        assertEquals("London", parsed.name)
        assertEquals(51.5072, parsed.lat, 0.0001)
        assertEquals(-0.1276, parsed.lon, 0.0001)
        assertEquals("GB", parsed.country)
        assertNull(parsed.state)
    }
}
