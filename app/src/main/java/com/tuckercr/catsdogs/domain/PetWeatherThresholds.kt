package com.tuckercr.catsdogs.domain

/**
 * The tunable numbers behind the pet moods and the walk advice, from Remote Config (see
 * `RemoteConfigRepository.petWeatherThresholds`). Temperatures are Celsius whatever units the
 * user has chosen; the mood and walk functions convert readings to Celsius before comparing.
 *
 * Always built through [validated], so every instance is in range and every pair is ordered.
 */
data class PetWeatherThresholds(
    /** Pavement at or above this burns paws: the walk rating becomes PAWS_HOT. */
    val hotPavementC: Double = 52.0,
    /** An hour is walkable only below this rain chance, in percent. */
    val maxRainChancePercent: Int = 30,
    /** An hour is walkable only between [comfortMinC] and [comfortMaxC], inclusive. */
    val comfortMinC: Double = 3.0,
    val comfortMaxC: Double = 27.0,
    /** Walks are only suggested between these local hours, inclusive (7 = 7 AM, 20 = 8 PM). */
    val walkHoursStart: Int = 7,
    val walkHoursEnd: Int = 20,
    /** At or above this the pets are HOT (and the walk rating PAWS_HOT). */
    val petHotC: Double = 30.0,
    /** At or below this the pets are COLD (and the walk rating TOO_COLD). */
    val petColdC: Double = 0.0,
) {
    companion object {
        /** The compiled-in values; also the fallback for anything invalid. */
        val DEFAULT = PetWeatherThresholds()

        val HOT_PAVEMENT_RANGE = 40.0..70.0
        val MAX_RAIN_CHANCE_RANGE = 0..100
        val COMFORT_MIN_RANGE = -20.0..20.0
        val COMFORT_MAX_RANGE = 15.0..40.0
        val WALK_HOURS_RANGE = 0..23
        val PET_HOT_RANGE = 20.0..45.0
        val PET_COLD_RANGE = -20.0..10.0

        /**
         * Builds thresholds from raw (possibly missing or bad) values. A null or out-of-range
         * value falls back to its default; then any pair that is not strictly ordered
         * (comfort min < max, walk start < end, pet cold < hot) falls back to the default pair.
         */
        fun validated(
            hotPavementC: Double? = null,
            maxRainChancePercent: Int? = null,
            comfortMinC: Double? = null,
            comfortMaxC: Double? = null,
            walkHoursStart: Int? = null,
            walkHoursEnd: Int? = null,
            petHotC: Double? = null,
            petColdC: Double? = null,
        ): PetWeatherThresholds {
            val d = DEFAULT
            val comfortMin = comfortMinC.inRangeOr(COMFORT_MIN_RANGE, d.comfortMinC)
            val comfortMax = comfortMaxC.inRangeOr(COMFORT_MAX_RANGE, d.comfortMaxC)
            val hoursStart = walkHoursStart.inRangeOr(WALK_HOURS_RANGE, d.walkHoursStart)
            val hoursEnd = walkHoursEnd.inRangeOr(WALK_HOURS_RANGE, d.walkHoursEnd)
            val hot = petHotC.inRangeOr(PET_HOT_RANGE, d.petHotC)
            val cold = petColdC.inRangeOr(PET_COLD_RANGE, d.petColdC)
            val comfortOrdered = comfortMin < comfortMax
            val hoursOrdered = hoursStart < hoursEnd
            val petOrdered = cold < hot
            return PetWeatherThresholds(
                hotPavementC = hotPavementC.inRangeOr(HOT_PAVEMENT_RANGE, d.hotPavementC),
                maxRainChancePercent = maxRainChancePercent.inRangeOr(MAX_RAIN_CHANCE_RANGE, d.maxRainChancePercent),
                comfortMinC = if (comfortOrdered) comfortMin else d.comfortMinC,
                comfortMaxC = if (comfortOrdered) comfortMax else d.comfortMaxC,
                walkHoursStart = if (hoursOrdered) hoursStart else d.walkHoursStart,
                walkHoursEnd = if (hoursOrdered) hoursEnd else d.walkHoursEnd,
                petHotC = if (petOrdered) hot else d.petHotC,
                petColdC = if (petOrdered) cold else d.petColdC,
            )
        }

        private fun Double?.inRangeOr(
            range: ClosedFloatingPointRange<Double>,
            default: Double,
        ): Double = this?.takeIf { it.isFinite() && it in range } ?: default

        private fun Int?.inRangeOr(
            range: IntRange,
            default: Int,
        ): Int = this?.takeIf { it in range } ?: default
    }
}
