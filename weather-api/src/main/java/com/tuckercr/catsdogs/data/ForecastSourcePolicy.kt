package com.tuckercr.catsdogs.data

/**
 * Chooses the forecast provider for locations with coordinates. Supplied by the app (from
 * Remote Config `open_meteo_enabled`) so this module stays free of Firebase.
 *
 * When [openMeteoEnabled] is true, [WeatherRepository] uses Open-Meteo (true hourly, UV,
 * pavement heat); when false it falls back to the OpenWeatherMap 3-hourly forecast by lat/lon.
 */
fun interface ForecastSourcePolicy {
    fun openMeteoEnabled(): Boolean

    companion object {
        /** Open-Meteo whenever coordinates are known — the behaviour before the switch existed. */
        val OpenMeteoPreferred = ForecastSourcePolicy { true }
    }
}
