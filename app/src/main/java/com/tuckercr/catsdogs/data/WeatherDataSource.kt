package com.tuckercr.catsdogs.data

import com.tuckercr.catsdogs.domain.CurrentWeather
import com.tuckercr.catsdogs.domain.DayForecast
import com.tuckercr.catsdogs.domain.WeatherUnits

interface WeatherDataSource {
    suspend fun fetchCurrentWeather(
        units: WeatherUnits,
        locationLabel: String,
        cityQuery: String?,
        latitude: Double?,
        longitude: Double?,
    ): Result<CurrentWeather>

    suspend fun fetchForecast(
        units: WeatherUnits,
        cityQuery: String?,
        latitude: Double?,
        longitude: Double?,
    ): Result<List<DayForecast>>
}
