package com.fangjet.weather.data

import com.fangjet.weather.data.remote.dto.AirQualityCurrentDto
import com.fangjet.weather.data.remote.dto.AirQualityHourlyResponse
import com.fangjet.weather.data.remote.dto.AirQualityResponse
import com.fangjet.weather.domain.AirQuality
import com.fangjet.weather.domain.AirQuality.Pollutant
import com.fangjet.weather.domain.AirQuality.Scale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton

/** The air quality now, from Open-Meteo's air quality endpoint: free, keyless and worldwide. */
@Singleton
class AirQualitySource @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val json: Json,
) {
    suspend fun fetch(
        latitude: Double,
        longitude: Double,
    ): Result<AirQuality?> =
        get(latitude, longitude, mapOf("current" to AirQualityCurrentDto.FIELDS)) { body ->
            toAirQuality(json.decodeFromString(AirQualityResponse.serializer(), body))
        }

    /** Each hour's band for the next two days (the walk planner's day ahead), by its start in Unix seconds. */
    suspend fun fetchHourlyBands(
        latitude: Double,
        longitude: Double,
    ): Result<Map<Long, Int>> =
        get(
            latitude,
            longitude,
            mapOf("hourly" to "us_aqi,european_aqi", "timeformat" to "unixtime", "forecast_days" to "2"),
        ) { body -> toHourlyBands(json.decodeFromString(AirQualityHourlyResponse.serializer(), body)) }

    private suspend fun <T> get(
        latitude: Double,
        longitude: Double,
        fields: Map<String, String>,
        parse: (String) -> T,
    ): Result<T> =
        withContext(Dispatchers.IO) {
            runCatching {
                val url = BASE_URL
                    .toHttpUrl()
                    .newBuilder()
                    .addQueryParameter("latitude", latitude.toString())
                    .addQueryParameter("longitude", longitude.toString())
                    // The response's time zone picks the scale (European or US).
                    .addQueryParameter("timezone", "auto")
                    .apply { fields.forEach { (name, value) -> addQueryParameter(name, value) } }
                    .build()
                okHttpClient.newCall(Request.Builder().url(url).build()).execute().use { response ->
                    if (!response.isSuccessful) error(if (response.code >= 500) "server_error" else "generic")
                    parse(response.body?.string() ?: error("invalid_payload"))
                }
            }
        }

    companion object {
        private const val BASE_URL = "https://air-quality-api.open-meteo.com/v1/air-quality"

        internal fun toHourlyBands(response: AirQualityHourlyResponse): Map<Long, Int> {
            val scale = Scale.forTimeZone(response.timezone)
            val indices = if (scale == Scale.EUROPEAN) response.hourly.europeanAqi else response.hourly.usAqi
            return response.hourly.time
                .zip(indices)
                .mapNotNull { (time, index) -> index?.let { time to AirQuality(it, scale, null).band } }
                .toMap()
        }

        internal fun toAirQuality(response: AirQualityResponse): AirQuality? {
            val c = response.current
            return AirQuality.from(response.timezone) { scale ->
                when (scale) {
                    Scale.US -> c.usAqi to listOf(
                        Pollutant.FINE_PARTICLES to c.usAqiPm25,
                        Pollutant.COARSE_PARTICLES to c.usAqiPm10,
                        Pollutant.OZONE to c.usAqiOzone,
                        Pollutant.NITROGEN_DIOXIDE to c.usAqiNitrogenDioxide,
                        Pollutant.SULPHUR_DIOXIDE to c.usAqiSulphurDioxide,
                        Pollutant.CARBON_MONOXIDE to c.usAqiCarbonMonoxide,
                    )
                    Scale.EUROPEAN -> c.europeanAqi to listOf(
                        Pollutant.FINE_PARTICLES to c.europeanAqiPm25,
                        Pollutant.COARSE_PARTICLES to c.europeanAqiPm10,
                        Pollutant.OZONE to c.europeanAqiOzone,
                        Pollutant.NITROGEN_DIOXIDE to c.europeanAqiNitrogenDioxide,
                        Pollutant.SULPHUR_DIOXIDE to c.europeanAqiSulphurDioxide,
                    )
                }
            }
        }
    }
}
