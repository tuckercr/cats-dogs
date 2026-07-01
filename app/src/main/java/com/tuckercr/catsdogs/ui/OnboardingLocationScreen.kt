package com.tuckercr.catsdogs.ui

import android.Manifest
import timber.log.Timber
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tuckercr.catsdogs.R
import com.tuckercr.catsdogs.domain.SavedLocation
import com.tuckercr.catsdogs.model.LocationFetchState
import com.tuckercr.catsdogs.model.LocationPermissionViewModel
import com.tuckercr.catsdogs.ui.theme.CatsDogsTheme

@Composable
fun OnboardingLocationScreen(
    onLocationResolved: (SavedLocation) -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LocationPermissionViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        Timber.d("screen shown")
    }

    LaunchedEffect(state) {
        Timber.d("state changed: $state")
        if (state is LocationFetchState.Located) {
            val location = (state as LocationFetchState.Located).location
            Timber.d("location resolved: lat=${location.latitude} lon=${location.longitude}")
            onLocationResolved(location)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { permissions ->
        val fine = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarse = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        Timber.d("permission result: fine=$fine coarse=$coarse")
        if (fine || coarse) viewModel.fetchLocation() else viewModel.onPermissionDenied()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(112.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (state == LocationFetchState.Locating) {
                CircularProgressIndicator(
                    modifier = Modifier.size(48.dp),
                    color = MaterialTheme.colorScheme.primary,
                )
            } else {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(56.dp),
                )
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = stringResource(R.string.location_onboarding_title),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = when (state) {
                LocationFetchState.PermissionDenied -> stringResource(R.string.location_permission_denied)
                LocationFetchState.Failed -> stringResource(R.string.location_fetch_failed)
                else -> stringResource(R.string.location_onboarding_body)
            },
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(48.dp))

        if (state != LocationFetchState.Locating) {
            Button(
                onClick = {
                    if (viewModel.hasLocationPermission()) {
                        Timber.d("permission already granted, fetching location")
                        viewModel.fetchLocation()
                    } else {
                        Timber.d("launching permission dialog")
                        permissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION,
                            ),
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.location_allow_button))
            }
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = {
                    Timber.d("user tapped Skip")
                    onSkip()
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.location_skip_button))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun OnboardingLocationScreenPreview() {
    CatsDogsTheme {
        OnboardingLocationScreen(onLocationResolved = {}, onSkip = {})
    }
}
