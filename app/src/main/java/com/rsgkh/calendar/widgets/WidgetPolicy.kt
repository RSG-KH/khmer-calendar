// SPDX-License-Identifier: Apache-2.0
package com.rsgkh.calendar.widgets

import com.rsgkh.calendar.data.CalendarEvent
import com.rsgkh.calendar.data.TodayTimeZone
import java.time.Instant
import java.time.DayOfWeek
import java.time.LocalDate
import java.text.BreakIterator
import java.time.ZoneId
import java.util.Locale
import kotlin.math.abs
import kotlin.math.floor

/** Pure policies; never approximate a civil day as 24 hours (DST can change its length). */
object WidgetPolicy {

    /**
     * Month widget card width ceiling: the calendar card never gets wider than 1.25x its height,
     * in portrait and landscape alike. The 456dp fallback only applies when the launcher reports
     * no usable height (0dp) and a default width is needed.
     */
    fun monthCardMaxWidth(heightDp: Float): Float = if (heightDp > 0f) heightDp * 1.25f else 456f

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

    fun plannerWindow(today: LocalDate): List<LocalDate> =
        (-14L..14L).map(today::plusDays)

    /** A full-width planner divider belongs after the final day of the selected week. */
    fun plannerWeekBoundaryAfter(date: LocalDate, mondayFirst: Boolean): Boolean =
        date.plusDays(1).dayOfWeek == if (mondayFirst) DayOfWeek.MONDAY else DayOfWeek.SUNDAY

    /** ListView places the requested row at the bottom; keep today near the top. */
    fun plannerScrollTarget(todayPosition: Int, rowHeightsDp: List<Float>, listHeightDp: Float): Int {
        if (rowHeightsDp.isEmpty()) return 0
        val today = todayPosition.coerceIn(rowHeightsDp.indices)
        val desiredSpan = (listHeightDp - rowHeightsDp[today] / 2f).coerceAtLeast(0f)
        var span = 0f
        var target = today
        var bestDifference = Float.POSITIVE_INFINITY
        for (position in today..rowHeightsDp.lastIndex) {
            span += rowHeightsDp[position].coerceAtLeast(1f)
            val difference = abs(span - desiredSpan)
            if (difference < bestDifference) {
                bestDifference = difference
                target = position
            }
            if (span >= desiredSpan) break
        }
        return target
    }

    /** Planner puts appointments first, then untimed events. */
    fun plannerSorted(events: List<CalendarEvent>, khmer: Boolean): List<CalendarEvent> =
        events.distinctBy { it.key }.sortedWith(
            compareBy<CalendarEvent> { it.time == null }
                .thenBy { it.time }
                .thenBy { it.kind.ordinal }
                .thenBy { it.title(khmer).lowercase(Locale.ROOT) }
                .thenBy { it.id },
        )

    fun plannerTitle(title: String, khmer: Boolean): String {
        val breaks = BreakIterator.getCharacterInstance(Locale.forLanguageTag(if (khmer) "km" else "en"))
        breaks.setText(title)
        var end = breaks.first()
        repeat(6) { end = breaks.next().takeIf { it != BreakIterator.DONE } ?: return title }
        return if (breaks.next() == BreakIterator.DONE) title else title.substring(0, end) + "..."
    }

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
