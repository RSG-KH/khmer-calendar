// Copyright (c) 2026 RSG-KH | Apache-2.0 License
package com.rsgkh.calendar

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rsgkh.calendar.data.DateBasis
import com.rsgkh.calendar.data.EventRepository
import com.rsgkh.calendar.domain.KhmerDateDetails
import com.rsgkh.calendar.i18n.L
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

/** Runs against Android's ICU regex engine, which differs from the host JVM. */
@RunWith(AndroidJUnit4::class)
class TranslationDeviceTest {
    @Test fun packagedTranslationsAndCalculatedEventsLoadOnAndroid() {
        assertTrue(L.text("ui.events.11d867", true).isNotBlank())
        assertEquals("4 hours", L.text("common.reminder_hours", false, "hours" to 4))
        val date = KhmerDateDetails.fromGregorian(LocalDate.of(2026, 9, 10)).fullKhmerDate()
        assertFalse(date.contains("ខែខែ"))
        assertFalse(date.contains('{'))
        val future = EventRepository.forYear(2031).filter { it.basis == DateBasis.CALCULATED }
        assertTrue(future.isNotEmpty())
        assertTrue(future.all { it.titleKm.isNotBlank() && it.titleEn.isNotBlank() && !it.titleKm.contains('{') })
    }
}
