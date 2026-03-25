# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

A minimal Android weather app (Kotlin + Jetpack Compose + Material3) whose primary value is a home screen widget displaying the current temperature. Open-Meteo is used for weather and geocoding (no API key required). Implementation lives in the `.worktrees/feature-initial-build` git worktree.

## Architecture

**Stack:** Kotlin 2.0.21, Compose BOM 2024.12.01, Glance 1.1.1, WorkManager 2.10, DataStore 1.1, Retrofit 2.11 + kotlinx.serialization, FusedLocationProviderClient. Min SDK 26 / Target SDK 35. No Hilt, no Room.

**Data flow:**
```
WorkManager (periodic) ──→ WeatherRepository ──→ Open-Meteo API
                                             ↓
                                        DataStore (cache last reading)
                                             ↓
                              SettingsViewModel (UI)
                              WeatherWidget (reads DataStore directly via .first())
```

**Key design decisions:**
- `WeatherRepository` takes two separate `OpenMeteoService` instances: `weatherService` (api.open-meteo.com) and `geocodingService` (geocoding-api.open-meteo.com) — both created in `NetworkModule`
- Retrofit does not support Kotlin default parameter values — all `@Query` params must be passed explicitly at every call site
- Temperature is always stored as Celsius (`LAST_TEMP_CELSIUS`); F/C conversion happens at display time only
- `WorkScheduler` uses `enqueueUniquePeriodicWork` with `ExistingPeriodicWorkPolicy.UPDATE` and requires `CONNECTED` network
- `WeatherFetchWorker` calls `WeatherWidget().updateAll(context)` after a successful fetch
- `SettingsViewModel` accepts a `CoroutineDispatcher` parameter for testability (inject `testScheduler` in tests, default `Dispatchers.IO` in production)
- `SettingsScreen` accepts `onLocationPermissionGranted: (() -> Unit)?` — do not cast `LocalContext` to `MainActivity`; pass the callback from the call site
- `MainActivity.onCreate` uses `lifecycleScope.launch(Dispatchers.IO)` for the WorkManager init block — never use a bare `CoroutineScope` here (leaks on Activity recreation)
- `WorkScheduler.schedule` is only called when `LOCATION_LAT` and `LOCATION_LON` are both present in DataStore — guard this in any new call sites

## Build & Test Commands

Run all commands from the worktree root: `.worktrees/feature-initial-build/`

```bash
# Build
./gradlew assembleDebug

# Compile-check only (faster)
./gradlew :app:compileDebugKotlin

# Run all unit tests
./gradlew :app:testDebugUnitTest

# Run a specific test class
./gradlew :app:testDebugUnitTest --tests "com.example.simpleweather.data.WeatherRepositoryTest"
```

Note: `./gradlew :app:test` is not supported — use `:app:testDebugUnitTest`. All unit tests use Robolectric (no emulator needed).

## Package Structure

```
com.example.simpleweather
├── data/
│   ├── WeatherDataStore.kt       # DataStore keys + Context.dataStore extension
│   ├── WeatherRepository.kt      # fetchAndCacheWeather(), geocodeLocation()
│   └── api/
│       ├── WeatherApiModels.kt   # @Serializable data classes
│       ├── OpenMeteoService.kt   # Retrofit interface (no default param values)
│       └── NetworkModule.kt      # Two Retrofit instances (weather + geocoding)
├── ui/
│   ├── MainActivity.kt           # Manual ViewModelProvider.Factory; fetchAndSaveDeviceLocation()
│   ├── theme/SimpleWeatherTheme.kt
│   └── settings/
│       ├── SettingsViewModel.kt  # StateFlow<SettingsUiState> from DataStore
│       └── SettingsScreen.kt     # Compose UI: location toggle, F/C selector, interval picker
├── widget/
│   ├── WeatherWidget.kt          # GlanceAppWidget; reads DataStore with .first()
│   └── WeatherWidgetReceiver.kt  # GlanceAppWidgetReceiver
└── worker/
    ├── WeatherFetchWorker.kt     # CoroutineWorker; Result.failure() if no location
    └── WorkScheduler.kt          # schedule() / cancel() helpers
```

## Testing Conventions

- All tests use `@RunWith(RobolectricTestRunner::class)` + `ApplicationProvider.getApplicationContext()`
- Each test class touching DataStore needs `@Before fun clearDataStore()` calling `context.dataStore.edit { it.clear() }` to prevent cross-test pollution
- Mock `OpenMeteoService` with MockK — pass separate mocks as `weatherService` and `geocodingService`
- ViewModel tests: inject `StandardTestDispatcher(testScheduler)` and call `advanceUntilIdle()` instead of `Thread.sleep`
- `SettingsViewModel.uiState` uses `stateIn(WhileSubscribed(5000))` — activate upstream with `backgroundScope.launch { vm.uiState.collect {} }` before asserting
- Assert ViewModel state via `vm.uiState.filter { ... }.first()`, not by reading DataStore directly

## Dependency Versions

All versions are in `gradle/libs.versions.toml`. Aliases follow the pattern `libs.compose.bom`, `libs.glance.appwidget`, `libs.work.runtime.ktx`, etc.
