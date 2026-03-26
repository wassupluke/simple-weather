# Dynamic Color Widget Design

**Date:** 2026-03-25
**Feature:** Material You dynamic color support for the weather widget

## Goal

Allow the widget's temperature text color to follow the wallpaper-extracted Material You palette automatically, making the homescreen feel cohesive. A toggle lets users opt out and use their existing custom color instead.

## Approach

Use `GlanceTheme` from `androidx.glance.material3` (already a project dependency). On API 31+, it seeds from `WallpaperColors`; on older APIs it falls back to a baseline scheme. We override the pre-31 fallback to white to match the prior default.

## Data Layer

**New DataStore key:**
```kotlin
val WIDGET_DYNAMIC_COLOR = booleanPreferencesKey("widget_dynamic_color")
```

**Smart default logic** (evaluated at read-time, no migration needed):

| `WIDGET_DYNAMIC_COLOR` present | `WIDGET_TEXT_COLOR` present | Effective value |
|-|-|-|
| No | No | `true` (new install) |
| No | Yes | `false` (existing user, preserve custom color) |
| Yes | — | stored value |

`SettingsUiState` gains `widgetDynamicColor: Boolean`. `SettingsViewModel` exposes `setWidgetDynamicColor(Boolean)` writing to DataStore.

## Widget

`WeatherWidget.kt` wraps `WeatherWidgetContent` in `GlanceTheme { }`.

Color resolution:
- `dynamicColor = true`, API 31+: use `GlanceTheme.colors.primary`
- `dynamicColor = true`, pre-API 31: `ColorProvider(Color.White)` fallback
- `dynamicColor = false`: existing `ColorProvider(textColor)` path, unchanged

`WeatherWidgetContent` signature changes to accept `ColorProvider` directly instead of `androidx.compose.ui.graphics.Color`, letting Glance handle light/dark resolution natively.

## Settings UI

- Add a **"Dynamic color"** toggle row above the color picker, styled identically to the "Use device location" toggle
- Color picker row is hidden entirely (removed from composition with an `if` guard) when dynamic color is on
- The dynamic color toggle row is only rendered when `Build.VERSION.SDK_INT >= Build.VERSION_CODES.S` — on older devices the color picker remains always visible and dynamic color doesn't exist as a concept

## Files Changed

| File | Change |
|-|-|
| `data/WeatherDataStore.kt` | Add `WIDGET_DYNAMIC_COLOR` key |
| `ui/settings/SettingsViewModel.kt` | Add `widgetDynamicColor` to state, smart-default read logic, `setWidgetDynamicColor()` |
| `widget/WeatherWidget.kt` | Wrap in `GlanceTheme`, branch on `dynamicColor`, change `ColorProvider` handling |
| `ui/settings/SettingsScreen.kt` | Add dynamic color toggle row, conditional color picker visibility |

## Non-Goals

- No manual palette extraction (`WallpaperManager` API not used)
- No dark/light mode color override — `GlanceTheme.colors.primary` handles this automatically
- No UI on pre-API 31 devices
