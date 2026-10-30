package com.fangjet.weather.data

/**
 * Chooses the forecast provider for locations with coordinates. Supplied by the app (from
 * Remote Config `open_meteo_enabled`) so this module stays free of Firebase.
 *
 * When [openMeteoEnabled] is true, [WeatherRepository] uses Open-Meteo (true hourly, UV,
 * pavement heat); when false it falls back to the OpenWeatherMap 3-hourly forecast by lat/lon.
 */
fun interface ForecastSourcePolicy {
    fun openMeteoEnabled(): Boolean

    /** Remote Config `air_quality_enabled`: false skips the air quality fetch. */
    fun airQualityEnabled(): Boolean = true

    companion object {
        /** Open-Meteo whenever coordinates are known — the behaviour before the switch existed. */
        val OpenMeteoPreferred = ForecastSourcePolicy { true }
    }
}
