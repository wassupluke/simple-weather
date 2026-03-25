package com.wassupluke.simpleweather.ui.settings

import android.app.Application
import android.os.Build
import androidx.datastore.preferences.core.edit
import androidx.test.core.app.ApplicationProvider
import com.wassupluke.simpleweather.data.WeatherDataStore
import com.wassupluke.simpleweather.data.WeatherRepository
import com.wassupluke.simpleweather.data.dataStore
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assume.assumeTrue
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val application: Application = ApplicationProvider.getApplicationContext()
    private val mockRepository = mockk<WeatherRepository>(relaxed = true)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        runBlocking { application.dataStore.edit { it.clear() } }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `setTempUnit writes to DataStore`() = runTest(testDispatcher) {
        val vm = SettingsViewModel(application, mockRepository, testDispatcher)
        // Subscribe to uiState so WhileSubscribed activates the upstream DataStore flow
        backgroundScope.launch { vm.uiState.collect {} }
        advanceUntilIdle()
        vm.setTempUnit("F")
        advanceUntilIdle()
        val state = vm.uiState.filter { it.tempUnit == "F" }.first()
        assertEquals("F", state.tempUnit)
    }

    @Test
    fun `setUpdateInterval writes to DataStore`() = runTest(testDispatcher) {
        val vm = SettingsViewModel(application, mockRepository, testDispatcher)
        // Subscribe to uiState so WhileSubscribed activates the upstream DataStore flow
        backgroundScope.launch { vm.uiState.collect {} }
        advanceUntilIdle()
        vm.setUpdateInterval(30)
        advanceUntilIdle()
        val state = vm.uiState.filter { it.updateIntervalMinutes == 30 }.first()
        assertEquals(30, state.updateIntervalMinutes)
    }

    @Test
    fun `setWidgetTextColor writes to DataStore`() = runTest(testDispatcher) {
        val vm = SettingsViewModel(application, mockRepository, testDispatcher)
        backgroundScope.launch { vm.uiState.collect {} }
        advanceUntilIdle()
        vm.setWidgetTextColor("red")
        advanceUntilIdle()
        val state = vm.uiState.filter { it.widgetTextColor == "red" }.first()
        assertEquals("red", state.widgetTextColor)
    }

    @Test
    fun `widgetTextColor defaults to white when key absent`() = runTest(testDispatcher) {
        val vm = SettingsViewModel(application, mockRepository, testDispatcher)
        backgroundScope.launch { vm.uiState.collect {} }
        advanceUntilIdle()
        val state = vm.uiState.first()
        assertEquals("white", state.widgetTextColor)
    }

    @Test
    fun `setWidgetTapPackage writes to DataStore`() = runTest(testDispatcher) {
        val vm = SettingsViewModel(application, mockRepository, testDispatcher)
        backgroundScope.launch { vm.uiState.collect {} }
        advanceUntilIdle()
        vm.setWidgetTapPackage("com.example.weather")
        advanceUntilIdle()
        val state = vm.uiState.filter { it.widgetTapPackage == "com.example.weather" }.first()
        assertEquals("com.example.weather", state.widgetTapPackage)
    }

    @Test
    fun `widgetTapPackage defaults to empty string when key absent`() = runTest(testDispatcher) {
        val vm = SettingsViewModel(application, mockRepository, testDispatcher)
        backgroundScope.launch { vm.uiState.collect {} }
        advanceUntilIdle()
        val state = vm.uiState.first()
        assertEquals("", state.widgetTapPackage)
    }

    @Test
    fun `widgetDynamicColor defaults to true when both keys absent (new install)`() = runTest(testDispatcher) {
        assumeTrue("dynamic color only supported on API 31+", Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
        val vm = SettingsViewModel(application, mockRepository, testDispatcher)
        backgroundScope.launch { vm.uiState.collect {} }
        advanceUntilIdle()
        val state = vm.uiState.filter { it.widgetDynamicColor }.first()
        assertEquals(true, state.widgetDynamicColor)
    }

    @Test
    fun `widgetDynamicColor defaults to false when text color already set (existing user)`() = runTest(testDispatcher) {
        runBlocking {
            application.dataStore.edit { it[WeatherDataStore.WIDGET_TEXT_COLOR] = "blue" }
        }
        val vm = SettingsViewModel(application, mockRepository, testDispatcher)
        backgroundScope.launch { vm.uiState.collect {} }
        advanceUntilIdle()
        val state = vm.uiState.filter { !it.widgetDynamicColor }.first()
        assertEquals(false, state.widgetDynamicColor)
    }

    @Test
    fun `setWidgetDynamicColor writes to DataStore`() = runTest(testDispatcher) {
        val vm = SettingsViewModel(application, mockRepository, testDispatcher)
        backgroundScope.launch { vm.uiState.collect {} }
        advanceUntilIdle()
        vm.setWidgetDynamicColor(false)
        advanceUntilIdle()
        val state = vm.uiState.filter { !it.widgetDynamicColor }.first()
        assertEquals(false, state.widgetDynamicColor)
    }
}
