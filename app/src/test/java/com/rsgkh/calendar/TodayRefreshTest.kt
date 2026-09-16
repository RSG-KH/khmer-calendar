// Copyright (c) 2026 RSG-KH | Apache-2.0 License
package com.rsgkh.calendar

import android.os.Looper
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import com.rsgkh.calendar.data.TodayTimeZone
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [32])
class TodayRefreshTest {
    private class Owner : LifecycleOwner {
        override val lifecycle = LifecycleRegistry(this)
    }

    @Test fun hiddenActivityDoesNoPollingAndReturningRefreshesImmediately() {
        val owner = Owner()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        val looper = Shadows.shadowOf(Looper.getMainLooper())
        var currentDate = LocalDate.of(2026, 12, 31)
        var reads = 0
        val dates = mutableListOf<LocalDate>()
        owner.lifecycle.currentState = Lifecycle.State.CREATED
        try {
            val job = scope.launch {
                owner.lifecycle.refreshTodayWhileVisible({ reads++; currentDate }, dates::add)
            }
            looper.idleFor(Duration.ofMinutes(2))
            assertEquals(0, reads)

            owner.lifecycle.currentState = Lifecycle.State.RESUMED
            looper.idle()
            assertEquals(listOf(currentDate), dates)
            looper.idleFor(Duration.ofSeconds(29))
            assertEquals(1, reads)

            // Paused but still visible (for example, multi-window) keeps refreshing.
            owner.lifecycle.currentState = Lifecycle.State.STARTED
            currentDate = currentDate.plusDays(1)
            looper.idleFor(Duration.ofSeconds(1))
            assertEquals(currentDate, dates.last())
            assertEquals(2, reads)

            owner.lifecycle.currentState = Lifecycle.State.CREATED
            currentDate = currentDate.minusYears(1)
            looper.idleFor(Duration.ofHours(3))
            assertEquals(2, reads)

            owner.lifecycle.currentState = Lifecycle.State.STARTED
            looper.idle()
            assertEquals(currentDate, dates.last())
            assertEquals(3, reads)
            looper.idleFor(Duration.ofSeconds(30))
            assertEquals(4, reads) // Only one polling loop restarts.

            owner.lifecycle.currentState = Lifecycle.State.DESTROYED
            looper.idleFor(Duration.ofMinutes(1))
            assertEquals(4, reads)
            assertTrue(job.isCompleted)
        } finally { scope.cancel() }
    }

    @Test fun visibleActivityPicksUpMidnightClockJumpsAndDeviceZoneChanges() {
        val owner = Owner()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        val looper = Shadows.shadowOf(Looper.getMainLooper())
        var now = Instant.parse("2026-12-31T23:59:50Z")
        var zone = ZoneId.of("UTC")
        var displayed: LocalDate? = null
        owner.lifecycle.currentState = Lifecycle.State.STARTED
        try {
            scope.launch {
                owner.lifecycle.refreshTodayWhileVisible({ TodayTimeZone.LOCAL.today(now, zone) }) { displayed = it }
            }
            looper.idle()
            assertEquals(LocalDate.of(2026, 12, 31), displayed)

            now = now.plusSeconds(30)
            looper.idleFor(Duration.ofSeconds(30))
            assertEquals(LocalDate.of(2027, 1, 1), displayed)

            now = Instant.parse("2028-06-15T01:00:00Z")
            looper.idleFor(Duration.ofSeconds(30))
            assertEquals(LocalDate.of(2028, 6, 15), displayed)

            now = Instant.parse("2026-09-16T01:00:00Z")
            looper.idleFor(Duration.ofSeconds(30))
            assertEquals(LocalDate.of(2026, 9, 16), displayed)

            zone = ZoneId.of("America/Los_Angeles")
            looper.idleFor(Duration.ofSeconds(30))
            assertEquals(LocalDate.of(2026, 9, 15), displayed)
        } finally { scope.cancel() }
    }
}
