// Copyright (c) 2026 RSG-KH | Apache-2.0 License
package com.rsgkh.calendar

import com.rsgkh.calendar.domain.KhmerCalendar
import com.rsgkh.calendar.data.EventKind
import com.rsgkh.calendar.data.EventRepository
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class KhmerCalendarTest {
    private fun check(date: String, day: Int, waxing: Boolean, month: Int, year: Int) {
        val lunar = KhmerCalendar.fromGregorian(LocalDate.parse(date))
        assertEquals(date, listOf(day, waxing, month, year), listOf(lunar.day, lunar.waxing, lunar.month, lunar.buddhistYear))
    }
    @Test fun referenceDatesAndOfficialFestivalAnchors() {
        check("2021-05-28", 2, false, 6, 2565)
        check("2026-05-01", 15, true, 5, 2569)
        check("2026-05-02", 1, false, 5, 2570)
        check("2026-09-10", 13, false, 8, 2570)
        check("2026-09-11", 14, false, 8, 2570)
        check("2026-09-12", 1, true, 9, 2570)
        check("2026-10-11", 15, false, 9, 2570)
        check("2026-11-24", 15, true, 11, 2570)
    }
    @Test fun everySupportedDayHasAContinuousLunarDate() {
        var date = KhmerCalendar.minDate
        while (date < KhmerCalendar.maxDate) {
            val a = KhmerCalendar.fromGregorian(date)
            val b = KhmerCalendar.fromGregorian(date.plusDays(1))
            assertTrue("$date", a.day in 1..15)
            val ordinal = a.day + if (a.waxing) 0 else 15
            val nextOrdinal = b.day + if (b.waxing) 0 else 15
            if (ordinal == a.monthLength) {
                assertEquals("$date", 1, nextOrdinal)
                assertNotEquals("$date", a.month, b.month)
                assertTrue("$date", a.isHolyDay)
            } else {
                assertEquals("$date", ordinal + 1, nextOrdinal)
                assertEquals("$date", a.month, b.month)
            }
            if (a.buddhistYear != b.buddhistYear) {
                assertEquals("$date", a.buddhistYear + 1, b.buddhistYear)
                assertEquals("$date", 5, b.month)
                assertEquals("$date", 1, b.day)
                assertFalse("$date", b.waxing)
            }
            date = date.plusDays(1)
        }
    }
    @Test fun eachYearHasOneBuddhistEraTransition() {
        for (year in 1800..2200) {
            var transitions = 0
            var date = LocalDate.of(year, 1, 2)
            while (date.year == year) {
                if (KhmerCalendar.fromGregorian(date).buddhistYear != KhmerCalendar.fromGregorian(date.minusDays(1)).buddhistYear) transitions++
                date = date.plusDays(1)
            }
            assertEquals("$year", 1, transitions)
        }
    }
    @Test fun shortMonthEndsOnFourteenthWaningHolyDay() {
        assertTrue(KhmerCalendar.fromGregorian(LocalDate.of(2026, 9, 11)).isHolyDay)
        assertFalse(KhmerCalendar.fromGregorian(LocalDate.of(2026, 9, 10)).isHolyDay)
        assertFalse(KhmerCalendar.fromGregorian(LocalDate.of(2026, 9, 12)).isHolyDay)
    }
    @Test fun buddhistYearDoesNotChangeAtGregorianNewYear() {
        assertEquals(KhmerCalendar.fromGregorian(LocalDate.of(2025, 12, 31)).buddhistYear,
            KhmerCalendar.fromGregorian(LocalDate.of(2026, 1, 1)).buddhistYear)
    }
    @Test fun leapMonthsAndLeapDaysExist() {
        val leapMonthYear = (2020..2030).first { year -> EventRepository.forYear(year).any { KhmerCalendar.fromGregorian(it.date).month == 12 } }
        val months = (1..12).flatMap { m -> (1..YearMonth.of(leapMonthYear, m).lengthOfMonth()).map { KhmerCalendar.fromGregorian(LocalDate.of(leapMonthYear, m, it)).month } }
        assertTrue(months.containsAll(listOf(12, 13)))
        assertFalse(months.contains(7))
        assertTrue((2540..2580).any { KhmerCalendar.leapType(it) == 2 })
    }
    @Test fun officialHolidaysAreScopedAndOverlapIsPreserved() {
        val holidays = EventRepository.forYear(2026).filter { it.kind == EventKind.HOLIDAY }
        assertEquals(22, holidays.size)
        assertEquals(21, holidays.map { it.date }.distinct().size)
        assertEquals(setOf("International Labor Day", "Visak Bochea"), holidays.filter { it.date == LocalDate.of(2026, 5, 1) }.map { it.titleEn }.toSet())
        assertEquals(listOf(10, 11, 12), holidays.filter { it.titleEn == "Pchum Ben Festival" }.map { it.date.dayOfMonth })
        assertEquals(listOf(23, 24, 25), holidays.filter { it.titleEn == "Water Festival" }.map { it.date.dayOfMonth })
        assertTrue(EventRepository.forYear(2027).none { it.kind == EventKind.HOLIDAY })
        assertTrue(EventRepository.forYear(1900).none { it.kind == EventKind.HOLIDAY })
        val events = EventRepository.forYear(2026)
        assertEquals(events.size, events.map { it.key }.distinct().size)
    }
    @Test(expected = IllegalArgumentException::class)
    fun unsupportedDateIsRejected() { KhmerCalendar.fromGregorian(LocalDate.of(1799, 12, 31)) }
}
