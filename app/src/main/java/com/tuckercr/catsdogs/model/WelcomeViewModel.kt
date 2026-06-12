package com.tuckercr.catsdogs.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tuckercr.catsdogs.data.PreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OnboardingState(
    val hasSeenWelcome: Boolean,
    val locationOnboardingDone: Boolean,
)

@HiltViewModel
class WelcomeViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
) : ViewModel() {

    /** null while the prefs read is in-flight. */
    private val _onboardingState = MutableStateFlow<OnboardingState?>(null)
    val onboardingState: StateFlow<OnboardingState?> = _onboardingState.asStateFlow()

    init {
        viewModelScope.launch {
            _onboardingState.value = OnboardingState(
                hasSeenWelcome = preferencesRepository.hasSeenWelcomeOnce(),
                locationOnboardingDone = preferencesRepository.locationOnboardingDoneOnce(),
            )
        }
    }

    fun completeWelcome() {
        viewModelScope.launch {
            preferencesRepository.setHasSeenWelcome(true)
            _onboardingState.value = _onboardingState.value?.copy(hasSeenWelcome = true)
        }
    }

    fun completeLocationOnboarding() {
        viewModelScope.launch {
            preferencesRepository.setLocationOnboardingDone()
            _onboardingState.value = _onboardingState.value?.copy(locationOnboardingDone = true)
        }
    }
}
