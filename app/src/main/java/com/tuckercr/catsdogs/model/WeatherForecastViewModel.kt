package com.tuckercr.catsdogs.model

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tuckercr.catsdogs.data.PreferencesRepository
import com.tuckercr.catsdogs.data.WeatherRepository
import com.tuckercr.catsdogs.domain.CurrentWeather
import com.tuckercr.catsdogs.domain.DayForecast
import com.tuckercr.catsdogs.domain.WeatherUnits
import com.tuckercr.catsdogs.util.resolveWeatherUnits
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject

@HiltViewModel
class WeatherForecastViewModel internal constructor(
    private val resolveUnits: () -> WeatherUnits,
    private val fetchCurrentWeather: suspend (
        units: WeatherUnits,
        locationLabel: String,
        cityQuery: String?,
        latitude: Double?,
        longitude: Double?,
    ) -> Result<CurrentWeather>,
    private val fetchForecast: suspend (
        units: WeatherUnits,
        cityQuery: String?,
        latitude: Double?,
        longitude: Double?,
    ) -> Result<List<DayForecast>>,
    private val saveLastCity: suspend (String) -> Unit,
) : ViewModel() {

    @Inject
    constructor(
        application: Application,
        preferencesRepository: PreferencesRepository,
        weatherRepository: WeatherRepository,
    ) : this(
        resolveUnits = { application.resolveWeatherUnits() },
        fetchCurrentWeather = weatherRepository::fetchCurrentWeather,
        fetchForecast = weatherRepository::fetchForecast,
        saveLastCity = preferencesRepository::setLastCity,
    )

    private val _currentWeather = MutableStateFlow<LoadingState<CurrentWeather>>(LoadingState.Idle)
    val currentWeather: StateFlow<LoadingState<CurrentWeather>> = _currentWeather.asStateFlow()

    private val _forecast = MutableStateFlow<LoadingState<List<DayForecast>>>(LoadingState.Idle)
    val forecast: StateFlow<LoadingState<List<DayForecast>>> = _forecast.asStateFlow()

    /** City name from the last successful current-weather response (used for forecast navigation). */
    private val _resolvedCity = MutableStateFlow<String?>(null)
    val resolvedCity: StateFlow<String?> = _resolvedCity.asStateFlow()

    private val currentWeatherFetchGeneration = AtomicInteger(0)
    private val forecastFetchGeneration = AtomicInteger(0)

    fun refreshCurrent(
        city: String,
        latitude: Double?,
        longitude: Double?,
    ) {
        val trimmedCity = city.trim()
        val lat = latitude
        val lon = longitude
        if ((lat == null || lon == null) && trimmedCity.isEmpty()) {
            _currentWeather.value = LoadingState.Error("Please enter a city name.", canRetry = false)
            return
        }
        val fetchId = currentWeatherFetchGeneration.incrementAndGet()
        viewModelScope.launch {
            _currentWeather.value = LoadingState.Loading
            val units = resolveUnits()
            val result = if (lat != null && lon != null) {
                fetchCurrentWeather(
                    units,
                    trimmedCity,
                    null,
                    lat,
                    lon,
                )
            } else {
                fetchCurrentWeather(
                    units,
                    trimmedCity,
                    trimmedCity,
                    null,
                    null,
                )
            }
            if (fetchId != currentWeatherFetchGeneration.get()) return@launch
            _currentWeather.value = result.fold(
                onSuccess = { weather ->
                    _resolvedCity.value = weather.cityName
                    saveLastCity(weather.cityName)
                    LoadingState.Success(weather)
                },
                onFailure = { e ->
                    LoadingState.Error(resolveUserMessage(e), canRetry = true)
                },
            )
        }
    }

    fun refreshForecast(
        resolvedCityName: String,
        latitude: Double?,
        longitude: Double?,
    ) {
        val lat = latitude
        val lon = longitude
        val name = resolvedCityName.trim()
        if ((lat == null || lon == null) && name.isEmpty()) {
            _forecast.value = LoadingState.Error("Load current weather first to pick a location.", canRetry = false)
            return
        }
        val fetchId = forecastFetchGeneration.incrementAndGet()
        viewModelScope.launch {
            _forecast.value = LoadingState.Loading
            val units = resolveUnits()
            val result = if (lat != null && lon != null) {
                fetchForecast(
                    units,
                    null,
                    lat,
                    lon,
                )
            } else {
                fetchForecast(
                    units,
                    name,
                    null,
                    null,
                )
            }
            if (fetchId != forecastFetchGeneration.get()) return@launch
            _forecast.value = result.fold(
                onSuccess = { LoadingState.Success(it) },
                onFailure = { e -> LoadingState.Error(resolveUserMessage(e), canRetry = true) },
            )
        }
    }

    fun clearCurrentError() {
        _currentWeather.update { if (it is LoadingState.Error) LoadingState.Idle else it }
    }

    fun clearForecastError() {
        _forecast.update { if (it is LoadingState.Error) LoadingState.Idle else it }
    }

    private fun resolveUserMessage(error: Throwable): String =
        when (error.message) {
            "missing_api_key" ->
                "Weather API key is missing. Add OWM_API_KEY to local.properties and rebuild."
            "empty_query" ->
                "Please enter a city name."
            "network" ->
                "We could not reach the weather service. Check your connection and try again."
            else -> {
                val raw = error.message?.takeIf { it.isNotBlank() }
                raw ?: "Weather data is not available right now. Please try again later."
            }
        }
}
