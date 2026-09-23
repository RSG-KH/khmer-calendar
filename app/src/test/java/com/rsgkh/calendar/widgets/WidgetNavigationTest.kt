// SPDX-License-Identifier: Apache-2.0
package com.rsgkh.calendar.widgets

import android.content.Context
import com.rsgkh.calendar.data.CustomEvent
import com.rsgkh.calendar.domain.EventRepeat
import com.rsgkh.calendar.domain.RepeatFrequency
import com.rsgkh.calendar.notifications.EventNotifications
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class WidgetNavigationTest {
    private val context: Context get() = RuntimeEnvironment.getApplication()
    private val day = LocalDate.of(2026, 1, 8)

    @Test fun distinctRowsDoNotSharePendingIntentIdentity() {
        val one = WidgetNavigation.dateIntent(context, 101, day, "custom:one")
        val two = WidgetNavigation.dateIntent(context, 101, day, "custom:two")
        assertFalse(one.filterEquals(two))
        assertFalse(one.filterEquals(WidgetNavigation.dateIntent(context, 102, day, "custom:one")))
        assertFalse(one.filterEquals(WidgetNavigation.dateIntent(context, 101, day.plusDays(1), "custom:one")))
    }

    @Test fun dayLinkKeepsExistingNotificationExtraAndNoEventExtra() {
        val intent = WidgetNavigation.dateIntent(context, 101, day)
        assertEquals(day.toString(), intent.getStringExtra(EventNotifications.EXTRA_DATE))
        assertFalse(intent.hasExtra(WidgetNavigation.EXTRA_EVENT_ID))
    }

    @Test fun deletedCustomEventFallsBackToDay() {
        assertNull(WidgetNavigation.resolve(WidgetEventRequest(day, "custom:missing", 1L), emptyList(), ZoneId.of("UTC")))
    }

    @Test fun repeatingOccurrenceResolvesUsingSourceAndDisplayDates() {
        val start = LocalDate.of(2026, 1, 1)
        val event = CustomEvent(
            id = "weekly", title = "Weekly", date = start, time = LocalTime.of(0, 30),
            zoneId = "Asia/Phnom_Penh",
            repeat = EventRepeat(RepeatFrequency.WEEKLY, LocalDate.of(2026, 1, 31)),
        )
        // Thursday 00:30 in Cambodia is Wednesday 17:30 UTC.
        val displayDay = LocalDate.of(2026, 1, 7)
        val request = WidgetEventRequest(displayDay, "custom:weekly@2026-01-08", 1L)
        val result = WidgetNavigation.resolve(request, listOf(event), ZoneId.of("UTC"))
        assertNotNull(result)
        assertEquals(displayDay, result!!.date)
        assertEquals(LocalTime.of(17, 30), result.time)
    }

    @Test fun monthDaysIncludeLunarLabelsAndLotusWatermarks() {
        val snapshot = WidgetDataSource.load(context, 0, Instant.parse("2026-05-15T10:00:00Z"), includeMonth = true)
        assertNotNull(snapshot)
        assertTrue(snapshot.monthDays.isNotEmpty())
        val holyDays = snapshot.monthDays.filter { it.isHolyDay }
        assertTrue(holyDays.isNotEmpty())
        assertTrue(holyDays.all { it.lotusRes != null })
    }
}
