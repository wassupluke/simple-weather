package com.wassupluke.simpleweather.widget

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import androidx.datastore.preferences.core.edit
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.updateAll
import com.wassupluke.simpleweather.data.WeatherDataStore
import com.wassupluke.simpleweather.data.dataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

class AlarmWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = AlarmWidget()

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            AlarmManager.ACTION_NEXT_ALARM_CLOCK_CHANGED,
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED -> updateAlarmText(context)
        }
    }

    private fun updateAlarmText(context: Context) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                val nextAlarm = alarmManager.nextAlarmClock
                val alarmText = if (nextAlarm == null) {
                    "No alarm"
                } else {
                    DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(nextAlarm.triggerTime))
                }
                context.dataStore.edit { it[WeatherDataStore.ALARM_TEXT] = alarmText }
                AlarmWidget().updateAll(context)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
