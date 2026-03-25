# Code Review Fixes Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Address all feedback from the initial code review: add shared constants, centralise repository construction, clamp WorkManager interval, add a permission guard, extract a scheduling helper, and improve the geocoding display name.

**Architecture:** Six isolated changes across five files. No new files. TDD where unit test infrastructure exists; compile+run for the rest.

**Tech Stack:** Kotlin, Robolectric unit tests (`./gradlew :app:testDebugUnitTest`), MockK.

---

## Task 1: Add shared constants to `WeatherDataStore`

**Files:**
- Modify: `app/src/main/java/com/example/simpleweather/data/WeatherDataStore.kt`
- Modify: `app/src/main/java/com/example/simpleweather/ui/settings/SettingsViewModel.kt`
- Modify: `app/src/main/java/com/example/simpleweather/ui/MainActivity.kt`

**Step 1: Add constants to `WeatherDataStore`**

In `WeatherDataStore.kt`, add two constants at the top of the object before the preference keys:

```kotlin
object WeatherDataStore {
    const val DEFAULT_TEMP_UNIT = "C"
    const val DEFAULT_INTERVAL_MINUTES = 60

    val USE_DEVICE_LOCATION = booleanPreferencesKey("use_device_location")
    // ... rest unchanged
}
```

**Step 2: Replace hard-coded literals in `SettingsViewModel.kt`**

Replace four literals in `SettingsViewModel.kt`:

```kotlin
// SettingsUiState default — line 19
tempUnit: String = WeatherDataStore.DEFAULT_TEMP_UNIT,
updateIntervalMinutes: Int = WeatherDataStore.DEFAULT_INTERVAL_MINUTES

// uiState map — lines 33-34
tempUnit = prefs[WeatherDataStore.TEMP_UNIT] ?: WeatherDataStore.DEFAULT_TEMP_UNIT,
updateIntervalMinutes = prefs[WeatherDataStore.UPDATE_INTERVAL_MINUTES] ?: WeatherDataStore.DEFAULT_INTERVAL_MINUTES

// setUpdateInterval — line 47
val prefs = context.dataStore.data.first()
// ...
val intervalMinutes = prefs[WeatherDataStore.UPDATE_INTERVAL_MINUTES] ?: WeatherDataStore.DEFAULT_INTERVAL_MINUTES

// resolveAndSaveLocation — line 73
val intervalMinutes = context.dataStore.data.first()[WeatherDataStore.UPDATE_INTERVAL_MINUTES] ?: WeatherDataStore.DEFAULT_INTERVAL_MINUTES

// saveDeviceLocation — line 85
val intervalMinutes = context.dataStore.data.first()[WeatherDataStore.UPDATE_INTERVAL_MINUTES] ?: WeatherDataStore.DEFAULT_INTERVAL_MINUTES
```

**Step 3: Replace hard-coded literal in `MainActivity.kt`**

```kotlin
// line 57
val intervalMinutes = prefs[WeatherDataStore.UPDATE_INTERVAL_MINUTES] ?: WeatherDataStore.DEFAULT_INTERVAL_MINUTES
```

**Step 4: Compile check**

```bash
./gradlew :app:compileDebugKotlin
```
Expected: BUILD SUCCESSFUL

**Step 5: Run tests**

```bash
./gradlew :app:testDebugUnitTest
```
Expected: BUILD SUCCESSFUL, all tests pass

**Step 6: Commit**

```bash
git add app/src/main/java/com/example/simpleweather/data/WeatherDataStore.kt \
        app/src/main/java/com/example/simpleweather/ui/settings/SettingsViewModel.kt \
        app/src/main/java/com/example/simpleweather/ui/MainActivity.kt
git commit -m "refactor: extract DEFAULT_TEMP_UNIT and DEFAULT_INTERVAL_MINUTES constants"
```

---

## Task 2: Fix geocoding display name + update tests

**Files:**
- Modify: `app/src/main/java/com/example/simpleweather/data/WeatherRepository.kt`
- Modify: `app/src/test/java/com/example/simpleweather/data/WeatherRepositoryTest.kt`

**Step 1: Update the existing geocoding test expectation**

The existing test asserts `"Portland, Oregon"` — after the fix it should include country. Open `WeatherRepositoryTest.kt` and update line 55:

```kotlin
assertEquals("Portland, Oregon, United States", result.displayName)
```

**Step 2: Add two new test cases**

Add after the existing geocoding tests in `WeatherRepositoryTest.kt`:

