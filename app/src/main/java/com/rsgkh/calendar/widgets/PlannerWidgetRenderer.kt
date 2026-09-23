// SPDX-License-Identifier: Apache-2.0
package com.rsgkh.calendar.widgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.text.TextPaint
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import androidx.compose.ui.graphics.toArgb
import com.rsgkh.calendar.MainActivity
import com.rsgkh.calendar.R
import kotlin.math.floor

/** A native ListView can open at today's position; Glance's LazyColumn always opens at the top. */
internal object PlannerWidgetRenderer {
    private const val MANUAL_REFRESH = "com.rsgkh.calendar.widgets.MANUAL_REFRESH"

    fun update(context: Context, id: Int) {
        val snapshot = WidgetDataSource.load(context, id, includePlanner = true)
        val days = snapshot.plannerDays
        if (days.isEmpty()) return
        val strings = WidgetStrings(context, snapshot.settings.khmer)
        val palette = WidgetPalette(snapshot.settings)
        val scale = snapshot.settings.widgetFontScale.multiplier
        val manager = AppWidgetManager.getInstance(context)
        val width = manager.getAppWidgetOptions(id).getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 280)
        val density = context.resources.displayMetrics.density
        fun textWidthDp(value: String, sizeSp: Float) =
            TextPaint().apply {
                textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sizeSp,
                    context.resources.displayMetrics)
            }.measureText(value) / density
        val rowTextSize = 11.5f * scale
        val dateTextWidth = days.maxOf { textWidthDp(strings.plannerDayLabel(it.date), rowTextSize) }
        val dateWidth = dateTextWidth + 5f * scale
        val chipsPerLine = floor(((width - 22f - dateWidth - 6f).coerceAtLeast(70f) + 4f) / (84f * scale + 4f))
            .toInt().coerceIn(1, 4)
        val packageName = context.packageName
        val views = RemoteViews(packageName, R.layout.widget_planner)

        views.setColorStateList(R.id.planner_root, "setBackgroundTintList",
            ColorStateList.valueOf(palette.background.getColor(context).toArgb()))
        val badgeTint = ColorStateList.valueOf(palette.surfaceVariant.getColor(context).toArgb())
        listOf(R.id.planner_period, R.id.planner_timezone, R.id.planner_refresh).forEach {
            views.setColorStateList(it, "setBackgroundTintList", badgeTint)
        }
        val period = "${strings.plannerDate(days.first().date)} - ${strings.plannerDate(days.last().date)}"
        val periodSize = 12.5f * scale
        val emojiOnly = width / scale < 340f
        val timezone = WidgetPolicy.timezoneLabel(snapshot.settings.todayTimeZone, strings.khmer, emojiOnly)
        val tzPadding = if (emojiOnly) 6f else 8f
        val timezoneWidth = textWidthDp(timezone, 12.5f * scale) + 2 * tzPadding
        val availablePeriodWidth = (width - 22f - 30f - 10f - timezoneWidth).coerceAtLeast(40f)
        val periodWidth = (textWidthDp(period, periodSize) + 16f).coerceAtMost(availablePeriodWidth)
        views.setViewLayoutWidth(R.id.planner_period, periodWidth, TypedValue.COMPLEX_UNIT_DIP)
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
        views.setInt(R.id.planner_refresh, "setColorFilter", palette.secondary.getColor(context).toArgb())
        views.setContentDescription(R.id.planner_refresh, strings(R.string.widget_refresh))

        val todayIntent = WidgetNavigation.dateIntent(context, id, snapshot.today)
        views.setOnClickPendingIntent(R.id.planner_period,
            PendingIntent.getActivity(context, id, todayIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
        val refreshIntent = Intent(context, WidgetRefreshReceiver::class.java).setAction(MANUAL_REFRESH)
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
        views.setOnClickPendingIntent(R.id.planner_refresh,
            PendingIntent.getBroadcast(context, id, refreshIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))

        val template = PendingIntent.getActivity(context, id, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE)
        views.setPendingIntentTemplate(R.id.planner_list, template)
        val collection = RemoteViews.RemoteCollectionItems.Builder().setHasStableIds(true)
        val slots = intArrayOf(R.id.planner_event_1, R.id.planner_event_2,
            R.id.planner_event_3, R.id.planner_event_4)
        var rowPosition = 0
        var todayPosition = 14
        days.forEach { day ->
            val dayIntent = WidgetNavigation.dateIntent(context, id, day.date)
            if (day.date == snapshot.today) todayPosition = rowPosition
            val groups = day.items.chunked(chipsPerLine).ifEmpty { listOf(emptyList()) }
            groups.forEachIndexed { groupIndex, group ->
                val row = RemoteViews(packageName, R.layout.widget_planner_row)
                row.setInt(R.id.planner_row, "setBackgroundColor",
                    if (day.date == snapshot.today) palette.plannerToday.getColor(context).toArgb() else Color.TRANSPARENT)
                row.setTextViewText(R.id.planner_date,
                    if (groupIndex == 0) strings.plannerDayLabel(day.date) else "")
                row.setViewLayoutWidth(R.id.planner_date, dateWidth, TypedValue.COMPLEX_UNIT_DIP)
                row.setViewLayoutWidth(R.id.planner_divider_spacer, dateWidth, TypedValue.COMPLEX_UNIT_DIP)
                if (day.date == days.last().date && groupIndex == groups.lastIndex) {
                    row.setViewVisibility(R.id.planner_divider_row, View.GONE)
                }
                row.setTextColor(R.id.planner_date, palette.text.getColor(context).toArgb())
                row.setTextViewTextSize(R.id.planner_date, TypedValue.COMPLEX_UNIT_SP, rowTextSize)
                row.setOnClickFillInIntent(R.id.planner_row, dayIntent)
                row.setOnClickFillInIntent(R.id.planner_date, dayIntent)
                row.setOnClickFillInIntent(R.id.planner_blank, dayIntent)
                group.forEachIndexed { itemIndex, item ->
                    val slot = slots[itemIndex]
                    val title = WidgetPolicy.plannerTitle(item.title, strings.khmer)
                    row.setViewVisibility(slot, View.VISIBLE)
                    row.setTextViewText(slot, listOfNotNull(item.time, title).joinToString(" "))
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

        // ListView scrolls until the target is visible at the bottom. Two later rows
        // keep today away from that edge at the default 4×2 size.
        views.setScrollPosition(R.id.planner_list, todayPosition + 2)
        manager.updateAppWidget(id, views)
        manager.partiallyUpdateAppWidget(id, RemoteViews(packageName, R.layout.widget_planner).apply {
            setScrollPosition(R.id.planner_list, todayPosition + 2)
        })
    }
}
