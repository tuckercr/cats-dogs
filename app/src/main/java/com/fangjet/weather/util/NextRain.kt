package com.fangjet.weather.util

import com.fangjet.weather.domain.QuarterHour

// "Rain starting around 3:15 PM": the one line under the top card when rain or snow is due in the
// next 2 hours. Same rules as iOS's Utilities/NextRain.swift.

/** What the next 2 hours hold, for the line under the top card. [snow] when it falls as snow. */
sealed interface NextRain {
    val snow: Boolean

    data class Starting(
        val epochSeconds: Long,
        override val snow: Boolean,
    ) : NextRain

    data class Stopping(
        val epochSeconds: Long,
        override val snow: Boolean,
    ) : NextRain

    /** Falling now and for the whole 2 hours. */
    data class Continuing(
        override val snow: Boolean,
    ) : NextRain
}

private const val QUARTER_HOUR_SECONDS = 15 * 60L

/** 2 hours ahead, in 15-minute steps. */
private const val QUARTERS_AHEAD = 8

/** Fewer than this many quarters left (a forecast cached for hours) says too little to go on. */
private const val MIN_QUARTERS = 4

/** 0.1 mm in 15 minutes (0.4 mm an hour): drizzle you'd notice. Less is noise. */
private const val WET_MM = 0.1

private val QuarterHour.wet: Boolean get() = precipitationMm >= WET_MM

/**
 * The next 2 hours from the quarter in progress: when rain starts, or when it stops (dry for at
 * least 30 minutes, so a short gap in a shower doesn't count), or that it keeps on. Null when it's
 * dry throughout, or when too little of the forecast is left.
 */
fun nextRain(
    quarters: List<QuarterHour>,
    nowEpochSeconds: Long,
): NextRain? {
    val ahead = quarters
        .filter { it.epochSeconds + QUARTER_HOUR_SECONDS > nowEpochSeconds }
        .take(QUARTERS_AHEAD)
    if (ahead.size < MIN_QUARTERS) return null
    if (!ahead.first().wet) {
        val start = ahead.firstOrNull { it.wet } ?: return null
        return NextRain.Starting(start.epochSeconds, start.snowfallCm > 0)
    }
    val snow = ahead.first().snowfallCm > 0
    val stop = ahead.indices.drop(1).firstOrNull { i -> !ahead[i].wet && ahead.getOrNull(i + 1)?.wet != true }
    return if (stop == null) NextRain.Continuing(snow) else NextRain.Stopping(ahead[stop].epochSeconds, snow)
}
