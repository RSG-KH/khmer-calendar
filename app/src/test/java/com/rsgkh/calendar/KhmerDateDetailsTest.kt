// Copyright (c) 2026 RSG-KH | Apache-2.0 License
package com.rsgkh.calendar

import com.rsgkh.calendar.domain.KhmerCalendar
import com.rsgkh.calendar.domain.KhmerDateDetails
import com.rsgkh.calendar.domain.KhmerNewYear
import com.rsgkh.calendar.i18n.CalendarWords
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class KhmerDateDetailsTest {
    @Test fun sharedEngineCorrectsThe2012DateLabels() {
        val before = KhmerDateDetails.fromGregorian(LocalDate.of(2012, 4, 12))
        val first = KhmerDateDetails.fromGregorian(LocalDate.of(2012, 4, 13))
        val middle = KhmerDateDetails.fromGregorian(LocalDate.of(2012, 4, 14))
        val last = KhmerDateDetails.fromGregorian(LocalDate.of(2012, 4, 15))
        assertEquals("Rabbit", before.animalLabel(false))
        assertEquals("Rabbit → Dragon", first.animalLabel(false))
        assertEquals("Dragon", middle.animalLabel(false))
        assertEquals(before.sak, first.sak)
        assertEquals((before.sak + 1) % 10, last.sak)
        assertEquals(before.lunar.buddhistYear, last.lunar.buddhistYear)
    }

    @Test fun khmerDateLabelIncludesTiPrefixForDayNumber() {
        val date = LocalDate.of(2026, 9, 8) // Tuesday, 8 September 2026
        assertEquals("ថ្ងៃអង្គារ ទី៨ ខែកញ្ញា", CalendarWords.date(date, true))
    }

    @Test fun suppliedScreenshotDateMatchesTraditionalLabels() {
        val details = KhmerDateDetails.fromGregorian(LocalDate.of(2026, 9, 10))
        assertEquals(6, details.animalYear)
        assertEquals(8, details.sak)
        assertEquals("Horse", details.animalLabel(false))
        assertEquals("13 Roach (Waning) · Srapon", details.lunar.fullLabel(false))
        assertEquals("Atthasak", com.rsgkh.calendar.i18n.L.text("calendar.sak.8", false))
        assertEquals("13 Roach (Waning) · Srapon\nYear of the Horse · Atthasak\nBuddhist Era 2570", details.fullEnglishDate())
        assertEquals("ថ្ងៃព្រហស្បតិ៍ ១៣រោច ខែស្រាពណ៍ ឆ្នាំមមី អដ្ឋស័ក ពុទ្ធសករាជ ២៥៧០ ត្រូវនឹងថ្ងៃទី១០ ខែកញ្ញា ឆ្នាំ២០២៦", details.fullKhmerDate())
        assertEquals("September 10, 2026", details.gregorianLabel)
        assertTrue(details.lunar.isShavingDay)
        assertFalse(details.lunar.isHolyDay)

        val sep24 = KhmerDateDetails.fromGregorian(LocalDate.of(2026, 9, 24))
        assertEquals("13 Koeut (Waxing) · Phutrobot", sep24.lunar.fullLabel(false))
        assertEquals("13 Koeut (Waxing) · Phutrobot\nYear of the Horse · Atthasak\nBuddhist Era 2570", sep24.fullEnglishDate())
        assertEquals("១៣កើត ខែភទ្របទ\nឆ្នាំមមី អដ្ឋស័ក", sep24.lunarSummary(true))
        assertEquals("13 Koeut (Waxing) · Phutrobot\nYear of the Horse · Atthasak", sep24.lunarSummary(false))

        val sep21 = KhmerDateDetails.fromGregorian(LocalDate.of(2026, 9, 21))
        assertEquals("១០កើត ខែភទ្របទ\nឆ្នាំមមី អដ្ឋស័ក", sep21.lunarSummary(true))
    }

    @Test fun animalSakAndBuddhistYearsHaveSeparateTransitions() {
        val before = KhmerDateDetails.fromGregorian(LocalDate.of(2026, 4, 13))
        val first = KhmerDateDetails.fromGregorian(LocalDate.of(2026, 4, 14))
        val last = KhmerDateDetails.fromGregorian(LocalDate.of(2026, 4, 16))
        val visak = KhmerDateDetails.fromGregorian(LocalDate.of(2026, 5, 1))
        val buddhistNewYear = KhmerDateDetails.fromGregorian(LocalDate.of(2026, 5, 2))
        assertEquals("Snake", before.animalLabel(false))
        assertEquals("Snake → Horse", first.animalLabel(false))
        assertEquals(7, first.sak)
        assertEquals(8, last.sak)
        assertEquals(2569, last.lunar.buddhistYear)
        assertEquals(2570, buddhistNewYear.lunar.buddhistYear)
        assertEquals(visak.animalYear, buddhistNewYear.animalYear)
        assertEquals(visak.sak, buddhistNewYear.sak)
        for (year in 1900..2100) {
            val festival = KhmerNewYear.forYear(year)
            val old = KhmerDateDetails.fromGregorian(festival.start.minusDays(1))
            val new = KhmerDateDetails.fromGregorian(festival.start)
            val final = KhmerDateDetails.fromGregorian(festival.dates.last())
            assertEquals("$year", (old.animalYear + 1) % 12, new.animalYear)
            assertTrue(new.animalYearChangesToday)
            assertEquals("$year", (old.sak + 1) % 10, final.sak)
        }
        KhmerDateDetails.fromGregorian(KhmerCalendar.minDate)
        KhmerDateDetails.fromGregorian(KhmerCalendar.maxDate)
    }

    @Test fun shavingDaysPrecedeHolyDaysIncludingShortMonthEnds() {
        var date = KhmerCalendar.minDate
        while (date < KhmerCalendar.maxDate) {
            val lunar = KhmerCalendar.fromGregorian(date)
            assertEquals("$date", KhmerCalendar.fromGregorian(date.plusDays(1)).isHolyDay, lunar.isShavingDay)
            assertFalse("$date", lunar.isHolyDay && lunar.isShavingDay)
            date = date.plusDays(1)
        }
    }
}
