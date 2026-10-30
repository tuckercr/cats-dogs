package com.fangjet.weather

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fangjet.weather.model.UpdatePromptViewModel
import com.fangjet.weather.ui.UpdateRequiredScreen
import com.fangjet.weather.ui.navigation.WeatherNavHost
import com.fangjet.weather.ui.theme.CatsDogsTheme
import com.fangjet.weather.util.UpdatePrompt
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (BuildConfig.DEBUG) {
            // adb shell am start -n com.fangjet.weather.debug/com.fangjet.weather.MainActivity --es updatePrompt required
            UpdatePromptViewModel.debugOverride = when (intent.getStringExtra("updatePrompt")) {
                "required" -> UpdatePrompt.REQUIRED
                "recommended" -> UpdatePrompt.RECOMMENDED
                else -> null
            }
        }
        setContent {
            CatsDogsTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val prompt by hiltViewModel<UpdatePromptViewModel>().prompt.collectAsStateWithLifecycle()
                    if (prompt == UpdatePrompt.REQUIRED) UpdateRequiredScreen() else WeatherNavHost()
                }
            }
        }
    }
}
