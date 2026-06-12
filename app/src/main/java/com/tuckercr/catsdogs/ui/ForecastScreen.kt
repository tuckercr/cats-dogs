package com.tuckercr.catsdogs.ui

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tuckercr.catsdogs.R
import com.tuckercr.catsdogs.domain.DayForecast
import com.tuckercr.catsdogs.domain.WeatherUnits
import com.tuckercr.catsdogs.model.CityListViewModel
import com.tuckercr.catsdogs.model.LoadingState
import com.tuckercr.catsdogs.model.WeatherForecastViewModel
import com.tuckercr.catsdogs.ui.theme.CatsDogsTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForecastRoute(onNavigateBack: () -> Unit) {
    val activity = LocalActivity.current as ComponentActivity
    val cityListViewModel = hiltViewModel<CityListViewModel>(viewModelStoreOwner = activity)
    val weatherForecastViewModel =
        hiltViewModel<WeatherForecastViewModel>(viewModelStoreOwner = activity)
    val state by weatherForecastViewModel.forecast.collectAsStateWithLifecycle()
    val activeLocation by cityListViewModel.activeLocation.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        activeLocation?.let { weatherForecastViewModel.refreshForecast(it) }
    }

    ForecastScreen(
        state = state,
        onRetry = {
            weatherForecastViewModel.clearForecastError()
            activeLocation?.let { weatherForecastViewModel.refreshForecast(it) }
        },
        onNavigateBack = onNavigateBack,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForecastScreen(
    state: LoadingState<List<DayForecast>>,
    onRetry: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.screen_forecast)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_navigate_back),
                        )
                    }
                },
            )
        },
    ) { padding ->
        when (state) {
            LoadingState.Idle, LoadingState.Loading -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CircularProgressIndicator()
            }

            is LoadingState.Error -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
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

            is LoadingState.Success -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item { Spacer(modifier = Modifier.height(4.dp)) }
                items(state.data, key = { it.dateLabel }) { day ->
                    ForecastDayCard(day = day)
                }
                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
private fun formatTemp(
    value: Double,
    units: WeatherUnits,
): String =
    when (units) {
        WeatherUnits.METRIC -> stringResource(R.string.format_temperature_c, value)
        WeatherUnits.IMPERIAL -> stringResource(R.string.format_temperature_f, value)
    }

@Composable
private fun ForecastDayCard(
    day: DayForecast,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            WeatherIcon(
                iconCode = day.iconCode,
                contentDescription = day.description,
                sizeDp = 56,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = day.dateLabel,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                )
                Text(
                    text = day.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formatTemp(day.tempMax, day.units),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                )
                Text(
                    text = formatTemp(day.tempMin, day.units),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ForecastScreenLoadingPreview() {
    CatsDogsTheme {
        ForecastScreen(
            state = LoadingState.Loading,
            onRetry = {},
            onNavigateBack = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ForecastScreenErrorPreview() {
    CatsDogsTheme {
        ForecastScreen(
            state = LoadingState.Error("Something went wrong", canRetry = true),
            onRetry = {},
            onNavigateBack = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ForecastScreenSuccessPreview() {
    val sampleDayForecasts = listOf(
        DayForecast(
            dateLabel = "Today",
            conditionMain = "Clouds",
            description = "Overcast clouds",
            iconCode = "04d",
            temperature = 22.0,
            feelsLike = 21.5,
            tempMin = 18.0,
            tempMax = 24.0,
            units = WeatherUnits.METRIC,
        ),
        DayForecast(
            dateLabel = "Tomorrow",
            conditionMain = "Rain",
            description = "Light rain",
            iconCode = "10d",
            temperature = 18.0,
            feelsLike = 17.5,
            tempMin = 15.0,
            tempMax = 20.0,
            units = WeatherUnits.METRIC,
        ),
        DayForecast(
            dateLabel = "Wednesday",
            conditionMain = "Clear",
            description = "Clear sky",
            iconCode = "01d",
            temperature = 25.0,
            feelsLike = 24.5,
            tempMin = 20.0,
            tempMax = 27.0,
            units = WeatherUnits.METRIC,
        ),
    )

    CatsDogsTheme {
        ForecastScreen(
            state = LoadingState.Success(sampleDayForecasts),
            onRetry = {},
            onNavigateBack = {},
        )
    }
}
