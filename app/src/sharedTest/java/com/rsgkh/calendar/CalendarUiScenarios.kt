// Copyright (c) 2026 RSG-KH | Apache-2.0 License
package com.rsgkh.calendar

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import com.rsgkh.calendar.data.AppSettings
import com.rsgkh.calendar.data.ThemeMode
import com.rsgkh.calendar.data.CustomEvent
import com.rsgkh.calendar.data.EventRepository
import com.rsgkh.calendar.i18n.L
import com.rsgkh.calendar.ui.CalendarApp
import com.rsgkh.calendar.ui.NotificationAccess
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.time.LocalDate
import java.time.Instant
import java.time.ZoneId

abstract class CalendarUiScenarios {
    @get:Rule val compose = createComposeRule()
    protected fun start(settings: AppSettings = AppSettings(khmer = false, theme = ThemeMode.LIGHT), now: Instant? = null,
        notificationAccess: NotificationAccess = NotificationAccess(),
        customEvents: List<CustomEvent> = emptyList()) {
        val state = mutableStateOf(settings)
        val custom = mutableStateOf<List<CustomEvent>>(customEvents)
        compose.setContent { CalendarApp(state.value, now?.let { state.value.todayTimeZone.today(it, ZoneId.of("Europe/Brussels")) } ?: LocalDate.of(2026, 9, 10), customEvents = custom.value,
            notificationAccess = notificationAccess,
            onSaveCustom = { event -> custom.value = custom.value.filterNot { it.id == event.id } + event },
            onDeleteCustom = { id -> custom.value = custom.value.filterNot { it.id == id } }) { state.value = it } }
    }
    @OptIn(ExperimentalTestApi::class)
    protected fun screenshot(name: String) {
        compose.waitForIdle()
        val jvmDirectory = System.getProperty("calendar.screenshots")
        val bitmap = if (jvmDirectory != null) {
            lateinit var result: Bitmap
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                val activity = ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED).single()
                val view = android.view.inspector.WindowInspector.getGlobalWindowViews().lastOrNull { it.isShown } ?: activity.window.decorView
                result = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
                view.draw(Canvas(result))
            }
            result
        } else compose.onRoot().captureToImage().asAndroidBitmap()
        val directory = jvmDirectory?.let { File(it) } ?: InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir("screenshots")!!
        directory.mkdirs()
        File(directory, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
    private fun chooseTimeZone(label: String) {
        compose.onNodeWithTag("today-time-zone").performScrollTo().performClick()
        compose.onNode(hasText(label, substring = true) and hasAnyAncestor(isPopup())).performClick()
        compose.onNodeWithTag("today-time-zone").assertTextContains(label, substring = true)
    }
    @Test fun monthNavigationAndToday() {
        start()
        screenshot("calendar-light")
        compose.onNodeWithTag("month-grid").performTouchInput { swipeLeft() }
        compose.onNodeWithText("October").assertIsDisplayed()
        compose.onNode(hasText("Today") and hasClickAction()).performClick()
        compose.onNodeWithText("September").assertIsDisplayed()
        compose.onNodeWithText("BE 2570").assertIsDisplayed()
        compose.onNodeWithTag("month-grid").performTouchInput { swipeRight() }
        compose.onNodeWithText("August").assertIsDisplayed()
        compose.onNodeWithContentDescription("Next month").performClick()
        compose.onNodeWithText("September").assertIsDisplayed()
        compose.onNodeWithContentDescription("Previous month").performClick()
        compose.onNodeWithText("August").assertIsDisplayed()
    }
    @Test fun searchFiltersAndEventDetails() {
        start()
        compose.onNodeWithText("Events").performClick()
        compose.onNodeWithText("Holidays").performClick()
        compose.onNodeWithText("Search events").performTextInput("Constitution")
        compose.onNodeWithText("Constitution Day").assertIsDisplayed().performClick()
        compose.onNodeWithText("Delete").assertDoesNotExist()
        compose.onAllNodesWithText("Public holiday").onLast().assertIsDisplayed()
        compose.onNodeWithText("Close").performClick()
        screenshot("events-search")
        compose.onNodeWithContentDescription("Choose year").performClick()
        compose.onNodeWithTag("event-year-input").assertTextContains("2026")
        compose.onNodeWithText("Cancel").performClick()
        compose.onNodeWithTag("event-year").assertTextEquals("2026")
        compose.onNodeWithText("Constitution Day").assertIsDisplayed()
        compose.onNodeWithContentDescription("Choose year").performClick()
        compose.onNodeWithTag("event-year-input").performTextReplacement("2027")
        compose.onNodeWithText("Go").performClick()
        compose.onNodeWithTag("event-year").assertTextEquals("2027")
        compose.onNodeWithText("Search events").assertTextContains("Constitution")
        compose.onNodeWithText("Holidays").assertIsSelected()
        compose.onNodeWithText("No matching events. Try another filter or search.").assertIsDisplayed()
        compose.onNodeWithText("Observances").performClick()
        compose.onNodeWithText("Constitution Day").assertIsDisplayed()
    }
    @Test fun themeAndLanguageCanBeChanged() {
        start()
        compose.onNodeWithText("Settings").performClick()
        compose.onNodeWithTag("theme-mode").performClick()
        compose.onNode(hasText("Dark") and hasAnyAncestor(isPopup())).performClick()
        compose.onNodeWithTag("theme-mode").assertTextContains("Dark")
        screenshot("settings-dark")
        compose.onNodeWithText("ខ្មែរ").performScrollTo().performClick()
        compose.onNode(hasText("ប្រតិទិន") and hasClickAction()).performClick()
        compose.onNodeWithText(L.text("calendar.month.short.9", true)).assertIsDisplayed()
        screenshot("calendar-khmer-dark")
    }
    @Test fun dateGridOpensDetailsAndReturnsFromEventDetails() {
        start()
        compose.onNode(hasContentDescription("Thursday, 10 September", substring = true)).performClick()
        compose.onNodeWithText("Date details").assertIsDisplayed()
        compose.onNodeWithText("September 10, 2026").assertIsDisplayed()
        compose.onNodeWithText("Year of the Horse", substring = true).assertIsDisplayed()
        compose.onNodeWithText("Shaving Day · Eve of Buddhist Holy Day").assertIsDisplayed()
        compose.onNodeWithText("Close").performClick()
        compose.onNode(hasContentDescription("Thursday, 24 September", substring = true)).performClick()
        compose.onNode(hasText("Constitution Day") and hasAnyAncestor(isDialog())).performClick()
        compose.onNodeWithText("Listed in Cambodia’s official 2026 holiday calendar.").assertIsDisplayed()
        compose.onNodeWithText("Close").performClick()
        compose.onNodeWithText("Date details").assertIsDisplayed()
        compose.onNodeWithText("September 24, 2026").assertIsDisplayed()
        compose.onNodeWithText("Close").performClick()
        compose.onNode(hasContentDescription("Thursday, 24 September", substring = true)).assertIsSelected().performClick()
        compose.onNodeWithText("Date details").assertIsDisplayed()
    }
    @Test fun khmerDatePopupIncludesTheFullTraditionalDateAndDayStatus() {
        start(AppSettings(khmer = true, theme = ThemeMode.DARK))
        compose.onNode(hasContentDescription("១៣រោច", substring = true)).performClick()
        compose.onNodeWithText(L.text("ui.date_details.e26d78", true)).assertIsDisplayed()
        compose.onNodeWithText("ថ្ងៃព្រហស្បតិ៍ ១៣រោច ខែស្រាពណ៍ ឆ្នាំមមី អដ្ឋស័ក ពុទ្ធសករាជ ២៥៧០ ត្រូវនឹងថ្ងៃទី១០ ខែកញ្ញា ឆ្នាំ២០២៦").assertIsDisplayed()
        compose.onNodeWithText("ថ្ងៃកោរ").assertIsDisplayed()
        compose.onNodeWithText("September 10, 2026").assertIsDisplayed()
        compose.onNodeWithText("បិទ").performClick()
        compose.onNode(hasContentDescription("១៤រោច", substring = true)).performClick()
        compose.onNode(hasText("ថ្ងៃសីល") and hasAnyAncestor(isDialog())).assertIsDisplayed()
        compose.onNodeWithText("ថ្ងៃកោរ").assertDoesNotExist()
    }
    @Test fun pastAndFutureEventsUseTheWebsiteDatabaseWithSeparateOfficialAttribution() {
        start()
        compose.onNodeWithText("Events").performClick()
        compose.onNodeWithText("Search events").performTextInput("Royal Ploughing")
        compose.onNodeWithContentDescription("Previous year").performClick()
        compose.onNodeWithText("Royal Ploughing Ceremony").performClick()
        compose.onNodeWithText("Listed in Cambodia’s official 2025 holiday calendar.").assertIsDisplayed()
        compose.onNodeWithText("Official source · 2025 ↗").assertDoesNotExist()
        compose.onNodeWithText("Close").performClick()
        compose.onNodeWithContentDescription("Next year").performClick()
        compose.onNodeWithContentDescription("Next year").performClick()
        compose.onNodeWithText("Royal Ploughing Ceremony").performClick()
        compose.onNodeWithText("An event from Khmer Lunar Calendar, saved for offline viewing.").assertDoesNotExist()
        compose.onNodeWithText("Khmer Lunar Calendar ↗").assertDoesNotExist()
        compose.onNodeWithText("Official source", substring = true).assertDoesNotExist()
        compose.onNodeWithText("Close").performClick()
        compose.onNodeWithText("Search events").performTextReplacement("Khmer New Year")
        compose.onAllNodes(hasText("Khmer New Year", substring = true) and !hasSetTextAction()).onFirst().performClick()
        compose.onNodeWithText("An event from Khmer Lunar Calendar, saved for offline viewing.").assertDoesNotExist()
        compose.onNodeWithText("Close").performClick()
        screenshot("future-festival")
    }
    @Test fun databaseCoverageAppearsOnlyOutside2000Through2030() {
        start()
        for (year in listOf(2024, 2027, 2000, 2030, 1999, 2031)) {
            compose.onNode(hasText("Calendar") and hasClickAction()).performClick()
            compose.onNodeWithContentDescription("Choose month and year").performClick()
            compose.onNode(hasSetTextAction()).performTextReplacement(year.toString())
            compose.onNodeWithText("Jan").performClick()
            compose.onNodeWithText("Go").performClick()
            if (year in 2000..2030) {
                compose.onNodeWithTag("event-coverage-note").assertDoesNotExist()
                compose.onNodeWithText("New Year's Day").performScrollTo().assertIsDisplayed()
            } else compose.onNodeWithTag("event-coverage-note").performScrollTo().assertIsDisplayed()
        }
        compose.onNodeWithText("Events").performClick()
        for (year in listOf(2024, 2027, 2000, 2030, 2031, 1800, 2200)) {
            compose.onNodeWithContentDescription("Choose year").performClick()
            compose.onNodeWithTag("event-year-input").performTextReplacement(year.toString())
            compose.onNodeWithText("Go").performClick()
            compose.onNodeWithTag("event-year").assertTextEquals(year.toString())
            compose.onNodeWithTag("event-year-options").assertDoesNotExist()
            if (year == 1800) compose.onNodeWithContentDescription("Previous year").assertIsNotEnabled()
            if (year == 2200) compose.onNodeWithContentDescription("Next year").assertIsNotEnabled()
            if (year in 2000..2030) {
                compose.onNodeWithTag("event-coverage-note").assertDoesNotExist()
                compose.onNodeWithText("New Year's Day").performScrollTo().assertIsDisplayed()
            } else compose.onNodeWithTag("event-coverage-note").performScrollTo().assertIsDisplayed()
        }
    }

    @Test fun calculatedFutureEventsShowTheirProvenanceAndStayOutOfHolidayFilter() {
        start()
        compose.onNodeWithText(L.text("ui.events.11d867", false)).performClick()
        compose.onNodeWithContentDescription(L.text("ui.choose_year.0853a0", false)).performClick()
        compose.onNodeWithTag("event-year-input").performTextReplacement("2031")
        compose.onNodeWithText(L.text("ui.go.ba4f19", false)).performClick()
        val title = EventRepository.forDate(LocalDate.of(2031, 1, 1)).single { it.id == "calculated:new_year_day" }.titleEn
        compose.onNodeWithText(title).performScrollTo().performClick()
        compose.onNode(hasText(L.text("rules.calculated_label", false)) and hasAnyAncestor(isDialog())).assertIsDisplayed()
        compose.onNodeWithText(L.text("rules.calculated_details", false)).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(L.text("about.source_link", false)).assertDoesNotExist()
        screenshot("calculated-event-details")
        compose.onNodeWithText(L.text("ui.close.7df7dc", false)).performClick()
        compose.onNodeWithText(L.text("ui.holidays.8a894c", false)).performScrollTo().performClick()
        compose.onNodeWithText(L.text("ui.no_matching_events_try_another_filter_or_search.57812b", false)).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(L.text("ui.observances.e4454c", false)).performScrollTo().performClick()
        compose.onNodeWithText(title).performScrollTo().assertIsDisplayed()
        screenshot("calculated-events-2031")
    }
    @Test fun khmerSearchFindsImportedTitlesWithoutTheirDisplaySpacing() {
        start(AppSettings(khmer = true))
        compose.onNodeWithText("ព្រឹត្តិការណ៍").performClick()
        compose.onNode(hasSetTextAction()).performTextInput("សែនចូលឆ្នាំចិន")
        compose.onNode(hasText("សែន", substring = true) and !hasSetTextAction()).performClick()
        compose.onNodeWithText("Chinese New Year's Eve").performScrollTo().assertIsDisplayed()
    }
    @Test fun jumpSupportsBoundsWithoutCrashing() {
        start()
        compose.onNodeWithContentDescription("Choose month and year").performClick()
        compose.onNodeWithTag("month-picker").assert(hasAnyAncestor(isDialog()))
        compose.onNodeWithTag("month-grid").assertExists()
        compose.onNodeWithText("Year (1800–2200)").assertIsDisplayed()
        compose.onNodeWithText("1800–2200").assertDoesNotExist()
        compose.onNodeWithTag("month-year-input").performTextReplacement("2000")
        compose.onNodeWithText("Cancel").performClick()
        compose.onNodeWithTag("month-picker").assertDoesNotExist()
        compose.onNodeWithContentDescription("Choose month and year").performClick()
        compose.onNodeWithTag("month-year-input").assertTextContains("2026")
        screenshot("month-picker")
        compose.onNode(hasSetTextAction()).performTextReplacement("1799")
        compose.onNodeWithText("Go").assertIsNotEnabled()
        compose.onNode(hasSetTextAction()).performTextReplacement("2200")
        compose.onNodeWithText("Dec").performClick()
        compose.onNodeWithText("Go").performClick()
        compose.onNodeWithText("2200").assertIsDisplayed()
        compose.onNodeWithText("December").assertIsDisplayed()
        compose.onNodeWithTag("month-grid").performTouchInput { swipeLeft() }
        compose.onNodeWithText("December").assertIsDisplayed()
        compose.onNodeWithContentDescription("Next month").assertIsNotEnabled()
    }
    @Test fun customEventsCanBeAddedInThePastEditedToTheFutureAndDeleted() {
        start()
        compose.onNodeWithText("Events").performClick()
        compose.onNode(hasText("Custom") and hasClickAction()).assertIsDisplayed().performClick()
        compose.onNodeWithText("No matching events. Try another filter or search.").assertIsDisplayed()
        compose.onNodeWithContentDescription("Add event").performClick()
        compose.onNodeWithTag("custom-title").performTextInput("Archive note")
        compose.onNodeWithTag("custom-date").performTextReplacement("1999-02-30")
        compose.onNodeWithText("Save").assertIsNotEnabled()
        compose.onNodeWithTag("custom-date").performTextReplacement("1999-01-02")
        compose.onNodeWithTag("custom-time").performTextReplacement("00:30")
        compose.onNodeWithText("Save").performScrollTo().performClick()
        compose.onNodeWithText("1999").assertIsDisplayed()
        compose.onNodeWithText("Archive note").performScrollTo().performClick()
        compose.onNodeWithText("00:30 · Local time").assertIsDisplayed()
        compose.onNodeWithText("Delete").assertIsDisplayed()
        compose.onNodeWithText("Edit").performClick()
        compose.onNodeWithText("Delete event").assertDoesNotExist()
        compose.onNodeWithText("Delete").assertDoesNotExist()
        compose.onNodeWithTag("custom-date").performTextReplacement("2099-12-31")
        compose.onNodeWithTag("custom-time").performTextReplacement("23:45")
        compose.onNodeWithText("Save").performScrollTo().performClick()
        compose.onNodeWithText("2099").assertIsDisplayed()
        compose.onNode(hasText("Custom") and hasClickAction()).assertIsDisplayed().assertIsSelected()
        screenshot("custom-events")
        compose.onNodeWithText("Archive note").performClick()
        compose.onNodeWithText("Delete").performClick()
        compose.onNodeWithText("Delete this event?").assertIsDisplayed()
        compose.onNodeWithText("Cancel").performClick()
        compose.onNodeWithText("23:45 · Local time").assertIsDisplayed()
        compose.onNodeWithText("Delete").performClick()
        compose.onNodeWithText("Delete").performClick()
        compose.onNodeWithText("Archive note").assertDoesNotExist()
        compose.onNodeWithText("No matching events. Try another filter or search.").assertIsDisplayed()
    }
    @Test fun customEventEditorFollowsTheSelectedTimeZoneInEnglishAndKhmer() {
        val originalZone = java.util.TimeZone.getDefault()
        java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("Europe/Brussels"))
        try {
            start()
            compose.onNodeWithText("Events").performClick()
            compose.onNodeWithContentDescription("Add event").performClick()
            compose.onNodeWithTag("custom-time-zone-description").assertTextContains("Date and time follow Local time.", substring = true)
            compose.onNodeWithTag("custom-title").performTextInput("Zone check")
            compose.onNodeWithTag("custom-date").performTextReplacement("2026-03-29")
            compose.onNodeWithTag("custom-time").performTextReplacement("02:30")
            compose.onNodeWithText("Save").performScrollTo().assertIsNotEnabled()
            compose.onNodeWithTag("custom-date").performScrollTo().performTextReplacement("2026-12-31")
            compose.onNodeWithTag("custom-time").performTextReplacement("23:30")
            compose.onNodeWithText("Save").performScrollTo().performClick()
            compose.onNodeWithText("Zone check").performClick()
            compose.onNodeWithText("23:30 · Local time").assertIsDisplayed()
            compose.onNodeWithText("Close").performClick()
            compose.onNodeWithText("Settings").performClick()
            chooseTimeZone("Cambodia (UTC+7)")
            compose.onNodeWithText("Events").performClick()
            compose.onNodeWithContentDescription("Choose year").performClick()
            compose.onNodeWithTag("event-year-input").performTextReplacement("2027")
            compose.onNodeWithText("Go").performClick()
            compose.onNodeWithText("Zone check").performClick()
            compose.onNodeWithText("05:30 · Cambodia time (UTC+7)").assertIsDisplayed()
            compose.onNodeWithText("Edit").performClick()
            compose.onNodeWithTag("custom-date").assertTextContains("2027-01-01")
            compose.onNodeWithTag("custom-time").assertTextContains("05:30")
            compose.onNodeWithTag("custom-time-zone-description").assertTextContains("Date and time follow Cambodia time (UTC+7).", substring = true)
            screenshot("custom-editor-cambodia")
            compose.onNodeWithTag("custom-time").performTextReplacement("06:45")
            compose.onNodeWithText("Save").performScrollTo().performClick()
            compose.onNodeWithText("Settings").performClick()
            chooseTimeZone("Local time")
            compose.onNodeWithText("ខ្មែរ").performScrollTo().performClick()
            compose.onNodeWithText("ព្រឹត្តិការណ៍").performClick()
            compose.onNodeWithText("Zone check").performClick()
            compose.onNodeWithText("00:45 · ម៉ោងក្នុងតំបន់").assertIsDisplayed()
            compose.onNodeWithText("កែប្រែ").performClick()
            compose.onNodeWithTag("custom-date").assertTextContains("2027-01-01")
            compose.onNodeWithTag("custom-time").assertTextContains("00:45")
            compose.onNodeWithTag("custom-time-zone-description").assertTextContains("កាលបរិច្ឆេទ និងម៉ោងគិតតាម ម៉ោងក្នុងតំបន់។", substring = true)
            screenshot("custom-editor-local-khmer")
            compose.onNodeWithText("បោះបង់").performScrollTo().performClick()
            compose.onNodeWithText("ការកំណត់").performClick()
            chooseTimeZone("កម្ពុជា (UTC+7)")
            compose.onNodeWithText("ព្រឹត្តិការណ៍").performClick()
            compose.onNodeWithContentDescription("បន្ថែមព្រឹត្តិការណ៍").performClick()
            compose.onNodeWithTag("custom-time-zone-description").assertTextContains("កាលបរិច្ឆេទ និងម៉ោងគិតតាម ម៉ោងកម្ពុជា (UTC+7)។", substring = true)
            compose.onNodeWithTag("custom-time").onChildren().filter(hasClickAction()).onFirst().performClick()
            compose.onNodeWithText(L.text("common.clock_label", true, "zone" to L.text("ui.cambodia_time_utc_7.6b9f2d", true))).assertIsDisplayed()
        } finally { java.util.TimeZone.setDefault(originalZone) }
    }
    @Test fun notificationControlsSupportTimeAndAllRepeatChoices() {
        start()
        compose.onNodeWithText("Settings").performClick()
        compose.onNodeWithTag("settings-scroll").performScrollToNode(hasText("Enable event notifications"))
        compose.onNodeWithTag("choose-push-time").assertDoesNotExist()
        compose.onNodeWithTag("reminder-interval").assertDoesNotExist()
        screenshot("notification-settings-disabled")
        compose.onNodeWithContentDescription("Enable event notifications").performClick().assertIsOn()
        compose.onNodeWithTag("choose-push-time").performScrollTo().assertIsEnabled().performClick()
        compose.onNodeWithTag("wheel-hour").performScrollToNode(hasText("06"))
        compose.onNode(hasText("06") and hasAnyAncestor(hasTestTag("wheel-hour"))).performClick()
        compose.onNodeWithTag("wheel-minute").performScrollToNode(hasText("15"))
        compose.onNode(hasText("15") and hasAnyAncestor(hasTestTag("wheel-minute"))).performClick()
        screenshot("time-selection-dialog")
        compose.onNodeWithText("OK").performClick()
        compose.onNodeWithText("06:15").assertIsDisplayed()
        for (hours in listOf(2, 6, 8, 4)) {
            compose.onNodeWithTag("reminder-interval").performScrollTo().performClick()
            compose.onNodeWithText("$hours hours").performClick()
        }
        compose.onNodeWithContentDescription("Enable event notifications").performScrollTo().performClick().assertIsOff()
        compose.onNodeWithTag("choose-push-time").assertDoesNotExist()
        compose.onNodeWithTag("reminder-interval").assertDoesNotExist()
        compose.onNodeWithContentDescription("Enable event notifications").performScrollTo().performClick().assertIsOn()
        compose.onNodeWithTag("choose-push-time").assertIsEnabled().assertTextContains("06:15")
        compose.onNodeWithTag("reminder-interval").assertIsEnabled().assertTextContains("4 hours")
        compose.onNodeWithTag("reminder-interval").performScrollTo().performClick()
        compose.onNodeWithText("Off").performClick()
        compose.onNodeWithText("Off").assertIsDisplayed()
        screenshot("notification-settings")
        compose.onNodeWithContentDescription("Enable event notifications").performScrollTo().performClick().assertIsOff()
        compose.onNodeWithTag("choose-push-time").assertDoesNotExist()
        compose.onNodeWithTag("reminder-interval").assertDoesNotExist()
    }
    @Test fun todayTimeZoneSwitchesTheDateAndKhmerSettingsUseUpdatedLabels() {
        start(now = Instant.parse("2026-09-09T18:00:00Z"))
        compose.onNodeWithText("Settings").performClick()
        compose.onNodeWithTag("accent-color").performScrollTo().performClick()
        compose.onNode(hasText("Lime") and hasAnyAncestor(isPopup())).performClick()
        compose.onNodeWithTag("accent-color").assertTextContains("Lime")
        screenshot("settings-lime-light")
        chooseTimeZone("Local time")
        compose.onNodeWithText("Calendar").performClick()
        compose.onNode(hasText("Today") and hasClickAction()).performClick()
        compose.onNode(hasContentDescription("Wednesday, 9 September", substring = true)).assertIsSelected()
        compose.onNodeWithText("Settings").performClick()
        chooseTimeZone("Cambodia (UTC+7)")
        compose.onNodeWithText("Calendar").performClick()
        compose.onNode(hasText("Today") and hasClickAction()).performClick()
        compose.onNode(hasContentDescription("Thursday, 10 September", substring = true)).assertIsSelected()
        compose.onNodeWithText("Settings").performClick()
        compose.onNodeWithTag("theme-mode").performClick()
        compose.onNode(hasText("Dark") and hasAnyAncestor(isPopup())).performClick()
        compose.onNodeWithTag("theme-mode").assertTextContains("Dark")
        compose.onNodeWithText("ខ្មែរ").performScrollTo().performClick()
        compose.onNodeWithTag("accent-color").performScrollTo().assertTextContains(L.text("ui.lime.46ea65", true))
        compose.onNodeWithText(L.text("ui.start_week_on_monday.5578c3", true)).assertIsDisplayed()
        compose.onNodeWithText("បិទដើម្បីផ្តើមសប្តាហ៍ពីថ្ងៃអាទិត្យ").assertIsDisplayed()
        compose.onNodeWithTag("today-time-zone").performScrollTo().assertTextContains("កម្ពុជា (UTC+7)")
        compose.onNodeWithTag("settings-scroll").performScrollToNode(hasText(L.text("ui.enable_event_notifications.b4d4f8", true)))
        compose.onNodeWithText(L.text("ui.enable_event_notifications.b4d4f8", true)).assertIsDisplayed()
        compose.onNodeWithTag("reminder-interval").assertDoesNotExist()
        screenshot("settings-lime-khmer-dark")
        compose.onNodeWithContentDescription(L.text("ui.enable_event_notifications.b4d4f8", true)).performClick()
        compose.onNodeWithTag("choose-push-time").performScrollTo().performClick()
        screenshot("time-selection-dialog-khmer")
        compose.onNodeWithText(L.text("ui.cancel.5bf834", true)).performClick()
    }
}
