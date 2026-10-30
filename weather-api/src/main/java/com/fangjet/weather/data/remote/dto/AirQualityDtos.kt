package com.fangjet.weather.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Open-Meteo's hourly air quality forecast with `timeformat=unixtime`, for the walk planner. */
@Serializable
data class AirQualityHourlyResponse(
    @SerialName("timezone") val timezone: String? = null,
    @SerialName("hourly") val hourly: AirQualityHourlyDto = AirQualityHourlyDto(),
)

@Serializable
data class AirQualityHourlyDto(
    @SerialName("time") val time: List<Long> = emptyList(),
    @SerialName("us_aqi") val usAqi: List<Int?> = emptyList(),
    @SerialName("european_aqi") val europeanAqi: List<Int?> = emptyList(),
)

/** Open-Meteo's air quality response with `timezone=auto`, only the fields the tile uses. */
@Serializable
data class AirQualityResponse(
    @SerialName("timezone") val timezone: String? = null,
    @SerialName("current") val current: AirQualityCurrentDto = AirQualityCurrentDto(),
)

@Serializable
data class AirQualityCurrentDto(
    @SerialName("us_aqi") val usAqi: Int? = null,
    @SerialName("us_aqi_pm2_5") val usAqiPm25: Int? = null,
    @SerialName("us_aqi_pm10") val usAqiPm10: Int? = null,
    @SerialName("us_aqi_ozone") val usAqiOzone: Int? = null,
    @SerialName("us_aqi_nitrogen_dioxide") val usAqiNitrogenDioxide: Int? = null,
    @SerialName("us_aqi_sulphur_dioxide") val usAqiSulphurDioxide: Int? = null,
    @SerialName("us_aqi_carbon_monoxide") val usAqiCarbonMonoxide: Int? = null,
    @SerialName("european_aqi") val europeanAqi: Int? = null,
    @SerialName("european_aqi_pm2_5") val europeanAqiPm25: Int? = null,
    @SerialName("european_aqi_pm10") val europeanAqiPm10: Int? = null,
    @SerialName("european_aqi_ozone") val europeanAqiOzone: Int? = null,
    @SerialName("european_aqi_nitrogen_dioxide") val europeanAqiNitrogenDioxide: Int? = null,
    @SerialName("european_aqi_sulphur_dioxide") val europeanAqiSulphurDioxide: Int? = null,
) {
    companion object {
        const val FIELDS =
            "us_aqi,us_aqi_pm2_5,us_aqi_pm10,us_aqi_ozone,us_aqi_nitrogen_dioxide,us_aqi_sulphur_dioxide," +
                "us_aqi_carbon_monoxide,european_aqi,european_aqi_pm2_5,european_aqi_pm10,european_aqi_ozone," +
                "european_aqi_nitrogen_dioxide,european_aqi_sulphur_dioxide"
    }
}
