package com.tuckercr.catsdogs.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.tuckercr.catsdogs.R
import com.tuckercr.catsdogs.domain.CurrentWeather
import com.tuckercr.catsdogs.domain.DayForecast
import com.tuckercr.catsdogs.domain.HourlySlot
import com.tuckercr.catsdogs.domain.SavedLocation
import com.tuckercr.catsdogs.domain.WeatherUnits
import com.tuckercr.catsdogs.model.LoadingState
import com.tuckercr.catsdogs.ui.theme.CatsDogsTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class CurrentWeatherScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun string(id: Int) = composeRule.activity.getString(id)

    @Test
    fun emptyStateShowsPromptAndAddCityInvokesCallback() {
        var addClicks = 0
        composeRule.setContent {
            CatsDogsTheme {
                CurrentWeatherScreen(
                    locations = emptyList(),
                    activeIndex = 0,
                    weatherState = LoadingState.Idle,
                    onTabSelected = {},
                    onAddCityClick = { addClicks++ },
                    onRetry = {},
                )
            }
        }

        composeRule.onNodeWithText(string(R.string.empty_cities_title)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.action_add_city)).performClick()

        assertEquals(1, addClicks)
    }

    @Test
    fun errorStateShowsMessageAndRetryInvokesCallback() {
        var retries = 0
        composeRule.setContent {
            CatsDogsTheme {
                CurrentWeatherScreen(
                    locations = listOf(london),
                    activeIndex = 0,
                    weatherState = LoadingState.Error("offline", canRetry = true),
                    onTabSelected = {},
                    onAddCityClick = {},
                    onRetry = { retries++ },
                )
            }
        }

        composeRule.onNodeWithText(string(R.string.error_offline)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.action_retry)).performClick()

        assertEquals(1, retries)
    }

    @Test
    fun forecastErrorIsSurfacedInUpcomingSectionWithRetry() {
        var retries = 0
        composeRule.setContent {
            CatsDogsTheme {
                CurrentWeatherScreen(
                    // Null coordinates keep RadarCard in its static branch (no animation loop).
                    locations = listOf(london),
                    activeIndex = 0,
                    weatherState = LoadingState.Success(weather()),
                    forecastState = LoadingState.Error("offline", canRetry = true),
                    onTabSelected = {},
                    onAddCityClick = {},
                    onRetry = { retries++ },
                )
            }
        }

        composeRule.onNodeWithText(string(R.string.section_upcoming)).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.action_retry)).performScrollTo().performClick()

        assertEquals(1, retries)
    }

    @Test
    fun forecastSuccessRendersUpcomingDay() {
        composeRule.setContent {
            CatsDogsTheme {
                CurrentWeatherScreen(
                    locations = listOf(london),
                    activeIndex = 0,
                    weatherState = LoadingState.Success(weather()),
                    forecastState = LoadingState.Success(listOf(dayForecast(dateLabel = "Mon, Jan 1"))),
                    onTabSelected = {},
                    onAddCityClick = {},
                    onRetry = {},
                )
            }
        }

        composeRule.onNodeWithText(string(R.string.section_upcoming)).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Mon, Jan 1").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun hourlyStripRendersWhenForecastHasHourlySlots() {
        composeRule.setContent {
            CatsDogsTheme {
                CurrentWeatherScreen(
                    locations = listOf(london),
                    activeIndex = 0,
                    weatherState = LoadingState.Success(weather()),
                    forecastState = LoadingState.Success(
                        listOf(
                            dayForecast(
                                dateLabel = "Mon, Jan 1",
                                hourlySlots = listOf(hourlySlot(timeLabel = "9 AM")),
                            ),
                        ),
                    ),
                    onTabSelected = {},
                    onAddCityClick = {},
                    onRetry = {},
                )
            }
        }

        composeRule.onNodeWithText(string(R.string.section_hourly)).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("9 AM").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun precipitationChanceIsShownInHourlyStrip() {
        composeRule.setContent {
            CatsDogsTheme {
                CurrentWeatherScreen(
                    locations = listOf(london),
                    activeIndex = 0,
                    weatherState = LoadingState.Success(weather()),
                    forecastState = LoadingState.Success(
                        listOf(
                            dayForecast(
                                dateLabel = "Mon, Jan 1",
                                hourlySlots = listOf(
                                    hourlySlot(timeLabel = "9 AM", precipitationChance = 80),
                                ),
                            ),
                        ),
                    ),
                    onTabSelected = {},
                    onAddCityClick = {},
                    onRetry = {},
                )
            }
        }

        composeRule.onNodeWithText("80%").performScrollTo().assertIsDisplayed()
    }

    private companion object {
        // No coordinates: keeps the radar tile static so the screen reaches idle for assertions.
        val london = SavedLocation(label = "London, GB", latitude = null, longitude = null)

        fun weather() =
            CurrentWeather(
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

        fun dayForecast(
            dateLabel: String,
            hourlySlots: List<HourlySlot> = emptyList(),
        ) = DayForecast(
            dateLabel = dateLabel,
            conditionMain = "Clouds",
            description = "cloudy",
            iconCode = "02d",
            temperature = 14.0,
            feelsLike = 13.0,
            tempMin = 10.0,
            tempMax = 16.0,
            units = WeatherUnits.METRIC,
            hourlySlots = hourlySlots,
        )

        fun hourlySlot(
            timeLabel: String,
            precipitationChance: Int = 0,
        ) = HourlySlot(
            timeLabel = timeLabel,
            iconCode = "02d",
            description = "Cloudy",
            temperature = 14.0,
            feelsLike = 13.0,
            windSpeed = 3.0,
            windDeg = 180,
            humidity = 60,
            pressure = 1012,
            units = WeatherUnits.METRIC,
            precipitationChance = precipitationChance,
        )
    }
}