```kotlin
@Test
fun `geocodeLocation display name omits state when absent`() = runTest {
    coEvery { mockGeocodingService.searchLocation(any(), any(), any(), any()) } returns
        GeocodingResponse(listOf(GeocodingResult("Austin", 30.27, -97.74, country = "United States", state = null)))

    val result = repo().geocodeLocation("Austin")

    assertEquals("Austin, United States", result!!.displayName)
}

@Test
fun `geocodeLocation display name is just city when state and country absent`() = runTest {
    coEvery { mockGeocodingService.searchLocation(any(), any(), any(), any()) } returns
        GeocodingResponse(listOf(GeocodingResult("Springfield", 39.80, -89.64, country = null, state = null)))

    val result = repo().geocodeLocation("Springfield")

    assertEquals("Springfield", result!!.displayName)
}
```

**Step 3: Run tests to confirm they fail**

```bash
./gradlew :app:testDebugUnitTest --tests "com.example.simpleweather.data.WeatherRepositoryTest"
```
Expected: FAIL — existing test fails on `"Portland, Oregon"` vs `"Portland, Oregon, United States"`; new tests fail because `state=null` currently produces `"Austin"` not `"Austin, United States"`

**Step 4: Fix display name in `WeatherRepository.kt`**

Replace line 35:

```kotlin
// before
val displayName = listOfNotNull(first.name, first.state).joinToString(", ")

// after
val displayName = listOfNotNull(first.name, first.state, first.country).joinToString(", ")
```

**Step 5: Run tests to confirm they pass**

```bash
./gradlew :app:testDebugUnitTest --tests "com.example.simpleweather.data.WeatherRepositoryTest"
```
Expected: BUILD SUCCESSFUL, all 4 tests pass

**Step 6: Commit**

```bash
git add app/src/main/java/com/example/simpleweather/data/WeatherRepository.kt \
        app/src/test/java/com/example/simpleweather/data/WeatherRepositoryTest.kt
git commit -m "fix: include country in geocoding display name"
```

---

## Task 3: Add `WeatherRepository.create(context)` factory

**Files:**
- Modify: `app/src/main/java/com/example/simpleweather/data/WeatherRepository.kt`
- Modify: `app/src/main/java/com/example/simpleweather/ui/MainActivity.kt`
- Modify: `app/src/main/java/com/example/simpleweather/worker/WeatherFetchWorker.kt`

**Step 1: Add companion object factory to `WeatherRepository.kt`**

Add after the `GeocodingResult` data class:

```kotlin
companion object {
    fun create(context: Context): WeatherRepository = WeatherRepository(
        context = context,
        weatherService = NetworkModule.weatherService,
        geocodingService = NetworkModule.geocodingService
    )
}
```

Add the missing import at the top:
```kotlin
import com.example.simpleweather.data.api.NetworkModule
```

**Step 2: Update `MainActivity.kt` to use the factory**

Replace lines 29-33 (the `WeatherRepository(...)` construction inside the ViewModelProvider.Factory):

```kotlin
val repo = WeatherRepository.create(applicationContext)
```

Remove the now-unused import `import com.example.simpleweather.data.api.NetworkModule` from `MainActivity.kt`.

**Step 3: Update `WeatherFetchWorker.kt` to use the factory**

Replace lines 25-29:

```kotlin
val repo = WeatherRepository.create(applicationContext)
```

Remove the now-unused import `import com.example.simpleweather.data.api.NetworkModule` from `WeatherFetchWorker.kt`.

**Step 4: Compile check and run tests**

```bash
./gradlew :app:testDebugUnitTest
```
Expected: BUILD SUCCESSFUL, all tests pass

**Step 5: Commit**

```bash
git add app/src/main/java/com/example/simpleweather/data/WeatherRepository.kt \
        app/src/main/java/com/example/simpleweather/ui/MainActivity.kt \
        app/src/main/java/com/example/simpleweather/worker/WeatherFetchWorker.kt
git commit -m "refactor: centralise WeatherRepository construction in companion factory"
```

---

## Task 4: Clamp interval in `WorkScheduler`

**Files:**
- Modify: `app/src/main/java/com/example/simpleweather/worker/WorkScheduler.kt`

WorkManager silently ignores periodic work requests with an interval below 15 minutes and uses 15 minutes instead. Clamping in `schedule()` makes this explicit.

**Step 1: Add clamp to `WorkScheduler.kt`**

Replace the `schedule` function body:

```kotlin
fun schedule(context: Context, intervalMinutes: Int) {
    val clampedInterval = maxOf(15, intervalMinutes)
    val request = PeriodicWorkRequestBuilder<WeatherFetchWorker>(
        clampedInterval.toLong(), TimeUnit.MINUTES
    )
        .setConstraints(
            Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
        )
        .build()

    WorkManager.getInstance(context).enqueueUniquePeriodicWork(
        WORK_NAME,
        ExistingPeriodicWorkPolicy.UPDATE,
        request
    )
}
```

