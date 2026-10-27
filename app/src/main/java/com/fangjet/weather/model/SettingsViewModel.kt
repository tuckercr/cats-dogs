package com.fangjet.weather.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fangjet.weather.data.PreferencesRepository
import com.fangjet.weather.domain.UnitOverride
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
) : ViewModel() {

    val unitOverride = preferencesRepository.unitOverride
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UnitOverride.SYSTEM)

    fun setUnitOverride(override: UnitOverride) {
        viewModelScope.launch { preferencesRepository.setUnitOverride(override) }
    }

    /** "Share usage statistics". Deliberately not itself tracked by analytics. */
    val shareUsageStatistics = preferencesRepository.shareUsageStatistics
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    fun setShareUsageStatistics(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setShareUsageStatistics(enabled) }
    }

    /** "Best walk time" on the main screen. */
    val showWalkCard = preferencesRepository.showWalkCard
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    fun setShowWalkCard(show: Boolean) {
        viewModelScope.launch { preferencesRepository.setShowWalkCard(show) }
    }

    fun clearCache() {
        viewModelScope.launch { preferencesRepository.clearCache() }
    }
}
