package com.tuckercr.catsdogs.ui.pets

import com.tuckercr.catsdogs.domain.HourlySlot

enum class WalkRating { GREAT, OKAY, POOR, PAWS_HOT, TOO_COLD }

/**
 * [bestTimeLabel] is null when now is a good time, or when there is no good window soon.
 * [pavementNow] is the estimated pavement temperature for the current hour, in the forecast's
 * units, when the source provides it.
 */
data class WalkAdvice(
    val rating: WalkRating,
    val bestTimeLabel: String?,
    val pavementNow: Double? = null,
)

private const val MAX_RAIN_CHANCE = 30
private const val MIN_COMFORT_C = 3.0
private const val MAX_COMFORT_C = 27.0
private const val LOOKAHEAD_SLOTS = 24 // a day of hourly slots

/** Asphalt this hot burns paws in under a minute (about 125 F). */
internal const val PAWS_HOT_PAVEMENT_C = 52.0
private const val FIRST_WALK_HOUR = 7
private const val LAST_WALK_HOUR = 20

/** Nobody wants walk advice for 2 AM: only suggest slots between 7 AM and 8 PM. */
private fun HourlySlot.isDaytime(): Boolean = localHour?.let { it in FIRST_WALK_HOUR..LAST_WALK_HOUR } ?: true

private fun HourlySlot.pavementC(): Double? = pavementTemperature?.let { units.toCelsius(it) }

private fun HourlySlot.isWalkable(): Boolean {
    val c = units.toCelsius(temperature)
    val pavementOk = (pavementC() ?: 0.0) < PAWS_HOT_PAVEMENT_C
    return isDaytime() && pavementOk && precipitationChance < MAX_RAIN_CHANCE && c in MIN_COMFORT_C..MAX_COMFORT_C
}

/**
 * Works out when to walk the dog from the current temperature and the upcoming hourly slots
 * (the first slot is the next forecast window, so it stands in for "now"). Returns null when
 * there are no slots yet (forecast loading or failed), since there is nothing to advise on.
 */
fun walkAdviceFor(
    currentTempC: Double,
    upcoming: List<HourlySlot>,
): WalkAdvice? {
    val slots = upcoming.take(LOOKAHEAD_SLOTS)
    if (slots.isEmpty()) return null
    val firstGood = slots.firstOrNull { it.isWalkable() }
    val pavementNow = slots.first().pavementTemperature
    val pavementHotNow = (slots.first().pavementC() ?: 0.0) >= PAWS_HOT_PAVEMENT_C
    val rating = when {
        currentTempC >= HOT_THRESHOLD_C || pavementHotNow -> WalkRating.PAWS_HOT
        currentTempC <= COLD_THRESHOLD_C -> WalkRating.TOO_COLD
        slots.first().isWalkable() -> WalkRating.GREAT
        firstGood != null -> WalkRating.OKAY
        else -> WalkRating.POOR
    }
    val best = if (rating == WalkRating.GREAT) null else firstGood?.timeLabel
    return WalkAdvice(rating, best, pavementNow)
}
