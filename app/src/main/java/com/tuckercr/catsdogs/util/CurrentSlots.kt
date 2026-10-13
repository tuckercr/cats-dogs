package com.tuckercr.catsdogs.util

import com.tuckercr.catsdogs.domain.HourlySlot

/**
 * Drops forecast slots that are already over at [nowEpochSeconds], so a cached forecast, a failed
 * refresh or an app left open for hours never shows (or advises on) hours that have passed.
 *
 * Keeps everything from the slot in progress onwards: the latest slot that started at or before
 * now, and every slot after it. Works for hourly and 3-hourly slots alike, and for half-hour
 * time-zone offsets, because it compares instants rather than clock hours. Slots without a
 * timestamp (forecasts cached before slots carried one) are always kept, and order is preserved.
 */
fun List<HourlySlot>.fromCurrentSlot(nowEpochSeconds: Long): List<HourlySlot> {
    val currentStart = mapNotNull { it.epochSeconds }.filter { it <= nowEpochSeconds }.maxOrNull() ?: return this
    return filter { slot -> slot.epochSeconds.let { it == null || it >= currentStart } }
}
