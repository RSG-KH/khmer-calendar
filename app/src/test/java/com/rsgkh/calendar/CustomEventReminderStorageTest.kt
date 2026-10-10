// Copyright (c) 2026 RSG-KH | Apache-2.0 License
package com.rsgkh.calendar

import android.app.Application
import android.database.sqlite.SQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import com.rsgkh.calendar.data.*
import com.rsgkh.calendar.domain.EventRepeat
import com.rsgkh.calendar.domain.RepeatFrequency
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.*

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [32])
class CustomEventReminderStorageTest {
    private val context get() = ApplicationProvider.getApplicationContext<Application>()
    private val now = Instant.parse("2026-10-01T00:00:00Z")
    private val event = CustomEvent(id = "lead", title = "Lead time", date = LocalDate.of(2026, 11, 9),
        time = LocalTime.of(9, 0), zoneId = "Europe/Brussels", reminderMinutesBefore = 15)

    @Test fun versionThreeMigrationPreservesSeriesAndAddsZeroMinuteDefault() {
        val file = context.getDatabasePath("custom-events.db")
        file.parentFile!!.mkdirs()
        SQLiteDatabase.openOrCreateDatabase(file, null).use { db ->
            db.execSQL("CREATE TABLE events (id TEXT PRIMARY KEY, title TEXT NOT NULL, date TEXT NOT NULL, time TEXT NOT NULL, notes TEXT NOT NULL, remind INTEGER NOT NULL, zone_id TEXT NOT NULL, offset_seconds INTEGER, repeat_frequency TEXT, repeat_until TEXT, repeat_interval INTEGER NOT NULL DEFAULT 3, include_thirty INTEGER NOT NULL DEFAULT 0, include_february INTEGER NOT NULL DEFAULT 0, remind_after TEXT)")
            db.execSQL("INSERT INTO events VALUES ('legacy', 'Keep series', '2026-10-25', '02:30', 'Keep notes', 1, 'Europe/Brussels', 3600, 'DAYS', '2026-12-31', 1, 0, 0, '2026-10-01T00:00:00Z')")
            db.version = 3
        }
        CustomEventRepository(context).use { repo ->
            val saved = repo.all().single()
            assertEquals(4, repo.readableDatabase.version)
            assertEquals(0, saved.reminderMinutesBefore)
            assertEquals(Instant.parse("2026-10-25T01:30:00Z"), saved.instant)
            assertEquals(EventRepeat(RepeatFrequency.DAYS, LocalDate.of(2026, 12, 31), interval = 1), saved.repeat)
            assertEquals(now, saved.remindersAfter)
            assertEquals("Keep notes", saved.notes)
            assertTrue(saved.remindersEligible)
        }
    }

    @Test fun advanceMinutesSurviveReopeningAndEditingBothSingleEventsAndSeries() {
        for (repeat in listOf(null, EventRepeat(RepeatFrequency.DAYS, event.date.plusDays(3), interval = 1))) {
            CustomEventRepository(context).use { it.save(event.copy(repeat = repeat), now) }
            CustomEventRepository(context).use { repo ->
                val saved = repo.all().single()
                assertEquals(15, saved.reminderMinutesBefore)
                assertEquals(repeat, saved.repeat)
                assertEquals(event.instant, saved.instant)
                repo.save(saved.copy(title = "Edited", reminderMinutesBefore = 180), now)
            }
            CustomEventRepository(context).use { repo ->
                assertEquals(180, repo.all().single().reminderMinutesBefore)
                repo.save(repo.all().single().copy(reminderMinutesBefore = 0), now)
                assertEquals(0, repo.all().single().reminderMinutesBefore)
            }
        }
    }

    @Test fun invalidLeadCannotReplaceSavedDataAndCorruptRowsDoNotHideValidNeighbors() {
        CustomEventRepository(context).use { repo ->
            repo.save(event, now)
            for (minutes in listOf(-1, 181, Int.MAX_VALUE)) {
                assertThrows(IllegalArgumentException::class.java) { repo.save(event.copy(reminderMinutesBefore = minutes), now) }
                assertEquals(15, repo.all().single().reminderMinutesBefore)
            }
            repo.save(event.copy(id = "damaged"), now)
            repo.writableDatabase.execSQL("UPDATE events SET reminder_minutes_before = 181 WHERE id = 'damaged'")
            assertEquals(listOf(event.id), repo.all().map { it.id })
        }
    }
}
