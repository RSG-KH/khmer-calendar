// Copyright (c) 2026 RSG-KH | Apache-2.0 License
package com.rsgkh.calendar

import com.rsgkh.calendar.data.TodayTimeZone
import com.rsgkh.calendar.data.Birthplace
import com.rsgkh.calendar.domain.westernBig3Signs
import com.rsgkh.calendar.engine.western.WesternZodiacSign
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class WesternBig3Test {
    private val brussels = ZoneId.of("Europe/Brussels")
    private val belgium = Birthplace("manual", "Brussels", "Brussels", "BE", 50.8503, 4.3517, "Europe/Brussels")

    @Test fun samePastDateAndClockTimeUseTheSelectedZone() {
        val date = LocalDate.of(2026, 1, 15)
        val time = LocalTime.of(0, 30)

        val cambodia = westernBig3Signs(date, time, TodayTimeZone.CAMBODIA, brussels, place = Birthplace.DEFAULT)
        val belgium = westernBig3Signs(date, time, TodayTimeZone.LOCAL, brussels, place = belgium)
        assertEquals(WesternZodiacSign.SCORPIO, cambodia?.rising)
        assertEquals(WesternZodiacSign.LIBRA, belgium?.rising)
    }

    @Test fun localOffsetUsesTheSelectedDateAcrossDaylightSavingTime() {
        assertEquals(WesternZodiacSign.SCORPIO,
            westernBig3Signs(LocalDate.of(2026, 1, 15), LocalTime.of(2, 30), TodayTimeZone.LOCAL, brussels, place = belgium)?.rising)
        assertEquals(WesternZodiacSign.ARIES,
            westernBig3Signs(LocalDate.of(2026, 7, 15), LocalTime.of(0, 30), TodayTimeZone.LOCAL, brussels, place = belgium)?.rising)
    }

    @Test fun skippedLocalTimeHasNoRisingSignAndKeepsNoonSunAndMoon() {
        val date = LocalDate.of(2026, 3, 29)
        val skipped = westernBig3Signs(date, LocalTime.of(2, 30), TodayTimeZone.LOCAL, brussels, place = belgium)
        val noon = westernBig3Signs(date, null, TodayTimeZone.LOCAL, brussels, place = belgium)
        assertNotNull(skipped)
        assertNotNull(noon)
        assertNull(skipped?.rising)
        assertEquals(noon?.sun, skipped?.sun)
        assertEquals(noon?.moon, skipped?.moon)
    }
    @Test fun noPlaceDoesNotInventARisingSign() {
        val signs = westernBig3Signs(LocalDate.of(2026, 9, 27), LocalTime.NOON, TodayTimeZone.CAMBODIA)
        assertNotNull(signs)
        assertNull(signs?.rising)
    }

    @Test fun placeZoneResolvesTheUnshiftedClockRegardlessOfTodayZone() {
        val date = LocalDate.of(2026, 9, 27)
        assertEquals(westernBig3Signs(date, LocalTime.of(9, 0), TodayTimeZone.LOCAL, brussels, Birthplace.DEFAULT),
            westernBig3Signs(date, LocalTime.of(9, 0), TodayTimeZone.CAMBODIA, brussels, Birthplace.DEFAULT))
    }
}
