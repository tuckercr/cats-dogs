package com.tuckercr.catsdogs.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tuckercr.catsdogs.data.GeocodingRepository
import com.tuckercr.catsdogs.domain.CitySuggestion
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GeoLocationViewModel @Inject constructor(
    private val geocodingRepository: GeocodingRepository,
) : ViewModel() {

    private val _cityInput = MutableStateFlow("")
    val cityInput: StateFlow<String> = _cityInput.asStateFlow()

    private val _citySuggestions = MutableStateFlow<List<CitySuggestion>>(emptyList())
    val citySuggestions: StateFlow<List<CitySuggestion>> = _citySuggestions.asStateFlow()

    private val _citySuggestLoading = MutableStateFlow(false)
    val citySuggestLoading: StateFlow<Boolean> = _citySuggestLoading.asStateFlow()

    /** Set after user picks a geocoder suggestion; cleared when the input field changes. */
    private val _selectedSuggestion = MutableStateFlow<CitySuggestion?>(null)
    val selectedSuggestion: StateFlow<CitySuggestion?> = _selectedSuggestion.asStateFlow()

    private var suggestJob: Job? = null

    fun onCityInputChange(value: String) {
        _cityInput.value = value
        _selectedSuggestion.value = null
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
        _selectedSuggestion.value = suggestion
        _cityInput.value = suggestion.label
    }

    fun dismissSuggestions() {
        suggestJob?.cancel()
        _citySuggestions.value = emptyList()
        _citySuggestLoading.value = false
    }

    fun reset() {
        suggestJob?.cancel()
        _cityInput.value = ""
        _citySuggestions.value = emptyList()
        _citySuggestLoading.value = false
        _selectedSuggestion.value = null
    }
}
