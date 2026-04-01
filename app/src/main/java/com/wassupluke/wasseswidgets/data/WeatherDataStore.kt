package com.wassupluke.wasseswidgets.data

import android.content.Context
import android.graphics.Color as AndroidColor
import android.os.Build
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore

private val extendedColorNames = mapOf(
    "pink" to "#FFC0CB",
    "orange" to "#FFA500",
    "brown" to "#A52A2A",
    "violet" to "#EE82EE",
    "gold" to "#FFD700",
    "coral" to "#FF7F50",
    "crimson" to "#DC143C",
    "turquoise" to "#40E0D0",
    "lavender" to "#E6E6FA",
    "indigo" to "#4B0082"
)

fun Preferences.resolveDynamicColor(): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return false
    val stored = this[WeatherDataStore.WIDGET_DYNAMIC_COLOR]
    return stored ?: (this[WeatherDataStore.WIDGET_TEXT_COLOR] == null)
}

fun parseColorSafe(colorString: String): Int? {
    val resolved = extendedColorNames[colorString.trim().lowercase()] ?: colorString
    return try {
        AndroidColor.parseColor(resolved)
    } catch (e: IllegalArgumentException) {
        null
    }
}

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "weather_settings")

object WeatherDataStore {
    const val DEFAULT_TEMP_UNIT = "F"
    const val DEFAULT_INTERVAL_MINUTES = 60

    val USE_DEVICE_LOCATION = booleanPreferencesKey("use_device_location")
    val LOCATION_LAT = floatPreferencesKey("location_lat")
    val LOCATION_LON = floatPreferencesKey("location_lon")
    val LOCATION_DISPLAY_NAME = stringPreferencesKey("location_display_name")
    val LOCATION_QUERY = stringPreferencesKey("location_query")
    val TEMP_UNIT = stringPreferencesKey("temp_unit")
    val UPDATE_INTERVAL_MINUTES = intPreferencesKey("update_interval_minutes")
    val LAST_TEMP_CELSIUS = floatPreferencesKey("last_temp_celsius")
    val LAST_UPDATED_EPOCH = longPreferencesKey("last_updated_epoch")
    val WIDGET_TEXT_COLOR = stringPreferencesKey("widget_text_color")
    val WIDGET_TAP_PACKAGE = stringPreferencesKey("widget_tap_package")
    val WIDGET_DYNAMIC_COLOR = booleanPreferencesKey("widget_dynamic_color")
    val FONT_SIZE = intPreferencesKey("font_size")
    val ALARM_WIDGET_TAP_PACKAGE = stringPreferencesKey("alarm_widget_tap_package")
    val ALARM_TEXT = stringPreferencesKey("alarm_text")

    const val DEFAULT_FONT_SIZE = 48
}
