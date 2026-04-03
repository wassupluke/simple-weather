package com.wassupluke.widgets.widget

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
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.Spacer
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.GlanceTheme
import androidx.glance.text.*
import androidx.glance.unit.ColorProvider
import com.wassupluke.widgets.R
import com.wassupluke.widgets.data.WeatherDataStore
import com.wassupluke.widgets.data.dataStore
import com.wassupluke.widgets.data.parseColorSafe
import com.wassupluke.widgets.data.resolveDynamicColor

@SuppressLint("RestrictedApi")
class AlarmWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            GlanceTheme {
                val prefs by context.dataStore.data.collectAsState(initial = emptyPreferences())
                val alarmText = prefs[WeatherDataStore.ALARM_TEXT] ?: context.getString(R.string.widget_alarm_none)
                val colorString = prefs[WeatherDataStore.WIDGET_TEXT_COLOR] ?: "white"
                val dynamicColor = prefs.resolveDynamicColor()
                val fontSize = prefs[WeatherDataStore.FONT_SIZE] ?: WeatherDataStore.DEFAULT_FONT_SIZE

                val textColorProvider: ColorProvider = if (dynamicColor) {
                    GlanceTheme.colors.primary
                } else {
                    val argb = parseColorSafe(colorString) ?: android.graphics.Color.WHITE
                    ColorProvider(Color(argb))
                }

                val tapAction = resolveTapAction(context, prefs[WeatherDataStore.ALARM_WIDGET_TAP_PACKAGE])

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
    Column(
        modifier = GlanceModifier.fillMaxSize(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = GlanceModifier.clickable(tapAction),
            verticalAlignment = Alignment.CenterVertically
        ) {
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
