package com.tuckercr.catsdogs

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.tuckercr.catsdogs.model.CityListViewModel
import com.tuckercr.catsdogs.model.WeatherForecastViewModel
import com.tuckercr.catsdogs.ui.navigation.WeatherNavHost
import com.tuckercr.catsdogs.ui.theme.CatsDogsTheme
import com.tuckercr.catsdogs.worker.WeatherNotificationWorker.Companion.EXTRA_LOCATION_INDEX
import com.tuckercr.catsdogs.worker.WeatherNotificationWorker.Companion.EXTRA_OPEN_FORECAST
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val cityListViewModel: CityListViewModel by viewModels()
    private val weatherForecastViewModel: WeatherForecastViewModel by viewModels()

    private val notifPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* nothing to do; notifications will simply work or stay silent */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestNotificationPermissionIfNeeded()
        applyNotificationLocationExtra(intent)
        setContent {
            CatsDogsTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    WeatherNavHost()
                }
            }
        }
    }

    // Called when the app is already running and a new notification is tapped.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        applyNotificationLocationExtra(intent)
    }

    private fun applyNotificationLocationExtra(intent: Intent) {
        val idx = intent.getIntExtra(EXTRA_LOCATION_INDEX, -1)
        if (idx >= 0) cityListViewModel.setActiveIndex(idx)
        if (intent.getBooleanExtra(EXTRA_OPEN_FORECAST, false)) {
            weatherForecastViewModel.requestForecastNavigation()
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
