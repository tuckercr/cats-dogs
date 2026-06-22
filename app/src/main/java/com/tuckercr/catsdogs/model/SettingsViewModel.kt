package com.tuckercr.catsdogs.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tuckercr.catsdogs.data.PreferencesRepository
import com.tuckercr.catsdogs.domain.UnitOverride
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

    fun clearCache() {
        viewModelScope.launch { preferencesRepository.clearCache() }
    }
}
