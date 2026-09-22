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
class RecurringEventsTest {
    @Test fun reviewedRulesMatchCapturedDatesExceptHistoricalOfficialBirthdayLeave() {
        val reference = javaClass.getResourceAsStream("/recurrence-reference.tsv")!!.bufferedReader().useLines { lines ->
            lines.filter { it.isNotBlank() && !it.startsWith('#') }.map {
                val fields = it.split('\t')
                fields[0] to LocalDate.parse(fields[1])
            }.toList().groupBy({ it.first }, { it.second })
        }
        val baseRules = RecurringEvents.rules.filter { !it.id.startsWith("chinese_") && it.id != "international_peace_day_ga_opening" && it.id != "ben_14" && it.id != "post_pchum_ben_festival" }
        assertEquals(reference.keys, baseRules.map { it.id }.toSet())
        assertEquals(9, RecurringEvents.rules.count { it.id.startsWith("chinese_") })
        val differences = mutableListOf<String>()
        for (year in 2000..2030) {
            val calculated = RecurringEvents.dates(year).mapKeys { it.key.id }
            for ((id, dates) in reference) {
                val expected = dates.filter { it.year == year }.toSet()
                val actual = calculated[id].orEmpty().toSet()
                if (expected != actual) differences.add("$year $id: source=$expected, calculated=$actual")
            }
        }
        val expectedDifferences = mutableListOf<String>()
        val capturedPchum = javaClass.getResourceAsStream("/recurrence-reference.tsv")!!.bufferedReader().useLines { lines ->
            lines.filter { it.startsWith("pchum_ben_festival\t") }.map { LocalDate.parse(it.substringAfter('\t')) }.toList()
        }
        for (year in 2000..2030) {
            if (year in 2005..2019) {
                // Document pre-2020 3-day official holiday block for King Sihamoni's Birthday (reduced to 1 day on May 14 from 2020 onward)
                expectedDifferences.add("$year king_sihamoni_birthday: source=[$year-05-13, $year-05-14, $year-05-15], calculated=[$year-05-14]")
            }
            // The captured archive printed one 3-day Pchum Ben block from 14 រោច; since v0.4.3 the catalog models
            // the traditional series — Ben 14 as its own day and the festival as the single 15 រោច climax —
            // which is the middle day of the captured block.
            val block = capturedPchum.filter { it.year == year }
            expectedDifferences.add("$year pchum_ben_festival: source=${block.toSet()}, calculated=[${block.sorted()[1]}]")
        }
        assertEquals(expectedDifferences, differences)

        val capturedNewYear = EventRepository.forYear(2012).filter { it.id.startsWith("khmer_new_year_") }
        assertEquals((13..15).map { LocalDate.of(2012, 4, it) }, capturedNewYear.map { it.date })
        assertTrue(capturedNewYear.all { it.basis == DateBasis.CALCULATED })
    }

    @Test fun rulesHaveOneAnnualAnchorAndUniqueOccurrencesAcrossTheSupportedRange() {
        for (year in 1800..2200) {
            val events = RecurringEvents.forYear(year)
            assertTrue(events.isNotEmpty())
            assertEquals("$year", events.size, events.map { it.key }.distinct().size)
            assertTrue(events.all { it.date.year == year && it.basis == DateBasis.CALCULATED &&
                it.kind == EventKind.OBSERVANCE && it.officialSourceUrl == null && it.time == null })
            assertTrue(events.all { it.titleKm.isNotBlank() && it.titleEn.isNotBlank() && !it.titleKm.contains('{') && !it.titleEn.contains('{') })
        }
    }

    @Test fun bundledKnowledgeCoversEveryCatalogEventWithCompleteEntries() {
        assertEquals(RecurringEvents.catalog.events.map { it.id }.toSet(), RecurringEvents.knowledgeById.keys)
        assertTrue(RecurringEvents.knowledgeById.values.all { it.category.isNotBlank() && it.nameKm.isNotBlank() && it.nameEn.isNotBlank() && it.summaryKm.isNotBlank() && it.summaryEn.isNotBlank() })
    }

