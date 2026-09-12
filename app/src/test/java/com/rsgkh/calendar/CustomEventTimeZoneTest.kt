// Copyright (c) 2026 RSG-KH | Apache-2.0 License
package com.rsgkh.calendar

import com.rsgkh.calendar.data.*
import com.rsgkh.calendar.notifications.ReminderPlanner
import org.junit.Assert.*
import org.junit.Test
import java.time.*

class CustomEventTimeZoneTest {
    private val brussels = ZoneId.of("Europe/Brussels")
    private val settings = AppSettings(notificationsEnabled = true, repeatHours = 0)

    @Test fun switchingDisplayZonesConvertsDateAndTimeWithoutMovingTheReminder() {
        val event = CustomEvent(title = "New Year appointment", date = LocalDate.of(2026, 12, 31), time = LocalTime.of(23, 30), zoneId = brussels.id)
        val local = event.asCalendarEvent(brussels)
        val cambodia = event.asCalendarEvent(CAMBODIA_ZONE)
        assertEquals(LocalDate.of(2026, 12, 31), local.date)
        assertEquals(LocalTime.of(23, 30), local.time)
        assertEquals(LocalDate.of(2027, 1, 1), cambodia.date)
        assertEquals(LocalTime.of(5, 30), cambodia.time)
        for (choice in TodayTimeZone.entries) {
            val batch = ReminderPlanner.next(event.instant.minusSeconds(1), settings.copy(todayTimeZone = choice), listOf(event), brussels) { emptyList() }!!
            assertEquals(Instant.parse("2026-12-31T22:30:00Z"), batch.at)
            assertEquals(if (choice == TodayTimeZone.LOCAL) local.date else cambodia.date, batch.date)
        }
    }

    @Test fun localEventsRepeatForElapsedHoursAndStopAtTheirOwnMidnight() {
        val spring = CustomEvent(title = "Clock change", date = LocalDate.of(2026, 3, 29), time = LocalTime.of(0, 30), zoneId = brussels.id)
        val config = settings.copy(repeatHours = 2)
        val next = ReminderPlanner.next(spring.instant, config, listOf(spring), brussels) { emptyList() }!!
        assertEquals(spring.instant.plusSeconds(7200), next.at)
        assertEquals(LocalTime.of(3, 30), next.at.atZone(brussels).toLocalTime())
        val late = spring.copy(date = LocalDate.of(2026, 9, 24), time = LocalTime.of(23, 30), zoneId = "America/Los_Angeles")
        assertEquals(late.instant, ReminderPlanner.next(late.instant.minusSeconds(1), config, listOf(late)) { emptyList() }!!.at)
        assertNull(ReminderPlanner.next(late.instant, config, listOf(late)) { emptyList() })
    }

    @Test fun repeatedAutumnClockTimeRetainsTheSavedOccurrence() {
        val later = CustomEvent(title = "Second 02:30", date = LocalDate.of(2026, 10, 25), time = LocalTime.of(2, 30), zoneId = brussels.id, offsetSeconds = 3600)
        assertEquals(Instant.parse("2026-10-25T01:30:00Z"), later.instant)
        val before = later.instant.minusSeconds(1)
        assertEquals(later.instant, ReminderPlanner.next(before, settings, listOf(later), brussels) { emptyList() }!!.at)
        assertNull(ReminderPlanner.next(before, settings, listOf(later.copy(offsetSeconds = 7200)), brussels) { emptyList() })
    }
}
