// Copyright (c) 2026 RSG-KH | Apache-2.0 License
package com.rsgkh.calendar

import com.rsgkh.calendar.data.TodayTimeZone
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class TodayTimeZoneTest {
    @Test fun localAndCambodiaDatesCanDifferAcrossMidnightAndNewYear() {
        val brussels = ZoneId.of("Europe/Brussels")
        val evening = Instant.parse("2026-09-09T18:00:00Z")
        assertEquals(LocalDate.of(2026, 9, 9), TodayTimeZone.LOCAL.today(evening, brussels))
        assertEquals(LocalDate.of(2026, 9, 10), TodayTimeZone.CAMBODIA.today(evening, brussels))
        val yearEnd = Instant.parse("2026-12-31T18:00:00Z")
        assertEquals(LocalDate.of(2026, 12, 31), TodayTimeZone.LOCAL.today(yearEnd, brussels))
        assertEquals(LocalDate.of(2027, 1, 1), TodayTimeZone.CAMBODIA.today(yearEnd, brussels))
        assertEquals(LocalDate.of(2027, 1, 1), TodayTimeZone.LOCAL.today(yearEnd, ZoneId.of("Pacific/Kiritimati")))
    }

    @Test fun offsetLabelFormatsPositiveNegativeFractionalAndUtcOffsets() {
        val winter = Instant.parse("2026-01-15T12:00:00Z")
        val summer = Instant.parse("2026-07-15T12:00:00Z")
        val brussels = ZoneId.of("Europe/Brussels")

        assertEquals("UTC+7", TodayTimeZone.CAMBODIA.offsetLabel())
        assertEquals("UTC+1", TodayTimeZone.LOCAL.offsetLabel(winter, brussels))
        assertEquals("UTC+2", TodayTimeZone.LOCAL.offsetLabel(summer, brussels))
        assertEquals("UTC-5", TodayTimeZone.LOCAL.offsetLabel(winter, ZoneId.of("America/New_York")))
        assertEquals("UTC-4", TodayTimeZone.LOCAL.offsetLabel(summer, ZoneId.of("America/New_York")))
        assertEquals("UTC+5:30", TodayTimeZone.LOCAL.offsetLabel(winter, ZoneId.of("Asia/Kolkata")))
        assertEquals("UTC+0", TodayTimeZone.LOCAL.offsetLabel(winter, ZoneId.of("UTC")))
    }

    @Test fun ganzhiHourUsesTheSelectedTodayTimeZone() {
        val moment = Instant.parse("2026-09-10T00:30:00Z")
        val brussels = ZoneId.of("Europe/Brussels")
        assertEquals(2, TodayTimeZone.LOCAL.hour(moment, brussels))
        assertEquals(7, TodayTimeZone.CAMBODIA.hour(moment, brussels))
    }
}
