# Widget Tap Launch Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Let the user pick any installed app to open when tapping the widget; the choice persists in DataStore and falls back to Simple Weather if unset.

**Architecture:** New `WIDGET_TAP_PACKAGE` DataStore key; `SettingsViewModel` gains `setWidgetTapPackage`; a `ModalBottomSheet` in `SettingsScreen` shows all launchable apps (icons loaded via `produceState` + `toBitmap()` + `asImageBitmap()`, no new dependency); `WeatherWidget` reads the key and builds the tap action at render time.

**Tech Stack:** Kotlin, Jetpack Compose + Material3, Glance 1.1.1, DataStore Preferences, core-ktx (already present — provides `Drawable.toBitmap()`)

---

### Task 1: Create the feature branch

**Files:** none

**Step 1: Create and switch to branch**

```bash
cd /home/wassu/StudioProjects/simple-weather
git checkout -b feature/widget-tap-launch
```

Expected: `Switched to a new branch 'feature/widget-tap-launch'`

---

### Task 2: Add the DataStore key

**Files:**
- Modify: `app/src/main/java/com/wassupluke/simpleweather/data/WeatherDataStore.kt`

**Step 1: Add the new key**

In `WeatherDataStore`, add after `WIDGET_TEXT_COLOR`:

```kotlin
val WIDGET_TAP_PACKAGE = stringPreferencesKey("widget_tap_package")
```

**Step 2: Compile-check**

```bash
./gradlew :app:compileDebugKotlin
```

Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add app/src/main/java/com/wassupluke/simpleweather/data/WeatherDataStore.kt
git commit -m "feat: add WIDGET_TAP_PACKAGE DataStore key"
```

---

### Task 3: Update SettingsUiState and SettingsViewModel (TDD)

**Files:**
- Modify: `app/src/test/java/com/wassupluke/simpleweather/ui/settings/SettingsViewModelTest.kt`
- Modify: `app/src/main/java/com/wassupluke/simpleweather/ui/settings/SettingsViewModel.kt`

**Step 1: Write the failing tests**

Add these two tests to `SettingsViewModelTest` (after the last existing test):

```kotlin
@Test
fun `setWidgetTapPackage writes to DataStore`() = runTest(testDispatcher) {
    val vm = SettingsViewModel(application, mockRepository, testDispatcher)
    backgroundScope.launch { vm.uiState.collect {} }
    advanceUntilIdle()
    vm.setWidgetTapPackage("com.example.weather")
    advanceUntilIdle()
    val state = vm.uiState.filter { it.widgetTapPackage == "com.example.weather" }.first()
    assertEquals("com.example.weather", state.widgetTapPackage)
}

@Test
fun `widgetTapPackage defaults to empty string when key absent`() = runTest(testDispatcher) {
    val vm = SettingsViewModel(application, mockRepository, testDispatcher)
    backgroundScope.launch { vm.uiState.collect {} }
    advanceUntilIdle()
    val state = vm.uiState.first()
    assertEquals("", state.widgetTapPackage)
}
```

**Step 2: Run tests to verify they fail**

```bash
./gradlew :app:testDebugUnitTest --tests "com.wassupluke.simpleweather.ui.settings.SettingsViewModelTest"
```

Expected: FAIL — `widgetTapPackage` doesn't exist yet on `SettingsUiState`.

**Step 3: Update SettingsUiState**

In `SettingsViewModel.kt`, add `widgetTapPackage` to the data class:

```kotlin
data class SettingsUiState(
    val useDeviceLocation: Boolean = false,
    val locationQuery: String = "",
    val locationDisplayName: String = "",
    val tempUnit: String = WeatherDataStore.DEFAULT_TEMP_UNIT,
    val updateIntervalMinutes: Int = WeatherDataStore.DEFAULT_INTERVAL_MINUTES,
    val widgetTextColor: String = "white",
    val widgetTapPackage: String = ""
)
```

**Step 4: Update uiState mapping**

In the `uiState` StateFlow map block, add:

```kotlin
widgetTapPackage = prefs[WeatherDataStore.WIDGET_TAP_PACKAGE] ?: ""
```

**Step 5: Add setWidgetTapPackage method**

Add after `setWidgetTextColor`:

```kotlin
fun setWidgetTapPackage(pkg: String) {
    viewModelScope.launch(dispatcher) {
        context.dataStore.edit { it[WeatherDataStore.WIDGET_TAP_PACKAGE] = pkg }
        WeatherWidget().updateAll(context)
    }
}
```

**Step 6: Run tests to verify they pass**

```bash
./gradlew :app:testDebugUnitTest --tests "com.wassupluke.simpleweather.ui.settings.SettingsViewModelTest"
```

Expected: All 6 tests PASS.

**Step 7: Commit**

```bash
git add app/src/main/java/com/wassupluke/simpleweather/ui/settings/SettingsViewModel.kt \
        app/src/test/java/com/wassupluke/simpleweather/ui/settings/SettingsViewModelTest.kt
