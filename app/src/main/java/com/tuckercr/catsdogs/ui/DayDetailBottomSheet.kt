package com.tuckercr.catsdogs.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tuckercr.catsdogs.R
import com.tuckercr.catsdogs.domain.DayForecast
import com.tuckercr.catsdogs.domain.HourlySlot
import com.tuckercr.catsdogs.domain.WeatherUnits
import com.tuckercr.catsdogs.ui.theme.CatsDogsTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayDetailBottomSheet(
    day: DayForecast,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        DayDetailContent(day = day)
    }
}

@Composable
private fun DayDetailContent(
    day: DayForecast,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            WeatherIcon(
                iconCode = day.iconCode,
                contentDescription = day.description,
                sizeDp = 64,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = day.dateLabel,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = day.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formatDayDetailTemp(day.tempMax, day.units),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = formatDayDetailTemp(day.tempMin, day.units),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp))
        Spacer(modifier = Modifier.height(8.dp))

        if (day.hourlySlots.isEmpty()) {
            Text(
                text = stringResource(R.string.detail_no_hourly_data),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(horizontal = 20.dp, vertical = 16.dp)
                    .fillMaxWidth(),
            )
        } else {
            // Column header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.detail_time_label),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = stringResource(R.string.detail_temp_label),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(end = 8.dp),
                )
                Text(
                    text = stringResource(R.string.detail_wind_label),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(end = 8.dp),
                )
                Text(
                    text = stringResource(R.string.detail_humidity_label),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
            ) {
                items(day.hourlySlots) { slot ->
                    HourlySlotRow(slot = slot)
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 20.dp),
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun HourlySlotRow(
    slot: HourlySlot,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = slot.timeLabel,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f),
        )
        WeatherIcon(
            iconCode = slot.iconCode,
            contentDescription = slot.description,
            sizeDp = 28,
        )
        Text(
            text = formatHourlyTemp(slot.temperature, slot.units),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 4.dp),
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Air,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(14.dp),
            )
            Text(
                text = formatHourlyWind(slot.windSpeed, slot.units),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Icon(
                imageVector = Icons.Default.WaterDrop,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(14.dp),
            )
            Text(
                text = stringResource(R.string.format_percent, slot.humidity),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun formatDayDetailTemp(
    value: Double,
    units: WeatherUnits,
): String =
    when (units) {
        WeatherUnits.METRIC -> stringResource(R.string.format_temperature_c, value)
        WeatherUnits.IMPERIAL -> stringResource(R.string.format_temperature_f, value)
    }

@Composable
private fun formatHourlyTemp(
    value: Double,
    units: WeatherUnits,
): String =
    when (units) {
        WeatherUnits.METRIC -> stringResource(R.string.format_temperature_c, value)
        WeatherUnits.IMPERIAL -> stringResource(R.string.format_temperature_f, value)
    }

@Composable
private fun formatHourlyWind(
    speed: Double,
    units: WeatherUnits,
): String =
    when (units) {
        WeatherUnits.METRIC -> stringResource(R.string.format_wind_ms, speed)
        WeatherUnits.IMPERIAL -> stringResource(R.string.format_wind_mph, speed)
    }

// --- Previews ---

private val previewSlots = listOf(
    HourlySlot("12 AM", "01n", "Clear sky", 14.0, 13.0, 3.2, 180, 55, 1013, WeatherUnits.METRIC),
    HourlySlot("3 AM", "02n", "Few clouds", 13.5, 12.5, 2.8, 200, 60, 1013, WeatherUnits.METRIC),
    HourlySlot(
        "6 AM",
        "03d",
        "Scattered clouds",
        14.0,
        13.2,
        3.0,
        190,
        62,
        1014,
        WeatherUnits.METRIC,
    ),
    HourlySlot("9 AM", "04d", "Overcast", 16.5, 15.8, 4.1, 210, 65, 1012, WeatherUnits.METRIC),
    HourlySlot("12 PM", "10d", "Light rain", 17.0, 16.2, 5.0, 220, 80, 1010, WeatherUnits.METRIC),
    HourlySlot("3 PM", "10d", "Light rain", 18.0, 17.0, 5.5, 215, 78, 1009, WeatherUnits.METRIC),
    HourlySlot("6 PM", "04d", "Overcast", 17.5, 16.8, 4.8, 205, 72, 1010, WeatherUnits.METRIC),
    HourlySlot(
        "9 PM",
        "03n",
        "Scattered clouds",
        16.0,
        15.5,
        3.5,
        195,
        68,
        1011,
        WeatherUnits.METRIC,
    ),
)

@Preview(showBackground = true)
@Composable
private fun DayDetailContentPreview() {
    CatsDogsTheme {
        DayDetailContent(
            day = DayForecast(
                dateLabel = "Wed, Jun 18",
                conditionMain = "Rain",
                description = "Light rain",
                iconCode = "10d",
                temperature = 17.0,
                feelsLike = 16.0,
                tempMin = 13.5,
                tempMax = 18.0,
                units = WeatherUnits.METRIC,
                hourlySlots = previewSlots,
            ),
        )
    }
}
