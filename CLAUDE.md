# CLAUDE.md

This file orients a new Claude Code session for this repository.

## Project Overview

This is a simple app to provide at-a-glance widgets to show the current temperature and the next upcoming alarm. It is written in Kotlin, Jetpack Compose, and Glance. The `MainActivity` for the app is a single settings page which controls how the widgets look and function. The `MainActivity` follows the device preference for light/dark theme. The user can set the widget font size to anywhere from 12-64sp. Open-Meteo provides weather and geocoding (no API key required).


## Design decisions

- Nothing is to be built unless a unit test has first been written to validate core functionality against.
- Only check for temperature updates if the user has the temperature widget placed and active on their homescreen.
- Temperature is always stored as Celsius (`LAST_TEMP_CELSIUS`); F/C conversion happens at display time only.
- Each widget updates independent of the other. e.g., the upcoming alarm widget only updates itself when triggered, and the temp widget only updates itself on the user-defined schedule.
- Each widget has it's own separate touch target.
- All widgets share the same font size and color/theme defined by the user in the app's settings activity.
  - The status bar font color must be of contrasting color to the `MainActivity` background color for visibility.
- The upcoming alarm widget must update itself any time device alarms change.
- The `MainActivity` screen is comprised of three sections:
  1. "All widget settings" which affect all widgets. Following this section are sections for settings on individual widgets.
    a. "Dynamic color" as a toggle. Defaults to off. More defaults are discussed further down this list. When on, uses the Compose Material 3 Primary color. When off, reveals a color picker and associated single-line text box. The text box is for displaying the ARGB hex of the currently selected color, and serves as an input field for the user to manually input an RGB or ARGB hex value. If the user enters a valid hex value in the input field, the color picker updates to also reflect the color input.
    b. "Font size" as a slider that shows the currently selected value to the right of the slider
  2. "Temperature widget settings"
    a. "Use device location" as a toggle, where the off state reveals a single-line text field that requires a zip code or City, State input
    b. "Temperature unit" as an either-or button toggle like [ °C | °F ]
    c. "Update frequency" with interval options as a dropdown list of 15 min, 30 min, 1 hr, 3 hr, or 6 hr.
    d. "Widget tap action" where the user selects one of any of the installed apps on their device (excluding deep system apps) as the touch target.
- All settings should be set automatically when changed by the user, without requiring any "Save" or "Set" action from the user.
- Default widget text color is white. Default widget font size is 30sp. Default widget touch target is to launch `MainActivity`.
- Default values are also fallback values.
- All strings are abstracted to xml string resource files.

### Temperature widget layout

This widget is simple, showing simply the numeric value of the current temperature in the unit the user selected in `MainActivity`, followed by a simple degree symbol.

### Upcoming alarm widget layout

The upcoming alarm widget has three elements in a row:
1. Google Alarm icon
  - ```kotlin
    dependencies {
        implementation("androidx.compose.material:material-icons-extended:$compose_version")
    }
  - ```kotlin
    import androidx.compose.material.Icon
    import androidx.compose.material.icons.Icons
    import androidx.compose.material.icons.filled.Alarm
    import androidx.compose.ui.graphics.Color

    @Composable
    fun AlarmIcon() {
        Icon(
            imageVector = Icons.Filled.Alarm,
            contentDescription = "Alarm Clock",
            tint = Color.Black
        )
    }
    ```
2. A 3-letter day of the week short code text. First letter capitalized (e.g., Tue)
3. The time of the upcoming alarm. Formatted per device locale and time format settings for 12-hr vs 24-hr. If 12-hr, must add the appropriate "AM"/"PM" suffix separated from the time by a single space.

## Build & Test Commands

Before building widgets, review all Glance documentation instructions in docs/glance-documentation-links.md.

TDD is mandatory.

Run all commands from the repo root.

| Command | Purpose |
|-|-|
| `./gradlew assembleDebug` | Full debug build |
| `./gradlew :app:compileDebugKotlin` | Compile-check only (faster) |
| `./gradlew :app:testDebugUnitTest` | All unit tests |
| `./gradlew :app:testDebugUnitTest --tests "com.wassupluke.widgets.data.WeatherRepositoryTest"` | Single test class |

`./gradlew :app:test` is not supported. All unit tests use Robolectric — no emulator needed.

## Testing Conventions

- `@RunWith(RobolectricTestRunner::class)` + `ApplicationProvider.getApplicationContext()` on all test classes.
- Mock `OpenMeteoService` with MockK; pass separate mocks as `weatherService` and `geocodingService`.
