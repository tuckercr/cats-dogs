# Furcast Weather

![Android CI](https://github.com/tuckercr/cats-dogs/actions/workflows/android.yml/badge.svg)
![ktlint](https://github.com/tuckercr/cats-dogs/actions/workflows/ktlint.yml/badge.svg)

A clean, modern Android weather app. Track current conditions, an hourly and multi-day forecast,
and an animated precipitation radar for any number of saved cities, get daily push briefings, and
switch between metric and imperial units. Built with Jetpack Compose and Material 3.

There is also a native iOS version of the same product, built in Swift and SwiftUI:
[tuckercr/cats-dogs-iOS](https://github.com/tuckercr/cats-dogs-iOS).

Originally a coding-challenge project, the app has since been extended as a personal showcase of
Android best practices: clean architecture, unidirectional data flow, Hilt DI, background work with
WorkManager, and full CI/CD.

## Features

- **Current conditions** for any city: temperature, feels-like, daily min/max, humidity, wind
  speed & direction, pressure, visibility, cloud cover, and sunrise/sunset.
- **Hourly strip** with the next ~36 hours: time, condition icon, temperature, and rain chance.
- **Multi-day forecast** shown inline, aggregated to the city's local-timezone days with tappable
  day details.
- **Rain probability** surfaced per hour and per day.
- **Animated radar** — a RainViewer precipitation loop (recent past plus short-range nowcast) over
  an OpenStreetMap base, with a timeline scrubber, colour legend, and play/pause.
- **Multiple saved cities** with a swipeable, tabbed switcher.
- **Current location** resolved with one tap via fused location services.
- **Local time everywhere** — forecast days, hourly slots, sunrise/sunset, and the radar all render
  in the selected city's timezone.
- **Daily notifications** — weather briefings delivered on a schedule via WorkManager.
- **Unit selection** — System, Metric, or Imperial, chosen in Settings.
- **Offline-first** — per-city caching paints instantly, then refreshes silently in the background,
  with cached-data fallback and inline retry when the network fails.

## Screenshots

| Pets, walk time & hourly | Radar & rest of week | Day detail | Settings & units |
|---|---|---|---|
| ![The cat and dog react to the weather, with best walk time and the hourly strip](./screenshots/current.png) | ![Animated RainViewer radar and the rest of the week](./screenshots/forecast.png) | ![Hour-by-hour detail for a day](./screenshots/day.png) | ![Settings with unit selection](./screenshots/settings.png) |

On tablets and in landscape, the pets sit beside today's details and the week splits into two columns:

![Phone in landscape with the pets beside today's details](./screenshots/landscape.png)

![Tablet layout with the pets beside today's details](./screenshots/tablet.png)

## Getting started

1. Create a free API key at [openweathermap.org](https://openweathermap.org/api).
2. Add it to `local.properties` (never committed to source control):
   ```
   OWM_API_KEY=your_key_here
   ```
3. Build and run. New keys may take up to 2 hours to activate.

The key is injected at build time into `BuildConfig.OWM_API_KEY`. On GitHub it is stored as an
Actions secret and injected automatically, so no key lives in the repository.

Firebase is optional: without `app/google-services.json` the app builds and runs normally, with
Remote Config falling back to its in-app defaults and analytics and crash reporting doing nothing.
See [Firebase setup](#firebase-setup) to wire it up.

Debug builds install as `com.tuckercr.catsdogs.debug` (version name suffixed `-debug`), so they sit
alongside the Play build and report to their own Firebase app.

## Architecture & tech stack

| Layer | Choice | Notes |
|---|---|---|
| UI | Jetpack Compose + Material 3 | Single-Activity, screen-level composables, light/dark |
| State | `StateFlow` + `collectAsStateWithLifecycle` | Unidirectional data flow |
| DI | Hilt | Repositories and ViewModels are injected |
| Navigation | Compose Navigation | Type-safe destination constants |
| Background work | WorkManager | Periodic weather refresh + daily notifications |
| Networking | Retrofit + OkHttp + kotlinx.serialization | Suspend functions, no RxJava |
| Radar | RainViewer + OpenStreetMap tiles | Animated precipitation loop stitched on a Compose `Canvas`; frame speed via Remote Config |
| Images | Coil | Weather condition icons and radar/basemap tiles |
| Persistence | DataStore Preferences | Onboarding flags, saved cities, active city, unit override, per-city cache |
| Analytics | Firebase Analytics, Remote Config, Crashlytics | Optional; no-op without `google-services.json`. No advertising ID; user opt-out and a remote kill switch |
| Modules | `:app`, `:weather-api` | API DTOs, parsing, and repositories split into `:weather-api` |
| Data sources | OpenWeatherMap `/weather` & `/forecast` + Geocoding; Open-Meteo hourly forecast; RainViewer radar frames | Free tiers; Open-Meteo and RainViewer need no key |
| CI/CD | GitHub Actions | ktlint, JVM unit tests, and Compose UI tests (emulator) on every push |

## App flow

1. **Loading** — reads onboarding state from `DataStore` and routes to the right screen.
2. **Onboarding** — first launch walks through Welcome, notification permission, and location
   permission. Existing installs are migrated so they are never re-prompted.
3. **Current Weather** — shows the active city's conditions, an hourly strip, an animated
   precipitation radar, and the upcoming days inline. Swipe between saved cities, or add one by name
   with autocomplete from the [Geocoding API](https://openweathermap.org/api/geocoding-api) (a
   selected suggestion pins exact latitude/longitude so the result is unambiguous).
4. **Forecast** — cities with coordinates use [Open-Meteo](https://open-meteo.com) (true hourly,
   with UV); cities without coordinates, or all cities when the `open_meteo_enabled` Remote Config
   switch is off, use OpenWeatherMap's `data/2.5/forecast` (free tier, 3-hour slots). Each calendar day shows the slot closest to local noon, plus the daily high/low and rain chance
   derived from all slots; tapping a day opens its full hourly breakdown.
5. **Radar** — RainViewer radar frames (recent past plus short-range nowcast) are stitched over an
   OpenStreetMap base and animated, with a scrubber and legend. All frame times display in the
   selected city's timezone.
6. **Settings** — choose temperature units (System / Metric / Imperial), manage saved locations,
   turn *Share usage statistics* off, and open the privacy policy.
7. **Notifications** — WorkManager delivers daily weather briefings and refreshes cached data in
   the background; the schedule survives reboots.
8. **Error handling** — network failures, HTTP errors, empty API keys, and malformed payloads all
   surface as inline error messages with *Retry* where appropriate.

## Analytics & privacy

Furcast Weather reports a handful of **anonymous app-usage events** — nothing about where you are,
which cities you save, or what the weather is. The whole catalogue is four functions in
[`AnalyticsRepository`](app/src/main/java/com/tuckercr/catsdogs/analytics/AnalyticsRepository.kt),
so the privacy policy can be checked against the code line by line.

| Event | Parameters | When |
|---|---|---|
| `onboarding_complete` | — | First-run onboarding finished |
| `city_added` | — | A city was saved (never which one) |
| `settings_opened` | — | Settings opened from the weather screen |
| `weather_error` | `error_key` = `offline` / `rate_limited` / `server_error` / `city_not_found` / `empty_query` / `missing_api_key` / `bad_api_key` / `generic` | A current-weather or forecast load failed — the category only, never coordinates or a city |

### Gates

An event is sent only if every gate passes:

1. **Remote Config kill switch** `analytics_enabled` — false drops every event, and turns Firebase
   Analytics collection off, on every install without an app update.
2. **The user's own opt-out** — Settings → About → *Share usage statistics* (on by default). It is
   enforced at the SDK level via `setAnalyticsCollectionEnabled`, at app start and whenever it
   changes, so Firebase's automatic events (`first_open`, sessions, screen views) stop too — not
   just ours. Flipping it is deliberately not itself tracked.
3. **Firebase configured** — without `google-services.json` there is no Firebase app and nothing is
   sent.

Debug builds do send, but to their own `com.tuckercr.catsdogs.debug` app stream, so development
never pollutes the production numbers. Every analytics call is wrapped so it can never crash the
app; a failure just drops the event.

**No advertising ID.** The app shows no ads, so the manifest strips the `AD_ID` and
`ACCESS_ADSERVICES_*` permissions that `firebase-analytics` merges in, and turns off ad-ID
collection and ad-personalisation signals. Analytics keeps working on its app-instance ID.

Crashlytics is enabled in release builds only (off in debug).

## Remote Config

Server-tunable values, so behaviour can change without shipping an update. The app never blocks on
a fetch: it starts from the in-app defaults in
[`remote_config_defaults.xml`](app/src/main/res/xml/remote_config_defaults.xml), reads the last
activated values on cold start, and refreshes in the background at most hourly. Every read is
validated — a missing or bad value falls back to the default rather than crashing.

| Key | Type | Default | What it does |
|---|---|---|---|
| `weather_refresh_interval_minutes` | number | `30` | Background weather refresh interval (WorkManager minimum is 15) |
| `radar_frame_interval_ms` | number | `850` | Delay between radar animation frames; outside 100–5000 falls back to 850 |
| `radar_enabled` | boolean | `true` | Radar kill switch — false hides the radar section (header and card, phone and tablet layouts) and skips the RainViewer timeline and tile requests, for when RainViewer or the OpenStreetMap tiles become unavailable |
| `walk_card_enabled` | boolean | `true` | Shows the "time to walk the dog" card; false hides it on the current-weather screen (phone and tablet layouts) |
| `open_meteo_enabled` | boolean | `true` | Forecasts for cities with coordinates come from Open-Meteo; false falls back to the OpenWeatherMap 3-hourly forecast by lat/lon (foreground, background refresh, and notifications) |
| `daily_weather_notifications_enabled` | boolean | `true` | Posts the daily 9 / 13 / 19 weather-summary notifications; false skips posting (the scheduled work still runs). Other notification types will get their own switches |
| `foreground_refresh_min_minutes` | number | `0` | Skips the silent refresh on returning to the foreground if that city's weather (or forecast, tracked separately) was fetched within this many minutes in this session; outside 0–1440 falls back to 0, which always refreshes. Pull-to-refresh, retry and switching cities are never throttled |
| `walk_hot_pavement_c` | number | `52` | Pavement °C at or above which the walk rating is *Paws hot*; valid 40–70 |
| `walk_max_rain_chance` | number | `30` | An hour is walkable only below this rain chance (%); valid 0–100 |
| `walk_comfort_min_c` | number | `3` | Coldest comfortable walking temperature (°C); valid -20–20, and below `walk_comfort_max_c` |
| `walk_comfort_max_c` | number | `27` | Warmest comfortable walking temperature (°C); valid 15–40, and above `walk_comfort_min_c` |
| `walk_hours_start` | number | `7` | First local hour a walk may be suggested (inclusive); valid 0–23, and before `walk_hours_end` |
| `walk_hours_end` | number | `20` | Last local hour a walk may be suggested (inclusive); valid 0–23, and after `walk_hours_start` |
| `pet_hot_threshold_c` | number | `30` | At or above this °C the pets look hot and the walk rating is *Paws hot*; valid 20–45, and above `pet_cold_threshold_c` |
| `pet_cold_threshold_c` | number | `0` | At or below this °C the pets look cold and the walk rating is *Too cold*; valid -20–10, and below `pet_hot_threshold_c` |
| `analytics_enabled` | boolean | `true` | Analytics kill switch — see [Gates](#gates) |

The walk and pet-mood thresholds are always Celsius, whatever units the user picks. A value outside
its valid range falls back to its default, and so does any pair that isn't strictly ordered (comfort
min/max, walk hours start/end, pet cold/hot) — both values of the pair revert together.

[`firebase/remoteconfig.template.json`](firebase/remoteconfig.template.json) is the source of truth
for the console parameters.

## Firebase setup

The app is wired to the Firebase project **`cats-dogs-weather`** (Analytics, Remote Config,
Crashlytics). To build against it, or your own:

1. Create a project at [console.firebase.google.com](https://console.firebase.google.com) **with
   Google Analytics enabled** — events silently no-op if the project has no linked Analytics
   property.
2. Register **two** Android apps under it: `com.tuckercr.catsdogs` and `com.tuckercr.catsdogs.debug`.
   Debug builds use an `applicationIdSuffix`, and the `google-services` plugin matches package
   names exactly.
3. Download the merged `google-services.json` to `app/`. It is git-ignored — each developer
   supplies their own. CI restores it from the base64-encoded `GOOGLE_SERVICES_JSON` Actions secret
   when that is set, and builds without Firebase when it isn't.
4. Deploy the Remote Config parameters from this repo (the default project is set in
   [`.firebaserc`](.firebaserc)):
   ```bash
   firebase deploy --only remoteconfig
   ```
   `firebase remoteconfig:get` shows what is live. See [`firebase/README.md`](firebase/README.md).
5. In Analytics settings, set data retention to **14 months**.

## Unit tests

| Test class | What it covers |
|---|---|
| `ForecastAggregatorTest` | Noon-slot selection and multi-day grouping |
| `OpenWeatherParsingTest` | Kotlinx Serialization round-trips for API DTOs |
| `WeatherRepositoryTest` | Repository contract: coordinate routing (incl. `open_meteo_enabled` on/off), city-query trimming, error mapping |
| `WeatherUnitsTest` | Locale-to-unit resolution (Android 13 and earlier fallback) |
| `WeatherForecastViewModelTest` | Cache-first loading, refresh races, and background-refresh staleness guards |
| `RadarViewModelTest` | `radar_enabled` gating of the radar timeline fetch |
| `SettingsViewModelTest` | Unit-override selection, *Share usage statistics*, and cache clearing |
| `AnalyticsRepositoryTest` | Analytics gates: kill switch, user opt-out, both on, and never crashing |
| `AnalyticsCollectionControllerTest` | SDK collection flag follows the opt-out and kill switch, at start and on change |
| `WelcomeViewModelTest` | Onboarding state and persistence |
| `CityListViewModelTest` | Add, remove, reorder, and active-city tracking |
| `RadarRepositoryTest` | RainViewer timeline parsing, past/nowcast frame ordering, and tile URLs |
| `NotificationWorkerLogicTest` | Notification content and city-timezone "today" matching |
| `UpdateWorkerLogicTest` | Background refresh result routing, caching, and unit-override handling |

Compose UI tests live in `app/src/androidTest` (Welcome, onboarding, and the current-weather
screen states) and run on an emulator in CI via a separate `instrumented-tests` job.

## Temperature units

Users choose their preferred units in Settings: **System**, **Metric**, or **Imperial**. When set
to System, the app reads the Android 14+ temperature preference where available, and otherwise falls
back to a locale-based heuristic (US to Fahrenheit, otherwise Metric).
