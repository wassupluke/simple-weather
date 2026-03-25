# simple-weather
A simple weather app.

## Project Overview

A minimal Android weather app (Kotlin + Jetpack Compose + Material3) whose primary value is a home screen widget displaying the current temperature. Open-Meteo is used for weather and geocoding (no API key required).

## Releases

For a release:

```
git tag -a v0.0.1 -m "Initial release"
git push origin v0.0.1
```

For a pre-release:

```
git tag -a v0.0.1-alpha -m "Initial release"
git push origin v0.0.1-alpha
```

## Wishlist (deferred features)

- Tapping widget can launch the user's choice of weather app
- Ability to lock in a typed setting (e.g., location or text color) by hitting Return/Enter
- Widget: condition icon, high/low temperatures, font picker
- v2: Room + Hilt migration, temperature trend sparkline widget (12/24hr graph)
