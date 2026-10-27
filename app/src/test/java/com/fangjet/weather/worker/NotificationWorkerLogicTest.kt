package com.fangjet.weather.worker

import androidx.work.ListenableWorker.Result
import com.fangjet.weather.domain.CurrentWeather
import com.fangjet.weather.domain.DayForecast
import com.fangjet.weather.domain.SavedLocation
import com.fangjet.weather.domain.WeatherUnits
import com.fangjet.weather.ui.pets.PetMood
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationWorkerLogicTest {

    // --- result routing ---

    @Test
    fun `returns success immediately when no locations are saved`() =
        runTest {
            val posted = mutableListOf<Pair<String, String>>()
            val logic = logic(savedLocations = emptyList(), postNotification = { posted += it.title to it.body })

            val result = logic.doWork()

            assertEquals(Result.success(), result)
            assertEquals(emptyList<Pair<String, String>>(), posted)
        }

    @Test
    fun `returns success when active index is out of bounds`() =
        runTest {
            val posted = mutableListOf<Pair<String, String>>()
            val logic = logic(
                savedLocations = listOf(london),
                activeIndex = 5,
                postNotification = { posted += it.title to it.body },
            )

            assertEquals(Result.success(), logic.doWork())
            assertEquals(emptyList<Pair<String, String>>(), posted)
        }

    @Test
    fun `returns retry when weather fetch fails`() =
        runTest {
            val logic = logic(
                fetchCurrentWeather = { _, _, _, _ -> kotlin.Result.failure(RuntimeException("offline")) },
            )

            assertEquals(Result.retry(), logic.doWork())
        }

    @Test
    fun `returns success and posts notification when weather fetch succeeds`() =
        runTest {
            val posted = mutableListOf<Pair<String, String>>()
            val logic = logic(postNotification = { posted += it.title to it.body })

            assertEquals(Result.success(), logic.doWork())
            assertEquals(1, posted.size)
        }

    @Test
    fun `returns success without posting when permission is denied`() =
        runTest {
            val posted = mutableListOf<Pair<String, String>>()
            val logic = logic(
                hasNotificationPermission = { false },
                postNotification = { posted += it.title to it.body },
            )

            assertEquals(Result.success(), logic.doWork())
            assertEquals(emptyList<Pair<String, String>>(), posted)
        }

    // --- daily_weather_notifications_enabled ---

    @Test
    fun `daily weather notifications switched off posts nothing but still fetches and succeeds`() =
        runTest {
            val posted = mutableListOf<Pair<String, String>>()
            var fetches = 0
            val logic = logic(
                fetchCurrentWeather = { _, _, _, _ ->
                    fetches++
                    kotlin.Result.success(weather())
                },
                postNotification = { posted += it.title to it.body },
                isDailyWeatherNotificationEnabled = { false },
            )

            assertEquals(Result.success(), logic.doWork())
            assertEquals(emptyList<Pair<String, String>>(), posted)
            assertEquals(1, fetches)
        }

    @Test
    fun `daily weather notifications switched on posts as before`() =
        runTest {
            val posted = mutableListOf<Pair<String, String>>()
            val logic = logic(
                postNotification = { posted += it.title to it.body },
                isDailyWeatherNotificationEnabled = { true },
            )

            assertEquals(Result.success(), logic.doWork())
            assertEquals(1, posted.size)
        }

    @Test
    fun `a failing daily weather notifications switch falls back to posting`() =
        runTest {
            val posted = mutableListOf<Pair<String, String>>()
            val logic = logic(
                postNotification = { posted += it.title to it.body },
                isDailyWeatherNotificationEnabled = { throw IllegalStateException("Remote Config unavailable") },
            )

            assertEquals(Result.success(), logic.doWork())
            assertEquals(1, posted.size)
        }

    // --- fetch routing ---

    @Test
    fun `passes lat-lon and null label to fetch when location has coordinates`() =
        runTest {
            var capturedLat: Double? = "not-called".hashCode().toDouble()
            var capturedLon: Double? = "not-called".hashCode().toDouble()
            val logic = logic(
                savedLocations = listOf(london),
                fetchCurrentWeather = { _, _, lat, lon ->
                    capturedLat = lat
                    capturedLon = lon
                    kotlin.Result.success(weather())
                },
            )

            logic.doWork()

            assertEquals(london.latitude, capturedLat)
            assertEquals(london.longitude, capturedLon)
        }

    @Test
    fun `passes null lat-lon when location has no coordinates`() =
        runTest {
            var capturedLat: Double? = 99.0
            var capturedLon: Double? = 99.0
            val logic = logic(
                savedLocations = listOf(cityByName),
                fetchCurrentWeather = { _, _, lat, lon ->
                    capturedLat = lat
                    capturedLon = lon
                    kotlin.Result.success(weather(cityName = "Austin"))
                },
            )

            logic.doWork()

            assertNull(capturedLat)
            assertNull(capturedLon)
        }

    @Test
    fun `passes location label as query when location has no coordinates`() =
        runTest {
            var capturedLabel: String? = null
            val logic = logic(
                savedLocations = listOf(cityByName),
                fetchCurrentWeather = { _, label, _, _ ->
                    capturedLabel = label
                    kotlin.Result.success(weather(cityName = "Austin"))
                },
            )

            logic.doWork()

            assertEquals(cityByName.label, capturedLabel)
        }

    // --- notification content ---

    @Test
    fun `notification title uses current temperature and API city name`() =
        runTest {
            var capturedTitle: String? = null
            val logic = logic(
                fetchCurrentWeather = { _, _, _, _ ->
                    kotlin.Result.success(weather(temperature = 86.4, cityName = "London, GB"))
                },
                postNotification = { capturedTitle = it.title },
            )

            logic.doWork()

            assertEquals("86° in London, GB", capturedTitle)
        }

    @Test
    fun `notification body uses forecast high-low when forecast is available`() =
        runTest {
            var capturedBody: String? = null
            val logic = logic(
                fetchCurrentWeather = { _, _, _, _ ->
                    kotlin.Result.success(weather(tempMin = 50.0, tempMax = 60.0))
                },
                fetchForecast = { _, _, _, _ ->
                    kotlin.Result.success(listOf(forecast(tempMin = 45.0, tempMax = 99.0)))
                },
                postNotification = { capturedBody = it.body },
            )

            logic.doWork()

            // Should use forecast values (99/45), not weather values (60/50)
            assertTrue("body was: $capturedBody", capturedBody?.startsWith("99°/45°") == true)
        }

    @Test
    fun `notification body ignores a forecast whose first day is tomorrow`() =
        runTest {
            // Late in the evening /forecast has no slots left for today, so its first day is tomorrow.
            var capturedBody: String? = null
            val logic = logic(
                fetchCurrentWeather = { _, _, _, _ ->
                    kotlin.Result.success(weather(tempMin = 50.0, tempMax = 60.0))
                },
                fetchForecast = { _, _, _, _ ->
                    kotlin.Result.success(listOf(forecast(tempMin = 45.0, tempMax = 99.0, dateLabel = "Tue, Jan 2")))
                },
                postNotification = { capturedBody = it.body },
            )

            logic.doWork()

            assertTrue("body was: $capturedBody", capturedBody?.startsWith("60°/50°") == true)
        }

    @Test
    fun `notification body uses today's entry even when it is not first`() =
        runTest {
            var capturedBody: String? = null
            val logic = logic(
                fetchForecast = { _, _, _, _ ->
                    kotlin.Result.success(
                        listOf(
                            forecast(tempMin = 10.0, tempMax = 20.0, dateLabel = "Sun, Dec 31"),
                            forecast(tempMin = 45.0, tempMax = 99.0),
                        ),
                    )
                },
                postNotification = { capturedBody = it.body },
            )

            logic.doWork()

            assertTrue("body was: $capturedBody", capturedBody?.startsWith("99°/45°") == true)
        }

    @Test
    fun `notification body falls back to current weather temps when forecast fails`() =
        runTest {
            var capturedBody: String? = null
            val logic = logic(
                fetchCurrentWeather = { _, _, _, _ ->
                    kotlin.Result.success(weather(tempMin = 50.0, tempMax = 99.0))
                },
                fetchForecast = { _, _, _, _ -> kotlin.Result.failure(RuntimeException("offline")) },
                postNotification = { capturedBody = it.body },
            )

            logic.doWork()

            assertTrue("body was: $capturedBody", capturedBody?.startsWith("99°/50°") == true)
        }

    @Test
    fun `notification body description is title-cased`() =
        runTest {
            var capturedBody: String? = null
            val logic = logic(
                fetchCurrentWeather = { _, _, _, _ ->
                    kotlin.Result.success(weather(description = "overcast clouds"))
                },
                postNotification = { capturedBody = it.body },
            )

            logic.doWork()

            assertTrue("body was: $capturedBody", capturedBody?.contains("Overcast Clouds") == true)
        }

    @Test
    fun `notification body format is high-low-bullet-description`() =
        runTest {
            var capturedBody: String? = null
            val logic = logic(
                fetchCurrentWeather = { _, _, _, _ ->
                    kotlin.Result.success(weather(tempMin = 50.0, tempMax = 99.0, description = "clear sky"))
                },
                fetchForecast = { _, _, _, _ -> kotlin.Result.failure(RuntimeException()) },
                postNotification = { capturedBody = it.body },
            )

            logic.doWork()

            assertEquals("99°/50° • Clear Sky", capturedBody)
        }

    // --- buildNotificationContent (pure formatter) ---

    @Test
    fun `buildNotificationContent uses forecast temps when forecast provided`() {
        val (_, body) = buildNotificationContent(
            weather = weather(temperature = 72.0, tempMin = 55.0, tempMax = 80.0, description = "light rain"),
            todayForecast = forecast(tempMin = 48.0, tempMax = 91.0),
        )
        assertEquals("91°/48° • Light Rain", body)
    }

    @Test
    fun `buildNotificationContent uses weather temps when forecast is null`() {
        val (_, body) = buildNotificationContent(
            weather = weather(temperature = 72.0, tempMin = 55.0, tempMax = 80.0, description = "few clouds"),
            todayForecast = null,
        )
        assertEquals("80°/55° • Few Clouds", body)
    }

    @Test
    fun `buildNotificationContent rounds temperature in title`() {
        val (title, _) = buildNotificationContent(
            weather = weather(temperature = 85.6, cityName = "Tokyo"),
            todayForecast = null,
        )
        assertEquals("86° in Tokyo", title)
    }

    @Test
    fun `today matching uses the city's utc offset`() =
        runTest {
            var capturedOffset: Int? = null
            val logic = NotificationWorkerLogic(
                getSavedLocations = { listOf(london) },
                getActiveIndex = { 0 },
                getUnits = { WeatherUnits.IMPERIAL },
                fetchCurrentWeather = { _, _, _, _ ->
                    kotlin.Result.success(weather(utcOffsetSeconds = -21600))
                },
                fetchForecast = { _, _, _, _ -> kotlin.Result.success(listOf(forecast())) },
                hasNotificationPermission = { true },
                postNotification = {},
                todayLabel = { offset ->
                    capturedOffset = offset
                    TODAY_LABEL
                },
            )

            logic.doWork()

            // The city's offset (Mountain Daylight, UTC-6h) is used to resolve "today", not the device's.
            assertEquals(-21600, capturedOffset)
        }

    @Test
    fun `the notification carries the pets' mood and hemisphere for its picture`() =
        runTest {
            val posted = mutableListOf<WeatherNotification>()
            // 72 °F and clear in the daytime: the sunny scene.
            logic(postNotification = { posted += it }).doWork()
            logic(
                savedLocations = listOf(SavedLocation(label = "Sydney", latitude = -33.9, longitude = 151.2)),
                fetchCurrentWeather = { _, _, _, _ -> kotlin.Result.success(weather().copy(iconCode = "01n")) },
                postNotification = { posted += it },
            ).doWork()

            assertEquals(PetMood.SUNNY, posted[0].mood)
            assertEquals(false, posted[0].southernHemisphere)
            assertEquals(PetMood.NIGHT, posted[1].mood)
            assertEquals(true, posted[1].southernHemisphere)
        }

    @Test
    fun `my location asks for the place's own name, a saved city keeps its label`() =
        runTest {
            val labels = mutableListOf<String>()
            val fetch: suspend (WeatherUnits, String, Double?, Double?) -> kotlin.Result<CurrentWeather> =
                { _, label, _, _ ->
                    labels += label
                    kotlin.Result.success(weather())
                }
            val here = SavedLocation(label = "My Location", latitude = 35.96, longitude = -83.92, isCurrentLocation = true)
            logic(savedLocations = listOf(here), fetchCurrentWeather = fetch).doWork()
            logic(savedLocations = listOf(cityByName), fetchCurrentWeather = fetch).doWork()

            assertEquals(listOf("", "Austin, TX"), labels)
        }

    // --- helpers ---

    private fun logic(
        savedLocations: List<SavedLocation> = listOf(london),
        activeIndex: Int = 0,
        units: WeatherUnits = WeatherUnits.IMPERIAL,
        fetchCurrentWeather: suspend (WeatherUnits, String, Double?, Double?) -> kotlin.Result<CurrentWeather> =
            { _, _, _, _ -> kotlin.Result.success(weather()) },
        fetchForecast: suspend (WeatherUnits, String, Double?, Double?) -> kotlin.Result<List<DayForecast>> =
            { _, _, _, _ -> kotlin.Result.success(listOf(forecast())) },
        hasNotificationPermission: () -> Boolean = { true },
        postNotification: (WeatherNotification) -> Unit = {},
        todayLabel: String = TODAY_LABEL,
        isDailyWeatherNotificationEnabled: () -> Boolean = { true },
    ) = NotificationWorkerLogic(
        getSavedLocations = { savedLocations },
        getActiveIndex = { activeIndex },
        getUnits = { units },
        fetchCurrentWeather = fetchCurrentWeather,
        fetchForecast = fetchForecast,
        hasNotificationPermission = hasNotificationPermission,
        postNotification = postNotification,
        isDailyWeatherNotificationEnabled = isDailyWeatherNotificationEnabled,
        todayLabel = { _ -> todayLabel },
    )

    private companion object {
        const val TODAY_LABEL = "Mon, Jan 1"
        val london = SavedLocation(label = "My Location", latitude = 51.5, longitude = -0.1)
        val cityByName = SavedLocation(label = "Austin, TX", latitude = null, longitude = null)

        fun weather(
            cityName: String = "London",
            temperature: Double = 72.0,
            tempMin: Double = 60.0,
            tempMax: Double = 80.0,
            description: String = "clear sky",
            utcOffsetSeconds: Int = 0,
        ) = CurrentWeather(
            cityName = cityName,
            conditionMain = "Clear",
            description = description,
            iconCode = "01d",
            temperature = temperature,
            feelsLike = temperature - 2,
            tempMin = tempMin,
            tempMax = tempMax,
            humidityPercent = 50,
            pressureHpa = 1013,
            windSpeed = 5.0,
            windDeg = 180,
            visibilityMeters = 10000,
            cloudPercent = 0,
            units = WeatherUnits.IMPERIAL,
            utcOffsetSeconds = utcOffsetSeconds,
        )

        fun forecast(
            tempMin: Double = 55.0,
            tempMax: Double = 85.0,
            dateLabel: String = TODAY_LABEL,
        ) = DayForecast(
            dateLabel = dateLabel,
            conditionMain = "Clear",
            description = "clear sky",
            iconCode = "01d",
            temperature = 70.0,
            feelsLike = 68.0,
            tempMin = tempMin,
            tempMax = tempMax,
            units = WeatherUnits.IMPERIAL,
        )
    }
}
