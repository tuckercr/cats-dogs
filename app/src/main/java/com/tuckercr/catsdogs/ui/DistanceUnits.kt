package com.tuckercr.catsdogs.ui

import androidx.compose.runtime.staticCompositionLocalOf

/** Whether distances (visibility) read in miles rather than kilometres; see usesMilesForDistance. */
val LocalDistanceInMiles = staticCompositionLocalOf { false }
