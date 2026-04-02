package com.wassupluke.widgets.data

import android.content.Context
import android.location.Geocoder
import androidx.datastore.preferences.core.edit
import com.wassupluke.widgets.data.api.NetworkModule
import com.wassupluke.widgets.data.api.OpenMeteoService
import java.util.Locale

class WeatherRepository(
    private val context: Context,
    private val weatherService: OpenMeteoService,
    private val geocodingService: OpenMeteoService
) {
    data class GeocodingResult(val lat: Float, val lon: Float, val displayName: String)

    companion object {
        fun create(context: Context): WeatherRepository = WeatherRepository(
            context = context,
            weatherService = NetworkModule.weatherService,
            geocodingService = NetworkModule.geocodingService
        )
    }

    suspend fun fetchAndCacheWeather(lat: Float, lon: Float) {
        val response = weatherService.getCurrentWeather(
            latitude = lat,
            longitude = lon,
            current = "temperature_2m",
            temperatureUnit = "celsius"
        )
        context.dataStore.edit { prefs ->
            prefs[WeatherDataStore.LAST_TEMP_CELSIUS] = response.current.temperatureCelsius
            prefs[WeatherDataStore.LAST_UPDATED_EPOCH] = System.currentTimeMillis()
        }
    }

    @Suppress("DEPRECATION")
    fun reverseGeocodeLocation(lat: Float, lon: Float): String {
        return try {
            val addresses = Geocoder(context, Locale.getDefault())
                .getFromLocation(lat.toDouble(), lon.toDouble(), 1)
            val addr = addresses?.firstOrNull()
            listOfNotNull(addr?.locality ?: addr?.subAdminArea, addr?.adminArea)
                .joinToString(", ")
                .ifEmpty { null } ?: "Current Location"
        } catch (e: Exception) {
            "Current Location"
        }
    }

    suspend fun geocodeLocation(query: String): GeocodingResult? {
        val response = geocodingService.searchLocation(
            query = query,
            count = 1,
            language = "en",
            format = "json"
        )
        val first = response.results?.firstOrNull() ?: return null
        val displayName = listOfNotNull(first.name, first.state, first.country).joinToString(", ")
        return GeocodingResult(
            lat = first.latitude.toFloat(),
            lon = first.longitude.toFloat(),
            displayName = displayName
        )
    }
}