    @Test fun secondAsadhAndPchumBenDurationAreHandledExplicitly() {
        val dates = RecurringEvents.dates(2026).mapKeys { it.key.id }
        assertEquals(listOf(LocalDate.of(2026, 7, 30)), dates["beginning_buddhist_lent"])
        assertEquals(13, KhmerCalendar.fromGregorian(dates.getValue("beginning_buddhist_lent").single()).month)
        assertEquals(listOf(LocalDate.of(2026, 10, 10)), dates["ben_14"])
        assertEquals(listOf(LocalDate.of(2026, 10, 11)), dates["pchum_ben_festival"])
        assertEquals(listOf(LocalDate.of(2026, 10, 12)), dates["post_pchum_ben_festival"])
        assertEquals((23..25).map { LocalDate.of(2026, 11, it) }, dates["water_festival"])
        val ordinary = RecurringEvents.dates(2025).entries.single { it.key.id == "beginning_buddhist_lent" }.value.single()
        assertEquals(LocalDate.of(2025, 7, 11), ordinary)
        assertEquals(7, KhmerCalendar.fromGregorian(ordinary).month)
    }

    @Test fun historicalYearsDoNotAcquireModernNationalEventsOrUnsupportedRules() {
        val old = RecurringEvents.forYear(1999)
        assertTrue(old.any { it.id == "new_year_day" })
        assertTrue(old.any { it.id == "meak_bochea" })
        assertTrue(old.any { it.id == "constitution_day" })
        assertTrue(old.any { it.id == "victory_over_genocide" })
        assertFalse(RecurringEvents.forYear(1978).any { it.id == "victory_over_genocide" })
        assertFalse(RecurringEvents.forYear(1992).any { it.id == "constitution_day" })
        assertFalse(RecurringEvents.forYear(2000).any { it.id == "national_fish_day" })
        assertTrue(RecurringEvents.forYear(2003).any { it.id == "national_fish_day" })
        assertFalse(RecurringEvents.rules.any { it.id in setOf("buddhist_lent_candles", "chinese_new_year_day1", "qingming_festival") })

        val chineseRuleIds = setOf(
            "chinese_new_year_days", "chinese_new_year_eve", "chinese_kitchen_god_festival",
            "chinese_spirit_parade", "chinese_zongzi_festival", "chinese_ghost_festival",
            "chinese_mid_autumn_festival", "chinese_qingming_festival", "chinese_winter_solstice",
        )
        val activeChineseRules = RecurringEvents.rules.filter { it.type == "chinese_festival" }
        assertEquals(chineseRuleIds, activeChineseRules.map { it.id }.toSet())
        assertTrue(activeChineseRules.all { it.fromYear == 1900 && it.throughYear == 2100 && it.monthPolicy == "cn-reference-utc8" })
        assertTrue(old.any { it.id == "chinese_new_year_days" })
        assertFalse(RecurringEvents.forYear(1899).any { it.id.startsWith("chinese_") })
    }

