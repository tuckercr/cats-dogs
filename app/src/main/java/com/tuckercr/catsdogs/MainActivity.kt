package com.tuckercr.catsdogs

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.tuckercr.catsdogs.model.GeoLocationViewModel
import com.tuckercr.catsdogs.model.WeatherForecastViewModel
import com.tuckercr.catsdogs.model.WelcomeViewModel
import com.tuckercr.catsdogs.ui.navigation.WeatherNavHost
import com.tuckercr.catsdogs.ui.theme.CatsDogsTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val welcomeViewModel: WelcomeViewModel by viewModels()
    private val geoLocationViewModel: GeoLocationViewModel by viewModels()
    private val weatherForecastViewModel: WeatherForecastViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        // A bonus splash screen, wasn't in requirements
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CatsDogsTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    WeatherNavHost(
                        welcomeViewModel = welcomeViewModel,
                        geoLocationViewModel = geoLocationViewModel,
                        weatherForecastViewModel = weatherForecastViewModel,
                    )
                }
            }
        }
    }
}
