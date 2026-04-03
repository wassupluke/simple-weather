package com.wassupluke.widgets.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import android.graphics.Color
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.LocationServices
import com.wassupluke.widgets.data.WeatherDataStore
import com.wassupluke.widgets.data.WeatherRepository
import com.wassupluke.widgets.data.dataStore
import com.wassupluke.widgets.ui.settings.SettingsScreen
import com.wassupluke.widgets.ui.settings.SettingsViewModel
import com.wassupluke.widgets.ui.theme.SimpleWeatherTheme
import com.wassupluke.widgets.widget.AlarmWidget
import com.wassupluke.widgets.widget.WeatherWidget
import com.wassupluke.widgets.worker.WorkScheduler
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: SettingsViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val repo = WeatherRepository.create(application)
                @Suppress("UNCHECKED_CAST")
                return SettingsViewModel(application, repo) as T
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        )

        setContent {
            SimpleWeatherTheme {
                SettingsScreen(
                    viewModel = viewModel,
                    onLocationPermissionGranted = { fetchAndSaveDeviceLocation() }
                )
            }
        }

        lifecycleScope.launch(Dispatchers.IO) {
            // Force both widgets to re-render, re-establishing correct Glance
            // state mapping in case it drifted after a force-stop or update
            WeatherWidget().updateAll(applicationContext)
            AlarmWidget().updateAll(applicationContext)

            // Schedule WorkManager (if location is already set)
            val prefs = applicationContext.dataStore.data.first()
            val lat = prefs[WeatherDataStore.LOCATION_LAT]
            val lon = prefs[WeatherDataStore.LOCATION_LON]
            val intervalMinutes = prefs[WeatherDataStore.UPDATE_INTERVAL_MINUTES] ?: WeatherDataStore.DEFAULT_INTERVAL_MINUTES

            if (lat != null && lon != null) {
                WorkScheduler.schedule(applicationContext, intervalMinutes)
            }
        }
    }

    /** Called from SettingsScreen when location permission is granted */
    @SuppressLint("MissingPermission")
    fun fetchAndSaveDeviceLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) return
        val fusedClient = LocationServices.getFusedLocationProviderClient(this)
        fusedClient.lastLocation.addOnSuccessListener { location ->
            location?.let {
                viewModel.saveDeviceLocation(it.latitude.toFloat(), it.longitude.toFloat())
            }
        }
    }
}
