# Notes

Open ideas, decisions and loose ends for Sunny & Misty, both apps. The same file lives in both
repos (cats-dogs-iOS and cats-dogs); keep them in step. Dates are when the item came up (2026).
Finished items move to Done at the bottom.

## Open PRs (10-30)

Merge in this order; each stacked PR moves onto main or master once the one below it merges.

- iOS: #34 tile gaps and empty state, then #35 air quality; #36 Xcode settings and the
  `nilIfBlank` fix stands alone.
- Android: #94 tile gaps, then #95 air quality; #96 release build checks stands alone.

## Do soon

- [ ] **First TestFlight upload**, also the build for Jessica. Create the app record in App Store
      Connect (`com.fangjet.weather`), archive from `main` once the open PRs merge, and upload
      from Xcode's Organizer. (10-24)
- [ ] **Storms in the dog walking line, and maybe the hero text.** Warn about thunder in the walk
      card ("Storms from 4 PM, walk before 3"), and possibly the pets' hero line. Check that both
      forecast providers give thunderstorm codes hourly. Should storm hours also be dropped from
      suggested walks, like bad air? (10-30)
- [ ] **Android location: "Ask every time" never clears the permanently-denied flag**, so
      "Add My Location" stays hidden after a reset in system settings (review finding 3). (10-29)

## Air quality

- [ ] The walk reminder and morning briefing just skip bad-air hours; they don't say why. Add a
      line ("Smoky air today, so a short one")? (10-30)
- [ ] Review the copy without a dog ("Fresh air all round" … "Stay indoors") and with one
      ("Good air for a long walk" … "Stay in. Potty breaks only"). (10-30)
- [ ] The pollutant ("Mostly ozone") is only read out by VoiceOver and TalkBack: a fourth line made
      every tile in the grid taller. Show it somewhere else, such as a tap-through? (10-30)
- Decided: the scale follows the city (European index for cities on a European clock, US
  everywhere else); "Unhealthy for sensitive groups" is shortened to "Poor for some".
- Remote Config: `air_quality_enabled` (on) and `walk_bad_air_band` (3, valid 1–5) are live.

## More tiles to consider (10-30)

- [ ] Pollen: free from Open-Meteo for Europe only. The US needs a paid provider (Google Pollen,
      Tomorrow.io, Ambee).
- [ ] A full UV tile (peak time, pale-nosed dogs). UV is only in the Daylight header now.
- [ ] Wind gusts in the Wind tile; sunshine hours; snow depth feeding the paw check.
- Decided: no Storm watch tile (mocked up 10-30). Storms go in the walk line instead (Do soon).

## Notifications

- [ ] Captions like "Perfect walkies weather!" still show without a dog. You said to leave the dog
      walking strings for now. (10-28)
- [ ] Let people choose the morning briefing time (fixed at 8:30 today). (10-26)
- [ ] iOS stack summary line ("3 more briefings") via `summaryArgument`. Probably not needed. (10-29)

## Location

- [ ] Android Settings: the location row could ask once more in the app before sending people to
      system settings (Android allows two prompts). (10-28)

## Walk planner and pet safety

- [ ] Per-pet thresholds: small, short-haired and flat-faced dogs struggle sooner. (10-28)
- [ ] Strong wind isn't considered for the night "stroll" caption. (10-26)
- [ ] Retune the pavement heat model after an outdoor check of your own. (10-27)

## Store and release

- [ ] Licences: RainViewer is personal and educational use only, and Open-Meteo's free tier is
      non-commercial. Emails were drafted on 10-24; no replies yet. Fallback: launch with
      `radar_enabled` / `open_meteo_enabled` off in Remote Config.
- [ ] Future radar: RainViewer's free feed has had no forecast frames since 1 Jan. A paid layer
      (Tomorrow.io, OpenWeather maps, Xweather, MapTiler) hasn't been researched. (10-28)
- [ ] OpenStreetMap tile policy: at scale, a commercial tile provider. (10-24)
- [ ] The OpenWeather key ships in the app: watch usage on the dashboard, rotate via Remote Config
      if abused. (10-24)
- [ ] Set `ios_app_store_url` in Remote Config once the App Store record exists. Until then the
      iOS update screen has no Update button. (10-30)
- [ ] App Store screenshots (`screenshots/app-store/`) and `screenshots/settings.png` in both repos
      are out of date. (10-28)
