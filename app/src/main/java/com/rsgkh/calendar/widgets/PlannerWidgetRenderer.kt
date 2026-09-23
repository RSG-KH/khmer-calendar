// SPDX-License-Identifier: Apache-2.0
package com.rsgkh.calendar.widgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Color
import android.text.TextPaint
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import androidx.compose.ui.graphics.toArgb
import com.rsgkh.calendar.MainActivity
import com.rsgkh.calendar.R
import kotlin.math.roundToInt
import kotlinx.coroutines.delay

/** A native ListView can open at today's position; Glance's LazyColumn always opens at the top. */
internal object PlannerWidgetRenderer {
    suspend fun update(context: Context, id: Int) {
        val snapshot = WidgetDataSource.load(context, id, includePlanner = true)
        val days = snapshot.plannerDays
        if (days.isEmpty()) return
        val strings = WidgetStrings(context, snapshot.settings.khmer)
        val palette = WidgetPalette(snapshot.settings)
        val scale = snapshot.settings.widgetFontScale.multiplier
        val manager = AppWidgetManager.getInstance(context)
        val options = manager.getAppWidgetOptions(id)
        val landscape = context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        // Widget options contain both orientations. The minimum height can describe the
        // landscape widget even while its portrait version spans several grid rows.
        val width = options.getInt(if (landscape) AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH
            else AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0).takeIf { it > 0 } ?: 280
        val height = options.getInt(if (landscape) AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT
            else AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 0).takeIf { it > 0 } ?: 140
        val density = context.resources.displayMetrics.density
        fun textWidthDp(value: String, sizeSp: Float) =
            TextPaint().apply {
                textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sizeSp,
                    context.resources.displayMetrics)
            }.measureText(value) / density
        val rowTextSize = 11.5f * scale
        val rowFontHeightDp = TextPaint().apply {
            textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, rowTextSize,
                context.resources.displayMetrics)
        }.fontMetrics.run { (bottom - top) / density }
        val weekdayWidth = days.maxOf { textWidthDp(strings.plannerWeekday(it.date), rowTextSize) } + 6f * scale
        val dateWidth = days.maxOf { textWidthDp(strings.plannerListDate(it.date), rowTextSize) } + 8f * scale
        val eventGapWidth = 2f * scale
        val eventsStartWidth = weekdayWidth + dateWidth + eventGapWidth
        val weekDividerColor = palette.secondary.getColor(context).copy(alpha = 0.38f).toArgb()
        val regularDividerColor = Color.argb(0x22, 0x6d, 0x74, 0x85)
        // Outer and row padding consume 22dp and 8dp. TextViews add 10dp
        // horizontal padding and a 4dp end margin to every visible chip.
        val eventSpaceWidth = (width - 30f - eventsStartWidth).coerceAtLeast(0f)
        fun eventLabel(item: WidgetItem) = listOfNotNull(item.time,
            WidgetPolicy.plannerTitle(item.title, strings.khmer)).joinToString(" ")
        fun eventWidthDp(item: WidgetItem) = textWidthDp(eventLabel(item), rowTextSize) + 14f
        val packageName = context.packageName
        val views = RemoteViews(packageName, R.layout.widget_planner)

        views.setColorStateList(R.id.planner_root, "setBackgroundTintList",
            ColorStateList.valueOf(palette.background.getColor(context).toArgb()))
        snapshot.details?.let { details ->
            // The weighted layout sizes against the host's actual bounds; fitEnd shows
            // the full image at 60% of the available fit and anchors it bottom right.
            views.setImageViewResource(R.id.planner_animal, zodiacDrawable(details.animalYear, compact = true))
            views.setInt(R.id.planner_animal, "setColorFilter", palette.accent.getColor(context).toArgb())
            views.setInt(R.id.planner_animal, "setImageAlpha", (resolveAnimalAlpha(context, snapshot) * 255).roundToInt())
        }
        val badgeTint = ColorStateList.valueOf(palette.surfaceVariant.getColor(context).toArgb())
        listOf(R.id.planner_period, R.id.planner_timezone).forEach {
            views.setColorStateList(it, "setBackgroundTintList", badgeTint)
        }
        val period = "${strings.plannerDate(days.first().date)} - ${strings.plannerDate(days.last().date)}"
        val periodSize = 12.5f * scale
        val emojiOnly = width / scale < 340f
        val timezone = WidgetPolicy.timezoneLabel(snapshot.settings.todayTimeZone, strings.khmer, emojiOnly)
        val tzPadding = if (emojiOnly) 6f else 8f
        views.setTextViewText(R.id.planner_period, period)
        views.setTextColor(R.id.planner_period, palette.text.getColor(context).toArgb())
        views.setTextViewTextSize(R.id.planner_period, TypedValue.COMPLEX_UNIT_SP, periodSize)

        views.setTextViewText(R.id.planner_timezone, timezone)
        val tzHorizontalPadding = (tzPadding * density).toInt()
        val tzVerticalPadding = (3f * density).toInt()
        views.setViewPadding(R.id.planner_timezone, tzHorizontalPadding, tzVerticalPadding,
            tzHorizontalPadding, tzVerticalPadding)
        views.setTextColor(R.id.planner_timezone, palette.secondary.getColor(context).toArgb())
        views.setTextViewTextSize(R.id.planner_timezone, TypedValue.COMPLEX_UNIT_SP, 12.5f * scale)
        WidgetRefreshControl.style(views, R.id.planner_refresh, context, id,
            strings(R.string.widget_refresh), palette)

        val todayIntent = WidgetNavigation.dateIntent(context, id, snapshot.today)
        views.setOnClickPendingIntent(R.id.planner_period,
            PendingIntent.getActivity(context, id, todayIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
        val template = PendingIntent.getActivity(context, id, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE)
        views.setPendingIntentTemplate(R.id.planner_list, template)
        val collection = RemoteViews.RemoteCollectionItems.Builder().setHasStableIds(true)
        val slots = intArrayOf(R.id.planner_event_1, R.id.planner_event_2,
            R.id.planner_event_3, R.id.planner_event_4)
        var rowPosition = 0
        var todayPosition = 14
        val rowHeightsDp = mutableListOf<Float>()
        days.forEach { day ->
            val dayIntent = WidgetNavigation.dateIntent(context, id, day.date)
            if (day.date == snapshot.today) todayPosition = rowPosition
            val groups = WidgetPolicy.plannerChipRows(day.items, eventSpaceWidth, ::eventWidthDp)
            groups.forEachIndexed { groupIndex, group ->
                // Each row is at least 27dp. A text chip adds 8dp vertical padding and
                // the row adds 2dp; larger fonts can therefore make event rows taller.
                rowHeightsDp += maxOf(27f, rowFontHeightDp + if (group.isEmpty()) 2f else 10f)
                val row = RemoteViews(packageName, R.layout.widget_planner_row)
                row.setColorStateList(R.id.planner_row, "setBackgroundTintList", ColorStateList.valueOf(
                    if (day.date == snapshot.today) palette.plannerToday.getColor(context).toArgb() else Color.TRANSPARENT))
                row.setTextViewText(R.id.planner_weekday,
                    if (groupIndex == 0) strings.plannerWeekday(day.date) else "")
                row.setTextViewText(R.id.planner_date,
                    if (groupIndex == 0) strings.plannerListDate(day.date) else "")
                row.setViewLayoutWidth(R.id.planner_weekday, weekdayWidth, TypedValue.COMPLEX_UNIT_DIP)
                row.setViewLayoutWidth(R.id.planner_date, dateWidth, TypedValue.COMPLEX_UNIT_DIP)
                row.setViewLayoutWidth(R.id.planner_event_gap, eventGapWidth, TypedValue.COMPLEX_UNIT_DIP)
                row.setViewLayoutWidth(R.id.planner_divider_spacer, eventsStartWidth, TypedValue.COMPLEX_UNIT_DIP)
                val lastRowForDay = groupIndex == groups.lastIndex
                val weekBoundary = lastRowForDay &&
                    WidgetPolicy.plannerWeekBoundaryAfter(day.date, snapshot.settings.mondayFirst)
                if (day.date == days.last().date && lastRowForDay) {
                    row.setViewVisibility(R.id.planner_divider_row, View.GONE)
                } else if (weekBoundary) {
                    row.setViewVisibility(R.id.planner_divider_row, View.VISIBLE)
                    row.setViewVisibility(R.id.planner_divider_spacer, View.GONE)
                    row.setViewLayoutHeight(R.id.planner_divider_row, 2f, TypedValue.COMPLEX_UNIT_DIP)
                    row.setViewLayoutHeight(R.id.planner_divider_line, 2f, TypedValue.COMPLEX_UNIT_DIP)
                } else {
                    row.setViewVisibility(R.id.planner_divider_row, View.VISIBLE)
                    row.setViewVisibility(R.id.planner_divider_spacer, View.VISIBLE)
                    row.setViewLayoutHeight(R.id.planner_divider_row, 1f, TypedValue.COMPLEX_UNIT_DIP)
                    row.setViewLayoutHeight(R.id.planner_divider_line, 1f, TypedValue.COMPLEX_UNIT_DIP)
                }
                row.setInt(R.id.planner_divider_line, "setBackgroundColor",
                    if (weekBoundary) weekDividerColor else regularDividerColor)
                row.setTextColor(R.id.planner_weekday,
                    palette.weekdayLabelColor(day.date.dayOfWeek).getColor(context).toArgb())
                row.setTextColor(R.id.planner_date, palette.text.getColor(context).toArgb())
                row.setTextViewTextSize(R.id.planner_weekday, TypedValue.COMPLEX_UNIT_SP, rowTextSize)
                row.setTextViewTextSize(R.id.planner_date, TypedValue.COMPLEX_UNIT_SP, rowTextSize)
                row.setOnClickFillInIntent(R.id.planner_row, dayIntent)
                row.setOnClickFillInIntent(R.id.planner_weekday, dayIntent)
                row.setOnClickFillInIntent(R.id.planner_date, dayIntent)
                row.setOnClickFillInIntent(R.id.planner_event_gap, dayIntent)
                row.setOnClickFillInIntent(R.id.planner_blank, dayIntent)
                // ListView can reapply this RemoteViews to a row from another date.
                // Hide every unused slot so old chips and font sizes cannot survive.
                slots.forEachIndexed { index, slot ->
                    row.setViewVisibility(slot, if (index < group.size) View.VISIBLE else View.GONE)
                }
                group.forEachIndexed { itemIndex, item ->
                    val slot = slots[itemIndex]
                    row.setTextViewText(slot, eventLabel(item))
                    row.setTextColor(slot, palette.event(item.kind).getColor(context).toArgb())
                    row.setTextViewTextSize(slot, TypedValue.COMPLEX_UNIT_SP, rowTextSize)
                    row.setColorStateList(slot, "setBackgroundTintList",
                        ColorStateList.valueOf(palette.eventBackground(item.kind).getColor(context).toArgb()))
                    val eventIntent = WidgetNavigation.dateIntent(context, id, day.date, item.eventId)
                    row.setOnClickFillInIntent(slot, eventIntent)
                }
                collection.addItem(day.date.toEpochDay() * 1000 + groupIndex, row)
                rowPosition++
            }
        }
        views.setRemoteAdapter(R.id.planner_list, collection.build())

        // ListView brings the target to its bottom edge. Choose a later row based on
        // the visible list height so today opens near the top at 4×2, 4×3 and larger.
        val listHeightDp = (height - 51f).coerceAtLeast(0f) // outer padding + header + list gap
        val scrollPosition = WidgetPolicy.plannerScrollTarget(todayPosition, rowHeightsDp, listHeightDp)
        views.setScrollPosition(R.id.planner_list, scrollPosition)
        manager.updateAppWidget(id, views)
        // The target after today positions it near the top when starting above it.
        // From the bottom that target may already be visible, making the first scroll
        // a no-op. Once the collection settles, ensure today's own row is visible.
        delay(400)
        manager.partiallyUpdateAppWidget(id, RemoteViews(packageName, R.layout.widget_planner).apply {
            setScrollPosition(R.id.planner_list, todayPosition)
        })
    }
}
