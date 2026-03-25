# Widget Text Color Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Let users set a custom text color for the weather widget, stored in DataStore, defaulting to white.

**Architecture:** Add a `WIDGET_TEXT_COLOR` string key to DataStore; surface it through `SettingsViewModel`; render it in `WeatherWidget` by parsing the stored string via `android.graphics.Color.parseColor()`. The settings UI gains an inline color picker (text field + preview swatch) and the interval radio group becomes a dropdown to keep the screen scroll-free.

**Tech Stack:** Kotlin, Jetpack Compose + Material 3, Glance 1.1.1, DataStore Preferences, Robolectric (tests)

---

### Task 1: Add WIDGET_TEXT_COLOR key to WeatherDataStore

**Files:**
- Modify: `app/src/main/java/com/example/simpleweather/data/WeatherDataStore.kt`
- Test: `app/src/test/java/com/example/simpleweather/data/WeatherDataStoreTest.kt`

**Step 1: Write the failing test**

Add to `WeatherDataStoreTest`:

```kotlin
@Test
fun `widget text color key is absent by default`() = runTest {
    val prefs = context.dataStore.data.first()
    assertNull(prefs[WeatherDataStore.WIDGET_TEXT_COLOR])
}
```

**Step 2: Run test to verify it fails**

```bash
./gradlew :app:testDebugUnitTest --tests "com.example.simpleweather.data.WeatherDataStoreTest.widget text color key is absent by default"
```

Expected: FAIL — `WIDGET_TEXT_COLOR` not yet defined.

**Step 3: Add the key to WeatherDataStore**

Add one line inside the `WeatherDataStore` object, after `LAST_UPDATED_EPOCH`:

```kotlin
val WIDGET_TEXT_COLOR = stringPreferencesKey("widget_text_color")
```

**Step 4: Run test to verify it passes**

```bash
./gradlew :app:testDebugUnitTest --tests "com.example.simpleweather.data.WeatherDataStoreTest.widget text color key is absent by default"
```

Expected: PASS

**Step 5: Commit**

```bash
git add app/src/main/java/com/example/simpleweather/data/WeatherDataStore.kt \
        app/src/test/java/com/example/simpleweather/data/WeatherDataStoreTest.kt
git commit -m "feat: add WIDGET_TEXT_COLOR DataStore key"
```

---

### Task 2: Expose widgetTextColor in SettingsUiState and SettingsViewModel

**Files:**
- Modify: `app/src/main/java/com/example/simpleweather/ui/settings/SettingsViewModel.kt`
- Test: `app/src/test/java/com/example/simpleweather/ui/settings/SettingsViewModelTest.kt`

**Step 1: Write the failing test**

Add to `SettingsViewModelTest`:

```kotlin
@Test
fun `setWidgetTextColor writes to DataStore`() = runTest(testDispatcher) {
    val vm = SettingsViewModel(context, mockRepository, testDispatcher)
    backgroundScope.launch { vm.uiState.collect {} }
    advanceUntilIdle()
    vm.setWidgetTextColor("red")
    advanceUntilIdle()
    val state = vm.uiState.filter { it.widgetTextColor == "red" }.first()
    assertEquals("red", state.widgetTextColor)
}

@Test
fun `widgetTextColor defaults to white when key absent`() = runTest(testDispatcher) {
    val vm = SettingsViewModel(context, mockRepository, testDispatcher)
    backgroundScope.launch { vm.uiState.collect {} }
    advanceUntilIdle()
    val state = vm.uiState.first()
    assertEquals("white", state.widgetTextColor)
}
```

**Step 2: Run tests to verify they fail**

```bash
./gradlew :app:testDebugUnitTest --tests "com.example.simpleweather.ui.settings.SettingsViewModelTest"
```

Expected: FAIL — `widgetTextColor` does not exist on `SettingsUiState`.

**Step 3: Update SettingsUiState and SettingsViewModel**

In `SettingsViewModel.kt`, update `SettingsUiState` to add the new field:

```kotlin
data class SettingsUiState(
    val useDeviceLocation: Boolean = false,
    val locationDisplayName: String = "",
    val tempUnit: String = WeatherDataStore.DEFAULT_TEMP_UNIT,
    val updateIntervalMinutes: Int = WeatherDataStore.DEFAULT_INTERVAL_MINUTES,
    val widgetTextColor: String = "white"
)
```

