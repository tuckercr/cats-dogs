package com.tuckercr.catsdogs.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tuckercr.catsdogs.data.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WelcomeViewModel @Inject constructor(
    private val preferencesRepository: UserPreferences,
) : ViewModel() {

    private val _welcomeDone = MutableStateFlow<Boolean?>(null)
    val welcomeDone: StateFlow<Boolean?> = _welcomeDone.asStateFlow()

    init {
        viewModelScope.launch {
            val seen = preferencesRepository.hasSeenWelcomeOnce()
            _welcomeDone.value = seen
        }
    }

    fun completeWelcome() {
        viewModelScope.launch {
            _welcomeDone.value = true
            preferencesRepository.setHasSeenWelcome(true)
        }
    }
}
