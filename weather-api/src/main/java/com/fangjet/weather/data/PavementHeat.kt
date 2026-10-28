package com.fangjet.weather.data

/**
 * Rough estimate of sun-baked asphalt temperature, in degrees Celsius.
 *
 * Dark pavement in direct sun runs far hotter than the air: at 25 C air and strong sun it reaches
 * about 50 C, hot enough to burn paws. We add a heating term proportional to incoming solar
 * radiation, capped so cloudless noon sun adds at most [MAX_SOLAR_GAIN_C]. At night, or with no sun,
 * pavement settles to the air temperature.
 *
 * Pavement doesn't jump to that temperature: it warms through the morning after a cold night and
 * keeps some heat into the evening. [series] follows that, hour by hour. Same model as iOS's
 * `PavementHeat.swift`.
 */
internal object PavementHeat {
    private const val GAIN_C_PER_WATT = 0.035
    private const val MAX_SOLAR_GAIN_C = 35.0

    /**
     * How far pavement moves toward the temperature the sun and air are pushing it to in one hour:
     * dark asphalt heats quickly in the sun and gives the heat back more slowly.
     */
    private const val HEATING_PER_HOUR = 0.6
    private const val COOLING_PER_HOUR = 0.4

    /** The temperature the pavement would settle at in this air and sun. */
    fun estimateC(
        airC: Double,
        shortwaveRadiationWm2: Double?,
        isDay: Boolean,
    ): Double {
        val radiation = shortwaveRadiationWm2 ?: 0.0
        if (!isDay || radiation <= 0.0) return airC
        return airC + (radiation * GAIN_C_PER_WATT).coerceAtMost(MAX_SOLAR_GAIN_C)
    }

    /**
     * Pavement temperature for each hour of an hourly series (consecutive hours, as Open-Meteo
     * sends from midnight), null where the air temperature is missing.
     *
     * Open-Meteo's radiation at time t is the average over the hour *before* t, so the hour that
     * starts at t uses the value at t + 1 h (the last hour, with nothing after it, keeps its own).
     * The first hour starts at its settled temperature; each later hour moves part of the way from
     * the previous hour's pavement towards its own settled temperature.
     */
    fun series(
        airC: List<Double?>,
        shortwaveRadiationWm2: List<Double?>,
        isDay: List<Boolean>,
    ): List<Double?> {
        var pavement: Double? = null
        return airC.indices.map { i ->
            val air = airC[i] ?: return@map null
            val radiation = if (i + 1 < shortwaveRadiationWm2.size) shortwaveRadiationWm2[i + 1] else shortwaveRadiationWm2.getOrNull(i)
            // The sun is up for some of this hour if it is at either end.
            val day = isDay.getOrElse(i) { false } || isDay.getOrElse(i + 1) { false }
            val settled = estimateC(air, radiation, day)
            val previous = pavement
            val next = if (previous == null) {
                settled
            } else {
                previous + (settled - previous) * (if (settled > previous) HEATING_PER_HOUR else COOLING_PER_HOUR)
            }
            pavement = next
            next
        }
    }
}
