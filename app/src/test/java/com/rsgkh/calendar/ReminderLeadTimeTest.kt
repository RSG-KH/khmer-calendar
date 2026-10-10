// Copyright (c) 2026 RSG-KH | Apache-2.0 License
package com.rsgkh.calendar

import com.rsgkh.calendar.data.*
import com.rsgkh.calendar.domain.EventRepeat
import com.rsgkh.calendar.domain.RepeatFrequency
import com.rsgkh.calendar.notifications.ReminderPlanner
import org.junit.Assert.*
import org.junit.Test
import java.time.*

class ReminderLeadTimeTest {
    private val settings = AppSettings(notificationsEnabled = true, pushHolidays = false,
        pushObservances = false, pushHolyDays = false, repeatHours = 0)
    private val event = CustomEvent(id = "appointment", title = "Appointment", date = LocalDate.of(2026, 11, 9),
        time = LocalTime.of(9, 0), zoneId = "Europe/Brussels")

    private fun next(now: Instant, events: List<CustomEvent>, config: AppSettings = settings) =
        ReminderPlanner.next(now, config, events, ZoneId.of("Europe/Brussels")) { error("Unexpected built-in load") }

    @Test fun zeroSendsOneAlertAndPositiveLeadSendsBothAdvanceAndEventTimeAlerts() {
        for (minutes in listOf(0, 15, 180)) {
            val source = event.copy(reminderMinutesBefore = minutes)
            val due = source.instant.minusSeconds(minutes * 60L)
            val batch = next(due.minusSeconds(1), listOf(source))!!
            assertEquals(due, batch.at)
            assertEquals(source.instant, batch.events.single().instant)
            assertEquals(LocalTime.of(9, 0), batch.events.single().time)
            if (minutes > 0) assertEquals(source.instant, next(due, listOf(source))!!.at)
            assertNull(next(source.instant, listOf(source)))
        }
        assertNull(next(event.instant.minusSeconds(3600), listOf(event), settings.copy(pushCustomEvents = false)))
    }

    @Test fun earlierAlertWinsEvenWhenItsEventStartsLaterAndEqualAlertsAreBatched() {
        val later = event.copy(id = "later", time = LocalTime.of(10, 0), reminderMinutesBefore = 180)
        val sameAlert = event.copy(id = "same", time = LocalTime.of(7, 0))
        val due = Instant.parse("2026-11-09T06:00:00Z")
        for (events in listOf(listOf(event, later, sameAlert), listOf(sameAlert, later, event))) {
            val batch = next(due.minusSeconds(1), events)!!
            assertEquals(due, batch.at)
            assertEquals(setOf("custom:later", "custom:same"), batch.events.map { it.id }.toSet())
        }
    }

    @Test fun advanceAcrossNewYearKeepsTheEventDateAndRepeatsUntilTheEventDayEnds() {
        val source = event.copy(date = LocalDate.of(2027, 1, 1), time = LocalTime.of(0, 15), reminderMinutesBefore = 180)
        val due = Instant.parse("2026-12-31T20:15:00Z")
        val config = settings.copy(repeatHours = 2)
        val batch = next(due.minusSeconds(1), listOf(source), config)!!
        assertEquals(due, batch.at)
        assertEquals(source.date, batch.date)
        assertEquals(Instant.parse("2027-01-01T23:00:00Z"), batch.expiresAt.values.single())
        assertEquals(source.instant, next(due, listOf(source), config)!!.at)
        assertEquals(source.instant.plusSeconds(7200), next(source.instant, listOf(source), config)!!.at)
        assertNull(next(Instant.parse("2027-01-01T23:00:00Z"), listOf(source), config))
    }

    @Test fun advanceUsesElapsedMinutesAcrossSpringAndAutumnClockChanges() {
        val spring = event.copy(date = LocalDate.of(2026, 3, 29), time = LocalTime.of(3, 15), reminderMinutesBefore = 30)
        val autumn = event.copy(date = LocalDate.of(2026, 10, 25), time = LocalTime.of(2, 15),
            offsetSeconds = 3600, reminderMinutesBefore = 30)
        for ((source, expected) in listOf(spring to "2026-03-29T00:45:00Z", autumn to "2026-10-25T00:45:00Z")) {
            val due = Instant.parse(expected)
            for (zone in TodayTimeZone.entries) {
                val batch = next(due.minusSeconds(1), listOf(source), settings.copy(todayTimeZone = zone))!!
                assertEquals(due, batch.at)
                assertEquals(source.instant, batch.events.single().instant)
                assertEquals(source.instant, next(due, listOf(source), settings.copy(todayTimeZone = zone))!!.at)
            }
        }
    }

    @Test fun eachRecurringOccurrenceHasAdvanceAndEventTimeAlerts() {
        val source = event.copy(reminderMinutesBefore = 15,
            repeat = EventRepeat(RepeatFrequency.DAYS, event.date.plusDays(2), interval = 1))
        val first = event.instant.minusSeconds(900)
        val atEvent = next(first, listOf(source))!!
        assertEquals(event.instant, atEvent.at)
        assertEquals("custom:appointment@2026-11-09", atEvent.events.single().id)
        val next = next(atEvent.at, listOf(source))!!
        assertEquals(first.plusSeconds(86400), next.at)
        assertEquals("custom:appointment@2026-11-10", next.events.single().id)
        assertEquals(event.instant.plusSeconds(2 * 86400), next(first.plusSeconds(2 * 86400), listOf(source))!!.at)
        assertNull(next(event.instant.plusSeconds(2 * 86400), listOf(source)))
    }

    @Test fun savingAfterTheAdvanceTimeFallsBackToEventTimeWithoutReplayingIt() {
        val saved = event.instant.minusSeconds(300)
        val source = event.copy(reminderMinutesBefore = 15, remindersAfter = saved)
        assertEquals(event.instant, next(saved, listOf(source))!!.at)
        assertNull(next(event.instant, listOf(source)))
        val past = source.copy(remindersAfter = event.instant.plusSeconds(1))
        assertNull(next(past.remindersAfter!!, listOf(past), settings.copy(repeatHours = 2)))
    }
}
