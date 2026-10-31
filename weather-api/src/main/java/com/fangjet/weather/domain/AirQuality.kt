package com.fangjet.weather.domain

import kotlinx.serialization.Serializable

/**
 * An hour's air quality: an index on the scale the city uses (Europe's, or the US one everywhere
 * else), and the pollutant behind it. Same rules as iOS's `AirQuality`. Serializable, as it's
 * cached with the forecast.
 */
@Serializable
data class AirQuality(
    val index: Int,
    val scale: Scale,
    // The pollutant with the highest sub-index; null when none were reported.
    val mainPollutant: Pollutant?,
) {
    enum class Scale(
        // Where each of the six bands starts, and where the last one is drawn to end.
        val bandEdges: List<Double>,
    ) {
        US(listOf(0.0, 50.0, 100.0, 150.0, 200.0, 300.0, 500.0)),
        EUROPEAN(listOf(0.0, 20.0, 40.0, 60.0, 80.0, 100.0, 150.0)),
        ;

        companion object {
            /** Europe's index for cities on a European clock, the US one everywhere else. */
            fun forTimeZone(identifier: String?): Scale = if (identifier?.startsWith("Europe/") == true) EUROPEAN else US
        }
    }

    enum class Pollutant { FINE_PARTICLES, COARSE_PARTICLES, OZONE, NITROGEN_DIOXIDE, SULPHUR_DIOXIDE, CARBON_MONOXIDE }

    /** 0 (good) to 5 (the worst band). */
    val band: Int
        get() = (1 until scale.bandEdges.size - 1).lastOrNull { index > scale.bandEdges[it] } ?: 0

    /** Where the index sits along the six equal bands of the tile's bar, 0 to 1. */
    val barPosition: Double
        get() {
            val edges = scale.bandEdges
            val within = (index - edges[band]) / (edges[band + 1] - edges[band])
            return ((band + within) / BANDS).coerceIn(0.0, 1.0)
        }

    companion object {
        const val BANDS = 6

        /** Picks the pollutant with the highest sub-index on [scale]. Null without an index. */
        fun from(
            scale: Scale,
            subIndices: (Scale) -> Pair<Int?, List<Pair<Pollutant, Int?>>>,
        ): AirQuality? {
            val (index, pollutants) = subIndices(scale)
            if (index == null) return null
            // The first listed wins a tie.
            var main: Pair<Pollutant, Int>? = null
            for ((pollutant, value) in pollutants) {
                if (value != null && value > (main?.second ?: Int.MIN_VALUE)) main = pollutant to value
            }
            return AirQuality(index, scale, main?.first)
        }
    }
}
