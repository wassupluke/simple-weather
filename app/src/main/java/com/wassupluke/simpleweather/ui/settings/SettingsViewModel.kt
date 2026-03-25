package com.wassupluke.simpleweather.ui.settings

import android.app.Application
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.wassupluke.simpleweather.data.WeatherDataStore
import com.wassupluke.simpleweather.data.WeatherRepository
import com.wassupluke.simpleweather.data.dataStore
import com.wassupluke.simpleweather.data.resolveDynamicColor
import androidx.glance.appwidget.updateAll
import com.wassupluke.simpleweather.widget.WeatherWidget
import com.wassupluke.simpleweather.worker.WorkScheduler
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch


data class SettingsUiState(
    val useDeviceLocation: Boolean = false,
    val locationQuery: String = "",
    val locationDisplayName: String = "",
    val tempUnit: String = WeatherDataStore.DEFAULT_TEMP_UNIT,
    val updateIntervalMinutes: Int = WeatherDataStore.DEFAULT_INTERVAL_MINUTES,
    val widgetTextColor: String = "white",
    val widgetDynamicColor: Boolean = false,
    val widgetTapPackage: String = ""
)

class SettingsViewModel(
    application: Application,
    private val repository: WeatherRepository,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : AndroidViewModel(application) {
    private val context: Application = application

    private fun prefsToUiState(prefs: androidx.datastore.preferences.core.Preferences): SettingsUiState {
        return SettingsUiState(
            useDeviceLocation = prefs[WeatherDataStore.USE_DEVICE_LOCATION] ?: false,
            locationQuery = prefs[WeatherDataStore.LOCATION_QUERY] ?: "",
            locationDisplayName = prefs[WeatherDataStore.LOCATION_DISPLAY_NAME] ?: "",
            tempUnit = prefs[WeatherDataStore.TEMP_UNIT] ?: WeatherDataStore.DEFAULT_TEMP_UNIT,
            updateIntervalMinutes = prefs[WeatherDataStore.UPDATE_INTERVAL_MINUTES] ?: WeatherDataStore.DEFAULT_INTERVAL_MINUTES,
            widgetTextColor = prefs[WeatherDataStore.WIDGET_TEXT_COLOR] ?: "white",
            widgetDynamicColor = prefs.resolveDynamicColor(),
            widgetTapPackage = prefs[WeatherDataStore.WIDGET_TAP_PACKAGE] ?: ""
        )
    }

    val uiState: StateFlow<SettingsUiState> = context.dataStore.data.map { prefs ->
        prefsToUiState(prefs)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsUiState())

    fun setTempUnit(unit: String) {
        viewModelScope.launch(dispatcher) {
            context.dataStore.edit { it[WeatherDataStore.TEMP_UNIT] = unit }
            WeatherWidget().updateAll(context)
        }
    }

    fun setUpdateInterval(minutes: Int) {
        viewModelScope.launch(dispatcher) {
            context.dataStore.edit { it[WeatherDataStore.UPDATE_INTERVAL_MINUTES] = minutes }
            scheduleIfLocationPresent()
        }
    }

    fun setWidgetTextColor(raw: String) {
        viewModelScope.launch(dispatcher) {
            context.dataStore.edit { it[WeatherDataStore.WIDGET_TEXT_COLOR] = raw }
            WeatherWidget().updateAll(context)
        }
    }

    fun setWidgetDynamicColor(enabled: Boolean) {
        viewModelScope.launch(dispatcher) {
            context.dataStore.edit { it[WeatherDataStore.WIDGET_DYNAMIC_COLOR] = enabled }
            WeatherWidget().updateAll(context)
        }
    }

    fun setWidgetTapPackage(pkg: String) {
        viewModelScope.launch(dispatcher) {
            context.dataStore.edit { it[WeatherDataStore.WIDGET_TAP_PACKAGE] = pkg }
            WeatherWidget().updateAll(context)
        }
    }

    fun setUseDeviceLocation(use: Boolean) {
        viewModelScope.launch(dispatcher) {
            context.dataStore.edit { it[WeatherDataStore.USE_DEVICE_LOCATION] = use }
            if (!use) {
                WorkScheduler.cancel(context)
            }
        }
    }

    private suspend fun scheduleIfLocationPresent() {
        val prefs = context.dataStore.data.first()
        val lat = prefs[WeatherDataStore.LOCATION_LAT]
        val lon = prefs[WeatherDataStore.LOCATION_LON]
        val intervalMinutes = prefs[WeatherDataStore.UPDATE_INTERVAL_MINUTES]
            ?: WeatherDataStore.DEFAULT_INTERVAL_MINUTES
        if (lat != null && lon != null) WorkScheduler.schedule(context, intervalMinutes)
    }

    fun resolveAndSaveLocation(query: String) {
        viewModelScope.launch(dispatcher) {
            val result = repository.geocodeLocation(query) ?: return@launch
            context.dataStore.edit { prefs ->
                prefs[WeatherDataStore.LOCATION_LAT] = result.lat
                prefs[WeatherDataStore.LOCATION_LON] = result.lon
                prefs[WeatherDataStore.LOCATION_DISPLAY_NAME] = result.displayName
                prefs[WeatherDataStore.LOCATION_QUERY] = query
            }
            scheduleIfLocationPresent()
            try {
                repository.fetchAndCacheWeather(lat = result.lat, lon = result.lon)
                WeatherWidget().updateAll(context)
            } catch (e: Exception) { /* best effort — WorkManager will retry on schedule */ }
        }
    }

    fun saveDeviceLocation(lat: Float, lon: Float) {
        viewModelScope.launch(dispatcher) {
            val displayName = repository.reverseGeocodeLocation(lat, lon)
            context.dataStore.edit { prefs ->
                prefs[WeatherDataStore.LOCATION_LAT] = lat
                prefs[WeatherDataStore.LOCATION_LON] = lon
                prefs[WeatherDataStore.LOCATION_DISPLAY_NAME] = displayName
            }
            scheduleIfLocationPresent()
            try {
                repository.fetchAndCacheWeather(lat = lat, lon = lon)
                WeatherWidget().updateAll(context)
            } catch (e: Exception) { /* best effort — WorkManager will retry on schedule */ }
        }
    }
}
