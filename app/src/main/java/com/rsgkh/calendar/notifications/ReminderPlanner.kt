// Copyright (c) 2026 RSG-KH | Apache-2.0 License
package com.rsgkh.calendar.notifications

import com.rsgkh.calendar.data.*
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

data class ReminderBatch(val at: Instant, val date: LocalDate, val events: List<CalendarEvent>, val expiresAt: Map<String, Instant>)

/** One next alarm, with same-day repeats. No polling, network or permanent service. */
object ReminderPlanner {
    fun next(now: Instant, settings: AppSettings, custom: List<CustomEvent>,
             localZone: ZoneId = ZoneId.systemDefault(),
             yearEvents: (Int) -> List<CalendarEvent> = EventRepository::forYear): ReminderBatch? {
        if (!settings.notificationsEnabled) return null
        require(settings.pushMinutes in 0..1439 && settings.repeatHours in listOf(0, 2, 4, 6, 8, 12))
        val reminderZone = settings.todayTimeZone.zone(localZone)
        val today = now.atZone(reminderZone).toLocalDate()
        var earliest: Instant? = null
        val due = mutableListOf<CalendarEvent>()
        val expirations = mutableMapOf<String, Instant>()
        fun consider(event: CalendarEvent, start: ZonedDateTime) {
            val enabled = when (event.kind) {
                EventKind.CUSTOM -> settings.pushCustomEvents
                EventKind.HOLIDAY -> settings.pushHolidays
                EventKind.OBSERVANCE -> settings.pushObservances
                // The saved push choice is preserved while holy days are hidden.
                EventKind.HOLY_DAY -> settings.showHolyDaysInEvents && settings.pushHolyDays
            }
            if (!enabled) return
            val end = start.toLocalDate().plusDays(1).atStartOfDay(start.zone).toInstant()
            if (end <= now) return
            var at = start.toInstant()
            while (at < end) {
                if (at > now) {
                    if (earliest == null || at < earliest) { earliest = at; due.clear(); expirations.clear() }
                    if (at == earliest) { due.add(event); expirations[event.key] = end }
                    break
                }
                if (settings.repeatHours == 0) break
                at = at.plusSeconds(settings.repeatHours * 3600L)
            }
        }
        val push = LocalTime.of(settings.pushMinutes / 60, settings.pushMinutes % 60)
        val startYear = today.year.coerceAtLeast(1800)
        if (startYear <= 2200) {
            for (year in startYear..minOf(startYear + 1, 2200)) {
                yearEvents(year).forEach { consider(it, it.date.atTime(push).atZone(reminderZone)) }
                if (earliest != null) break
            }
        }
        custom.filter { it.remindersEligible }.forEach { consider(it.asCalendarEvent(reminderZone), it.zonedDateTime) }
        return earliest?.let { ReminderBatch(it, due.first().date, due.distinctBy { event -> event.key }, expirations.toMap()) }
    }
}
