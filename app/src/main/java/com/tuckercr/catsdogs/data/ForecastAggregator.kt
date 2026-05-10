package com.tuckercr.catsdogs.data

import com.tuckercr.catsdogs.domain.DayForecast
import com.tuckercr.catsdogs.domain.WeatherUnits
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

/**
 * Collapses OpenWeatherMap 3-hour forecast samples into one representative row per calendar day
 * (sample closest to local noon).
 */
object ForecastAggregator {

    private val dayLabelFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("EEE, MMM d")

    data class Slot(
        val epochSeconds: Long,
        val temperature: Double,
        val feelsLike: Double,
        val conditionMain: String,
        val description: String,
        val iconCode: String,
    )

    fun aggregate(slots: List<Slot>, zoneId: ZoneId, units: WeatherUnits): List<DayForecast> {
        if (slots.isEmpty()) return emptyList()

        val byDay = slots.groupBy { slot ->
            ZonedDateTime.ofInstant(Instant.ofEpochSecond(slot.epochSeconds), zoneId).toLocalDate()
        }

        return byDay.keys.sorted().map { date ->
            val daySlots = byDay[date].orEmpty()
            val noonMinutes = 12 * 60
            val representative = daySlots.minBy { slot ->
                val zdt = ZonedDateTime.ofInstant(Instant.ofEpochSecond(slot.epochSeconds), zoneId)
                val minutes = zdt.hour * 60 + zdt.minute
                abs(minutes - noonMinutes)
            }
            DayForecast(
                dateLabel = date.atStartOfDay(zoneId).format(dayLabelFormatter),
                conditionMain = representative.conditionMain,
                description = representative.description.replaceFirstChar { it.titlecase(Locale.getDefault()) },
                iconCode = representative.iconCode,
                temperature = representative.temperature,
                feelsLike = representative.feelsLike,
                units = units,
            )
        }
    }
}
