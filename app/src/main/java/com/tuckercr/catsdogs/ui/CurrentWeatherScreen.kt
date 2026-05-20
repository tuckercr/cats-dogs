package com.tuckercr.catsdogs.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tuckercr.catsdogs.R
import com.tuckercr.catsdogs.domain.CitySuggestion
import com.tuckercr.catsdogs.domain.CurrentWeather
import com.tuckercr.catsdogs.domain.WeatherUnits
import com.tuckercr.catsdogs.model.GeoLocationViewModel
import com.tuckercr.catsdogs.model.LoadingState
import com.tuckercr.catsdogs.model.WeatherForecastViewModel
import com.tuckercr.catsdogs.ui.theme.CatsDogsTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrentWeatherRoute(onOpenForecast: () -> Unit) {
    val geoLocationViewModel = activityHiltViewModel<GeoLocationViewModel>()
    val weatherForecastViewModel = activityHiltViewModel<WeatherForecastViewModel>()
    val city by geoLocationViewModel.cityInput.collectAsStateWithLifecycle()
    val suggestions by geoLocationViewModel.citySuggestions.collectAsStateWithLifecycle()
    val suggestLoading by geoLocationViewModel.citySuggestLoading.collectAsStateWithLifecycle()
    val state by weatherForecastViewModel.currentWeather.collectAsStateWithLifecycle()
    val resolved by weatherForecastViewModel.resolvedCity.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        geoLocationViewModel.restoreSavedCityOnce()?.let { restoredCity ->
            geoLocationViewModel.dismissSuggestions()
            weatherForecastViewModel.refreshCurrent(
                city = restoredCity,
                latitude = geoLocationViewModel.pinnedLatitude(),
                longitude = geoLocationViewModel.pinnedLongitude(),
            )
        }
    }

    CurrentWeatherScreen(
        city = city,
        citySuggestions = suggestions,
        citySuggestLoading = suggestLoading,
        onCityChange = geoLocationViewModel::onCityInputChange,
        onCitySuggestionChosen = geoLocationViewModel::onCitySuggestionChosen,
        onSearch = {
            geoLocationViewModel.dismissSuggestions()
            weatherForecastViewModel.refreshCurrent(
                city = city,
                latitude = geoLocationViewModel.pinnedLatitude(),
                longitude = geoLocationViewModel.pinnedLongitude(),
            )
        },
        state = state,
        onRetry = {
            weatherForecastViewModel.clearCurrentError()
            geoLocationViewModel.dismissSuggestions()
            weatherForecastViewModel.refreshCurrent(
                city = city,
                latitude = geoLocationViewModel.pinnedLatitude(),
                longitude = geoLocationViewModel.pinnedLongitude(),
            )
        },
        onOpenForecast = onOpenForecast,
        forecastEnabled = resolved != null,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrentWeatherScreen(
    city: String,
    citySuggestions: List<CitySuggestion>,
    citySuggestLoading: Boolean,
    onCityChange: (String) -> Unit,
    onCitySuggestionChosen: (CitySuggestion) -> Unit,
    onSearch: () -> Unit,
    state: LoadingState<CurrentWeather>,
    onRetry: () -> Unit,
    onOpenForecast: () -> Unit,
    forecastEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val keyboardController = LocalSoftwareKeyboardController.current

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.screen_current_weather)) },
                actions = {
                    IconButton(
                        onClick = onOpenForecast,
                        enabled = forecastEnabled,
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = stringResource(R.string.cd_open_forecast),
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Spacer(modifier = Modifier.height(4.dp))
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = city,
                    onValueChange = onCityChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.field_city_label)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            keyboardController?.hide()
                            onSearch()
                        },
                    ),
                )
                if (citySuggestLoading) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
                if (citySuggestions.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 220.dp),
                        ) {
                            citySuggestions.forEach { suggestion ->
                                TextButton(
                                    onClick = {
                                        keyboardController?.hide()
                                        onCitySuggestionChosen(suggestion)
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Text(
                                        text = suggestion.label,
                                        modifier = Modifier.fillMaxWidth(),
                                        textAlign = TextAlign.Start,
                                        style = MaterialTheme.typography.bodyLarge,
                                    )
                                }
                            }
                        }
                    }
                }
            }
            Button(
                onClick = {
                    keyboardController?.hide()
                    onSearch()
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = state !is LoadingState.Loading,
            ) {
                Text(stringResource(R.string.action_get_weather))
            }

            when (state) {
                LoadingState.Idle -> Text(
                    text = stringResource(R.string.hint_enter_city),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                LoadingState.Loading -> Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    CircularProgressIndicator()
                }

                is LoadingState.Error -> Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = state.message,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error,
                    )
                    if (state.canRetry) {
                        TextButton(onClick = onRetry) {
                            Text(stringResource(R.string.action_retry))
                        }
                    }
                }

                is LoadingState.Success -> CurrentWeatherContent(weather = state.data)
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CurrentWeatherScreenIdlePreview() {
    CatsDogsTheme {
        CurrentWeatherScreen(
            city = "",
            citySuggestions = emptyList(),
            citySuggestLoading = false,
            onCityChange = {},
            onCitySuggestionChosen = {},
            onSearch = {},
            state = LoadingState.Idle,
            onRetry = {},
            onOpenForecast = {},
            forecastEnabled = false,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun CurrentWeatherScreenSuggestionsPreview() {
    CatsDogsTheme {
        CurrentWeatherScreen(
            city = "Lon",
            citySuggestions = listOf(
                CitySuggestion("London, GB", 51.5074, -0.1278),
                CitySuggestion("London, ON, CA", 42.9849, -81.2453),
            ),
            citySuggestLoading = false,
            onCityChange = {},
            onCitySuggestionChosen = {},
            onSearch = {},
            state = LoadingState.Idle,
            onRetry = {},
            onOpenForecast = {},
            forecastEnabled = false,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun CurrentWeatherScreenLoadingPreview() {
    CatsDogsTheme {
        CurrentWeatherScreen(
            city = "London",
            citySuggestions = emptyList(),
            citySuggestLoading = false,
            onCityChange = {},
            onCitySuggestionChosen = {},
            onSearch = {},
            state = LoadingState.Loading,
            onRetry = {},
            onOpenForecast = {},
            forecastEnabled = false,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun CurrentWeatherScreenErrorPreview() {
    CatsDogsTheme {
        CurrentWeatherScreen(
            city = "London",
            citySuggestions = emptyList(),
            citySuggestLoading = false,
            onCityChange = {},
            onCitySuggestionChosen = {},
            onSearch = {},
            state = LoadingState.Error("Network error", canRetry = true),
            onRetry = {},
            onOpenForecast = {},
            forecastEnabled = false,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun CurrentWeatherScreenSuccessPreview() {
    CatsDogsTheme {
        CurrentWeatherScreen(
            city = "London",
            citySuggestions = emptyList(),
            citySuggestLoading = false,
            onCityChange = {},
            onCitySuggestionChosen = {},
            onSearch = {},
            state = LoadingState.Success(
                CurrentWeather(
                    cityName = "London",
                    conditionMain = "Clouds",
                    description = "broken clouds",
                    iconCode = "04d",
                    temperature = 15.0,
                    feelsLike = 14.2,
                    humidityPercent = 72,
                    windSpeed = 4.1,
                    units = WeatherUnits.METRIC,
                ),
            ),
            onRetry = {},
            onOpenForecast = { /* no-op */ },
            forecastEnabled = true,
        )
    }
}

@Composable
private fun CurrentWeatherContent(
    weather: CurrentWeather,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                WeatherIcon(
                    iconCode = weather.iconCode,
                    contentDescription = weather.description,
                    sizeDp = 72,
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = weather.cityName,
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Text(
                        text = weather.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        MetricRow(
            label = stringResource(R.string.label_temperature),
            value = formatTemperature(weather.temperature, weather.units),
        )
        MetricRow(
            label = stringResource(R.string.label_feels_like),
            value = formatTemperature(weather.feelsLike, weather.units),
        )
        MetricRow(
            label = stringResource(R.string.label_humidity),
            value = stringResource(R.string.format_percent, weather.humidityPercent),
        )
        MetricRow(
            label = stringResource(R.string.label_wind),
            value = formatWind(weather.windSpeed, weather.units),
        )
    }
}

@Composable
private fun formatTemperature(
    value: Double,
    units: WeatherUnits,
): String =
    when (units) {
        WeatherUnits.METRIC -> stringResource(R.string.format_temperature_c, value)
        WeatherUnits.IMPERIAL -> stringResource(R.string.format_temperature_f, value)
    }

@Composable
private fun formatWind(
    speed: Double,
    units: WeatherUnits,
): String =
    when (units) {
        WeatherUnits.METRIC -> stringResource(R.string.format_wind_ms, speed)
        WeatherUnits.IMPERIAL -> stringResource(R.string.format_wind_mph, speed)
    }

@Composable
private fun MetricRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
        Text(text = value, style = MaterialTheme.typography.bodyLarge)
    }
}
