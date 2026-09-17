// Copyright (c) 2026 RSG-KH | Apache-2.0 License
package com.rsgkh.calendar

import com.rsgkh.calendar.data.*
import com.rsgkh.calendar.domain.*
import com.rsgkh.calendar.notifications.ReminderPlanner
import org.junit.Assert.*
import org.junit.Test
import java.time.*
import java.time.temporal.ChronoUnit

class CustomEventRepeatTest {
    private fun date(value: String) = LocalDate.parse(value)
    private fun rule(frequency: RepeatFrequency, end: String, thirty: Boolean = false, february: Boolean = false, interval: Long = 3) =
        EventRepeat(frequency, date(end), interval, thirty, february)
    private fun event(start: String, rule: EventRepeat, zone: String = "Europe/Brussels", time: String = "09:00") =
        CustomEvent(id = "series", title = "Repeat", date = date(start), time = LocalTime.parse(time), zoneId = zone, repeat = rule)
    private val onlyCustom = AppSettings(notificationsEnabled = true, pushHolidays = false, pushObservances = false, pushHolyDays = false)

    @Test fun monthEndFallbacksAreIndependentAndNeverMoveTheAnchor() {
        val start = date("2026-01-31")
        val strict = rule(RepeatFrequency.MONTHLY, "2026-12-31")
        assertEquals(listOf(1, 3, 5, 7, 8, 10, 12), strict.dates(start).map { it.monthValue }.toList())
        assertEquals(11, strict.copy(includeThirty = true).dates(start).count())
        assertEquals(8, strict.copy(includeFebruary = true).dates(start).count())
        val both = strict.copy(includeThirty = true, includeFebruary = true).dates(start).toList()
        assertEquals(12, both.size)
        assertEquals(date("2026-02-28"), both[1])
        assertEquals(date("2026-03-31"), both[2])
        assertEquals(date("2026-05-31"), both[4])
    }

    @Test fun missingDatesOnlyAffectTheChosenRange() {
        val start = date("2026-03-29")
        val strict = rule(RepeatFrequency.MONTHLY, "2026-12-31")
        assertFalse(strict.preview(start).affectsFebruary)
        assertFalse(strict.copy(until = date("2027-02-27")).preview(start).affectsFebruary)
        val preview = strict.copy(until = date("2027-02-28")).preview(start)
        assertTrue(preview.affectsFebruary)
        assertFalse(preview.affectsThirty)
        assertEquals(listOf(date("2027-02-28")), preview.skipped)
    }

    @Test fun daysAndWeeksUseInclusiveEndDates() {
        val start = date("2026-01-01")
        assertEquals(listOf(1, 4, 7, 10), rule(RepeatFrequency.DAYS, "2026-01-10").dates(start).map { it.dayOfMonth }.toList())
        assertEquals(listOf(1, 4, 7), rule(RepeatFrequency.DAYS, "2026-01-09").dates(start).map { it.dayOfMonth }.toList())
        assertEquals(listOf(8, 15), rule(RepeatFrequency.WEEKLY, "2026-01-15").dates(start, date("2026-01-02")).map { it.dayOfMonth }.toList())
        assertFalse(rule(RepeatFrequency.DAYS, "2026-01-10", interval = 0).isValid(start))
        assertFalse(rule(RepeatFrequency.MONTHLY, "2025-12-31").isValid(start))
        assertFalse(rule(RepeatFrequency.YEARLY, "2201-01-01").isValid(start))
        assertEquals(listOf(start), rule(RepeatFrequency.DAYS, "2200-12-31", interval = 9_007_199_254_740_991L).dates(start).toList())
        assertEquals(146462, rule(RepeatFrequency.DAYS, "2200-12-31", interval = 1).dates(date("1800-01-01")).count())
    }

    @Test fun leapCenturiesMatchGregorianRules() {
        val leap = rule(RepeatFrequency.YEARLY, "2104-02-29")
        assertEquals(listOf(2096, 2104), leap.dates(date("2096-02-29")).map { it.year }.toList())
        assertEquals(9, leap.copy(includeFebruary = true).dates(date("2096-02-29")).count())
        assertEquals(listOf(1996, 2000), rule(RepeatFrequency.YEARLY, "2000-02-29").dates(date("1996-02-29")).map { it.year }.toList())
    }

