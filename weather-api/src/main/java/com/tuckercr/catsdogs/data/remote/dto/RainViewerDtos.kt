package com.tuckercr.catsdogs.data.remote.dto

import kotlinx.serialization.Serializable

/** Response of RainViewer's public `weather-maps.json` endpoint. */
@Serializable
data class RainViewerMapsDto(
    val host: String = "",
    val radar: RainViewerRadarDto = RainViewerRadarDto(),
)

@Serializable
data class RainViewerRadarDto(
    val past: List<RainViewerFrameDto> = emptyList(),
    val nowcast: List<RainViewerFrameDto> = emptyList(),
)

@Serializable
data class RainViewerFrameDto(
    val time: Long = 0,
    val path: String = "",
)