**Step 2: Compile check and run tests**

```bash
./gradlew :app:testDebugUnitTest
```
Expected: BUILD SUCCESSFUL, all tests pass

**Step 3: Commit**

```bash
git add app/src/main/java/com/example/simpleweather/worker/WorkScheduler.kt
git commit -m "fix: clamp WorkScheduler interval to WorkManager 15-minute minimum"
```

---

## Task 5: Add permission guard in `MainActivity.fetchAndSaveDeviceLocation`

**Files:**
- Modify: `app/src/main/java/com/example/simpleweather/ui/MainActivity.kt`

**Step 1: Add the permission check**

Add these imports to `MainActivity.kt`:

```kotlin
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
```

Replace the body of `fetchAndSaveDeviceLocation`:

```kotlin
@SuppressLint("MissingPermission")
fun fetchAndSaveDeviceLocation() {
    if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) return
    val fusedClient = LocationServices.getFusedLocationProviderClient(this)
    fusedClient.lastLocation.addOnSuccessListener { location ->
        location?.let {
            viewModel.saveDeviceLocation(it.latitude.toFloat(), it.longitude.toFloat())
        }
    }
}
```

**Step 2: Compile check and run tests**

```bash
./gradlew :app:testDebugUnitTest
```
Expected: BUILD SUCCESSFUL, all tests pass

**Step 3: Commit**

```bash
git add app/src/main/java/com/example/simpleweather/ui/MainActivity.kt
git commit -m "fix: guard fetchAndSaveDeviceLocation with explicit permission check"
```

---

## Task 6: Extract `scheduleIfLocationPresent()` in `SettingsViewModel`

**Files:**
- Modify: `app/src/main/java/com/example/simpleweather/ui/settings/SettingsViewModel.kt`

**Step 1: Add the private helper**

Add this method to `SettingsViewModel` after `setUseDeviceLocation`:

```kotlin
private suspend fun scheduleIfLocationPresent() {
    val prefs = context.dataStore.data.first()
    val lat = prefs[WeatherDataStore.LOCATION_LAT]
    val lon = prefs[WeatherDataStore.LOCATION_LON]
    val intervalMinutes = prefs[WeatherDataStore.UPDATE_INTERVAL_MINUTES]
        ?: WeatherDataStore.DEFAULT_INTERVAL_MINUTES
    if (lat != null && lon != null) WorkScheduler.schedule(context, intervalMinutes)
}
```

**Step 2: Replace duplicated scheduling logic in `setUpdateInterval`**

```kotlin
fun setUpdateInterval(minutes: Int) {
    viewModelScope.launch(dispatcher) {
        context.dataStore.edit { it[WeatherDataStore.UPDATE_INTERVAL_MINUTES] = minutes }
        scheduleIfLocationPresent()
    }
}
```

**Step 3: Replace duplicated scheduling logic in `resolveAndSaveLocation`**

```kotlin
fun resolveAndSaveLocation(query: String) {
    viewModelScope.launch(dispatcher) {
        val result = repository.geocodeLocation(query) ?: return@launch
        context.dataStore.edit { prefs ->
            prefs[WeatherDataStore.LOCATION_LAT] = result.lat
            prefs[WeatherDataStore.LOCATION_LON] = result.lon
            prefs[WeatherDataStore.LOCATION_DISPLAY_NAME] = result.displayName
        }
        scheduleIfLocationPresent()
    }
}
```

**Step 4: Replace duplicated scheduling logic in `saveDeviceLocation`**

```kotlin
fun saveDeviceLocation(lat: Float, lon: Float) {
    viewModelScope.launch(dispatcher) {
        context.dataStore.edit { prefs ->
            prefs[WeatherDataStore.LOCATION_LAT] = lat
            prefs[WeatherDataStore.LOCATION_LON] = lon
            prefs[WeatherDataStore.LOCATION_DISPLAY_NAME] = "Current Location"
        }
        scheduleIfLocationPresent()
    }
}
```

**Step 5: Run existing ViewModel tests to confirm no regression**

```bash
./gradlew :app:testDebugUnitTest --tests "com.example.simpleweather.ui.settings.SettingsViewModelTest"
```
Expected: BUILD SUCCESSFUL, all tests pass

**Step 6: Run full suite**

```bash
./gradlew :app:testDebugUnitTest
```
Expected: BUILD SUCCESSFUL, all tests pass

**Step 7: Commit**

```bash
git add app/src/main/java/com/example/simpleweather/ui/settings/SettingsViewModel.kt
git commit -m "refactor: extract scheduleIfLocationPresent helper in SettingsViewModel"
```
