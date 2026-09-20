// SPDX-License-Identifier: Apache-2.0
package com.rsgkh.calendar.widgets

import android.content.Context
import android.util.Log
import androidx.annotation.Keep
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.action.ActionCallback
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Keep
class WidgetRefreshWorker(context: Context, parameters: WorkerParameters) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result = try {
        withContext(Dispatchers.IO) {
            if (WidgetUpdater.installedIds(applicationContext).isEmpty()) {
                WidgetUpdater.stopIfUnused(applicationContext)
            } else {
                WidgetUpdater.ensureScheduled(applicationContext)
                WidgetUpdater.refreshAll(applicationContext)
            }
        }
        Result.success()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        Log.w("CalendarWidgets", "Refresh failed: ${error.javaClass.simpleName}")
        if (runAttemptCount < 3) Result.retry() else Result.failure()
    }
}

/** This action only refreshes. Activity launches use direct activity PendingIntents instead. */
@Keep
class RefreshWidgetAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        withContext(Dispatchers.IO) {
            WidgetUpdater.ensureScheduled(context)
            WidgetUpdater.refreshOne(context, GlanceAppWidgetManager(context).getAppWidgetId(glanceId))
        }
    }
}
