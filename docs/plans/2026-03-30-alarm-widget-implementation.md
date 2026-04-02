# Alarm Widget + Global Font Size — Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Add a home screen alarm widget showing the next scheduled system alarm, and a global font size setting that applies to all widgets.

**Architecture:** A new `AlarmWidget` (Glance) + `AlarmWidgetReceiver` (BroadcastReceiver) reads the next alarm via `AlarmManager.nextAlarmClock`, caches it to DataStore, and re-renders on `NEXT_ALARM_CLOCK_CHANGED`, `BOOT_COMPLETED`, `TIME_SET`, and `TIMEZONE_CHANGED`. Font size and text color are shared DataStore keys consumed by both widgets.

**Tech Stack:** Kotlin, Glance 1.1.1, DataStore Preferences, AlarmManager, BroadcastReceiver, Robolectric (tests).

---

### Task 1: Add DataStore keys

**Files:**
- Modify: `app/src/main/java/com/wassupluke/simpleweather/data/WeatherDataStore.kt:40-56`

**Step 1: Add the four new keys and default constant**

Add inside the `WeatherDataStore` object after line 55 (`WIDGET_DYNAMIC_COLOR`):

```kotlin
val FONT_SIZE = intPreferencesKey("font_size")
val ALARM_WIDGET_TAP_PACKAGE = stringPreferencesKey("alarm_widget_tap_package")
val ALARM_TEXT = stringPreferencesKey("alarm_text")

const val DEFAULT_FONT_SIZE = 48
```

**Step 2: Compile-check**

```bash
./gradlew :app:compileDebugKotlin
```
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add app/src/main/java/com/wassupluke/simpleweather/data/WeatherDataStore.kt
git commit -m "feat: add FONT_SIZE, ALARM_TEXT, ALARM_WIDGET_TAP_PACKAGE DataStore keys"
```

---

### Task 2: Add string resources

**Files:**
- Modify: `app/src/main/res/values/strings.xml`

**Step 1: Add new strings**

Find `strings.xml` and add (alongside the existing widget-related strings):

```xml
<string name="title_font_size">Widget font size</string>
<string name="title_alarm_widget">Alarm widget</string>
<string name="title_alarm_widget_tap_action">Alarm widget tap action</string>
<string name="alarm_widget_description">Simple Alarm Widget</string>
```

**Step 2: Compile-check**

```bash
./gradlew :app:compileDebugKotlin
```
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add app/src/main/res/values/strings.xml
git commit -m "feat: add string resources for alarm widget and font size"
```

---

### Task 3: Update SettingsUiState and ViewModel

**Files:**
- Modify: `app/src/main/java/com/wassupluke/simpleweather/ui/settings/SettingsViewModel.kt`
- Test: `app/src/test/java/com/wassupluke/simpleweather/ui/settings/SettingsViewModelTest.kt`

**Step 1: Write failing tests**

Open `SettingsViewModelTest.kt` and add at the end of the class (before the closing `}`):

```kotlin
@Test
fun `setFontSize writes to DataStore`() = runTest(testDispatcher) {
    val vm = SettingsViewModel(application, mockRepository, testDispatcher)
    backgroundScope.launch { vm.uiState.collect {} }
    advanceUntilIdle()
    vm.setFontSize(32)
    advanceUntilIdle()
    val state = vm.uiState.filter { it.fontSize == 32 }.first()
    assertEquals(32, state.fontSize)
}

@Test
fun `setAlarmWidgetTapPackage writes to DataStore`() = runTest(testDispatcher) {
    val vm = SettingsViewModel(application, mockRepository, testDispatcher)
    backgroundScope.launch { vm.uiState.collect {} }
    advanceUntilIdle()
    vm.setAlarmWidgetTapPackage("com.example.clock")
    advanceUntilIdle()
    val state = vm.uiState.filter { it.alarmWidgetTapPackage == "com.example.clock" }.first()
    assertEquals("com.example.clock", state.alarmWidgetTapPackage)
}

@Test
fun `fontSize defaults to 48`() = runTest(testDispatcher) {
    val vm = SettingsViewModel(application, mockRepository, testDispatcher)
    backgroundScope.launch { vm.uiState.collect {} }
    advanceUntilIdle()
    val state = vm.uiState.filter { it.fontSize == 48 }.first()
    assertEquals(48, state.fontSize)
}
```

