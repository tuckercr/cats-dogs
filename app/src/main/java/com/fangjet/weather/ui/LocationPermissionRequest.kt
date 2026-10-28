package com.fangjet.weather.ui

import android.Manifest
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.app.ActivityCompat
import com.fangjet.weather.model.LocationPermissionViewModel
import timber.log.Timber

private val LOCATION_PERMISSIONS = arrayOf(
    Manifest.permission.ACCESS_FINE_LOCATION,
    Manifest.permission.ACCESS_COARSE_LOCATION,
)

/**
 * Shows the system location dialog and hands its answer to [viewModel], which fetches the location
 * on a grant and remembers a refusal Android won't ask about again.
 */
@Composable
internal fun rememberLocationPermissionRequest(viewModel: LocationPermissionViewModel): () -> Unit {
    val activity = LocalActivity.current
    var rationaleBefore by remember { mutableStateOf(false) }

    fun rationale() =
        activity != null &&
            LOCATION_PERMISSIONS.any { ActivityCompat.shouldShowRequestPermissionRationale(activity, it) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { permissions ->
        val granted = permissions.values.any { it }
        Timber.d("permission result: granted=$granted")
        viewModel.onPermissionResult(granted, rationaleBefore, rationaleAfter = rationale())
    }
    return {
        rationaleBefore = rationale()
        launcher.launch(LOCATION_PERMISSIONS)
    }
}
