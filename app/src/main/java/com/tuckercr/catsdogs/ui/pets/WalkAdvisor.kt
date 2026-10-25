package com.tuckercr.catsdogs.ui.pets

import com.tuckercr.catsdogs.domain.HourlySlot
import com.tuckercr.catsdogs.domain.PetWeatherThresholds
import java.time.Instant
import java.time.ZoneOffset

enum class WalkRating { GREAT, OKAY, POOR, PAWS_HOT, TOO_COLD }

/**
 * [bestTimeLabel] is null when now is a good time, or when there is no good window soon.
 * [pavementNow] is the estimated pavement temperature for the current hour, in the forecast's
 * units, when the source provides it. [bestTimeIsTomorrow] is true when [bestTimeLabel] falls on
 * the next calendar day in the city's own time zone.
 */
data class WalkAdvice(
    val rating: WalkRating,
    val bestTimeLabel: String?,
    val pavementNow: Double? = null,
    val bestTimeIsTomorrow: Boolean = false,
)

private const val LOOKAHEAD_SLOTS = 24 // a day of hourly slots
private const val LOOKAHEAD_SECONDS = 24 * 60 * 60L // and never more than a day, for 3-hourly slots

// The walk thresholds (pavement heat, rain chance, comfort range, walking hours) are Remote Config
// values; see PetWeatherThresholds. Defaults: pavement 52 C (about 125 F, burns paws in under a
// minute), under 30% rain, 3..27 C, and 7 AM..8 PM.

/**
 * Nobody wants walk advice for 2 AM: only suggest slots within the walking hours that are also in
 * daylight. The walking hours alone are a fixed clock window, so on a winter morning 7 AM can be
 * before sunrise; a night icon ("n", the same signal that puts the pets to sleep) rules it out.
 */
private fun HourlySlot.isDaytime(t: PetWeatherThresholds): Boolean =
    (localHour?.let { it in t.walkHoursStart..t.walkHoursEnd } ?: true) && !iconCode.endsWith("n")

private fun HourlySlot.pavementC(): Double? = pavementTemperature?.let { units.toCelsius(it) }

private fun HourlySlot.isWalkable(t: PetWeatherThresholds): Boolean {
    val c = units.toCelsius(temperature)
    val pavementOk = (pavementC() ?: 0.0) < t.hotPavementC
    return isDaytime(t) &&
        pavementOk &&
        precipitationChance < t.maxRainChancePercent &&
        c in t.comfortMinC..t.comfortMaxC
}

/** Whether the current conditions are rain, drizzle, snow or a thunderstorm (as [petMoodFor] reads them). */
fun isPrecipitating(conditionMain: String): Boolean = conditionMain.lowercase() in setOf("thunderstorm", "snow", "rain", "drizzle")

/**
 * Works out when to walk the dog from the current temperature and the upcoming hourly slots
 * (the first slot is the next forecast window, so it stands in for "now"). Returns null when
 * there are no slots yet (forecast loading or failed), since there is nothing to advise on.
 *
 * [precipitatingNow] is the current observation. The hourly forecast can say the hour in progress
 * is dry while it is raining, so when it's raining now, now is never the walk time.
 */
fun walkAdviceFor(
    currentTempC: Double,
    upcoming: List<HourlySlot>,
    thresholds: PetWeatherThresholds = PetWeatherThresholds.DEFAULT,
    // The city's UTC offset (CurrentWeather.offsetSeconds): "tomorrow" means the city's tomorrow.
    utcOffsetSeconds: Int = 0,
    nowEpochSeconds: Long = Instant.now().epochSecond,
    precipitatingNow: Boolean = false,
): WalkAdvice? {
    val slots = upcoming
        .take(LOOKAHEAD_SLOTS)
        .filter { slot -> slot.epochSeconds.let { it == null || it < nowEpochSeconds + LOOKAHEAD_SECONDS } }
    if (slots.isEmpty()) return null
    val nowWalkable = !precipitatingNow && slots.first().isWalkable(thresholds)
    val firstGood = (if (precipitatingNow) slots.drop(1) else slots).firstOrNull { it.isWalkable(thresholds) }
    val pavementNow = slots.first().pavementTemperature
    val pavementHotNow = (slots.first().pavementC() ?: 0.0) >= thresholds.hotPavementC
    val rating = when {
        currentTempC >= thresholds.petHotC || pavementHotNow -> WalkRating.PAWS_HOT
        currentTempC <= thresholds.petColdC -> WalkRating.TOO_COLD
        nowWalkable -> WalkRating.GREAT
        firstGood != null -> WalkRating.OKAY
        else -> WalkRating.POOR
    }
    val bestSlot = if (rating == WalkRating.GREAT) null else firstGood
    val bestIsTomorrow = bestSlot?.epochSeconds?.let { isNextDay(it, nowEpochSeconds, utcOffsetSeconds) } == true
    return WalkAdvice(rating, bestSlot?.timeLabel, pavementNow, bestIsTomorrow)
}

/** True when [epochSeconds] falls on the calendar day after [nowEpochSeconds], in the city's zone. */
private fun isNextDay(
    epochSeconds: Long,
    nowEpochSeconds: Long,
    utcOffsetSeconds: Int,
): Boolean {
    val zone = ZoneOffset.ofTotalSeconds(utcOffsetSeconds)
    // atOffset().toLocalDate() rather than LocalDate.ofInstant, which needs API 31.
    val today = Instant.ofEpochSecond(nowEpochSeconds).atOffset(zone).toLocalDate()
    return Instant.ofEpochSecond(epochSeconds).atOffset(zone).toLocalDate() == today.plusDays(1)
}
