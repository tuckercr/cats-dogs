package com.tuckercr.catsdogs.ui.navigation

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.tuckercr.catsdogs.model.CityListViewModel
import com.tuckercr.catsdogs.model.WelcomeViewModel
import com.tuckercr.catsdogs.ui.CurrentWeatherRoute
import com.tuckercr.catsdogs.ui.ForecastRoute
import com.tuckercr.catsdogs.ui.OnboardingLocationScreen
import com.tuckercr.catsdogs.ui.WelcomeScreen

object WeatherDestinations {
    const val LOADING = "loading"
    const val WELCOME = "welcome"
    const val LOCATION_PERMISSION = "location_permission"
    const val CURRENT = "current"
    const val FORECAST = "forecast"
}

@Composable
fun WeatherNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    val activity = LocalActivity.current as ComponentActivity
    val welcomeViewModel = hiltViewModel<WelcomeViewModel>(viewModelStoreOwner = activity)
    val cityListViewModel = hiltViewModel<CityListViewModel>(viewModelStoreOwner = activity)
    val onboardingState by welcomeViewModel.onboardingState.collectAsStateWithLifecycle()

    NavHost(
        navController = navController,
        startDestination = WeatherDestinations.LOADING,
        modifier = modifier,
    ) {
        composable(WeatherDestinations.LOADING) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            LaunchedEffect(onboardingState) {
                val state = onboardingState ?: return@LaunchedEffect
                val next = when {
                    !state.hasSeenWelcome -> WeatherDestinations.WELCOME
                    !state.locationOnboardingDone -> WeatherDestinations.LOCATION_PERMISSION
                    else -> WeatherDestinations.CURRENT
                }
                navController.navigate(next) {
                    popUpTo(WeatherDestinations.LOADING) { inclusive = true }
                    launchSingleTop = true
                }
            }
        }

        composable(WeatherDestinations.WELCOME) {
            WelcomeScreen(
                onGetStarted = {
                    welcomeViewModel.completeWelcome()
                    navController.navigate(WeatherDestinations.LOCATION_PERMISSION) {
                        popUpTo(WeatherDestinations.WELCOME) { inclusive = true }
                        launchSingleTop = true
                    }
                },
            )
        }

        composable(WeatherDestinations.LOCATION_PERMISSION) {
            OnboardingLocationScreen(
                onLocationResolved = { location ->
                    cityListViewModel.addLocation(location)
                    welcomeViewModel.completeLocationOnboarding()
                    navController.navigate(WeatherDestinations.CURRENT) {
                        popUpTo(WeatherDestinations.LOCATION_PERMISSION) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onSkip = {
                    welcomeViewModel.completeLocationOnboarding()
                    navController.navigate(WeatherDestinations.CURRENT) {
                        popUpTo(WeatherDestinations.LOCATION_PERMISSION) { inclusive = true }
                        launchSingleTop = true
                    }
                },
            )
        }

        composable(WeatherDestinations.CURRENT) {
            CurrentWeatherRoute(
                onOpenForecast = {
                    navController.navigate(WeatherDestinations.FORECAST)
                },
            )
        }

        composable(WeatherDestinations.FORECAST) {
            ForecastRoute(onNavigateBack = { navController.popBackStack() })
        }
    }
}