Update the `uiState` mapping inside `SettingsViewModel` to map the new field (add one line):

```kotlin
val uiState: StateFlow<SettingsUiState> = context.dataStore.data.map { prefs ->
    SettingsUiState(
        useDeviceLocation = prefs[WeatherDataStore.USE_DEVICE_LOCATION] ?: false,
        locationDisplayName = prefs[WeatherDataStore.LOCATION_DISPLAY_NAME] ?: "",
        tempUnit = prefs[WeatherDataStore.TEMP_UNIT] ?: WeatherDataStore.DEFAULT_TEMP_UNIT,
        updateIntervalMinutes = prefs[WeatherDataStore.UPDATE_INTERVAL_MINUTES] ?: WeatherDataStore.DEFAULT_INTERVAL_MINUTES,
        widgetTextColor = prefs[WeatherDataStore.WIDGET_TEXT_COLOR] ?: "white"
    )
}.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsUiState())
```

Add `setWidgetTextColor` after `setUpdateInterval`:

```kotlin
fun setWidgetTextColor(raw: String) {
    viewModelScope.launch(dispatcher) {
        context.dataStore.edit { it[WeatherDataStore.WIDGET_TEXT_COLOR] = raw }
    }
}
```

**Step 4: Run tests to verify they pass**

```bash
./gradlew :app:testDebugUnitTest --tests "com.example.simpleweather.ui.settings.SettingsViewModelTest"
```

Expected: all 4 tests PASS

**Step 5: Commit**

```bash
git add app/src/main/java/com/example/simpleweather/ui/settings/SettingsViewModel.kt \
        app/src/test/java/com/example/simpleweather/ui/settings/SettingsViewModelTest.kt
git commit -m "feat: add widgetTextColor to SettingsUiState and SettingsViewModel"
```

---

### Task 3: Apply color in WeatherWidget

**Files:**
- Modify: `app/src/main/java/com/example/simpleweather/widget/WeatherWidget.kt`

No unit tests are written for Glance widgets (requires emulator). A compile check suffices.

**Step 1: Update WeatherWidget.provideGlance to read and parse the color**

Replace the body of `provideGlance` and `WeatherWidgetContent`:

```kotlin
override suspend fun provideGlance(context: Context, id: GlanceId) {
    val prefs = context.dataStore.data.first()
    val tempCelsius = prefs[WeatherDataStore.LAST_TEMP_CELSIUS]
    val unit = prefs[WeatherDataStore.TEMP_UNIT] ?: "C"
    val colorString = prefs[WeatherDataStore.WIDGET_TEXT_COLOR] ?: "white"

    val displayTemp = if (tempCelsius == null) {
        "--°"
    } else {
        val value = if (unit == "F") celsiusToFahrenheit(tempCelsius) else tempCelsius.roundToInt()
        "$value°"
    }

    val textColor = try {
        val argb = android.graphics.Color.parseColor(colorString)
        androidx.compose.ui.graphics.Color(
            red = android.graphics.Color.red(argb) / 255f,
            green = android.graphics.Color.green(argb) / 255f,
            blue = android.graphics.Color.blue(argb) / 255f,
            alpha = android.graphics.Color.alpha(argb) / 255f
        )
    } catch (e: IllegalArgumentException) {
        androidx.compose.ui.graphics.Color.White
    }

    provideContent {
        WeatherWidgetContent(displayTemp = displayTemp, textColor = textColor)
    }
}
```

Update `WeatherWidgetContent` signature and body — remove `DayNightColorProvider`, use the passed color:

```kotlin
@Composable
private fun WeatherWidgetContent(
    displayTemp: String,
    textColor: androidx.compose.ui.graphics.Color
) {
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .clickable(actionStartActivity<MainActivity>()),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = displayTemp,
            style = TextStyle(
                fontSize = 48.sp,
                fontWeight = FontWeight.Normal,
                color = ColorProvider(textColor)
            )
        )
    }
}
```

Update imports — remove `DayNightColorProvider`, add `ColorProvider`:

```kotlin
import androidx.glance.unit.ColorProvider
// remove: import androidx.glance.color.DayNightColorProvider
```

