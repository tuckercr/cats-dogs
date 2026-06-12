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

@HiltViewModel
class WelcomeViewModel internal constructor(
    private val hasSeenWelcomeOnce: suspend () -> Boolean,
    private val setHasSeenWelcome: suspend (Boolean) -> Unit,
) : ViewModel() {

    @Inject
    constructor(
        preferencesRepository: PreferencesRepository,
    ) : this(
        hasSeenWelcomeOnce = preferencesRepository::hasSeenWelcomeOnce,
        setHasSeenWelcome = preferencesRepository::setHasSeenWelcome,
    )

    private val _welcomeDone = MutableStateFlow<Boolean?>(null)
    val welcomeDone: StateFlow<Boolean?> = _welcomeDone.asStateFlow()

    init {
        viewModelScope.launch {
            val seen = hasSeenWelcomeOnce()
            _welcomeDone.value = seen
        }
    }

    fun completeWelcome() {
        viewModelScope.launch {
            _welcomeDone.value = true
            setHasSeenWelcome(true)
        }
    }
}
