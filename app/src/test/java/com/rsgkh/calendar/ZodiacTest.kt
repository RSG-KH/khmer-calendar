// Copyright (c) 2026 RSG-KH | Apache-2.0 License
package com.rsgkh.calendar

import com.rsgkh.calendar.domain.KhmerDateDetails
import com.rsgkh.calendar.domain.Zodiac
import com.rsgkh.calendar.domain.ZodiacSign
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class ZodiacTest {

    @Test
    fun allTwelveSignsFormatCorrectly() {
        assertEquals("♈︎ Aries (Fire · Mars)", ZodiacSign.ARIES.label)
        assertEquals("♉︎ Taurus (Earth · Venus)", ZodiacSign.TAURUS.label)
        assertEquals("♊︎ Gemini (Air · Mercury)", ZodiacSign.GEMINI.label)
        assertEquals("♋︎ Cancer (Water · Moon)", ZodiacSign.CANCER.label)
        assertEquals("♌︎ Leo (Fire · Sun)", ZodiacSign.LEO.label)
        assertEquals("♍︎ Virgo (Earth · Mercury)", ZodiacSign.VIRGO.label)
        assertEquals("♎︎ Libra (Air · Venus)", ZodiacSign.LIBRA.label)
        assertEquals("♏︎ Scorpio (Water · Pluto)", ZodiacSign.SCORPIO.label)
        assertEquals("♐︎ Sagittarius (Fire · Jupiter)", ZodiacSign.SAGITTARIUS.label)
        assertEquals("♑︎ Capricorn (Earth · Saturn)", ZodiacSign.CAPRICORN.label)
        assertEquals("♒︎ Aquarius (Air · Uranus)", ZodiacSign.AQUARIUS.label)
        assertEquals("♓︎ Pisces (Water · Neptune)", ZodiacSign.PISCES.label)
    }

    @Test
    fun boundaryDatesMatchSpecifiedRanges() {
        // Aries: Mar 21 – Apr 19
        assertEquals(ZodiacSign.PISCES, Zodiac.forDate(LocalDate.of(2026, 3, 20)))
        assertEquals(ZodiacSign.ARIES, Zodiac.forDate(LocalDate.of(2026, 3, 21)))
        assertEquals(ZodiacSign.ARIES, Zodiac.forDate(LocalDate.of(2026, 4, 19)))
        assertEquals(ZodiacSign.TAURUS, Zodiac.forDate(LocalDate.of(2026, 4, 20)))

        // Taurus: Apr 20 – May 20
        assertEquals(ZodiacSign.TAURUS, Zodiac.forDate(LocalDate.of(2026, 5, 20)))
        assertEquals(ZodiacSign.GEMINI, Zodiac.forDate(LocalDate.of(2026, 5, 21)))

        // Gemini: May 21 – Jun 20
        assertEquals(ZodiacSign.GEMINI, Zodiac.forDate(LocalDate.of(2026, 6, 20)))
        assertEquals(ZodiacSign.CANCER, Zodiac.forDate(LocalDate.of(2026, 6, 21)))

        // Cancer: Jun 21 – Jul 22
        assertEquals(ZodiacSign.CANCER, Zodiac.forDate(LocalDate.of(2026, 7, 22)))
        assertEquals(ZodiacSign.LEO, Zodiac.forDate(LocalDate.of(2026, 7, 23)))

        // Leo: Jul 23 – Aug 22
        assertEquals(ZodiacSign.LEO, Zodiac.forDate(LocalDate.of(2026, 8, 22)))
        assertEquals(ZodiacSign.VIRGO, Zodiac.forDate(LocalDate.of(2026, 8, 23)))

        // Virgo: Aug 23 – Sep 22
        assertEquals(ZodiacSign.VIRGO, Zodiac.forDate(LocalDate.of(2026, 9, 10)))
        assertEquals(ZodiacSign.VIRGO, Zodiac.forDate(LocalDate.of(2026, 9, 22)))
        assertEquals(ZodiacSign.LIBRA, Zodiac.forDate(LocalDate.of(2026, 9, 23)))

        // Libra: Sep 23 – Oct 22
        assertEquals(ZodiacSign.LIBRA, Zodiac.forDate(LocalDate.of(2026, 9, 24)))
        assertEquals(ZodiacSign.LIBRA, Zodiac.forDate(LocalDate.of(2026, 10, 22)))
        assertEquals(ZodiacSign.SCORPIO, Zodiac.forDate(LocalDate.of(2026, 10, 23)))

        // Scorpio: Oct 23 – Nov 21
        assertEquals(ZodiacSign.SCORPIO, Zodiac.forDate(LocalDate.of(2026, 11, 21)))
        assertEquals(ZodiacSign.SAGITTARIUS, Zodiac.forDate(LocalDate.of(2026, 11, 22)))

        // Sagittarius: Nov 22 – Dec 21
        assertEquals(ZodiacSign.SAGITTARIUS, Zodiac.forDate(LocalDate.of(2026, 12, 21)))
        assertEquals(ZodiacSign.CAPRICORN, Zodiac.forDate(LocalDate.of(2026, 12, 22)))

        // Capricorn: Dec 22 – Jan 19
        assertEquals(ZodiacSign.CAPRICORN, Zodiac.forDate(LocalDate.of(2026, 12, 31)))
        assertEquals(ZodiacSign.CAPRICORN, Zodiac.forDate(LocalDate.of(2026, 1, 1)))
        assertEquals(ZodiacSign.CAPRICORN, Zodiac.forDate(LocalDate.of(2026, 1, 19)))
        assertEquals(ZodiacSign.AQUARIUS, Zodiac.forDate(LocalDate.of(2026, 1, 20)))

        // Aquarius: Jan 20 – Feb 18
        assertEquals(ZodiacSign.AQUARIUS, Zodiac.forDate(LocalDate.of(2026, 2, 18)))
        assertEquals(ZodiacSign.PISCES, Zodiac.forDate(LocalDate.of(2026, 2, 19)))

        // Pisces: Feb 19 – Mar 20 (including leap day)
        assertEquals(ZodiacSign.PISCES, Zodiac.forDate(LocalDate.of(2024, 2, 29)))
        assertEquals(ZodiacSign.PISCES, Zodiac.forDate(LocalDate.of(2026, 3, 20)))
    }

    @Test
    fun khmerDateDetailsExposesZodiac() {
        val detailsSep10 = KhmerDateDetails.fromGregorian(LocalDate.of(2026, 9, 10))
        assertEquals("♍︎ Virgo (Earth · Mercury)", detailsSep10.zodiac.label)

        val detailsSep24 = KhmerDateDetails.fromGregorian(LocalDate.of(2026, 9, 24))
        assertEquals("♎︎ Libra (Air · Venus)", detailsSep24.zodiac.label)
    }

    @Test
    fun fullYearContinuityHasNoGaps() {
        var d = LocalDate.of(2024, 1, 1) // Leap year
        val end = LocalDate.of(2024, 12, 31)
        while (!d.isAfter(end)) {
            val sign = Zodiac.forDate(d)
            org.junit.Assert.assertNotNull(sign)
            d = d.plusDays(1)
        }
    }
}
