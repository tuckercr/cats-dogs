package com.tuckercr.catsdogs.data

import com.tuckercr.catsdogs.data.remote.OpenWeatherApi
import com.tuckercr.catsdogs.data.remote.dto.OpenWeatherErrorResponse
import com.tuckercr.catsdogs.di.AppConfigModule
import com.tuckercr.catsdogs.domain.CurrentWeather
import com.tuckercr.catsdogs.domain.DayForecast
import com.tuckercr.catsdogs.domain.WeatherUnits
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import java.io.IOException
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class WeatherRepository @Inject constructor(
    private val api: OpenWeatherApi,
    @param:Named(AppConfigModule.OWM_API_KEY) private val apiKey: String,
    private val zoneId: ZoneId,
    private val json: Json,
) {

    suspend fun fetchCurrentWeather(
        units: WeatherUnits,
        locationLabel: String = "",
        cityQuery: String? = null,
        latitude: Double? = null,
        longitude: Double? = null,
    ): Result<CurrentWeather> {
        val key = apiKey.trim()
        if (key.isEmpty()) {
            return Result.failure(IllegalStateException("missing_api_key"))
        }
        if (latitude != null && longitude != null) {
            return runCatching {
                val response = api.currentWeather(
                    cityQuery = null,
                    latitude = latitude,
                    longitude = longitude,
                    apiKey = key,
                    units = units.units,
                )
                mapCurrent(response, units, locationLabel)
            }.mapApiFailure()
        }
        val q = cityQuery?.trim().orEmpty()
        if (q.isEmpty()) {
            return Result.failure(IllegalStateException("empty_query"))
        }
        return runCatching {
            val response = api.currentWeather(
                cityQuery = q,
                latitude = null,
                longitude = null,
                apiKey = key,
                units = units.units,
            )
            mapCurrent(response, units, locationLabel)
        }.mapApiFailure()
    }

    suspend fun fetchForecast(
        units: WeatherUnits,
        cityQuery: String? = null,
        latitude: Double? = null,
        longitude: Double? = null,
    ): Result<List<DayForecast>> {
        val key = apiKey.trim()
        if (key.isEmpty()) {
            return Result.failure(IllegalStateException("missing_api_key"))
        }
        if (latitude != null && longitude != null) {
            return runCatching {
                val response = api.forecast(
                    cityQuery = null,
                    latitude = latitude,
                    longitude = longitude,
                    apiKey = key,
                    units = units.units,
                )
                mapForecast(response, units)
            }.mapApiFailure()
        }
        val q = cityQuery?.trim().orEmpty()
        if (q.isEmpty()) {
            return Result.failure(IllegalStateException("empty_query"))
        }
        return runCatching {
            val response = api.forecast(
                cityQuery = q,
                latitude = null,
                longitude = null,
                apiKey = key,
                units = units.units,
            )
            mapForecast(response, units)
        }.mapApiFailure()
    }

    private fun mapForecast(
        response: com.tuckercr.catsdogs.data.remote.dto.ForecastResponse,
        units: WeatherUnits,
    ): List<DayForecast> {
        val slots = response.list.map { item ->
            val w = item.weather.firstOrNull()
                ?: throw IllegalStateException("invalid_payload")
            ForecastAggregator.Slot(
                epochSeconds = item.dt,
                temperature = item.main.temp,
                feelsLike = item.main.feelsLike,
                conditionMain = w.main,
                description = w.description,
                iconCode = w.icon,
            )
        }
        return ForecastAggregator.aggregate(slots, zoneId, units)
    }

    private fun mapCurrent(
        response: com.tuckercr.catsdogs.data.remote.dto.CurrentWeatherResponse,
        units: WeatherUnits,
        locationLabel: String,
    ): CurrentWeather {
        val w = response.weather.firstOrNull() ?: throw IllegalStateException("invalid_payload")
        val displayCity = locationLabel.trim().takeIf { it.isNotEmpty() } ?: response.name
        return CurrentWeather(
            cityName = displayCity,
            conditionMain = w.main,
            description = w.description.replaceFirstChar { char ->
                char.titlecase(java.util.Locale.getDefault())
            },
            iconCode = w.icon,
            temperature = response.main.temp,
            feelsLike = response.main.feelsLike,
            humidityPercent = response.main.humidity,
            windSpeed = response.wind.speed,
            units = units,
        )
    }

    private fun <T> Result<T>.mapApiFailure(): Result<T> = fold(
        onSuccess = { Result.success(it) },
        onFailure = { throwable ->
            Result.failure(mapThrowable(throwable))
        },
    )

    private fun mapThrowable(throwable: Throwable): Throwable = when (throwable) {
        is IOException -> IOException("network", throwable)
        is HttpException -> {
            val body = throwable.response()?.errorBody()?.string().orEmpty()
            val parsed = runCatching { json.decodeFromString<OpenWeatherErrorResponse>(body) }
                .getOrNull()
            val message = parsed?.message?.takeIf { it.isNotBlank() } ?: "http_${throwable.code()}"
            IllegalStateException(message, throwable)
        }
        else -> throwable
    }
}
