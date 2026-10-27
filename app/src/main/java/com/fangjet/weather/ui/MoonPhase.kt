package com.fangjet.weather.ui

import androidx.compose.runtime.staticCompositionLocalOf

/** The moon's phase in eight steps, worked out from the date alone, so it needs no API. */
enum class MoonPhase {
    NEW,
    WAXING_CRESCENT,
    FIRST_QUARTER,
    WAXING_GIBBOUS,
    FULL,
    WANING_GIBBOUS,
    LAST_QUARTER,
    WANING_CRESCENT,
    ;

    /**
     * The terminator angle to draw, 0 for new to 180 for full. Crescent and gibbous are drawn a
     * quarter and three quarters lit rather than at their mid-phase 15% / 85%, which at icon size
     * look like new and full.
     */
    val drawAngleDegrees: Int get() = DRAW_ANGLES[ordinal]

    val isWaxing: Boolean get() = this in WAXING_CRESCENT..WAXING_GIBBOUS

    val isGibbous: Boolean get() = this == WAXING_GIBBOUS || this == WANING_GIBBOUS

    /** New and full look the same everywhere; the others are lit from the other side down south. */
    val differsInSouth: Boolean get() = this != NEW && this != FULL
}

private val DRAW_ANGLES = intArrayOf(0, 60, 90, 120, 180, 240, 270, 300)

private const val SYNODIC_MONTH_DAYS = 29.530588853

/** A known new moon: 6 January 2000, 18:14 UTC. */
private const val REFERENCE_NEW_MOON_EPOCH = 947_182_440L

/** The phase at [epochSeconds]: the moon's age in its 29.5-day cycle, rounded to the nearest eighth. */
fun moonPhaseAt(epochSeconds: Long): MoonPhase {
    val days = (epochSeconds - REFERENCE_NEW_MOON_EPOCH) / 86_400.0
    val age = ((days % SYNODIC_MONTH_DAYS) + SYNODIC_MONTH_DAYS) % SYNODIC_MONTH_DAYS
    return MoonPhase.entries[(age / SYNODIC_MONTH_DAYS * 8 + 0.5).toInt() % 8]
}

/** Whether the city on screen is south of the equator, where the moon is lit from the other side. */
val LocalSouthernHemisphere = staticCompositionLocalOf { false }
