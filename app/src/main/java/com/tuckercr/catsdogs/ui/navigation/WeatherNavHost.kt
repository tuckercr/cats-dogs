package com.tuckercr.catsdogs.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.tuckercr.catsdogs.ui.CurrentWeatherRoute
import com.tuckercr.catsdogs.ui.ForecastRoute
import com.tuckercr.catsdogs.model.WeatherViewModel
import com.tuckercr.catsdogs.ui.WelcomeScreen

object WeatherDestinations {
    const val LOADING = "loading"
    const val WELCOME = "welcome"
    const val CURRENT = "current"
    const val FORECAST = "forecast"
}
@Composable
fun WeatherNavHost(
    viewModel: WeatherViewModel,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    val welcomeDone by viewModel.welcomeDone.collectAsStateWithLifecycle()

    NavHost(
        navController = navController,
        startDestination = WeatherDestinations.LOADING,
        modifier = modifier,
    ) {
        composable(WeatherDestinations.LOADING) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
            LaunchedEffect(welcomeDone) {
                val isWelcomeDone = welcomeDone ?: return@LaunchedEffect
                val next = if (isWelcomeDone) {
                    WeatherDestinations.CURRENT
                } else {
                    WeatherDestinations.WELCOME
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
                    viewModel.completeWelcome()
                    navController.navigate(WeatherDestinations.CURRENT) {
                        popUpTo(WeatherDestinations.WELCOME) { inclusive = true }
                        launchSingleTop = true
                    }
                },
            )
        }
        composable(WeatherDestinations.CURRENT) {
            CurrentWeatherRoute(
                viewModel = viewModel,
                onOpenForecast = {
                    navController.navigate(WeatherDestinations.FORECAST)
                },
            )
        }
        composable(WeatherDestinations.FORECAST) {
            ForecastRoute(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
            )
        }
    }
}
