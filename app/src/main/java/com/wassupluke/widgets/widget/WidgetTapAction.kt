package com.wassupluke.widgets.widget

import android.content.Context
import androidx.glance.action.Action
import androidx.glance.action.actionStartActivity
import com.wassupluke.widgets.ui.MainActivity

internal fun resolveTapAction(context: Context, tapPackage: String?): Action {
    if (!tapPackage.isNullOrEmpty()) {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(tapPackage)
        if (launchIntent?.component != null) return actionStartActivity(launchIntent.component!!)
    }
    return actionStartActivity<MainActivity>()
}
