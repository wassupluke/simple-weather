package com.wassupluke.simpleweather.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.test.core.app.ApplicationProvider
import com.wassupluke.simpleweather.data.api.*
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlinx.coroutines.runBlocking

@RunWith(RobolectricTestRunner::class)
class WeatherRepositoryTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val mockWeatherService = mockk<OpenMeteoService>()
    private val mockGeocodingService = mockk<OpenMeteoService>()

    private fun repo() = WeatherRepository(context, mockWeatherService, mockGeocodingService)

    @Before
    fun clearDataStore() {
        runBlocking { context.dataStore.edit { it.clear() } }
    }

    @Test
    fun `fetchAndCacheWeather stores temperature in DataStore`() = runTest {
        coEvery {
            mockWeatherService.getCurrentWeather(any(), any(), any(), any())
        } returns WeatherResponse(CurrentWeather("2024-01-01T12:00", 21.5f))

        repo().fetchAndCacheWeather(lat = 40.71f, lon = -74.01f)

        val prefs = context.dataStore.data.first()
        assertEquals(21.5f, prefs[WeatherDataStore.LAST_TEMP_CELSIUS] ?: 0f, 0.01f)
        assertTrue((prefs[WeatherDataStore.LAST_UPDATED_EPOCH] ?: 0L) > 0L)
    }

    @Test
    fun `geocodeLocation returns lat-lon and display name on success`() = runTest {
        coEvery { mockGeocodingService.searchLocation(any(), any(), any(), any()) } returns
            GeocodingResponse(listOf(GeocodingResult("Portland", 45.52, -122.68, "United States", "Oregon")))

        val result = repo().geocodeLocation("Portland")

        assertNotNull(result)
        assertEquals(45.52f, result!!.lat, 0.01f)
        assertEquals(-122.68f, result.lon, 0.01f)
        assertEquals("Portland, Oregon, United States", result.displayName)
    }

    @Test
    fun `geocodeLocation display name omits state when absent`() = runTest {
        coEvery { mockGeocodingService.searchLocation(any(), any(), any(), any()) } returns
            GeocodingResponse(listOf(GeocodingResult("Austin", 30.27, -97.74, country = "United States", state = null)))

        val result = repo().geocodeLocation("Austin")

        assertEquals("Austin, United States", result!!.displayName)
    }

    @Test
    fun `geocodeLocation display name is just city when state and country absent`() = runTest {
        coEvery { mockGeocodingService.searchLocation(any(), any(), any(), any()) } returns
            GeocodingResponse(listOf(GeocodingResult("Springfield", 39.80, -89.64, country = null, state = null)))

        val result = repo().geocodeLocation("Springfield")

        assertEquals("Springfield", result!!.displayName)
    }

    @Test
    fun `geocodeLocation returns null when no results`() = runTest {
        coEvery { mockGeocodingService.searchLocation(any(), any(), any(), any()) } returns
            GeocodingResponse(emptyList())

        val result = repo().geocodeLocation("XYZNotAPlace")
        assertNull(result)
    }
}
