package com.wassupluke.simpleweather.worker

import android.content.Context
import androidx.work.*
import java.util.concurrent.TimeUnit

object WorkScheduler {
    private const val WORK_NAME = "weather_fetch"

    fun schedule(context: Context, intervalMinutes: Int) {
        val clampedInterval = maxOf(15, intervalMinutes)
        val request = PeriodicWorkRequestBuilder<WeatherFetchWorker>(
            clampedInterval.toLong(), TimeUnit.MINUTES
        )
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }
}
