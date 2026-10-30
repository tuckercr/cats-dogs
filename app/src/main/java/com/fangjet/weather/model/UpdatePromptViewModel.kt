package com.fangjet.weather.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fangjet.weather.BuildConfig
import com.fangjet.weather.data.PreferencesRepository
import com.fangjet.weather.data.RemoteConfigRepository
import com.fangjet.weather.util.UpdatePrompt
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Which update prompt to show, if any. Follows Remote Config as new values are activated. */
@HiltViewModel
class UpdatePromptViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
    private val remoteConfig: RemoteConfigRepository,
) : ViewModel() {
    internal var version: String = BuildConfig.VERSION_NAME
    internal var now: () -> Long = System::currentTimeMillis

    val prompt: StateFlow<UpdatePrompt> =
        combine(remoteConfig.activationCount, preferencesRepository.updateBannerDismissedAt) { _, dismissedAt ->
            UpdatePrompt
                .forVersion(
                    version,
                    remoteConfig.androidMinSupportedVersion(),
                    remoteConfig.androidMinRecommendedVersion(),
                    dismissedAt,
                    now(),
                ).let { debugOverride ?: it }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, UpdatePrompt.NONE)

    /** "Not now" on the banner. */
    fun dismissBanner() {
        viewModelScope.launch { preferencesRepository.setUpdateBannerDismissedAt(now()) }
    }

    companion object {
        /**
         * Debug builds only: set by `adb shell am start ... --es updatePrompt required` (or
         * recommended), to see a prompt without Remote Config.
         */
        internal var debugOverride: UpdatePrompt? = null
    }
}
