// SPDX-License-Identifier: Apache-2.0
package com.rsgkh.calendar.widgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.widget.RemoteViews
import androidx.compose.ui.graphics.toArgb
import com.rsgkh.calendar.R

/** The same native refresh control is used by Planner and all Jetpack Glance widgets. */
internal object WidgetRefreshControl {
    const val ACTION_MANUAL_REFRESH = "com.rsgkh.calendar.widgets.MANUAL_REFRESH"

    fun views(context: Context, id: Int, description: String, palette: WidgetPalette): RemoteViews =
        RemoteViews(context.packageName, R.layout.widget_refresh_button).apply {
            style(this, R.id.widget_refresh_button, context, id, description, palette)
        }

    fun style(views: RemoteViews, viewId: Int, context: Context, id: Int,
        description: String, palette: WidgetPalette) {
        views.setColorStateList(viewId, "setBackgroundTintList", ColorStateList(
            arrayOf(intArrayOf(android.R.attr.state_pressed), intArrayOf()),
            intArrayOf(palette.accent.getColor(context).copy(alpha = 0.30f).toArgb(),
                palette.surfaceVariant.getColor(context).toArgb()),
        ))
        views.setInt(viewId, "setColorFilter", palette.secondary.getColor(context).toArgb())
        views.setContentDescription(viewId, description)
        val intent = Intent(context, WidgetRefreshReceiver::class.java).setAction(ACTION_MANUAL_REFRESH)
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
        views.setOnClickPendingIntent(viewId, PendingIntent.getBroadcast(context, id, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
    }
}
