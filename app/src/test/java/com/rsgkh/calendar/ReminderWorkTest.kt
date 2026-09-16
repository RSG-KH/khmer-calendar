// Copyright (c) 2026 RSG-KH | Apache-2.0 License
package com.rsgkh.calendar

import android.app.AlarmManager
import android.app.Application
import android.app.NotificationManager
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.IntentFilter
import android.os.Looper
import androidx.core.content.ContextCompat
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
import java.time.temporal.ChronoUnit
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [32])
class ReminderWorkTest {
    private val context get() = ApplicationProvider.getApplicationContext<Application>()

    @Test fun schedulingReturnsWhileEarlierWorkIsBlockedAndUsesTheLatestSettings() {
        val caller = Thread.currentThread()
        val started = CountDownLatch(1)
        val release = CountDownLatch(1)
        val first = ReminderWork.submit(ContextWrapper(context)) {
            assertSame(context, it)
            assertNotSame(caller, Thread.currentThread())
            started.countDown()
            check(release.await(5, TimeUnit.SECONDS))
        }
        try {
            assertTrue(started.await(5, TimeUnit.SECONDS))
            AppPreferences(context).write(AppSettings(notificationsEnabled = true))
            val scheduled = EventNotifications.rescheduleAsync(context)
            assertFalse(scheduled.isDone)
            // Disabling reminders while work is queued must not leave an obsolete alarm.
            AppPreferences(context).write(AppSettings(notificationsEnabled = false))
            release.countDown()
            scheduled.get(5, TimeUnit.SECONDS)
            assertTrue(Shadows.shadowOf(context.getSystemService(AlarmManager::class.java)).scheduledAlarms.isEmpty())
        } finally {
            release.countDown()
            first.get(5, TimeUnit.SECONDS)
        }
    }

    @Test fun failedWorkStillFinishesAndDoesNotPreventTheNextRequest() {
        val finishes = AtomicInteger()
        val failure = IllegalStateException("Simulated storage failure")
        val failed = ReminderWork.submit(context, onFinished = { finishes.incrementAndGet() }) { throw failure }
        val error = assertThrows(ExecutionException::class.java) { failed.get(5, TimeUnit.SECONDS) }
        assertSame(failure, error.cause)
        assertEquals(1, finishes.get())
        EventNotifications.rescheduleAsync(context).get(5, TimeUnit.SECONDS)
    }

    @Test fun failedRestoreBroadcastStillReleasesItsPendingResult() {
        val preferences = context.getSharedPreferences("appearance", Context.MODE_PRIVATE)
        preferences.edit().putString("notificationsEnabled", "invalid stored type").apply()
        val receiver = ReminderRestoreReceiver()
        context.registerReceiver(receiver, IntentFilter(Intent.ACTION_BOOT_COMPLETED))
        try {
            context.sendBroadcast(Intent(Intent.ACTION_BOOT_COMPLETED))
            Shadows.shadowOf(Looper.getMainLooper()).idle()
            val shadow = Shadows.shadowOf(receiver)
            assertTrue(shadow.wentAsync())
            Shadows.shadowOf(shadow.originalPendingResult).future.get(5, TimeUnit.SECONDS)
        } finally {
            context.unregisterReceiver(receiver)
            preferences.edit().remove("notificationsEnabled").apply()
        }
        EventNotifications.rescheduleAsync(context).get(5, TimeUnit.SECONDS)
    }

    @Test fun reminderBroadcastFinishesAfterPostingAndSchedulingTheNextAlarm() {
        val due = Instant.now().truncatedTo(ChronoUnit.MINUTES)
        val local = due.atZone(CAMBODIA_ZONE)
        val event = CustomEvent(title = "Background reminder", date = local.toLocalDate(), time = local.toLocalTime())
        AppPreferences(context).write(AppSettings(notificationsEnabled = true, khmer = false))
        CustomEventRepository(context).use { it.save(event, due.minusSeconds(1)) }
        EventNotifications.reschedule(context, due.minusMillis(1))
        val receiver = EventReminderReceiver()
        // Robolectric does not automatically grant the manifest's signature permission.
        Shadows.shadowOf(context).grantPermissions("${context.packageName}.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION")
        ContextCompat.registerReceiver(context, receiver, IntentFilter(EventNotifications.ACTION_FIRE), ContextCompat.RECEIVER_NOT_EXPORTED)
        try {
            repeat(2) {
                context.sendBroadcast(Intent(EventNotifications.ACTION_FIRE).putExtra("at", due.toEpochMilli()))
                Shadows.shadowOf(Looper.getMainLooper()).idle()
                val shadow = Shadows.shadowOf(receiver)
                assertTrue(shadow.wentAsync())
                Shadows.shadowOf(shadow.originalPendingResult).future.get(5, TimeUnit.SECONDS)
                val posted = context.getSystemService(NotificationManager::class.java).activeNotifications.single().notification
                assertTrue(posted.extras.getCharSequence("android.bigText").toString().contains(event.title))
                val next = Shadows.shadowOf(context.getSystemService(AlarmManager::class.java)).scheduledAlarms.single()
                assertTrue(next.triggerAtMs > due.toEpochMilli())
            }
            EventNotifications.clearDisplayedAsync(context).get(5, TimeUnit.SECONDS)
            assertTrue(context.getSystemService(NotificationManager::class.java).activeNotifications.isEmpty())
        } finally { context.unregisterReceiver(receiver) }
    }
}
