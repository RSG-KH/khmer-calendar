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
        val defaults = AppSettings(ThemeMode.SYSTEM, Accent.BLUE, khmer = true, mondayFirst = false, showLunar = true,
            showHolyDaysInCalendar = true, showHolyDaysInEvents = false,
            highlightSunday = true, notificationsEnabled = false, pushMinutes = 300, repeatHours = 0, todayTimeZone = TodayTimeZone.LOCAL,
            fontScale = FontScale.PERCENT_100)
        assertEquals(defaults, AppSettings())
        assertEquals(defaults, AppPreferences(context).read())
        val expected = AppSettings(ThemeMode.DARK, Accent.LIME, khmer = false, mondayFirst = true, showLunar = false,
            showHolyDaysInCalendar = false, showHolyDaysInEvents = true,
            highlightSunday = false, repeatHours = 6, todayTimeZone = TodayTimeZone.CAMBODIA,
            fontScale = FontScale.PERCENT_110)
        AppPreferences(context).write(expected)
        assertEquals(expected, AppPreferences(context).read())
    }

    @Test fun fontSizeSettingCanBeChanged() {
        start()
        val height100 = compose.onNodeWithText("International Literacy Day").fetchSemanticsNode().size.height

        compose.onNodeWithText("Settings").performClick()
        compose.onNodeWithText("Font size").assertIsDisplayed()
        compose.onNodeWithTag("font-scale").assertTextContains("100%")

        compose.onNodeWithTag("font-scale").performClick()
        compose.onNode(hasText("120%") and hasAnyAncestor(isPopup())).performClick()
        compose.onNodeWithTag("font-scale").assertTextContains("120%")
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
    fun customEventPopupDialogDisplaysStar() {
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
}

