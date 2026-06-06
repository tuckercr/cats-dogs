package com.tuckercr.catsdogs.util

import android.content.Context
import com.tuckercr.catsdogs.domain.WeatherUnits
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

interface WeatherUnitsProvider {
    fun resolve(): WeatherUnits
}

@Singleton
class ContextWeatherUnitsProvider @Inject constructor(
    @ApplicationContext private val context: Context,
) : WeatherUnitsProvider {
    override fun resolve(): WeatherUnits = context.resolveWeatherUnits()
}
