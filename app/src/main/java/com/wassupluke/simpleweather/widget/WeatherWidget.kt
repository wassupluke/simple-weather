package com.wassupluke.simpleweather.widget

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.emptyPreferences
import androidx.glance.*
import androidx.glance.action.Action
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.*
import androidx.glance.GlanceTheme
import androidx.glance.text.*
import androidx.glance.unit.ColorProvider
import com.wassupluke.simpleweather.data.WeatherDataStore
import com.wassupluke.simpleweather.data.dataStore
import com.wassupluke.simpleweather.data.parseColorSafe
import com.wassupluke.simpleweather.data.resolveDynamicColor
import com.wassupluke.simpleweather.ui.MainActivity
import kotlin.math.roundToInt

class WeatherWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            GlanceTheme {
                val prefs by context.dataStore.data.collectAsState(initial = emptyPreferences())
                val tempCelsius = prefs[WeatherDataStore.LAST_TEMP_CELSIUS]
                val unit = prefs[WeatherDataStore.TEMP_UNIT] ?: "C"
                val colorString = prefs[WeatherDataStore.WIDGET_TEXT_COLOR] ?: "white"
                val dynamicColor = prefs.resolveDynamicColor()

                val displayTemp = if (tempCelsius == null) {
                    "--°"
                } else {
                    val value = if (unit == "F") celsiusToFahrenheit(tempCelsius) else tempCelsius.roundToInt()
                    "$value°"
                }

                val textColorProvider: ColorProvider = if (dynamicColor) {
                    GlanceTheme.colors.primary
                } else {
                    val resolved = parseColorSafe(colorString)?.let { argb ->
                        Color(
                            red = android.graphics.Color.red(argb) / 255f,
                            green = android.graphics.Color.green(argb) / 255f,
                            blue = android.graphics.Color.blue(argb) / 255f,
                            alpha = android.graphics.Color.alpha(argb) / 255f
                        )
                    } ?: Color.White
                    ColorProvider(resolved)
                }

                val tapPackage = prefs[WeatherDataStore.WIDGET_TAP_PACKAGE]
                val tapAction = if (!tapPackage.isNullOrEmpty()) {
                    val launchIntent = context.packageManager.getLaunchIntentForPackage(tapPackage)
                    if (launchIntent?.component != null) actionStartActivity(launchIntent.component!!)
                    else actionStartActivity<MainActivity>()
                } else {
                    actionStartActivity<MainActivity>()
                }

                val fontSize = prefs[WeatherDataStore.FONT_SIZE] ?: WeatherDataStore.DEFAULT_FONT_SIZE

                WeatherWidgetContent(
                    displayTemp = displayTemp,
                    textColorProvider = textColorProvider,
                    tapAction = tapAction,
                    fontSize = fontSize
                )
            }
        }
    }

    private fun celsiusToFahrenheit(celsius: Float): Int =
        ((celsius * 9f / 5f) + 32f).roundToInt()
}

@SuppressLint("RestrictedApi")
@Composable
private fun WeatherWidgetContent(
    displayTemp: String,
    textColorProvider: ColorProvider,
    tapAction: Action,
    fontSize: Int
) {
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .clickable(tapAction),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = displayTemp,
            style = TextStyle(
                fontSize = fontSize.sp,
                fontWeight = FontWeight.Normal,
                color = textColorProvider
            )
        )
    }
}