git commit -m "feat: add widgetTapPackage to SettingsViewModel and uiState"
```

---

### Task 4: Update SettingsScreen — new section + bottom sheet

**Files:**
- Modify: `app/src/main/java/com/wassupluke/simpleweather/ui/settings/SettingsScreen.kt`

**Step 1: Add the callback parameter to SettingsScreenContent**

Add `onSetWidgetTapPackage: (String) -> Unit` to the `SettingsScreenContent` parameter list (after `onSetWidgetTextColor`):

```kotlin
internal fun SettingsScreenContent(
    uiState: SettingsUiState,
    onRequestDeviceLocation: () -> Unit,
    onDisableDeviceLocation: () -> Unit,
    onSetLocation: (String) -> Unit,
    onSetTempUnit: (String) -> Unit,
    onSetUpdateInterval: (Int) -> Unit,
    onSetWidgetTextColor: (String) -> Unit,
    onSetWidgetTapPackage: (String) -> Unit,
)
```

**Step 2: Add bottom sheet state + app list at the top of SettingsScreenContent body**

After the existing `var locationInputInitialized` block, add:

```kotlin
var showAppPicker by remember { mutableStateOf(false) }

val context = LocalContext.current
val installedApps = remember {
    val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    context.packageManager
        .queryIntentActivities(intent, 0)
        .sortedBy { it.loadLabel(context.packageManager).toString() }
}
```

**Step 3: Add new section inside the Column, after the color preview Box and its error Text, before the final Spacer**

```kotlin
HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

Text(
    stringResource(R.string.title_widget_tap_action),
    style = MaterialTheme.typography.titleSmall
)

