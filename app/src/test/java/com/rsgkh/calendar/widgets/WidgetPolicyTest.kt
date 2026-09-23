// SPDX-License-Identifier: Apache-2.0
package com.rsgkh.calendar.widgets

import com.rsgkh.calendar.data.CalendarEvent
import com.rsgkh.calendar.data.DateBasis
import com.rsgkh.calendar.data.EventKind
import com.rsgkh.calendar.data.TodayTimeZone
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import org.junit.Assert.*
import org.junit.Test

class WidgetPolicyTest {
    @Test fun cambodiaRolloverUsesDisplayZone() {
        assertEquals(Instant.parse("2026-09-18T17:00:00Z"),
            WidgetPolicy.nextMidnight(Instant.parse("2026-09-18T16:59:59Z"), ZoneId.of("Asia/Phnom_Penh")))
    }

    @Test fun exactlyMidnightSchedulesTheFollowingMidnight() {
        assertEquals(Instant.parse("2026-09-19T17:00:00Z"),
            WidgetPolicy.nextMidnight(Instant.parse("2026-09-18T17:00:00Z"), ZoneId.of("Asia/Phnom_Penh")))
    }

    @Test fun springDstDayHasTwentyThreeHours() {
        val now = Instant.parse("2026-03-28T23:00:00Z")
        assertEquals(23L, Duration.between(now, WidgetPolicy.nextMidnight(now, ZoneId.of("Europe/Brussels"))).toHours())
    }

    @Test fun autumnDstDayHasTwentyFiveHours() {
        val now = Instant.parse("2026-10-24T22:00:00Z")
        assertEquals(25L, Duration.between(now, WidgetPolicy.nextMidnight(now, ZoneId.of("Europe/Brussels"))).toHours())
    }

    @Test fun windowCrossesYearBoundary() {
        assertEquals(listOf(LocalDate.of(2025, 12, 31), LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 2)),
            WidgetPolicy.window(LocalDate.of(2026, 1, 1)))
    }

    @Test fun windowIncludesLeapDay() {
        assertEquals(LocalDate.of(2028, 2, 29), WidgetPolicy.window(LocalDate.of(2028, 3, 1)).first())
    }

    @Test fun plannerWindowHasFourteenDaysOnEachSideOfToday() {
        val dates = WidgetPolicy.plannerWindow(LocalDate.of(2026, 1, 15))
        assertEquals(29, dates.size)
        assertEquals(LocalDate.of(2026, 1, 1), dates.first())
        assertEquals(LocalDate.of(2026, 1, 15), dates[14])
        assertEquals(LocalDate.of(2026, 1, 29), dates.last())
    }

    @Test fun allDayEventsPrecedeChronologicalTimedEvents() {
        val early = event("early", LocalTime.of(9, 0))
        val late = event("late", LocalTime.of(15, 0))
        val allDay = event("holiday", null, EventKind.HOLIDAY)
        assertEquals(listOf("holiday", "early", "late"), WidgetPolicy.sorted(listOf(late, early, allDay), false).map { it.id })
    }

    @Test fun duplicateKeysAreRemovedButOtherOccurrencesRemain() {
        val first = event("series", LocalTime.NOON)
        val next = first.copy(date = first.date.plusDays(1))
        assertEquals(2, WidgetPolicy.sorted(listOf(first, first, next), false).size)
    }

    @Test fun plannerSortsTimedEventsBeforeUntimedEvents() {
        val early = event("early", LocalTime.of(7, 15))
        val late = event("late", LocalTime.of(15, 0))
        val allDay = event("holiday", null, EventKind.HOLIDAY)
        assertEquals(listOf("early", "late", "holiday"),
            WidgetPolicy.plannerSorted(listOf(allDay, late, early), false).map { it.id })
    }

    @Test fun plannerTitleUsesSixCharactersAndThreeDots() {
        assertEquals("Breakf...", WidgetPolicy.plannerTitle("Breakfast", false))
        assertEquals("Lunch", WidgetPolicy.plannerTitle("Lunch", false))
    }

    @Test fun expandedSizeAllowsMoreContent() {
        val small = WidgetPolicy.layout(320f, 140f, 1f)
        val large = WidgetPolicy.layout(320f, 240f, 1f)
        assertFalse(small.previewTitles)
        assertTrue(large.previewTitles)
        assertTrue(large.expanded)
        assertTrue(large.eventRows > small.eventRows)
    }

    @Test fun largerFontsNeverIncreaseEventDensity() {
        assertTrue(WidgetPolicy.layout(320f, 220f, 1.5f).eventRows <= WidgetPolicy.layout(320f, 220f, 1f).eventRows)
        assertTrue(WidgetPolicy.layout(240f, 120f, 2f).tiny)
    }

    @Test fun monthCardWidthFollowsHeightWithoutAbsoluteCeiling() {
        assertEquals(250f, WidgetPolicy.monthCardMaxWidth(200f), 0.001f)
        // Tall portrait widgets keep widening past the former 456dp ceiling.
        assertEquals(570f, WidgetPolicy.monthCardMaxWidth(456f), 0.001f)
        assertEquals(750f, WidgetPolicy.monthCardMaxWidth(600f), 0.001f)
        // Launchers that report no usable height fall back to the default card width.
        assertEquals(456f, WidgetPolicy.monthCardMaxWidth(0f), 0.001f)
    }

    @Test fun timezoneLabelCollapsesToEmojiOnlyWhenConstrained() {
        assertEquals("🇰🇭", WidgetPolicy.timezoneLabel(TodayTimeZone.CAMBODIA, khmer = true, emojiOnly = true))
        assertEquals("🌐", WidgetPolicy.timezoneLabel(TodayTimeZone.LOCAL, khmer = true, emojiOnly = true))
        assertEquals("🇰🇭", WidgetPolicy.timezoneLabel(TodayTimeZone.CAMBODIA, khmer = false, emojiOnly = true))
        assertEquals("🌐", WidgetPolicy.timezoneLabel(TodayTimeZone.LOCAL, khmer = false, emojiOnly = true))

        assertEquals("🇰🇭 កម្ពុជា", WidgetPolicy.timezoneLabel(TodayTimeZone.CAMBODIA, khmer = true, emojiOnly = false))
        assertEquals("🇰🇭 Cambodia", WidgetPolicy.timezoneLabel(TodayTimeZone.CAMBODIA, khmer = false, emojiOnly = false))
        assertEquals("🌐 ក្នុងតំបន់", WidgetPolicy.timezoneLabel(TodayTimeZone.LOCAL, khmer = true, emojiOnly = false))
        assertEquals("🌐 Local", WidgetPolicy.timezoneLabel(TodayTimeZone.LOCAL, khmer = false, emojiOnly = false))
    }

    private fun event(id: String, time: LocalTime?, kind: EventKind = EventKind.CUSTOM) = CalendarEvent(
        id, LocalDate.of(2026, 9, 18), id, id, kind, DateBasis.USER, time,
    )
}
