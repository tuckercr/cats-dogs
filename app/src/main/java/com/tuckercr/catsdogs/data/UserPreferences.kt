package com.tuckercr.catsdogs.data

import kotlinx.coroutines.flow.Flow

interface UserPreferences {
    val hasSeenWelcome: Flow<Boolean>
    val lastCity: Flow<String?>

    suspend fun setHasSeenWelcome(value: Boolean)

    suspend fun setLastCity(cityName: String)

    suspend fun hasSeenWelcomeOnce(): Boolean
}
