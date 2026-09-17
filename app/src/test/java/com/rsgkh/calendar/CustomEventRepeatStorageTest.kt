// Copyright (c) 2026 RSG-KH | Apache-2.0 License
package com.rsgkh.calendar

import android.app.Application
import android.database.sqlite.SQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import com.rsgkh.calendar.data.*
import com.rsgkh.calendar.domain.*
import com.rsgkh.calendar.notifications.ReminderPlanner
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.*

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [32])
class CustomEventRepeatStorageTest {
    private val context get() = ApplicationProvider.getApplicationContext<Application>()
    private val now = Instant.parse("2026-04-01T10:00:00Z")
    private val event = CustomEvent(id = "series", title = "Monthly", date = LocalDate.of(2026, 1, 31), time = LocalTime.of(9, 0),
        zoneId = "Europe/Brussels", repeat = EventRepeat(RepeatFrequency.MONTHLY, LocalDate.of(2026, 12, 31), includeThirty = true, includeFebruary = true))

    @Test fun versionTwoDatabaseMigratesWithoutChangingInstantsOrEligibility() {
        val file = context.getDatabasePath("custom-events.db")
        file.parentFile!!.mkdirs()
        SQLiteDatabase.openOrCreateDatabase(file, null).use { db ->
            db.execSQL("CREATE TABLE events (id TEXT PRIMARY KEY, title TEXT NOT NULL, date TEXT NOT NULL, time TEXT NOT NULL, notes TEXT NOT NULL, remind INTEGER NOT NULL, zone_id TEXT NOT NULL, offset_seconds INTEGER)")
            db.execSQL("INSERT INTO events VALUES ('legacy', 'Second fold', '2026-10-25', '02:30', 'Keep notes', 0, 'Europe/Brussels', 3600)")
            db.version = 2
        }
        CustomEventRepository(context).use {
            val saved = it.all().single()
            assertEquals(3, it.readableDatabase.version)
            assertEquals(Instant.parse("2026-10-25T01:30:00Z"), saved.instant)
            assertEquals("Keep notes", saved.notes)
            assertFalse(saved.remindersEligible)
            assertNull(saved.repeat)
            assertNull(saved.remindersAfter)
        }
    }

    @Test fun seriesRoundTripsAndPastAnchorStillSchedulesFutureOccurrences() {
        CustomEventRepository(context).use { it.save(event, now) }
        CustomEventRepository(context).use {
            val saved = it.all().single()
            assertEquals(event.repeat, saved.repeat)
            assertEquals(event.date, saved.date)
            assertEquals(event.instant, saved.instant)
            assertEquals(now, saved.remindersAfter)
            assertTrue(saved.remindersEligible)
            assertEquals(12, saved.occurrences(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), saved.zone).size)
            val next = ReminderPlanner.next(now, AppSettings(notificationsEnabled = true), listOf(saved)) { emptyList() }!!
            assertEquals(Instant.parse("2026-04-30T07:00:00Z"), next.at)
            it.save(saved.copy(title = "Edited", repeat = null), now)
            val single = it.all().single()
            assertNull(single.repeat)
            assertFalse(single.remindersEligible)
            assertEquals("Edited", single.title)
            it.delete(single.id)
            assertTrue(it.all().isEmpty())
        }
    }

    @Test fun invalidRepeatIsRejectedWithoutReplacingTheSavedEvent() {
        CustomEventRepository(context).use { repo ->
            repo.save(event, now)
            for (invalid in listOf(event.repeat!!.copy(until = event.date.minusDays(1)),
                EventRepeat(RepeatFrequency.DAYS, event.date, interval = 0))) {
                assertThrows(IllegalArgumentException::class.java) { repo.save(event.copy(repeat = invalid), now) }
                assertEquals(event.repeat, repo.all().single().repeat)
            }
        }
    }
}
