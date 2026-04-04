# CLAUDE.md

This file orients a new Claude Code session for this repository.

## Project Overview

Android home-screen widget app (Kotlin + Jetpack Compose + Glance) showing current temperature and next alarm. Open-Meteo provides weather and geocoding (no API key required).

## Architecture

**Key design decisions:**

1. `WeatherRepository` receives two separate `OpenMeteoService` instances: `weatherService` (api.open-meteo.com) and `geocodingService` (geocoding-api.open-meteo.com) — both wired in `NetworkModule`.
2. Retrofit does not honour Kotlin default parameter values — all `@Query` params must be passed explicitly at every call site.
3. Temperature is always stored as Celsius (`LAST_TEMP_CELSIUS`); F/C conversion happens at display time only.
4. `WorkScheduler.schedule` is guarded: only called when both `LOCATION_LAT` and `LOCATION_LON` are present in DataStore. Uses `ExistingPeriodicWorkPolicy.UPDATE` with a `CONNECTED` network constraint.
5. `WeatherFetchWorker` calls `WeatherWidget().updateAll(context)` after a successful fetch.
6. `SettingsViewModel` accepts a `CoroutineDispatcher` for testability (default `Dispatchers.IO`; inject `StandardTestDispatcher` in tests).
7. `MainActivity.onCreate` launches WorkManager init with `lifecycleScope.launch(Dispatchers.IO)` — never use a bare `CoroutineScope` (leaks on Activity recreation).
8. Both widgets are wrapped in `GlanceTheme`. When dynamic color is on, use `GlanceTheme.colors.primary` for text; otherwise use a static `ColorProvider`.
9. Widget tap uses a `startActivity` Action. An empty/null `WIDGET_TAP_PACKAGE` (or `ALARM_WIDGET_TAP_PACKAGE`) falls back to launching `MainActivity`.
10. **Glance layout isolation (critical):** Never share a `@Composable` root scaffold across multiple `GlanceAppWidget` subclasses. Glance can assign the same layout resource ID to different widgets, causing content bleed. Each widget's Content composable must own its content independently.
11. `AlarmWidgetReceiver` calls `goAsync()` for all broadcasts **except** `ACTION_APPWIDGET_UPDATE` (Glance's base class already calls `goAsync` internally for that action).

## Build & Test Commands

Run all commands from the repo root.

| Command | Purpose |
|-|-|
| `./gradlew assembleDebug` | Full debug build |
| `./gradlew :app:compileDebugKotlin` | Compile-check only (faster) |
| `./gradlew :app:testDebugUnitTest` | All unit tests |
| `./gradlew :app:testDebugUnitTest --tests "com.wassupluke.widgets.data.WeatherRepositoryTest"` | Single test class |

`./gradlew :app:test` is not supported. All unit tests use Robolectric — no emulator needed.


## Testing Conventions

- `@RunWith(RobolectricTestRunner::class)` + `ApplicationProvider.getApplicationContext()` on all test classes.
- `@Before` must call `context.dataStore.edit { it.clear() }` to prevent cross-test DataStore pollution.
- Mock `OpenMeteoService` with MockK; pass separate mocks as `weatherService` and `geocodingService`.
- ViewModel tests: inject `StandardTestDispatcher(testScheduler)` and call `advanceUntilIdle()`.
- `SettingsViewModel.uiState` uses `stateIn(WhileSubscribed(5000))` — activate upstream before asserting: `backgroundScope.launch { vm.uiState.collect {} }`.
- Assert ViewModel state via `vm.uiState.filter { ... }.first()`, not by reading DataStore directly.
