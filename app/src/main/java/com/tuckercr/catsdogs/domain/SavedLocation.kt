package com.tuckercr.catsdogs.domain

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
