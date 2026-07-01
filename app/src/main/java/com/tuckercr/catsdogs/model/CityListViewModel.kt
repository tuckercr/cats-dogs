package com.tuckercr.catsdogs.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tuckercr.catsdogs.analytics.AnalyticsRepository
import com.tuckercr.catsdogs.data.GeocodingRepository
import com.tuckercr.catsdogs.data.PreferencesRepository
import com.tuckercr.catsdogs.domain.SavedLocation
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CityListViewModel internal constructor(
    private val preferencesRepository: PreferencesRepository,
    private val geocodingRepository: GeocodingRepository,
    private val onCityAdded: () -> Unit,
) : ViewModel() {

    @Inject
    constructor(
        preferencesRepository: PreferencesRepository,
        geocodingRepository: GeocodingRepository,
        analyticsRepository: AnalyticsRepository,
    ) : this(
        preferencesRepository = preferencesRepository,
        geocodingRepository = geocodingRepository,
        onCityAdded = analyticsRepository::logCityAdded,
    )

    private val _locations = MutableStateFlow<List<SavedLocation>>(emptyList())
    val locations: StateFlow<List<SavedLocation>> = _locations.asStateFlow()

    private val _activeIndex = MutableStateFlow(0)
    val activeIndex: StateFlow<Int> = _activeIndex.asStateFlow()

    val activeLocation: StateFlow<SavedLocation?> = combine(_locations, _activeIndex) { locs, idx ->
        locs.getOrNull(idx)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    init {
        viewModelScope.launch {
            var locs = preferencesRepository.savedLocations.first()
            val idx = preferencesRepository.activeLocationIndex.first()

            // Migrate from legacy single-city storage
            if (locs.isEmpty()) {
                val lastCity = preferencesRepository.lastCity.first()
                if (!lastCity.isNullOrBlank()) {
                    locs = listOf(SavedLocation(label = lastCity, latitude = null, longitude = null))
                    preferencesRepository.setSavedLocations(locs)
                }
            }

            _locations.value = locs
            _activeIndex.value = idx.coerceIn(0, (locs.size - 1).coerceAtLeast(0))
        }
    }

    fun addLocation(location: SavedLocation) {
        // Switch to an existing identical entry rather than duplicating
        val existingIdx = _locations.value.indexOfFirst { it.label == location.label }
        if (existingIdx >= 0) {
            setActiveIndex(existingIdx)
            return
        }
        viewModelScope.launch {
            val locationToAdd = if (location.latitude == null || location.longitude == null) {
                // Geocode location by name to get coordinates
                geocodingRepository
                    .searchCities(location.label)
                    .getOrNull()
                    ?.firstOrNull()
                    ?.let { suggestion ->
                        location.copy(
                            latitude = suggestion.weatherLat,
                            longitude = suggestion.weatherLon,
                        )
                    } ?: location
            } else {
                location
            }

            val updated = _locations.value + locationToAdd
            val newIdx = updated.size - 1
            _locations.value = updated
            _activeIndex.value = newIdx
            onCityAdded()
            preferencesRepository.setSavedLocations(updated)
            preferencesRepository.setActiveLocationIndex(newIdx)
        }
    }

    fun removeLocation(index: Int) {
        val updated = _locations.value.toMutableList().apply { removeAt(index) }
        val newIdx = _activeIndex.value.coerceIn(0, (updated.size - 1).coerceAtLeast(0))
        _locations.value = updated
        _activeIndex.value = newIdx
        viewModelScope.launch {
            preferencesRepository.setSavedLocations(updated)
            preferencesRepository.setActiveLocationIndex(newIdx)
        }
    }

    fun setActiveIndex(index: Int) {
        if (index == _activeIndex.value) return
        _activeIndex.value = index
        viewModelScope.launch {
            preferencesRepository.setActiveLocationIndex(index)
        }
    }

    fun reorderLocations(
        fromIndex: Int,
        toIndex: Int,
    ) {
        val updated = _locations.value.toMutableList()
        val item = updated.removeAt(fromIndex)
        updated.add(toIndex, item)

        // Update active index if needed
        val newActiveIndex = when {
            _activeIndex.value == fromIndex -> toIndex
            fromIndex < _activeIndex.value && toIndex >= _activeIndex.value -> _activeIndex.value - 1
            fromIndex > _activeIndex.value && toIndex <= _activeIndex.value -> _activeIndex.value + 1
            else -> _activeIndex.value
        }

        _locations.value = updated
        _activeIndex.value = newActiveIndex
        viewModelScope.launch {
            preferencesRepository.setSavedLocations(updated)
            preferencesRepository.setActiveLocationIndex(newActiveIndex)
        }
    }
}
