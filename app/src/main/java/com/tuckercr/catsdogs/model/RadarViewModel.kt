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
 * once and shared, then refreshed whenever it has gone stale ([STALE_AFTER_MS]): RainViewer
 * publishes a new frame every 10 minutes and only serves the last couple of hours, so an old
 * timeline would label hours-old frames "Now" and point at tiles that no longer exist.
 *
 * [load] is a no-op when Remote Config `radar_enabled` is off, while a load is in flight, or
 * while the timeline is fresh. A refresh keeps the
 * current timeline visible until the new one arrives, and a failed refresh keeps it (the next
 * [load] retries); only a failed first load shows the error.
 */
@HiltViewModel
class RadarViewModel internal constructor(
    private val getTimeline: suspend () -> Result<RadarTimeline>,
    /** Delay between animation frames (ms), from Remote Config. */
    val frameIntervalMs: Long,
    /**
     * Remote Config `radar_enabled`. When false the radar section is hidden and nothing is
     * fetched from RainViewer or the OpenStreetMap tile servers.
     */
    val radarEnabled: Boolean = true,
    // Monotonic clock in milliseconds; injectable for tests.
    private val nowMillis: () -> Long = { System.nanoTime() / 1_000_000 },
) : ViewModel() {

    @Inject
    constructor(
        radarRepository: RadarRepository,
        remoteConfigRepository: RemoteConfigRepository,
    ) : this(
        getTimeline = radarRepository::getTimeline,
        frameIntervalMs = remoteConfigRepository.radarFrameIntervalMs(),
        radarEnabled = remoteConfigRepository.radarEnabled(),
    )

    private val _timeline = MutableStateFlow<LoadingState<RadarTimeline>>(LoadingState.Idle)
    val timeline: StateFlow<LoadingState<RadarTimeline>> = _timeline.asStateFlow()

    private var loadInFlight = false

    /** When the request behind the current timeline was made; null until one has loaded. */
    private var loadedAtMillis: Long? = null

    private fun isStale(): Boolean = loadedAtMillis?.let { nowMillis() - it >= STALE_AFTER_MS } ?: true

    fun load() {
        if (!radarEnabled) return
        if (loadInFlight) return
        val current = _timeline.value
        if (current is LoadingState.Success && !isStale()) return
        val isRefresh = current is LoadingState.Success
        // A refresh keeps the old timeline on screen; only a first load shows the spinner.
        if (!isRefresh) _timeline.value = LoadingState.Loading
        loadInFlight = true
        // Age is measured from the request, so the 10-minute re-check always finds it stale.
        val requestedAt = nowMillis()
        viewModelScope.launch {
            getTimeline().fold(
                onSuccess = {
                    _timeline.value = LoadingState.Success(it)
                    loadedAtMillis = requestedAt
                },
                onFailure = {
                    // A failed refresh keeps the old timeline; it is retried on the next load().
                    if (!isRefresh) _timeline.value = LoadingState.Error("radar_unavailable")
                },
            )
            loadInFlight = false
        }
    }

    fun retry() {
        if (_timeline.value is LoadingState.Error) _timeline.value = LoadingState.Idle
        load()
    }

    companion object {
        /** RainViewer adds a frame every 10 minutes, so a timeline older than this is out of date. */
        const val STALE_AFTER_MS = 10 * 60 * 1000L
    }
}
