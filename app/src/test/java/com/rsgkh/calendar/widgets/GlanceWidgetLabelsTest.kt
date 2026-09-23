// SPDX-License-Identifier: Apache-2.0
package com.rsgkh.calendar.widgets

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.rsgkh.calendar.data.AppSettings
import com.rsgkh.calendar.BuildConfig
import com.rsgkh.calendar.domain.KhmerDateDetails
import com.rsgkh.calendar.domain.ganzhiAnimalLabel
import com.rsgkh.calendar.engine.ChineseZodiacCalculator
import java.time.LocalDate
import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class GlanceWidgetLabelsTest {
    private val context: Context get() = ApplicationProvider.getApplicationContext()

    @Test fun threeDateTileLinesFitAndFollowFontSizeAtBothHeights() {
        assertEquals(0.8f, glanceDateTileScale(60f, 0.8f, 1f), 0.001f)
        assertEquals(0.9f, glanceDateTileScale(60f, 0.9f, 1f), 0.001f)
        assertEquals(1f, glanceDateTileScale(60f, 1f, 1f), 0.001f)
        val shortLarge = glanceDateTileScale(60f, 2f, 1f)
        assertTrue(shortLarge * 41f * 1.25f <= 52.001f)
        assertEquals(2f, glanceDateTileScale(140f, 2f, 1f), 0.001f)
        val largeSystemText = glanceDateTileScale(60f, 2f, 1.3f)
        assertTrue(largeSystemText * 1.3f * 41f * 1.25f <= 52.001f)
    }

    @Test fun versionFallbackUsesAppLanguage() {
        assertEquals("Khmer Calendar ${BuildConfig.VERSION_NAME}", glanceAppVersionLabel(false))
        assertEquals("ប្រតិទិនខ្មែរ ${BuildConfig.VERSION_NAME}", glanceAppVersionLabel(true))
    }

    @Test fun compactLabelsOmitGregorianYearAndPutTraditionalYearOnThirdLine() {
        val snapshot = snapshot(LocalDate.of(2026, 9, 23), AppSettings(khmer = false))
        val labels = glanceLabels(snapshot, WidgetStrings(context, false), compact = true)
        assertTrue(labels.solar.startsWith("Sep · "))
        assertFalse(labels.solar.contains("26"))
        assertTrue(labels.solar.endsWith("2570"))
        assertFalse(labels.solar.contains("BE"))
        assertFalse(labels.lunar.contains("Horse"))
        assertTrue(labels.traditional.contains("Horse"))
        assertFalse(labels.traditional.contains("Year of"))
        assertTrue(labels.traditional.contains("Atthasak"))
        assertNull(labels.zodiac)
        assertNull(labels.ganzhi)
        assertNull(labels.clash)
        assertFalse(labels.showAppVersion)

        val khmer = glanceLabels(snapshot.copy(settings = snapshot.settings.copy(khmer = true)),
            WidgetStrings(context, true), compact = true)
        assertFalse(khmer.solar.contains("ខែ"))
        assertFalse(khmer.solar.contains("២៦"))
        assertFalse(khmer.solar.contains("ព.ស."))
        assertTrue(khmer.solar.contains("២៥៧០"))
        assertTrue(khmer.traditional.contains("អដ្ឋស័ក"))

        val expanded = glanceLabels(snapshot, WidgetStrings(context, false), compact = false)
        assertTrue(expanded.solar.contains("2026"))
        assertTrue(expanded.lunar.contains("Horse · Atthasak"))
        assertFalse(expanded.lunar.contains("Year of"))
        assertTrue(expanded.lunar.contains("Atthasak"))
        assertNotNull(expanded.zodiac)
        assertNotNull(expanded.ganzhi)
        assertFalse(expanded.showAppVersion)
        val date = snapshot.today
        val pillars = listOf(
            ChineseZodiacCalculator.getYearPillar(date.year, date.monthValue, date.dayOfMonth),
            ChineseZodiacCalculator.getMonthPillar(date.year, date.monthValue, date.dayOfMonth),
            snapshot.details!!.ganzhiDay,
        )
        assertTrue(expanded.ganzhi == pillars.joinToString("") {
            it.branch.ganzhiAnimalLabel(false, useEmoji = true)
        })
        assertTrue(expanded.clash == pillars.joinToString("") {
            it.clashBranch.ganzhiAnimalLabel(false, useEmoji = true)
        })
    }

    @Test fun badgeVisibilityFollowsHolyDayAndAstrologySettings() {
        val holyDate = (1..30).map { LocalDate.of(2026, 9, it) }
            .first { KhmerDateDetails.fromGregorian(it).lunar.isHolyDay }
        val base = snapshot(holyDate, AppSettings(khmer = false,
            showHolyDaysInCalendar = false, showHolyDaysInEvents = false,
            showWesternZodiac = false, showGanzhi = false))
        val strings = WidgetStrings(context, false)
        val hidden = glanceLabels(base, strings, compact = false)
        assertFalse(hidden.holy)
        assertNull(hidden.zodiac)
        assertNull(hidden.ganzhi)
        assertNull(hidden.clash)
        assertTrue(hidden.showAppVersion)
        val ordinaryDate = (1..30).map { LocalDate.of(2026, 9, it) }
            .first { !KhmerDateDetails.fromGregorian(it).lunar.isHolyDay }
        val ordinary = glanceLabels(snapshot(ordinaryDate, base.settings), strings, compact = false)
        assertTrue(ordinary.showAppVersion)
        assertFalse(glanceLabels(snapshot(ordinaryDate,
            base.settings.copy(showWesternZodiac = true)), strings, compact = false).showAppVersion)
        assertFalse(glanceLabels(snapshot(ordinaryDate,
            base.settings.copy(showGanzhi = true)), strings, compact = false).showAppVersion)
        val calendarHoly = glanceLabels(base.copy(settings = base.settings.copy(showHolyDaysInCalendar = true)),
            strings, compact = false)
        assertTrue(calendarHoly.holy)
        assertFalse(calendarHoly.showAppVersion)
        val eventsHoly = glanceLabels(base.copy(settings = base.settings.copy(showHolyDaysInEvents = true)),
            strings, compact = false)
        assertTrue(eventsHoly.holy)
        assertFalse(eventsHoly.showAppVersion)
        val unavailable = base.copy(details = null)
        assertTrue(glanceLabels(unavailable, strings, compact = false).showAppVersion)
        assertTrue(glanceLabels(unavailable, strings, compact = true).showAppVersion)
    }

    private fun snapshot(date: LocalDate, settings: AppSettings): WidgetSnapshot {
        val day = WidgetDay(date, emptyList(), 0)
        return WidgetSnapshot(settings, date, KhmerDateDetails.fromGregorian(date), day, day, day, null)
    }
}
