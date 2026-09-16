// Copyright (c) 2026 RSG-KH | Apache-2.0 License
package com.rsgkh.calendar

import android.content.res.Configuration
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalConfiguration
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.annotation.LooperMode
import androidx.test.core.app.ApplicationProvider
import androidx.compose.ui.test.*
import androidx.compose.ui.unit.dp
import com.rsgkh.calendar.data.*
import com.rsgkh.calendar.domain.KhmerDateDetails
import com.rsgkh.calendar.i18n.L
import com.rsgkh.calendar.ui.CalendarApp
import com.rsgkh.calendar.ui.NotificationAccess
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Test

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [32], qualifiers = "w411dp-h891dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@LooperMode(LooperMode.Mode.PAUSED)
class CalendarRenderTest : CalendarUiScenarios() {
    @Test fun preferencesSurviveRepositoryRecreation() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val defaults = AppSettings(ThemeMode.SYSTEM, Accent.BLUE, khmer = true, mondayFirst = false, showLongerWeekdayNames = false, showCopyButtons = false, showLunar = true,
            highlightWeekdayNames = false, showHolyDaysInCalendar = true, showHolyDaysInEvents = false,
            highlightSunday = true, notificationsEnabled = false, pushCustomEvents = true, pushHolidays = true,
            pushObservances = true, pushHolyDays = false, pushMinutes = 300, repeatHours = 0, todayTimeZone = TodayTimeZone.LOCAL,
            fontScale = FontScale.PERCENT_100, backgroundAccent = true)
        assertEquals(defaults, AppSettings())
        assertEquals(defaults, AppPreferences(context).read())
        val expected = AppSettings(ThemeMode.DARK, Accent.LIME, khmer = false, mondayFirst = true, showLongerWeekdayNames = true, showCopyButtons = true, showLunar = false,
            highlightWeekdayNames = true, showHolyDaysInCalendar = false, showHolyDaysInEvents = true,
            highlightSunday = false, pushCustomEvents = false, pushHolidays = false, pushObservances = false, pushHolyDays = false,
            repeatHours = 6, todayTimeZone = TodayTimeZone.CAMBODIA,
            fontScale = FontScale.PERCENT_110, backgroundAccent = false)
        AppPreferences(context).write(expected)
        assertEquals(expected, AppPreferences(context).read())
        AppPreferences(context).write(expected.copy(showCopyButtons = false, showLongerWeekdayNames = false, highlightWeekdayNames = false, backgroundAccent = true))
        assertEquals(expected.copy(showCopyButtons = false, showLongerWeekdayNames = false, highlightWeekdayNames = false, backgroundAccent = true), AppPreferences(context).read())
    }

    @Test fun systemThemeFollowsDeviceUntilAChipIsChosen() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val state = mutableStateOf(AppPreferences(context).read().copy(khmer = false))
        val configuration = mutableStateOf(Configuration(context.resources.configuration).apply {
            uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or Configuration.UI_MODE_NIGHT_NO
        })
        compose.setContent {
            CompositionLocalProvider(LocalConfiguration provides configuration.value) {
                CalendarApp(state.value, LocalDate.of(2026, 9, 10)) {
                    AppPreferences(context).write(it)
                    state.value = it
                }
            }
        }
        fun chip(label: String) = compose.onNode(hasText(label) and hasAnyAncestor(hasTestTag("theme-mode")))
        fun systemNight(night: Boolean) = compose.runOnIdle {
            configuration.value = Configuration(configuration.value).apply {
                uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or
                    if (night) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
            }
            state.value = AppPreferences(context).read()
        }
        compose.onNodeWithText("Settings").performClick()
        chip("Light").assertIsSelected()
        chip("Dark").assertIsNotSelected()
        compose.onNodeWithText("System", substring = false).assertDoesNotExist()
        compose.onNodeWithContentDescription("Background accent").performScrollTo().performClick()
        assertEquals(ThemeMode.SYSTEM, AppPreferences(context).read().theme)
        systemNight(true)
        chip("Dark").performScrollTo().assertIsSelected()
        chip("Light").assertIsNotSelected()
        assertEquals(ThemeMode.SYSTEM, AppPreferences(context).read().theme)

        // Choosing the already highlighted chip must still create an override.
        chip("Dark").performClick()
        assertEquals(ThemeMode.DARK, AppPreferences(context).read().theme)
        systemNight(false)
        chip("Dark").assertIsSelected()
        chip("Light").assertIsNotSelected().performClick()
        assertEquals(ThemeMode.LIGHT, AppPreferences(context).read().theme)
        systemNight(true)
        chip("Light").assertIsSelected()
        chip("Dark").assertIsNotSelected()
    }

    @Test fun weekdayColorsFollowTheDayAcrossWeekStartLanguageAndThemeChanges() {
        start(AppSettings(khmer = false, theme = ThemeMode.LIGHT, highlightSunday = false))
        fun colors() = (1..7).map { day ->
            val layouts = mutableListOf<androidx.compose.ui.text.TextLayoutResult>()
            compose.onNodeWithTag("weekday-header-$day").performSemanticsAction(
                androidx.compose.ui.semantics.SemanticsActions.GetTextLayoutResult) { it(layouts) }
            layouts.single().layoutInput.style.color
        }
        fun toggle(key: String, khmer: Boolean = false) {
            val title = L.text(key, khmer)
            compose.onNodeWithTag("settings-scroll").performScrollToNode(hasText(title))
            compose.onNodeWithContentDescription(title).performClick()
        }
        assertEquals(1, colors().distinct().size)
        compose.onNodeWithText("Settings").performClick()
        compose.onNodeWithTag("settings-scroll").performScrollToNode(hasText("Highlight weekday names"))
        compose.onNodeWithContentDescription("Highlight weekday names").assertIsOff().performClick().assertIsOn()
        screenshot("settings-weekday-colors")
        compose.onNode(hasText("Calendar") and hasClickAction()).performClick()
        val lightColors = colors()
        assertEquals(7, lightColors.distinct().size)
        screenshot("weekday-colors-compact-light")

        compose.onNodeWithText("Settings").performClick()
        toggle("ui.start_week_on_monday.5578c3")
        toggle("ui.show_longer_weekday_names")
        compose.onNode(hasText("Calendar") and hasClickAction()).performClick()
        assertEquals(lightColors, colors())
        compose.onNodeWithTag("weekday-header-1").assertTextEquals("Mon")
        screenshot("weekday-colors-long-light")

        compose.onNodeWithText("Settings").performClick()
        compose.onNode(hasText("Dark") and hasAnyAncestor(hasTestTag("theme-mode"))).performScrollTo().performClick()
        compose.onNodeWithTag("settings-scroll").performScrollToNode(hasText("ខ្មែរ"))
        compose.onNodeWithText("ខ្មែរ").performClick()
        compose.onNode(hasText("ប្រតិទិន") and hasClickAction()).performClick()
        val darkColors = colors()
        assertEquals(7, darkColors.distinct().size)
        org.junit.Assert.assertTrue(lightColors.zip(darkColors).all { (light, dark) -> light != dark })
        compose.onNodeWithTag("weekday-header-4").assertTextEquals("ព្រហស្បតិ៍")
        screenshot("weekday-colors-khmer-dark")
        compose.onNode(hasText(L.text("ui.settings.0e0a4f", true)) and hasClickAction()).performClick()
        toggle("ui.highlight_weekday_names", true)
        compose.onNode(hasText("ប្រតិទិន") and hasClickAction()).performClick()
        assertEquals(1, colors().distinct().size)
        compose.onNode(hasText(L.text("ui.settings.0e0a4f", true)) and hasClickAction()).performClick()
        toggle("ui.highlight_sunday_column.549462", true)
        compose.onNode(hasText("ប្រតិទិន") and hasClickAction()).performClick()
        assertEquals(2, colors().distinct().size)
        assertEquals(1, colors().take(6).distinct().size)
    }

    @Test
    @Config(qualifiers = "w360dp-h800dp-xhdpi")
    fun longerWeekdaysFollowSettingsLanguageAndWeekStartOnSmallPhones() {
        start(AppSettings(khmer = false, fontScale = FontScale.PERCENT_120))
        fun header(text: String) = compose.onNode(hasText(text) and hasAnyAncestor(hasTestTag("month-grid")))
        header("M").assertIsDisplayed()
        header("Mon").assertDoesNotExist()
        compose.onNodeWithText("Settings").performClick()
        compose.onNodeWithTag("settings-scroll").performScrollToNode(hasText("Show longer weekday names"))
        compose.onNodeWithContentDescription("Show longer weekday names").assertIsOff().performClick().assertIsOn()
        compose.onNodeWithTag("settings-scroll").performScrollToNode(hasText("Start week on Monday"))
        compose.onNodeWithContentDescription("Start week on Monday").performClick()
        compose.onNode(hasText("Calendar") and hasClickAction()).performClick()
        listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun").forEach { header(it).assertIsDisplayed() }
        org.junit.Assert.assertTrue(header("Mon").getUnclippedBoundsInRoot().left < header("Sun").getUnclippedBoundsInRoot().left)
        screenshot("longer-weekdays-english")
        compose.onNodeWithText("Settings").performClick()
        compose.onNodeWithTag("settings-scroll").performScrollToNode(hasText("ខ្មែរ"))
        compose.onNodeWithText("ខ្មែរ").performClick()
        compose.onNode(hasText("ប្រតិទិន") and hasClickAction()).performClick()
        listOf("ចន្ទ", "អង្គារ", "ពុធ", "ព្រហស្បតិ៍", "សុក្រ", "សៅរ៍", "អាទិត្យ").forEach { name ->
            header(name).assertIsDisplayed()
            val layouts = mutableListOf<androidx.compose.ui.text.TextLayoutResult>()
            header(name).performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.GetTextLayoutResult) { it(layouts) }
            org.junit.Assert.assertFalse("Weekday must fit: $name", layouts.single().hasVisualOverflow)
        }
        screenshot("longer-weekdays-khmer")
        compose.onNode(hasText(L.text("ui.settings.0e0a4f", true)) and hasClickAction()).performClick()
        val title = L.text("ui.show_longer_weekday_names", true)
        compose.onNodeWithTag("settings-scroll").performScrollToNode(hasText(title))
        compose.onNodeWithContentDescription(title).assertIsOn().performClick().assertIsOff()
        compose.onNode(hasText("ប្រតិទិន") and hasClickAction()).performClick()
        header("ច").assertIsDisplayed()
        header("ចន្ទ").assertDoesNotExist()
    }

    @Test fun copyButtonsToggleControlsDateAndEventDetails() {
        start()
        fun openDate() {
            compose.onNode(hasContentDescription("Thursday, 24 September", substring = true)).performClick()
            compose.onNodeWithText("Date details").assertIsDisplayed()
        }
        fun openEvent() {
            compose.onNode(hasText("Constitution Day") and hasAnyAncestor(isDialog())).performClick()
        }
        fun toggleCopyButtons(enabled: Boolean) {
            compose.onNodeWithText("Settings").performClick()
            compose.onNodeWithTag("settings-scroll").performScrollToNode(hasText("Show copy buttons"))
            val toggle = compose.onNodeWithContentDescription("Show copy buttons")
            if (enabled) toggle.assertIsOff() else toggle.assertIsOn()
            toggle.performClick()
            if (enabled) toggle.assertIsOn() else toggle.assertIsOff()
            if (enabled) screenshot("settings-copy-buttons")
            compose.onNode(hasText("Calendar") and hasClickAction()).performClick()
        }
        fun assertHiddenInBothDialogs() {
            openDate()
            compose.onNodeWithContentDescription("Copy full date description").assertDoesNotExist()
            openEvent()
            compose.onNodeWithContentDescription("Copy event title").assertDoesNotExist()
            compose.onNodeWithText("Close").performClick()
            compose.onNodeWithText("Close").performClick()
        }

        assertHiddenInBothDialogs()
        toggleCopyButtons(true)
        openDate()
        pressCopyButton("Copy full date description", "date-copy-buttons-english")
        assertClipboardText(KhmerDateDetails.fromGregorian(LocalDate.of(2026, 9, 24)).fullEnglishDate())
        assertCopyConfirmation("Copy full date description", "Date description copied", "date-copy-buttons-english", retry = true)
        openEvent()
        pressCopyButton("Copy event title", "event-copy-buttons-english")
        assertClipboardText("Constitution Day")
        assertCopyConfirmation("Copy event title", "Event title copied", "event-copy-buttons-english")
        compose.onNodeWithText("Close").performClick()
        compose.onNodeWithText("Close").performClick()
        toggleCopyButtons(false)
        assertHiddenInBothDialogs()
    }

    @Test fun copyButtonsPreserveKhmerDateAndCustomEventTitle() {
        val event = CustomEvent(title = "ជួបគ្រួសារ · Family meeting", date = LocalDate.of(2026, 9, 10),
            time = LocalTime.NOON, notes = "Notes must not be copied with the title")
        start(AppSettings(khmer = true, theme = ThemeMode.DARK, showCopyButtons = true, todayTimeZone = TodayTimeZone.CAMBODIA),
            customEvents = listOf(event))
        compose.onNode(hasContentDescription("១៣រោច", substring = true)).performClick()
        pressCopyButton(L.text("ui.copy_full_date", true), "date-copy-buttons-khmer")
        assertClipboardText("ថ្ងៃព្រហស្បតិ៍ ១៣រោច ខែស្រាពណ៍ ឆ្នាំមមី អដ្ឋស័ក ពុទ្ធសករាជ ២៥៧០ ត្រូវនឹងថ្ងៃទី១០ ខែកញ្ញា ឆ្នាំ២០២៦")
        assertCopyConfirmation(L.text("ui.copy_full_date", true), L.text("ui.full_date_copied", true), "date-copy-buttons-khmer")
        compose.onNode(hasText(event.title) and hasAnyAncestor(isDialog())).performScrollTo().performClick()
        pressCopyButton(L.text("ui.copy_event_title", true), "event-copy-buttons-khmer")
        assertClipboardText(event.title)
        assertCopyConfirmation(L.text("ui.copy_event_title", true), L.text("ui.event_title_copied", true), "event-copy-buttons-khmer")
        compose.onNodeWithText(L.text("ui.edit.bbdcac", true)).assertIsDisplayed()
        compose.onNodeWithText(L.text("ui.delete.4708f4", true)).assertIsDisplayed()
    }

    private fun assertCopyButtonCentered(label: String) {
        val button = compose.onNodeWithContentDescription(label).assertIsDisplayed()
        val buttonBounds = button.fetchSemanticsNode().boundsInRoot
        val iconBounds = compose.onNodeWithContentDescription(label, useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        assertEquals("Copy icon and touch feedback must share their horizontal center", buttonBounds.center.x, iconBounds.center.x, 0.5f)
        assertEquals("Copy icon and touch feedback must share their vertical center", buttonBounds.center.y, iconBounds.center.y, 0.5f)
        button.assertWidthIsEqualTo(48.dp).assertHeightIsEqualTo(48.dp)
    }

    private fun pressCopyButton(label: String, screenshotName: String) {
        assertCopyButtonCentered(label)
        val button = compose.onNodeWithContentDescription(label)
        compose.mainClock.autoAdvance = false
        try {
            button.performTouchInput { down(center) }
            compose.mainClock.advanceTimeBy(300)
            screenshot("$screenshotName-pressed")
            button.performTouchInput { up() }
        } finally {
            compose.mainClock.autoAdvance = true
        }
        compose.waitForIdle()
    }

    private fun assertCopyConfirmation(label: String, message: String, screenshotName: String, retry: Boolean = false) {
        val button = compose.onNodeWithContentDescription(label)
        val copied = SemanticsMatcher.expectValue(androidx.compose.ui.semantics.SemanticsProperties.StateDescription, message)
        button.assert(copied)
        assertCopyButtonCentered(label)
        screenshot("$screenshotName-check")
        if (retry) {
            compose.mainClock.advanceTimeBy(1_000)
            button.performClick()
            compose.waitForIdle()
            compose.mainClock.advanceTimeBy(1_500)
            button.assert(copied)
            compose.mainClock.advanceTimeBy(600)
        } else compose.mainClock.advanceTimeBy(2_100)
        button.assert(SemanticsMatcher.keyNotDefined(androidx.compose.ui.semantics.SemanticsProperties.StateDescription))
        screenshot(screenshotName)
    }

    private fun assertClipboardText(expected: String) {
        compose.runOnIdle {
            val context = ApplicationProvider.getApplicationContext<android.content.Context>()
            val clipboard = context.getSystemService(android.content.ClipboardManager::class.java)
            assertEquals(expected, clipboard.primaryClip?.getItemAt(0)?.text?.toString())
        }
    }

    @Test fun fontSizeSettingCanBeChanged() {
        start()
        val height100 = compose.onNodeWithText("International Literacy Day").fetchSemanticsNode().size.height

        compose.onNodeWithText("Settings").performClick()
        compose.onNodeWithText("Font size").assertIsDisplayed()
        compose.onNodeWithTag("font-scale").assertTextContains("100%")

        compose.onNodeWithTag("font-scale").performClick()
        listOf("130%", "140%", "150%").forEach { compose.onNodeWithText(it).assertDoesNotExist() }
        compose.onNode(hasText("120%") and hasAnyAncestor(isPopup())).performClick()
        compose.onNodeWithTag("font-scale").assertTextContains("120%")
        val phoneNavigation = compose.onNodeWithTag("bottom-navigation").getUnclippedBoundsInRoot()
        assertEquals(64f, (phoneNavigation.bottom - phoneNavigation.top).value, .5f)
        compose.onNode(hasText("Calendar") and hasClickAction()).performClick()
        val height120 = compose.onNodeWithText("International Literacy Day").fetchSemanticsNode().size.height

        compose.onNodeWithText("Settings").performClick()
        compose.onNodeWithTag("font-scale").performClick()
        compose.onNode(hasText("80%") and hasAnyAncestor(isPopup())).performClick()
        compose.onNodeWithTag("font-scale").assertTextContains("80%")
        compose.onNode(hasText("Calendar") and hasClickAction()).performClick()
        val height80 = compose.onNodeWithText("International Literacy Day").fetchSemanticsNode().size.height

        org.junit.Assert.assertTrue("120% ($height120) should be > 100% ($height100)", height120 > height100)
        org.junit.Assert.assertTrue("80% ($height80) should be < 100% ($height100)", height80 < height100)
    }

    @Test fun holyDaysInCalendarOnlyShowsLotusAndDetailsWithoutListingInCalendarEventList() {
        start(AppSettings(khmer = false, showHolyDaysInCalendar = true, showHolyDaysInEvents = false))
        // Verify calendar has holy day legend
        compose.onNodeWithText("Buddhist holy day").assertIsDisplayed()
        // Verify lotus/holy day marker exists in calendar grid
        compose.onAllNodes(hasContentDescription("Buddhist Holy Day", substring = true)).onFirst().assertIsDisplayed()
        // Verify Buddhist Holy Day is NOT listed as an event card in the calendar-scroll list below
        compose.onAllNodes(hasText("Buddhist Holy Day") and hasAnyAncestor(hasTestTag("calendar-scroll")) and hasClickAction()).assertCountEquals(0)
        // Open date details for a holy day (Sept 11, 2026 is 14 Roach / Buddhist Holy Day)
        compose.onNode(hasContentDescription("Friday, 11 September", substring = true)).performClick()
        compose.onNodeWithText("Date details").assertIsDisplayed()
        compose.onNodeWithText("Buddhist Holy Day", substring = true).assertIsDisplayed()
        compose.onNodeWithText("Close").performClick()
    }

    @Test
    @Config(qualifiers = "w360dp-h800dp-xhdpi")
    fun notificationCategoriesPreserveHolyDayChoiceWhenHiddenInBothLanguages() {
        start(AppSettings(khmer = false, notificationsEnabled = true, fontScale = FontScale.PERCENT_120))
        compose.onNodeWithText("Settings").performClick()
        val categories = listOf("notifications.push_custom", "notifications.push_holidays", "notifications.push_observances")
        val holyVisibility = "ui.buddhist_holy_days_in_events.53e502"
        val holyPush = "notifications.push_holy_days"
        for (k in listOf(false, true)) {
            if (k) {
                compose.onNodeWithTag("settings-scroll").performScrollToNode(hasText("ខ្មែរ"))
                compose.onNodeWithText("ខ្មែរ").performClick()
            }
            fun toggle(key: String): SemanticsNodeInteraction {
                val title = L.text(key, k)
                compose.onNodeWithTag("settings-scroll").performScrollToNode(hasText(title))
                return compose.onNodeWithContentDescription(title)
            }
            categories.forEach { key ->
                val option = toggle(key)
                if (!k) option.assertIsOn().performClick().assertIsOff() else option.assertIsOff()
            }
            compose.onNodeWithContentDescription(L.text(holyPush, k)).assertDoesNotExist()
            toggle(holyVisibility).assertIsOff().performClick().assertIsOn()
            toggle(holyPush).assertIsOff().performClick().assertIsOn()
            compose.onNodeWithTag("choose-push-time").performScrollTo()
            screenshot("notification-categories-$k")

            toggle(holyVisibility).performClick().assertIsOff()
            toggle(categories.last()).assertIsOff()
            compose.onNodeWithContentDescription(L.text(holyPush, k)).assertDoesNotExist()
            toggle(holyVisibility).performClick().assertIsOn()
            toggle(holyPush).assertIsOn().performClick().assertIsOff()
            toggle(holyVisibility).performClick().assertIsOff()
            toggle(holyVisibility).performClick().assertIsOn()
            toggle(holyPush).assertIsOff() // Both on and off choices survive hiding the option.
            toggle(holyVisibility).performClick().assertIsOff()
        }
    }

    @Test fun preciseRemindersPromptShowsWhenNotificationsEnabledAndExactPermissionNotGranted() {
        start(AppSettings(khmer = false, notificationsEnabled = true, repeatHours = 0), notificationAccess = NotificationAccess(canPost = true, exact = false))
        compose.onNodeWithText("Settings").performClick()
        compose.onNodeWithTag("settings-scroll").performScrollToNode(hasText(L.text("ui.allow_precise_reminders.733d9d", false)))
        compose.onNodeWithText(L.text("ui.allow_precise_reminders.733d9d", false)).assertIsDisplayed()
        compose.onNodeWithText(L.text("ui.the_app_won_t_guarantee_to_fire_notifications_correctly.b4cad2", false)).assertIsDisplayed()
        compose.onNodeWithText(L.text("ui.push_time.8421c3", false)).assertDoesNotExist()
        compose.onNodeWithText(L.text("ui.remind_every.a38a5d", false)).assertDoesNotExist()

        // If notifications are turned off, the prompt disappears
        compose.onNodeWithContentDescription(L.text("ui.enable_event_notifications.b4d4f8", false)).performScrollTo().performClick()
        compose.onNodeWithText(L.text("ui.allow_precise_reminders.733d9d", false)).assertDoesNotExist()
    }

    @Test fun timePickerWheelsRotateCyclicallyBeyondMinAndMax() {
        start()
        compose.onNodeWithText("Settings").performClick()
        compose.onNodeWithTag("settings-scroll").performScrollToNode(hasText("Enable event notifications"))
        compose.onNodeWithContentDescription("Enable event notifications").performClick().assertIsOn()
        compose.onNodeWithTag("choose-push-time").performScrollTo().performClick()
        compose.onNodeWithText("Select time").assertIsDisplayed()

        // Push time starts at 05:00.
        // For minute (starts at 00):
        // In cyclic mode, the top slot above 00 is 59!
        compose.onNode(hasText("59") and hasAnyAncestor(hasTestTag("wheel-minute"))).performClick()
        // Now 59 is centered. Above 59 is 58:
        compose.onNode(hasText("58") and hasAnyAncestor(hasTestTag("wheel-minute"))).performClick()
        // Now rotate forward: 58 -> 59 -> 00 -> 01
        compose.onNode(hasText("59") and hasAnyAncestor(hasTestTag("wheel-minute"))).performClick()
        compose.onNode(hasText("00") and hasAnyAncestor(hasTestTag("wheel-minute"))).performClick()
        compose.onNode(hasText("01") and hasAnyAncestor(hasTestTag("wheel-minute"))).performClick()

        // For hour: scroll to 23
        compose.onNodeWithTag("wheel-hour").performScrollToNode(hasText("23"))
        compose.onNode(hasText("23") and hasAnyAncestor(hasTestTag("wheel-hour"))).performClick()
        // In cyclic mode, the bottom slot below 23 is 00!
        compose.onNode(hasText("00") and hasAnyAncestor(hasTestTag("wheel-hour"))).performClick()
        // Below 00 is 01!
        compose.onNode(hasText("01") and hasAnyAncestor(hasTestTag("wheel-hour"))).performClick()
        // Now rotate backward: 01 -> 00 -> 23 -> 22
        compose.onNode(hasText("00") and hasAnyAncestor(hasTestTag("wheel-hour"))).performClick()
        compose.onNode(hasText("23") and hasAnyAncestor(hasTestTag("wheel-hour"))).performClick()
        compose.onNode(hasText("22") and hasAnyAncestor(hasTestTag("wheel-hour"))).performClick()

        compose.onNodeWithText("OK").performClick()
        compose.onNodeWithText("22:01").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "w891dp-h411dp-land-xhdpi")
    fun landscapeCalendarScreenshot() {
        start()
        screenshot("landscape-calendar")
    }

    @Test
    @Config(qualifiers = "sw800dp-w1280dp-h800dp-land-xhdpi")
    fun tabletLandscapeEventsOnTheDayRespectsHolyDaysToggle() {
        start(AppSettings(khmer = false, showHolyDaysInCalendar = true, showHolyDaysInEvents = false))
        compose.onNode(hasContentDescription("Friday, 11 September", substring = true)).performClick()
        compose.onNodeWithText("Events on the day").assertDoesNotExist()

        compose.onNodeWithText("Settings").performClick()
        compose.onNodeWithTag("settings-scroll").performScrollToNode(hasText("Buddhist holy days in events"))
        compose.onNode(hasContentDescription("Buddhist holy days in events") and isToggleable()).performClick()
        compose.onNodeWithText("Calendar").performClick()
        compose.onNode(hasContentDescription("Friday, 11 September", substring = true)).performClick()
        compose.onNodeWithText("Events on the day").assertIsDisplayed()
        compose.onAllNodes(hasText("Buddhist Holy Day", substring = true) and hasClickAction()).onFirst().assertIsDisplayed()
    }

    @Test
    fun dateDetailsPopupRespectsHolyDaysToggle() {
        start(AppSettings(khmer = false, showHolyDaysInCalendar = true, showHolyDaysInEvents = false))
        // Click 11 September once to select, second time to open Date details dialog
        compose.onNode(hasContentDescription("Friday, 11 September", substring = true)).performClick()
        compose.onNode(hasContentDescription("Friday, 11 September", substring = true)).performClick()
        compose.onNodeWithText("Date details").assertIsDisplayed()
        // Event section in Date details should NOT exist when toggle is false
        compose.onNodeWithTag("date-details-events").assertDoesNotExist()
        compose.onNodeWithText("Close").performClick()

        // Turn toggle ON in Settings
        compose.onNodeWithText("Settings").performClick()
        compose.onNodeWithTag("settings-scroll").performScrollToNode(hasText("Buddhist holy days in events"))
        compose.onNode(hasContentDescription("Buddhist holy days in events") and isToggleable()).performClick()
        compose.onNodeWithText("Calendar").performClick()

        // Reopen Date details dialog for 11 September
        compose.onNode(hasContentDescription("Friday, 11 September", substring = true)).performClick()
        compose.onNode(hasContentDescription("Friday, 11 September", substring = true)).performClick()
        compose.onNodeWithText("Date details").assertIsDisplayed()
        // Event section in Date details should now exist
        compose.onNodeWithTag("date-details-events").assertIsDisplayed()
        val holyDayEventRow = compose.onNode(
            hasAnyAncestor(hasTestTag("date-details-events")) and
                hasText("Buddhist holy day", substring = true) and
                hasClickAction()
        )
        holyDayEventRow.assertIsDisplayed()
        holyDayEventRow.performClick()
        // Clicking should open EventDialog with description
        compose.onNodeWithText(L.text("events.engine_calculations", false)).assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "sw800dp-w1280dp-h800dp-land-xhdpi")
    fun tabletLandscapeDateInfoBlockDisplaysGregorianAndZodiacBesideLunarDate() {
        start(AppSettings(khmer = false))
        compose.onNode(hasContentDescription("Saturday, 5 September", substring = true)).performClick()
        compose.onNodeWithText("September 5, 2026").assertIsDisplayed()
        compose.onNode(hasText("Virgo (Earth · Mercury)", substring = true)).assertIsDisplayed()
        compose.onNodeWithText("8 Roach (Waning) · Srapon\nYear of the Horse · Atthasak\nBuddhist Era 2570").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "w800dp-h400dp-land-xhdpi")
    fun phoneLandscapeNavigationRailItemsExpandAcrossVerticalSpace() {
        start(AppSettings(khmer = false))
        val rail = compose.onNodeWithTag("navigation-rail")
        rail.assertIsDisplayed()
        val railHeight = rail.fetchSemanticsNode().size.height
        val calendarNode = compose.onNode(hasText("Calendar") and hasAnyAncestor(hasTestTag("navigation-rail")) and hasClickAction())
        val eventsNode = compose.onNode(hasText("Events") and hasAnyAncestor(hasTestTag("navigation-rail")) and hasClickAction())
        val settingsNode = compose.onNode(hasText("Settings") and hasAnyAncestor(hasTestTag("navigation-rail")) and hasClickAction())

        val calHeight = calendarNode.fetchSemanticsNode().size.height
        val eventsHeight = eventsNode.fetchSemanticsNode().size.height
        val settingsHeight = settingsNode.fetchSemanticsNode().size.height

        org.junit.Assert.assertTrue("calHeight ($calHeight) should take approx 1/3 of railHeight ($railHeight)", calHeight > railHeight / 4)
        org.junit.Assert.assertEquals(calHeight, eventsHeight)
        org.junit.Assert.assertEquals(calHeight, settingsHeight)
    }

    @Test
    @Config(qualifiers = "sw800dp-w1280dp-h800dp-land-xhdpi")
    fun tabletLandscapeNavigationRailItemsRemainCompact() {
        start(AppSettings(khmer = false))
        val rail = compose.onNodeWithTag("navigation-rail")
        rail.assertIsDisplayed()
        val railHeight = rail.fetchSemanticsNode().size.height
        val calendarNode = compose.onNode(hasText("Calendar") and hasAnyAncestor(hasTestTag("navigation-rail")) and hasClickAction())
        val calHeight = calendarNode.fetchSemanticsNode().size.height

        org.junit.Assert.assertTrue("calHeight ($calHeight) should be compact, much less than railHeight / 4 ($railHeight)", calHeight < railHeight / 4)
    }

    @Test
    fun customEventDisplaysStarMarkerAndLegendWhenPresent() {
        val customEvent = CustomEvent(
            title = "Family Celebration",
            date = LocalDate.of(2026, 9, 15),
            time = LocalTime.of(18, 30),
            notes = "Special dinner"
        )
        start(AppSettings(khmer = false), customEvents = listOf(customEvent))
        compose.onNodeWithText("Custom").assertIsDisplayed()
        screenshot("custom-event-calendar-grid")

        // Click September 15 date cell
        compose.onNode(hasContentDescription("Tuesday, 15 September", substring = true)).performClick()
        // Event should appear in the events list below calendar
        compose.onNode(hasText("Family Celebration") and hasAnyAncestor(hasTestTag("calendar-scroll"))).assertIsDisplayed()

        // Click date again to open Date details dialog
        compose.onNode(hasContentDescription("Tuesday, 15 September", substring = true)).performClick()
        compose.onNodeWithText("Date details").assertIsDisplayed()
        compose.onNodeWithTag("date-details-events").assertIsDisplayed()
        compose.onNode(
            hasAnyAncestor(hasTestTag("date-details-events")) and
                hasText("Family Celebration")
        ).assertIsDisplayed()
        screenshot("custom-event-preview")
        compose.onNodeWithText("Close").performClick()
    }

    @Test
    fun customEventDarkThemeDisplaysStarMarkerAndLegend() {
        val customEvent = CustomEvent(
            title = "Family Celebration with a Very Long Title Across the Entire Event Card",
            date = LocalDate.of(2026, 9, 15),
            time = LocalTime.of(18, 30),
            notes = "Special dinner"
        )
        start(AppSettings(khmer = false, theme = ThemeMode.DARK), customEvents = listOf(customEvent))
        compose.onNodeWithText("Custom").assertIsDisplayed()
        screenshot("custom-event-calendar-dark")
        compose.onNode(hasContentDescription("Tuesday, 15 September", substring = true)).performClick()
        compose.onNodeWithText("Close").performClick()
        screenshot("calendar-today-and-selected-custom-dark")
    }

    @Test
    fun customEventPopupDialogDisplaysZodiacBackground() {
        val customEvent = CustomEvent(
            title = "This is a custom event that I manually added to test if everything is ok.",
            date = LocalDate.of(2026, 9, 14),
            time = LocalTime.of(9, 0),
            notes = ""
        )
        start(AppSettings(khmer = true, theme = ThemeMode.DARK), customEvents = listOf(customEvent))
        compose.onNode(hasContentDescription("This is a custom event", substring = true) and hasClickAction()).performClick()
        compose.onAllNodes(hasText("This is a custom event that I manually added to test if everything is ok.") and hasClickAction()).onFirst().performClick()
        screenshot("custom-event-dialog-popup")
    }

    @Test
    fun noMatchingEventsTextNotDisplayedOnCalendarTabWhenNoEvents() {
        start()
        // Jump to March 1994 on Calendar tab
        compose.onNodeWithContentDescription("Choose month and year").performClick()
        compose.onNode(hasSetTextAction()).performTextReplacement("1994")
        compose.onNodeWithText("Mar").performClick()
        compose.onNodeWithText("Go").performClick()
        compose.onNodeWithText("1994").assertIsDisplayed()
        compose.onNodeWithText("March").assertIsDisplayed()
        screenshot("calendar-selected-other-year")

        // Verify "No matching events. Try another filter or search." does NOT exist on Calendar tab
        compose.onNodeWithText(L.text("ui.no_matching_events_try_another_filter_or_search.57812b", false)).assertDoesNotExist()

        // Switch to Events tab and filter by Custom (no custom events exist)
        compose.onNodeWithText("Events").performClick()
        compose.onNode(hasText("Custom") and hasClickAction()).performClick()

        // Verify "No matching events. Try another filter or search." DOES exist on Events tab
        compose.onNodeWithText(L.text("ui.no_matching_events_try_another_filter_or_search.57812b", false)).assertIsDisplayed()
    }

    @Test
    fun addEventFromDateDetailsPopupOpensEditorWithSelectedDate() {
        start()
        compose.onNode(hasContentDescription("Thursday, 10 September", substring = true)).performClick()
        compose.onNodeWithText("Date details").assertIsDisplayed()

        // "Add event" button should be displayed inside Date details popup
        val addEventText = L.text("ui.add_event.bf2f10", false)
        compose.onNodeWithText(addEventText).assertIsDisplayed().performClick()

        // "Date details" popup should close and Custom Event Editor should open with 2026-09-10 pre-filled
        compose.onNodeWithText("Date details").assertDoesNotExist()
        compose.onNodeWithTag("custom-title").assertIsDisplayed()
        compose.onNodeWithTag("custom-date").assertTextContains("2026-09-10")
    }

    @Test
    fun secondClickOnSameDateOpensDateDetailsPopup() {
        start()
        compose.onNode(hasContentDescription("Thursday, 10 September", substring = true)).performClick()
        compose.onNodeWithText("Date details").assertIsDisplayed()
        compose.onNodeWithText("Close").performClick()
        compose.onNodeWithText("Date details").assertDoesNotExist()

        // Clicking the same selected date again re-opens Date details popup
        compose.onNode(hasContentDescription("Thursday, 10 September", substring = true)).performClick()
        compose.onNodeWithText("Date details").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "sw800dp-w800dp-h1280dp-port-xhdpi")
    fun tabletPortraitSupportsLargerFontSizes() = checkTabletFontSizes("portrait")

    @Test
    @Config(qualifiers = "sw800dp-w1280dp-h800dp-land-xhdpi")
    fun tabletLandscapeSupportsLargerFontSizes() = checkTabletFontSizes("landscape")

    private fun checkTabletFontSizes(orientation: String) {
        start()
        compose.onNodeWithText("Settings").performClick()
        val landscape = orientation == "landscape"
        val navigationTag = if (landscape) "navigation-rail" else "bottom-navigation"
        val baselineNavigation = compose.onNodeWithTag(navigationTag).getUnclippedBoundsInRoot()
        for ((scale, expectedSize) in listOf(
            FontScale.PERCENT_120 to if (landscape) 80f else 64f,
            FontScale.PERCENT_130 to if (landscape) 88f else 70.4f,
            FontScale.PERCENT_140 to if (landscape) 92f else 73.6f,
            FontScale.PERCENT_150 to if (landscape) 96f else 76.8f,
        )) {
            compose.onNodeWithTag("font-scale").performClick()
            compose.onNode(hasText(scale.label) and hasAnyAncestor(isPopup())).performClick()
            compose.onNodeWithTag("font-scale").assertTextContains(scale.label)
            val navigation = compose.onNodeWithTag(navigationTag).getUnclippedBoundsInRoot()
            if (landscape) {
                assertEquals(expectedSize, (navigation.right - navigation.left).value, .5f)
                assertEquals((baselineNavigation.bottom - baselineNavigation.top).value, (navigation.bottom - navigation.top).value, .5f)
            } else {
                assertEquals(expectedSize, (navigation.bottom - navigation.top).value, .5f)
                assertEquals((baselineNavigation.right - baselineNavigation.left).value, (navigation.right - navigation.left).value, .5f)
            }
            val context = ApplicationProvider.getApplicationContext<android.content.Context>()
            AppPreferences(context).write(AppSettings(fontScale = scale))
            assertEquals(scale, AppPreferences(context).read().fontScale)
        }
        screenshot("tablet-$orientation-settings-150")
        compose.onNode(hasText("Calendar") and hasClickAction()).performClick()
        screenshot("tablet-$orientation-calendar-150")
        compose.onNodeWithContentDescription("Choose month and year").performClick()
        compose.onNodeWithTag("month-picker").assert(hasAnyAncestor(isDialog()))
        compose.onNodeWithTag("month-grid").assertExists()
        compose.onNodeWithText("Year (1800–2200)").assertIsDisplayed()
        compose.onNodeWithText("Dec").performScrollTo().assertIsDisplayed()
        screenshot("tablet-$orientation-month-picker-150")
        compose.onNodeWithText("Cancel").performScrollTo().performClick()
        compose.onNode(hasText("Events") and hasClickAction()).performClick()
        val header = compose.onNodeWithTag("events-header").getUnclippedBoundsInRoot()
        val nextYear = compose.onNodeWithContentDescription("Next year").getUnclippedBoundsInRoot()
        val add = compose.onNodeWithTag("add-event").getUnclippedBoundsInRoot()
        val content = compose.onNodeWithTag("events-content").getUnclippedBoundsInRoot()
        assertEquals(10f, (header.right - nextYear.right).value, .5f)
        assertEquals(32f, (content.right - add.right).value, .5f)
        assertEquals(32f, (content.bottom - add.bottom).value, .5f)
        org.junit.Assert.assertTrue(add.top > header.bottom)
        screenshot("tablet-$orientation-events-150")
        compose.onNodeWithTag("event-year").performClick()
        compose.onNodeWithTag("event-year-options").assert(hasAnyAncestor(isDialog()))
        compose.onNodeWithText("Year (1800–2200)").assertIsDisplayed()
        screenshot("tablet-$orientation-year-picker-150")
        compose.onNodeWithText("Cancel").performScrollTo().performClick()
    }
}
