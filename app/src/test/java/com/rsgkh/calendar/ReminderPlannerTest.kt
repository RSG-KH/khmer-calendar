// Copyright (c) 2026 RSG-KH | Apache-2.0 License
package com.rsgkh.calendar

import com.rsgkh.calendar.data.*
import com.rsgkh.calendar.notifications.ReminderPlanner
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.*

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [32])
class ReminderPlannerTest {
    private val day = LocalDate.of(2026, 9, 24)
    private val settings = AppSettings(notificationsEnabled = true, showHolyDaysInEvents = false, repeatHours = 4,
        todayTimeZone = TodayTimeZone.CAMBODIA)
    private val event = CalendarEvent("test", day, "ថ្ងៃពិសេស", "Special day", EventKind.OBSERVANCE, DateBasis.RECORDED)
    private fun at(date: LocalDate = day, hour: Int, minute: Int = 0) = date.atTime(hour, minute).atZone(CAMBODIA_ZONE).toInstant()
    private fun next(now: Instant, config: AppSettings = settings, custom: List<CustomEvent> = emptyList()) =
        ReminderPlanner.next(now, config, custom) { year -> if (year == 2026) listOf(event) else emptyList() }

    @Test fun websiteEventsUseTheSameNotificationPlannerOutsideOfficialSnapshotYears() {
        val date = LocalDate.of(2027, 2, 5)
        val next = ReminderPlanner.next(at(date, 4, 59), settings.copy(repeatHours = 0), emptyList())!!
        assertEquals(at(date, 5), next.at)
        assertEquals(listOf("Chinese New Year's Eve"), next.events.map { it.titleEn })
    }

    @Test fun calculatedFallbackUsesNormalPushTimeAcrossTheCoverageBoundary() {
        val date = LocalDate.of(2031, 1, 1)
        val batch = ReminderPlanner.next(at(date, 4, 59), settings.copy(repeatHours = 0), emptyList())!!
        assertEquals(at(date, 5), batch.at)
        assertTrue(batch.events.any { it.id == "new_year_day" && it.basis == DateBasis.CALCULATED })
        assertTrue(batch.events.none { it.kind == EventKind.HOLIDAY })
    }

    @Test fun dailyRemindersUseCambodiaTimeAndRepeatOnlyWithinTheEventDay() {
        val first = next(at(hour = 4, minute = 59))!!
        assertEquals(Instant.parse("2026-09-23T22:00:00Z"), first.at)
        assertEquals(day, first.date)
        assertEquals(at(hour = 9), next(first.at)!!.at)
        assertEquals(at(hour = 13), next(at(hour = 11))!!.at) // Missed slots are not replayed.
        assertNull(next(at(hour = 23, minute = 59)))
        for (hours in listOf(2, 4, 6, 8, 12)) assertEquals(at(hour = 5 + hours), next(first.at, settings.copy(repeatHours = hours))!!.at)
    }
    @Test fun customEventsStartAtTheirOwnTimeAndNeverLeakIntoAnotherDay() {
        val custom = CustomEvent(title = "Appointment", date = day, time = LocalTime.of(6, 30))
        val batch = next(at(hour = 6), custom = listOf(custom))!!
        assertEquals(at(hour = 6, minute = 30), batch.at)
        assertEquals(EventKind.CUSTOM, batch.events.single().kind)
        val late = custom.copy(time = LocalTime.of(23, 30))
        assertEquals(at(hour = 23, minute = 30), next(at(hour = 23), custom = listOf(late))!!.at)
        assertNull(next(at(hour = 23, minute = 31), custom = listOf(late)))
        assertNull(next(at(hour = 23), custom = listOf(late.copy(remindersEligible = false))))
    }
    @Test fun simultaneousEventsAreGroupedAndDisablingCancelsEveryCategory() {
        val custom = CustomEvent(title = "At five", date = day, time = LocalTime.of(5, 0))
        assertEquals(2, next(at(hour = 4), custom = listOf(custom))!!.events.size)
        assertNull(next(at(hour = 4), settings.copy(notificationsEnabled = false), listOf(custom)))
        val holy = event.copy(kind = EventKind.HOLY_DAY)
        assertNull(ReminderPlanner.next(at(hour = 4), settings, emptyList()) { listOf(holy) })
    }
    @Test fun repeatOffStillSchedulesEachEventOnceAndSkipsPastSlots() {
        val once = settings.copy(repeatHours = 0)
        assertEquals(at(hour = 5), next(at(hour = 4), once)!!.at)
        assertNull(next(at(hour = 5), once))
        val custom = CustomEvent(title = "Appointment", date = day, time = LocalTime.of(6, 30))
        assertEquals(at(hour = 6, minute = 30), next(at(hour = 5), once, listOf(custom))!!.at)
        assertNull(next(at(hour = 6, minute = 30), once, listOf(custom)))
        val tomorrow = custom.copy(date = day.plusDays(1))
        assertEquals(tomorrow.instant, next(at(hour = 23), once, listOf(custom, tomorrow))!!.at)
    }
    @Test fun categoryChoicesFilterInitialAndRepeatRemindersIndependently() {
        val kinds = listOf(EventKind.CUSTOM, EventKind.HOLIDAY, EventKind.OBSERVANCE, EventKind.HOLY_DAY)
        val builtIn = kinds.drop(1).map { event.copy(id = it.name, kind = it) }
        val custom = CustomEvent(title = "Personal reminder", date = day, time = LocalTime.of(5, 0))
        for (mask in 0..15) for (showHolyDays in listOf(false, true)) {
            val enabled = kinds.filterIndexed { index, _ -> mask and (1 shl index) != 0 }.toSet()
            val config = settings.copy(
                showHolyDaysInEvents = showHolyDays,
                pushCustomEvents = EventKind.CUSTOM in enabled,
                pushHolidays = EventKind.HOLIDAY in enabled,
                pushObservances = EventKind.OBSERVANCE in enabled,
                pushHolyDays = EventKind.HOLY_DAY in enabled,
            )
            val expected = enabled.filter { it != EventKind.HOLY_DAY || showHolyDays }.toSet()
            for ((hour, expectedHour) in listOf(4 to 5, 5 to 9)) {
                val batch = ReminderPlanner.next(at(hour = hour), config, listOf(custom)) { year ->
                    if (year == day.year) builtIn else emptyList()
                }
                assertEquals("Categories $enabled, holy days visible $showHolyDays, after $hour:00",
                    expected, batch?.events?.map { it.kind }?.toSet().orEmpty())
                if (expected.isEmpty()) assertNull(batch) else assertEquals(at(hour = expectedHour), batch!!.at)
            }
        }
    }
    @Test fun futureCustomEventsAndYearBoundariesStayScheduled() {
        val future = CustomEvent(title = "Future", date = LocalDate.of(2099, 12, 31), time = LocalTime.of(23, 45))
        val next = ReminderPlanner.next(at(date = LocalDate.of(2026, 12, 31), hour = 23), settings, listOf(future)) { emptyList() }
        assertEquals(future.instant, next!!.at)
        val newYear = event.copy(date = LocalDate.of(2027, 1, 1))
        val rollover = ReminderPlanner.next(at(date = LocalDate.of(2026, 12, 31), hour = 23), settings, emptyList()) { year ->
            if (year == 2027) listOf(newYear) else emptyList()
        }
        assertEquals(at(date = newYear.date, hour = 5), rollover!!.at)
    }
}
