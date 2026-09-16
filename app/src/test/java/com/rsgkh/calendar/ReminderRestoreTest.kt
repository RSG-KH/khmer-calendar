// Copyright (c) 2026 RSG-KH | Apache-2.0 License
package com.rsgkh.calendar

import android.app.AlarmManager
import android.app.Application
import android.app.NotificationManager
import android.content.Intent
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
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import java.util.TimeZone
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [32])
class ReminderRestoreTest {
    private val context get() = ApplicationProvider.getApplicationContext<Application>()
    private fun scheduled() = Shadows.shadowOf(context.getSystemService(AlarmManager::class.java))
        .scheduledAlarms.single().triggerAtMs

    private fun sendSystemChange(action: String) {
        val intent = Intent(action).setPackage(context.packageName)
        assertTrue(context.packageManager.queryBroadcastReceivers(intent, 0)
            .any { it.activityInfo.name == ReminderRestoreReceiver::class.java.name })
        // Use the manifest receiver: no activity or dynamically registered receiver.
        context.sendBroadcast(intent)
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        ReminderWork.submit(context) {}.get(5, TimeUnit.SECONDS)
    }

    @Test fun clockChangeBroadcastRepairsForwardAndBackwardJumpsWithoutAnActivity() {
        val now = Instant.now()
        val today = now.atZone(ZoneOffset.UTC).toLocalDate()
        val events = listOf(-2L, 1L, 4L).map { days ->
            CustomEvent(title = "Appointment $days", date = today.plusDays(days), time = LocalTime.NOON, zoneId = "UTC")
        }
        CustomEventRepository(context).use { repository ->
            events.forEach { repository.save(it, now.minus(4, ChronoUnit.DAYS)) }
        }
        AppPreferences(context).write(AppSettings(notificationsEnabled = true, khmer = false,
            pushHolidays = false, pushObservances = false, pushHolyDays = false))

        // The stored alarm reflects the clock before a forward jump.
        EventNotifications.reschedule(context, now.minus(3, ChronoUnit.DAYS))
        val obsoletePastAlarm = scheduled()
        assertEquals(events[0].instant.toEpochMilli(), obsoletePastAlarm)
        sendSystemChange(Intent.ACTION_TIME_CHANGED)
        assertEquals(events[1].instant.toEpochMilli(), scheduled())

        // The stored alarm reflects the clock before a backward jump.
        EventNotifications.reschedule(context, now.plus(3, ChronoUnit.DAYS))
        val obsoleteFutureAlarm = scheduled()
        assertEquals(events[2].instant.toEpochMilli(), obsoleteFutureAlarm)
        sendSystemChange(Intent.ACTION_TIME_CHANGED)
        assertEquals(events[1].instant.toEpochMilli(), scheduled())

        val manager = context.getSystemService(NotificationManager::class.java)
        EventNotifications.deliver(context, obsoletePastAlarm, now)
        EventNotifications.deliver(context, obsoleteFutureAlarm, now)
        assertTrue(manager.activeNotifications.isEmpty())

        EventNotifications.deliver(context, scheduled(), events[1].instant)
        val posted = manager.activeNotifications.single().notification
        assertTrue(posted.extras.getCharSequence("android.bigText").toString().contains(events[1].title))
        assertEquals(events[2].instant.toEpochMilli(), scheduled())
    }

    @Test fun zoneChangeBroadcastUpdatesLocalRemindersAndPreservesCambodiaTimeWithoutAnActivity() {
        val originalZone = TimeZone.getDefault()
        try {
            val pushHour = (Instant.now().atZone(ZoneOffset.UTC).hour + 12) % 24
            for (choice in TodayTimeZone.entries) {
                TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
                val settings = AppSettings(notificationsEnabled = true, todayTimeZone = choice,
                    pushCustomEvents = false, pushMinutes = pushHour * 60)
                AppPreferences(context).write(settings)
                EventNotifications.reschedule(context)
                val originalAlarm = scheduled()

                TimeZone.setDefault(TimeZone.getTimeZone(CAMBODIA_ZONE))
                sendSystemChange(Intent.ACTION_TIMEZONE_CHANGED)
                val expected = ReminderPlanner.next(Instant.now(), settings, emptyList())!!.at.toEpochMilli()
                assertEquals(expected, scheduled())
                if (choice == TodayTimeZone.LOCAL) assertNotEquals(originalAlarm, scheduled())
                else assertEquals(originalAlarm, scheduled())
            }
        } finally { TimeZone.setDefault(originalZone) }
    }
}
