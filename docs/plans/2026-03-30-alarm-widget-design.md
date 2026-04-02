# Alarm Widget + Global Font Size — Design

**Date:** 2026-03-30
**Status:** Approved

## Overview

Add a new home screen widget that displays the next scheduled system alarm. Extend the Settings page with a global font size slider that applies to all widgets. Each widget gets its own independent tap-target app setting.

## DataStore Changes (`WeatherDataStore.kt`)

New keys:

| Key | Type | Default | Purpose |
|-|-|-|-|
| `FONT_SIZE` | `intPreferencesKey("font_size")` | 48 | Global font size for all widgets |
| `ALARM_WIDGET_TAP_PACKAGE` | `stringPreferencesKey("alarm_widget_tap_package")` | `""` | Tap target for alarm widget (empty = MainActivity) |
| `ALARM_TEXT` | `stringPreferencesKey("alarm_text")` | `"No alarm"` | Cached alarm display string |

Migration: rename `WIDGET_TAP_PACKAGE` → `WEATHER_WIDGET_TAP_PACKAGE`. On first read, if old key exists, copy value to new key and clear old key.

`WIDGET_TEXT_COLOR` and `WIDGET_DYNAMIC_COLOR` remain unchanged — already global.

`WeatherWidget` hardcoded `48.sp` replaced with `FONT_SIZE` value.

## New Files

### `widget/AlarmWidget.kt`
- Extends `GlanceAppWidget`
- Reads `WIDGET_TEXT_COLOR`, `WIDGET_DYNAMIC_COLOR`, `FONT_SIZE`, `ALARM_WIDGET_TAP_PACKAGE`, `ALARM_TEXT` from DataStore via `collectAsState()`
- Displays `ALARM_TEXT` using same color/dynamic color logic as `WeatherWidget` (reuse `parseColorSafe()`)
- Tap action: `actionStartActivity` using `ALARM_WIDGET_TAP_PACKAGE`; empty/null falls back to MainActivity
- Wrapped in `GlanceTheme`

### `widget/AlarmWidgetReceiver.kt`
- Extends `GlanceAppWidgetReceiver`
- Overrides `onReceive()` to handle:
  - `android.app.action.NEXT_ALARM_CLOCK_CHANGED`
  - `android.intent.action.BOOT_COMPLETED`
  - `android.intent.action.TIME_SET`
  - `android.intent.action.TIMEZONE_CHANGED`
  - `android.appwidget.action.APPWIDGET_UPDATE` (passes to super)
- On alarm-related broadcasts: reads `AlarmManager.nextAlarmClock`, formats with `DateFormat.getTimeInstance(DateFormat.SHORT)` (respects device 12/24h), saves to `ALARM_TEXT` in DataStore, calls `AlarmWidget().updateAll(context)`
- Uses `goAsync()` for the coroutine work

## Settings Changes

### `SettingsUiState`
Add fields: `fontSize: Int = 48`, `alarmWidgetTapPackage: String = ""`

### `SettingsViewModel`
- `onSetFontSize(size: Int)` — writes `FONT_SIZE`; calls `WeatherWidget().updateAll()` and `AlarmWidget().updateAll()`
- `onSetAlarmWidgetTapPackage(pkg: String)` — writes `ALARM_WIDGET_TAP_PACKAGE`; calls `AlarmWidget().updateAll()`
- `onSetWidgetTapPackage` → renamed to `onSetWeatherWidgetTapPackage`

### `SettingsScreen`
- Font size slider (range 12–96, step 1, default 48) added to the shared widget settings section near existing color controls
- New "Alarm Widget" section with app-picker for `ALARM_WIDGET_TAP_PACKAGE`

## Manifest Changes (`AndroidManifest.xml`)

New receiver:
```xml
<receiver android:name=".widget.AlarmWidgetReceiver" android:exported="true">
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

New file: `res/xml/alarm_widget_info.xml` — widget provider metadata (updatePeriodMillis=0, min dimensions, description).

Add `<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />` if not present.

## Update Architecture

```
System alarm change
        │
        ▼
AlarmWidgetReceiver.onReceive()
        │
        ├── AlarmManager.nextAlarmClock
        ├── format with DateFormat.SHORT
        ├── save to ALARM_TEXT in DataStore
        └── AlarmWidget().updateAll(context)
                │
                ▼
        Glance re-renders from DataStore state
```

## Files to Create/Modify

| Action | File |
|-|-|
| Modify | `data/WeatherDataStore.kt` |
| Modify | `widget/WeatherWidget.kt` |
| Modify | `ui/settings/SettingsViewModel.kt` |
| Modify | `ui/settings/SettingsScreen.kt` |
| Modify | `ui/MainActivity.kt` (rename tap package references) |
| Modify | `AndroidManifest.xml` |
| Create | `widget/AlarmWidget.kt` |
| Create | `widget/AlarmWidgetReceiver.kt` |
| Create | `res/xml/alarm_widget_info.xml` |