**Step 2: Run tests to verify they fail**

```bash
./gradlew :app:testDebugUnitTest --tests "com.wassupluke.simpleweather.ui.settings.SettingsViewModelTest.setFontSize*"
./gradlew :app:testDebugUnitTest --tests "com.wassupluke.simpleweather.ui.settings.SettingsViewModelTest.setAlarmWidgetTapPackage*"
```
Expected: FAIL — `setFontSize` and `setAlarmWidgetTapPackage` do not exist yet.

**Step 3: Update SettingsUiState**

Change the `data class SettingsUiState` (lines 20-29) to add two new fields:

```kotlin
data class SettingsUiState(
    val useDeviceLocation: Boolean = false,
    val locationQuery: String = "",
    val locationDisplayName: String = "",
    val tempUnit: String = WeatherDataStore.DEFAULT_TEMP_UNIT,
    val updateIntervalMinutes: Int = WeatherDataStore.DEFAULT_INTERVAL_MINUTES,
    val widgetTextColor: String = "white",
    val widgetDynamicColor: Boolean = false,
    val widgetTapPackage: String = "",
    val fontSize: Int = WeatherDataStore.DEFAULT_FONT_SIZE,
    val alarmWidgetTapPackage: String = ""
)
```

**Step 4: Update prefsToUiState**

In `prefsToUiState` (lines 38-49), add two new fields to the returned `SettingsUiState`:

```kotlin
fontSize = prefs[WeatherDataStore.FONT_SIZE] ?: WeatherDataStore.DEFAULT_FONT_SIZE,
alarmWidgetTapPackage = prefs[WeatherDataStore.ALARM_WIDGET_TAP_PACKAGE] ?: ""
```

**Step 5: Add new ViewModel methods**

Add after `setWidgetTapPackage` (line 88):

```kotlin
fun setFontSize(size: Int) {
    viewModelScope.launch(dispatcher) {
        context.dataStore.edit { it[WeatherDataStore.FONT_SIZE] = size }
        WeatherWidget().updateAll(context)
        AlarmWidget().updateAll(context)
    }
}

fun setAlarmWidgetTapPackage(pkg: String) {
    viewModelScope.launch(dispatcher) {
        context.dataStore.edit { it[WeatherDataStore.ALARM_WIDGET_TAP_PACKAGE] = pkg }
        AlarmWidget().updateAll(context)
    }
}
```

Also update `setWidgetTextColor` and `setWidgetDynamicColor` to call `AlarmWidget().updateAll(context)` after the existing `WeatherWidget().updateAll(context)` call. Both widgets share the same color settings.

Add the import at the top of the file:
```kotlin
import com.wassupluke.simpleweather.widget.AlarmWidget
```

**Step 6: Run tests to verify they pass**

```bash
./gradlew :app:testDebugUnitTest --tests "com.wassupluke.simpleweather.ui.settings.SettingsViewModelTest"
```
Expected: All tests PASS

**Step 7: Commit**

```bash
git add app/src/main/java/com/wassupluke/simpleweather/ui/settings/SettingsViewModel.kt
git add app/src/test/java/com/wassupluke/simpleweather/ui/settings/SettingsViewModelTest.kt
git commit -m "feat: add fontSize and alarmWidgetTapPackage to SettingsViewModel"
```

---

### Task 4: Update WeatherWidget to use FONT_SIZE

**Files:**
- Modify: `app/src/main/java/com/wassupluke/simpleweather/widget/WeatherWidget.kt`

**Step 1: Read the font size from prefs**

In `WeatherWidget.provideGlance`, after the existing prefs reads (around line 38), add:

```kotlin
val fontSize = prefs[WeatherDataStore.FONT_SIZE] ?: WeatherDataStore.DEFAULT_FONT_SIZE
```

