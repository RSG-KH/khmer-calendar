// SPDX-License-Identifier: Apache-2.0
package com.rsgkh.calendar.widgets

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.rsgkh.calendar.R
import com.rsgkh.calendar.data.Accent
import com.rsgkh.calendar.data.AppPreferences
import com.rsgkh.calendar.data.AppSettings
import com.rsgkh.calendar.data.CustomEvent
import com.rsgkh.calendar.data.CustomEventRepository
import com.rsgkh.calendar.data.EventKind
import com.rsgkh.calendar.data.FontScale
import com.rsgkh.calendar.data.ThemeMode
import com.rsgkh.calendar.data.TodayTimeZone
import com.rsgkh.calendar.domain.ZodiacSign
import com.rsgkh.calendar.ui.accentColor
import com.rsgkh.calendar.ui.weekdayNameColor
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class WidgetSettingsTest {
    private val context: Context get() = ApplicationProvider.getApplicationContext()

    @Test fun globalObservanceChoiceOverridesWidgetChoiceAndRestoresIt() {
        val preferences = AppPreferences(context)
        val original = preferences.read()
        val observanceDate = (1..30).map { LocalDate.of(2026, 9, it) }
            .first { date -> com.rsgkh.calendar.data.EventRepository.forDate(date).any { it.kind == EventKind.OBSERVANCE } }
        val now = observanceDate.atTime(15, 0).atZone(com.rsgkh.calendar.data.CAMBODIA_ZONE).toInstant()
        try {
            val enabled = original.copy(khmer = false, todayTimeZone = TodayTimeZone.CAMBODIA,
                widgetShowObservances = true, showObservances = true)
            preferences.write(enabled)
            val visible = WidgetDataSource.load(context, now = now, includeMonth = true)
            assertTrue(visible.current.items.any { it.kind == EventKind.OBSERVANCE })
            assertTrue(visible.monthDays.first { it.date == observanceDate }.hasObservance)
            preferences.write(enabled.copy(showObservances = false))
            val hidden = WidgetDataSource.load(context, now = now, includeMonth = true)
            assertFalse(hidden.current.items.any { it.kind == EventKind.OBSERVANCE })
            assertFalse(hidden.monthDays.any { it.hasObservance })
            assertTrue(preferences.read().widgetShowObservances)
            preferences.write(enabled)
            assertTrue(WidgetDataSource.load(context, now = now).current.items.any { it.kind == EventKind.OBSERVANCE })
        } finally {
            preferences.write(original)
        }
    }

    @Test fun savedWidgetChoicesControlSnapshotAndHolidayVisibility() {
        val preferences = AppPreferences(context)
        val original = preferences.read()
        val now = Instant.parse("2026-09-24T08:00:00Z")
        try {
            val hiddenSettings = original.copy(
                khmer = false, theme = ThemeMode.DARK, accent = Accent.ROSE,
                todayTimeZone = TodayTimeZone.CAMBODIA,
                widgetFontScale = FontScale.PERCENT_150,
                widgetShowPersonal = false, widgetShowHolidays = false,
                widgetShowObservances = false, showHolyDaysInEvents = false,
            )
            preferences.write(hiddenSettings)
            val hidden = WidgetDataSource.load(context, now = now, includeMonth = true)
            assertEquals(hiddenSettings, hidden.settings)
            assertEquals(LocalDate.of(2026, 9, 24), hidden.today)
            assertNull(hidden.holidayTitle)
            assertTrue(hidden.current.items.isEmpty())
            assertFalse(hidden.monthDays.any { it.hasHoliday || it.hasObservance || it.hasPersonal })

            preferences.write(hiddenSettings.copy(widgetShowHolidays = true))
            val visible = WidgetDataSource.load(context, now = now, includeMonth = true)
            assertNotNull(visible.holidayTitle)
            assertTrue(visible.current.items.any { it.kind == EventKind.HOLIDAY })
            assertTrue(visible.monthDays.first { it.dayNumber == 24 }.hasHoliday)
        } finally {
            preferences.write(original)
        }
    }

    @Test fun paletteUsesAppearanceThemeAndAccent() {
        val light = WidgetPalette(AppSettings(theme = ThemeMode.LIGHT, accent = Accent.AMBER))
        val dark = WidgetPalette(AppSettings(theme = ThemeMode.DARK, accent = Accent.AMBER))
        assertEquals(accentColor(Accent.AMBER, false), light.accent.getColor(context))
        assertEquals(accentColor(Accent.AMBER, true), dark.accent.getColor(context))
        assertFalse(light.background.getColor(context) == dark.background.getColor(context))
    }

    @Test fun widgetWeekdayLabelsFollowCalendarColorSettings() {
        val colored = WidgetPalette(AppSettings(theme = ThemeMode.LIGHT,
            highlightWeekdayNames = true, highlightSunday = false))
        assertEquals(weekdayNameColor(DayOfWeek.MONDAY, false),
            colored.weekdayLabelColor(DayOfWeek.MONDAY).getColor(context))
        assertEquals(weekdayNameColor(DayOfWeek.SUNDAY, false),
            colored.weekdayLabelColor(DayOfWeek.SUNDAY).getColor(context))

        val sundayOnly = WidgetPalette(AppSettings(theme = ThemeMode.DARK,
            highlightWeekdayNames = false, highlightSunday = true))
        assertEquals(sundayOnly.secondary.getColor(context),
            sundayOnly.weekdayLabelColor(DayOfWeek.MONDAY).getColor(context))
        assertEquals(sundayOnly.holiday.getColor(context),
            sundayOnly.weekdayLabelColor(DayOfWeek.SUNDAY).getColor(context))

        val plain = WidgetPalette(AppSettings(theme = ThemeMode.LIGHT,
            highlightWeekdayNames = false, highlightSunday = false))
        assertEquals(plain.secondary.getColor(context),
            plain.weekdayLabelColor(DayOfWeek.SUNDAY).getColor(context))
    }

    @Test fun westernZodiacNameStaysEnglishInKhmerMode() {
        assertEquals("Virgo ♍️", WidgetStrings(context, false).zodiac(ZodiacSign.VIRGO))
        assertEquals("Virgo ♍️", WidgetStrings(context, true).zodiac(ZodiacSign.VIRGO))
        assertEquals("Libra ♎️", WidgetStrings(context, true).zodiac(ZodiacSign.LIBRA))
    }

    @Test fun languageChangeIsReadForAFreshProductivitySnapshot() {
        val preferences = AppPreferences(context)
        val original = preferences.read()
        val date = Instant.parse("2026-09-24T08:00:00Z")
        try {
            val settings = original.copy(
                khmer = false, todayTimeZone = TodayTimeZone.CAMBODIA,
                widgetShowHolidays = true, widgetShowPersonal = false,
                widgetShowObservances = false, showHolyDaysInEvents = false,
            )
            preferences.write(settings)
            val english = WidgetDataSource.load(context, now = date)
            preferences.write(settings.copy(khmer = true))
            val khmer = WidgetDataSource.load(context, now = date)

            assertFalse(english.settings.khmer)
            assertTrue(khmer.settings.khmer)
            assertNotNull(english.holidayTitle)
            assertNotNull(khmer.holidayTitle)
            assertNotEquals(english.holidayTitle, khmer.holidayTitle)
            assertEquals("Today", WidgetStrings(context, english.settings.khmer)(R.string.widget_today_heading))
            assertEquals("ថ្ងៃនេះ", WidgetStrings(context, khmer.settings.khmer)(R.string.widget_today_heading))
        } finally {
            preferences.write(original)
        }
    }

    @Test fun plannerReadsTwentyNineDaysAndRespectsTimezonePrivacyAndWidgetFilters() {
        val preferences = AppPreferences(context)
        val original = preferences.read()
        val event = CustomEvent(id = "planner-widget-test", title = "Breakfast meeting",
            date = LocalDate.of(2026, 1, 15), time = LocalTime.of(7, 15))
        CustomEventRepository(context).use { it.save(event) }
        val now = Instant.parse("2026-01-14T17:30:00Z") // 00:30 on Jan 15 in Cambodia.
        try {
            val settings = original.copy(khmer = false, todayTimeZone = TodayTimeZone.CAMBODIA,
                widgetShowPersonal = true, widgetHidePersonalDetails = false,
                widgetShowHolidays = false, widgetShowObservances = false,
                showHolyDaysInEvents = false)
            preferences.write(settings)
            val visible = WidgetDataSource.load(context, now = now, includePlanner = true)
            assertEquals(LocalDate.of(2026, 1, 15), visible.today)
            assertEquals(29, visible.plannerDays.size)
            assertEquals(LocalDate.of(2026, 1, 1), visible.plannerDays.first().date)
            assertEquals(LocalDate.of(2026, 1, 29), visible.plannerDays.last().date)
            val item = visible.plannerDays[14].items.single { it.eventId == "custom:planner-widget-test" }
            assertEquals("7:15", item.time)
            assertEquals("Breakfast meeting", item.title)

            preferences.write(settings.copy(widgetHidePersonalDetails = true))
            val private = WidgetDataSource.load(context, now = now, includePlanner = true)
            assertTrue(private.plannerDays[14].items.none { it.title.contains("Breakfast") || it.time == "7:15" })
            assertTrue(private.plannerDays[14].items.any { it.kind == EventKind.CUSTOM && it.eventId == null })

            preferences.write(settings.copy(widgetShowPersonal = false))
            val hidden = WidgetDataSource.load(context, now = now, includePlanner = true)
            assertTrue(hidden.plannerDays.all { it.items.none { item -> item.kind == EventKind.CUSTOM } })
        } finally {
            CustomEventRepository(context).use { it.delete(event.id) }
            preferences.write(original)
        }
    }
}
