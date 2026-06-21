package com.tuckercr.catsdogs.worker

import androidx.work.ListenableWorker.Result
import com.tuckercr.catsdogs.domain.CurrentWeather
import com.tuckercr.catsdogs.domain.DayForecast
import com.tuckercr.catsdogs.domain.SavedLocation
import com.tuckercr.catsdogs.domain.WeatherUnits
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
            val logic = logic(savedLocations = emptyList(), postNotification = { t, b -> posted += t to b })

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
                postNotification = { t, b -> posted += t to b },
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
            val logic = logic(postNotification = { t, b -> posted += t to b })

            assertEquals(Result.success(), logic.doWork())
            assertEquals(1, posted.size)
        }

    @Test
    fun `returns success without posting when permission is denied`() =
        runTest {
            val posted = mutableListOf<Pair<String, String>>()
            val logic = logic(
                hasNotificationPermission = { false },
                postNotification = { t, b -> posted += t to b },
            )

            assertEquals(Result.success(), logic.doWork())
            assertEquals(emptyList<Pair<String, String>>(), posted)
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
                postNotification = { title, _ -> capturedTitle = title },
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
                postNotification = { _, body -> capturedBody = body },
            )

            logic.doWork()

            // Should use forecast values (99/45), not weather values (60/50)
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
                postNotification = { _, body -> capturedBody = body },
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
                postNotification = { _, body -> capturedBody = body },
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
                postNotification = { _, body -> capturedBody = body },
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
        postNotification: (String, String) -> Unit = { _, _ -> },
    ) = NotificationWorkerLogic(
        getSavedLocations = { savedLocations },
        getActiveIndex = { activeIndex },
        getUnits = { units },
        fetchCurrentWeather = fetchCurrentWeather,
        fetchForecast = fetchForecast,
        hasNotificationPermission = hasNotificationPermission,
        postNotification = postNotification,
    )

    private companion object {
        val london = SavedLocation(label = "My Location", latitude = 51.5, longitude = -0.1)
        val cityByName = SavedLocation(label = "Austin, TX", latitude = null, longitude = null)

        fun weather(
            cityName: String = "London",
            temperature: Double = 72.0,
            tempMin: Double = 60.0,
            tempMax: Double = 80.0,
            description: String = "clear sky",
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
        )

        fun forecast(
            tempMin: Double = 55.0,
            tempMax: Double = 85.0,
        ) = DayForecast(
            dateLabel = "Mon, Jan 1",
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
