package com.wassupluke.widgets.worker

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.wassupluke.widgets.data.WeatherDataStore
import com.wassupluke.widgets.data.WeatherRepository
import com.wassupluke.widgets.data.dataStore
import com.wassupluke.widgets.widget.WeatherWidget
import kotlinx.coroutines.flow.first

class WeatherFetchWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val prefs = applicationContext.dataStore.data.first()
        val lat = prefs[WeatherDataStore.LOCATION_LAT] ?: return Result.failure()
        val lon = prefs[WeatherDataStore.LOCATION_LON] ?: return Result.failure()

        return try {
            val repo = WeatherRepository.create(applicationContext)
            repo.fetchAndCacheWeather(lat = lat, lon = lon)
            WeatherWidget().updateAll(applicationContext)
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
