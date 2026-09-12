// Copyright (c) 2026 RSG-KH | Apache-2.0 License
package com.rsgkh.calendar

import com.rsgkh.calendar.data.*
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class EventRepositoryTest {
    @Test fun completeWebsiteSnapshotIsBundledAcrossAllThirtyOneYears() {
        val events = (2000..2030).flatMap(EventRepository::forYear)
        val website = events.filter { it.basis == DateBasis.WEBSITE }
        assertEquals(3246, website.size)
        assertEquals(1533, events.count { it.kind == EventKind.HOLY_DAY })
        assertEquals(events.size, events.map { it.key }.distinct().size)
        assertTrue(website.all { it.titleKm.isNotBlank() && it.titleEn.isNotBlank() })
        for (year in 2000..2030) {
            assertTrue(EventRepository.hasBundledYear(year))
            assertTrue(website.count { it.date.year == year } >= 89)
        }
        for (year in listOf(2000, 2024, 2027, 2030)) {
            assertTrue(EventRepository.forDate(LocalDate.of(year, 1, 1)).any { it.titleEn == "New Year's Day" })
        }
    }

    @Test fun outsideDatabaseCoverageOnlyCalculatedObservancesAndHolyDaysAppear() {
        for (year in 1900..2100) {
            if (year in 2000..2030) continue
            assertFalse(EventRepository.hasBundledYear(year))
            val events = EventRepository.forYear(year)
            assertTrue(events.isNotEmpty())
            assertTrue(events.all { it.date.year == year && ((it.kind == EventKind.HOLY_DAY && it.basis == DateBasis.KHMER_LUNAR) ||
                (it.kind == EventKind.OBSERVANCE && it.basis == DateBasis.CALCULATED)) })
            assertTrue(events.any { it.basis == DateBasis.CALCULATED })
            assertTrue(events.all { it.officialSourceUrl == null })
        }
    }

    @Test fun importedEventsIncludeChineseFestivalsAndPreserveMultiDayEntries() {
        val events = EventRepository.forYear(2027)
        assertEquals(listOf(6, 7, 8), events.filter { it.titleEn == "Chinese New Year" }.map { it.date.dayOfMonth })
        assertEquals(LocalDate.of(2027, 2, 5), events.single { it.titleEn == "Chinese New Year's Eve" }.date)
        assertEquals(3, events.count { it.titleEn == "Pchum Ben Festival" })
        assertTrue(events.filter { it.basis == DateBasis.WEBSITE }.all { it.officialSourceUrl == null && it.kind == EventKind.OBSERVANCE })
        assertEquals(113, events.count { it.basis == DateBasis.WEBSITE })
    }

    @Test fun reviewedGovernmentStatusEnrichesMatchingWebsiteOccurrencesOnly() {
        val official = EventRepository.forYear(2025).filter { it.kind == EventKind.HOLIDAY }
        assertEquals(22, official.size)
        assertEquals(22, official.map { it.date }.distinct().size)
        assertTrue(official.all { it.basis == DateBasis.WEBSITE && it.officialSourceUrl == "https://mef.gov.kh/calendar-holiday-2025/" })
        fun ploughing(year: Int) = EventRepository.forYear(year).single { it.titleEn == "Royal Ploughing Ceremony" }.date
        assertEquals(LocalDate.of(2025, 5, 15), ploughing(2025))
        assertEquals(LocalDate.of(2026, 5, 5), ploughing(2026))
        assertTrue(EventRepository.forYear(2024).filter { it.basis == DateBasis.WEBSITE }.all { it.officialSourceUrl == null })
    }
}
