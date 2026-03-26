# Dynamic Color Widget Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Add a "Dynamic color" toggle to the widget settings that uses Material You wallpaper-extracted colors for the temperature text; toggling off reveals the existing manual color picker.

**Architecture:** Wrap `WeatherWidgetContent` in `GlanceTheme` (already a dep); read a new `WIDGET_DYNAMIC_COLOR` DataStore key with smart-default logic (absent + no custom color → true; absent + custom color present → false). SettingsScreen shows the toggle only on API 31+; the color picker row is hidden when dynamic is on.

**Tech Stack:** Glance 1.1.1, `androidx.glance.material3.GlanceTheme`, DataStore Preferences, Robolectric unit tests.

---

### Task 1: Add `WIDGET_DYNAMIC_COLOR` DataStore key

**Files:**
- Modify: `app/src/main/java/com/wassupluke/simpleweather/data/WeatherDataStore.kt`

**Step 1: Add the key**

In `WeatherDataStore`, add after the `WIDGET_TAP_PACKAGE` line:

```kotlin
val WIDGET_DYNAMIC_COLOR = booleanPreferencesKey("widget_dynamic_color")
```

**Step 2: Compile-check**

```bash
./gradlew :app:compileDebugKotlin
```
Expected: BUILD SUCCESSFUL, no errors.

**Step 3: Commit**

```bash
git add app/src/main/java/com/wassupluke/simpleweather/data/WeatherDataStore.kt
git commit -m "feat: add WIDGET_DYNAMIC_COLOR DataStore key"
```

---

### Task 2: Add string resources

**Files:**
- Modify: `app/src/main/res/values/strings.xml`

**Step 1: Add two strings**

Inside `<resources>`, add:

```xml
<string name="label_dynamic_color">Dynamic color</string>
<string name="title_widget_appearance">Widget Appearance</string>
```

**Step 2: Compile-check**

```bash
./gradlew :app:compileDebugKotlin
```
Expected: BUILD SUCCESSFUL.

**Step 3: Commit**

```bash
git add app/src/main/res/values/strings.xml
git commit -m "feat: add dynamic color string resources"
```

---

### Task 3: Add `widgetDynamicColor` to SettingsViewModel with smart-default logic

**Files:**
- Modify: `app/src/main/java/com/wassupluke/simpleweather/ui/settings/SettingsViewModel.kt`
- Test: `app/src/test/java/com/wassupluke/simpleweather/ui/settings/SettingsViewModelTest.kt`

**Background — smart-default logic:**

The `WIDGET_DYNAMIC_COLOR` key may be absent (never written). We infer the right default:
- Key absent AND `WIDGET_TEXT_COLOR` absent → `true` (new install)
- Key absent AND `WIDGET_TEXT_COLOR` present → `false` (existing user with custom color)
- Key present → use its stored value

This is evaluated in the DataStore `map { prefs -> ... }` block — `prefs[key]` returns `null` when absent.

**Step 1: Write the failing tests**

Add to `SettingsViewModelTest.kt`:

```kotlin
@Test
fun `widgetDynamicColor defaults to true when both keys absent (new install)`() = runTest(testDispatcher) {
    val vm = SettingsViewModel(application, mockRepository, testDispatcher)
    backgroundScope.launch { vm.uiState.collect {} }
    advanceUntilIdle()
    val state = vm.uiState.first()
    assertEquals(true, state.widgetDynamicColor)
}

@Test
fun `widgetDynamicColor defaults to false when text color already set (existing user)`() = runTest(testDispatcher) {
    // Pre-seed WIDGET_TEXT_COLOR to simulate existing user
    runBlocking {
        application.dataStore.edit { it[WeatherDataStore.WIDGET_TEXT_COLOR] = "blue" }
    }
    val vm = SettingsViewModel(application, mockRepository, testDispatcher)
    backgroundScope.launch { vm.uiState.collect {} }
    advanceUntilIdle()
    val state = vm.uiState.first()
    assertEquals(false, state.widgetDynamicColor)
}

@Test
fun `setWidgetDynamicColor writes to DataStore`() = runTest(testDispatcher) {
    val vm = SettingsViewModel(application, mockRepository, testDispatcher)
    backgroundScope.launch { vm.uiState.collect {} }
    advanceUntilIdle()
    vm.setWidgetDynamicColor(false)
    advanceUntilIdle()
    val state = vm.uiState.filter { !it.widgetDynamicColor }.first()
    assertEquals(false, state.widgetDynamicColor)
}
```

