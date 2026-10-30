package com.fangjet.weather.util

/**
 * Whether to ask for an update, from the versions set in Remote Config. Below the minimum supported
 * version the app is blocked until it's updated; below the recommended one a banner asks, and
 * "Not now" hides it for a week. Unset keys never ask. Same rules as iOS's `UpdatePrompt`, with
 * separate keys per platform.
 */
enum class UpdatePrompt {
    NONE,
    RECOMMENDED,
    REQUIRED,
    ;

    companion object {
        /** How long "Not now" hides the banner. */
        const val SNOOZE_MS = 7 * 24 * 60 * 60 * 1000L

        fun forVersion(
            version: String,
            minSupported: String?,
            minRecommended: String?,
            bannerDismissedAtMs: Long?,
            nowMs: Long = System.currentTimeMillis(),
        ): UpdatePrompt {
            if (isOlder(version, minSupported)) return REQUIRED
            if (!isOlder(version, minRecommended)) return NONE
            if (bannerDismissedAtMs != null && nowMs - bannerDismissedAtMs < SNOOZE_MS) return NONE
            return RECOMMENDED
        }

        /** Compares number by number, so 1.10 is newer than 1.9 and 1.2 equals 1.2.0. A blank minimum is never newer. */
        fun isOlder(
            version: String,
            minimum: String?,
        ): Boolean {
            val wanted = minimum?.trim()?.takeIf { it.isNotEmpty() }?.let(::numbers) ?: return false
            val mine = numbers(version)
            for (i in 0 until maxOf(mine.size, wanted.size)) {
                val a = mine.getOrElse(i) { 0 }
                val b = wanted.getOrElse(i) { 0 }
                if (a != b) return a < b
            }
            return false
        }

        /** "1.2.3" as [1, 2, 3]; anything after the digits in a part ("3-debug") is ignored. */
        private fun numbers(version: String): List<Int> =
            version.split('.').map { part ->
                part.takeWhile { it.isDigit() }.toIntOrNull()
                    ?: 0
            }
    }
}
