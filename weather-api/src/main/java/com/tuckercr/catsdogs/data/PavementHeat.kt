package com.tuckercr.catsdogs.data

/**
 * Rough estimate of sun-baked asphalt temperature, in degrees Celsius.
 *
 * Dark pavement in direct sun runs far hotter than the air: at 25 C air and strong sun it reaches
 * about 50 C, hot enough to burn paws. We add a heating term proportional to incoming solar
 * radiation, capped so cloudless noon sun adds at most [MAX_SOLAR_GAIN_C]. At night, or with no sun,
 * pavement sits near the air temperature.
 */
internal object PavementHeat {
    private const val GAIN_C_PER_WATT = 0.035
    private const val MAX_SOLAR_GAIN_C = 35.0

    fun estimateC(
        airC: Double,
        shortwaveRadiationWm2: Double?,
        isDay: Boolean,
    ): Double {
        val radiation = shortwaveRadiationWm2 ?: 0.0
        if (!isDay || radiation <= 0.0) return airC
        return airC + (radiation * GAIN_C_PER_WATT).coerceAtMost(MAX_SOLAR_GAIN_C)
    }
}
