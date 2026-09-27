// Copyright (c) 2026 RSG-KH | Apache-2.0 License
package com.rsgkh.calendar

import android.os.SystemClock
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.rsgkh.calendar.data.AppPreferences
import com.rsgkh.calendar.notifications.EventNotifications
import java.lang.ref.WeakReference
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Real ART collection check; complements deterministic lifecycle and cache unit tests. */
@RunWith(AndroidJUnit4::class)
class LifecycleDeviceTest {
    @Test fun recreatedAndClosedActivitiesAreReleased() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val preferences = AppPreferences(context)
        val original = preferences.read()
        val destroyed = mutableListOf<WeakReference<MainActivity>>()
        try {
            preferences.write(original.copy(notificationsEnabled = false))
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                repeat(8) {
                    scenario.onActivity { activity ->
                        destroyed.add(WeakReference(activity))
                        // Exercise repeated state changes and cancellation before recreation.
                        val settings = preferences.read()
                        activity.updateSettings(settings.copy(khmer = !settings.khmer))
                        activity.updateSettings(settings)
                    }
                    scenario.moveToState(Lifecycle.State.CREATED)
                    scenario.moveToState(Lifecycle.State.RESUMED)
                    scenario.recreate()
                }
                scenario.onActivity { destroyed.add(WeakReference(it)) }
            }
            EventNotifications.rescheduleAsync(context).get(10, TimeUnit.SECONDS)
            val deadline = SystemClock.uptimeMillis() + 10_000
            do {
                instrumentation.waitForIdleSync()
                Runtime.getRuntime().gc()
                System.runFinalization()
                SystemClock.sleep(100)
            } while (destroyed.any { it.get() != null } && SystemClock.uptimeMillis() < deadline)
            assertTrue("${destroyed.count { it.get() != null }} of ${destroyed.size} destroyed activities remain retained",
                destroyed.all { it.get() == null })
        } finally {
            preferences.write(original)
            EventNotifications.rescheduleAsync(context).get(10, TimeUnit.SECONDS)
        }
    }
}
