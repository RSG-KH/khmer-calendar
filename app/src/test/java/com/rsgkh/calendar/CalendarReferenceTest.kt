// Copyright (c) 2026 RSG-KH | Apache-2.0 License
package com.rsgkh.calendar

import com.rsgkh.calendar.domain.KhmerCalendar
import com.rsgkh.calendar.domain.KhmerNewYear
import org.junit.Assert.*
import org.junit.Test
import java.security.MessageDigest
import java.time.LocalDate

class CalendarReferenceTest {
    private val references get() = javaClass.getResourceAsStream("/momentkh-reference.txt")!!
        .bufferedReader().use { it.readLines() }.filterNot { it.startsWith("#") || it.isBlank() }.map { it.split('|') }

    @Test fun allSupportedDatesMatchPinnedMomentKh() {
        val mismatches = mutableListOf<Int>()
        for ((yearText, expected) in references) {
            val year = yearText.toInt()
            val hash = MessageDigest.getInstance("SHA-256")
            var date = LocalDate.of(year, 1, 1)
            while (date.year == year) {
                val lunar = KhmerCalendar.fromGregorian(date)
                hash.update("$date,${lunar.day},${if (lunar.waxing) 0 else 1},${lunar.month},${lunar.buddhistYear}\n".toByteArray())
                date = date.plusDays(1)
            }
            if (expected != hash.digest().joinToString("") { "%02x".format(it) }) mismatches.add(year)
        }
        assertEquals("Gregorian years with at least one reference discrepancy", emptyList<Int>(), mismatches)
    }

    @Test fun allNewYearStartsMatchPinnedMomentKh() {
        val mismatches = mutableListOf<String>()
        for ((year, _, expected) in references) {
            val actual = KhmerNewYear.forYear(year.toInt())
            if (actual.start.toString() != expected) mismatches.add("$year: $expected != ${actual.start}")
            assertTrue(actual.days in 3..4)
            assertEquals(4, actual.start.monthValue)
        }
        assertEquals(emptyList<String>(), mismatches)
    }

    @Test fun newYearMatchesGovernmentFestivalDatesIncludingFourDayYear() {
        // National Radio 2024: https://rnk.gov.kh/index.php/interior-minister-calls-on-authorities-at-all-levels-to-be-well-prepared-to-ensure-safety-security-and-social-order-during-the-coming-traditional-khmer-new-year-celebration
        // MEF 2025 and Legal Reform Committee 2026 annual calendars (tools/reference-government-holidays.json).
        assertEquals(listOf(13, 14, 15, 16), KhmerNewYear.forYear(2024).dates.map { it.dayOfMonth })
        assertEquals(listOf(14, 15, 16), KhmerNewYear.forYear(2025).dates.map { it.dayOfMonth })
        assertEquals(listOf(14, 15, 16), KhmerNewYear.forYear(2026).dates.map { it.dayOfMonth })
    }
}
