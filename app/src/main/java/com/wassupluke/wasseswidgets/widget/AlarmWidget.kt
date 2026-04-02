package com.wassupluke.wasseswidgets.widget

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.emptyPreferences
import androidx.compose.ui.unit.dp
import androidx.glance.*
import androidx.glance.action.Action
import androidx.glance.action.actionStartActivity
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Alignment
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.GlanceTheme
import androidx.glance.text.*
import androidx.glance.unit.ColorProvider
import com.wassupluke.wasseswidgets.R
import com.wassupluke.wasseswidgets.data.WeatherDataStore
import com.wassupluke.wasseswidgets.data.dataStore
import com.wassupluke.wasseswidgets.data.parseColorSafe
import com.wassupluke.wasseswidgets.data.resolveDynamicColor
import com.wassupluke.wasseswidgets.ui.MainActivity

@SuppressLint("RestrictedApi")
class AlarmWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            GlanceTheme {
                val prefs by context.dataStore.data.collectAsState(initial = emptyPreferences())
                val alarmText = prefs[WeatherDataStore.ALARM_TEXT] ?: "No alarm"
                val colorString = prefs[WeatherDataStore.WIDGET_TEXT_COLOR] ?: "white"
                val dynamicColor = prefs.resolveDynamicColor()
                val fontSize = prefs[WeatherDataStore.FONT_SIZE] ?: WeatherDataStore.DEFAULT_FONT_SIZE

                val textColorProvider: ColorProvider = if (dynamicColor) {
                    GlanceTheme.colors.primary
                } else {
                    val argb = parseColorSafe(colorString) ?: android.graphics.Color.WHITE
                    ColorProvider(Color(argb))
                }

                val tapPackage = prefs[WeatherDataStore.ALARM_WIDGET_TAP_PACKAGE]
                val tapAction: Action = if (!tapPackage.isNullOrEmpty()) {
                    val launchIntent = context.packageManager.getLaunchIntentForPackage(tapPackage)
                    if (launchIntent?.component != null) actionStartActivity(launchIntent.component!!)
                    else actionStartActivity<MainActivity>()
                } else {
                    actionStartActivity<MainActivity>()
                }

                AlarmWidgetContent(
                    alarmText = alarmText,
                    textColorProvider = textColorProvider,
                    tapAction = tapAction,
                    fontSize = fontSize
                )
            }
        }
    }
}

@SuppressLint("RestrictedApi")
@Composable
private fun AlarmWidgetContent(
    alarmText: String,
    textColorProvider: ColorProvider,
    tapAction: Action,
    fontSize: Int
) {
    WidgetRoot(tapAction = tapAction) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                provider = ImageProvider(R.drawable.ic_alarm),
                contentDescription = null,
                modifier = GlanceModifier.size(fontSize.dp),
                colorFilter = ColorFilter.tint(textColorProvider)
            )
            Spacer(GlanceModifier.width(4.dp))
            Text(
                text = alarmText,
                style = TextStyle(
                    fontSize = fontSize.sp,
                    fontWeight = FontWeight.Normal,
                    color = textColorProvider
                )
            )
        }
    }
}
