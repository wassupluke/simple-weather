package com.wassupluke.widgets.data.api

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit

object NetworkModule {

    private val json = Json { ignoreUnknownKeys = true }
    private val contentType = "application/json".toMediaType()
    private val okHttpClient = OkHttpClient.Builder().build()

    val weatherService: OpenMeteoService = Retrofit.Builder()
        .baseUrl("https://api.open-meteo.com/")
        .client(okHttpClient)
        .addConverterFactory(json.asConverterFactory(contentType))
        .build()
        .create(OpenMeteoService::class.java)

    val geocodingService: OpenMeteoService = Retrofit.Builder()
        .baseUrl("https://geocoding-api.open-meteo.com/")
        .client(okHttpClient)
        .addConverterFactory(json.asConverterFactory(contentType))
        .build()
        .create(OpenMeteoService::class.java)
}
