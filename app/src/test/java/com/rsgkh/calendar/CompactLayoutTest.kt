// Copyright (c) 2026 RSG-KH | Apache-2.0 License
package com.rsgkh.calendar

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.inspector.WindowInspector
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.test.platform.app.InstrumentationRegistry
import com.rsgkh.calendar.data.*
import com.rsgkh.calendar.i18n.L
import com.rsgkh.calendar.ui.CalendarApp
import com.rsgkh.calendar.ui.NotificationAccess
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.annotation.LooperMode
import java.io.File
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [32], qualifiers = "w320dp-h640dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@LooperMode(LooperMode.Mode.PAUSED)
class CompactLayoutTest {
    @get:Rule val compose = createComposeRule()
    private val access = mutableStateOf(NotificationAccess())
    private fun start(khmer: Boolean = false, showPreciseReminderPrompt: Boolean = false) {
        val settings = mutableStateOf(AppSettings(khmer = khmer, theme = ThemeMode.LIGHT, notificationsEnabled = showPreciseReminderPrompt, repeatHours = if (showPreciseReminderPrompt) 4 else 0))
        access.value = NotificationAccess(exact = !showPreciseReminderPrompt)
        compose.setContent { CalendarApp(settings.value, LocalDate.of(2026, 9, 10),
            notificationAccess = access.value,
            onAllowExact = { access.value = NotificationAccess(exact = true) }) { settings.value = it } }
    }
    private fun screenshot(name: String): Bitmap {
        compose.waitForIdle()
        lateinit var image: Bitmap
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val view = WindowInspector.getGlobalWindowViews().last { it.isShown }
            image = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            view.draw(Canvas(image))
        }
        val folder = File(checkNotNull(System.getProperty("calendar.screenshots")))
        folder.mkdirs()
        File(folder, "compact-$name.png").outputStream().use { image.compress(Bitmap.CompressFormat.PNG, 100, it) }
        return image
    }
    private fun assertTitle(key: String, k: Boolean) {
        val results = mutableListOf<TextLayoutResult>()
        compose.onNodeWithTag("settings-scroll").performScrollToNode(hasText(L.text(key, k)))
        compose.onNodeWithText(L.text(key, k))
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(results) }
        assertEquals(15.75f, results.single().layoutInput.style.fontSize.value, .01f)
        val layout = results.single()
        // Paragraph constraints can exceed the final wrap-content width; inspect the actual lines.
        repeat(layout.lineCount) { line ->
            assertTrue("Clipped title: $key", layout.getLineRight(line) <= layout.size.width + 1f)
            assertFalse("Ellipsized title: $key", layout.isLineEllipsized(line))
        }
        assertFalse("Clipped title height: $key", layout.didOverflowHeight)
    }
    @Test fun narrowCalendarHeaderFitsAtLargestPhoneFontSize() {
        start()
        compose.onNodeWithText("Settings").performClick()
        compose.onNodeWithTag("font-scale").performClick()
        compose.onNode(hasText("120%") and hasAnyAncestor(isPopup())).performClick()
        compose.onNode(hasText("Calendar") and hasClickAction()).performClick()
        screenshot("calendar-header-120")
        for (label in listOf("2026", "BE 2570", "Sep")) {
            val layouts = mutableListOf<TextLayoutResult>()
            compose.onNode(hasText(label) and hasAnyAncestor(hasTestTag("calendar-header")))
                .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
            val layout = layouts.single()
            assertEquals(1, layout.lineCount)
            val lineWidth = layout.getLineRight(0) - layout.getLineLeft(0)
            assertTrue("Clipped header: $label", lineWidth <= layout.size.width + 1f)
            assertFalse("Ellipsized header: $label", layout.isLineEllipsized(0))
        }
    }
    @Test fun headersAndAddButtonStayFixedWhileTheirListsScroll() {
        start()
        compose.onNode(hasText("Sep") and hasAnyAncestor(hasTestTag("calendar-header"))).assertIsDisplayed()
        val calendarHeader = compose.onNodeWithTag("calendar-header").getUnclippedBoundsInRoot()
        compose.onNodeWithTag("calendar-scroll").performScrollToIndex(3)
        assertEquals(calendarHeader, compose.onNodeWithTag("calendar-header").getUnclippedBoundsInRoot())
        compose.onNodeWithContentDescription("Choose month and year").assertIsDisplayed()
        screenshot("calendar-scrolled")
        compose.onNodeWithText("Events").performClick()
        val header = compose.onNodeWithTag("events-header").getUnclippedBoundsInRoot()
        val add = compose.onNodeWithTag("add-event").getUnclippedBoundsInRoot()
        val content = compose.onNodeWithTag("events-content").getUnclippedBoundsInRoot()
        assertEquals(32f, (content.right - add.right).value, .5f)
        assertEquals(32f, (content.bottom - add.bottom).value, .5f)
        assertTrue(add.top > header.bottom)
        val nextYear = compose.onNodeWithContentDescription("Next year").getUnclippedBoundsInRoot()
        assertEquals(10f, (header.right - nextYear.right).value, .5f)
        val navigation = compose.onNodeWithTag("bottom-navigation").getUnclippedBoundsInRoot()
        assertEquals(64f, (navigation.bottom - navigation.top).value, .5f)
        val title = compose.onNode(hasText("Events") and hasAnyAncestor(hasTestTag("events-header"))).getUnclippedBoundsInRoot()
        val year = compose.onNodeWithTag("event-year").getUnclippedBoundsInRoot()
        assertEquals((title.top + title.bottom).value / 2, (year.top + year.bottom).value / 2, 1f)
        assertTrue(title.right <= year.left)
        screenshot("events")
        compose.onNodeWithTag("events-scroll").performScrollToIndex(25)
        assertEquals(header, compose.onNodeWithTag("events-header").getUnclippedBoundsInRoot())
        assertEquals(add, compose.onNodeWithTag("add-event").getUnclippedBoundsInRoot())
        compose.onNodeWithTag("event-year").performClick()
        compose.onNodeWithTag("event-year-options").assertIsDisplayed().assert(hasAnyAncestor(isDialog()))
        compose.onNodeWithTag("events-scroll").assertExists()
        compose.onNodeWithText("Year (1800–2200)").assertIsDisplayed()
        compose.onNodeWithText("1800–2200").assertDoesNotExist()
        screenshot("event-year-picker-en")
        compose.onNodeWithTag("event-year-input").performTextReplacement("1799")
        compose.onNodeWithText("Go").assertIsNotEnabled()
        compose.onNodeWithText("Cancel").performClick()
        compose.onNodeWithTag("event-year").assertTextContains("2026")
        compose.onNodeWithText("Settings").performClick()
        compose.onNodeWithText("ខ្មែរ").performScrollTo().performClick()
        compose.onNodeWithText("ព្រឹត្តិការណ៍").performClick()
        compose.onNodeWithTag("event-year").performClick()
        compose.onNodeWithTag("event-year-options").assertIsDisplayed().assert(hasAnyAncestor(isDialog()))
        compose.onNodeWithText("${L.text("ui.year.61d597", true)} (1800–2200)").assertIsDisplayed()
        screenshot("event-year-picker-km")
        compose.onNodeWithText(L.text("ui.cancel.5bf834", true)).performClick()
        compose.onNodeWithTag("add-event").performClick()
        compose.onNodeWithTag("custom-title").assertIsDisplayed()
    }
    @Test fun settingsFitKhmerAndEnglishWithConsistentTitles() {
        start(khmer = true, showPreciseReminderPrompt = true)
        compose.onNodeWithText(L.text("ui.settings.0e0a4f", true)).performClick()
        for (k in listOf(true, false)) {
            assertTitle("ui.language.b03320", k)
            assertTitle("ui.theme.99ca72", k)
            assertTitle("ui.accent_color.97e2af", k)
            screenshot("settings-appearance-$k")
            compose.onNodeWithTag("accent-color").performClick()
            screenshot("settings-accent-dropdown-$k")
            compose.onNode(hasText(L.text("ui.blue.cf6f1f", k)) and hasAnyAncestor(isPopup())).performClick()
            for (key in listOf("ui.today_follows.b52168", "ui.start_week_on_monday.5578c3", "ui.show_longer_weekday_names", "ui.highlight_sunday_column.549462", "ui.lunar_dates_in_calendar.4dffed", "ui.buddhist_holy_days_in_calendar.d1e9b6", "ui.buddhist_holy_days_in_events.53e502")) assertTitle(key, k)
            compose.onNodeWithTag("settings-scroll").performScrollToNode(hasTestTag("today-time-zone"))
            compose.onNodeWithTag("today-time-zone").performClick()
            compose.onNode(hasText(L.text("ui.cambodia_utc_7.458037", k)) and hasAnyAncestor(isPopup())).performClick()
            compose.onNodeWithTag("today-time-zone").assertTextContains(L.text("ui.cambodia_utc_7.458037", k))
            screenshot("settings-calendar-$k")
            compose.onNodeWithTag("today-time-zone").performClick()
            screenshot("settings-time-zone-$k")
            compose.onNode(hasText(L.text("ui.local_time.541b44", k), substring = true) and hasAnyAncestor(isPopup())).performClick()
            assertTitle("ui.enable_event_notifications.b4d4f8", k)
            compose.onNodeWithTag("settings-scroll").performScrollToNode(hasText(L.text("ui.allow_precise_reminders.733d9d", k)))
            screenshot("settings-precise-reminders-$k")
            compose.onNodeWithText(L.text("ui.allow_precise_reminders.733d9d", k)).performClick()
            for (key in listOf("ui.push_time.8421c3", "ui.remind_every.a38a5d")) assertTitle(key, k)
            screenshot("settings-notifications-$k")
            if (k) {
                access.value = NotificationAccess(exact = false)
                compose.onNodeWithTag("settings-scroll").performScrollToNode(hasText("English"))
                compose.onNodeWithText("English").performClick()
            }
        }
    }
    @Test fun lightDateAndEventPopupsUseNeutralSurfacesAndHaveNoSourceLinks() {
        start(khmer = true)
        compose.onNode(hasContentDescription("១៣រោច", substring = true)).performClick()
        compose.onNodeWithText(L.text("ui.date_details.e26d78", true)).assertIsDisplayed()
        val image = screenshot("date-details-light")
        // Clear space near the dialog's top center, away from text and rounded corners.
        assertEquals(android.graphics.Color.WHITE, image.getPixel(image.width / 2, 16))
        compose.onNodeWithText(L.text("ui.close.7df7dc", true)).performClick()
        compose.onNodeWithText(L.text("ui.events.11d867", true)).performClick()
        compose.onNodeWithText(L.text("ui.search_events.08c608", true)).performTextInput("Constitution")
        compose.onNodeWithText(EventRepository.forDate(LocalDate.of(2026, 9, 24)).single { it.kind == EventKind.HOLIDAY }.titleKm).performClick()
        compose.onNodeWithText(L.text("about.source_link", true)).assertDoesNotExist()
        compose.onNodeWithText(L.text("ui.an_event_from_khmer_lunar_calendar_saved_for_offline_vi.98ee90", true)).assertDoesNotExist()
        screenshot("event-details-light")
    }
}
