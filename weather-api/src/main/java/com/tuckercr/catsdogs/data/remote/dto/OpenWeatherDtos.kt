@file:OptIn(kotlinx.serialization.InternalSerializationApi::class)

package com.tuckercr.catsdogs.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CurrentWeatherResponse(
    @SerialName("name") val name: String,
    @SerialName("weather") val weather: List<WeatherDescDto>,
    @SerialName("main") val main: MainDto,
    @SerialName("wind") val wind: WindDto,
    @SerialName("visibility") val visibility: Int? = null,
    @SerialName("clouds") val clouds: CloudsDto? = null,
)

@Serializable
data class CloudsDto(
    @SerialName("all") val all: Int = 0,
)

@Serializable
data class ForecastResponse(
    @SerialName("list") val list: List<ForecastListItemDto>,
    @SerialName("city") val city: CityDto?,
)

@Serializable
data class ForecastListItemDto(
    @SerialName("dt") val dt: Long,
    @SerialName("main") val main: MainDto,
    @SerialName("weather") val weather: List<WeatherDescDto>,
    @SerialName("wind") val wind: WindDto,
)

@Serializable
data class CityDto(
    @SerialName("name") val name: String?,
)

@Serializable
data class WeatherDescDto(
    @SerialName("main") val main: String,
    @SerialName("description") val description: String,
    @SerialName("icon") val icon: String,
)

@Serializable
data class MainDto(
    @SerialName("temp") val temp: Double,
    @SerialName("feels_like") val feelsLike: Double,
    @SerialName("temp_min") val tempMin: Double = 0.0,
    @SerialName("temp_max") val tempMax: Double = 0.0,
    @SerialName("humidity") val humidity: Int,
    @SerialName("pressure") val pressure: Int = 0,
)

@Serializable
data class WindDto(
    @SerialName("speed") val speed: Double,
    @SerialName("deg") val deg: Int = 0,
    @SerialName("gust") val gust: Double? = null,
)

@Serializable
data class OpenWeatherErrorResponse(
    @SerialName("cod") val code: String?,
    @SerialName("message") val message: String?,
)