**Step 2: Pass fontSize to WeatherWidgetContent**

Change the `WeatherWidgetContent` call (lines 69-73) to pass fontSize:

```kotlin
WeatherWidgetContent(
    displayTemp = displayTemp,
    textColorProvider = textColorProvider,
    tapAction = tapAction,
    fontSize = fontSize
)
```

**Step 3: Update WeatherWidgetContent signature and body**

Change the function signature (line 84-88) to add the parameter:

```kotlin
private fun WeatherWidgetContent(
    displayTemp: String,
    textColorProvider: ColorProvider,
    tapAction: Action,
    fontSize: Int
)
```

Replace the hardcoded `fontSize = 48.sp` (line 98) with:

```kotlin
fontSize = fontSize.sp,
```

**Step 4: Compile-check**

```bash
./gradlew :app:compileDebugKotlin
```
Expected: BUILD SUCCESSFUL

**Step 5: Commit**

```bash
git add app/src/main/java/com/wassupluke/simpleweather/widget/WeatherWidget.kt
git commit -m "feat: WeatherWidget reads font size from DataStore instead of hardcoded 48sp"
```

---

### Task 5: Create AlarmWidget

**Files:**
- Create: `app/src/main/java/com/wassupluke/simpleweather/widget/AlarmWidget.kt`

**Step 1: Create the file**

```kotlin
package com.wassupluke.simpleweather.widget

import android.annotation.SuppressLint
import android.content.Context
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
import androidx.glance.GlanceTheme
import androidx.glance.text.*
import androidx.glance.unit.ColorProvider
import com.wassupluke.simpleweather.data.WeatherDataStore
import com.wassupluke.simpleweather.data.dataStore
import com.wassupluke.simpleweather.data.parseColorSafe
import com.wassupluke.simpleweather.data.resolveDynamicColor
import com.wassupluke.simpleweather.ui.MainActivity

class AlarmWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            GlanceTheme {
                val prefs by context.dataStore.data.collectAsState(initial = emptyPreferences())
                val alarmText = prefs[WeatherDataStore.ALARM_TEXT] ?: "No alarm"
                val colorString = prefs[WeatherDataStore.WIDGET_TEXT_COLOR] ?: "white"
                val dynamicColor = prefs.resolveDynamicColor()
                val fontSize = prefs[WeatherDataStore.FONT_SIZE] ?: WeatherDataStore.DEFAULT_FONT_SIZE

                val textColorProvider: ColorProvider = if (dynamicColor) {
                    GlanceTheme.colors.primary
                } else {
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

                val tapPackage = prefs[WeatherDataStore.ALARM_WIDGET_TAP_PACKAGE]
                val tapAction: Action = if (!tapPackage.isNullOrEmpty()) {
                    val launchIntent = context.packageManager.getLaunchIntentForPackage(tapPackage)
                    if (launchIntent?.component != null) actionStartActivity(launchIntent.component!!)
                    else actionStartActivity<MainActivity>()
                } else {
                    actionStartActivity<MainActivity>()
                }

                AlarmWidgetContent(
                    alarmText = alarmText,
                    textColorProvider = textColorProvider,
                    tapAction = tapAction,
                    fontSize = fontSize
                )
            }
        }
    }
}

@SuppressLint("RestrictedApi")
@Composable
private fun AlarmWidgetContent(
    alarmText: String,
    textColorProvider: ColorProvider,
    tapAction: Action,
    fontSize: Int
) {
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .clickable(tapAction),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = alarmText,
            style = TextStyle(
                fontSize = fontSize.sp,
                fontWeight = FontWeight.Normal,
                color = textColorProvider
            )
        )
    }
}
```

**Step 2: Compile-check**

