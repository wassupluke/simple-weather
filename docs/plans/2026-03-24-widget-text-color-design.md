# Widget Text Color — Design Doc

**Date:** 2026-03-24
**Branch:** feature/initial-build

## Goal

Allow the user to set a custom text color for the home screen widget. Replace the existing day/night auto-switching color with a single user-configurable color, defaulting to white on first launch.

## Decisions

- Text color only (no background color in this iteration).
- Color is stored as a raw string (e.g. `"white"`, `"#FF0000"`, `"#80FF0000"`).
- `android.graphics.Color.parseColor()` handles parsing — supports `#RRGGBB`, `#AARRGGBB`, and named colors (red, blue, green, black, white, transparent, etc.).
- Transparency is supported via `#AARRGGBB` format.
- `DayNightColorProvider` is removed entirely.
- Default value is `"white"` (used when the key is absent from DataStore).
- The update interval radio group is replaced with an `ExposedDropdownMenuBox` to keep the settings screen to a single non-scrolling page.

## Data Layer

**`WeatherDataStore`**
- Add `WIDGET_TEXT_COLOR = stringPreferencesKey("widget_text_color")`
- Default: `"white"` (applied at read sites when key is absent)

**`SettingsUiState`**
- Add `widgetTextColor: String = "white"`

**`SettingsViewModel`**
- Add `setWidgetTextColor(raw: String)` — writes `WIDGET_TEXT_COLOR` to DataStore

**`WeatherWidget`**
- Read `WIDGET_TEXT_COLOR` from DataStore in `provideGlance`
- Parse with `android.graphics.Color.parseColor()` in try/catch; fall back to `Color.White` on failure
- Convert to `androidx.compose.ui.graphics.Color` via `Color(parsedInt)`
- Replace `DayNightColorProvider` with the resolved color

## UI (SettingsScreen)

**Update Interval** — replace 5 radio buttons with `ExposedDropdownMenuBox` (same 5 options: 15 min, 30 min, 1 hr, 3 hr, 6 hr).

**Widget Text Color** — new section after Update Interval:
- Section label: `"Widget Text Color"`
- `OutlinedTextField`, full-width, label `"Text color"`, placeholder `"#RRGGBB, #AARRGGBB, or name like red"`
- Trailing `TextButton("Set")` — saves to DataStore on click (same pattern as location field)
- Pre-populated on launch from `uiState.widgetTextColor`
- Color preview: full-width `Box`, ~40dp tall, filled with the currently *saved* color (from `uiState`), bordered with `MaterialTheme.colorScheme.outline`
- If the saved color string fails to parse, preview box shows `MaterialTheme.colorScheme.errorContainer` and a `bodySmall` error text below
- No live debounce — preview always reflects the saved value, not the typed draft

## Testing

- `WeatherDataStoreTest`: verify `WIDGET_TEXT_COLOR` absent → default `"white"` applied correctly
- `SettingsViewModelTest`: verify `setWidgetTextColor("red")` writes to DataStore and is reflected in `uiState.widgetTextColor`
- No new Glance widget unit tests (requires emulator)

## Out of Scope

- Background color customization
- Visual color wheel / palette picker
- Per-widget color (only one widget instance assumed)
