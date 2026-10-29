package com.fangjet.weather.ui.navigation

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.fangjet.weather.model.CityListViewModel
import com.fangjet.weather.model.SettingsViewModel
import com.fangjet.weather.model.WelcomeViewModel
import com.fangjet.weather.ui.CurrentWeatherRoute
import com.fangjet.weather.ui.LocationsRoute
import com.fangjet.weather.ui.NotificationSettingsRoute
import com.fangjet.weather.ui.OnboardingDogScreen
import com.fangjet.weather.ui.OnboardingLocationScreen
import com.fangjet.weather.ui.OnboardingNotificationScreen
import com.fangjet.weather.ui.SettingsRoute
import com.fangjet.weather.ui.WelcomeScreen
import com.fangjet.weather.worker.NotificationKind

object WeatherDestinations {
    const val LOADING = "loading"
    const val WELCOME = "welcome"
    const val DOG_QUESTION = "dog_question"
    const val NOTIFICATION_PERMISSION = "notification_permission"
    const val LOCATION_PERMISSION = "location_permission"
    const val CURRENT = "current"
    const val SETTINGS = "settings"
    const val NOTIFICATION_SETTINGS = "notification_settings"
    const val LOCATIONS = "locations"
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

    // The dog answer, for the notification screen right after it (a restored app shows the dog wording).
    var answeredHasDog by rememberSaveable { mutableStateOf(true) }
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
                    !state.notificationOnboardingDone -> WeatherDestinations.NOTIFICATION_PERMISSION
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
                    navController.navigate(WeatherDestinations.DOG_QUESTION) {
                        popUpTo(WeatherDestinations.WELCOME) { inclusive = true }
                        launchSingleTop = true
                    }
                },
            )
        }

        // Part of a new user's first run only: not resumed on a later launch, and existing users
        // aren't asked after an update (they have the switch in Settings).
        composable(WeatherDestinations.DOG_QUESTION) {
            OnboardingDogScreen(
                onAnswered = { hasDog ->
                    answeredHasDog = hasDog
                    welcomeViewModel.answerHasDog(hasDog)
                    navController.navigate(WeatherDestinations.NOTIFICATION_PERMISSION) {
                        popUpTo(WeatherDestinations.DOG_QUESTION) { inclusive = true }
                        launchSingleTop = true
                    }
                },
            )
        }

        composable(WeatherDestinations.NOTIFICATION_PERMISSION) {
            val settingsViewModel = hiltViewModel<SettingsViewModel>()
            OnboardingNotificationScreen(
                hasDog = answeredHasDog,
                walkTimeEnabled = settingsViewModel.remotelyEnabled(NotificationKind.WALK_TIME),
                rainEnabled = settingsViewModel.remotelyEnabled(NotificationKind.RAIN),
                onDone = {
                    welcomeViewModel.completeNotificationOnboarding()
                    navController.navigate(WeatherDestinations.LOCATION_PERMISSION) {
                        popUpTo(WeatherDestinations.NOTIFICATION_PERMISSION) { inclusive = true }
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
                onOpenSettings = {
                    navController.navigate(WeatherDestinations.SETTINGS)
                },
            )
        }

        composable(WeatherDestinations.SETTINGS) {
            SettingsRoute(
                onNavigateBack = { navController.popBackStack() },
                onOpenNotifications = {
                    navController.navigate(WeatherDestinations.NOTIFICATION_SETTINGS)
                },
                onOpenLocations = {
                    navController.navigate(WeatherDestinations.LOCATIONS)
                },
            )
        }

        composable(WeatherDestinations.NOTIFICATION_SETTINGS) {
            NotificationSettingsRoute(onNavigateBack = { navController.popBackStack() })
        }

        composable(WeatherDestinations.LOCATIONS) {
            val geoViewModel = hiltViewModel<com.fangjet.weather.model.GeoLocationViewModel>(
                viewModelStoreOwner = activity,
            )
            LocationsRoute(
                locations = cityListViewModel.locations.collectAsStateWithLifecycle().value,
                onReorder = cityListViewModel::reorderLocations,
                onRemove = cityListViewModel::removeLocation,
                onNavigateBack = { navController.popBackStack() },
                geoViewModel = geoViewModel,
                cityListViewModel = cityListViewModel,
            )
        }
    }
}