val selectedAppInfo = remember(uiState.widgetTapPackage) {
    if (uiState.widgetTapPackage.isEmpty()) null
    else runCatching {
        context.packageManager.getApplicationInfo(uiState.widgetTapPackage, 0)
    }.getOrNull()
}

Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = Modifier
        .fillMaxWidth()
        .clickable { showAppPicker = true }
        .padding(vertical = 8.dp)
) {
    if (selectedAppInfo != null) {
        val icon by produceState<ImageBitmap?>(null, uiState.widgetTapPackage) {
            value = withContext(Dispatchers.IO) {
                runCatching {
                    context.packageManager
                        .getApplicationIcon(uiState.widgetTapPackage)
                        .toBitmap()
                        .asImageBitmap()
                }.getOrNull()
            }
        }
        if (icon != null) {
            Image(
                bitmap = icon!!,
                contentDescription = null,
                modifier = Modifier.size(40.dp).padding(end = 8.dp)
            )
        } else {
            Spacer(Modifier.size(40.dp).padding(end = 8.dp))
        }
        Text(
            text = selectedAppInfo.loadLabel(context.packageManager).toString(),
            modifier = Modifier.weight(1f)
        )
    } else if (uiState.widgetTapPackage.isNotEmpty()) {
        // Package saved but app no longer installed
        Spacer(Modifier.size(40.dp).padding(end = 8.dp))
        Text(
            text = stringResource(R.string.label_selected_app_not_found),
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.error
        )
    } else {
        Spacer(Modifier.size(40.dp).padding(end = 8.dp))
        Text(
            text = stringResource(R.string.label_widget_tap_none),
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    Icon(
        imageVector = Icons.Default.ChevronRight,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

if (showAppPicker) {
    ModalBottomSheet(onDismissRequest = { showAppPicker = false }) {
        LazyColumn {
            items(installedApps) { resolveInfo ->
                val pkg = resolveInfo.activityInfo.packageName
                val label = resolveInfo.loadLabel(context.packageManager).toString()
                val appIcon by produceState<ImageBitmap?>(null, pkg) {
                    value = withContext(Dispatchers.IO) {
                        runCatching {
                            context.packageManager
                                .getApplicationIcon(pkg)
                                .toBitmap()
                                .asImageBitmap()
                        }.getOrNull()
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onSetWidgetTapPackage(pkg)
                            showAppPicker = false
                        }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    if (appIcon != null) {
                        Image(
                            bitmap = appIcon!!,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp).padding(end = 12.dp)
                        )
                    } else {
                        Spacer(Modifier.size(40.dp).padding(end = 12.dp))
                    }
                    Text(label, modifier = Modifier.weight(1f))
                    if (pkg == uiState.widgetTapPackage) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}
```

**Step 4: Add required imports**

Add to the import block at the top of `SettingsScreen.kt`:

```kotlin
import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
```

**Step 5: Wire up in SettingsScreen**

In the `SettingsScreen` composable, add the callback to `SettingsScreenContent`:

```kotlin
onSetWidgetTapPackage = { viewModel.setWidgetTapPackage(it) },
```

**Step 6: Add string resources**

In `app/src/main/res/values/strings.xml`, add:

```xml
<string name="title_widget_tap_action">Widget tap action</string>
<string name="label_widget_tap_none">None (opens Simple Weather)</string>
<string name="label_selected_app_not_found">Selected app not found</string>
```

**Step 7: Update all preview functions**

Each preview that calls `SettingsScreenContent` must add the new parameter:

```kotlin
onSetWidgetTapPackage = {},
```

**Step 8: Compile-check**

```bash
./gradlew :app:compileDebugKotlin
```

Expected: BUILD SUCCESSFUL (fix any import errors before proceeding)

**Step 9: Commit**

```bash
git add app/src/main/java/com/wassupluke/simpleweather/ui/settings/SettingsScreen.kt \
        app/src/main/res/values/strings.xml
git commit -m "feat: add widget tap action picker to SettingsScreen"
```

---

### Task 5: Update WeatherWidget to use the stored package

**Files:**
- Modify: `app/src/main/java/com/wassupluke/simpleweather/widget/WeatherWidget.kt`

**Step 1: Read the new key and build the tap action**

Replace the existing `WeatherWidgetContent(...)` call inside `provideGlance` with:

```kotlin
val tapPackage = prefs[WeatherDataStore.WIDGET_TAP_PACKAGE]
val tapAction = if (!tapPackage.isNullOrEmpty()) {
    val launchIntent = context.packageManager.getLaunchIntentForPackage(tapPackage)
    if (launchIntent != null) actionStartActivity(launchIntent)
    else actionStartActivity<MainActivity>()
} else {
    actionStartActivity<MainActivity>()
}

WeatherWidgetContent(displayTemp = displayTemp, textColor = textColor, tapAction = tapAction)
```

**Step 2: Add tapAction parameter to WeatherWidgetContent**

Change the function signature to:

```kotlin
private fun WeatherWidgetContent(
    displayTemp: String,
    textColor: androidx.compose.ui.graphics.Color,
    tapAction: Action
)
```

And update the `.clickable(...)` call:

```kotlin
.clickable(tapAction)
```

**Step 3: Add import for Action**

```kotlin
import androidx.glance.action.Action
```

**Step 4: Compile-check**

```bash
./gradlew :app:compileDebugKotlin
```

Expected: BUILD SUCCESSFUL

**Step 5: Run all tests**

```bash
./gradlew :app:testDebugUnitTest
```

Expected: All tests PASS

**Step 6: Commit**

```bash
git add app/src/main/java/com/wassupluke/simpleweather/widget/WeatherWidget.kt
git commit -m "feat: widget tap launches user-selected app with MainActivity fallback"
```

---

### Task 6: Final build verification

**Step 1: Full debug build**

```bash
./gradlew assembleDebug
```

Expected: BUILD SUCCESSFUL with a generated APK

**Step 2: Done**

The feature is complete. Create a PR from `feature/widget-tap-launch` → `main`.