    @Test fun dynamicCalculationProducesCorrectBasesAcrossCoveredYears() {
        for (year in 2005..2015) {
            val birthday = EventRepository.forYear(year).filter { it.id == "king_sihamoni_birthday" }
            assertEquals(listOf(LocalDate.of(year, 5, 13), LocalDate.of(year, 5, 14), LocalDate.of(year, 5, 15)), birthday.map { it.date })
            assertTrue(birthday.all { it.basis == DateBasis.CORRECTED })
        }
        for (year in 2016..2019) {
            val birthday = EventRepository.forYear(year).filter { it.id == "king_sihamoni_birthday" }
            assertEquals(listOf(LocalDate.of(year, 5, 13), LocalDate.of(year, 5, 14), LocalDate.of(year, 5, 15)), birthday.map { it.date })
            assertTrue(birthday.all { it.basis == DateBasis.OFFICIAL })
        }
        for (year in listOf(1800, 1900, 1993, 2031, 2200)) {
            val actual = EventRepository.forYear(year)
            assertEquals(RecurringEvents.forYear(year).toSet(), actual.filter { it.basis == DateBasis.CALCULATED }.toSet())
            assertTrue(actual.any { it.kind == EventKind.HOLY_DAY })
            assertTrue(actual.none { it.kind == EventKind.HOLIDAY })
        }
        val actual1999 = EventRepository.forYear(1999)
        val calculated1999 = RecurringEvents.forYear(1999).filter { it.id != "international_peace_day_ga_opening" }.toSet()
        assertEquals(calculated1999, actual1999.filter { it.basis == DateBasis.CALCULATED }.toSet())
        assertEquals(listOf("international_peace_day_ga_opening"), actual1999.filter { it.basis == DateBasis.CORRECTED }.map { it.id })
    }

    @Test fun ordainedDragonMonkAndPreLentTraditionsCalculateFor1993AndAllYears() {
        val events1993 = EventRepository.forYear(1993)
        val dragon1993 = events1993.firstOrNull { it.id == "the_ordained_dragon_monk" }
        assertNotNull(dragon1993)
        assertEquals(LocalDate.of(1993, 8, 1), dragon1993!!.date)
        assertEquals("The Ordained Dragon Monk", dragon1993.titleEn)
        assertEquals("ពិធី​បំបួស​នាគ​ខ្នាន", dragon1993.titleKm)

        val candles1993 = events1993.firstOrNull { it.id == "buddhist_lent_candles_making_day" }
        assertNotNull(candles1993)
        assertEquals(LocalDate.of(1993, 7, 26), candles1993!!.date)

        val lent1993 = events1993.firstOrNull { it.id == "beginning_buddhist_lent" }
        assertNotNull(lent1993)
        assertEquals(LocalDate.of(1993, 8, 3), lent1993!!.date)
    }

    @Test fun floatingWeekdaysCalculateSecondSundayMayAndThirdSundayJune() {
        val dates2024 = RecurringEvents.dates(2024).mapKeys { it.key.id }
        assertEquals(listOf(LocalDate.of(2024, 5, 12)), dates2024["mothers_day"])
        assertEquals(listOf(LocalDate.of(2024, 6, 16)), dates2024["fathers_day"])

        val dates2031 = RecurringEvents.dates(2031).mapKeys { it.key.id }
        assertEquals(listOf(LocalDate.of(2031, 5, 11)), dates2031["mothers_day"])
        assertEquals(listOf(LocalDate.of(2031, 6, 15)), dates2031["fathers_day"])
    }

    @Test fun royalHolidaysAndNationalHeritageCalculateWithAnniversariesForFutureYears() {
        val events2031 = EventRepository.forYear(2031)

        val sihamoni = events2031.single { it.id == "king_sihamoni_birthday" }
        assertEquals(LocalDate.of(2031, 5, 14), sihamoni.date)

        val queenMother = events2031.single { it.id == "queen_mother_birthday" }
        assertEquals(LocalDate.of(2031, 6, 18), queenMother.date)

        val kingFather = events2031.single { it.id == "king_father_commemoration" }
        assertEquals(LocalDate.of(2031, 10, 15), kingFather.date)

        val coronation = events2031.single { it.id == "king_coronation_day" }
        assertEquals(LocalDate.of(2031, 10, 29), coronation.date)

        val peaceDay = events2031.single { it.id == "peace_day_cambodia" }
        assertEquals(LocalDate.of(2031, 12, 29), peaceDay.date)

        val angkor = events2031.single { it.id == "angkor_wat_unesco" }
        assertEquals(LocalDate.of(2031, 12, 14), angkor.date)
        assertTrue(angkor.titleKm.contains("៣៩")) // 2031 - 1992 = 39

        val preahVihear = events2031.single { it.id == "preah_vihear_unesco" }
        assertEquals(LocalDate.of(2031, 7, 7), preahVihear.date)
        assertTrue(preahVihear.titleKm.contains("២៣")) // 2031 - 2008 = 23
    }

