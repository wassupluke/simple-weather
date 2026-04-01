package com.wassupluke.wasseswidgets.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.LocationServices
import com.wassupluke.wasseswidgets.data.WeatherDataStore
import com.wassupluke.wasseswidgets.data.WeatherRepository
import com.wassupluke.wasseswidgets.data.dataStore
import com.wassupluke.wasseswidgets.ui.settings.SettingsScreen
import com.wassupluke.wasseswidgets.ui.settings.SettingsViewModel
import com.wassupluke.wasseswidgets.ui.theme.SimpleWeatherTheme
import com.wassupluke.wasseswidgets.worker.WorkScheduler
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
        enableEdgeToEdge()

        setContent {
            SimpleWeatherTheme {
                SettingsScreen(
                    viewModel = viewModel,
                    onLocationPermissionGranted = { fetchAndSaveDeviceLocation() }
                )
            }
        }

        // Schedule WorkManager on first launch (if location is already set)
        lifecycleScope.launch(Dispatchers.IO) {
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
