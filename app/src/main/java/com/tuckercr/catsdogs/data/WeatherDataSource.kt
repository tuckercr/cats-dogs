package com.tuckercr.catsdogs.data

import com.tuckercr.catsdogs.domain.CurrentWeather
import com.tuckercr.catsdogs.domain.DayForecast
import com.tuckercr.catsdogs.domain.WeatherUnits

interface WeatherDataSource {
    suspend fun fetchCurrentWeather(
        units: WeatherUnits,
        locationLabel: String = "",
        cityQuery: String? = null,
        latitude: Double? = null,
        longitude: Double? = null,
    ): Result<CurrentWeather>

    suspend fun fetchForecast(
        units: WeatherUnits,
        cityQuery: String? = null,
        latitude: Double? = null,
        longitude: Double? = null,
    ): Result<List<DayForecast>>
}
