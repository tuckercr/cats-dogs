package com.fangjet.weather.ui.pets

import com.fangjet.weather.R
import com.fangjet.weather.util.PavementBand
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WalkCardTest {

    private fun advice(
        rating: WalkRating,
        bestTimeLabel: String? = null,
        bestTimeIsTomorrow: Boolean = false,
        pavementNow: Double? = null,
        pavementBand: PavementBand? = null,
    ) = WalkAdvice(rating, bestTimeLabel, pavementNow, bestTimeIsTomorrow, pavementBand)

    // --- the sentence ---

    @Test
    fun `great now says walk now even when a later time is known`() {
        assertEquals(WalkText(R.string.walk_now), walkDetail(advice(WalkRating.GREAT, bestTimeLabel = "6 PM")))
    }

    @Test
    fun `a later time today names the time`() {
        assertEquals(
            WalkText(R.string.walk_around, listOf("6 PM")),
            walkDetail(advice(WalkRating.POOR, bestTimeLabel = "6 PM")),
        )
    }

    @Test
    fun `a time tomorrow says tomorrow`() {
        assertEquals(
            WalkText(R.string.walk_around_tomorrow, listOf("7 AM")),
            walkDetail(advice(WalkRating.TOO_COLD, bestTimeLabel = "7 AM", bestTimeIsTomorrow = true)),
        )
    }

    @Test
    fun `no good time soon says so`() {
        assertEquals(WalkText(R.string.walk_none), walkDetail(advice(WalkRating.POOR)))
    }

    // --- the caption: pavement when it's the reason ---

    @Test
    fun `dangerous pavement names the rounded temperature`() {
        assertEquals(
            WalkText(R.string.walk_pavement_dangerous, listOf(61)),
            walkCaption(advice(WalkRating.PAWS_HOT, pavementNow = 60.6, pavementBand = PavementBand.DANGEROUS)),
        )
    }

    @Test
    fun `too-hot pavement names the temperature`() {
        assertEquals(
            WalkText(R.string.walk_pavement, listOf(54)),
            walkCaption(advice(WalkRating.PAWS_HOT, pavementNow = 54.2, pavementBand = PavementBand.TOO_HOT)),
        )
    }

    @Test
    fun `warm pavement on an okay walk names the temperature`() {
        assertEquals(
            WalkText(R.string.walk_pavement_warm, listOf(47)),
            walkCaption(advice(WalkRating.OKAY, pavementNow = 47.0, pavementBand = PavementBand.WARM)),
        )
    }

    // --- the caption: the air when the pavement isn't the reason ---

    @Test
    fun `hot air with safe pavement blames the heat, not the pavement`() {
        assertEquals(
            WalkText(R.string.walk_too_hot),
            walkCaption(advice(WalkRating.PAWS_HOT, pavementNow = 40.0, pavementBand = PavementBand.SAFE)),
        )
    }

    @Test
    fun `hot with no pavement estimate blames the heat`() {
        assertEquals(WalkText(R.string.walk_too_hot), walkCaption(advice(WalkRating.PAWS_HOT)))
    }

    @Test
    fun `too cold blames the cold, with or without a pavement reading`() {
        assertEquals(WalkText(R.string.walk_too_cold), walkCaption(advice(WalkRating.TOO_COLD)))
        assertEquals(
            WalkText(R.string.walk_too_cold),
            walkCaption(advice(WalkRating.TOO_COLD, pavementNow = -3.0, pavementBand = PavementBand.ICY)),
        )
    }

    // --- no caption ---

    @Test
    fun `great, poor and okay without warm pavement have no caption`() {
        assertNull(walkCaption(advice(WalkRating.GREAT, pavementNow = 30.0, pavementBand = PavementBand.SAFE)))
        assertNull(walkCaption(advice(WalkRating.POOR, pavementNow = 30.0, pavementBand = PavementBand.SAFE)))
        assertNull(walkCaption(advice(WalkRating.OKAY, pavementNow = 30.0, pavementBand = PavementBand.SAFE)))
        assertNull(walkCaption(advice(WalkRating.OKAY)))
    }

    @Test
    fun `a band without a pavement temperature never shows a pavement caption`() {
        assertNull(walkCaption(advice(WalkRating.OKAY, pavementBand = PavementBand.WARM)))
    }
}
