package com.tuckercr.catsdogs.data

import com.tuckercr.catsdogs.data.remote.dto.RainViewerMapsDto
import com.tuckercr.catsdogs.domain.RadarFrame
import com.tuckercr.catsdogs.domain.RadarTimeline
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fetches the RainViewer radar animation timeline (observed past frames + short-range nowcast).
 * The frame list is global — only the tiles are location-specific — so callers fetch it once and
 * reuse it across cities.
 */
@Singleton
class RadarRepository @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val json: Json,
) {
    suspend fun getTimeline(): Result<RadarTimeline> =
        withContext(Dispatchers.IO) {
            runCatching {
                val request = Request.Builder().url(WEATHER_MAPS_URL).build()
                okHttpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) error("radar_http_${response.code}")
                    val body = response.body?.string() ?: error("radar_empty")
                    parseTimeline(json, body)
                }
            }
        }

    companion object {
        private const val WEATHER_MAPS_URL = "https://api.rainviewer.com/public/weather-maps.json"

        /**
         * Parses the weather-maps payload into an ordered [RadarTimeline]: past frames (chronological)
         * followed by nowcast frames. Extracted so it can be unit-tested without a network call.
         */
        internal fun parseTimeline(
            json: Json,
            body: String,
        ): RadarTimeline {
            val dto = json.decodeFromString(RainViewerMapsDto.serializer(), body)
            val past = dto.radar.past.map { RadarFrame(it.time, it.path, isForecast = false) }
            val nowcast = dto.radar.nowcast.map { RadarFrame(it.time, it.path, isForecast = true) }
            val frames = (past + nowcast).sortedBy { it.timeEpochSeconds }
            if (dto.host.isBlank() || frames.isEmpty()) error("radar_empty")
            return RadarTimeline(host = dto.host, frames = frames)
        }
    }
}
