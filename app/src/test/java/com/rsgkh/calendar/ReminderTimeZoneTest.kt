// Copyright (c) 2026 RSG-KH | Apache-2.0 License
package com.rsgkh.calendar

import com.rsgkh.calendar.data.*
import com.rsgkh.calendar.notifications.ReminderPlanner
import org.junit.Assert.*
import org.junit.Test
import java.time.*

class ReminderTimeZoneTest {
    private val settings = AppSettings(notificationsEnabled = true, showHolyDaysInEvents = true)
    private val brussels = ZoneId.of("Europe/Brussels")
    private fun events(date: LocalDate) = listOf(EventKind.HOLIDAY, EventKind.OBSERVANCE, EventKind.HOLY_DAY).map {
        CalendarEvent("test-$it", date, "Test", "Test", it, DateBasis.CALCULATED)
    }
    private fun next(now: Instant, date: LocalDate, config: AppSettings = settings, zone: ZoneId = brussels) =
        ReminderPlanner.next(now, config, emptyList(), zone) { year -> if (year == date.year) events(date) else emptyList() }

    @Test fun builtInCategoriesUseTheSelectedZoneForPushTimeAndMidnight() {
        val date = LocalDate.of(2026, 9, 24)
        val cases = listOf(
            Triple("Europe/Brussels", "2026-09-24T03:00:00Z", "2026-09-24T22:00:00Z"),
            Triple("America/Los_Angeles", "2026-09-24T12:00:00Z", "2026-09-25T07:00:00Z"),
            Triple("Asia/Kolkata", "2026-09-23T23:30:00Z", "2026-09-24T18:30:00Z"),
        )
        for ((zoneId, localPush, localMidnight) in cases) for (choice in TodayTimeZone.entries) {
            val expectedPush = Instant.parse(if (choice == TodayTimeZone.LOCAL) localPush else "2026-09-23T22:00:00Z")
            val expectedEnd = Instant.parse(if (choice == TodayTimeZone.LOCAL) localMidnight else "2026-09-24T17:00:00Z")
            val batch = next(expectedPush.minusSeconds(60), date, settings.copy(todayTimeZone = choice), ZoneId.of(zoneId))!!
            assertEquals(expectedPush, batch.at)
            assertEquals(date, batch.date)
            assertEquals(events(date).map { it.kind }.toSet(), batch.events.map { it.kind }.toSet())
            assertEquals(setOf(expectedEnd), batch.expiresAt.values.toSet())
        }
    }

    @Test fun eventSearchUsesTheLocalYearOnBothSidesOfTheDateLine() {
        val cases = listOf(
            Triple("Pacific/Honolulu", "2027-01-01T08:00:00Z", "2027-01-01T09:00:00Z"),
            Triple("Pacific/Kiritimati", "2026-12-31T10:30:00Z", "2026-12-31T11:00:00Z"),
        )
        for ((zoneId, nowText, dueText) in cases) {
            val zone = ZoneId.of(zoneId)
            val due = Instant.parse(dueText)
            val localDate = due.atZone(zone).toLocalDate()
            val requestedYears = mutableListOf<Int>()
            val pushMinutes = if (zoneId == "Pacific/Honolulu") 23 * 60 else 60
            val batch = ReminderPlanner.next(Instant.parse(nowText), settings.copy(pushMinutes = pushMinutes), emptyList(), zone) { year ->
                requestedYears.add(year)
                if (year == localDate.year) events(localDate) else emptyList()
            }!!
            assertEquals(localDate.year, requestedYears.first())
            assertEquals(due, batch.at)
            assertEquals(localDate, batch.date)
        }
    }

    @Test fun daylightSavingGapsAndRepeatedClockTimesScheduleOneInitialReminder() {
        val cases = listOf(
            Triple("2026-03-29", "2026-03-29T01:30:00Z", "2026-03-29T22:00:00Z"),
            Triple("2026-10-25", "2026-10-25T00:30:00Z", "2026-10-25T23:00:00Z"),
        )
        val config = settings.copy(pushMinutes = 2 * 60 + 30, repeatHours = 0)
        for ((dateText, dueText, endText) in cases) {
            val date = LocalDate.parse(dateText)
            val due = Instant.parse(dueText)
            val batch = next(due.minusSeconds(60), date, config)!!
            assertEquals(due, batch.at)
            assertEquals(setOf(Instant.parse(endText)), batch.expiresAt.values.toSet())
            assertNull(next(due, date, config))
        }
    }

    @Test fun repeatsUseElapsedHoursAndStopAtLocalMidnightAcrossDaylightSaving() {
        val date = LocalDate.of(2026, 3, 29)
        val config = settings.copy(pushMinutes = 30, repeatHours = 2)
        val first = next(Instant.parse("2026-03-28T23:29:00Z"), date, config)!!
        assertEquals(Instant.parse("2026-03-28T23:30:00Z"), first.at)
        val repeat = next(first.at, date, config)!!
        assertEquals(first.at.plusSeconds(7200), repeat.at)
        assertEquals(LocalTime.of(3, 30), repeat.at.atZone(brussels).toLocalTime())
        val last = next(Instant.parse("2026-03-29T21:29:00Z"), date, config)!!
        assertEquals(Instant.parse("2026-03-29T21:30:00Z"), last.at)
        assertNull(next(last.at, date, config))
    }
}
