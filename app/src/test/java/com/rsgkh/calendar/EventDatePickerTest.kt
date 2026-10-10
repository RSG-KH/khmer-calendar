// Copyright (c) 2026 RSG-KH | Apache-2.0 License
@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.rsgkh.calendar

import androidx.compose.material3.DatePickerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.rsgkh.calendar.ui.EventDatePicker
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.annotation.LooperMode
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [32], qualifiers = "w360dp-h800dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@LooperMode(LooperMode.Mode.PAUSED)
class EventDatePickerTest {
    @get:Rule val compose = createComposeRule()

    private fun picker(date: String): DatePickerState {
        val state = DatePickerState(Locale.forLanguageTag("km-KH-u-nu-latn"),
            initialSelectedDateMillis = millis(date), yearRange = 1800..2200)
        compose.setContent { MaterialTheme { EventDatePicker(state, khmer = true) } }
        return state
    }

    @Test fun monthSwipeAndYearSelectionReachLeapDayWithoutChangingSelectionEarly() {
        val state = picker("2026-01-12")
        compose.onNodeWithTag("picker-next").performClick()
        compose.onNodeWithTag("picker-day-2026-02-28").assertIsDisplayed()
        compose.onNodeWithTag("picker-day-2026-02-29").assertDoesNotExist()
        compose.onNodeWithTag("picker-year-toggle").performClick()
        compose.onNodeWithTag("picker-years").performScrollToNode(hasText("2028"))
        compose.onNodeWithText("2028").performClick()
        compose.onNodeWithTag("picker-day-2028-02-29").assertIsDisplayed()
        compose.onNodeWithTag("picker-months").performTouchInput { swipeLeft() }
        compose.onNodeWithTag("picker-day-2028-03-31").assertIsDisplayed()
        compose.onNodeWithTag("picker-previous").performClick()
        compose.runOnIdle { assertEquals(millis("2026-01-12"), state.selectedDateMillis) }
        compose.onNodeWithTag("picker-day-2028-02-29").performClick().assertIsSelected()
        compose.runOnIdle { assertEquals(millis("2028-02-29"), state.selectedDateMillis) }
    }

    @Test fun firstSupportedMonthStopsBackwardNavigation() {
        picker("1800-01-01")
        compose.onNodeWithTag("picker-previous").assertIsNotEnabled()
        compose.onNodeWithTag("picker-next").performClick()
        compose.onNodeWithTag("picker-previous").assertIsEnabled().performClick()
        compose.onNodeWithTag("picker-day-1800-01-01").assertIsSelected()
        compose.onNodeWithTag("picker-previous").assertIsNotEnabled()
    }

    @Test fun lastSupportedMonthStopsForwardNavigation() {
        picker("2200-12-31")
        compose.onNodeWithTag("picker-next").assertIsNotEnabled()
        compose.onNodeWithTag("picker-previous").performClick()
        compose.onNodeWithTag("picker-next").assertIsEnabled().performClick()
        compose.onNodeWithTag("picker-day-2200-12-31").assertIsSelected()
        compose.onNodeWithTag("picker-next").assertIsNotEnabled()
    }

    private fun millis(date: String) = LocalDate.parse(date).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
}
