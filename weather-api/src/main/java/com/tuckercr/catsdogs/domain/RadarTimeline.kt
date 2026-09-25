package com.tuckercr.catsdogs.domain

/** A single radar frame: a snapshot timestamp and the tile path that serves it. */
data class RadarFrame(
    val timeEpochSeconds: Long,
    val path: String,
    /** True for nowcast (future) frames, false for observed past frames. */
    val isForecast: Boolean,
)

/**
 * An ordered radar animation timeline from RainViewer: observed past frames followed by
 * short-range nowcast frames, all sharing a single tile [host].
 */
data class RadarTimeline(
    val host: String,
    val frames: List<RadarFrame>,
) {
    /** Builds the tile URL for [frame] at the given slippy-map tile coordinates. */
    fun tileUrl(
        frame: RadarFrame,
        z: Int,
        x: Int,
        y: Int,
    ): String = "$host${frame.path}/$TILE_SIZE/$z/$x/$y/$COLOR_SCHEME/${SMOOTH}_$SNOW.png"

    companion object {
        private const val TILE_SIZE = 256

        // RainViewer color scheme 4 (Universal Blue -> heavy). smooth=1 (interpolated),
        // snow=1 (render snow distinctly).
        private const val COLOR_SCHEME = 4
        private const val SMOOTH = 1
        private const val SNOW = 1
    }
}
