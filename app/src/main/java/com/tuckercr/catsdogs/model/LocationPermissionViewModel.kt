package com.tuckercr.catsdogs.model

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.tuckercr.catsdogs.domain.SavedLocation
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

sealed class LocationFetchState {
    object Idle : LocationFetchState()

    object Locating : LocationFetchState()

    data class Located(
        val location: SavedLocation,
    ) : LocationFetchState()

    object PermissionDenied : LocationFetchState()

    object Failed : LocationFetchState()
}

@HiltViewModel
class LocationPermissionViewModel internal constructor(
    application: Application,
    private val hasLocationPermissionProvider: () -> Boolean,
    private val locationFetcher: LocationFetcher,
) : AndroidViewModel(application) {

    @Inject
    constructor(application: Application) : this(
        application = application,
        hasLocationPermissionProvider = { application.hasAnyLocationPermission() },
        locationFetcher = PlayServicesLocationFetcher(application),
    )

    private val _state = MutableStateFlow<LocationFetchState>(LocationFetchState.Idle)
    val state: StateFlow<LocationFetchState> = _state.asStateFlow()

    fun hasLocationPermission(): Boolean = hasLocationPermissionProvider()

    fun onPermissionDenied() {
        _state.value = LocationFetchState.PermissionDenied
    }

    fun fetchLocation() {
        if (!hasLocationPermission()) {
            _state.value = LocationFetchState.PermissionDenied
            return
        }
        _state.value = LocationFetchState.Locating
        viewModelScope.launch {
            try {
                val location = locationFetcher.tryLastLocation()
                    ?: locationFetcher.tryCurrentLocation()
                if (location != null) {
                    _state.value = LocationFetchState.Located(
                        SavedLocation(
                            label = "My Location",
                            latitude = location.latitude,
                            longitude = location.longitude,
                            isCurrentLocation = true,
                        ),
                    )
                } else {
                    _state.value = LocationFetchState.Failed
                }
            } catch (e: Exception) {
                Log.e("LocationPermissionVM", "Error fetching location", e)
                _state.value = LocationFetchState.Failed
            }
        }
    }
}

internal data class DeviceLocation(
    val latitude: Double,
    val longitude: Double,
)

internal interface LocationFetcher {
    suspend fun tryLastLocation(): DeviceLocation?

    suspend fun tryCurrentLocation(): DeviceLocation?
}

private class PlayServicesLocationFetcher(
    application: Application,
) : LocationFetcher {

    private val fusedClient = LocationServices.getFusedLocationProviderClient(application)

    @Suppress("MissingPermission")
    override suspend fun tryLastLocation(): DeviceLocation? =
        suspendCancellableCoroutine { cont ->
            fusedClient.lastLocation
                .addOnSuccessListener { loc -> if (cont.isActive) cont.resume(loc) }
                .addOnFailureListener { e -> if (cont.isActive) cont.resumeWithException(e) }
                .addOnCanceledListener { cont.cancel() }
        }?.toDeviceLocation()

    @Suppress("MissingPermission")
    override suspend fun tryCurrentLocation(): DeviceLocation? =
        suspendCancellableCoroutine { cont ->
            val request = CurrentLocationRequest
                .Builder()
                .setPriority(Priority.PRIORITY_BALANCED_POWER_ACCURACY)
                .setMaxUpdateAgeMillis(30_000)
                .build()
            fusedClient
                .getCurrentLocation(request, null)
                .addOnSuccessListener { loc -> if (cont.isActive) cont.resume(loc) }
                .addOnFailureListener { e -> if (cont.isActive) cont.resumeWithException(e) }
                .addOnCanceledListener { cont.cancel() }
        }?.toDeviceLocation()
}

private fun Application.hasAnyLocationPermission(): Boolean =
    ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        ) ==
        PackageManager.PERMISSION_GRANTED

private fun android.location.Location.toDeviceLocation() =
    DeviceLocation(
        latitude = latitude,
        longitude = longitude,
    )
