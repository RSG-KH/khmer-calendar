// Copyright (c) 2026 RSG-KH | Apache-2.0 License
package com.rsgkh.calendar

import android.Manifest
import android.app.AlarmManager
import android.app.Application
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.sqlite.SQLiteDatabase
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.rsgkh.calendar.data.*
import com.rsgkh.calendar.notifications.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAlarmManager
import java.time.*
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [32])
class NotificationIntegrationTest {
    private val context get() = ApplicationProvider.getApplicationContext<Application>()
    private val now = Instant.parse("2026-09-23T21:59:00Z")
    @Test fun existingDatabaseMigratesWithoutMovingSavedCambodiaEvents() {
        val file = context.getDatabasePath("custom-events.db")
        file.parentFile!!.mkdirs()
        SQLiteDatabase.openOrCreateDatabase(file, null).use { db ->
            db.execSQL("CREATE TABLE events (id TEXT PRIMARY KEY, title TEXT NOT NULL, date TEXT NOT NULL, time TEXT NOT NULL, notes TEXT NOT NULL, remind INTEGER NOT NULL)")
            db.execSQL("INSERT INTO events VALUES ('legacy', 'Existing reminder', '2026-09-24', '09:00', 'Keep notes', 1)")
            db.version = 1
        }
        CustomEventRepository(context).use { repository ->
            val saved = repository.all().single()
            assertEquals(CAMBODIA_ZONE.id, saved.zoneId)
            assertEquals(Instant.parse("2026-09-24T02:00:00Z"), saved.instant)
            assertEquals("Keep notes", saved.notes)
            assertTrue(saved.remindersEligible)
            assertEquals(2, repository.readableDatabase.version)
        }
    }
    @Test fun localTimeAndRepeatedClockOffsetSurviveStorageAndDeterminePastEligibility() {
        val local = CustomEvent(title = "Local appointment", date = LocalDate.of(2026, 9, 24), time = LocalTime.of(9, 0), zoneId = "Europe/Brussels")
        CustomEventRepository(context).use { it.save(local, Instant.parse("2026-09-24T06:00:00Z")) }
        CustomEventRepository(context).use {
            val saved = it.all().single()
            assertEquals(local.instant, saved.instant)
            assertEquals(local.zoneId, saved.zoneId)
            assertTrue(saved.remindersEligible) // 09:00 local is still ahead; 09:00 Cambodia would be past.
            it.save(local, Instant.parse("2026-09-24T07:01:00Z"))
            assertFalse(it.all().single().remindersEligible)
            val repeated = local.copy(date = LocalDate.of(2026, 10, 25), time = LocalTime.of(2, 30), offsetSeconds = 3600)
            it.save(repeated, now)
            assertEquals(Instant.parse("2026-10-25T01:30:00Z"), it.all().single().instant)
        }
    }
    @Test fun localReminderUsesItsOwnDayAndOpensTheDateShownInTheSelectedZone() {
        val originalZone = java.util.TimeZone.getDefault()
        java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("America/Los_Angeles"))
        try {
            val local = CustomEvent(title = "Late local appointment", date = LocalDate.of(2026, 9, 24), time = LocalTime.of(23, 30), zoneId = "America/Los_Angeles")
            AppPreferences(context).write(AppSettings(notificationsEnabled = true, khmer = false, repeatHours = 0))
            CustomEventRepository(context).use { it.save(local, local.instant.minusSeconds(60)) }
            EventNotifications.reschedule(context, local.instant.minusSeconds(1))
            val alarm = Shadows.shadowOf(context.getSystemService(AlarmManager::class.java))
            val scheduled = alarm.scheduledAlarms.single().triggerAtTime
            assertEquals(local.instant.toEpochMilli(), scheduled)
            EventNotifications.deliver(context, scheduled, local.instant.plusSeconds(60))
            val posted = context.getSystemService(NotificationManager::class.java).activeNotifications.single().notification
            assertTrue(posted.extras.getCharSequence("android.bigText").toString().contains("23:30 · Late local appointment"))
            assertEquals("2026-09-24", Shadows.shadowOf(posted.contentIntent).savedIntent.getStringExtra(EventNotifications.EXTRA_DATE))
            EventNotifications.clearDisplayed(context)
            context.getSharedPreferences("reminder-state", Context.MODE_PRIVATE).edit().remove("delivered").apply()
            EventNotifications.reschedule(context, local.instant.minusSeconds(1))
            EventNotifications.deliver(context, scheduled, local.instant.plusSeconds(31 * 60))
            assertTrue(context.getSystemService(NotificationManager::class.java).activeNotifications.isEmpty())
        } finally { java.util.TimeZone.setDefault(originalZone) }
    }
    @Test fun customEventStoragePersistsEditsAndSuppressesHistoricalReminders() {
        val event = CustomEvent(title = " Visit ", date = LocalDate.of(2099, 1, 2), time = LocalTime.of(9, 30), notes = "Details")
        CustomEventRepository(context).use { it.save(event, now) }
        CustomEventRepository(context).use {
            assertEquals("Visit", it.all().single().title)
            assertTrue(it.all().single().remindersEligible)
            it.save(event.copy(date = LocalDate.of(1999, 1, 2)), now)
        }
        CustomEventRepository(context).use {
            assertEquals(1999, it.all().single().date.year)
            assertFalse(it.all().single().remindersEligible)
            it.delete(event.id)
            assertTrue(it.all().isEmpty())
        }
    }
    @Test fun settingsSurviveRecreationAndAlarmDeliverySchedulesTheNextRepeat() {
        val settings = AppSettings(notificationsEnabled = true, pushMinutes = 300, repeatHours = 4, khmer = false)
        AppPreferences(context).write(settings)
        assertEquals(settings, AppPreferences(context).read())
        ShadowAlarmManager.setCanScheduleExactAlarms(true)
        EventNotifications.reschedule(context, now)
        val alarm = Shadows.shadowOf(context.getSystemService(AlarmManager::class.java))
        val scheduled = alarm.scheduledAlarms.single().triggerAtTime
        assertEquals(Instant.parse("2026-09-23T22:00:00Z").toEpochMilli(), scheduled)
        EventNotifications.deliver(context, scheduled, Instant.ofEpochMilli(scheduled))
        val posted = context.getSystemService(NotificationManager::class.java).activeNotifications.single().notification
        assertTrue(posted.extras.getCharSequence("android.bigText").toString().contains("Constitution Day"))
        assertEquals("2026-09-24", Shadows.shadowOf(posted.contentIntent).savedIntent.getStringExtra(EventNotifications.EXTRA_DATE))
        assertEquals(scheduled + 4 * 3600000, alarm.scheduledAlarms.single().triggerAtTime)
        EventNotifications.deliver(context, scheduled, Instant.ofEpochMilli(scheduled))
        assertEquals(1, context.getSystemService(NotificationManager::class.java).activeNotifications.size)
        AppPreferences(context).write(settings.copy(notificationsEnabled = false))
        EventNotifications.reschedule(context, now)
        assertTrue(alarm.scheduledAlarms.isEmpty())
        assertTrue(context.getSystemService(NotificationManager::class.java).activeNotifications.isEmpty())
    }
    @Test fun restoreAndInexactFallbackKeepOneAlarmAndHonorChangedSettings() {
        AppPreferences(context).write(AppSettings(notificationsEnabled = true))
        ShadowAlarmManager.setCanScheduleExactAlarms(false)
        EventNotifications.reschedule(context, now)
        val alarm = Shadows.shadowOf(context.getSystemService(AlarmManager::class.java))
        assertEquals(1, alarm.scheduledAlarms.size)
        assertFalse(EventNotifications.canBeExact(context))
        AppPreferences(context).write(AppPreferences(context).read().copy(pushMinutes = 6 * 60, repeatHours = 8))
        EventNotifications.reschedule(context, now)
        assertEquals(Instant.parse("2026-09-23T23:00:00Z").toEpochMilli(), alarm.scheduledAlarms.single().triggerAtTime)
        val receiver = ReminderRestoreReceiver()
        context.registerReceiver(receiver, IntentFilter(Intent.ACTION_BOOT_COMPLETED))
        try {
            context.sendBroadcast(Intent(Intent.ACTION_BOOT_COMPLETED))
            Shadows.shadowOf(Looper.getMainLooper()).idle()
            val shadow = Shadows.shadowOf(receiver)
            assertTrue(shadow.wentAsync())
            Shadows.shadowOf(shadow.originalPendingResult).future.get(5, TimeUnit.SECONDS)
            assertEquals(1, alarm.scheduledAlarms.size)
            assertTrue(alarm.scheduledAlarms.single().triggerAtTime > Instant.now().toEpochMilli())
        } finally { context.unregisterReceiver(receiver) }
    }
    @Test fun repeatOffDeliversTheFirstNotificationEvenWhenAndroidIsLate() {
        AppPreferences(context).write(AppSettings(notificationsEnabled = true, repeatHours = 0, khmer = false))
        EventNotifications.reschedule(context, now)
        val alarm = Shadows.shadowOf(context.getSystemService(AlarmManager::class.java))
        val scheduled = alarm.scheduledAlarms.single().triggerAtTime
        val due = Instant.ofEpochMilli(scheduled)
        EventNotifications.deliver(context, scheduled, due.plusSeconds(30))
        val posted = context.getSystemService(NotificationManager::class.java).activeNotifications.single().notification
        assertTrue(posted.extras.getCharSequence("android.bigText").toString().contains("Constitution Day"))
        val nextDay = Instant.ofEpochMilli(alarm.scheduledAlarms.single().triggerAtTime).atZone(CAMBODIA_ZONE).toLocalDate()
        assertTrue(nextDay > due.atZone(CAMBODIA_ZONE).toLocalDate())
    }
    @Test @Config(sdk = [33]) fun deniedNotificationPermissionDoesNotLeaveAnAlarmRunning() {
        val app = Shadows.shadowOf(context)
        app.denyPermissions(Manifest.permission.POST_NOTIFICATIONS)
        AppPreferences(context).write(AppSettings(notificationsEnabled = true))
        EventNotifications.reschedule(context, now)
        assertFalse(EventNotifications.canPost(context))
        val alarm = Shadows.shadowOf(context.getSystemService(AlarmManager::class.java))
        assertTrue(alarm.scheduledAlarms.isEmpty())
        app.grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        EventNotifications.reschedule(context, now)
        assertTrue(EventNotifications.canPost(context))
        assertEquals(1, alarm.scheduledAlarms.size)
    }
}