**Step 2: Run tests — expect failure**

```bash
./gradlew :app:testDebugUnitTest --tests "com.wassupluke.simpleweather.ui.settings.SettingsViewModelTest" 2>&1 | tail -20
```
Expected: FAILED — `widgetDynamicColor` doesn't exist yet.

**Step 3: Update `SettingsUiState`**

Add `widgetDynamicColor: Boolean = true` to the data class:

```kotlin
data class SettingsUiState(
    val useDeviceLocation: Boolean = false,
    val locationQuery: String = "",
    val locationDisplayName: String = "",
    val tempUnit: String = WeatherDataStore.DEFAULT_TEMP_UNIT,
    val updateIntervalMinutes: Int = WeatherDataStore.DEFAULT_INTERVAL_MINUTES,
    val widgetTextColor: String = "white",
    val widgetDynamicColor: Boolean = true,
    val widgetTapPackage: String = ""
)
```

**Step 4: Update the `uiState` map block in `SettingsViewModel`**

Replace the existing `uiState` StateFlow mapping with:

```kotlin
val uiState: StateFlow<SettingsUiState> = context.dataStore.data.map { prefs ->
    val storedDynamic = prefs[WeatherDataStore.WIDGET_DYNAMIC_COLOR]
    val dynamicColor = storedDynamic ?: (prefs[WeatherDataStore.WIDGET_TEXT_COLOR] == null)
    SettingsUiState(
        useDeviceLocation = prefs[WeatherDataStore.USE_DEVICE_LOCATION] ?: false,
        locationQuery = prefs[WeatherDataStore.LOCATION_QUERY] ?: "",
        locationDisplayName = prefs[WeatherDataStore.LOCATION_DISPLAY_NAME] ?: "",
        tempUnit = prefs[WeatherDataStore.TEMP_UNIT] ?: WeatherDataStore.DEFAULT_TEMP_UNIT,
        updateIntervalMinutes = prefs[WeatherDataStore.UPDATE_INTERVAL_MINUTES] ?: WeatherDataStore.DEFAULT_INTERVAL_MINUTES,
        widgetTextColor = prefs[WeatherDataStore.WIDGET_TEXT_COLOR] ?: "white",
        widgetDynamicColor = dynamicColor,
        widgetTapPackage = prefs[WeatherDataStore.WIDGET_TAP_PACKAGE] ?: ""
    )
}.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsUiState())
```

**Step 5: Add `setWidgetDynamicColor` method**

Add after `setWidgetTextColor`:

```kotlin
fun setWidgetDynamicColor(enabled: Boolean) {
    viewModelScope.launch(dispatcher) {
        context.dataStore.edit { it[WeatherDataStore.WIDGET_DYNAMIC_COLOR] = enabled }
        WeatherWidget().updateAll(context)
    }
}
```

**Step 6: Run tests — expect pass**

```bash
./gradlew :app:testDebugUnitTest --tests "com.wassupluke.simpleweather.ui.settings.SettingsViewModelTest" 2>&1 | tail -20
```
Expected: All tests pass including the 3 new ones.

**Step 7: Commit**

```bash
git add app/src/main/java/com/wassupluke/simpleweather/ui/settings/SettingsViewModel.kt \
        app/src/test/java/com/wassupluke/simpleweather/ui/settings/SettingsViewModelTest.kt
git commit -m "feat: add widgetDynamicColor to SettingsViewModel with smart-default logic"
```

---

### Task 4: Update WeatherWidget to use GlanceTheme

**Files:**
- Modify: `app/src/main/java/com/wassupluke/simpleweather/widget/WeatherWidget.kt`

**Background — Glance color resolution:**

`GlanceTheme` (from `androidx.glance.material3`) is a Composable that provides Material You colors. Inside it, `GlanceTheme.colors.primary` is a `ColorProvider` that resolves to the wallpaper-seeded palette on API 31+, and a baseline scheme below that. We pass `ColorProvider` directly to `WeatherWidgetContent` rather than a resolved `Color`, so Glance can perform light/dark resolution at render time.

**Step 1: Apply the changes**

Replace `WeatherWidget.kt` entirely with:

