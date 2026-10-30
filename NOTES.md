# Notes

Open ideas, decisions and loose ends for Sunny & Misty, both apps. The same file lives in both
repos (cats-dogs-iOS and cats-dogs); keep them in step. Dates are when the item came up (2026).
Tick items off or delete them once done.

## Do soon

- [x] **iOS signing** (10-30): FangJet LLC's team (`3V3NR8HP58`), your iPhone is registered, and a
      Release archive exports with App Store signing.
- [ ] **First TestFlight upload.** Create the app record in App Store Connect (`com.fangjet.weather`),
      then upload from Xcode's Organizer. Archive from `main` once the open PRs merge. (10-30)
- [ ] **Android location: "Ask every time" never clears the permanently-denied flag**, so
      "Add My Location" stays hidden after a reset in system settings (review finding 3). (10-29)
- [ ] **Storms in the dog walking line, and maybe the hero text.** Warn about thunder in the walk
      card ("Storms from 4 PM, walk before 3"), and possibly the pets' hero line. Check that both
      forecast providers give thunderstorm codes hourly. Should storm hours also be dropped from
      suggested walks, like bad air? (10-30)

## Air quality tile (built 10-30)

- [x] Bad air affects the walk planner (10-30): hours at "Unhealthy" or worse (US above 150,
      Europe's "Poor" and up) are never suggested, and the walk card says "Bad air. Potty breaks
      only." when it's bad now. Heat still outranks it.
- [ ] The walk reminder and morning briefing just skip bad-air hours; they don't say why. Add a
      line ("Smoky air today, so a short one")?
- [ ] Make the bad-air band a Remote Config threshold, like the pavement ones?
- [ ] The tile fetches air quality now, and the forecast fetches it hourly: two requests to the same
      service. Could the tile read the forecast's current hour instead?
- [ ] Add a Remote Config switch (`air_quality_enabled`) like the radar and walk card have?
- [ ] Review the copy without a dog ("Fresh air all round" … "Stay indoors") and with one
      ("Good air for a long walk" … "Stay in. Potty breaks only").
- [ ] The pollutant ("Mostly ozone") is only read out by VoiceOver and TalkBack: a fourth line made
      every tile in the grid taller. Show it somewhere else, such as a tap-through?
- [x] Five small tiles left Air quality alone on its row: an odd tile out now takes the full
      width (10-30).
- [ ] Not cached, so it doesn't show offline or until the first fetch returns.
- Decided: the scale follows the city (European index for cities on a European clock, US
  everywhere else); "Unhealthy for sensitive groups" is shortened to "Poor for some".

## More tiles to consider (10-30)

- Decided 10-30: no Storm watch tile (mocked up). Storms go in the walk line instead (Do soon).
- [ ] Pollen: free from Open-Meteo for Europe only. The US needs a paid provider (Google Pollen,
      Tomorrow.io, Ambee).
- [ ] A full UV tile (peak time, pale-nosed dogs). UV is only in the Daylight header now.
- [ ] Wind gusts in the Wind tile; sunshine hours; snow depth feeding the paw check.

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
- [ ] App Store screenshots (`screenshots/app-store/`) and `screenshots/settings.png` in both repos
      are out of date. (10-28)
- [ ] Website store buttons need the real App Store and Play links on launch day, ideally the
      official badges. (10-27)
- [ ] TestFlight build for Jessica. (10-24)
- [x] Update prompts (10-30): `ios_`/`android_min_supported_version` block the app until it's
      updated, `ios_`/`android_min_recommended_version` show a banner ("Not now" hides it a week).
- [x] The five keys are live in Remote Config, all empty (deployed 10-30).
- [ ] Set `ios_app_store_url` once the App Store record exists. Until then iOS has no Update button.
- [ ] Debug builds can show either prompt: launch iOS with `-updatePrompt required` (or
      `recommended`); on Android, `adb shell am start -n com.fangjet.weather.debug/com.fangjet.weather.MainActivity --es updatePrompt required`.
- [ ] Store screenshot captions in the pets' voice. (10-26)

## Testing

- [ ] Reverse case: My Location showing, then location permission revoked. (10-28)
- [ ] "Open Settings" from iOS notification and location settings on a real iPhone. (10-28)
- [ ] Listen to new spoken text with VoiceOver and TalkBack. (10-28)
- [ ] Check on a device: the Remote Config switches hiding notification types, the Walk time
      channel only appearing once you have a dog, and the 15-minute My Location check. (10-29)
- [ ] The half-hourly refresh while the app stays open (an overnight test). (10-26)

## Infrastructure

- [ ] The Android release build doesn't fail loudly when the signing key or weather API key is
      missing. (10-27)
- [x] iOS test runs left hundreds of `CatsDogsTests-*.plist` files on the test simulator (10-05).
      Tests now reuse 8 settings files (`CatsDogsTests-0` to `-7`); the system rewrites deleted ones,
      so zero would need in-memory settings. 60 old test-clone simulators (329 GB) deleted 10-30.
- [ ] After the first release: rename the Xcode project and targets ("Cats&Dogs") and the Android
      folder (`cats-dogs`) in their own PRs. (10-28)
- [ ] Rename the "Furcast-Shots" and "Furcast-iPad-Shots" simulators. (10-28)
- [x] Clean up old local branches (10-26, done 10-30).

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
