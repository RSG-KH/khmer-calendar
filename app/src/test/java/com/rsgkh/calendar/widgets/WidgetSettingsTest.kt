// SPDX-License-Identifier: Apache-2.0
package com.rsgkh.calendar.widgets

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.rsgkh.calendar.R
import com.rsgkh.calendar.data.Accent
import com.rsgkh.calendar.data.AppPreferences
import com.rsgkh.calendar.data.AppSettings
import com.rsgkh.calendar.data.EventKind
import com.rsgkh.calendar.data.FontScale
import com.rsgkh.calendar.data.ThemeMode
import com.rsgkh.calendar.data.TodayTimeZone
import com.rsgkh.calendar.domain.ZodiacSign
import com.rsgkh.calendar.ui.accentColor
import java.time.Instant
import java.time.LocalDate
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
            val hidden = WidgetDataSource.load(context, now = now)
            assertEquals(hiddenSettings, hidden.settings)
            assertEquals(LocalDate.of(2026, 9, 24), hidden.today)
            assertNull(hidden.holidayTitle)
            assertTrue(hidden.current.items.isEmpty())
            assertFalse(hidden.monthDays.any { it.hasHoliday || it.hasObservance || it.hasPersonal })

            preferences.write(hiddenSettings.copy(widgetShowHolidays = true))
            val visible = WidgetDataSource.load(context, now = now)
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

    @Test fun zodiacNameUsesTheAppLanguage() {
        assertEquals("Virgo ♍️", WidgetStrings(context, false).zodiac(ZodiacSign.VIRGO))
        assertEquals("កញ្ញា ♍️", WidgetStrings(context, true).zodiac(ZodiacSign.VIRGO))
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
}
