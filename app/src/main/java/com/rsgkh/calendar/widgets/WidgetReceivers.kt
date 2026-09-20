// SPDX-License-Identifier: Apache-2.0
package com.rsgkh.calendar.widgets

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

abstract class CalendarWidgetReceiver : GlanceAppWidgetReceiver() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        super.onUpdate(context, manager, ids)
        WidgetUpdater.requestUpdate(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        WidgetUpdater.stopIfUnused(context)
    }

    override fun onRestored(context: Context, oldWidgetIds: IntArray, newWidgetIds: IntArray) {
        super.onRestored(context, oldWidgetIds, newWidgetIds)
        val manager = AppWidgetManager.getInstance(context)
        newWidgetIds.forEach { id ->
            manager.updateAppWidgetOptions(id, Bundle().apply {
                putBoolean(AppWidgetManager.OPTION_APPWIDGET_RESTORE_COMPLETED, true)
            })
        }
        WidgetUpdater.requestUpdate(context)
    }
}

class FocusWidgetReceiver : CalendarWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = FocusWidget()
}

class ProductivityWidgetReceiver : CalendarWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ProductivityWidget()
}

class MonthWidgetReceiver : CalendarWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = MonthWidget()
}

/** Not direct-boot aware: the SQLite database and preferences require an unlocked user. */
class WidgetRefreshReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in ACTIONS) return
        val pending = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Hold the receiver only until work is persisted, not while widgets render.
                WidgetUpdater.enqueue(appContext)?.result?.get(8, TimeUnit.SECONDS)
            } catch (error: Exception) {
                Log.w("CalendarWidgets", "System refresh could not be queued: ${error.javaClass.simpleName}")
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        private val ACTIONS = setOf(
            WidgetUpdater.ACTION_MIDNIGHT,
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_DATE_CHANGED,
            Intent.ACTION_LOCALE_CHANGED,
        )
    }
}
