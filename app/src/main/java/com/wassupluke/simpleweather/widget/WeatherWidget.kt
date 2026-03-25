package com.wassupluke.simpleweather.widget

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.emptyPreferences
import androidx.glance.*
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.*
import androidx.glance.text.*
import androidx.glance.unit.ColorProvider
import com.wassupluke.simpleweather.data.WeatherDataStore
import com.wassupluke.simpleweather.data.dataStore
import com.wassupluke.simpleweather.data.parseColorSafe
import com.wassupluke.simpleweather.ui.MainActivity
import kotlin.math.roundToInt

class WeatherWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val prefs by context.dataStore.data.collectAsState(initial = emptyPreferences())
            val tempCelsius = prefs[WeatherDataStore.LAST_TEMP_CELSIUS]
            val unit = prefs[WeatherDataStore.TEMP_UNIT] ?: "C"
            val colorString = prefs[WeatherDataStore.WIDGET_TEXT_COLOR] ?: "white"

            val displayTemp = if (tempCelsius == null) {
                "--°"
            } else {
                val value = if (unit == "F") celsiusToFahrenheit(tempCelsius) else tempCelsius.roundToInt()
                "$value°"
            }

            val textColor = parseColorSafe(colorString)?.let { argb ->
                androidx.compose.ui.graphics.Color(
                    red = android.graphics.Color.red(argb) / 255f,
                    green = android.graphics.Color.green(argb) / 255f,
                    blue = android.graphics.Color.blue(argb) / 255f,
                    alpha = android.graphics.Color.alpha(argb) / 255f
                )
            } ?: androidx.compose.ui.graphics.Color.White

            WeatherWidgetContent(displayTemp = displayTemp, textColor = textColor)
        }
    }

    private fun celsiusToFahrenheit(celsius: Float): Int =
        ((celsius * 9f / 5f) + 32f).roundToInt()
}

@SuppressLint("RestrictedApi")
@Composable
private fun WeatherWidgetContent(
    displayTemp: String,
    textColor: androidx.compose.ui.graphics.Color
) {
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .clickable(actionStartActivity<MainActivity>()),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = displayTemp,
            style = TextStyle(
                fontSize = 48.sp,
                fontWeight = FontWeight.Normal,
                color = ColorProvider(textColor)
            )
        )
    }
}
