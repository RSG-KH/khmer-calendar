// Copyright (c) 2026 RSG-KH | Apache-2.0 License
package com.rsgkh.calendar

import com.rsgkh.calendar.data.*
import com.rsgkh.calendar.domain.KhmerCalendar
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [32])
class EventRepositoryTest {
    @Test fun dynamicCatalogCoverageSpansAllSupportedYearsFrom1800To2200() {
        assertEquals(1800..2200, EventRepository.coveredYears)
        for (year in listOf(1800, 1900, 1975, 2000, 2025, 2050, 2100, 2200)) {
            assertTrue(EventRepository.hasBundledYear(year))
            val events = EventRepository.forYear(year)
            assertTrue(events.isNotEmpty())
            assertEquals("Unique keys in $year", events.size, events.map { it.key }.distinct().size)
            assertTrue(events.all { it.date.year == year })
            assertTrue(events.any { it.kind == EventKind.HOLY_DAY && it.basis == DateBasis.KHMER_LUNAR })
        }
        for (year in listOf(2000, 2024, 2027, 2030)) {
            assertTrue(EventRepository.forDate(LocalDate.of(year, 1, 1)).any { it.titleEn == "New Year's Day" })
        }
    }

    @Test fun holyDaysAndCalculatedEventsMatchEngineAcrossYears() {
        for (year in listOf(1800, 1980, 2024, 2026, 2031, 2200)) {
            val events = EventRepository.forYear(year)
            val expectedHolyDays = generateSequence(LocalDate.of(year, 1, 1)) { it.plusDays(1) }
                .takeWhile { it.year == year }.filter { KhmerCalendar.fromGregorian(it).isHolyDay }.toList()
            assertEquals("Holy days in $year", expectedHolyDays, events.filter { it.kind == EventKind.HOLY_DAY }.map { it.date })
            assertEquals("Unique keys in $year", events.size, events.map { it.key }.distinct().size)
        }
    }

    @Test fun staticEventsIncludeChineseFestivalsAndUnescoMilestones() {
        val events2027 = EventRepository.forYear(2027)
        assertEquals(listOf(6, 7, 8), events2027.filter { it.titleEn == "Chinese New Year" }.map { it.date.dayOfMonth })
        assertEquals(LocalDate.of(2027, 2, 5), events2027.single { it.titleEn == "Chinese New Year's Eve" }.date)
        assertTrue(events2027.filter { it.id.startsWith("chinese_") }.all { it.basis == DateBasis.CALCULATED })

        // UNESCO static milestone in 2023: Koh Ker inscribed on World Heritage List
        val events2023 = EventRepository.forYear(2023)
        val kohKer = events2023.single { it.id == "static_koh_ker_inscribed_on_the_unesco_world_heritage_lis" }
        assertEquals(LocalDate.of(2023, 9, 17), kohKer.date)
        assertEquals(DateBasis.RECORDED, kohKer.basis)
    }

