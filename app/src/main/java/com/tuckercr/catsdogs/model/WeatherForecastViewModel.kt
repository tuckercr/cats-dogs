package com.tuckercr.catsdogs.model

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tuckercr.catsdogs.data.PreferencesRepository
import com.tuckercr.catsdogs.data.WeatherRepository
import com.tuckercr.catsdogs.domain.CurrentWeather
import com.tuckercr.catsdogs.domain.DayForecast
import com.tuckercr.catsdogs.domain.SavedLocation
import com.tuckercr.catsdogs.domain.WeatherUnits
import com.tuckercr.catsdogs.util.resolveWeatherUnits
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject

@HiltViewModel
class WeatherForecastViewModel internal constructor(
    private val resolveWeatherUnits: suspend () -> WeatherUnits,
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
    private val setLastCity: suspend (String) -> Unit,
    private val getCachedCurrentWeather: suspend () -> CurrentWeather?,
    private val getCachedForecast: suspend () -> List<DayForecast>?,
    private val setCachedCurrentWeather: suspend (String) -> Unit,
    private val setCachedForecast: suspend (String) -> Unit,
    private val json: Json,
) : ViewModel() {

    @Inject
    constructor(
        application: Application,
        preferencesRepository: PreferencesRepository,
        weatherRepository: WeatherRepository,
        injectedJson: Json,
    ) : this(
        resolveWeatherUnits = {
            val override = preferencesRepository.unitOverride.first()
            application.resolveWeatherUnits(override)
        },
        fetchCurrentWeather = weatherRepository::fetchCurrentWeather,
        fetchForecast = weatherRepository::fetchForecast,
        setLastCity = preferencesRepository::setLastCity,
        getCachedCurrentWeather = {
            val raw = preferencesRepository.cachedCurrentWeatherJson.first()
            if (raw != null) try { injectedJson.decodeFromString<CurrentWeather>(raw) } catch (_: Exception) { null } else null
        },
        getCachedForecast = {
            val raw = preferencesRepository.cachedForecastJson.first()
            if (raw != null) try { injectedJson.decodeFromString<List<DayForecast>>(raw) } catch (_: Exception) { null } else null
        },
        setCachedCurrentWeather = preferencesRepository::setCachedCurrentWeather,
        setCachedForecast = preferencesRepository::setCachedForecast,
        json = injectedJson,
    )

    private val _currentWeather = MutableStateFlow<LoadingState<CurrentWeather>>(LoadingState.Idle)
    val currentWeather: StateFlow<LoadingState<CurrentWeather>> = _currentWeather.asStateFlow()

    private val _forecast = MutableStateFlow<LoadingState<List<DayForecast>>>(LoadingState.Idle)
    val forecast: StateFlow<LoadingState<List<DayForecast>>> = _forecast.asStateFlow()

    private val currentWeatherFetchGeneration = AtomicInteger(0)
    private val forecastFetchGeneration = AtomicInteger(0)
    private val _lastRefreshedLocation = MutableStateFlow<SavedLocation?>(null)

    init {
        // Pre-populate UI from last background fetch so the user sees data immediately on open.
        viewModelScope.launch {
            getCachedCurrentWeather()?.let { cached ->
                _currentWeather.compareAndSet(LoadingState.Idle, LoadingState.Success(cached))
            }
        }
        viewModelScope.launch {
            getCachedForecast()?.let { cached ->
                _forecast.compareAndSet(LoadingState.Idle, LoadingState.Success(cached))
            }
        }
    }

    fun backgroundRefreshCurrent(location: SavedLocation) {
        _lastRefreshedLocation.value = location
        viewModelScope.launch {
            val units = resolveWeatherUnits()
            val result = if (location.latitude != null && location.longitude != null) {
                fetchCurrentWeather(units, location.label, null, location.latitude, location.longitude)
            } else {
                fetchCurrentWeather(units, location.label, location.label, null, null)
            }
            result.onSuccess { weather ->
                _currentWeather.value = LoadingState.Success(weather)
                setLastCity(weather.cityName)
                viewModelScope.launch {
                    setCachedCurrentWeather(json.encodeToString(CurrentWeather.serializer(), weather))
                }
            }
        }
    }

    fun backgroundRefreshForecast(location: SavedLocation) {
        viewModelScope.launch {
            val units = resolveWeatherUnits()
            val result = if (location.latitude != null && location.longitude != null) {
                fetchForecast(units, null, location.latitude, location.longitude)
            } else {
                fetchForecast(units, location.label, null, null)
            }
            result.onSuccess { forecast ->
                _forecast.value = LoadingState.Success(forecast)
                viewModelScope.launch {
                    setCachedForecast(
                        json.encodeToString(
                            kotlinx.serialization.builtins.ListSerializer(DayForecast.serializer()),
                            forecast,
                        ),
                    )
                }
            }
        }
    }

    fun refreshCurrent(location: SavedLocation) {
        _lastRefreshedLocation.value = location
        val fetchId = currentWeatherFetchGeneration.incrementAndGet()
        viewModelScope.launch {
            _currentWeather.value = LoadingState.Loading
            val units = resolveWeatherUnits()
            val result = if (location.latitude != null && location.longitude != null) {
                fetchCurrentWeather(units, location.label, null, location.latitude, location.longitude)
            } else {
                fetchCurrentWeather(units, location.label, location.label, null, null)
            }
            if (fetchId != currentWeatherFetchGeneration.get()) return@launch
            _currentWeather.value = result.fold(
                onSuccess = { weather ->
                    setLastCity(weather.cityName)
                    viewModelScope.launch {
                        setCachedCurrentWeather(json.encodeToString(CurrentWeather.serializer(), weather))
                    }
                    LoadingState.Success(weather)
                },
                onFailure = { e -> LoadingState.Error(resolveUserMessage(e), canRetry = true) },
            )
        }
    }

    fun refreshForecast(location: SavedLocation) {
        val fetchId = forecastFetchGeneration.incrementAndGet()
        viewModelScope.launch {
            _forecast.value = LoadingState.Loading
            val units = resolveWeatherUnits()
            val result = if (location.latitude != null && location.longitude != null) {
                fetchForecast(units, null, location.latitude, location.longitude)
            } else {
                fetchForecast(units, location.label, null, null)
            }
            if (fetchId != forecastFetchGeneration.get()) return@launch
            _forecast.value = result.fold(
                onSuccess = { forecast ->
                    viewModelScope.launch {
                        setCachedForecast(
                            json.encodeToString(
                                kotlinx.serialization.builtins.ListSerializer(DayForecast.serializer()),
                                forecast,
                            ),
                        )
                    }
                    LoadingState.Success(forecast)
                },
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
