package com.wassupluke.wasseswidgets.data.api

import retrofit2.http.GET
import retrofit2.http.Query

interface OpenMeteoService {

    // Note: Retrofit does not support Kotlin default parameters — all @Query params are explicit.
    @GET("v1/forecast")
    suspend fun getCurrentWeather(
        @Query("latitude") latitude: Float,
        @Query("longitude") longitude: Float,
        @Query("current") current: String,
        @Query("temperature_unit") temperatureUnit: String
    ): WeatherResponse

    @GET("v1/search")
    suspend fun searchLocation(
        @Query("name") query: String,
        @Query("count") count: Int,
        @Query("language") language: String,
        @Query("format") format: String
    ): GeocodingResponse
}
