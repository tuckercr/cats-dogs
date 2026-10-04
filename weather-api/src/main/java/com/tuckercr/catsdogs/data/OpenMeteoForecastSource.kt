package com.tuckercr.catsdogs.data

import com.tuckercr.catsdogs.data.remote.dto.OpenMeteoResponse
import com.tuckercr.catsdogs.domain.DayForecast
import com.tuckercr.catsdogs.domain.WeatherUnits
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import javax.inject.Inject
import javax.inject.Singleton

/**
 * True hourly forecast from Open-Meteo (free, no API key), including UV index and the solar
 * radiation used to estimate pavement heat. Needs coordinates; name-only cities fall back to
 * OpenWeatherMap in [WeatherRepository].
 */
@Singleton
class OpenMeteoForecastSource @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val json: Json,
) {
    suspend fun fetch(
        units: WeatherUnits,
        latitude: Double,
        longitude: Double,
    ): Result<List<DayForecast>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val url = BASE_URL
                    .toHttpUrl()
                    .newBuilder()
                    .addQueryParameter("latitude", latitude.toString())
                    .addQueryParameter("longitude", longitude.toString())
                    .addQueryParameter("hourly", HOURLY_FIELDS)
                    .addQueryParameter("timezone", "auto")
                    .addQueryParameter("timeformat", "unixtime")
                    .addQueryParameter("forecast_days", FORECAST_DAYS.toString())
                    .addQueryParameter("temperature_unit", if (units == WeatherUnits.IMPERIAL) "fahrenheit" else "celsius")
                    .addQueryParameter("wind_speed_unit", if (units == WeatherUnits.IMPERIAL) "mph" else "ms")
                    .build()
                okHttpClient.newCall(Request.Builder().url(url).build()).execute().use { response ->
                    if (!response.isSuccessful) error(if (response.code >= 500) "server_error" else "generic")
                    val body = response.body?.string() ?: error("invalid_payload")
                    val parsed = json.decodeFromString(OpenMeteoResponse.serializer(), body)
                    toDayForecasts(parsed, units, System.currentTimeMillis() / 1000)
                }
            }
        }

    companion object {
        private const val BASE_URL = "https://api.open-meteo.com/v1/forecast"
        private const val FORECAST_DAYS = 7
        private const val HOURLY_FIELDS =
            "temperature_2m,apparent_temperature,precipitation_probability,weather_code," +
                "wind_speed_10m,wind_direction_10m,relative_humidity_2m,pressure_msl," +
                "uv_index,shortwave_radiation,is_day"

        /**
         * Converts the parallel hourly arrays into day forecasts in the city's own timezone,
         * dropping hours before the current one so hourly views start at "now".
         */
        internal fun toDayForecasts(
            response: OpenMeteoResponse,
            units: WeatherUnits,
            nowEpochSeconds: Long,
        ): List<DayForecast> {
            val h = response.hourly
            val zone = response.timezone
                ?.let { runCatching { ZoneId.of(it) }.getOrNull() }
                ?: ZoneOffset.ofTotalSeconds(response.utcOffsetSeconds)
            // Slots start on local hour boundaries, which aren't UTC hour boundaries in half-hour
            // zones (India +5:30, Adelaide +9:30, Nepal +5:45), so floor "now" in local time.
            val offset = zone.rules.getOffset(Instant.ofEpochSecond(nowEpochSeconds)).totalSeconds
            val localNow = nowEpochSeconds + offset
            val currentHourStart = localNow - Math.floorMod(localNow, 3600L) - offset
            val slots = h.time.indices.mapNotNull { i ->
                val epoch = h.time[i]
                val temp = h.temperature.getOrNull(i) ?: return@mapNotNull null
                if (epoch < currentHourStart) return@mapNotNull null
                val isDay = h.isDay.getOrNull(i) == 1
                val condition = WmoWeatherCodes.condition(h.weatherCode.getOrNull(i) ?: 3, isDay)
                ForecastAggregator.Slot(
                    epochSeconds = epoch,
                    temperature = temp,
                    feelsLike = h.apparentTemperature.getOrNull(i) ?: temp,
                    conditionMain = condition.main,
                    description = condition.description,
                    iconCode = condition.iconCode,
                    windSpeed = h.windSpeed.getOrNull(i) ?: 0.0,
                    windDeg = h.windDirection.getOrNull(i) ?: 0,
                    humidity = h.relativeHumidity.getOrNull(i) ?: 0,
                    pressure = h.seaLevelPressure.getOrNull(i)?.toInt() ?: 0,
                    pop = (h.precipitationProbability.getOrNull(i) ?: 0) / 100.0,
                    uvIndex = h.uvIndex.getOrNull(i),
                    pavementTemperature = pavementIn(units, temp, h.shortwaveRadiation.getOrNull(i), isDay),
                )
            }
            return ForecastAggregator.aggregate(slots, zone, units)
        }

        private fun pavementIn(
            units: WeatherUnits,
            air: Double,
            radiation: Double?,
            isDay: Boolean,
        ): Double = units.fromCelsius(PavementHeat.estimateC(units.toCelsius(air), radiation, isDay))
    }
}
