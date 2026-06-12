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
class WelcomeViewModel internal constructor(
    private val hasSeenWelcomeOnce: suspend () -> Boolean,
    private val locationOnboardingDoneOnce: suspend () -> Boolean,
    private val setHasSeenWelcome: suspend (Boolean) -> Unit,
    private val setLocationOnboardingDone: suspend () -> Unit,
) : ViewModel() {

    @Inject
    constructor(
        preferencesRepository: PreferencesRepository,
    ) : this(
        hasSeenWelcomeOnce = preferencesRepository::hasSeenWelcomeOnce,
        locationOnboardingDoneOnce = preferencesRepository::locationOnboardingDoneOnce,
        setHasSeenWelcome = preferencesRepository::setHasSeenWelcome,
        setLocationOnboardingDone = preferencesRepository::setLocationOnboardingDone,
    )

    /** null while the prefs read is in-flight. */
    private val _onboardingState = MutableStateFlow<OnboardingState?>(null)
    val onboardingState: StateFlow<OnboardingState?> = _onboardingState.asStateFlow()

    init {
        viewModelScope.launch {
            _onboardingState.value = OnboardingState(
                hasSeenWelcome = hasSeenWelcomeOnce(),
                locationOnboardingDone = locationOnboardingDoneOnce(),
            )
        }
    }

    fun completeWelcome() {
        viewModelScope.launch {
            setHasSeenWelcome(true)
            _onboardingState.value = _onboardingState.value?.copy(hasSeenWelcome = true)
        }
    }

    fun completeLocationOnboarding() {
        viewModelScope.launch {
            setLocationOnboardingDone()
            _onboardingState.value = _onboardingState.value?.copy(locationOnboardingDone = true)
        }
    }
}
