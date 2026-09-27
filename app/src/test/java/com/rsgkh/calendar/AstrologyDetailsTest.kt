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
}
