# simple-weather
A simple weather app.

## Project Overview

A minimal Android weather app (Kotlin + Jetpack Compose + Material3) whose primary value is a home screen widget displaying the current temperature. Open-Meteo is used for weather and geocoding (no API key required).

## Releases

**Always tag after the PR merges on GitHub.** GitHub signs merge commits automatically, so tagging the merged commit produces a "Verified" release without any local GPG/SSH setup.

```bash
# After merging on GitHub:
git checkout main && git pull

# Stable release
git tag -a v1.0.0 -m "Release v1.0.0"
git push origin v1.0.0

# Pre-release (alpha/beta — triggers release workflow, marked as pre-release)
git tag -a v1.0.0-alpha -m "Pre-release v1.0.0-alpha"
git push origin v1.0.0-alpha
```

Pushing any `v*` tag triggers `.github/workflows/release.yml`, which builds, signs, and publishes the APK. Tags containing a hyphen (e.g. `-alpha`, `-beta`) are automatically marked as pre-releases.

## TODO
- [ ] Determine if location always allowed is necessary or if only while using is sufficient

## Wishlist (deferred features)

- [x] Tapping widget can launch the user's choice of weather app
- [x] Ability to lock in a typed setting (e.g., location or text color) by hitting Return/ Enter
- [ ] Widget: condition icon, high/low temperatures, font picker
- [ ] v2: Room + Hilt migration, temperature trend sparkline widget (12/24hr graph)
- [ ] Settings page follows device theme
- [ ] More guestures on widget (e.g., swipeUp can be set to launch a different app than tap)
- [ ] Advanced tap actions to launch specific app activity
- [ ] Minimally / cleanly support displaying even more data: humidity, wind speed or chill, UV, AQI, precip chance
- [ ] Make a stinkin app icon already