**Step 2: Compile check**

```bash
./gradlew :app:compileDebugKotlin
```

Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add app/src/main/java/com/example/simpleweather/widget/WeatherWidget.kt
git commit -m "feat: apply user-selected text color in WeatherWidget"
```

---

### Task 4: Update SettingsScreen — dropdown interval + color picker

**Files:**
- Modify: `app/src/main/java/com/example/simpleweather/ui/settings/SettingsScreen.kt`

No unit tests — Compose UI changes are verified by compile check and manual review.

**Step 1: Replace the radio group with ExposedDropdownMenuBox**

Replace the entire "Update Interval" section (from `Text("Update Interval"...)` through the closing `}` of the `forEach`) with:

```kotlin
Text("Update Interval", style = MaterialTheme.typography.titleSmall)

val intervalOptions = listOf(15 to "15 min", 30 to "30 min", 60 to "1 hr", 180 to "3 hr", 360 to "6 hr")
var intervalExpanded by remember { mutableStateOf(false) }
val selectedLabel = intervalOptions.firstOrNull { it.first == uiState.updateIntervalMinutes }?.second ?: "1 hr"

ExposedDropdownMenuBox(
    expanded = intervalExpanded,
    onExpandedChange = { intervalExpanded = !intervalExpanded }
) {
    OutlinedTextField(
        value = selectedLabel,
        onValueChange = {},
        readOnly = true,
        label = { Text("Interval") },
        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = intervalExpanded) },
        modifier = Modifier
            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
            .fillMaxWidth()
    )
    ExposedDropdownMenu(
        expanded = intervalExpanded,
        onDismissRequest = { intervalExpanded = false }
    ) {
        intervalOptions.forEach { (minutes, label) ->
            DropdownMenuItem(
                text = { Text(label) },
                onClick = {
                    viewModel.setUpdateInterval(minutes)
                    intervalExpanded = false
                }
            )
        }
    }
}
```

**Step 2: Add color picker section**

After the interval dropdown (but before `Spacer(Modifier.height(16.dp))`), add:

```kotlin
HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

Text("Widget Text Color", style = MaterialTheme.typography.titleSmall)

var colorInput by remember { mutableStateOf(uiState.widgetTextColor) }
LaunchedEffect(uiState.widgetTextColor) { colorInput = uiState.widgetTextColor }

val previewColor = remember(uiState.widgetTextColor) {
    try {
        val argb = android.graphics.Color.parseColor(uiState.widgetTextColor)
        Color(
            red = android.graphics.Color.red(argb) / 255f,
            green = android.graphics.Color.green(argb) / 255f,
            blue = android.graphics.Color.blue(argb) / 255f,
            alpha = android.graphics.Color.alpha(argb) / 255f
        )
    } catch (e: IllegalArgumentException) {
        null
    }
}

OutlinedTextField(
    value = colorInput,
    onValueChange = { colorInput = it },
    label = { Text("Text color") },
    placeholder = { Text("#RRGGBB, #AARRGGBB, or name like red") },
    modifier = Modifier.fillMaxWidth(),
    singleLine = true,
    trailingIcon = {
        if (colorInput.isNotBlank()) {
            TextButton(onClick = { viewModel.setWidgetTextColor(colorInput.trim()) }) {
                Text("Set")
            }
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
        text = "Invalid color — enter a hex value or name like \"red\"",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.error
    )
}
```

**Step 3: Update imports**

Add any missing imports at the top of `SettingsScreen.kt`:

```kotlin
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
```

Note: `Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable)` requires the `MenuAnchorType` import. If the project's Compose Material3 version doesn't have `MenuAnchorType`, fall back to the deprecated `Modifier.menuAnchor()` with no arguments.

**Step 4: Compile check**

```bash
./gradlew :app:compileDebugKotlin
```

Expected: BUILD SUCCESSFUL

**Step 5: Run all tests**

```bash
./gradlew :app:testDebugUnitTest
```

Expected: all tests PASS

**Step 6: Commit**

```bash
git add app/src/main/java/com/example/simpleweather/ui/settings/SettingsScreen.kt
git commit -m "feat: replace interval radio buttons with dropdown, add text color picker"
```
