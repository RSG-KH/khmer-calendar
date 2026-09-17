// Copyright (c) 2026 RSG-KH | Apache-2.0 License
package com.rsgkh.calendar

import com.rsgkh.calendar.domain.KhmerCalendar
import com.rsgkh.calendar.data.EventKind
import com.rsgkh.calendar.data.EventRepository
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.YearMonth

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [32])
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
            date = date.plusDays(1)
        }
    }
    @Test fun holyDaysAndShavingDaysAreCorrectAcrossMultipleYears() {
        for (year in 2024..2027) {
            var holyDays = 0
            var shavingDays = 0
            for (m in 1..12) {
                for (d in 1..YearMonth.of(year, m).lengthOfMonth()) {
                    val date = LocalDate.of(year, m, d)
                    val lunar = KhmerCalendar.fromGregorian(date)
                    if (lunar.isHolyDay) {
                        holyDays++
                        val tomorrow = KhmerCalendar.fromGregorian(date.plusDays(1))
                        assertTrue("$date holy day followed by 1 or 9", tomorrow.day in listOf(1, 9))
                    }
                    if (lunar.isShavingDay) {
                        shavingDays++
                        assertTrue("$date shaving day followed by holy day", KhmerCalendar.fromGregorian(date.plusDays(1)).isHolyDay)
                    }
                }
            }
            assertTrue("Expected around 48 holy days in $year, got $holyDays", holyDays in 48..50)
            assertTrue("Expected around 48 shaving days in $year, got $shavingDays", shavingDays in 48..50)
            val expectedShavingDays = holyDays -
                (if (KhmerCalendar.fromGregorian(LocalDate.of(year, 1, 1)).isHolyDay) 1 else 0) +
                (if (KhmerCalendar.fromGregorian(LocalDate.of(year, 12, 31)).isShavingDay) 1 else 0)
            assertEquals("Shaving days must match holy days adjusted for year boundaries in $year", expectedShavingDays, shavingDays)
        }
    }
    @Test fun buddhistYearIncrementsAtVisakBochea() {
        assertEquals(2569, KhmerCalendar.fromGregorian(LocalDate.of(2026, 5, 1)).buddhistYear)
        assertEquals(2570, KhmerCalendar.fromGregorian(LocalDate.of(2026, 5, 2)).buddhistYear)
        assertEquals("Civil year transition does not advance Buddhist year", 2569,
            KhmerCalendar.fromGregorian(LocalDate.of(2026, 1, 1)).buddhistYear)
    }
    @Test fun leapMonthsAndLeapDaysExist() {
        val leapMonthYear = (2020..2030).first { year -> EventRepository.forYear(year).any { KhmerCalendar.fromGregorian(it.date).month == 12 } }
        val months = (1..12).flatMap { m -> (1..YearMonth.of(leapMonthYear, m).lengthOfMonth()).map { KhmerCalendar.fromGregorian(LocalDate.of(leapMonthYear, m, it)).month } }
        assertTrue(months.containsAll(listOf(12, 13)))
        assertFalse(months.contains(7))
        // Exercise the public result instead of reaching into a duplicated leap-year algorithm.
        assertTrue((2020..2030).any { year ->
            (1..30).any { day ->
                val lunar = KhmerCalendar.fromGregorian(LocalDate.of(year, 6, day))
                lunar.month == 6 && lunar.monthLength == 30
            }
        })
    }
    @Test fun officialHolidaysAreScopedAndOverlapIsPreserved() {
        val holidays = EventRepository.forYear(2026).filter { it.kind == EventKind.HOLIDAY }
        assertEquals(22, holidays.size)
        assertEquals(21, holidays.map { it.date }.distinct().size)
        assertEquals(setOf("International Labor Day", "Visak Bochea"), holidays.filter { it.date == LocalDate.of(2026, 5, 1) }.map { it.titleEn }.toSet())
        assertEquals(listOf(10, 11, 12), holidays.filter { it.titleEn == "Pchum Ben Festival" }.map { it.date.dayOfMonth })
        assertEquals(listOf(23, 24, 25), holidays.filter { it.titleEn == "Water Festival" }.map { it.date.dayOfMonth })
        assertEquals(22, EventRepository.forYear(2027).count { it.kind == EventKind.HOLIDAY })
        assertTrue(EventRepository.forYear(2028).none { it.kind == EventKind.HOLIDAY })
        assertTrue(EventRepository.forYear(1900).none { it.kind == EventKind.HOLIDAY })
        val events = EventRepository.forYear(2026)
        assertEquals(events.size, events.map { it.key }.distinct().size)
    }
    @Test(expected = IllegalArgumentException::class)
    fun unsupportedDateIsRejected() { KhmerCalendar.fromGregorian(LocalDate.of(1799, 12, 31)) }
}
