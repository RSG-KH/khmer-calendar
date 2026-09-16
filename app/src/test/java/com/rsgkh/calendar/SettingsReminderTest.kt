// Copyright (c) 2026 RSG-KH | Apache-2.0 License
package com.rsgkh.calendar

import android.app.AlarmManager
import android.app.NotificationManager
import com.rsgkh.calendar.data.*
import com.rsgkh.calendar.i18n.L
import com.rsgkh.calendar.notifications.EventNotifications
import com.rsgkh.calendar.notifications.ReminderWork
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [32])
class SettingsReminderTest {
    private val enabled = AppSettings(notificationsEnabled = true, showHolyDaysInEvents = true, pushHolyDays = true)

    @Test fun appearanceChangesPersistWithoutReplacingTheScheduledAlarm() {
        Robolectric.buildActivity(MainActivity::class.java).create().use { controller ->
            val activity = controller.get()
            val preferences = AppPreferences(activity)
            preferences.write(enabled)
            EventNotifications.reschedule(activity)
            val alarms = shadowOf(activity.getSystemService(AlarmManager::class.java))
            val original = alarms.scheduledAlarms.single()
            val choices = listOf(
                enabled, // Saving the same settings is also a no-op for reminders.
                enabled.copy(theme = ThemeMode.DARK),
                enabled.copy(accent = Accent.LIME),
                enabled.copy(backgroundAccent = false),
                enabled.copy(khmer = false),
                enabled.copy(fontScale = FontScale.PERCENT_120),
                enabled.copy(mondayFirst = true),
                enabled.copy(showLongerWeekdayNames = true),
                enabled.copy(showCopyButtons = true),
                enabled.copy(highlightWeekdayNames = false),
                enabled.copy(showLunar = false),
                enabled.copy(showHolyDaysInCalendar = false),
                enabled.copy(highlightSunday = false),
            )
            for (next in choices) {
                activity.updateSettings(next)
                ReminderWork.submit(activity) {}.get(5, TimeUnit.SECONDS)
                assertEquals(next, preferences.read())
                assertSame("Appearance change replaced the alarm: $next", original, alarms.scheduledAlarms.single())
                val channel = activity.getSystemService(NotificationManager::class.java)
                    .getNotificationChannel(EventNotifications.CHANNEL)
                assertEquals(L.text("notifications.channel", next.khmer), channel.name.toString())
            }
        }
    }

    @Test fun everyReminderControlStillUpdatesTheAlarm() {
        Robolectric.buildActivity(MainActivity::class.java).create().use { controller ->
            val activity = controller.get()
            val preferences = AppPreferences(activity)
            val alarms = shadowOf(activity.getSystemService(AlarmManager::class.java))
            val choices = listOf(
                enabled.copy(notificationsEnabled = false),
                enabled.copy(pushCustomEvents = false),
                enabled.copy(pushHolidays = false),
                enabled.copy(pushObservances = false),
                enabled.copy(pushHolyDays = false),
                enabled.copy(showHolyDaysInEvents = false),
                enabled.copy(pushMinutes = 600),
                enabled.copy(repeatHours = 4),
                enabled.copy(todayTimeZone = TodayTimeZone.CAMBODIA),
            )
            for (next in choices) {
                preferences.write(enabled)
                EventNotifications.reschedule(activity)
                val original = alarms.scheduledAlarms.single()
                activity.updateSettings(next)
                ReminderWork.submit(activity) {}.get(5, TimeUnit.SECONDS)
                assertEquals(next, preferences.read())
                if (next.notificationsEnabled) {
                    assertNotSame("Reminder change kept the old alarm: $next", original, alarms.scheduledAlarms.single())
                } else {
                    assertTrue(alarms.scheduledAlarms.isEmpty())
                }
            }
        }
    }
}
