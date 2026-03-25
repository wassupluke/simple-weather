package com.wassupluke.simpleweather.data

import android.content.Context
import android.graphics.Color as AndroidColor
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
    const val DEFAULT_TEMP_UNIT = "C"
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
}
