// Copyright (c) 2026 RSG-KH | Apache-2.0 License
package com.rsgkh.calendar

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay
import java.time.LocalDate

/** UI-only date refresh; reminders are handled independently by AlarmManager. */
internal suspend fun Lifecycle.refreshTodayWhileVisible(
    readToday: () -> LocalDate,
    update: (LocalDate) -> Unit,
) {
    repeatOnLifecycle(Lifecycle.State.STARTED) {
        while (true) {
            update(readToday())
            delay(30_000)
        }
    }
}
