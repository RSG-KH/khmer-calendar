// Copyright (c) 2026 RSG-KH | Apache-2.0 License
package com.rsgkh.calendar.notifications

import android.content.Context
import android.util.Log
import java.util.concurrent.Executors
import java.util.concurrent.Future

/** Serialize reminder work without holding up the activity or broadcast thread. */
internal object ReminderWork {
    private val executor = Executors.newSingleThreadExecutor { task -> Thread(task, "calendar-reminders") }

    fun submit(context: Context, onFinished: () -> Unit = {}, work: (Context) -> Unit): Future<*> {
        val appContext = context.applicationContext
        return executor.submit {
            try {
                work(appContext)
            } catch (error: Exception) {
                Log.e("CalendarReminders", "Reminder work failed", error)
                throw error
            } finally {
                onFinished()
            }
        }
    }
}
