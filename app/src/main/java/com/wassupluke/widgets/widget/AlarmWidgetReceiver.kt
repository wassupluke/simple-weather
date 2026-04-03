package com.wassupluke.widgets.widget

import android.app.AlarmManager
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import androidx.datastore.preferences.core.edit
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.updateAll
import com.wassupluke.widgets.R
import com.wassupluke.widgets.data.WeatherDataStore
import com.wassupluke.widgets.data.dataStore
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
            Intent.ACTION_TIMEZONE_CHANGED,
            AppWidgetManager.ACTION_APPWIDGET_UPDATE -> updateAlarmText(context, intent.action ?: "")
        }
    }

    private fun updateAlarmText(context: Context, action: String) {
        // APPWIDGET_UPDATE: super already called goAsync() internally — calling it again returns null.
        // For all other broadcasts, goAsync() is available and needed to keep the process alive.
        val pendingResult = if (action != AppWidgetManager.ACTION_APPWIDGET_UPDATE) goAsync() else null
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                val nextAlarm = alarmManager.nextAlarmClock
                val alarmText = if (nextAlarm == null) {
                    context.getString(R.string.widget_alarm_none)
                } else {
                    val creator = nextAlarm.showIntent?.creatorPackage
                    val fromClockApp = creator != null && isUserFacingApp(context, creator)
                    if (fromClockApp) {
                        DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(nextAlarm.triggerTime))
                    } else {
                        context.getString(R.string.widget_alarm_none)
                    }
                }
                context.dataStore.edit { it[WeatherDataStore.ALARM_TEXT] = alarmText }
                AlarmWidget().updateAll(context)
            } finally {
                pendingResult?.finish()
            }
        }
    }

    private fun isUserFacingApp(context: Context, packageName: String): Boolean =
        context.packageManager.getLaunchIntentForPackage(packageName) != null
}
