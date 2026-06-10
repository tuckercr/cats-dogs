# Cats & Dogs

![Android CI](https://github.com/tuckercr/cats-dogs/actions/workflows/android.yml/badge.svg)
![ktlint](https://github.com/tuckercr/cats-dogs/actions/workflows/ktlint.yml/badge.svg)

A clean, modern Android weather app. Search any city for current conditions — temperature,
feels-like, min/max, humidity, wind speed & direction, pressure, visibility, and cloud cover —
plus a 5-day forecast. Built with Jetpack Compose and Material 3.

Originally a coding-challenge project, the app has since been extended as a personal showcase of
Android best practices: clean architecture, unidirectional data flow, Hilt DI, and full CI/CD.

## Screenshots

> *Coming soon — run locally and screenshot the current-weather and forecast screens.*

## Getting started

1. Create a free API key at [openweathermap.org](https://openweathermap.org/api).
2. Add it to `local.properties` (never committed to source control):
   ```
   OWM_API_KEY=your_key_here
   ```
3. Build and run. New keys may take up to 2 hours to activate.

The key is injected at build time into `BuildConfig.OWM_API_KEY`. On GitHub it is stored as an
Actions secret and injected automatically — no key in the repository.

## Architecture & tech stack

| Layer | Choice | Notes |
|---|---|---|
| UI | Jetpack Compose + Material 3 | Single-Activity, screen-level composables |
| State | `StateFlow` + `collectAsStateWithLifecycle` | Unidirectional data flow |
| DI | Hilt | Repositories and ViewModels are injected |
| Navigation | Compose Navigation | Type-safe destination constants |
| Networking | Retrofit + OkHttp + kotlinx.serialization | Suspend functions, no RxJava |
| Images | Coil `AsyncImage` | Weather condition icons from OpenWeatherMap |
| Persistence | DataStore Preferences | Welcome-seen flag + last resolved city |
| API | OpenWeatherMap `/weather` & `/forecast` + Geocoding | Free tier |
| CI/CD | GitHub Actions | Build, ktlint, unit tests on every push |

## App flow

1. **Splash / loading** — checks `DataStore` for prior visit; routes to Welcome or Current Weather.
2. **Welcome screen** — shown on first launch; tap *Get started* to proceed.
3. **Current Weather** — type a city name; autocomplete suggestions appear via the
   [Geocoding API](https://openweathermap.org/api/geocoding-api). Selecting a suggestion pins
   exact latitude/longitude so the result is unambiguous.  
   Displays: condition icon, temperature (with daily min/max), feels-like, humidity, wind speed &
   direction, pressure, visibility, and cloud cover.
4. **5-Day Forecast** — reached via the *View 5-day forecast* button once weather loads.
   Uses `data/2.5/forecast` (free tier, 3-hour slots) with the same coordinates. Each calendar
   day shows the slot closest to local noon, plus the daily high/low derived from all slots.
5. **Persistence** — a successful fetch saves the location to `DataStore` so it prefills on
   next launch.
6. **Error handling** — network failures, HTTP errors, empty API keys, and malformed payloads all
   surface as inline error messages with *Retry* where appropriate.

## Unit tests

| Test class | What it covers |
|---|---|
| `ForecastAggregatorTest` | Noon-slot selection and multi-day grouping |
| `OpenWeatherParsingTest` | Kotlinx Serialization round-trips for API DTOs |
| `WeatherRepositoryTest` | Repository contract: coordinate routing, city-query trimming, error mapping |
| `WeatherUnitsTest` | Locale-to-unit resolution (Android 13 and earlier fallback) |

## Temperature units

On Android 14+ the app reads the system temperature preference. On earlier versions it falls back
to a locale-based heuristic (US → Fahrenheit, otherwise Metric). A production app would surface
an explicit user setting.

## Demo video

> Note: the file is too large to preview on GitHub — click *Raw* to download.

[![Watch the demo](./screen_recording/thumbnail.png)](./screen_recording/cats_and_dogs_demo_vid.mp4)
