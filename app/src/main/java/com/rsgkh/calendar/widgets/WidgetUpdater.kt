// SPDX-License-Identifier: Apache-2.0
package com.rsgkh.calendar.widgets

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.Operation
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.rsgkh.calendar.data.AppPreferences
import java.time.Instant
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

object WidgetUpdater {
    internal val REVISION = stringPreferencesKey("calendar_widget_revision_v1")
    internal const val ACTION_MIDNIGHT = "com.rsgkh.calendar.widgets.MIDNIGHT"
    private const val PERIODIC = "calendar-widgets-hourly-v1"
    private const val IMMEDIATE = "calendar-widgets-refresh-v1"
    private val renderLock = Mutex()

    /** Enables or disables widget receivers in PackageManager to show/hide them in system widget browser. */
    fun setWidgetsEnabled(context: Context, enabled: Boolean) {
        val appContext = context.applicationContext
        val pm = appContext.packageManager
        val state = if (enabled) {
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        } else {
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        }
        try {
            pm.setComponentEnabledSetting(
                ComponentName(appContext, FocusWidgetReceiver::class.java),
                state,
                PackageManager.DONT_KILL_APP,
            )
            pm.setComponentEnabledSetting(
                ComponentName(appContext, ProductivityWidgetReceiver::class.java),
                state,
                PackageManager.DONT_KILL_APP,
            )
            pm.setComponentEnabledSetting(
                ComponentName(appContext, MonthWidgetReceiver::class.java),
                state,
                PackageManager.DONT_KILL_APP,
            )
            pm.setComponentEnabledSetting(
                ComponentName(appContext, PlannerWidgetReceiver::class.java),
                state,
                PackageManager.DONT_KILL_APP,
            )
            pm.setComponentEnabledSetting(
                ComponentName(appContext, GlanceWidgetReceiver::class.java),
                state,
                PackageManager.DONT_KILL_APP,
            )
        } catch (error: Exception) {
            Log.w("CalendarWidgets", "Could not update widget component enabled state: ${error.javaClass.simpleName}")
        }
        if (enabled) {
            requestUpdate(appContext)
        } else {
            stopScheduled(appContext)
        }
    }

    /** Safe, non-blocking entry point for MainActivity and future event-import/save paths. */
    fun requestUpdate(context: Context) {
        try {
            enqueue(context.applicationContext)
        } catch (error: Exception) {
            Log.w("CalendarWidgets", "Refresh could not be queued: ${error.javaClass.simpleName}")
        }
    }

    internal fun enqueue(context: Context): Operation? {
        if (!AppPreferences(context).read().widgetsEnabled) return null
        if (installedIds(context).isEmpty()) return null
        return runCatching {
            WorkManager.getInstance(context).enqueueUniqueWork(
                IMMEDIATE, ExistingWorkPolicy.REPLACE, OneTimeWorkRequestBuilder<WidgetRefreshWorker>().build(),
            )
        }.getOrNull()
    }

    internal fun installedIds(context: Context): List<Int> {
        val manager = AppWidgetManager.getInstance(context)
        return listOf(FocusWidgetReceiver::class.java, ProductivityWidgetReceiver::class.java,
            MonthWidgetReceiver::class.java, PlannerWidgetReceiver::class.java,
            GlanceWidgetReceiver::class.java)
            .flatMap { manager.getAppWidgetIds(ComponentName(context, it)).toList() }
    }

    internal fun widgetForId(context: Context, id: Int): CalendarHomeWidget? {
        val provider = AppWidgetManager.getInstance(context).getAppWidgetInfo(id)?.provider ?: return null
        return when (provider) {
            ComponentName(context, FocusWidgetReceiver::class.java) -> FocusWidget()
            ComponentName(context, ProductivityWidgetReceiver::class.java) -> ProductivityWidget()
            ComponentName(context, MonthWidgetReceiver::class.java) -> MonthWidget()
            ComponentName(context, GlanceWidgetReceiver::class.java) -> GlanceWidget()
            else -> null
        }
    }

    internal suspend fun refreshOne(context: Context, id: Int) = renderLock.withLock {
        if (!AppPreferences(context).read().widgetsEnabled) return@withLock
        val provider = AppWidgetManager.getInstance(context).getAppWidgetInfo(id)?.provider ?: return@withLock
        if (provider == ComponentName(context, PlannerWidgetReceiver::class.java)) {
            PlannerWidgetRenderer.update(context, id)
            return@withLock
        }
        val widget = widgetForId(context, id) ?: return@withLock
        val manager = GlanceAppWidgetManager(context)
        val glanceId = try {
            manager.getGlanceIdBy(id)
        } catch (_: IllegalArgumentException) {
            return@withLock // The user removed it between enumeration and this read.
        }
        updateAppWidgetState(context, glanceId) { state -> state[REVISION] = UUID.randomUUID().toString() }
        widget.update(context, glanceId)
    }

    internal suspend fun refreshAll(context: Context) {
        var firstFailure: Exception? = null
        installedIds(context).forEach { id ->
            try {
                refreshOne(context, id)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Log.w("CalendarWidgets", "Widget $id refresh failed: ${error.javaClass.simpleName}")
                if (firstFailure == null) firstFailure = error
            }
        }
        firstFailure?.let { throw it }
    }

    /** Separate from notification alarms. Does NOT require notification or exact-alarm access. */
    internal fun ensureScheduled(context: Context) {
        val settings = AppPreferences(context).read()
        if (!settings.widgetsEnabled) return
        if (installedIds(context).isEmpty()) return
        runCatching {
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                PERIODIC, ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<WidgetRefreshWorker>(1, TimeUnit.HOURS).build(),
            )
        }
        val next = WidgetPolicy.nextMidnight(Instant.now(), settings.todayTimeZone.zone())
        val alarm = context.getSystemService(AlarmManager::class.java) ?: return
        // Intentionally inexact; Android may defer this under Doze/battery restrictions.
        // Reusing one PendingIntent replaces the previous boundary after a zone/clock change.
        runCatching {
            alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next.toEpochMilli(), midnightIntent(context))
        }.onFailure { error ->
            Log.w("CalendarWidgets", "Midnight refresh could not be scheduled: ${error.javaClass.simpleName}")
        }
    }

    internal fun stopIfUnused(context: Context) {
        if (installedIds(context).isNotEmpty()) return
        stopScheduled(context)
    }

    internal fun stopScheduled(context: Context) {
        runCatching {
            val work = WorkManager.getInstance(context)
            work.cancelUniqueWork(PERIODIC)
            work.cancelUniqueWork(IMMEDIATE)
        }
        context.getSystemService(AlarmManager::class.java)?.cancel(midnightIntent(context))
    }

    private fun midnightIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context, 71042,
        Intent(context, WidgetRefreshReceiver::class.java).setAction(ACTION_MIDNIGHT),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
}
