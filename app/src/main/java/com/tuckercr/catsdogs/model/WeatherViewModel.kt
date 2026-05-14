package com.tuckercr.catsdogs.model

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tuckercr.catsdogs.data.GeocodingRepository
import com.tuckercr.catsdogs.data.PreferencesRepository
import com.tuckercr.catsdogs.data.WeatherRepository
import com.tuckercr.catsdogs.domain.CitySuggestion
import com.tuckercr.catsdogs.domain.CurrentWeather
import com.tuckercr.catsdogs.domain.DayForecast
import com.tuckercr.catsdogs.util.resolveWeatherUnits
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject

@HiltViewModel
class WeatherViewModel @Inject constructor(
    application: Application,
    private val preferencesRepository: PreferencesRepository,
    private val weatherRepository: WeatherRepository,
    private val geocodingRepository: GeocodingRepository,
) : AndroidViewModel(application) {

    private val _welcomeDone = MutableStateFlow<Boolean?>(null)
    val welcomeDone: StateFlow<Boolean?> = _welcomeDone.asStateFlow()

    private val _cityInput = MutableStateFlow("")
    val cityInput: StateFlow<String> = _cityInput.asStateFlow()

    private val _citySuggestions = MutableStateFlow<List<CitySuggestion>>(emptyList())
    val citySuggestions: StateFlow<List<CitySuggestion>> = _citySuggestions.asStateFlow()

    private val _citySuggestLoading = MutableStateFlow(false)
    val citySuggestLoading: StateFlow<Boolean> = _citySuggestLoading.asStateFlow()

    private val _currentWeather = MutableStateFlow<LoadingState<CurrentWeather>>(LoadingState.Idle)
    val currentWeather: StateFlow<LoadingState<CurrentWeather>> = _currentWeather.asStateFlow()

    private val _forecast = MutableStateFlow<LoadingState<List<DayForecast>>>(LoadingState.Idle)
    val forecast: StateFlow<LoadingState<List<DayForecast>>> = _forecast.asStateFlow()

    /** City name from the last successful current-weather response (used for forecast navigation). */
    private val _resolvedCity = MutableStateFlow<String?>(null)
    val resolvedCity: StateFlow<String?> = _resolvedCity.asStateFlow()

    /** When set (after picking autocomplete), weather + forecast use lat/lon instead of free-text. */
    private var pinnedWeatherLat: Double? = null
    private var pinnedWeatherLon: Double? = null

    private var suggestJob: Job? = null
    private val currentWeatherFetchGeneration = AtomicInteger(0)
    private val forecastFetchGeneration = AtomicInteger(0)

    init {
        viewModelScope.launch {
            val seen = preferencesRepository.hasSeenWelcomeOnce()
            _welcomeDone.value = seen
        }
    }

    fun onCityInputChange(value: String) {
        _cityInput.value = value
        pinnedWeatherLat = null
        pinnedWeatherLon = null
        suggestJob?.cancel()
        val trimmed = value.trim()
        if (trimmed.length < 2) {
            _citySuggestions.value = emptyList()
            _citySuggestLoading.value = false
            return
        }
        suggestJob = viewModelScope.launch {
            _citySuggestLoading.value = true
            delay(280)
            if (_cityInput.value.trim() != trimmed) {
                _citySuggestLoading.value = false
                return@launch
            }
            val result = geocodingRepository.searchCities(trimmed)
            if (_cityInput.value.trim() != trimmed) {
                _citySuggestLoading.value = false
                return@launch
            }
            _citySuggestLoading.value = false
            _citySuggestions.value = result.getOrElse { emptyList() }
        }
    }

    fun onCitySuggestionChosen(suggestion: CitySuggestion) {
        suggestJob?.cancel()
        _citySuggestions.value = emptyList()
        _citySuggestLoading.value = false
        pinnedWeatherLat = suggestion.weatherLat
        pinnedWeatherLon = suggestion.weatherLon
        _cityInput.value = suggestion.label
    }

    fun completeWelcome() {
        viewModelScope.launch {
            preferencesRepository.setHasSeenWelcome(true)
        }
    }

    /** Guard so we only read [PreferencesRepository.lastCity] and auto-refresh once per ViewModel. */
    private var savedCityRestoreDone = false

    /**
     * If the user has a previously saved city, copies it into the search field and loads current
     * weather once. No-op on subsequent calls.
     */
    fun restoreSavedCityOnce() {
        if (savedCityRestoreDone) return
        savedCityRestoreDone = true
        viewModelScope.launch {
            val last = preferencesRepository.lastCity.first()
            if (!last.isNullOrBlank()) {
                _cityInput.value = last
                refreshCurrent(last)
            }
        }
    }

    fun refreshCurrent(explicitCity: String? = null) {
        suggestJob?.cancel()
        _citySuggestions.value = emptyList()
        _citySuggestLoading.value = false

        val city = explicitCity?.trim()?.takeIf { it.isNotEmpty() } ?: _cityInput.value.trim()
        val lat = pinnedWeatherLat
        val lon = pinnedWeatherLon
        if ((lat == null || lon == null) && city.isEmpty()) {
            _currentWeather.value = LoadingState.Error("Please enter a city name.", canRetry = false)
            return
        }
        val fetchId = currentWeatherFetchGeneration.incrementAndGet()
        viewModelScope.launch {
            _currentWeather.value = LoadingState.Loading
            val units = getApplication<Application>().resolveWeatherUnits()
            val result = if (lat != null && lon != null) {
                weatherRepository.fetchCurrentWeather(
                    units = units,
                    locationLabel = city,
                    cityQuery = null,
                    latitude = lat,
                    longitude = lon,
                )
            } else {
                weatherRepository.fetchCurrentWeather(
                    units = units,
                    locationLabel = city,
                    cityQuery = city,
                    latitude = null,
                    longitude = null,
                )
            }
            if (fetchId != currentWeatherFetchGeneration.get()) return@launch
            _currentWeather.value = result.fold(
                onSuccess = { weather ->
                    _resolvedCity.value = weather.cityName
                    preferencesRepository.setLastCity(weather.cityName)
                    LoadingState.Success(weather)
                },
                onFailure = { e ->
                    LoadingState.Error(resolveUserMessage(e), canRetry = true)
                },
            )
        }
    }

    fun refreshForecast() {
        val lat = pinnedWeatherLat
        val lon = pinnedWeatherLon
        val name = _resolvedCity.value?.trim().orEmpty()
        if ((lat == null || lon == null) && name.isEmpty()) {
            _forecast.value = LoadingState.Error("Load current weather first to pick a location.", canRetry = false)
            return
        }
        val fetchId = forecastFetchGeneration.incrementAndGet()
        viewModelScope.launch {
            _forecast.value = LoadingState.Loading
            val units = getApplication<Application>().resolveWeatherUnits()
            val result = if (lat != null && lon != null) {
                weatherRepository.fetchForecast(
                    units = units,
                    cityQuery = null,
                    latitude = lat,
                    longitude = lon,
                )
            } else {
                weatherRepository.fetchForecast(
                    units = units,
                    cityQuery = name,
                    latitude = null,
                    longitude = null,
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
