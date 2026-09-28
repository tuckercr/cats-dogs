package com.tuckercr.catsdogs.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tuckercr.catsdogs.data.RadarRepository
import com.tuckercr.catsdogs.data.RemoteConfigRepository
import com.tuckercr.catsdogs.domain.RadarTimeline
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Holds the RainViewer radar timeline. The frame list is global (not per-city), so it is fetched
 * once and shared; [load] is a no-op once a timeline is loaded or a load is in flight.
 */
@HiltViewModel
class RadarViewModel @Inject constructor(
    private val radarRepository: RadarRepository,
    remoteConfigRepository: RemoteConfigRepository,
) : ViewModel() {

    /** Delay between animation frames (ms), from Remote Config. */
    val frameIntervalMs: Long = remoteConfigRepository.radarFrameIntervalMs()

    private val _timeline = MutableStateFlow<LoadingState<RadarTimeline>>(LoadingState.Idle)
    val timeline: StateFlow<LoadingState<RadarTimeline>> = _timeline.asStateFlow()

    fun load() {
        if (_timeline.value is LoadingState.Loading || _timeline.value is LoadingState.Success) return
        _timeline.value = LoadingState.Loading
        viewModelScope.launch {
            _timeline.value = radarRepository.getTimeline().fold(
                onSuccess = { LoadingState.Success(it) },
                onFailure = { LoadingState.Error("radar_unavailable") },
            )
        }
    }

    fun retry() {
        _timeline.value = LoadingState.Idle
        load()
    }
}
