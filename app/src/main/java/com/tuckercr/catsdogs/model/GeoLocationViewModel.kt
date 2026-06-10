package com.tuckercr.catsdogs.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tuckercr.catsdogs.data.CitySearchRepository
import com.tuckercr.catsdogs.data.WeatherPreferences
import com.tuckercr.catsdogs.domain.CitySuggestion
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GeoLocationViewModel @Inject constructor(
    private val preferences: WeatherPreferences,
    private val citySearchRepository: CitySearchRepository,
) : ViewModel() {

    private val _cityInput = MutableStateFlow("")
    val cityInput: StateFlow<String> = _cityInput.asStateFlow()

    private val _citySuggestions = MutableStateFlow<List<CitySuggestion>>(emptyList())
    val citySuggestions: StateFlow<List<CitySuggestion>> = _citySuggestions.asStateFlow()

    private val _citySuggestLoading = MutableStateFlow(false)
    val citySuggestLoading: StateFlow<Boolean> = _citySuggestLoading.asStateFlow()

    /** When set (after picking autocomplete), weather + forecast use lat/lon instead of free-text. */
    private var pinnedWeatherLat: Double? = null
    private var pinnedWeatherLon: Double? = null

    private var suggestJob: Job? = null

    /** Guard so we only read [PreferencesRepository.lastCity] once per ViewModel. */
    private var savedCityRestoreDone = false

    fun pinnedLatitude(): Double? = pinnedWeatherLat

    fun pinnedLongitude(): Double? = pinnedWeatherLon

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
            val result = citySearchRepository.searchCities(trimmed)
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

    fun dismissSuggestions() {
        suggestJob?.cancel()
        _citySuggestions.value = emptyList()
        _citySuggestLoading.value = false
    }

    /**
     * If the user has a previously saved city, copies it into the search field once.
     * Returns the city name for loading weather, or null if nothing to restore.
     */
    suspend fun restoreSavedCityOnce(): String? {
        if (savedCityRestoreDone) return null
        savedCityRestoreDone = true
        val last = preferences.lastCity.first()
        if (!last.isNullOrBlank()) {
            _cityInput.value = last
            return last
        }
        return null
    }
}
