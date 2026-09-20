// SPDX-License-Identifier: Apache-2.0
package com.rsgkh.calendar.widgets

import com.rsgkh.calendar.data.CalendarEvent
import com.rsgkh.calendar.data.TodayTimeZone
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale
import kotlin.math.floor

/** Pure policies; never approximate a civil day as 24 hours (DST can change its length). */
object WidgetPolicy {
    /** Widget font zoom is capped at 130% even if the app font scale is set to 140% or 150%. */
    const val MAX_WIDGET_FONT_SCALE = 1.30f

    fun effectiveFontScale(multiplier: Float): Float =
        multiplier.coerceAtMost(MAX_WIDGET_FONT_SCALE)

    /**
     * Formats timezone text for widget chips. When space is constrained, collapses to only emoji
     * to prevent truncation ellipses (e.g. "🌐" instead of "🌐 ក្នុងតំបន់").
     */
    fun timezoneLabel(timeZone: TodayTimeZone, khmer: Boolean, emojiOnly: Boolean = false): String = when (timeZone) {
        TodayTimeZone.CAMBODIA -> if (emojiOnly) "🇰🇭" else "🇰🇭 " + (if (khmer) "កម្ពុជា" else "Cambodia")
        TodayTimeZone.LOCAL -> if (emojiOnly) "🌐" else "🌐 " + (if (khmer) "ក្នុងតំបន់" else "Local")
    }

    fun nextMidnight(now: Instant, zone: ZoneId): Instant =
        now.atZone(zone).toLocalDate().plusDays(1).atStartOfDay(zone).toInstant()

    fun window(today: LocalDate): List<LocalDate> =
        listOf(today.minusDays(1), today, today.plusDays(1))

    fun sorted(events: List<CalendarEvent>, khmer: Boolean): List<CalendarEvent> =
        events.distinctBy { it.key }.sortedWith(
            compareBy<CalendarEvent> { if (it.time == null) 0 else 1 }
                .thenBy { it.time }
                .thenBy { it.kind.ordinal }
                .thenBy { it.title(khmer).lowercase(Locale.ROOT) }
                .thenBy { it.id },
        )

    /**
     * Budget based on launcher-provided dp, not guessed grid cells. At accessibility sizes
     * show fewer fields, never squeeze text down. These are conservative estimates; OEM
     * fonts still need device testing. Fixed line limits provide the final safety net.
     */
    fun layout(widthDp: Float, heightDp: Float, textScale: Float): WidgetLayout {
        val scale = textScale.coerceAtLeast(1f)
        val available = (heightDp - 24f).coerceAtLeast(0f)
        val tiny = available < 110f * scale || widthDp < 245f * scale
        val expanded = !tiny && available >= 190f * scale
        val previewTitles = !tiny && available >= 155f * scale
        val header = 40f * scale
        val footer = (if (previewTitles) 68f else 24f) * scale
        // Reserve space for "+N more" even on a busy day.
        val rows = floor((available - header - footer - 18f * scale) / (29f * scale))
            .toInt().coerceIn(0, 3)
        return WidgetLayout(
            tiny = tiny,
            expanded = expanded,
            previewTitles = previewTitles,
            eventRows = if (tiny) 0 else rows,
            lunarLines = if (expanded) 2 else 1,
        )
    }
}

data class WidgetLayout(
    val tiny: Boolean,
    val expanded: Boolean,
    val previewTitles: Boolean,
    val eventRows: Int,
    val lunarLines: Int,
)