```kotlin
package com.wassupluke.simpleweather.widget

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.emptyPreferences
import androidx.glance.*
import androidx.glance.action.Action
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.*
import androidx.glance.material3.GlanceTheme
import androidx.glance.text.*
import androidx.glance.unit.ColorProvider
import com.wassupluke.simpleweather.data.WeatherDataStore
import com.wassupluke.simpleweather.data.dataStore
import com.wassupluke.simpleweather.data.parseColorSafe
import com.wassupluke.simpleweather.ui.MainActivity
import kotlin.math.roundToInt

class WeatherWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            GlanceTheme {
                val prefs by context.dataStore.data.collectAsState(initial = emptyPreferences())
                val tempCelsius = prefs[WeatherDataStore.LAST_TEMP_CELSIUS]
                val unit = prefs[WeatherDataStore.TEMP_UNIT] ?: "C"
                val colorString = prefs[WeatherDataStore.WIDGET_TEXT_COLOR] ?: "white"
                val storedDynamic = prefs[WeatherDataStore.WIDGET_DYNAMIC_COLOR]
                val dynamicColor = storedDynamic ?: (prefs[WeatherDataStore.WIDGET_TEXT_COLOR] == null)

                val displayTemp = if (tempCelsius == null) {
                    "--°"
                } else {
                    val value = if (unit == "F") celsiusToFahrenheit(tempCelsius) else tempCelsius.roundToInt()
                    "$value°"
                }

                val textColorProvider: ColorProvider = when {
                    dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
                        GlanceTheme.colors.primary
                    dynamicColor ->
                        ColorProvider(Color.White)
                    else -> {
                        val resolved = parseColorSafe(colorString)?.let { argb ->
                            Color(
                                red = android.graphics.Color.red(argb) / 255f,
                                green = android.graphics.Color.green(argb) / 255f,
                                blue = android.graphics.Color.blue(argb) / 255f,
                                alpha = android.graphics.Color.alpha(argb) / 255f
                            )
                        } ?: Color.White
                        ColorProvider(resolved)
                    }
                }

                val tapPackage = prefs[WeatherDataStore.WIDGET_TAP_PACKAGE]
                val tapAction = if (!tapPackage.isNullOrEmpty()) {
                    val launchIntent = context.packageManager.getLaunchIntentForPackage(tapPackage)
                    if (launchIntent?.component != null) actionStartActivity(launchIntent.component!!)
                    else actionStartActivity<MainActivity>()
                } else {
                    actionStartActivity<MainActivity>()
                }

                WeatherWidgetContent(
                    displayTemp = displayTemp,
                    textColorProvider = textColorProvider,
                    tapAction = tapAction
                )
            }
        }
    }

    private fun celsiusToFahrenheit(celsius: Float): Int =
        ((celsius * 9f / 5f) + 32f).roundToInt()
}

@SuppressLint("RestrictedApi")
@Composable
private fun WeatherWidgetContent(
    displayTemp: String,
    textColorProvider: ColorProvider,
    tapAction: Action
) {
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .clickable(tapAction),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = displayTemp,
            style = TextStyle(
                fontSize = 48.sp,
                fontWeight = FontWeight.Normal,
                color = textColorProvider
            )
        )
    }
}
```

Key changes vs. the old file:
- `GlanceTheme { }` wraps all content
- `WeatherWidgetContent` now takes `textColorProvider: ColorProvider` instead of `textColor: androidx.compose.ui.graphics.Color`
- The smart-default logic is duplicated from the ViewModel (DataStore is the source of truth; the widget reads it independently)

**Step 2: Compile-check**

```bash
./gradlew :app:compileDebugKotlin
```
Expected: BUILD SUCCESSFUL.

**Step 3: Commit**

```bash
git add app/src/main/java/com/wassupluke/simpleweather/widget/WeatherWidget.kt
git commit -m "feat: wrap WeatherWidget in GlanceTheme for Material You dynamic color"
```

---

### Task 5: Add dynamic color toggle to SettingsScreen

**Files:**
- Modify: `app/src/main/java/com/wassupluke/simpleweather/ui/settings/SettingsScreen.kt`

**Background — what changes:**

