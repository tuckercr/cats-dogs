package com.tuckercr.catsdogs.ui

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbCloudy
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tuckercr.catsdogs.R
import com.tuckercr.catsdogs.domain.CitySuggestion
import com.tuckercr.catsdogs.domain.CurrentWeather
import com.tuckercr.catsdogs.domain.DayForecast
import com.tuckercr.catsdogs.domain.SavedLocation
import com.tuckercr.catsdogs.domain.WeatherUnits
import com.tuckercr.catsdogs.model.CityListViewModel
import com.tuckercr.catsdogs.model.GeoLocationViewModel
import com.tuckercr.catsdogs.model.LoadingState
import com.tuckercr.catsdogs.model.WeatherForecastViewModel
import com.tuckercr.catsdogs.ui.theme.CatsDogsTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrentWeatherRoute(onOpenSettings: () -> Unit = {}) {
    val activity = LocalActivity.current as ComponentActivity
    val cityListViewModel = hiltViewModel<CityListViewModel>(viewModelStoreOwner = activity)
    val weatherForecastViewModel =
        hiltViewModel<WeatherForecastViewModel>(viewModelStoreOwner = activity)
    val geoViewModel = hiltViewModel<GeoLocationViewModel>(viewModelStoreOwner = activity)

    val locations by cityListViewModel.locations.collectAsStateWithLifecycle()
    val activeIndex by cityListViewModel.activeIndex.collectAsStateWithLifecycle()
    val activeLocation by cityListViewModel.activeLocation.collectAsStateWithLifecycle()
    val weatherState by weatherForecastViewModel.currentWeather.collectAsStateWithLifecycle()
    val forecastState by weatherForecastViewModel.forecast.collectAsStateWithLifecycle()
    val isRefreshing by weatherForecastViewModel.isRefreshing.collectAsStateWithLifecycle()
    val cityInput by geoViewModel.cityInput.collectAsStateWithLifecycle()
    val suggestions by geoViewModel.citySuggestions.collectAsStateWithLifecycle()
    val suggestLoading by geoViewModel.citySuggestLoading.collectAsStateWithLifecycle()
    val selectedSuggestion by geoViewModel.selectedSuggestion.collectAsStateWithLifecycle()

    // Full refresh whenever the active location changes
    LaunchedEffect(activeLocation) {
        activeLocation?.let {
            weatherForecastViewModel.refreshCurrent(it)
            weatherForecastViewModel.refreshForecast(it)
        }
    }

    // Silent background refresh on every foreground resume (picks up new units or stale data)
    val currentActiveLocation by rememberUpdatedState(activeLocation)
    val lifecycleOwner = LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                currentActiveLocation?.let {
                    weatherForecastViewModel.backgroundRefreshCurrent(it)
                    weatherForecastViewModel.backgroundRefreshForecast(it)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    var showAddSheet by remember { mutableStateOf(false) }

    CurrentWeatherScreen(
        locations = locations,
        activeIndex = activeIndex,
        weatherState = weatherState,
        forecastState = forecastState,
        isRefreshing = isRefreshing,
        onTabSelected = cityListViewModel::setActiveIndex,
        onOpenSettings = {
            weatherForecastViewModel.logSettingsOpened()
            onOpenSettings()
        },
        onAddCityClick = { showAddSheet = true },
        onRefresh = {
            activeLocation?.let {
                weatherForecastViewModel.refreshCurrent(it)
                weatherForecastViewModel.refreshForecast(it)
            }
        },
    ) {
        activeLocation?.let {
            weatherForecastViewModel.refreshCurrent(it)
            weatherForecastViewModel.refreshForecast(it)
        }
    }

    if (showAddSheet) {
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(
            onDismissRequest = {
                geoViewModel.reset()
                showAddSheet = false
            },
            sheetState = sheetState,
        ) {
            AddCitySheetContent(
                savedLocations = locations,
                cityInput = cityInput,
                suggestions = suggestions,
                suggestLoading = suggestLoading,
                selectedSuggestion = selectedSuggestion,
                onCityInputChange = geoViewModel::onCityInputChange,
                onSuggestionChosen = geoViewModel::onCitySuggestionChosen,
                onAddCity = {
                    val suggestion = selectedSuggestion
                    if (suggestion != null) {
                        cityListViewModel.addLocation(
                            SavedLocation(
                                label = suggestion.label,
                                latitude = suggestion.weatherLat,
                                longitude = suggestion.weatherLon,
                            ),
                        )
                    } else if (cityInput.isNotBlank()) {
                        cityListViewModel.addLocation(
                            SavedLocation(
                                label = cityInput.trim(),
                                latitude = null,
                                longitude = null,
                            ),
                        )
                    }
                    geoViewModel.reset()
                    showAddSheet = false
                },
                onRemoveSaved = cityListViewModel::removeLocation,
                onDismiss = {
                    geoViewModel.reset()
                    showAddSheet = false
                },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun CurrentWeatherScreen(
    modifier: Modifier = Modifier,
    locations: List<SavedLocation>,
    activeIndex: Int,
    weatherState: LoadingState<CurrentWeather>,
    forecastState: LoadingState<List<DayForecast>> = LoadingState.Idle,
    isRefreshing: Boolean = false,
    onTabSelected: (Int) -> Unit,
    onOpenSettings: () -> Unit = {},
    onAddCityClick: () -> Unit,
    onRefresh: () -> Unit = {},
    onRetry: () -> Unit,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Text(
                            text = if (locations.isEmpty()) {
                                stringResource(R.string.app_name)
                            } else {
                                locations.getOrNull(activeIndex)?.label
                                    ?: stringResource(R.string.app_name)
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    actions = {
                        IconButton(onClick = onAddCityClick) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = stringResource(R.string.cd_add_city),
                            )
                        }
                        IconButton(onClick = onOpenSettings) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = stringResource(R.string.cd_open_settings),
                            )
                        }
                    },
                )
                if (locations.size >= 2) {
                    PrimaryScrollableTabRow(
                        selectedTabIndex = activeIndex,
                        edgePadding = 16.dp,
                    ) {
                        locations.forEachIndexed { index, location ->
                            Tab(
                                selected = index == activeIndex,
                                onClick = { onTabSelected(index) },
                            ) {
                                Row(
                                    modifier = Modifier.padding(
                                        vertical = 12.dp,
                                        horizontal = 4.dp,
                                    ),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    if (location.isCurrentLocation) {
                                        Icon(
                                            imageVector = Icons.Default.LocationOn,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp),
                                        )
                                    }
                                    Text(
                                        text = location.label,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
    ) { padding ->
        if (locations.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                EmptyLocationsState(onAddCityClick = onAddCityClick)
            }
        } else {
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = onRefresh,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Spacer(modifier = Modifier.height(8.dp))
                    when (weatherState) {
                        LoadingState.Idle, LoadingState.Loading -> Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator()
                        }

                        is LoadingState.Error -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = weatherErrorMessage(weatherState.errorKey),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.error,
                            )
                            if (weatherState.canRetry) {
                                TextButton(onClick = onRetry) {
                                    Text(stringResource(R.string.action_retry))
                                }
                            }
                        }

                        is LoadingState.Success -> CurrentWeatherContent(
                            weather = weatherState.data,
                            forecastState = forecastState,
                            location = locations.getOrNull(activeIndex),
                            onForecastRetry = onRetry,
                        )
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun EmptyLocationsState(
    onAddCityClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(
            imageVector = Icons.Default.Cloud,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
        )
        Text(
            text = stringResource(R.string.empty_cities_title),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.empty_cities_body),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(onClick = onAddCityClick) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.size(8.dp))
            Text(stringResource(R.string.action_add_city))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCitySheetContent(
    savedLocations: List<SavedLocation>,
    cityInput: String,
    suggestions: List<CitySuggestion>,
    suggestLoading: Boolean,
    selectedSuggestion: CitySuggestion?,
    onCityInputChange: (String) -> Unit,
    onSuggestionChosen: (CitySuggestion) -> Unit,
    onAddCity: () -> Unit,
    onRemoveSaved: (Int) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val keyboardController = LocalSoftwareKeyboardController.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.add_city_sheet_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 8.dp),
        )

        if (savedLocations.isNotEmpty()) {
            Text(
                text = stringResource(R.string.saved_cities_section),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            savedLocations.forEachIndexed { index, location ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (location.isCurrentLocation) {
                        Icon(
                            Icons.Default.LocationOn,
                            contentDescription = null,
                            modifier = Modifier
                                .size(16.dp)
                                .padding(end = 0.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(modifier = Modifier.size(4.dp))
                    }
                    Text(
                        text = location.label,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    IconButton(onClick = { onRemoveSaved(index) }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = stringResource(R.string.cd_remove_city),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            HorizontalDivider()
        }

        Text(
            text = stringResource(R.string.search_city_section),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Column(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = cityInput,
                onValueChange = onCityInputChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.field_city_label)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { keyboardController?.hide() }),
            )
            if (suggestLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            if (suggestions.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 220.dp),
                    ) {
                        suggestions.forEach { suggestion ->
                            TextButton(
                                onClick = {
                                    keyboardController?.hide()
                                    onSuggestionChosen(suggestion)
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
                onAddCity()
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = cityInput.isNotBlank() || selectedSuggestion != null,
        ) {
            Text(stringResource(R.string.action_add_city))
        }
        OutlinedButton(
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.action_cancel))
        }
    }
}

@Composable
private fun CurrentWeatherContent(
    weather: CurrentWeather,
    forecastState: LoadingState<List<DayForecast>>,
    modifier: Modifier = Modifier,
    location: SavedLocation? = null,
    onForecastRetry: () -> Unit = {},
) {
    val forecastDays = (forecastState as? LoadingState.Success)?.data.orEmpty()
    val todayLabel = remember {
        java.time.LocalDate
            .now()
            .format(
                java.time.format.DateTimeFormatter
                    .ofPattern("EEE, MMM d"),
            )
    }
    val todayForecast = forecastDays.firstOrNull()?.takeIf { it.dateLabel == todayLabel }
    val upcomingDays = if (todayForecast != null) forecastDays.drop(1) else forecastDays
    var selectedDay by remember { mutableStateOf<DayForecast?>(null) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (todayForecast != null) {
                        Modifier.clickable { selectedDay = todayForecast }
                    } else {
                        Modifier
                    },
                ),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                WeatherIcon(
                    iconCode = weather.iconCode,
                    contentDescription = weather.description,
                    sizeDp = 88,
                )
                Text(
                    text = weather.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (location != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = weather.cityName,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = formatTemperature(weather.temperature, weather.units),
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    text = stringResource(
                        R.string.label_temp_range,
                        formatTemperature(weather.tempMin, weather.units),
                        formatTemperature(weather.tempMax, weather.units),
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(R.string.label_feels_like) + " " + formatTemperature(
                        weather.feelsLike,
                        weather.units,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // Details card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                MetricIconRow(
                    icon = Icons.Default.WaterDrop,
                    label = stringResource(R.string.label_humidity),
                    value = stringResource(R.string.format_percent, weather.humidityPercent),
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                MetricIconRow(
                    icon = Icons.Default.Air,
                    label = stringResource(R.string.label_wind),
                    value = formatWind(weather.windSpeed, weather.units) + "  " + windDirection(
                        weather.windDeg,
                    ),
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                MetricIconRow(
                    icon = Icons.Default.Compress,
                    label = stringResource(R.string.label_pressure),
                    value = stringResource(R.string.format_pressure_hpa, weather.pressureHpa),
                )
                weather.visibilityMeters?.let { visibilityMeters ->
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    MetricIconRow(
                        icon = Icons.Default.Visibility,
                        label = stringResource(R.string.label_visibility),
                        value = formatVisibility(visibilityMeters),
                    )
                }
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                MetricIconRow(
                    icon = Icons.Default.WbCloudy,
                    label = stringResource(R.string.label_cloud_cover),
                    value = stringResource(R.string.format_percent, weather.cloudPercent),
                )
                weather.sunriseEpoch?.let { epoch ->
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    MetricIconRow(
                        icon = Icons.Default.WbSunny,
                        label = stringResource(R.string.label_sunrise),
                        value = formatEpochTime(epoch),
                    )
                }
                weather.sunsetEpoch?.let { epoch ->
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    MetricIconRow(
                        icon = Icons.Default.WbSunny,
                        label = stringResource(R.string.label_sunset),
                        value = formatEpochTime(epoch),
                    )
                }
            }
        }

        // Weather radar
        RadarCard(location = location)

        // Upcoming days section (excludes today)
        if (upcomingDays.isNotEmpty() ||
            forecastState is LoadingState.Loading ||
            forecastState is LoadingState.Error
        ) {
            Text(
                text = stringResource(R.string.section_upcoming),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
            when {
                upcomingDays.isNotEmpty() -> upcomingDays.forEach { day ->
                    UpcomingDayRow(day = day, onClick = { selectedDay = day })
                }

                forecastState is LoadingState.Loading -> Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }

                forecastState is LoadingState.Error -> Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = weatherErrorMessage(forecastState.errorKey),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                    if (forecastState.canRetry) {
                        TextButton(onClick = onForecastRetry) {
                            Text(stringResource(R.string.action_retry))
                        }
                    }
                }
            }
        }
    }

    selectedDay?.let { day ->
        DayDetailBottomSheet(day = day) { selectedDay = null }
    }
}

@Composable
internal fun weatherErrorMessage(errorKey: String): String =
    when (errorKey) {
        "offline" -> stringResource(R.string.error_offline)
        "city_not_found" -> stringResource(R.string.error_city_not_found)
        "missing_api_key", "bad_api_key" -> stringResource(R.string.error_bad_api_key)
        "rate_limited" -> stringResource(R.string.error_rate_limited)
        "server_error" -> stringResource(R.string.error_server)
        else -> stringResource(R.string.error_generic)
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
private fun formatVisibility(meters: Int): String =
    if (meters >= 1000) {
        stringResource(R.string.format_visibility_km, meters / 1000.0)
    } else {
        stringResource(R.string.format_visibility_m, meters)
    }

private fun formatEpochTime(epochSeconds: Long): String {
    val localTime = java.time.Instant
        .ofEpochSecond(epochSeconds)
        .atZone(java.time.ZoneId.systemDefault())
        .toLocalTime()
    return localTime.format(
        java.time.format.DateTimeFormatter
            .ofPattern("h:mm a"),
    )
}

private fun windDirection(deg: Int): String {
    val directions = listOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
    return directions[((deg + 22) / 45) % 8]
}

@Composable
private fun UpcomingDayRow(
    day: DayForecast,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        WeatherIcon(iconCode = day.iconCode, contentDescription = day.description, sizeDp = 36)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = day.dateLabel,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = day.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = formatTemperature(day.tempMax, day.units),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = formatTemperature(day.tempMin, day.units),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
private fun MetricIconRow(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
        )
    }
}

// --- Previews ---

@Preview(showBackground = true)
@Composable
private fun CurrentWeatherEmptyPreview() {
    CatsDogsTheme {
        CurrentWeatherScreen(
            locations = emptyList(),
            activeIndex = 0,
            weatherState = LoadingState.Idle,
            onTabSelected = {},
            onAddCityClick = {},
        ) {}
    }
}

private val previewWeather = CurrentWeather(
    cityName = "London",
    conditionMain = "Clouds",
    description = "Broken clouds",
    iconCode = "04d",
    temperature = 15.0,
    feelsLike = 14.2,
    tempMin = 12.0,
    tempMax = 17.5,
    humidityPercent = 72,
    pressureHpa = 1012,
    windSpeed = 4.1,
    windDeg = 225,
    visibilityMeters = 9000,
    cloudPercent = 75,
    units = WeatherUnits.METRIC,
)

@Preview(showBackground = true)
@Composable
private fun CurrentWeatherSuccessPreview() {
    CatsDogsTheme {
        CurrentWeatherScreen(
            locations = listOf(
                SavedLocation("My Location", 51.5, -0.1, isCurrentLocation = true),
                SavedLocation("London, GB", 51.5074, -0.1278),
                SavedLocation("New York, US", 40.7128, -74.0060),
            ),
            activeIndex = 0,
            weatherState = LoadingState.Success(previewWeather),
            onTabSelected = {},
            onAddCityClick = {},
        ) {}
    }
}

@Preview(showBackground = true)
@Composable
private fun AddCitySheetPreview() {
    CatsDogsTheme {
        AddCitySheetContent(
            savedLocations = listOf(
                SavedLocation("My Location", 51.5, -0.1, isCurrentLocation = true),
                SavedLocation("London, GB", 51.5074, -0.1278),
            ),
            cityInput = "New Y",
            suggestions = listOf(
                CitySuggestion("New York, US", 40.71, -74.0),
                CitySuggestion("New Haven, US", 41.30, -72.92),
            ),
            suggestLoading = false,
            selectedSuggestion = null,
            onCityInputChange = {},
            onSuggestionChosen = {},
            onAddCity = {},
            onRemoveSaved = {},
            onDismiss = {},
        )
    }
}
