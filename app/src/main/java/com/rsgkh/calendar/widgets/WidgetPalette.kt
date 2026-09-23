// SPDX-License-Identifier: Apache-2.0
package com.rsgkh.calendar.widgets

import androidx.compose.ui.graphics.Color
import androidx.glance.color.ColorProvider as DayNightColor
import androidx.glance.unit.ColorProvider
import com.rsgkh.calendar.data.AppSettings
import com.rsgkh.calendar.data.EventKind
import com.rsgkh.calendar.data.ThemeMode
import com.rsgkh.calendar.ui.accentColor
import com.rsgkh.calendar.ui.weekdayNameColor
import java.time.DayOfWeek

/** Day/night providers using translucent matte foreground containers so the background animal is visible. */
internal class WidgetPalette(settings: AppSettings) {
    private val mode = settings.theme
    private val lightAccent = accentColor(settings.accent, false)
    private val darkAccent = accentColor(settings.accent, true)

    // Calendar container background from the app
    val background = token(
        Color.White,
        Color(0xFF1A1D24),
    )

    // Semi-transparent matte for foreground elements (so background animal shines through)
    val surfaceVariant = token(
        Color(0xFFF0F2F7).copy(alpha = 0.70f),
        Color(0xFF252932).copy(alpha = 0.55f),
    )

    val text = token(Color(0xFF222632), Color(0xFFE9EAF0))
    val secondary = token(Color(0xFF6D7485), Color(0xFFA2A8B7))
    val accent = token(lightAccent, darkAccent)
    val plannerToday = token(lightAccent.copy(alpha = 0.50f), darkAccent.copy(alpha = 0.50f))

    // Content on top of a filled accent cell (the today highlight). Light accents are deep
    // enough for white; dark accents are pastels, so use dark text there.
    val onAccent = token(Color.White, Color(0xFF1A1D24))

    // Holiday colors with matte background
    val holiday = token(Color(0xFFB34B5D), Color(0xFFEEA0A7))
    val holidayBackground = token(
        Color(0xFFFDE8EF).copy(alpha = 0.75f),
        Color(0xFF382329).copy(alpha = 0.60f),
    )

    // Holy Day colors with matte background
    val holy = token(Color(0xFF99601D), Color(0xFFECAF80))
    val holyBackground = token(
        Color(0xFFFFF3E4).copy(alpha = 0.75f),
        Color(0xFF382C24).copy(alpha = 0.60f),
    )

    // Matches the app's custom-event red (CustomEventRed / CustomEventRedDark in CalendarApp.kt).
    val personal = token(Color(0xFFE53935), Color(0xFFFF5252))
    val personalBackground = token(
        Color(0xFFE53935).copy(alpha = 0.16f), Color(0xFFFF5252).copy(alpha = 0.20f),
    )
    val observanceBackground = token(
        lightAccent.copy(alpha = 0.16f), darkAccent.copy(alpha = 0.20f),
    )

    fun eventBackground(kind: EventKind): ColorProvider = when (kind) {
        EventKind.HOLIDAY -> holidayBackground
        EventKind.OBSERVANCE -> observanceBackground
        EventKind.HOLY_DAY -> holyBackground
        EventKind.CUSTOM -> personalBackground
    }

    fun weekdayColor(day: DayOfWeek): ColorProvider =
        token(weekdayNameColor(day, false), weekdayNameColor(day, true))

    fun event(kind: EventKind): ColorProvider = when (kind) {
        EventKind.HOLIDAY -> holiday
        EventKind.OBSERVANCE -> accent
        EventKind.HOLY_DAY -> holy
        EventKind.CUSTOM -> personal
    }

    private fun token(day: Color, night: Color): ColorProvider = when (mode) {
        ThemeMode.SYSTEM -> DayNightColor(day = day, night = night)
        ThemeMode.LIGHT -> ColorProvider(day)
        ThemeMode.DARK -> ColorProvider(night)
    }
}
