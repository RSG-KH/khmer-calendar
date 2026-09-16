// Copyright (c) 2026 RSG-KH | Apache-2.0 License
package com.rsgkh.calendar

import android.Manifest
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.rsgkh.calendar.data.*
import com.rsgkh.calendar.notifications.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** Exercises the manifest receiver and real OS notification service, not shadows. */
@RunWith(AndroidJUnit4::class)
class NotificationDeviceTest {
    @Test fun receiverPostsOneNotificationAndDisablingClearsIt() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val preferences = AppPreferences(context)
        val original = preferences.read()
        val manager = context.getSystemService(NotificationManager::class.java)
        val due = Instant.now().truncatedTo(ChronoUnit.MINUTES)
        val local = due.atZone(CAMBODIA_ZONE)
        val event = CustomEvent(title = "Device reminder check", date = local.toLocalDate(), time = local.toLocalTime())
        fun fireDueAlarm() {
            val completed = CountDownLatch(1)
            // Ordered-broadcast completion waits for the receiver's goAsync work to finish.
            context.sendOrderedBroadcast(Intent(context, EventReminderReceiver::class.java)
                .setAction(EventNotifications.ACTION_FIRE).putExtra("at", due.toEpochMilli()),
                null, object : BroadcastReceiver() {
                    override fun onReceive(context: Context, intent: Intent) { completed.countDown() }
                }, null, 0, null, null)
            assertTrue("Reminder receiver did not finish", completed.await(5, TimeUnit.SECONDS))
        }
        try {
            if (Build.VERSION.SDK_INT >= 33) instrumentation.uiAutomation.grantRuntimePermission(context.packageName, Manifest.permission.POST_NOTIFICATIONS)
            // The device may have categories disabled from an earlier manual test.
            preferences.write(original.copy(notificationsEnabled = true, khmer = false,
                pushCustomEvents = true, pushHolidays = true, pushObservances = true))
            CustomEventRepository(context).use { it.save(event, due.minusSeconds(1)) }
            // Recreate a due alarm without waiting for the wall clock or changing the device clock.
            EventNotifications.reschedule(context, due.minusSeconds(1))
            fireDueAlarm()
            val deadline = SystemClock.elapsedRealtime() + 5000
            while (manager.activeNotifications.none { it.notification.extras.getCharSequence("android.bigText").toString().contains(event.title) } &&
                SystemClock.elapsedRealtime() < deadline) SystemClock.sleep(50)
            val posted = manager.activeNotifications.single { it.notification.channelId == EventNotifications.CHANNEL }.notification
            assertTrue(posted.extras.getCharSequence("android.bigText").toString().contains("${event.asCalendarEvent(original.todayTimeZone.zone()).time} · ${event.title}"))
            assertEquals(context.packageName, posted.contentIntent.creatorPackage)
            assertTrue(context.getSharedPreferences("reminder-state", Context.MODE_PRIVATE).getLong("scheduled", 0) > due.toEpochMilli())
            fireDueAlarm()
            assertEquals(1, manager.activeNotifications.count { it.notification.channelId == EventNotifications.CHANNEL })
            preferences.write(original.copy(notificationsEnabled = false))
            EventNotifications.reschedule(context)
            val cancelDeadline = SystemClock.elapsedRealtime() + 3000
            while (manager.activeNotifications.any { it.notification.channelId == EventNotifications.CHANNEL } &&
                SystemClock.elapsedRealtime() < cancelDeadline) SystemClock.sleep(50)
            assertTrue(manager.activeNotifications.none { it.notification.channelId == EventNotifications.CHANNEL })
        } finally {
            CustomEventRepository(context).use { it.delete(event.id) }
            preferences.write(original)
            EventNotifications.clearDisplayed(context)
            EventNotifications.reschedule(context)
        }
    }
}
