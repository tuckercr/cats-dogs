package com.tuckercr.catsdogs.ui.pets

import com.tuckercr.catsdogs.domain.HourlySlot
import com.tuckercr.catsdogs.domain.PetWeatherThresholds

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

private const val LOOKAHEAD_SLOTS = 24 // a day of hourly slots

// The walk thresholds (pavement heat, rain chance, comfort range, walking hours) are Remote Config
// values; see PetWeatherThresholds. Defaults: pavement 52 C (about 125 F, burns paws in under a
// minute), under 30% rain, 3..27 C, and 7 AM..8 PM.

/** Nobody wants walk advice for 2 AM: only suggest slots within the walking hours. */
private fun HourlySlot.isDaytime(t: PetWeatherThresholds): Boolean = localHour?.let { it in t.walkHoursStart..t.walkHoursEnd } ?: true

private fun HourlySlot.pavementC(): Double? = pavementTemperature?.let { units.toCelsius(it) }

private fun HourlySlot.isWalkable(t: PetWeatherThresholds): Boolean {
    val c = units.toCelsius(temperature)
    val pavementOk = (pavementC() ?: 0.0) < t.hotPavementC
    return isDaytime(t) &&
        pavementOk &&
        precipitationChance < t.maxRainChancePercent &&
        c in t.comfortMinC..t.comfortMaxC
}

/**
 * Works out when to walk the dog from the current temperature and the upcoming hourly slots
 * (the first slot is the next forecast window, so it stands in for "now"). Returns null when
 * there are no slots yet (forecast loading or failed), since there is nothing to advise on.
 */
fun walkAdviceFor(
    currentTempC: Double,
    upcoming: List<HourlySlot>,
    thresholds: PetWeatherThresholds = PetWeatherThresholds.DEFAULT,
): WalkAdvice? {
    val slots = upcoming.take(LOOKAHEAD_SLOTS)
    if (slots.isEmpty()) return null
    val firstGood = slots.firstOrNull { it.isWalkable(thresholds) }
    val pavementNow = slots.first().pavementTemperature
    val pavementHotNow = (slots.first().pavementC() ?: 0.0) >= thresholds.hotPavementC
    val rating = when {
        currentTempC >= thresholds.petHotC || pavementHotNow -> WalkRating.PAWS_HOT
        currentTempC <= thresholds.petColdC -> WalkRating.TOO_COLD
        slots.first().isWalkable(thresholds) -> WalkRating.GREAT
        firstGood != null -> WalkRating.OKAY
        else -> WalkRating.POOR
    }
    val best = if (rating == WalkRating.GREAT) null else firstGood?.timeLabel
    return WalkAdvice(rating, best, pavementNow)
}