- [ ] Store screenshot captions in the pets' voice. (10-26)
- [ ] Website store buttons need the real App Store and Play links on launch day, ideally the
      official badges. (10-27)
- Debug builds can show either update prompt: launch iOS with `-updatePrompt required` (or
  `recommended`); on Android,
  `adb shell am start -n com.fangjet.weather.debug/com.fangjet.weather.MainActivity --es updatePrompt required`.

## Small fixes found while taking screenshots (10-30)

- [ ] City search shows the same suggestion twice ("Amsterdam, North Holland, NL"). Drop duplicates.
- [ ] The day detail sheet has a thin empty row between the day's header and 12 AM.
- [ ] On the day clocks fall back, the day detail lists "1 AM" twice. That's right, but the
      second could say so (for example "1 AM PST").
- [ ] My Location shows the reverse-geocoded neighbourhood ("Horinouchi" for central Tokyo), not
      the city. Prefer the city name?

## Testing

- [ ] On an Android device: the full-width odd tile and the Air quality tile now read from the
      forecast (both only checked on iOS so far). (10-30)
- [ ] Reverse case: My Location showing, then location permission revoked. (10-28)
- [ ] "Open Settings" from iOS notification and location settings on a real iPhone. (10-28)
- [ ] Listen to new spoken text with VoiceOver and TalkBack. (10-28)
- [ ] Check on a device: the Remote Config switches hiding notification types, the Walk time
      channel only appearing once you have a dog, and the 15-minute My Location check. (10-29)
- [ ] The half-hourly refresh while the app stays open (an overnight test). (10-26)

## Infrastructure

- [ ] After the first release: rename the Xcode project and targets ("Cats&Dogs") and the Android
      folder (`cats-dogs`) in their own PRs. (10-28)
- [ ] Rename the "Furcast-Shots" and "Furcast-iPad-Shots" simulators. (10-28)

## Ideas

- [ ] Home screen widget: the pets reacting to the weather, plus the next walk time. (10-26)
- [ ] Small touches: tap the pets (Sunny wags, Misty ignores you), seasonal scenes, a "Sunny's
      take / Misty's take" line in the day detail. (10-26)
- [ ] Sunny's pronoun is still undecided; the copy avoids needing one. (10-26)
- [ ] Kotlin Multiplatform, as a learning project. Low priority. (09-24)

## Outside the apps

- [ ] Résumé PDF still links the old repo. (10-26)
- [ ] fangjet.com: the old WordPress install is no longer served (10-30), but its files and MySQL
      database may still sit in the IONOS webspace. Back up and delete them, leaving Rolo alone.
      Also tidy the Tailwind one-off values; the AI page is out of the navigation but still in the
      sitemap. (10-24)

## Done

- [x] iOS signing: FangJet LLC's team (`3V3NR8HP58`), your iPhone registered, and a Release archive
      that exports with App Store signing. (10-30)
- [x] Update prompts: `ios_`/`android_min_supported_version` block the app until it's updated,
      `ios_`/`android_min_recommended_version` show a banner ("Not now" hides it a week). The keys
      are live in Remote Config, all empty. (10-30)
- [x] Air quality tile, and bad air in the walk planner: hours at "Unhealthy" or worse are never
      suggested, and the walk card says "Bad air. Potty breaks only." Heat still outranks it. (10-30)
- [x] Air quality in one request: the forecast fetches each hour's full reading and the tile shows
      the hour in progress, so it's cached and shows offline. (10-30)
- [x] Remote Config: `air_quality_enabled` switch and `walk_bad_air_band` threshold. (10-30)
- [x] An odd tile out (Air quality, with Paw check showing) takes the full width. (10-30)
- [x] iOS "No cities added yet" fills the screen instead of showing as a box in dark mode. (10-30)
- [x] The Android release build stops at the start and lists what's missing when the signing key
      or weather API key isn't in `local.properties`. (10-27, done 10-30)
- [x] iOS test runs left hundreds of `CatsDogsTests-*.plist` files; tests now reuse 8. 60 old
      test-clone simulators (329 GB) deleted. (10-05, done 10-30)
- [x] Xcode 27.1 recommended settings, and the `nilIfBlank` isolation error. (10-30)
- [x] Old local and merged branches cleaned up. (10-26, done 10-30)
- [x] The hero's high and low came from OpenWeather's current reading (the spread across nearby
      stations right now) and today's forecast only covered the hours left. Both now show the
      whole day from the forecast. (10-30)
- [x] Fresh README, App Store and Play Store screenshots. (10-30)