    @Test fun officialHolidayCalendarsStandardizeOffDaysWithSubDecreeCitationsAcrossTwelveYears() {
        assertEquals("0.3.3", RecurringEvents.catalog.dataVersion)
        // 12 official holiday calendars (2016–2027) totaling 283 off-days
        var totalOfficialDays = 0
        for (year in 2016..2027) {
            val holidays = EventRepository.forYear(year).filter { it.kind == EventKind.HOLIDAY }
            assertTrue(holidays.isNotEmpty())
            assertTrue(holidays.all { it.basis == DateBasis.OFFICIAL })
            assertTrue(holidays.all { it.citation?.startsWith("📜 Anukret No. ") == true })
            if (year <= 2024) {
                assertTrue(holidays.all { it.citation?.contains("signed by Prime Minister Hun Sen") == true })
            } else {
                assertTrue(holidays.all { it.citation?.contains("signed by Prime Minister Hun Manet") == true })
            }
            totalOfficialDays += holidays.size
        }
        assertEquals(283, totalOfficialDays)

        // 2016 Sub-decree No. 137
        val official2016 = EventRepository.forYear(2016).filter { it.kind == EventKind.HOLIDAY }
        assertEquals(28, official2016.size)
        assertTrue(official2016.all { it.citation?.contains("Anukret No. 137") == true })

        // 2017 Sub-decree No. 223
        val official2017 = EventRepository.forYear(2017).filter { it.kind == EventKind.HOLIDAY }
        assertEquals(27, official2017.size)
        assertTrue(official2017.all { it.citation?.contains("Anukret No. 223") == true })

        // 2018 Sub-decree No. 202
        val official2018 = EventRepository.forYear(2018).filter { it.kind == EventKind.HOLIDAY }
        assertEquals(27, official2018.size)
        assertTrue(official2018.all { it.citation?.contains("Anukret No. 202") == true })

        // 2019 Sub-decree No. 126
        val official2019 = EventRepository.forYear(2019).filter { it.kind == EventKind.HOLIDAY }
        assertEquals(28, official2019.size)
        assertTrue(official2019.all { it.citation?.contains("Anukret No. 126") == true })

        // 2025 MEF official calendar
        val official2025 = EventRepository.forYear(2025).filter { it.kind == EventKind.HOLIDAY }
        assertEquals(22, official2025.size)
        assertEquals(22, official2025.map { it.date }.distinct().size)
        assertTrue(official2025.all { it.basis == DateBasis.OFFICIAL && it.officialSourceUrl == "https://mef.gov.kh/calendar-holiday-2025/" })
        assertTrue(official2025.all { it.citation?.contains("Anukret No. 204") == true })

        // 2026 Legal Reform Committee calendar
        val official2026 = EventRepository.forYear(2026).filter { it.kind == EventKind.HOLIDAY }
        assertEquals(22, official2026.size)
        assertTrue(official2026.all { it.basis == DateBasis.OFFICIAL && it.officialSourceUrl == "https://lrc.gov.kh/en/annual-holiday-calendar-2026/" })
        assertTrue(official2026.all { it.citation?.contains("Anukret No. 167") == true })

        // Royal Ploughing Ceremony dates
        fun ploughing(year: Int) = EventRepository.forYear(year).single { it.titleEn == "Royal Ploughing Ceremony" }.date
        assertEquals(LocalDate.of(2025, 5, 15), ploughing(2025))
        assertEquals(LocalDate.of(2026, 5, 5), ploughing(2026))
        assertEquals(LocalDate.of(2027, 5, 24), ploughing(2027))
    }

    @Test fun historicalKingSihamoniBirthdayOverridesApplyFor2005To2019() {
        // 2005–2015: Historical 3-day observances from website capture
        for (year in 2005..2015) {
            val bday = EventRepository.forYear(year).filter { it.id == "king_sihamoni_birthday" }
            assertEquals(3, bday.size)
            assertEquals(listOf(LocalDate.of(year, 5, 13), LocalDate.of(year, 5, 14), LocalDate.of(year, 5, 15)), bday.map { it.date })
            assertTrue(bday.all { it.basis == DateBasis.CORRECTED && it.kind == EventKind.OBSERVANCE })
        }
        // 2016–2019: 3-day official public holidays confirmed by Royal Government Sub-Decrees
        for (year in 2016..2019) {
            val bday = EventRepository.forYear(year).filter { it.id == "king_sihamoni_birthday" }
            assertEquals(3, bday.size)
            assertEquals(listOf(LocalDate.of(year, 5, 13), LocalDate.of(year, 5, 14), LocalDate.of(year, 5, 15)), bday.map { it.date })
            assertTrue(bday.all { it.basis == DateBasis.OFFICIAL && it.kind == EventKind.HOLIDAY })
        }
        // From 2020 onward: 1-day official holiday (2020–2027) or recurrence (2028+)
        val bday2020 = EventRepository.forYear(2020).filter { it.id == "king_sihamoni_birthday" }
        assertEquals(listOf(LocalDate.of(2020, 5, 14)), bday2020.map { it.date })
        assertEquals(EventKind.HOLIDAY, bday2020.single().kind)

        val bday2031 = EventRepository.forYear(2031).filter { it.id == "king_sihamoni_birthday" }
        assertEquals(listOf(LocalDate.of(2031, 5, 14)), bday2031.map { it.date })
        assertEquals(EventKind.OBSERVANCE, bday2031.single().kind)
        assertEquals(DateBasis.CALCULATED, bday2031.single().basis)
    }
}
