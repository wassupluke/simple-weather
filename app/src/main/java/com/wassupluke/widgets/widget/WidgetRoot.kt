package com.wassupluke.widgets.widget

import androidx.compose.runtime.Composable
import androidx.glance.GlanceModifier
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.fillMaxSize

/**
 * Standard widget scaffold: centers content and bounds the tap target to the content area,
 * so empty widget surface space does not intercept touches.
 */
@Composable
fun WidgetRoot(tapAction: Action, content: @Composable () -> Unit) {
    Box(
        modifier = GlanceModifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Box(modifier = GlanceModifier.clickable(tapAction)) {
            content()
        }
    }
}
