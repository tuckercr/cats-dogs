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

class UpdateWorkerLogicTest {

    // --- result routing ---

    @Test
    fun `returns success immediately when no locations are saved`() =
        runTest {
            val cached = mutableListOf<String>()
            val logic = logic(savedLocations = emptyList(), cacheCurrentWeather = { k, _ -> cached += k })

            assertEquals(Result.success(), logic.doWork())
            assertEquals(emptyList<String>(), cached)
        }

    @Test
    fun `returns success when active index is out of bounds`() =
        runTest {
            val cached = mutableListOf<String>()
            val logic = logic(
                savedLocations = listOf(london),
                activeIndex = 5,
                cacheCurrentWeather = { k, _ -> cached += k },
            )

            assertEquals(Result.success(), logic.doWork())
            assertEquals(emptyList<String>(), cached)
        }

    @Test
    fun `returns success when only current weather succeeds`() =
        runTest {
            val logic = logic(
                fetchForecast = { _, _, _, _ -> kotlin.Result.failure(RuntimeException("offline")) },
            )

            assertEquals(Result.success(), logic.doWork())
        }

    @Test
    fun `returns success when only forecast succeeds`() =
        runTest {
            val logic = logic(
                fetchCurrentWeather = { _, _, _, _ -> kotlin.Result.failure(RuntimeException("offline")) },
            )

            assertEquals(Result.success(), logic.doWork())
        }

    @Test
    fun `returns retry when both fetches fail`() =
        runTest {
            val logic = logic(
                fetchCurrentWeather = { _, _, _, _ -> kotlin.Result.failure(RuntimeException("offline")) },
                fetchForecast = { _, _, _, _ -> kotlin.Result.failure(RuntimeException("offline")) },
            )

            assertEquals(Result.retry(), logic.doWork())
        }

    // --- caching ---

    @Test
    fun `caches current weather and forecast under the location cache key on success`() =
        runTest {
            val cachedWeather = mutableListOf<Pair<String, CurrentWeather>>()
            val cachedForecast = mutableListOf<Pair<String, List<DayForecast>>>()
            val logic = logic(
                savedLocations = listOf(london),
                cacheCurrentWeather = { k, w -> cachedWeather += k to w },
                cacheForecast = { k, f -> cachedForecast += k to f },
            )

            logic.doWork()

            assertEquals(listOf(london.cacheKey), cachedWeather.map { it.first })
            assertEquals(listOf(london.cacheKey), cachedForecast.map { it.first })
        }

    @Test
    fun `does not cache when a fetch fails`() =
        runTest {
            val cachedWeather = mutableListOf<String>()
            val cachedForecast = mutableListOf<String>()
            val logic = logic(
                fetchCurrentWeather = { _, _, _, _ -> kotlin.Result.failure(RuntimeException("offline")) },
                fetchForecast = { _, _, _, _ -> kotlin.Result.failure(RuntimeException("offline")) },
                cacheCurrentWeather = { k, _ -> cachedWeather += k },
                cacheForecast = { k, _ -> cachedForecast += k },
            )

            logic.doWork()

            assertEquals(emptyList<String>(), cachedWeather)
            assertEquals(emptyList<String>(), cachedForecast)
        }

    // --- units propagation (regression guard for the override bug) ---

    @Test
    fun `resolved units are passed to both the current and forecast fetches`() =
        runTest {
            // A US-locale device whose system default would resolve to IMPERIAL, but the user
            // picked METRIC: getUnits returns METRIC and that MUST reach both fetches, otherwise
            // the worker writes the wrong unit system into the cache.
            var currentUnits: WeatherUnits? = null
            var forecastUnits: WeatherUnits? = null
            val logic = logic(
                units = WeatherUnits.METRIC,
                fetchCurrentWeather = { units, _, _, _ ->
                    currentUnits = units
                    kotlin.Result.success(weather(units = units))
                },
                fetchForecast = { units, _, _, _ ->
                    forecastUnits = units
                    kotlin.Result.success(listOf(forecast(units = units)))
                },
            )

            logic.doWork()

            assertEquals(WeatherUnits.METRIC, currentUnits)
            assertEquals(WeatherUnits.METRIC, forecastUnits)
        }

