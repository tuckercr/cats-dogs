package com.tuckercr.catsdogs.domain

/**
 * A row from the geocoder shown in autocomplete; [weatherLat]/[weatherLon] are used for
 * OpenWeather current + forecast when the user picks this suggestion.
 */
data class CitySuggestion(
    val label: String,
    val weatherLat: Double,
    val weatherLon: Double,
)
