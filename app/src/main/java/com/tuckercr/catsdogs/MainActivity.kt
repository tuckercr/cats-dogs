package com.tuckercr.catsdogs

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.tuckercr.catsdogs.model.CityListViewModel
import com.tuckercr.catsdogs.ui.navigation.WeatherNavHost
import com.tuckercr.catsdogs.ui.theme.CatsDogsTheme
import com.tuckercr.catsdogs.worker.WeatherNotificationWorker.Companion.EXTRA_LOCATION_INDEX
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val cityListViewModel: CityListViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
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
    }
}
