// SPDX-License-Identifier: Apache-2.0
package com.rsgkh.calendar.widgets

import android.content.Context
import com.rsgkh.calendar.data.CalendarEvent
import com.rsgkh.calendar.data.DateBasis
import com.rsgkh.calendar.data.EventKind
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class WidgetPrivacyTest {
    private val context: Context get() = RuntimeEnvironment.getApplication()

    @Test fun privateItemsExposeNeitherTitlesTimesNorIndividualEventIds() {
        val events = listOf(event("a", "Secret meeting"), event("b", "Private visit"))
        val rows = WidgetDataSource.displayItems(events, true, WidgetStrings(context, false))
        assertEquals(1, rows.size)
        assertEquals(2, rows.single().representedEvents)
        assertEquals("2 personal events", rows.single().title)
        assertNull(rows.single().time)
        assertNull(rows.single().eventId)
    }

    @Test fun publicHolidayRemainsVisibleInPrivateMode() {
        val events = listOf(event("a", "Private"), event("holiday", "Holiday").copy(kind = EventKind.HOLIDAY))
        val rows = WidgetDataSource.displayItems(events, true, WidgetStrings(context, false))
        assertEquals("Holiday", rows.first().title)
        assertEquals("holiday", rows.first().eventId)
    }

    @Test fun khmerLanguageIsIndependentOfSystemLanguage() {
        val rows = WidgetDataSource.displayItems(listOf(event("a", "Private")), true, WidgetStrings(context, true))
        assertTrue(rows.single().title.contains("១"))
        assertFalse(rows.single().title.contains("Private"))
    }

    private fun event(id: String, title: String) = CalendarEvent(
        id, LocalDate.of(2026, 9, 18), title, title, EventKind.CUSTOM, DateBasis.USER, LocalTime.of(9, 0),
    )
}
