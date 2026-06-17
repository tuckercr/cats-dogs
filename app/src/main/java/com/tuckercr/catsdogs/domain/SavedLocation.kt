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
)
