package com.tuckercr.catsdogs.data.remote

import com.tuckercr.catsdogs.data.remote.dto.CurrentWeatherResponse
import com.tuckercr.catsdogs.data.remote.dto.ForecastResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface OpenWeatherApi {

    /** Pass either [cityQuery] or both [latitude] and [longitude]; coordinates take precedence. */
    @GET("data/2.5/weather")
    suspend fun currentWeather(
        @Query("q") cityQuery: String? = null,
        @Query("lat") latitude: Double? = null,
        @Query("lon") longitude: Double? = null,
        @Query("appid") apiKey: String,
        @Query("units") units: String = UNITS_METRIC,
    ): CurrentWeatherResponse

    /** Pass either [cityQuery] or both [latitude] and [longitude]; coordinates take precedence. */
    @GET("data/2.5/forecast")
    suspend fun forecast(
        @Query("q") cityQuery: String? = null,
        @Query("lat") latitude: Double? = null,
        @Query("lon") longitude: Double? = null,
        @Query("appid") apiKey: String,
        @Query("units") units: String = UNITS_METRIC,
    ): ForecastResponse

    companion object {
        const val UNITS_METRIC = "metric"
        const val UNITS_IMPERIAL = "imperial"
    }
}