```bash
./gradlew :app:compileDebugKotlin
```
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add app/src/main/java/com/wassupluke/simpleweather/widget/AlarmWidget.kt
git commit -m "feat: add AlarmWidget Glance widget"
```

---

### Task 6: Create AlarmWidgetReceiver

**Files:**
- Create: `app/src/main/java/com/wassupluke/simpleweather/widget/AlarmWidgetReceiver.kt`

**Step 1: Create the file**

```kotlin
package com.wassupluke.simpleweather.widget

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import androidx.datastore.preferences.core.edit
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import com.wassupluke.simpleweather.data.WeatherDataStore
import com.wassupluke.simpleweather.data.dataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

class AlarmWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = AlarmWidget()

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            AlarmManager.ACTION_NEXT_ALARM_CLOCK_CHANGED,
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED -> updateAlarmText(context)
        }
    }

    private fun updateAlarmText(context: Context) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                val nextAlarm = alarmManager.nextAlarmClock
                val alarmText = if (nextAlarm == null) {
                    "No alarm"
                } else {
                    DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(nextAlarm.triggerTime))
                }
                context.dataStore.edit { it[WeatherDataStore.ALARM_TEXT] = alarmText }
                AlarmWidget().updateAll(context)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
```

**Step 2: Compile-check**

```bash
./gradlew :app:compileDebugKotlin
```
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add app/src/main/java/com/wassupluke/simpleweather/widget/AlarmWidgetReceiver.kt
git commit -m "feat: add AlarmWidgetReceiver with alarm change broadcast handling"
```

---

### Task 7: Widget provider XML and AndroidManifest

**Files:**
- Create: `app/src/main/res/xml/alarm_widget_info.xml`
- Modify: `app/src/main/AndroidManifest.xml`

**Step 1: Create alarm_widget_info.xml**

```xml
<?xml version="1.0" encoding="utf-8"?>
<appwidget-provider
    xmlns:android="http://schemas.android.com/apk/res/android"
    android:minWidth="80dp"
    android:minHeight="80dp"
    android:targetCellWidth="2"
    android:targetCellHeight="1"
    android:updatePeriodMillis="0"
    android:description="@string/alarm_widget_description"
    android:resizeMode="horizontal|vertical" />
```

**Step 2: Update AndroidManifest.xml**

Add `RECEIVE_BOOT_COMPLETED` permission after the existing permissions (line 6):

```xml
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
```

Add the alarm widget receiver inside `<application>` after the existing `WeatherWidgetReceiver` block (after line 40):

```xml
<receiver
    android:name=".widget.AlarmWidgetReceiver"
    android:exported="true">
    <intent-filter>
        <action android:name="android.appwidget.action.APPWIDGET_UPDATE" />
        <action android:name="android.app.action.NEXT_ALARM_CLOCK_CHANGED" />
        <action android:name="android.intent.action.BOOT_COMPLETED" />
        <action android:name="android.intent.action.TIME_SET" />
        <action android:name="android.intent.action.TIMEZONE_CHANGED" />
    </intent-filter>
    <meta-data
        android:name="android.appwidget.provider"
        android:resource="@xml/alarm_widget_info" />
</receiver>
```

**Step 3: Full build to verify**

```bash
./gradlew assembleDebug
```
Expected: BUILD SUCCESSFUL

**Step 4: Commit**

```bash
git add app/src/main/res/xml/alarm_widget_info.xml app/src/main/AndroidManifest.xml
git commit -m "feat: register AlarmWidgetReceiver and alarm_widget_info provider in manifest"
```

---

### Task 8: Update SettingsScreen UI

**Files:**
- Modify: `app/src/main/java/com/wassupluke/simpleweather/ui/settings/SettingsScreen.kt`

**Step 1: Add new parameters to SettingsScreenContent**

Add two new lambda parameters to `SettingsScreenContent` after `onSetWidgetDynamicColor` (line 68):

```kotlin
onSetFontSize: (Int) -> Unit,
onSetAlarmWidgetTapPackage: (String) -> Unit,
```

**Step 2: Add showAlarmAppPicker state**

After the existing `var showAppPicker by remember { mutableStateOf(false) }` (line 79), add:

```kotlin
var showAlarmAppPicker by remember { mutableStateOf(false) }
```

**Step 3: Add font size slider**

Add the following block after the dynamic color / color section (after the `HorizontalDivider` at line 292, before the weather tap action section):

