// Copyright (c) 2026 RSG-KH | Apache-2.0 License
package com.rsgkh.calendar

import com.rsgkh.calendar.domain.*
import com.rsgkh.calendar.engine.western.WesternZodiacSign
import com.rsgkh.calendar.ui.*
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.*
import org.junit.Test

class AstrologyDetailsTest {
    @Test fun allCalculatedSignsUseTheExistingSignDetails() {
        WesternZodiacSign.entries.forEach { sign ->
            assertEquals(sign.englishName, sign.displaySign().signName)
            assertTrue(sign.displaySign().element.isNotBlank())
            assertTrue(sign.displaySign().planet.isNotBlank())
        }
    }

    @Test fun watermarkUsesTheTableYearAndCalculatedSunInsteadOfTheKhmerYearAndDateLabel() {
        val info = KhmerDateDetails.fromGregorian(LocalDate.of(2026, 3, 1))
        val pillars = ganzhiColumns(info, LocalTime.NOON, false)
        assertNotEquals(info.animalYear, pillars.first().pillar!!.branch.index)
        assertEquals(R.drawable.zodiac_horse_400, astrologyBackground(AstrologyDetail.GANZHI, null, pillars))
        val signs = WesternBig3Signs(WesternZodiacSign.SCORPIO, WesternZodiacSign.ARIES, null)
        assertEquals(R.drawable.western_zodiac_scorpio, astrologyBackground(AstrologyDetail.BIG_3, signs, emptyList()))
    }

    @Test fun unavailableSignsAreNotInventedInWatermarksOrAiQueries() {
        val pillars = ganzhiColumns(KhmerDateDetails.fromGregorian(LocalDate.of(1850, 1, 1)), null, false)
        assertNull(astrologyBackground(AstrologyDetail.GANZHI, null, pillars))
        val signs = WesternBig3Signs(WesternZodiacSign.SCORPIO, WesternZodiacSign.ARIES, null)
        val query = astrologySearchQuery(AstrologyDetail.BIG_3, signs, pillars, false)
        assertTrue(query.contains("Sun: Scorpio"))
        assertTrue(query.contains("Moon: Aries"))
        assertFalse(query.contains("Rising:"))
        assertFalse(query.contains("1850"))
        val ganzhi = astrologySearchQuery(AstrologyDetail.GANZHI, null, pillars, false)
        assertTrue(ganzhi.contains("Day:"))
        assertFalse(ganzhi.contains("Year:"))
        assertFalse(ganzhi.contains("Hour:"))
    }

    @Test fun chineseGanzhiAiQueryFormatsWithDateAndTimeInEnglishAndKhmer() {
        val date = LocalDate.of(2026, 9, 29)
        val time = LocalTime.of(20, 58)
        val info = KhmerDateDetails.fromGregorian(date)
        val pillarsEn = ganzhiColumns(info, time, false)
        val queryEn = astrologySearchQuery(AstrologyDetail.GANZHI, null, pillarsEn, false, date, time)
        val expectedEn = """
Please explain the traditional astrological meanings of the Chinese Ganzhi (干支) for September 29, 2026, at 20:58:
- Year: 丙午 Horse (Clash: Rat);
- Month: 丁酉 Rooster (Clash: Rabbit);
- Day: 丙午 Horse (Clash: Rat);
- Hour: 戊戌 Dog (Clash: Dragon).
        """.trimIndent()
        assertEquals(expectedEn, queryEn)

        val pillarsKm = ganzhiColumns(info, time, true)
        val queryKm = astrologySearchQuery(AstrologyDetail.GANZHI, null, pillarsKm, true, date, time)
        val expectedKm = """
ចូរពន្យល់អត្ថន័យតាមហោរាសាស្ត្រចិន(干支) ដែលត្រូវនឹងថ្ងៃទី ២៩ ខែកញ្ញា ឆ្នាំ ២០២៦ ម៉ោង ២០ និង ៥៨ នាទី៖
- ឆ្នាំ៖ 丙午 មមី (ឆុង៖ ជូត)
- ខែ៖ 丁酉 រកា (ឆុង៖ ថោះ)
- ថ្ងៃ៖ 丙午 មមី (ឆុង៖ ជូត)
- ម៉ោង៖ 戊戌 ច (ឆុង៖ រោង)។
        """.trimIndent()
        assertEquals(expectedKm, queryKm)
    }