1. `SettingsScreenContent` gains `onSetWidgetDynamicColor: (Boolean) -> Unit` parameter.
2. Under the `HorizontalDivider` before `title_widget_text_color`, on API 31+ devices, render a new toggle row for dynamic color (same pattern as the "Use device location" row).
3. The color picker section (`title_widget_text_color` + text field + preview box) is wrapped in `if (!uiState.widgetDynamicColor)`.
4. The section heading (`Text(stringResource(R.string.title_widget_text_color), ...)`) moves inside the `if` block too (no orphaned heading when color picker is hidden).
5. `SettingsScreen` passes `viewModel.setWidgetDynamicColor` to the new parameter.
6. All three `@Preview` functions and `SettingsScreenEmptyPreview` need the new parameter added (`onSetWidgetDynamicColor = {}`).

**Step 1: Add the new callback to `SettingsScreenContent`**

Add `onSetWidgetDynamicColor: (Boolean) -> Unit,` to the function signature, after `onSetWidgetTapPackage`.

**Step 2: Replace the widget color section**

Find the block starting with:
```kotlin
HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

Text(stringResource(R.string.title_widget_text_color), style = MaterialTheme.typography.titleSmall)
```

Replace through to (and including) the invalid-color `Text` error row and its closing `}` with:

```kotlin
HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSetWidgetDynamicColor(!uiState.widgetDynamicColor) }
    ) {
        Text(stringResource(R.string.label_dynamic_color), modifier = Modifier.weight(1f))
        Switch(
            checked = uiState.widgetDynamicColor,
            onCheckedChange = { onSetWidgetDynamicColor(it) }
        )
    }
}

if (!uiState.widgetDynamicColor) {
    Text(stringResource(R.string.title_widget_text_color), style = MaterialTheme.typography.titleSmall)

    var colorInput by remember { mutableStateOf(uiState.widgetTextColor) }
    LaunchedEffect(uiState.widgetTextColor) { colorInput = uiState.widgetTextColor }

    val previewColor = remember(uiState.widgetTextColor) {
        parseColorSafe(uiState.widgetTextColor)?.let { argb ->
            Color(
                red = android.graphics.Color.red(argb) / 255f,
                green = android.graphics.Color.green(argb) / 255f,
                blue = android.graphics.Color.blue(argb) / 255f,
                alpha = android.graphics.Color.alpha(argb) / 255f
            )
        }
    }

    OutlinedTextField(
        value = colorInput,
        onValueChange = { colorInput = it },
        label = { Text(stringResource(R.string.label_text_color)) },
        placeholder = { Text(stringResource(R.string.hint_color_input)) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = {
            onSetWidgetTextColor(colorInput.trim())
        }),
        trailingIcon = {
            if (colorInput.isNotBlank()) {
                SetButton { onSetWidgetTextColor(colorInput.trim()) }
            }
        }
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .background(previewColor ?: MaterialTheme.colorScheme.errorContainer)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(4.dp))
    )

    if (previewColor == null && uiState.widgetTextColor.isNotEmpty()) {
        Text(
            text = stringResource(R.string.error_invalid_color),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error
        )
    }
}
```

**Step 3: Update `SettingsScreen` to wire the new callback**

Add to the `SettingsScreenContent(...)` call:
```kotlin
onSetWidgetDynamicColor = { viewModel.setWidgetDynamicColor(it) },
```

**Step 4: Update all `@Preview` functions**

Add `onSetWidgetDynamicColor = {},` to every `SettingsScreenContent(...)` call in the preview functions (there are 3).

**Step 5: Compile-check**

```bash
./gradlew :app:compileDebugKotlin
```
Expected: BUILD SUCCESSFUL. Fix any "unresolved reference" errors (check `Build` import is present — it should already be, from the app picker code).

**Step 6: Run all tests**

```bash
./gradlew :app:testDebugUnitTest 2>&1 | tail -20
```
Expected: All tests pass.

**Step 7: Commit**

```bash
git add app/src/main/java/com/wassupluke/simpleweather/ui/settings/SettingsScreen.kt
git commit -m "feat: add dynamic color toggle to SettingsScreen, hide color picker when enabled"
```

---

### Task 6: Full build verification

**Step 1: Assemble debug APK**

```bash
./gradlew assembleDebug 2>&1 | tail -20
```
Expected: BUILD SUCCESSFUL.

**Step 2: Run all unit tests**

```bash
./gradlew :app:testDebugUnitTest 2>&1 | tail -20
```
Expected: All tests pass.

**Step 3: Commit (if any stray changes)**

If clean, no commit needed. The feature is complete.
