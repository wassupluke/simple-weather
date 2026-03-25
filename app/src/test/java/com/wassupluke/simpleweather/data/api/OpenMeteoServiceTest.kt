package com.wassupluke.simpleweather.data.api

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OpenMeteoServiceTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `deserializes weather response correctly`() {
        val raw = """{"current":{"time":"2024-01-01T12:00","temperature_2m":22.3}}"""
        val response = json.decodeFromString<WeatherResponse>(raw)
        assertEquals(22.3f, response.current.temperatureCelsius, 0.01f)
    }

    @Test
    fun `deserializes geocoding response correctly`() {
        val raw = """{"results":[{"name":"New York","latitude":40.71427,"longitude":-74.00597,"country":"United States","admin1":"New York"}]}"""
        val response = json.decodeFromString<GeocodingResponse>(raw)
        assertEquals("New York", response.results?.first()?.name)
        assertEquals(40.71427, response.results?.first()?.latitude ?: 0.0, 0.001)
    }

    @Test
    fun `geocoding response with empty results does not crash`() {
        val raw = """{}"""
        val response = json.decodeFromString<GeocodingResponse>(raw)
        assertNull(response.results)
    }

    @Test
    fun `weather response ignores unknown fields`() {
        val raw = """{"current":{"time":"2024-01-01T12:00","temperature_2m":15.0,"wind_speed":5.2,"unknown_future_field":"x"}}"""
        val response = json.decodeFromString<WeatherResponse>(raw)
        assertEquals(15.0f, response.current.temperatureCelsius, 0.01f)
    }
}