    @Test fun westernBig3AiQueryFormatsWithDateAndTimeInEnglishAndKhmer() {
        val date = LocalDate.of(2026, 9, 29)
        val time = LocalTime.of(20, 58)
        val signs = WesternBig3Signs(
            WesternZodiacSign.LIBRA,
            WesternZodiacSign.TAURUS,
            WesternZodiacSign.GEMINI,
        )
        val queryEn = astrologySearchQuery(AstrologyDetail.BIG_3, signs, emptyList(), false, date, time)
        val expectedEn = """
Please explain the traditional astrological meanings of the Big 3 (Sun, Moon, and Rising) for September 29, 2026, at 20:58 (unspecified location):
- Sun: Libra;
- Moon: Taurus;
- Rising: Gemini.
        """.trimIndent()
        assertEquals(expectedEn, queryEn)

        val queryKm = astrologySearchQuery(AstrologyDetail.BIG_3, signs, emptyList(), true, date, time)
        val expectedKm = """
ចូរពន្យល់ពីអត្ថន័យតាមក្បួនហោរាសាស្ត្រលោកខាងលិច នៃធាតុសំខាន់ទាំង ៣ (Big 3) សម្រាប់ថ្ងៃទី ២៩ ខែកញ្ញា ឆ្នាំ ២០២៦ ម៉ោង ២០:៥៨ (ទីតាំងមិនបានបញ្ជាក់)៖
- ព្រះអាទិត្យ (Sun)៖ Libra
- ព្រះចន្ទ (Moon)៖ Taurus
- រះ (Rising)៖ Gemini ។
        """.trimIndent()
        assertEquals(expectedKm, queryKm)

        val signsTaurusRising = WesternBig3Signs(
            WesternZodiacSign.LIBRA,
            WesternZodiacSign.TAURUS,
            WesternZodiacSign.TAURUS,
        )
        val queryKmTaurus = astrologySearchQuery(AstrologyDetail.BIG_3, signsTaurusRising, emptyList(), true, date, time)
        val expectedKmTaurus = """
ចូរពន្យល់ពីអត្ថន័យតាមក្បួនហោរាសាស្ត្រលោកខាងលិច នៃធាតុសំខាន់ទាំង ៣ (Big 3) សម្រាប់ថ្ងៃទី ២៩ ខែកញ្ញា ឆ្នាំ ២០២៦ ម៉ោង ២០:៥៨ (ទីតាំងមិនបានបញ្ជាក់)៖
- ព្រះអាទិត្យ (Sun)៖ Libra
- ព្រះចន្ទ (Moon)៖ Taurus
- រះ (Rising)៖ Taurus ។
        """.trimIndent()
        assertEquals(expectedKmTaurus, queryKmTaurus)
    }

    @Test fun westernBig3AiQueryAlwaysKeepsLocationUnspecifiedForPrivacy() {
        val date = LocalDate.of(2026, 9, 29)
        val time = LocalTime.of(20, 58)
        val signs = WesternBig3Signs(
            WesternZodiacSign.LIBRA,
            WesternZodiacSign.TAURUS,
            WesternZodiacSign.GEMINI,
        )
        val queryEn = astrologySearchQuery(AstrologyDetail.BIG_3, signs, emptyList(), false, date, time)
        assertTrue(queryEn.contains("(unspecified location):"))

        val queryKm = astrologySearchQuery(AstrologyDetail.BIG_3, signs, emptyList(), true, date, time)
        assertTrue(queryKm.contains("(ទីតាំងមិនបានបញ្ជាក់)៖"))
    }
}
