package com.fangjet.weather.domain

import kotlinx.serialization.Serializable

/**
 * A city pinned by the user.  [latitude]/[longitude] are null for cities added by name only
 * (migration path from the old single-city storage), which causes weather fetches to use the
 * city-name query instead of coordinates.
 */
@Serializable
data class SavedLocation(
    val label: String,
    val latitude: Double?,
    val longitude: Double?,
    val isCurrentLocation: Boolean = false,
) {
    val cacheKey: String get() = if (latitude != null && longitude != null) {
        String.format(java.util.Locale.ROOT, "%.4f,%.4f", latitude, longitude)
    } else {
        label.trim().lowercase(java.util.Locale.ROOT)
    }
}

/**
 * The city notifications are about: My Location when it's saved, otherwise the first city. Fixed
 * on purpose, so looking at another city's tab doesn't move tomorrow's briefing there.
 */
fun List<SavedLocation>.notificationLocation(): SavedLocation? = firstOrNull { it.isCurrentLocation } ?: firstOrNull()

/** Great-circle distance in metres, for telling a real move from GPS wobble. */
fun distanceMeters(
    lat1: Double,
    lon1: Double,
    lat2: Double,
    lon2: Double,
): Double {
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = kotlin.math.sin(dLat / 2).let { it * it } +
        kotlin.math.cos(Math.toRadians(lat1)) * kotlin.math.cos(Math.toRadians(lat2)) *
        kotlin.math.sin(dLon / 2).let { it * it }
    return 2 * EARTH_RADIUS_METERS * kotlin.math.asin(kotlin.math.sqrt(a))
}

private const val EARTH_RADIUS_METERS = 6_371_000.0