    @Test fun schedulesMatchIndependentDayByDayWalkAcrossPwaCrossCheckCases() {
        val options = listOf(1L, 3L, 31L, 9_007_199_254_740_991L).map { EventRepeat(RepeatFrequency.DAYS, date("2200-12-31"), it) } +
            EventRepeat(RepeatFrequency.WEEKLY, date("2200-12-31")) +
            listOf(false, true).flatMap { thirty -> listOf(false, true).map { feb -> rule(RepeatFrequency.MONTHLY, "2200-12-31", thirty, feb) } } +
            listOf(false, true).map { rule(RepeatFrequency.YEARLY, "2200-12-31", february = it) }
        var comparisons = 0
        for (year in listOf(1800, 1900, 1999, 2000, 2026, 2028, 2099, 2100, 2199)) {
            for (suffix in listOf("01-01", "01-29", "01-30", "01-31", "02-28", "02-29", "03-31", "08-31", "12-31")) {
                val start = runCatching { date("$year-$suffix") }.getOrNull() ?: continue
                for (option in options) {
                    val repeat = option.copy(until = minOf(start.plusDays(735), date("2200-12-31")))
                    val dates = mutableListOf<LocalDate>()
                    val skips = mutableListOf<LocalDate>()
                    var current = start
                    while (current <= repeat.until) {
                        val days = ChronoUnit.DAYS.between(start, current)
                        if (repeat.frequency == RepeatFrequency.DAYS || repeat.frequency == RepeatFrequency.WEEKLY) {
                            if (days % (if (repeat.frequency == RepeatFrequency.DAYS) repeat.interval else 7) == 0L) dates.add(current)
                        } else if (repeat.frequency == RepeatFrequency.MONTHLY || current.month == start.month) {
                            if (current.dayOfMonth == start.dayOfMonth) dates.add(current)
                            else if (current.dayOfMonth < start.dayOfMonth && current.plusDays(1).dayOfMonth == 1) {
                                if (if (current.monthValue == 2) repeat.includeFebruary else repeat.includeThirty) dates.add(current) else skips.add(current)
                            }
                        }
                        current = current.plusDays(1)
                    }
                    val preview = repeat.preview(start)
                    assertEquals("$start $repeat", dates, preview.dates)
                    assertEquals(skips, preview.skipped)
                    val first = start.plusDays(37)
                    val last = repeat.until.minusDays(13)
                    assertEquals(dates.filter { it in first..last }, repeat.dates(start, first, last).toList())
                    comparisons += 2
                }
            }
        }
        assertEquals(1628, comparisons)
    }

    @Test fun dstGapsShiftForwardAndFutureFoldsUseFirstOffsetWhileAnchorKeepsSavedOffset() {
        val gap = event("2026-03-22", rule(RepeatFrequency.WEEKLY, "2026-04-05"), time = "02:30")
        assertEquals(LocalTime.of(3, 30), gap.atOccurrence(date("2026-03-29")).toLocalTime())
        assertEquals(LocalTime.of(2, 30), gap.atOccurrence(date("2026-04-05")).toLocalTime())
        val fold = event("2026-10-18", rule(RepeatFrequency.WEEKLY, "2026-11-01"), time = "02:30")
        assertEquals(Instant.parse("2026-10-25T00:30:00Z"), fold.atOccurrence(date("2026-10-25")).toInstant())
        val anchor = fold.copy(date = date("2026-10-25"), offsetSeconds = 3600)
        assertEquals(Instant.parse("2026-10-25T01:30:00Z"), anchor.atOccurrence(anchor.date).toInstant())
    }

    @Test fun monthQueriesPartitionYearAcrossDstAndDateLine() {
        for ((source, display) in listOf("Pacific/Kiritimati" to "Pacific/Pago_Pago", "Pacific/Pago_Pago" to "Pacific/Kiritimati",
            "Europe/Brussels" to "Asia/Phnom_Penh", "Asia/Phnom_Penh" to "Europe/Brussels")) {
            for (frequency in RepeatFrequency.entries) {
                val event = event("2025-12-31", rule(frequency, "2027-01-02", true, true), source, "23:45")
                val zone = ZoneId.of(display)
                val all = event.occurrences(date("2026-01-01"), date("2026-12-31"), zone)
                val months = (1..12).flatMap { val month = YearMonth.of(2026, it); event.occurrences(month.atDay(1), month.atEndOfMonth(), zone) }
                assertEquals(all, months)
                assertEquals(all.size, all.map { it.key }.distinct().size)
                assertTrue(all.all { it.customSeriesId == event.id })
            }
        }
    }

    @Test fun remindersFindFutureDatesFromPastAnchorsAndDistantLeapYears() {
        val monthly = event("2026-01-31", rule(RepeatFrequency.MONTHLY, "2026-12-31", true, true))
        assertEquals(Instant.parse("2026-04-30T07:00:00Z"), ReminderPlanner.next(Instant.parse("2026-04-01T00:00:00Z"), onlyCustom, listOf(monthly)) { emptyList() }!!.at)
        val leap = event("2096-02-29", rule(RepeatFrequency.YEARLY, "2104-02-29"))
        assertEquals(leap.atOccurrence(date("2104-02-29")).toInstant(), ReminderPlanner.next(Instant.parse("2097-01-01T00:00:00Z"), onlyCustom, listOf(leap)) { emptyList() }!!.at)
        assertNull(ReminderPlanner.next(Instant.parse("2105-01-01T00:00:00Z"), onlyCustom, listOf(leap)) { emptyList() })
        assertNull(ReminderPlanner.next(monthly.instant.minusSeconds(1), onlyCustom.copy(pushCustomEvents = false), listOf(monthly)) { emptyList() })
    }

    @Test fun newlySavedSeriesDoesNotSendRetrospectiveSameDayReminders() {
        val daily = event("2026-01-01", rule(RepeatFrequency.DAYS, "2026-01-03", interval = 1), "UTC").copy(remindersAfter = Instant.parse("2026-01-02T10:00:00Z"))
        val next = ReminderPlanner.next(daily.remindersAfter!!, onlyCustom.copy(repeatHours = 2), listOf(daily)) { emptyList() }!!
        assertEquals(Instant.parse("2026-01-03T09:00:00Z"), next.at)
        assertEquals("custom:series@2026-01-03", next.events.single().id)
        val repeat = ReminderPlanner.next(next.at, onlyCustom.copy(repeatHours = 2), listOf(daily)) { emptyList() }!!
        assertEquals(next.at.plusSeconds(7200), repeat.at)
        assertEquals(Instant.parse("2026-01-04T00:00:00Z"), repeat.expiresAt.values.single())
    }
}
