# Widget Tap Launch — Design

**Date:** 2026-03-25
**Feature:** Tapping the widget launches the user's choice of installed app

## Summary

Add a setting that lets the user pick any installed app to launch when the widget is tapped. The choice persists in DataStore. If no app is chosen, the widget falls back to opening Simple Weather (current behavior).

## Data Layer

- New DataStore key: `stringPreferencesKey("widget_tap_package")` in `WeatherDataStore`
- Stores the chosen app's package name (e.g. `com.weather.app`), or absent if none chosen
- `SettingsUiState` gains `widgetTapPackage: String` (empty = none chosen)
- `SettingsViewModel` gains `setWidgetTapPackage(pkg: String)` — writes to DataStore, then calls `WeatherWidget().updateAll(context)` so the widget re-renders immediately

## Settings UI

New section at the bottom of `SettingsScreen` (after the color picker):

- `HorizontalDivider` (consistent with all other sections)
- Title: "Widget tap action" (`titleSmall`)
- Tappable row showing current selection: app icon (40dp, Coil) + app name, or "None (opens Simple Weather)" if unset, or "Selected app not found" if the saved package is no longer installed
- Tapping the row opens a `ModalBottomSheet`
- Bottom sheet contains a `LazyColumn` of all launchable apps, queried via `packageManager.queryIntentActivities(Intent(ACTION_MAIN).addCategory(CATEGORY_LAUNCHER), 0)`, sorted A–Z by label
- Each row: 40dp icon (Coil `rememberAsyncImagePainter` + `DrawablePainter`), app name, checkmark trailing icon on currently selected item
- Tapping a row calls `onSetWidgetTapPackage(pkg)` and dismisses the sheet

## Widget

In `WeatherWidget.provideGlance`, read `WIDGET_TAP_PACKAGE` from prefs and build the click action:

```kotlin
val tapPackage = prefs[WeatherDataStore.WIDGET_TAP_PACKAGE]
val tapAction = if (tapPackage != null) {
    val launchIntent = context.packageManager.getLaunchIntentForPackage(tapPackage)
    if (launchIntent != null) actionStartActivity(launchIntent)
    else actionStartActivity<MainActivity>()
} else {
    actionStartActivity<MainActivity>()
}
```

Pass `tapAction` into `WeatherWidgetContent` and apply it to the `Box`'s `.clickable(tapAction)`.

## Error Handling & Edge Cases

| Case | Behavior |
|-|-|
| App uninstalled after selection | `getLaunchIntentForPackage` returns null → falls back to MainActivity silently; DataStore not auto-cleared |
| Unresolvable package shown in Settings | Row label shows "Selected app not found" |
| No apps returned by query | LazyColumn is empty — acceptable |
| Icon load failure | Coil shows nothing; app name still readable |
| Widget update on selection | `updateAll(context)` called immediately in ViewModel |

## Testing

- `SettingsViewModelTest`: verify `setWidgetTapPackage(pkg)` writes `WIDGET_TAP_PACKAGE` to DataStore and `uiState.widgetTapPackage` reflects the new value
- No widget unit test — null-check logic is trivial
- No UI test — no existing UI test baseline; Robolectric doesn't exercise `ModalBottomSheet` well

## Dependencies

No new dependencies required. Coil is already used for icon loading (verify in `libs.versions.toml`); if not present, add `io.coil-kt.coil3:coil-compose`.
