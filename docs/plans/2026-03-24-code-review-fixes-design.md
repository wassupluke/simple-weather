# Code Review Fixes Design

**Goal:** Address all feedback from the initial code review — two security/correctness issues, two refactors, and two clean-up items.

---

## Changes

### 1. Shared constants in `WeatherDataStore`

Add `const val DEFAULT_TEMP_UNIT = "C"` and `const val DEFAULT_INTERVAL_MINUTES = 60` to the `WeatherDataStore` object alongside the existing preference keys. Replace all `?: "C"` and `?: 60` literals in `SettingsViewModel`, `SettingsUiState`, and `MainActivity` with these constants.

### 2. `WeatherRepository.create(context)` factory

Add a companion object to `WeatherRepository` with a `create(context: Context): WeatherRepository` factory method that wires `NetworkModule.weatherService` and `NetworkModule.geocodingService`. Replace the identical inline construction blocks in `MainActivity` and `WeatherFetchWorker` with calls to this factory.

### 3. Clamp interval in `WorkScheduler.schedule`

WorkManager enforces a 15-minute minimum for periodic work. Add `val clampedInterval = maxOf(15, intervalMinutes)` at the top of `schedule()` and use it in the `PeriodicWorkRequestBuilder` call. No caller changes needed.

### 4. Permission check in `MainActivity.fetchAndSaveDeviceLocation`

Before calling `fusedClient.lastLocation`, guard with:
```kotlin
if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
        != PackageManager.PERMISSION_GRANTED) return
```
Keep the `@SuppressLint` annotation (still needed to satisfy the lint rule on the `lastLocation` call itself).

### 5. Extract `scheduleIfLocationPresent()` in `SettingsViewModel`

Add a private suspend helper:
```kotlin
private suspend fun scheduleIfLocationPresent() {
    val prefs = context.dataStore.data.first()
    val lat = prefs[WeatherDataStore.LOCATION_LAT]
    val lon = prefs[WeatherDataStore.LOCATION_LON]
    val intervalMinutes = prefs[WeatherDataStore.UPDATE_INTERVAL_MINUTES]
        ?: DEFAULT_INTERVAL_MINUTES
    if (lat != null && lon != null) WorkScheduler.schedule(context, intervalMinutes)
}
```
Replace the three duplicated `dataStore.data.first()` + `WorkScheduler.schedule` blocks in `setUpdateInterval`, `resolveAndSaveLocation`, and `saveDeviceLocation` with a call to this helper.

### 6. Fix geocoding display name in `WeatherRepository`

Replace:
```kotlin
val displayName = listOfNotNull(first.name, first.state).joinToString(", ")
```
With:
```kotlin
val displayName = listOfNotNull(first.name, first.state, first.country).joinToString(", ")
```
`name` is non-nullable so output is never blank. Avoids the duplicate-country issue in the reviewer's suggested snippet.

---

## Files changed

| File | Change |
|-|-|
| `data/WeatherDataStore.kt` | Add 2 constants |
| `data/WeatherRepository.kt` | Add companion factory; fix display name |
| `worker/WorkScheduler.kt` | Clamp interval |
| `worker/WeatherFetchWorker.kt` | Use factory |
| `ui/MainActivity.kt` | Use factory; add permission guard |
| `ui/settings/SettingsViewModel.kt` | Extract helper; use constants |

## Testing

All existing unit tests cover the affected logic. After changes:
- `WeatherRepositoryTest` — verify geocoding display name with state=null produces `"City, Country"` not `"City, Country, Country"`
- `SettingsViewModelTest` — existing scheduling tests cover `scheduleIfLocationPresent` indirectly; no new tests needed
- `WorkScheduler` — no unit tests exist; the clamp is simple enough that a compile check suffices
