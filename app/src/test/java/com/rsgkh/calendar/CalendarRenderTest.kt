// Copyright (c) 2026 RSG-KH | Apache-2.0 License
package com.rsgkh.calendar

import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.annotation.LooperMode
import androidx.test.core.app.ApplicationProvider
import androidx.compose.ui.test.*
import com.rsgkh.calendar.data.*
import com.rsgkh.calendar.domain.KhmerDateDetails
import com.rsgkh.calendar.i18n.L
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
        val defaults = AppSettings(ThemeMode.SYSTEM, Accent.BLUE, khmer = true, mondayFirst = false, showCopyButtons = false, showLunar = true,
            showHolyDaysInCalendar = true, showHolyDaysInEvents = false,
            highlightSunday = true, notificationsEnabled = false, pushMinutes = 300, repeatHours = 0, todayTimeZone = TodayTimeZone.LOCAL,
            fontScale = FontScale.PERCENT_100)
        assertEquals(defaults, AppSettings())
        assertEquals(defaults, AppPreferences(context).read())
        val expected = AppSettings(ThemeMode.DARK, Accent.LIME, khmer = false, mondayFirst = true, showCopyButtons = true, showLunar = false,
            showHolyDaysInCalendar = false, showHolyDaysInEvents = true,
            highlightSunday = false, repeatHours = 6, todayTimeZone = TodayTimeZone.CAMBODIA,
            fontScale = FontScale.PERCENT_110)
        AppPreferences(context).write(expected)
        assertEquals(expected, AppPreferences(context).read())
        AppPreferences(context).write(expected.copy(showCopyButtons = false))
        assertEquals(expected.copy(showCopyButtons = false), AppPreferences(context).read())
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
        compose.onNodeWithContentDescription("Copy full date description").assertIsDisplayed().performClick()
        assertClipboardText(KhmerDateDetails.fromGregorian(LocalDate.of(2026, 9, 24)).fullEnglishDate())
        screenshot("date-copy-buttons-english")
        openEvent()
        compose.onNodeWithContentDescription("Copy event title").assertIsDisplayed().performClick()
        assertClipboardText("Constitution Day")
        screenshot("event-copy-buttons-english")
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
        compose.onNodeWithContentDescription(L.text("ui.copy_full_date", true)).assertIsDisplayed().performClick()
        assertClipboardText("ថ្ងៃព្រហស្បតិ៍ ១៣រោច ខែស្រាពណ៍ ឆ្នាំមមី អដ្ឋស័ក ពុទ្ធសករាជ ២៥៧០ ត្រូវនឹងថ្ងៃទី១០ ខែកញ្ញា ឆ្នាំ២០២៦")
        screenshot("date-copy-buttons-khmer")
        compose.onNode(hasText(event.title) and hasAnyAncestor(isDialog())).performScrollTo().performClick()
        compose.onNodeWithContentDescription(L.text("ui.copy_event_title", true)).assertIsDisplayed().performClick()
        assertClipboardText(event.title)
        screenshot("event-copy-buttons-khmer")
        compose.onNodeWithText(L.text("ui.edit.bbdcac", true)).assertIsDisplayed()
        compose.onNodeWithText(L.text("ui.delete.4708f4", true)).assertIsDisplayed()
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
        compose.onNodeWithTag("settings-scroll").performScrollToNode(hasText("Buddhist holy days in Events"))
        compose.onNode(hasContentDescription("Buddhist holy days in Events") and isToggleable()).performClick()
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
        compose.onNodeWithTag("settings-scroll").performScrollToNode(hasText("Buddhist holy days in Events"))
        compose.onNode(hasContentDescription("Buddhist holy days in Events") and isToggleable()).performClick()
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
        compose.onNodeWithText(L.text("ui.a_buddhist_observance_on_the_8th_and_15th_waxing_days_t.4bac2c", false)).assertIsDisplayed()
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

    @Test fun aboutLinksAndCorrectedSourceTextWorkInBothLanguages() {
        start()
        compose.onNodeWithText("Settings").performClick()
        for (k in listOf(false, true)) {
            val version = L.text("about.version", k, "version" to BuildConfig.VERSION_NAME)
            compose.onNodeWithTag("settings-scroll").performScrollToNode(hasText(version))
            compose.onNodeWithText(version).assertIsDisplayed()
            compose.onNodeWithText("PWA · RSG-KH/khmer-calendar-pwa").assertIsDisplayed().assertHasClickAction()
            compose.onNodeWithText("Android · RSG-KH/khmer-calendar").assertIsDisplayed().assertHasClickAction()
            screenshot("about-$k")
            val sourceText = L.text("ui.lunar_calendar_1900_2100_based_on_work_by_phylypo_tum_t.d8396b", k)
            org.junit.Assert.assertFalse(sourceText.contains("1 Roach") || sourceText.contains("ពុទ្ធសករាជប្ដូរ"))
            if (!k) {
                compose.onNodeWithTag("settings-scroll").performScrollToNode(hasText("ខ្មែរ"))
                compose.onNodeWithText("ខ្មែរ").performClick()
            }
        }
    }
}

