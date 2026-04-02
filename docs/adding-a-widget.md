# Adding a New Widget

## String Resources (`res/values/strings.xml`)

Follow the `<where>_<what>` convention. Each widget gets a label (shown in the picker) and a description (shown beneath it).

```xml
<!-- Widget picker -->
<string name="widget_<name>_label">Human Name</string>
<string name="widget_<name>_description">Shows X on your home screen</string>

<!-- Settings section title -->
<string name="settings_<name>_tap_title"><Name> widget tap action</string>
```

Shared strings (`settings_tap_none_label`, `settings_tap_app_missing_label`, appearance keys) are reused — don't duplicate them.

## Files to Create

| File | Purpose |
|-|-|
| `widget/<Name>Widget.kt` | Glance widget UI |
| `widget/<Name>WidgetReceiver.kt` | `GlanceAppWidgetReceiver` + update trigger |
| `res/xml/<name>_widget_info.xml` | Widget provider metadata |

### `<name>_widget_info.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<appwidget-provider
    xmlns:android="http://schemas.android.com/apk/res/android"
    android:minWidth="80dp"
    android:minHeight="80dp"
    android:targetCellWidth="2"
    android:targetCellHeight="1"
    android:updatePeriodMillis="0"
    android:description="@string/widget_<name>_description"
    android:resizeMode="horizontal|vertical" />
```

### `<Name>Widget.kt`

Mirror `WeatherWidget.kt` or `AlarmWidget.kt`. All widgets share these DataStore keys:
- `WIDGET_TEXT_COLOR` + `WIDGET_DYNAMIC_COLOR` + `resolveDynamicColor()` — color
- `FONT_SIZE` / `DEFAULT_FONT_SIZE` — font size
- Widget-specific tap key: `<NAME>_WIDGET_TAP_PACKAGE`

### `<Name>WidgetReceiver.kt`

Extend `GlanceAppWidgetReceiver`. Override `onReceive` and call your update function for both `AppWidgetManager.ACTION_APPWIDGET_UPDATE` (first placement) and any domain-specific system broadcasts.

Use `goAsync()` for any async work:

```kotlin
private fun updateContent(context: Context) {
    val pendingResult = goAsync()
    CoroutineScope(Dispatchers.IO).launch {
        try {
            // read data, write to DataStore, call <Name>Widget().updateAll(context)
        } finally {
            pendingResult.finish()
        }
    }
}
```

## Files to Modify

### `AndroidManifest.xml`

```xml
<receiver
    android:name=".widget.<Name>WidgetReceiver"
    android:exported="true"
    android:label="@string/widget_<name>_label">
    <intent-filter>
        <action android:name="android.appwidget.action.APPWIDGET_UPDATE" />
        <!-- add any domain-specific broadcasts here -->
    </intent-filter>
    <meta-data
        android:name="android.appwidget.provider"
        android:resource="@xml/<name>_widget_info" />
</receiver>
```

Add any required permissions (e.g. `RECEIVE_BOOT_COMPLETED`) if new broadcasts are needed.

### `WeatherDataStore.kt`

Add a tap-package key and any widget-specific cache keys:

```kotlin
val <NAME>_WIDGET_TAP_PACKAGE = stringPreferencesKey("<name>_widget_tap_package")
val <NAME>_DATA = stringPreferencesKey("<name>_data")  // if caching display text
```

### `SettingsViewModel.kt`

Add to `SettingsUiState`:
```kotlin
val <name>WidgetTapPackage: String = ""
```

Add to `prefsToUiState`:
```kotlin
<name>WidgetTapPackage = prefs[WeatherDataStore.<NAME>_WIDGET_TAP_PACKAGE] ?: ""
```

Add method:
```kotlin
fun set<Name>WidgetTapPackage(pkg: String) {
    viewModelScope.launch(dispatcher) {
        context.dataStore.edit { it[WeatherDataStore.<NAME>_WIDGET_TAP_PACKAGE] = pkg }
        <Name>Widget().updateAll(context)
    }
}
```

Also add `<Name>Widget().updateAll(context)` to `setFontSize`, `setWidgetTextColor`, and `setWidgetDynamicColor` so shared appearance settings propagate to the new widget.

### `SettingsScreen.kt`

Add `onSet<Name>WidgetTapPackage: (String) -> Unit` parameter to `SettingsScreenContent`. Add a new section (after the existing alarm widget section) using `settings_<name>_tap_title` and reusing the existing app-picker pattern.
