package com.fangjet.weather.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fangjet.weather.data.AirQualitySource
import com.fangjet.weather.data.ForecastSourcePolicy
import com.fangjet.weather.domain.AirQuality
import com.fangjet.weather.domain.SavedLocation
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * The air quality for the city on screen, by [SavedLocation.cacheKey]. Kept in memory and fetched
 * again once it's an hour old, which is how often Open-Meteo updates it. Nothing shows until a
 * reading arrives, and a failed fetch just leaves the tile out. It's Open-Meteo data, so Remote
 * Config's `open_meteo_enabled` switches it off too.
 */
@HiltViewModel
class AirQualityViewModel @Inject constructor(
    private val source: AirQualitySource,
    private val forecastSourcePolicy: ForecastSourcePolicy,
) : ViewModel() {
    internal var now: () -> Long = System::currentTimeMillis

    private val _readings = MutableStateFlow<Map<String, AirQuality>>(emptyMap())
    val readings: StateFlow<Map<String, AirQuality>> = _readings.asStateFlow()

    private val fetchedAt = mutableMapOf<String, Long>()
    private val inFlight = mutableSetOf<String>()

    /**
     * Fetches the city's reading unless a fresh one is held or a fetch is in flight. Cities without
     * coordinates wait until they're geocoded.
     */
    fun load(location: SavedLocation?) {
        if (!forecastSourcePolicy.openMeteoEnabled()) return
        val latitude = location?.latitude ?: return
        val longitude = location.longitude ?: return
        val key = location.cacheKey
        fetchedAt[key]?.let { if (now() - it < STALE_AFTER_MS) return }
        if (!inFlight.add(key)) return
        viewModelScope.launch {
            try {
                val reading = source.fetch(latitude, longitude).getOrNull() ?: return@launch
                _readings.update { it + (key to reading) }
                fetchedAt[key] = now()
            } finally {
                inFlight.remove(key)
            }
        }
    }

    companion object {
        const val STALE_AFTER_MS = 60 * 60 * 1000L
    }
}
