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

    @Test fun anniversaryCountsRenderWithEnglishOrdinalInEventAndHolidayNames() {
        // Holiday layer (2026 sub-decree): English placeholder resolves to an ordinal, Khmer keeps the printed count.
        val jan7 = EventRepository.forDate(LocalDate.of(2026, 1, 7)).single { it.id == "victory_over_genocide" }
        assertEquals("Victory Over Genocide Day · 47th", jan7.titleEn)
        assertTrue(jan7.titleKm.contains("ខួបលើកទី៤៧"))
        // Holiday layer with placeholder on both sides (2016): 2016 − 1948 = 68th.
        val rights2016 = EventRepository.forDate(LocalDate.of(2016, 12, 10)).single { it.id == "international_human_rights_day" }
        assertEquals("International Human Rights Day · 68th", rights2016.titleEn)
        assertTrue(rights2016.titleKm.contains("ខួបលើកទី៦៨"))
        // Rule layer outside official calendars (2028): 2028 − 1953 = 75th.
        val nov9 = EventRepository.forDate(LocalDate.of(2028, 11, 9)).single { it.id == "independence_day" }
        assertEquals("Independence Day · 75th", nov9.titleEn)
        assertTrue(nov9.titleKm.contains("ខួបលើកទី៧៥"))
        assertEquals(1979, jan7.anniversaryBase)
        assertEquals(1948, rights2016.anniversaryBase)
        assertEquals(1953, nov9.anniversaryBase)
    }

    @Test fun ordinalSuffixFollowsEnglishConvention() {
        val expected = listOf(1 to "1st", 2 to "2nd", 3 to "3rd", 4 to "4th", 11 to "11th", 12 to "12th",
            13 to "13th", 21 to "21st", 22 to "22nd", 23 to "23rd", 47 to "47th", 101 to "101st", 111 to "111th")
        for ((n, label) in expected) assertEquals(label, "$n${ordinalSuffix(n)}")
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
        assertEquals("0.5.0", RecurringEvents.catalog.dataVersion)
        assertEquals(3, RecurringEvents.catalog.schemaVersion)
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

    @Test fun newYearArrivalCatalogAndUnifiedTitleDisplay() {
        assertEquals(3, RecurringEvents.catalog.schemaVersion)
        assertEquals("0.5.0", RecurringEvents.catalog.dataVersion)
        val arrivals = RecurringEvents.catalog.newYearArrivals
        assertEquals(19, arrivals.size)
        val expectedYears = listOf(1997, 2009) + (2010..2026).toList()
        assertEquals(expectedYears, arrivals.map { it.year }.sorted())
        assertTrue(arrivals.all { it.status == "evidenced" })

        // 2024 with seconds precision from official TVK broadcast
        val arrival2024 = arrivals.single { it.year == 2024 }
        assertEquals("22:17:24", arrival2024.localTime)
        assertEquals(24, arrival2024.second)
        val ny2024 = EventRepository.forYear(2024).single { it.id == "khmer_new_year_1" || it.id == "2024_khmer_new_year_1" }
        assertEquals("Khmer New Year – Moha Sankranta 10:17:24 PM (Official time)", ny2024.titleEn)
        assertEquals("ពិធី​បុណ្យ​ចូល​ឆ្នាំ​ថ្មី ប្រពៃណី​ជាតិ – មហា​សង្ក្រាន្ត ម៉ោង ១០:១៧:២៤ យប់ (ម៉ោងផ្លូវការ)", ny2024.titleKm)
        assertNull(ny2024.time)

        // 2026 official arrival from AKP / TVK
        val arrival2026 = arrivals.single { it.year == 2026 }
        assertEquals("10:48", arrival2026.localTime)
        assertNull(arrival2026.second)
        val ny2026 = EventRepository.forYear(2026).single { it.id == "khmer_new_year_1" || it.id == "2026_khmer_new_year_1" }
        assertEquals("Khmer New Year – Moha Sankranta 10:48 AM (Official time)", ny2026.titleEn)
        assertEquals("ពិធី​បុណ្យ​ចូល​ឆ្នាំ​ថ្មី ប្រពៃណី​ជាតិ – មហា​សង្ក្រាន្ត ម៉ោង ១០:៤៨ ព្រឹក (ម៉ោងផ្លូវការ)", ny2026.titleKm)
        assertNull(ny2026.time)

        // 2027 calculated estimate fallback
        val ny2027 = EventRepository.forYear(2027).single { it.id == "khmer_new_year_1" || it.id == "2027_khmer_new_year_1" }
        assertEquals("Khmer New Year – Moha Sankranta 4:48 PM (Estimated time)", ny2027.titleEn)
        assertEquals("ពិធី​បុណ្យ​ចូល​ឆ្នាំ​ថ្មី ប្រពៃណី​ជាតិ – មហា​សង្ក្រាន្ត ម៉ោង ០៤:៤៨ ល្ងាច (ម៉ោងប៉ាន់ស្មាន)", ny2027.titleKm)
        assertNull(ny2027.time)

        // Helper functions for Khmer period descriptors
        assertEquals("រំលងអធ្រាត្រ", getKhmerPeriod(0, 0))
        assertEquals("រំលងអធ្រាត្រ", getKhmerPeriod(2, 59))
        assertEquals("ព្រឹក", getKhmerPeriod(3, 0))
        assertEquals("ព្រឹក", getKhmerPeriod(11, 59))
        assertEquals("ថ្ងៃត្រង់", getKhmerPeriod(12, 0))
        assertEquals("រសៀល", getKhmerPeriod(12, 1))
        assertEquals("រសៀល", getKhmerPeriod(14, 59))
        assertEquals("ល្ងាច", getKhmerPeriod(15, 0))
        assertEquals("ល្ងាច", getKhmerPeriod(19, 59))
        assertEquals("យប់", getKhmerPeriod(20, 0))
        assertEquals("យប់", getKhmerPeriod(23, 59))
    }
}