```kotlin
HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

Text(stringResource(R.string.title_font_size), style = MaterialTheme.typography.titleSmall)
Slider(
    value = uiState.fontSize.toFloat(),
    onValueChange = { onSetFontSize(it.toInt()) },
    valueRange = 12f..96f,
    steps = 83,
    modifier = Modifier.fillMaxWidth()
)
Text(
    text = "${uiState.fontSize}sp",
    style = MaterialTheme.typography.bodySmall,
    color = MaterialTheme.colorScheme.onSurfaceVariant
)
```

**Step 4: Add alarm widget section**

Add the following block after the weather widget tap action section (after the existing `showAppPicker` ModalBottomSheet block, before the final `Spacer`):

```kotlin
HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

Text(stringResource(R.string.title_alarm_widget), style = MaterialTheme.typography.titleSmall)

Text(
    stringResource(R.string.title_alarm_widget_tap_action),
    style = MaterialTheme.typography.titleSmall
)

val selectedAlarmAppInfo = remember(uiState.alarmWidgetTapPackage) {
    if (uiState.alarmWidgetTapPackage.isEmpty()) null
    else runCatching {
        context.packageManager.getApplicationInfo(uiState.alarmWidgetTapPackage, 0)
    }.getOrNull()
}

Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = Modifier
        .fillMaxWidth()
        .clickable { showAlarmAppPicker = true }
        .padding(vertical = 8.dp)
) {
    if (selectedAlarmAppInfo != null) {
        val icon by produceState<ImageBitmap?>(null, uiState.alarmWidgetTapPackage) {
            value = withContext(Dispatchers.IO) {
                runCatching {
                    context.packageManager
                        .getApplicationIcon(uiState.alarmWidgetTapPackage)
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
            text = selectedAlarmAppInfo.loadLabel(context.packageManager).toString(),
            modifier = Modifier.weight(1f)
        )
    } else if (uiState.alarmWidgetTapPackage.isNotEmpty()) {
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
        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

if (showAlarmAppPicker) {
    ModalBottomSheet(onDismissRequest = { showAlarmAppPicker = false }) {
        LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onSetAlarmWidgetTapPackage("")
                            showAlarmAppPicker = false
                        }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Spacer(Modifier.size(40.dp).padding(end = 12.dp))
                    Text(
                        text = stringResource(R.string.label_widget_tap_none),
                        modifier = Modifier.weight(1f)
                    )
                    if (uiState.alarmWidgetTapPackage.isEmpty()) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            items(installedApps) { entry ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onSetAlarmWidgetTapPackage(entry.pkg)
                            showAlarmAppPicker = false
                        }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    if (entry.icon != null) {
                        Image(
                            bitmap = entry.icon,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp).padding(end = 12.dp)
                        )
                    } else {
                        Spacer(Modifier.size(40.dp).padding(end = 12.dp))
                    }
                    Text(entry.label, modifier = Modifier.weight(1f))
                    if (entry.pkg == uiState.alarmWidgetTapPackage) {
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

**Step 5: Wire up SettingsScreen to ViewModel**

In the `SettingsScreen` composable (around line 442), add the two new lambda arguments to the `SettingsScreenContent` call:

```kotlin
onSetFontSize = { viewModel.setFontSize(it) },
onSetAlarmWidgetTapPackage = { viewModel.setAlarmWidgetTapPackage(it) },
```

**Step 6: Update all preview calls**

Each `SettingsScreenContent(...)` call in the preview functions needs the two new lambdas added:

```kotlin
onSetFontSize = {},
onSetAlarmWidgetTapPackage = {},
```

**Step 7: Full build and tests**

```bash
./gradlew assembleDebug
./gradlew :app:testDebugUnitTest
```
Expected: BUILD SUCCESSFUL, all tests PASS

**Step 8: Commit**

```bash
git add app/src/main/java/com/wassupluke/simpleweather/ui/settings/SettingsScreen.kt
git commit -m "feat: add font size slider and alarm widget tap-action picker to Settings"
```