    @Test fun mohaSangkranUnifiedTitleMatchesAllNineteenVerifiedYears() {
        val expected = mapOf(
            1997 to ("Khmer New Year – Moha Sankranta 10:48 PM (Official time)" to "ពិធី​បុណ្យ​ចូល​ឆ្នាំ​ថ្មី ប្រពៃណី​ជាតិ – មហា​សង្ក្រាន្ត ម៉ោង ១០:៤៨ យប់ (ម៉ោងផ្លូវការ)"),
            2009 to ("Khmer New Year – Moha Sankranta 1:30 AM (Official time)" to "ពិធី​បុណ្យ​ចូល​ឆ្នាំ​ថ្មី ប្រពៃណី​ជាតិ – មហា​សង្ក្រាន្ត ម៉ោង ០១:៣០ រំលងអធ្រាត្រ (ម៉ោងផ្លូវការ)"),
            2010 to ("Khmer New Year – Moha Sankranta 7:36 AM (Official time)" to "ពិធី​បុណ្យ​ចូល​ឆ្នាំ​ថ្មី ប្រពៃណី​ជាតិ – មហា​សង្ក្រាន្ត ម៉ោង ០៧:៣៦ ព្រឹក (ម៉ោងផ្លូវការ)"),
            2011 to ("Khmer New Year – Moha Sankranta 1:12 PM (Official time)" to "ពិធី​បុណ្យ​ចូល​ឆ្នាំ​ថ្មី ប្រពៃណី​ជាតិ – មហា​សង្ក្រាន្ត ម៉ោង ០១:១២ រសៀល (ម៉ោងផ្លូវការ)"),
            2012 to ("Khmer New Year – Moha Sankranta 7:11 PM (Official time)" to "ពិធី​បុណ្យ​ចូល​ឆ្នាំ​ថ្មី ប្រពៃណី​ជាតិ – មហា​សង្ក្រាន្ត ម៉ោង ០៧:១១ ល្ងាច (ម៉ោងផ្លូវការ)"),
            2013 to ("Khmer New Year – Moha Sankranta 2:12 AM (Official time)" to "ពិធី​បុណ្យ​ចូល​ឆ្នាំ​ថ្មី ប្រពៃណី​ជាតិ – មហា​សង្ក្រាន្ត ម៉ោង ០២:១២ រំលងអធ្រាត្រ (ម៉ោងផ្លូវការ)"),
            2014 to ("Khmer New Year – Moha Sankranta 8:07 AM (Official time)" to "ពិធី​បុណ្យ​ចូល​ឆ្នាំ​ថ្មី ប្រពៃណី​ជាតិ – មហា​សង្ក្រាន្ត ម៉ោង ០៨:០៧ ព្រឹក (ម៉ោងផ្លូវការ)"),
            2015 to ("Khmer New Year – Moha Sankranta 2:01 PM (Official time)" to "ពិធី​បុណ្យ​ចូល​ឆ្នាំ​ថ្មី ប្រពៃណី​ជាតិ – មហា​សង្ក្រាន្ត ម៉ោង ០២:០១ រសៀល (ម៉ោងផ្លូវការ)"),
            2016 to ("Khmer New Year – Moha Sankranta 8:00 PM (Official time)" to "ពិធី​បុណ្យ​ចូល​ឆ្នាំ​ថ្មី ប្រពៃណី​ជាតិ – មហា​សង្ក្រាន្ត ម៉ោង ០៨:០០ យប់ (ម៉ោងផ្លូវការ)"),
            2017 to ("Khmer New Year – Moha Sankranta 3:12 AM (Official time)" to "ពិធី​បុណ្យ​ចូល​ឆ្នាំ​ថ្មី ប្រពៃណី​ជាតិ – មហា​សង្ក្រាន្ត ម៉ោង ០៣:១២ ព្រឹក (ម៉ោងផ្លូវការ)"),
            2018 to ("Khmer New Year – Moha Sankranta 9:12 AM (Official time)" to "ពិធី​បុណ្យ​ចូល​ឆ្នាំ​ថ្មី ប្រពៃណី​ជាតិ – មហា​សង្ក្រាន្ត ម៉ោង ០៩:១២ ព្រឹក (ម៉ោងផ្លូវការ)"),
            2019 to ("Khmer New Year – Moha Sankranta 3:12 PM (Official time)" to "ពិធី​បុណ្យ​ចូល​ឆ្នាំ​ថ្មី ប្រពៃណី​ជាតិ – មហា​សង្ក្រាន្ត ម៉ោង ០៣:១២ ល្ងាច (ម៉ោងផ្លូវការ)"),
            2020 to ("Khmer New Year – Moha Sankranta 8:48 PM (Official time)" to "ពិធី​បុណ្យ​ចូល​ឆ្នាំ​ថ្មី ប្រពៃណី​ជាតិ – មហា​សង្ក្រាន្ត ម៉ោង ០៨:៤៨ យប់ (ម៉ោងផ្លូវការ)"),
            2021 to ("Khmer New Year – Moha Sankranta 4:00 AM (Official time)" to "ពិធី​បុណ្យ​ចូល​ឆ្នាំ​ថ្មី ប្រពៃណី​ជាតិ – មហា​សង្ក្រាន្ត ម៉ោង ០៤:០០ ព្រឹក (ម៉ោងផ្លូវការ)"),
            2022 to ("Khmer New Year – Moha Sankranta 10:00 AM (Official time)" to "ពិធី​បុណ្យ​ចូល​ឆ្នាំ​ថ្មី ប្រពៃណី​ជាតិ – មហា​សង្ក្រាន្ត ម៉ោង ១០:០០ ព្រឹក (ម៉ោងផ្លូវការ)"),
            2023 to ("Khmer New Year – Moha Sankranta 4:00 PM (Official time)" to "ពិធី​បុណ្យ​ចូល​ឆ្នាំ​ថ្មី ប្រពៃណី​ជាតិ – មហា​សង្ក្រាន្ត ម៉ោង ០៤:០០ ល្ងាច (ម៉ោងផ្លូវការ)"),
            2024 to ("Khmer New Year – Moha Sankranta 10:17:24 PM (Official time)" to "ពិធី​បុណ្យ​ចូល​ឆ្នាំ​ថ្មី ប្រពៃណី​ជាតិ – មហា​សង្ក្រាន្ត ម៉ោង ១០:១៧:២៤ យប់ (ម៉ោងផ្លូវការ)"),
            2025 to ("Khmer New Year – Moha Sankranta 4:48 AM (Official time)" to "ពិធី​បុណ្យ​ចូល​ឆ្នាំ​ថ្មី ប្រពៃណី​ជាតិ – មហា​សង្ក្រាន្ត ម៉ោង ០៤:៤៨ ព្រឹក (ម៉ោងផ្លូវការ)"),
            2026 to ("Khmer New Year – Moha Sankranta 10:48 AM (Official time)" to "ពិធី​បុណ្យ​ចូល​ឆ្នាំ​ថ្មី ប្រពៃណី​ជាតិ – មហា​សង្ក្រាន្ត ម៉ោង ១០:៤៨ ព្រឹក (ម៉ោងផ្លូវការ)"),
        )
        for ((year, expectedTitles) in expected) {
            val event = RecurringEvents.forYear(year).single { it.id == "khmer_new_year_1" }
            assertEquals("Year $year English title", expectedTitles.first, event.titleEn)
            assertEquals("Year $year Khmer title", expectedTitles.second, event.titleKm)
            assertTrue("Year $year should have arrival sources", event.sourceIds.isNotEmpty())
        }
    }
}
