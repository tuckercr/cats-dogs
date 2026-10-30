package com.fangjet.weather.domain

import com.fangjet.weather.data.AirQualitySource
import com.fangjet.weather.data.remote.dto.AirQualityHourlyResponse
import com.fangjet.weather.data.remote.dto.AirQualityResponse
import com.fangjet.weather.data.withAirQuality
import com.fangjet.weather.domain.AirQuality.Pollutant
import com.fangjet.weather.domain.AirQuality.Scale
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AirQualityTest {
    private fun us(index: Int) = AirQuality(index, Scale.US, null)

    private fun european(index: Int) = AirQuality(index, Scale.EUROPEAN, null)

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `US bands change above each breakpoint`() {
        assertEquals(
            listOf(0, 0, 1, 1, 2, 2, 3, 3, 4, 4, 5, 5),
            listOf(0, 50, 51, 100, 101, 150, 151, 200, 201, 300, 301, 500).map { us(it).band },
        )
    }

    @Test
    fun `European bands change every twenty`() {
        assertEquals(
            listOf(0, 0, 1, 1, 2, 2, 3, 3, 4, 4, 5),
            listOf(0, 20, 21, 40, 41, 60, 61, 80, 81, 100, 101).map { european(it).band },
        )
    }

    @Test
    fun `bar position splits the bar into six equal bands`() {
        assertEquals(0.0, us(0).barPosition, 0.001)
        assertEquals(1.0 / 12, us(25).barPosition, 0.001)
        assertEquals(2.0 / 6, us(100).barPosition, 0.001)
        assertEquals(1.5 / 6, european(30).barPosition, 0.001)
        assertEquals(1.0, us(900).barPosition, 0.001)
    }

    @Test
    fun `European cities use the European scale`() {
        assertEquals(Scale.EUROPEAN, Scale.forTimeZone("Europe/London"))
        assertEquals(Scale.US, Scale.forTimeZone("America/New_York"))
        assertEquals(Scale.US, Scale.forTimeZone(null))
    }

    @Test
    fun `parses the index and names the biggest pollutant for the city's scale`() {
        val response = json.decodeFromString(
            AirQualityResponse.serializer(),
            """{"timezone":"Europe/London","current":{"us_aqi":67,"us_aqi_pm2_5":67,"us_aqi_ozone":1,
            "european_aqi":50,"european_aqi_pm2_5":45,"european_aqi_nitrogen_dioxide":50,"european_aqi_ozone":1}}""",
        )

        assertEquals(
            AirQuality(50, Scale.EUROPEAN, Pollutant.NITROGEN_DIOXIDE),
            AirQualitySource.toAirQuality(response),
        )
    }

    @Test
    fun `a tie goes to the first listed pollutant`() {
        val response = json.decodeFromString(
            AirQualityResponse.serializer(),
            """{"timezone":"America/New_York","current":{"us_aqi":46,"us_aqi_pm2_5":46,"us_aqi_ozone":46}}""",
        )

        assertEquals(Pollutant.FINE_PARTICLES, AirQualitySource.toAirQuality(response)?.mainPollutant)
    }

    @Test
    fun `hourly bands use the city's scale`() {
        val response = json.decodeFromString(
            AirQualityHourlyResponse.serializer(),
            """{"timezone":"Europe/Paris","hourly":{"time":[100,3700],"us_aqi":[40,40],"european_aqi":[50,90]}}""",
        )

        assertEquals(mapOf(100L to 2, 3700L to 4), AirQualitySource.toHourlyBands(response))
    }

    @Test
    fun `each slot gets its hour's band`() {
        val slot = HourlySlot("1 AM", "01n", "clear", 10.0, 10.0, 1.0, 0, 50, 1013, WeatherUnits.METRIC, epochSeconds = 100)
        val day =
            DayForecast(
                "Mon",
                "Clear",
                "clear",
                "01d",
                10.0,
                10.0,
                5.0,
                15.0,
                WeatherUnits.METRIC,
                hourlySlots = listOf(slot, slot.copy(epochSeconds = 3700)),
            )

        val bands = withAirQuality(listOf(day), mapOf(3700L to 4)).single().hourlySlots.map { it.airQualityBand }

        assertEquals(listOf(null, 4), bands)
    }

    @Test
    fun `no index means no reading`() {
        val response = json.decodeFromString(
            AirQualityResponse.serializer(),
            """{"timezone":"America/New_York","current":{"us_aqi":null}}""",
        )

        assertNull(AirQualitySource.toAirQuality(response))
    }
}