    // --- fetch routing ---

    @Test
    fun `passes lat-lon to fetches when location has coordinates`() =
        runTest {
            var capturedLat: Double? = 99.0
            var capturedLon: Double? = 99.0
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
    fun `passes null lat-lon and label as query when location has no coordinates`() =
        runTest {
            var capturedLat: Double? = 99.0
            var capturedLon: Double? = 99.0
            var capturedLabel: String? = null
            val logic = logic(
                savedLocations = listOf(cityByName),
                fetchCurrentWeather = { _, label, lat, lon ->
                    capturedLat = lat
                    capturedLon = lon
                    capturedLabel = label
                    kotlin.Result.success(weather(cityName = "Austin"))
                },
            )

            logic.doWork()

            assertNull(capturedLat)
            assertNull(capturedLon)
            assertEquals(cityByName.label, capturedLabel)
        }

    @Test
    fun `refreshes the active location when several cities are saved`() =
        runTest {
            var capturedLabel: String? = null
            val logic = logic(
                savedLocations = listOf(cityByName, london),
                activeIndex = 1,
                fetchCurrentWeather = { _, label, _, _ ->
                    capturedLabel = label
                    kotlin.Result.success(weather())
                },
            )

            assertTrue(logic.doWork() == Result.success())
            assertEquals(london.label, capturedLabel)
        }

    // --- helpers ---

    private fun logic(
        savedLocations: List<SavedLocation> = listOf(london),
        activeIndex: Int = 0,
        units: WeatherUnits = WeatherUnits.IMPERIAL,
        fetchCurrentWeather: suspend (WeatherUnits, String, Double?, Double?) -> kotlin.Result<CurrentWeather> =
            { u, _, _, _ -> kotlin.Result.success(weather(units = u)) },
        fetchForecast: suspend (WeatherUnits, String, Double?, Double?) -> kotlin.Result<List<DayForecast>> =
            { u, _, _, _ -> kotlin.Result.success(listOf(forecast(units = u))) },
        cacheCurrentWeather: suspend (String, CurrentWeather) -> Unit = { _, _ -> },
        cacheForecast: suspend (String, List<DayForecast>) -> Unit = { _, _ -> },
    ) = UpdateWorkerLogic(
        getSavedLocations = { savedLocations },
        getActiveIndex = { activeIndex },
        getUnits = { units },
        fetchCurrentWeather = fetchCurrentWeather,
        fetchForecast = fetchForecast,
        cacheCurrentWeather = cacheCurrentWeather,
        cacheForecast = cacheForecast,
    )

    private companion object {
        val london = SavedLocation(label = "My Location", latitude = 51.5, longitude = -0.1)
        val cityByName = SavedLocation(label = "Austin, TX", latitude = null, longitude = null)

        fun weather(
            cityName: String = "London",
            units: WeatherUnits = WeatherUnits.IMPERIAL,
        ) = CurrentWeather(
            cityName = cityName,
            conditionMain = "Clear",
            description = "clear sky",
            iconCode = "01d",
            temperature = 72.0,
            feelsLike = 70.0,
            tempMin = 60.0,
            tempMax = 80.0,
            humidityPercent = 50,
            pressureHpa = 1013,
            windSpeed = 5.0,
            windDeg = 180,
            visibilityMeters = 10000,
            cloudPercent = 0,
            units = units,
        )

        fun forecast(units: WeatherUnits = WeatherUnits.IMPERIAL) =
            DayForecast(
                dateLabel = "Mon, Jan 1",
                conditionMain = "Clear",
                description = "clear sky",
                iconCode = "01d",
                temperature = 70.0,
                feelsLike = 68.0,
                tempMin = 55.0,
                tempMax = 85.0,
                units = units,
            )
    }
}
