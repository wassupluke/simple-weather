package com.wassupluke.simpleweather.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WeatherDataStoreTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Before
    fun clearDataStore() {
        runBlocking { context.dataStore.edit { it.clear() } }
    }

    @Test
    fun `temp unit key is absent by default`() = runTest {
        val prefs = context.dataStore.data.first()
        assertNull(prefs[WeatherDataStore.TEMP_UNIT])
    }

    @Test
    fun `writing and reading last temp celsius`() = runTest {
        context.dataStore.edit { it[WeatherDataStore.LAST_TEMP_CELSIUS] = 21.5f }
        val prefs = context.dataStore.data.first()
        assertEquals(21.5f, prefs[WeatherDataStore.LAST_TEMP_CELSIUS] ?: 0f, 0.0f)
    }

    @Test
    fun `no location stored by default`() = runTest {
        val prefs = context.dataStore.data.first()
        assertNull(prefs[WeatherDataStore.LOCATION_LAT])
        assertNull(prefs[WeatherDataStore.LOCATION_LON])
    }

    @Test
    fun `widget text color key is absent by default`() = runTest {
        val prefs = context.dataStore.data.first()
        assertNull(prefs[WeatherDataStore.WIDGET_TEXT_COLOR])
    }
}
