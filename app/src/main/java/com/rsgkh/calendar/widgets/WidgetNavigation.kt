// SPDX-License-Identifier: Apache-2.0
package com.rsgkh.calendar.widgets

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.glance.action.Action
import androidx.glance.appwidget.action.actionStartActivity
import com.rsgkh.calendar.MainActivity
import com.rsgkh.calendar.data.CalendarEvent
import com.rsgkh.calendar.data.CustomEvent
import com.rsgkh.calendar.data.EventRepository
import com.rsgkh.calendar.notifications.EventNotifications
import java.time.LocalDate
import java.time.ZoneId

/** A nonce lets a repeated tap on the same event reopen its dismissed detail dialog. */
data class WidgetEventRequest(val date: LocalDate, val eventId: String, val nonce: Long)

object WidgetNavigation {
    const val EXTRA_EVENT_ID = "com.rsgkh.calendar.widget.EVENT_ID"

    fun dateIntent(context: Context, widgetId: Int, date: LocalDate, eventId: String? = null): Intent =
        Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EventNotifications.EXTRA_DATE, date.toString())
            if (eventId != null) putExtra(EXTRA_EVENT_ID, eventId)
            // Extras alone do NOT distinguish PendingIntents. No titles/notes in this URI.
            data = Uri.Builder().scheme("khmer-calendar-widget").authority("open")
                .appendPath(widgetId.toString()).appendPath(date.toString())
                .appendQueryParameter("event", eventId ?: "").build()
        }

    internal fun openDate(context: Context, id: Int, date: LocalDate, eventId: String? = null): Action =
        actionStartActivity(dateIntent(context, id, date, eventId))

    /** Resolve by occurrence ID AND displayed day. A stale/deleted event falls back to its day. */
    fun resolve(request: WidgetEventRequest, personal: List<CustomEvent>, zone: ZoneId): CalendarEvent? {
        if (request.date.year !in EventRepository.coveredYears) return null
        return if (request.eventId.startsWith("custom:")) {
            personal.asSequence().flatMap {
                it.occurrences(request.date, request.date, zone).asSequence()
            }.firstOrNull { it.id == request.eventId && it.date == request.date }
        } else {
            EventRepository.forDate(request.date).firstOrNull { it.id == request.eventId }
        }
    }
}
