// Copyright (c) 2026 RSG-KH | Apache-2.0 License
package com.rsgkh.calendar

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
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

    @Test fun recurringMonthlyPreviewSaveEditAndDeleteUseTheOriginalSeries() {
        start()
        compose.onNodeWithText("Events").performClick()
        compose.onNodeWithContentDescription("Add event").performClick()
        compose.onNodeWithTag("custom-title").performTextInput("Monthly series")
        compose.onNodeWithTag("custom-date").performTextReplacement("2026-01-31")
        compose.onNodeWithTag("repeat-preview").assertDoesNotExist()
        compose.onNodeWithTag("repeat-monthly").performScrollTo().performSemanticsAction(SemanticsActions.OnClick)
        compose.onNodeWithText("Save").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithTag("repeat-end").performScrollTo().performTextReplacement("2026-12-31")
        compose.onNodeWithTag("repeat-count").assertTextContains("7 occurrences · Last: Dec 31, 2026")
        compose.onNodeWithContentDescription("Include day 30 in 30-day months").performScrollTo().performClick()
        compose.onNodeWithTag("repeat-count").assertTextContains("11 occurrences · Last: Dec 31, 2026")
        compose.onNodeWithContentDescription("Include day 28 or 29 of February").performScrollTo().performClick()
        compose.onNodeWithTag("repeat-count").assertTextContains("12 occurrences · Last: Dec 31, 2026")
        compose.onNodeWithTag("repeat-count").performScrollTo()
        screenshot("repeat-monthly-preview")
        compose.onNodeWithText("Save").performScrollTo().performSemanticsAction(SemanticsActions.OnClick)
        compose.onNodeWithText("Search events").performTextInput("2026-09-30")
        compose.onNodeWithText("Monthly series").performClick()
        screenshot("repeat-series-details")
        compose.onNodeWithText("Edit series").performClick()
        compose.onNodeWithTag("custom-date").assertTextContains("2026-01-31")
        compose.onNodeWithTag("repeat-count").assertTextContains("12 occurrences · Last: Dec 31, 2026")
        compose.onNodeWithTag("custom-title").performTextReplacement("Updated series")
        compose.onNodeWithText("Save").performScrollTo().performSemanticsAction(SemanticsActions.OnClick)
        compose.onAllNodesWithText("Updated series").onFirst().performSemanticsAction(SemanticsActions.OnClick)
        compose.onNodeWithText("Delete series").performClick()
        compose.onNodeWithText("Delete all occurrences in this series?").assertIsDisplayed()
        compose.onNodeWithText("Cancel").performClick()
        compose.onNodeWithText("Delete series").performClick()
        compose.onNodeWithText("Delete").performClick()
        compose.onAllNodesWithText("Updated series").assertCountEquals(0)
    }

    @Test fun recurringValidationAndLeapPreviewKeepNativePickers() {
        start()
        compose.onNodeWithText("Events").performClick()
        compose.onNodeWithContentDescription("Add event").performClick()
        compose.onNodeWithTag("custom-title").performTextInput("Repeat validation")
        compose.onNodeWithTag("custom-date").performTextReplacement("2026-01-01")
        for (frequency in listOf("days", "weekly", "monthly", "yearly")) {
            compose.onNodeWithTag("repeat-$frequency").performScrollTo().performSemanticsAction(SemanticsActions.OnClick)
            compose.onNodeWithText("Save").performScrollTo().assertIsNotEnabled()
        }
        compose.onNodeWithTag("repeat-days").performScrollTo().performClick()
        compose.onNodeWithTag("repeat-interval").assertTextContains("3")
        compose.onNodeWithTag("repeat-end").performScrollTo()
        screenshot("repeat-days-editor")
        compose.onNodeWithTag("repeat-end").performScrollTo().performTextReplacement("2026-01-10")
        compose.onNodeWithTag("repeat-count").assertTextContains("4 occurrences · Last: Jan 10, 2026")
        for (invalid in listOf("0", "1.5", "")) {
            compose.onNodeWithTag("repeat-interval").performScrollTo().performTextReplacement(invalid)
            compose.onNodeWithText("Save").performScrollTo().assertIsNotEnabled()
        }
        compose.onNodeWithTag("repeat-interval").performScrollTo().performTextReplacement("3")
        compose.onNodeWithTag("custom-date").performScrollTo().performTextReplacement("2028-02-29")
        compose.onNodeWithTag("repeat-yearly").performScrollTo().performClick()
        compose.onNodeWithTag("repeat-end").performScrollTo().performTextReplacement("2032-02-29")
        compose.onNodeWithTag("repeat-count").assertTextContains("2 occurrences · Last: Feb 29, 2032")
        compose.onNodeWithContentDescription("Include February 28 in non-leap years").performScrollTo().performClick()
        compose.onNodeWithTag("repeat-count").assertTextContains("5 occurrences · Last: Feb 29, 2032")
        compose.onNodeWithTag("repeat-end").performScrollTo().onChildren().filter(hasClickAction()).onFirst().performClick()
        compose.onNodeWithText("OK").assertIsEnabled()
        screenshot("repeat-end-native-picker")
        compose.onNodeWithText("OK").performClick()
        compose.onNodeWithTag("repeat-end").assertTextContains("2032-02-29")
        compose.onNodeWithTag("repeat-end").performTextReplacement("2027-01-01")
        compose.onNodeWithText("Save").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithTag("repeat-none").performScrollTo().performClick()
        compose.onNodeWithTag("repeat-preview").assertDoesNotExist()
        compose.onNodeWithTag("repeat-end").assertDoesNotExist()
        compose.onNodeWithText("Save").performScrollTo().assertIsEnabled().performClick()
        compose.onNodeWithText("Repeat validation").performClick()
        compose.onNodeWithText("Edit").assertIsDisplayed()
        compose.onNodeWithText("Edit series").assertDoesNotExist()
    }

    @Test fun recurringSeriesRetainsSavedZoneAndCanBecomeOneEvent() {
        val originalZone = java.util.TimeZone.getDefault()
        java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("Europe/Brussels"))
        try {
            val event = CustomEvent(title = "Zone series", date = LocalDate.of(2026, 1, 31), time = java.time.LocalTime.of(9, 0), zoneId = "Europe/Brussels",
                repeat = com.rsgkh.calendar.domain.EventRepeat(com.rsgkh.calendar.domain.RepeatFrequency.MONTHLY, LocalDate.of(2026, 12, 31), includeThirty = true))
            start(AppSettings(khmer = false, todayTimeZone = com.rsgkh.calendar.data.TodayTimeZone.CAMBODIA), customEvents = listOf(event))
            compose.onNodeWithText("Events").performClick()
            compose.onNodeWithText("Search events").performTextInput("2026-09-30")
            compose.onNodeWithText("Zone series").performClick()
            compose.onNodeWithText("14:00 · Cambodia time (UTC+7)").assertIsDisplayed()
            compose.onNodeWithText("Edit series").performClick()
            compose.onNodeWithTag("custom-date").assertTextContains("2026-01-31")
            compose.onNodeWithTag("custom-time").assertTextContains("09:00")
            compose.onNodeWithTag("custom-time-zone").assertTextContains("Europe/Brussels (UTC+1)")
            compose.onNodeWithTag("repeat-none").performScrollTo().performClick()
            compose.onNodeWithText("Save").performScrollTo().performClick()
            compose.onNodeWithText("Zone series").performClick()
            compose.onNodeWithText("15:00 · Cambodia time (UTC+7)").assertIsDisplayed()
            compose.onNodeWithText("Edit").performClick()
            compose.onNodeWithTag("custom-date").assertTextContains("2026-01-31")
            compose.onNodeWithTag("custom-time").assertTextContains("15:00")
            compose.onNodeWithTag("repeat-weekly").performScrollTo().performClick()
            compose.onNodeWithTag("repeat-end").performScrollTo().performTextReplacement("2026-04-30")
            compose.onNodeWithText("Save").performScrollTo().performClick()
            compose.onNodeWithText("Search events").performTextInput("2026-04-04")
            compose.onNodeWithText("Zone series").performClick()
            compose.onNodeWithText("15:00 · Cambodia time (UTC+7)").assertIsDisplayed()
        } finally { java.util.TimeZone.setDefault(originalZone) }
    }
    protected fun start(settings: AppSettings = AppSettings(khmer = false, theme = ThemeMode.LIGHT), now: Instant? = null,
        notificationAccess: NotificationAccess = NotificationAccess(),
        customEvents: List<CustomEvent> = emptyList(), uriHandler: UriHandler? = null) {
        val state = mutableStateOf(settings)
        val custom = mutableStateOf<List<CustomEvent>>(customEvents)
        compose.setContent {
            CompositionLocalProvider(LocalUriHandler provides (uriHandler ?: LocalUriHandler.current)) {
                CalendarApp(state.value, now?.let { state.value.todayTimeZone.today(it, ZoneId.of("Europe/Brussels")) } ?: LocalDate.of(2026, 9, 10), customEvents = custom.value,
                    notificationAccess = notificationAccess,
                    onSaveCustom = { event -> custom.value = custom.value.filterNot { it.id == event.id } + event },
                    onDeleteCustom = { id -> custom.value = custom.value.filterNot { it.id == id } }) { state.value = it }
            }
        }
    }
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
        } else {
            // Dialogs and popups add Compose roots; capture the complete device
            // display so they and the system bars are included in the screenshot.
            checkNotNull(InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()) {
                "Could not capture the device display"
            }
        }
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
        compose.onNode(hasContentDescription("Thursday, 1 October", substring = true)).assertIsSelected()
        screenshot("calendar-october-light")
        compose.onNode(hasText("Today") and hasClickAction()).performClick()
        compose.onNodeWithText("September").assertIsDisplayed()
        compose.onNodeWithText("BE 2570").assertIsDisplayed()
        compose.onNode(hasContentDescription("Thursday, 10 September", substring = true)).assertIsSelected()
        compose.onNodeWithTag("month-grid").performTouchInput { swipeRight() }
        compose.onNodeWithText("August").assertIsDisplayed()
        compose.onNode(hasContentDescription("Saturday, 1 August", substring = true)).assertIsSelected()
        compose.onNodeWithContentDescription("Next month").performClick()
        compose.onNodeWithText("September").assertIsDisplayed()
        compose.onNode(hasContentDescription("Tuesday, 1 September", substring = true)).assertIsSelected()
        compose.onNodeWithContentDescription("Previous month").performClick()
        compose.onNodeWithText("August").assertIsDisplayed()
        compose.onNode(hasContentDescription("Saturday, 1 August", substring = true)).assertIsSelected()
    }
    @Test fun monthSwipesFromDay31SelectFirstDayAcrossShortMonthsAndYears() {
        start(now = Instant.parse("2024-03-31T12:00:00Z"))
        fun selected(description: String) = compose.onNode(hasContentDescription(description, substring = true)).assertIsSelected()
        fun jumpToDay31(year: String, month: String, description: String) {
            compose.onNodeWithContentDescription("Choose month and year").performClick()
            compose.onNodeWithTag("month-year-input").performTextReplacement(year)
            compose.onNodeWithText(month).performClick()
            compose.onNodeWithText("Go").performClick()
            compose.onNode(hasContentDescription(description, substring = true)).performClick()
            compose.onNodeWithText("Close").performClick()
            selected(description)
        }

        selected("Sunday, 31 March")
        compose.onNodeWithTag("month-grid").performTouchInput { swipeLeft() }
        selected("Monday, 1 April") // 30-day month.
        compose.onNode(hasText("Today") and hasClickAction()).performClick()
        selected("Sunday, 31 March")
        compose.onNodeWithTag("month-grid").performTouchInput { swipeRight() }
        selected("Thursday, 1 February") // Leap-year February.

        jumpToDay31("2025", "Jan", "Friday, 31 January")
        compose.onNodeWithTag("month-grid").performTouchInput { swipeLeft() }
        selected("Saturday, 1 February") // Non-leap-year February.

        jumpToDay31("2025", "Dec", "Wednesday, 31 December")
        compose.onNodeWithTag("month-grid").performTouchInput { swipeLeft() }
        compose.onNodeWithText("2026").assertIsDisplayed()
        selected("Thursday, 1 January")
        compose.onNodeWithTag("month-grid").performTouchInput { swipeRight() }
        compose.onNodeWithText("2025").assertIsDisplayed()
        selected("Monday, 1 December")
    }
    @Test fun searchFiltersAndEventDetails() {
        start()
        compose.onNodeWithText("Events").performClick()
        compose.onNodeWithText("Holidays").performClick()
        compose.onNodeWithText("Search events").performTextInput("Constitution")
        compose.onNodeWithText("Constitution Day · 33rd").assertIsDisplayed().performClick()
        compose.onNodeWithText("Delete").assertDoesNotExist()
        compose.onAllNodesWithText("Public holiday").onLast().assertIsDisplayed()
        compose.onNodeWithText("Close").performClick()
        screenshot("events-search")
        compose.onNodeWithContentDescription("Choose year").performClick()
        compose.onNodeWithTag("event-year-input").assertTextContains("2026")
        compose.onNodeWithText("Cancel").performClick()
        compose.onNodeWithTag("event-year").assertTextEquals("2026")
        compose.onNodeWithText("Constitution Day · 33rd").assertIsDisplayed()
        compose.onNodeWithContentDescription("Choose year").performClick()
        compose.onNodeWithTag("event-year-input").performTextReplacement("2028")
        compose.onNodeWithText("Go").performClick()
        compose.onNodeWithTag("event-year").assertTextEquals("2028")
        compose.onNodeWithText("Search events").assertTextContains("Constitution")
        compose.onNodeWithText("Holidays").assertIsSelected()
        compose.onNodeWithText("No matching events. Try another filter or search.").assertIsDisplayed()
        compose.onNodeWithText("Observances").performClick()
        compose.onNodeWithText("Constitution Day · 35th").assertIsDisplayed()
    }
    @Test fun themeAndLanguageCanBeChanged() {
        start()
        compose.onNodeWithText("Settings").performClick()
        compose.onNodeWithContentDescription("Background accent").performScrollTo().assertIsOn()
        screenshot("settings-background-accent-light")
        compose.onNodeWithContentDescription("Background accent").performClick().assertIsOff()
        screenshot("settings-background-neutral-light")
        compose.onNode(hasText("Dark") and hasAnyAncestor(hasTestTag("theme-mode"))).performScrollTo().performClick().assertIsSelected()
        compose.onNode(hasText("Light") and hasAnyAncestor(hasTestTag("theme-mode"))).assertIsNotSelected()
        compose.onNodeWithContentDescription("Background accent").performScrollTo().assertIsOff()
        screenshot("settings-background-neutral-dark")
        compose.onNodeWithContentDescription("Background accent").performClick().assertIsOn()
        screenshot("settings-dark")
        compose.onNodeWithText("ខ្មែរ").performScrollTo().performClick()
        compose.onNodeWithContentDescription(L.text("ui.background_accent", true)).performScrollTo().assertIsOn()
        compose.onNode(hasText("ប្រតិទិន") and hasClickAction()).performClick()
        compose.onNodeWithText(L.text("calendar.month.short.9", true)).assertIsDisplayed()
        screenshot("calendar-khmer-dark")
    }
    @Test fun dateGridOpensDetailsAndReturnsFromEventDetails() {
        start()
        compose.onNode(hasContentDescription("Thursday, 10 September", substring = true)).performClick()
        compose.onNodeWithTag("date-details-title").assertIsDisplayed().assertTextEquals("Thursday, September 10, 2026")
        compose.onNodeWithText("Year of the Horse", substring = true).assertIsDisplayed()
        compose.onNodeWithText("Shaving Day").assertIsDisplayed()
        screenshot("date-details-shaving-day")
        compose.onNodeWithText("Close").performClick()
        compose.onNode(hasContentDescription("Thursday, 24 September", substring = true)).performClick()
        compose.onNode(hasText("Constitution Day · 33rd") and hasAnyAncestor(isDialog())).performClick()
        compose.onNodeWithText(L.text("ui.listed_in_cambodia_s_official_year_holiday_calendar.044398", false, "year" to 2026)).assertIsDisplayed()
        compose.onNode(hasText("(១៩៩៣)", substring = true) and hasAnyAncestor(isDialog())).assertIsDisplayed()
        compose.onNodeWithText("Close").performClick()
        compose.onNodeWithTag("date-details-title").assertIsDisplayed().assertTextEquals("Thursday, September 24, 2026")
        compose.onNodeWithText("Close").performClick()
        screenshot("calendar-today-and-selected-holiday")
        compose.onNode(hasContentDescription("Thursday, 24 September", substring = true)).assertIsSelected().performClick()
        compose.onNodeWithTag("date-details-title").assertIsDisplayed()
    }
    @Test fun khmerDatePopupIncludesTheFullTraditionalDateAndDayStatus() {
        start(AppSettings(khmer = true, theme = ThemeMode.DARK))
        compose.onNode(hasContentDescription("១៣រោច", substring = true)).performClick()
        compose.onNodeWithTag("date-details-title").assertIsDisplayed().assertTextEquals("September 10, 2026")
        compose.onNodeWithText("ថ្ងៃព្រហស្បតិ៍ ១៣រោច ខែស្រាពណ៍ ឆ្នាំមមី អដ្ឋស័ក ពុទ្ធសករាជ ២៥៧០ ត្រូវនឹងថ្ងៃទី១០ ខែកញ្ញា ឆ្នាំ២០២៦").assertIsDisplayed()
        compose.onNodeWithText("ថ្ងៃកោរ").assertIsDisplayed()
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
        compose.onNodeWithText(L.text("ui.listed_in_cambodia_s_official_year_holiday_calendar.044398", false, "year" to 2025)).assertIsDisplayed()
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
    @Test fun calendarAndEventYearNavigationWorksAcrossSupportedRange() {
        start()
        for (year in listOf(2024, 2027, 2000, 2030, 1999, 2031)) {
            compose.onNode(hasText("Calendar") and hasClickAction()).performClick()
            compose.onNodeWithContentDescription("Choose month and year").performClick()
            compose.onNode(hasSetTextAction()).performTextReplacement(year.toString())
            compose.onNodeWithText("Jan").performClick()
            compose.onNodeWithText("Go").performClick()
            if (year in 2000..2030) {
                compose.onNodeWithText("New Year's Day").performScrollTo().assertIsDisplayed()
            }
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
                compose.onNodeWithText("New Year's Day").performScrollTo().assertIsDisplayed()
            }
        }
    }

    @Test fun calculatedFutureEventsShowTheirProvenanceAndStayOutOfHolidayFilter() {
        start()
        compose.onNodeWithText(L.text("ui.events.11d867", false)).performClick()
        compose.onNodeWithContentDescription(L.text("ui.choose_year.0853a0", false)).performClick()
        compose.onNodeWithTag("event-year-input").performTextReplacement("2031")
        compose.onNodeWithText(L.text("ui.go.ba4f19", false)).performClick()
        val title = EventRepository.forDate(LocalDate.of(2031, 1, 1)).single { it.id == "new_year_day" }.titleEn
        compose.onNodeWithText(title).performScrollTo().performClick()
        compose.onNode(hasText(L.text("rules.calculated_label", false)) and hasAnyAncestor(isDialog())).assertIsDisplayed()
        compose.onNodeWithText(L.text("events.engine_calculations", false)).performScrollTo().assertIsDisplayed()
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
        compose.onNode(hasText("Personal") and hasClickAction()).assertIsDisplayed().performClick()
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
        compose.onNode(hasText("Personal") and hasClickAction()).assertIsDisplayed().assertIsSelected()
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
            compose.onNodeWithTag("custom-time-zone").assertTextContains("Local time (UTC+2)", substring = true)
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
            compose.onNodeWithTag("custom-time-zone").assertTextContains("Cambodia time (UTC+7)", substring = true)
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
            compose.onNodeWithTag("custom-time-zone").assertTextContains("ម៉ោងក្នុងតំបន់ (UTC+1)", substring = true)
            screenshot("custom-editor-local-khmer")
            compose.onNodeWithText("បោះបង់").performScrollTo().performClick()
            compose.onNodeWithText("ការកំណត់").performClick()
            chooseTimeZone("កម្ពុជា (UTC+7)")
            compose.onNodeWithText("ព្រឹត្តិការណ៍").performClick()
            compose.onNodeWithContentDescription("បន្ថែមព្រឹត្តិការណ៍").performClick()
            compose.onNodeWithTag("custom-time-zone").assertTextContains("ម៉ោងកម្ពុជា (UTC+7)", substring = true)
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
        compose.onNode(hasText("Dark") and hasAnyAncestor(hasTestTag("theme-mode"))).performScrollTo().performClick().assertIsSelected()
        compose.onNodeWithText("ខ្មែរ").performScrollTo().performClick()
        compose.onNodeWithTag("accent-color").performScrollTo().assertTextContains(L.text("ui.lime.46ea65", true))
        compose.onNodeWithTag("settings-scroll").performScrollToNode(hasText(L.text("ui.sunday_when_turned_off.e40816", true)))
        compose.onNodeWithText(L.text("ui.start_week_on_monday.5578c3", true)).assertIsDisplayed()
        compose.onNodeWithText("បិទដើម្បីផ្តើមសប្តាហ៍ពីថ្ងៃអាទិត្យ").assertIsDisplayed()
        compose.onNodeWithTag("settings-scroll").performScrollToNode(hasTestTag("today-time-zone"))
        compose.onNodeWithTag("today-time-zone").assertTextContains("កម្ពុជា (UTC+7)")
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
