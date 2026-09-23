// SPDX-License-Identifier: Apache-2.0
package com.rsgkh.calendar.widgets

import android.content.Context
import android.content.res.Configuration
import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.AndroidRemoteViews
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.rsgkh.calendar.R
import com.rsgkh.calendar.data.EventKind
import com.rsgkh.calendar.data.ThemeMode
import com.rsgkh.calendar.data.TodayTimeZone
import com.rsgkh.calendar.domain.LunarDate
import com.rsgkh.calendar.domain.Zodiac
import com.rsgkh.calendar.i18n.CalendarWords
import com.rsgkh.calendar.i18n.L
import java.time.DayOfWeek
import java.time.YearMonth

@DrawableRes
internal fun zodiacDrawable(animalYear: Int, compact: Boolean = true): Int = when (Math.floorMod(animalYear, 12)) {
    0 -> if (compact) R.drawable.zodiac_rat_400 else R.drawable.zodiac_rat
    1 -> if (compact) R.drawable.zodiac_ox_400 else R.drawable.zodiac_ox
    2 -> if (compact) R.drawable.zodiac_tiger_400 else R.drawable.zodiac_tiger
    3 -> if (compact) R.drawable.zodiac_rabbit_400 else R.drawable.zodiac_rabbit
    4 -> if (compact) R.drawable.zodiac_dragon_400 else R.drawable.zodiac_dragon
    5 -> if (compact) R.drawable.zodiac_snake_400 else R.drawable.zodiac_snake
    6 -> if (compact) R.drawable.zodiac_horse_400 else R.drawable.zodiac_horse
    7 -> if (compact) R.drawable.zodiac_goat_400 else R.drawable.zodiac_goat
    8 -> if (compact) R.drawable.zodiac_monkey_400 else R.drawable.zodiac_monkey
    9 -> if (compact) R.drawable.zodiac_rooster_400 else R.drawable.zodiac_rooster
    10 -> if (compact) R.drawable.zodiac_dog_400 else R.drawable.zodiac_dog
    11 -> if (compact) R.drawable.zodiac_pig_400 else R.drawable.zodiac_pig
    else -> if (compact) R.drawable.zodiac_rat_400 else R.drawable.zodiac_rat
}

internal fun resolveAnimalAlpha(context: Context, snapshot: WidgetSnapshot): Float {
    val isNight = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
    val isDark = if (snapshot.settings.theme == ThemeMode.SYSTEM) isNight else snapshot.settings.theme == ThemeMode.DARK
    return if (isDark) 0.08f else 0.09f
}

private fun animalEmoji(year: Int): String = when (Math.floorMod(year, 12)) {
    0 -> "🐀"
    1 -> "🐂"
    2 -> "🐅"
    3 -> "🐇"
    4 -> "🐉"
    5 -> "🐍"
    6 -> "🐴"
    7 -> "🐐"
    8 -> "🐒"
    9 -> "🐓"
    10 -> "🐕"
    11 -> "🐖"
    else -> "🐾"
}

private fun moonEmoji(lunar: LunarDate?): String {
    if (lunar == null) return "🌙 "
    return when {
        lunar.day == 15 && lunar.waxing -> "🌕 "
        lunar.day == 15 && !lunar.waxing -> "🌑 "
        lunar.waxing -> if (lunar.day >= 8) "🌔 " else "🌓 "
        else -> if (lunar.day >= 8) "🌘 " else "🌗 "
    }
}

/**
 * 2nd Widget (Today Details):
 * Header: Animal Year & Sak + BE Year chip + Action buttons.
 * Body: 2 Big Columns expanding to bottom edge:
 *   - Left Column:
 *       - Left sub-column: Weekday (without "tngai") on top + Big day number below
 *       - Right sub-column: 1 row per item in order:
 *           1. Year
 *           2. Month
 *           3. Lunar day [emoji]
 *           4. Lunar month
 *           5. Holy day [emoji] (if applicable)
 *           6. Zodiac [emoji]
 *   - Right Column: Today's Events list (scrollable LazyColumn)
 */
@Composable
internal fun ProductivityWidgetContent(snapshot: WidgetSnapshot, id: Int) {
    val context = LocalContext.current
    val size = LocalSize.current
    val s = WidgetStrings(context, snapshot.settings.khmer)
    val p = WidgetPalette(snapshot.settings)
    val scale = snapshot.settings.widgetFontScale.multiplier
    val open = WidgetNavigation.openDate(context, id, snapshot.today)
    val date = snapshot.today
    val details = snapshot.details
    val animalAlpha = resolveAnimalAlpha(context, snapshot)

    val cardWidthModifier = GlanceModifier.fillMaxWidth()

    Box(
        GlanceModifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            cardWidthModifier
                .fillMaxHeight()
                .appWidgetBackground()
                .background(p.background)
                .cornerRadius(24.dp)
                .clickable(open),
        ) {
        // --- 1. SUBTLE BACKGROUND ANIMAL WATERMARK ---
        details?.let { d ->
            Box(
                GlanceModifier.fillMaxSize().padding(end = 8.dp, bottom = 8.dp),
                contentAlignment = Alignment.BottomEnd,
            ) {
                Image(
                    ImageProvider(zodiacDrawable(d.animalYear, compact = true)),
                    contentDescription = null,
                    alpha = animalAlpha,
                    modifier = GlanceModifier.size(175.dp),
                    colorFilter = ColorFilter.tint(p.accent),
                )
            }
        }

        // --- 2. FOREGROUND CONTENT ---
        Column(
            GlanceModifier.fillMaxSize().padding(start = 11.dp, top = 8.dp, end = 11.dp, bottom = 10.dp),
        ) {
            // Header: Animal Year & Sak + BE Year Chip + Timezone Badge + Action Buttons
            Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Row(
                    GlanceModifier.defaultWeight(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val emoji = details?.let { animalEmoji(it.animalYear) } ?: "🐾"
                    val traditional = details?.let {
                        s(R.string.widget_traditional_year, it.animalLabel(s.khmer), L.text("calendar.sak.${it.sak}", s.khmer))
                    } ?: ""
                    val beYear = details?.let { " · " + s(R.string.widget_buddhist_year, s.number(it.lunar.buddhistYear)) } ?: ""

                    // Badge 1: Traditional & BE Year
                    Box(
                        GlanceModifier.background(p.surfaceVariant)
                            .cornerRadius(8.dp)
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                    ) {
                        WText("$emoji $traditional$beYear", p.text, 12.5f, scale, bold = false, lines = 1)
                    }

                    if (size.width.value >= 315f) {
                        Spacer(GlanceModifier.width(5.dp))

                        // Badge 2: Current Selected Timezone (collapses to emoji only when space is tight)
                        val emojiOnly = size.width.value / scale < 340f
                        val tzText = WidgetPolicy.timezoneLabel(snapshot.settings.todayTimeZone, s.khmer, emojiOnly)
                        val tzPad = if (emojiOnly) 6.dp else 8.dp
                        Box(
                            GlanceModifier.background(p.surfaceVariant)
                                .cornerRadius(8.dp)
                                .padding(horizontal = tzPad, vertical = 3.dp),
                        ) {
                            WText(tzText, p.secondary, 12.5f, scale, bold = false, lines = 1)
                        }
                    }
                }

                // Quick Action Buttons
                Row(verticalAlignment = Alignment.CenterVertically) {
                    WidgetRefreshIcon(context, id, s(R.string.widget_refresh), p)
                }
            }

            Spacer(GlanceModifier.height(4.dp))

            // Body: Adaptive Left Column & Right Column!
            val availWidth = if (size.width.value > 0f) size.width - 24.dp else 334.dp
            val showDetailsList = size.width.value >= 330f && size.height.value >= 120f
            // Freeze day detail width at step 2 (330dp+ threshold: ~152dp)
            // so any additional width from step 3+ expands the event lists instead.
            val step2DetailWidth = 152.dp * scale.coerceAtLeast(1f)
            val compactDayWidth = (availWidth * 0.36f).coerceAtMost(105.dp * scale.coerceAtLeast(1f))
            val leftColWidth = if (showDetailsList) step2DetailWidth else compactDayWidth

            Row(GlanceModifier.fillMaxWidth().defaultWeight()) {
                // Left Column
                Column(
                    GlanceModifier.width(leftColWidth)
                        .fillMaxHeight()
                        .background(p.surfaceVariant)
                        .cornerRadius(16.dp)
                        .padding(6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // Sub-col 1 (Left): Weekday on top (without "tngai") + Big day number below
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            val rawWeekday = CalendarWords.weekday(date.dayOfWeek.value, s.khmer, style = "short")
                            val weekdayClean = if (s.khmer) rawWeekday.removePrefix("ថ្ងៃ") else rawWeekday
                            WText(weekdayClean, p.secondary, if (showDetailsList) 13.5f else 15f, scale, bold = true)
                            Spacer(GlanceModifier.height(2.dp))
                            WText(s.number(date.dayOfMonth), p.accent, if (showDetailsList) 30f else 36f, scale, bold = true)
                        }

                        if (showDetailsList) {
                            Spacer(GlanceModifier.width(8.dp))

                            // Sub-col 2 (Right): 5 items
                            Column {
                                // 1. Month (short form)
                                val rawMonth = CalendarWords.month(date.monthValue, s.khmer, short = true)
                                val monthClean = if (s.khmer) rawMonth.removePrefix("ខែ") else rawMonth
                                WText(monthClean, p.text, 12f, scale, bold = false, lines = 1)

                                details?.lunar?.let { l ->
                                    // 2. Lunar month
                                    Spacer(GlanceModifier.height(2.dp))
                                    WText(L.text("calendar.lunar_month.${l.month}", s.khmer), p.secondary, 11.5f, scale, bold = false, lines = 1)
                                    Spacer(GlanceModifier.height(2.dp))

                                    // 3. Lunar day [emoji]
                                    WText("${l.shortLabel(s.khmer)} ${moonEmoji(l).trim()}", p.accent, 11.5f, scale, bold = false, lines = 1)
                                }

                                val holy = when {
                                    snapshot.settings.showHolyDaysInCalendar && details?.lunar?.isHolyDay == true -> "${s(R.string.widget_holy_day)} 🪷"
                                    snapshot.settings.showHolyDaysInCalendar && details?.lunar?.isShavingDay == true -> "${s(R.string.widget_shaving_day)} 🪒"
                                    else -> null
                                }
                                val badgeText = holy ?: snapshot.holidayTitle?.let { "$it 🎉" }
                                if (badgeText != null) {
                                    val isHoly = details?.lunar?.isHolyDay == true || details?.lunar?.isShavingDay == true
                                    // 4. Holy day [emoji]
                                    Spacer(GlanceModifier.height(2.dp))
                                    WText(badgeText, if (isHoly) p.holy else p.holiday, 11.5f, scale, bold = false, lines = 1)
                                }

                                if (snapshot.settings.showWesternZodiac) {
                                    // 5. Western Zodiac [emoji], matching the Date details setting.
                                    Spacer(GlanceModifier.height(2.dp))
                                    val z = details?.zodiac ?: Zodiac.forDate(date)
                                    WText(s.zodiac(z), p.text, 11.5f, scale, bold = false, lines = 1)
                                }
                            }
                        }
                    }
                }

                Spacer(GlanceModifier.width(8.dp))

                // Right Column: 60% width (defaultWeight), Divided into 2 big row blocks
                Column(
                    GlanceModifier.defaultWeight()
                        .fillMaxHeight(),
                ) {
                    // Top Block: Today's Events
                    Column(
                        GlanceModifier.fillMaxWidth()
                            .defaultWeight()
                            .background(p.surfaceVariant)
                            .cornerRadius(14.dp)
                            .clickable(WidgetNavigation.openDate(context, id, date))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                    ) {
                        WText(s(R.string.widget_today_heading), p.accent, 12.5f, scale, bold = false)
                        Spacer(GlanceModifier.height(2.dp))
                        val todayItems = snapshot.current.items
                        if (todayItems.isEmpty()) {
                            Spacer(GlanceModifier.defaultWeight())
                            Box(GlanceModifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                WText(s(R.string.widget_no_events), p.secondary, 11.5f, scale)
                            }
                            Spacer(GlanceModifier.defaultWeight())
                        } else {
                            LazyColumn(GlanceModifier.fillMaxWidth().defaultWeight()) {
                                items(items = todayItems, itemId = { item -> item.eventId?.hashCode()?.toLong() ?: item.title.hashCode().toLong() }) { item ->
                                    val icon = when (item.kind) {
                                        EventKind.HOLY_DAY -> "🪷"
                                        EventKind.HOLIDAY -> "🎉"
                                        EventKind.OBSERVANCE -> "📌"
                                        EventKind.CUSTOM -> "⏰"
                                    }
                                    val fg = when (item.kind) {
                                        EventKind.HOLY_DAY -> p.holy
                                        EventKind.HOLIDAY -> p.holiday
                                        EventKind.OBSERVANCE -> p.accent
                                        EventKind.CUSTOM -> p.text
                                    }
                                    Row(
                                        GlanceModifier.fillMaxWidth()
                                            .padding(vertical = 3.dp)
                                            .clickable(WidgetNavigation.openDate(context, id, date, item.eventId)),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        WText(icon, fg, 12f, scale)
                                        Spacer(GlanceModifier.width(4.dp))
                                        val text = listOfNotNull(item.time, item.title).joinToString(" ")
                                        WText(text, fg, 12f, scale, bold = false, modifier = GlanceModifier.defaultWeight(), lines = 1)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(GlanceModifier.height(6.dp))

                    val tomorrowDay = snapshot.tomorrow
                    // Bottom Block: Tomorrow's Events
                    Column(
                        GlanceModifier.fillMaxWidth()
                            .defaultWeight()
                            .background(p.surfaceVariant)
                            .cornerRadius(14.dp)
                            .clickable(WidgetNavigation.openDate(context, id, tomorrowDay.date))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                    ) {
                        WText(s(R.string.widget_tomorrow), p.accent, 12.5f, scale, bold = false)
                        Spacer(GlanceModifier.height(2.dp))
                        if (tomorrowDay.items.isEmpty()) {
                            Spacer(GlanceModifier.defaultWeight())
                            Box(GlanceModifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                WText(s(R.string.widget_no_events), p.secondary, 11.5f, scale)
                            }
                            Spacer(GlanceModifier.defaultWeight())
                        } else {
                            LazyColumn(GlanceModifier.fillMaxWidth().defaultWeight()) {
                                items(items = tomorrowDay.items, itemId = { item -> item.eventId?.hashCode()?.toLong() ?: item.title.hashCode().toLong() }) { item ->
                                    val icon = when (item.kind) {
                                        EventKind.HOLY_DAY -> "🪷"
                                        EventKind.HOLIDAY -> "🎉"
                                        EventKind.OBSERVANCE -> "📌"
                                        EventKind.CUSTOM -> "⏰"
                                    }
                                    val fg = when (item.kind) {
                                        EventKind.HOLY_DAY -> p.holy
                                        EventKind.HOLIDAY -> p.holiday
                                        EventKind.OBSERVANCE -> p.accent
                                        EventKind.CUSTOM -> p.text
                                    }
                                    Row(
                                        GlanceModifier.fillMaxWidth()
                                            .padding(vertical = 3.dp)
                                            .clickable(WidgetNavigation.openDate(context, id, tomorrowDay.date, item.eventId)),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        WText(icon, fg, 12f, scale)
                                        Spacer(GlanceModifier.width(4.dp))
                                        val text = listOfNotNull(item.time, item.title).joinToString(" ")
                                        WText(text, fg, 12f, scale, bold = false, modifier = GlanceModifier.defaultWeight(), lines = 1)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
}

/**
 * 1st Widget (Daily Events):
 * Header: [១៩ កញ្ញា] [🌕 ៨កើត ខែភទ្របទ · ព.ស. ២៥៧០] + Refresh/Settings buttons (consistent 12f bold).
 * Middle: Scrollable LazyColumn for today's events, fitting 2 full items cleanly at 2-grid height.
 * Footer: Yesterday and Tomorrow sub-cards, expanding up to 2 rows when widget is 4+ rows.
 */
@Composable
internal fun FocusWidgetContent(snapshot: WidgetSnapshot, id: Int) {
    val context = LocalContext.current
    val size = LocalSize.current
    val s = WidgetStrings(context, snapshot.settings.khmer)
    val p = WidgetPalette(snapshot.settings)
    val scale = snapshot.settings.widgetFontScale.multiplier
    val effectiveScale = context.resources.configuration.fontScale * scale
    val plan = WidgetPolicy.layout(size.width.value, size.height.value, effectiveScale)
    val day = snapshot.current
    val animalAlpha = resolveAnimalAlpha(context, snapshot)
    val open = WidgetNavigation.openDate(context, id, snapshot.today)

    val cardWidthModifier = GlanceModifier.fillMaxWidth()

    Box(
        GlanceModifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            cardWidthModifier
                .fillMaxHeight()
                .appWidgetBackground()
                .background(p.background)
                .cornerRadius(24.dp)
                .clickable(open),
        ) {
        // --- 1. SUBTLE BACKGROUND ANIMAL WATERMARK ---
        snapshot.details?.let { d ->
            Box(
                GlanceModifier.fillMaxSize().padding(end = 8.dp, bottom = 8.dp),
                contentAlignment = Alignment.BottomEnd,
            ) {
                Image(
                    ImageProvider(zodiacDrawable(d.animalYear, compact = true)),
                    contentDescription = null,
                    alpha = animalAlpha,
                    modifier = GlanceModifier.size(175.dp),
                    colorFilter = ColorFilter.tint(p.accent),
                )
            }
        }

        // --- 2. FOREGROUND CONTENT ---
        Column(
            GlanceModifier.fillMaxSize().padding(start = 11.dp, top = 8.dp, end = 11.dp, bottom = 10.dp),
        ) {
            // Header: [🌕 ៨កើត ខែភទ្របទ · ព.ស. ២៥៧០] [🌐 ក្នុងតំបន់]
            Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Row(
                    GlanceModifier.defaultWeight().clickable(WidgetNavigation.openDate(context, id, snapshot.today)),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Badge 1: [🌕 ៨កើត · ភទ្របទ · ព.ស. ២៥៧០]
                    snapshot.details?.let { d ->
                        Box(
                            GlanceModifier.background(p.surfaceVariant)
                                .cornerRadius(8.dp)
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                        ) {
                            val lunarMonth = L.text("calendar.lunar_month.${d.lunar.month}", s.khmer)
                            val beYear = s(R.string.widget_buddhist_year, s.number(d.lunar.buddhistYear))
                            val lunarText = moonEmoji(d.lunar) + lunarMonth + " · " + d.lunar.shortLabel(s.khmer) + " · " + beYear
                            WText(
                                lunarText,
                                p.text, 13f, scale, bold = false, lines = 1,
                            )
                        }
                    }

                    if (size.width.value >= 315f) {
                        // Badge 2 (Last): Current Selected Timezone (collapses to emoji only when space is tight)
                        Spacer(GlanceModifier.width(5.dp))
                        val emojiOnly = size.width.value / scale < 340f
                        val tzText = WidgetPolicy.timezoneLabel(snapshot.settings.todayTimeZone, s.khmer, emojiOnly)
                        val tzPad = if (emojiOnly) 6.dp else 8.dp
                        Box(
                            GlanceModifier.background(p.surfaceVariant)
                                .cornerRadius(8.dp)
                                .padding(horizontal = tzPad, vertical = 4.dp),
                        ) {
                            WText(tzText, p.secondary, 13f, scale, bold = false, lines = 1)
                        }
                    }
                }

                // Action Buttons
                Row(verticalAlignment = Alignment.CenterVertically) {
                    WidgetRefreshIcon(context, id, s(R.string.widget_refresh), p)
                }
            }

            Spacer(GlanceModifier.height(6.dp))

            // Main Events Area: Scrollable LazyColumn!
            if (day.items.isEmpty()) {
                Box(
                    GlanceModifier.fillMaxWidth().defaultWeight()
                        .background(p.surfaceVariant)
                        .cornerRadius(12.dp)
                        .padding(8.dp)
                        .clickable(WidgetNavigation.openDate(context, id, day.date)),
                    contentAlignment = Alignment.Center,
                ) {
                    WText("📅  " + s(R.string.widget_no_events), p.text, 13.5f, scale, bold = true)
                }
            } else {
                LazyColumn(GlanceModifier.fillMaxWidth().defaultWeight()) {
                    items(items = day.items, itemId = { item -> item.eventId?.hashCode()?.toLong() ?: item.title.hashCode().toLong() }) { item ->
                        EventCard(item, WidgetNavigation.openDate(context, id, day.date, item.eventId), p, scale)
                        Spacer(GlanceModifier.height(6.dp))
                    }
                }
            }

            Spacer(GlanceModifier.height(6.dp))

            // Footer Sub-Cards: Yesterday & Tomorrow without labels; up to 2 rows when expanded
            if (!plan.tiny || size.height.value >= 120f * effectiveScale) {
                Row(GlanceModifier.fillMaxWidth()) {
                    AdjacentDayCard(
                        snapshot.yesterday, context, id,
                        p, s, scale, plan.expanded, GlanceModifier.defaultWeight(),
                    )
                    Spacer(GlanceModifier.width(8.dp))
                    AdjacentDayCard(
                        snapshot.tomorrow, context, id,
                        p, s, scale, plan.expanded, GlanceModifier.defaultWeight(),
                    )
                }
            }
        }
    }
}
}

@Composable
private fun EventCard(item: WidgetItem, action: Action, p: WidgetPalette, scale: Float) {
    val (icon, bg, fg) = when (item.kind) {
        EventKind.HOLY_DAY -> Triple("🪷", p.holyBackground, p.holy)
        EventKind.HOLIDAY -> Triple("🎉", p.holidayBackground, p.holiday)
        EventKind.OBSERVANCE -> Triple("📌", p.surfaceVariant, p.accent)
        EventKind.CUSTOM -> Triple("⏰", p.surfaceVariant, p.text)
    }
    Row(
        GlanceModifier.fillMaxWidth()
            .background(bg)
            .cornerRadius(10.dp)
            .clickable(action)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            GlanceModifier.size(22.dp).cornerRadius(11.dp).background(p.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            WText(icon, fg, 12f, scale)
        }
        Spacer(GlanceModifier.width(7.dp))
        val text = listOfNotNull(item.time, item.title).joinToString("  ")
        WText(text, fg, 13f, scale, bold = false, modifier = GlanceModifier.defaultWeight(), lines = 1)
    }
}

/** Bottom sub-card for adjacent day: Clean Date chip, up to 2 rows of items when expanded. */
@Composable
private fun AdjacentDayCard(
    day: WidgetDay, context: Context, id: Int, p: WidgetPalette,
    s: WidgetStrings, scale: Float, expanded: Boolean, modifier: GlanceModifier,
) {
    val dateLabel = s.number(day.date.dayOfMonth) + " " + CalendarWords.month(day.date.monthValue, s.khmer, short = true)
    Column(
        modifier
            .background(p.surfaceVariant)
            .cornerRadius(12.dp)
            .clickable(WidgetNavigation.openDate(context, id, day.date))
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        WText(dateLabel, p.accent, 12.5f, scale, bold = false)
        Spacer(GlanceModifier.height(2.dp))
        if (!day.supported || day.unavailable) {
            WText(daySummary(day, s), p.text, 11.5f, scale, lines = 1)
        } else if (day.items.isEmpty()) {
            WText(s(R.string.widget_no_events), p.secondary, 11.5f, scale, lines = 1)
        } else {
            val takeCount = if (expanded) 2 else 1
            day.items.take(takeCount).forEach { item ->
                val icon = when (item.kind) {
                    EventKind.HOLY_DAY -> "🪷 "
                    EventKind.HOLIDAY -> "🎉 "
                    else -> "• "
                }
                WText(icon + listOfNotNull(item.time, item.title).joinToString(" "), p.text, 11.5f, scale, lines = 1)
            }
        }
    }
}

@Composable
private fun WidgetRefreshIcon(context: Context, id: Int, description: String, palette: WidgetPalette) {
    AndroidRemoteViews(WidgetRefreshControl.views(context, id, description, palette),
        modifier = GlanceModifier.size(30.dp))
}

/**
 * 3rd Widget (4x3 Full Month Calendar Grid):
 * Header: 7 weekday column headers with traditional day colors or secondary color.
 * Grid: 7-column x 5..6 row month calendar. Each cell shows:
 *   - Gregorian day number (e.g. 19 / ១៩)
 *   - Lunar date short label (e.g. 8K / ៨កើត)
 *   - Holy day lotus watermark
 *   - Today highlight: filled accent cell with contrast text
 *   - Event markers row (● holiday, ▲ holy, ■ observance, ★ personal)
 * Footer: Legend row (● Holiday  ▲ Holy day  ■ Observance  ★ Personal), centered like the app.
 */
@Composable
internal fun MonthWidgetContent(snapshot: WidgetSnapshot, id: Int) {
    val context = LocalContext.current
    val size = LocalSize.current
    val s = WidgetStrings(context, snapshot.settings.khmer)
    val p = WidgetPalette(snapshot.settings)
    val scale = snapshot.settings.widgetFontScale.multiplier
    val animalAlpha = resolveAnimalAlpha(context, snapshot)
    val open = WidgetNavigation.openDate(context, id, snapshot.today)
    val month = YearMonth.from(snapshot.today)
    // Holy-day lotus watermark opacity matching the in-app MonthGrid (0.25f).
    val lotusAlpha = 0.25f

    // Height-adaptive budget: on tablets or compact widget heights in landscape (4x3),
    // available height can drop below 240dp. Tighten paddings and offsets so day numbers
    // and event markers never clip or overlap.
    val isCompactHeight = size.height.value in 1f..250f
    val isVeryShort = size.height.value in 1f..195f

    // Enforce aspect ratio: the card is never wider than 1.25x height, with no absolute
    // width ceiling — tall portrait widgets widen proportionally just like landscape ones.
    val maxAllowedWidth = WidgetPolicy.monthCardMaxWidth(size.height.value)
    val cardWidthModifier = if (size.width.value > maxAllowedWidth) {
        GlanceModifier.width(maxAllowedWidth.dp)
    } else {
        GlanceModifier.fillMaxWidth()
    }
    val effectiveWidth = if (size.width.value > 0f) minOf(size.width.value, maxAllowedWidth) else maxAllowedWidth

    // From font zoom 110%, dynamically increase the gap between day number and event markers (+10% per 10% zoom step).
    val gapMultiplier = if (scale >= 1.05f) {
        1f + ((scale - 1.0f) / 0.10f) * 0.10f
    } else {
        1f
    }

    val outerVPad = if (isCompactHeight) 4.dp else 10.dp
    val outerHPad = if (effectiveWidth < 380f) 10.dp else 14.dp
    val dividerVPad = if (isCompactHeight) 2.dp else 6.dp
    val headerBottomPad = if (isCompactHeight) 1.dp else 2.dp
    val weekdayBottomPad = if (isCompactHeight) 1.dp else 3.dp
    val weekdayFontSize = if (isCompactHeight) 10f else 11f

    val dayFontSize = if (isCompactHeight) 11.5f else 13f
    val baseDayBottomPad = if (isCompactHeight) 6.5f else 8f
    val baseMarkerTopPad = if (isCompactHeight) 12.5f else 13.5f
    val dayBottomPad = (baseDayBottomPad * gapMultiplier).dp
    val markerTopPad = (baseMarkerTopPad * gapMultiplier).dp
    val markerHolidaySize = if (isCompactHeight) 8f else 11f
    val markerOtherSize = if (isCompactHeight) 6.5f else 8f

    Box(
        GlanceModifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            cardWidthModifier
                .fillMaxHeight()
                .appWidgetBackground()
                .background(p.background)
                .cornerRadius(24.dp)
                .clickable(open),
        ) {
        // --- 1. SUBTLE BACKGROUND ANIMAL WATERMARK (Mirrors in-app MonthCard) ---
        if (month.monthValue == 4) {
            val oldAnimal = Math.floorMod(month.year - 4 - 1, 12)
            val newAnimal = Math.floorMod(month.year - 4, 12)
            Box(
                GlanceModifier.fillMaxSize().padding(start = 10.dp, top = 10.dp),
                contentAlignment = Alignment.TopStart,
            ) {
                Image(
                    ImageProvider(zodiacDrawable(oldAnimal, compact = true)),
                    contentDescription = null,
                    alpha = animalAlpha,
                    modifier = GlanceModifier.size(120.dp),
                    contentScale = ContentScale.Fit,
                    colorFilter = ColorFilter.tint(p.accent),
                )
            }
            Box(
                GlanceModifier.fillMaxSize().padding(end = 10.dp, bottom = 10.dp),
                contentAlignment = Alignment.BottomEnd,
            ) {
                Image(
                    ImageProvider(zodiacDrawable(newAnimal, compact = false)),
                    contentDescription = null,
                    alpha = animalAlpha,
                    modifier = GlanceModifier.size(160.dp),
                    contentScale = ContentScale.Fit,
                    colorFilter = ColorFilter.tint(p.accent),
                )
            }
        } else {
            val animalYear = snapshot.details?.animalYear ?: Math.floorMod(month.year - 4, 12)
            Box(
                GlanceModifier.fillMaxSize().padding(12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    ImageProvider(zodiacDrawable(animalYear, compact = false)),
                    contentDescription = null,
                    alpha = animalAlpha,
                    modifier = GlanceModifier.fillMaxSize(),
                    contentScale = ContentScale.Fit,
                    colorFilter = ColorFilter.tint(p.accent),
                )
            }
        }

        // --- 2. FOREGROUND CONTENT ---
        // Glance translates at most 10 children per Row/Column into RemoteViews and silently
        // drops the rest (RemoteViewsTranslator.setChildren uses take(10)). Keep this Column
        // at 7 children: fold spacing into padding on the views instead of Spacer children,
        // or the footer legend below the grid will disappear.
        Column(
            GlanceModifier.fillMaxSize().padding(horizontal = outerHPad, vertical = outerVPad),
        ) {
            // --- HEADER: SOLAR MONTH BADGE + TRADITIONAL YEAR & BE YEAR + MINI TIMEZONE + REFRESH BUTTON ---
            Row(
                GlanceModifier.fillMaxWidth().padding(bottom = headerBottomPad),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    GlanceModifier.defaultWeight()
                        .clickable(WidgetNavigation.openDate(context, id, snapshot.today)),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val badgeHPad = if (effectiveWidth >= 380f) 8.dp else 5.dp
                    val badgeVPad = if (isCompactHeight) 1.dp else 2.dp
                    val badgeSpacer = if (effectiveWidth >= 380f) 5.dp else 3.5.dp
                    val badgeFontSize = if (isCompactHeight) 11f else if (effectiveWidth >= 380f) 12f else 11.5f

                    // Badge 1: Dedicated Solar Month Name (without "ខែ" in Khmer, short form in English)
                    val solMonthText = CalendarWords.month(month.monthValue, s.khmer, short = true)
                    Box(
                        GlanceModifier.background(p.surfaceVariant)
                            .cornerRadius(8.dp)
                            .padding(horizontal = badgeHPad, vertical = badgeVPad),
                    ) {
                        WText("📅 $solMonthText", p.text, badgeFontSize, scale, bold = true, lines = 1)
                    }

                    Spacer(GlanceModifier.width(badgeSpacer))

                    // Badge 2: [🐎 ឆ្នាំមមី · អដ្ឋស័ក · ព.ស. ២៥៧០]
                    snapshot.details?.let { d ->
                        Box(
                            GlanceModifier.background(p.surfaceVariant)
                                .cornerRadius(8.dp)
                                .padding(horizontal = badgeHPad, vertical = badgeVPad),
                        ) {
                            val emoji = animalEmoji(d.animalYear)
                            val traditional = s(R.string.widget_traditional_year, d.animalLabel(s.khmer), L.text("calendar.sak.${d.sak}", s.khmer))
                            val beYear = " · " + s(R.string.widget_buddhist_year, s.number(d.lunar.buddhistYear))
                            WText("$emoji $traditional$beYear", p.text, badgeFontSize, scale, bold = false, lines = 1)
                        }
                    }

                    if (effectiveWidth >= 315f) {
                        Spacer(GlanceModifier.width(badgeSpacer))
                        // Badge 3: Current Selected Timezone (collapses to emoji only when space is constrained)
                        val emojiOnly = effectiveWidth / scale < 355f
                        val tzText = WidgetPolicy.timezoneLabel(snapshot.settings.todayTimeZone, s.khmer, emojiOnly)
                        val tzPad = if (emojiOnly) 6.dp else badgeHPad
                        Box(
                            GlanceModifier.background(p.surfaceVariant)
                                .cornerRadius(8.dp)
                                .padding(horizontal = tzPad, vertical = badgeVPad),
                        ) {
                            WText(tzText, p.secondary, badgeFontSize, scale, bold = false, lines = 1)
                        }
                    }
                }

                // Quick Action Buttons
                Row(verticalAlignment = Alignment.CenterVertically) {
                    WidgetRefreshIcon(context, id, s(R.string.widget_refresh), p)
                }
            }

            // --- HEADER DIVIDER LINE (WITH GAPS BEFORE AND AFTER) ---
            Box(
                GlanceModifier.fillMaxWidth().padding(vertical = dividerVPad),
            ) {
                Box(
                    GlanceModifier.fillMaxWidth().height(1.dp).background(p.secondary),
                ) {}
            }

            // --- WEEKDAY HEADERS (7 COLUMNS) ---
            Row(
                GlanceModifier.fillMaxWidth().padding(bottom = weekdayBottomPad),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val weekdays = listOf(
                    DayOfWeek.SUNDAY,
                    DayOfWeek.MONDAY,
                    DayOfWeek.TUESDAY,
                    DayOfWeek.WEDNESDAY,
                    DayOfWeek.THURSDAY,
                    DayOfWeek.FRIDAY,
                    DayOfWeek.SATURDAY,
                )
                val orderedDays = if (snapshot.settings.mondayFirst) weekdays.drop(1) + weekdays.first() else weekdays

                orderedDays.forEach { dayOfWeek ->
                    val style = if (snapshot.settings.showLongerWeekdayNames) "grid_long" else "grid"
                    val headerText = CalendarWords.weekday(dayOfWeek.value, s.khmer, style = style)
                    val color = p.weekdayLabelColor(dayOfWeek)

                    Box(
                        GlanceModifier.defaultWeight(),
                        contentAlignment = Alignment.Center,
                    ) {
                        WText(headerText, color, weekdayFontSize, scale, medium = true)
                    }
                }
            }

            // --- MONTH GRID (Takes remaining vertical height with defaultWeight) ---
            val firstDay = month.atDay(1)
            val daysInMonth = month.lengthOfMonth()
            val startOffset = if (snapshot.settings.mondayFirst) {
                (firstDay.dayOfWeek.value - 1)
            } else {
                (firstDay.dayOfWeek.value % 7)
            }

            val totalCells = startOffset + daysInMonth
            val numRows = (totalCells + 6) / 7
            val daysMap = snapshot.monthDays.associateBy { it.dayNumber }

            Column(
                GlanceModifier.fillMaxWidth().defaultWeight(),
            ) {
                for (r in 0 until numRows) {
                    Row(
                        modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        for (c in 0..6) {
                            val cellIndex = r * 7 + c
                            if (cellIndex < startOffset || cellIndex >= totalCells) {
                                Box(modifier = GlanceModifier.defaultWeight()) {}
                            } else {
                                val dayNum = cellIndex - startOffset + 1
                                val info = daysMap[dayNum]
                                val cellDate = info?.date ?: month.atDay(dayNum)
                                val isToday = (cellDate == snapshot.today)
                                val isSunday = cellDate.dayOfWeek == DayOfWeek.SUNDAY

                                Box(
                                    modifier = GlanceModifier.defaultWeight()
                                        .fillMaxHeight()
                                        .then(
                                            if (isToday) GlanceModifier.background(p.accent).cornerRadius(10.dp)
                                            // Launcher hosts can reapply RemoteViews to an existing cell. Explicitly
                                            // clear yesterday's accent instead of omitting the background action.
                                            else GlanceModifier.background(Color.Transparent)
                                        )
                                        .clickable(WidgetNavigation.openDate(context, id, cellDate)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    // 1. Holy-day lotus watermark in cell background
                                    info?.lotusRes?.let { res ->
                                        Image(
                                            ImageProvider(res),
                                            contentDescription = null,
                                            alpha = lotusAlpha,
                                            modifier = GlanceModifier.padding(1.dp),
                                        )
                                    }

                                    // 2. Gregorian Day Number (Layer 1 - Centered, slightly lifted with adaptive bottom padding)
                                    val holidayTint = info?.hasHoliday == true ||
                                        (snapshot.settings.highlightSunday && isSunday)
                                    WText(
                                        s.number(dayNum),
                                        when {
                                            isToday -> p.onAccent
                                            holidayTint -> p.holiday
                                            else -> p.text
                                        },
                                        dayFontSize, scale, bold = isToday, medium = !isToday,
                                        modifier = GlanceModifier.padding(bottom = dayBottomPad),
                                    )

                                    // 3. Event Markers (Layer 2 - Adaptive top offset, drawn ON TOP of everything)
                                    if (info != null) {
                                        val hasAnyMarker = info.hasHoliday || info.isHolyDay ||
                                            info.hasObservance || info.hasPersonal
                                        if (hasAnyMarker) {
                                            Box(
                                                GlanceModifier.fillMaxSize(),
                                                contentAlignment = Alignment.Center,
                                            ) {
                                                Row(
                                                    GlanceModifier.padding(top = markerTopPad),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                ) {
                                                    if (info.hasHoliday) {
                                                        WText("●", if (isToday) p.onAccent else p.holiday, markerHolidaySize, scale)
                                                    }
                                                    if (info.isHolyDay) {
                                                        WText("▲", if (isToday) p.onAccent else p.holy, markerOtherSize, scale)
                                                    }
                                                    if (info.hasObservance) {
                                                        WText("■", if (isToday) p.onAccent else p.accent, markerOtherSize, scale)
                                                    }
                                                    if (info.hasPersonal) {
                                                        WText("★", if (isToday) p.onAccent else p.personal, markerOtherSize, scale)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // --- FOOTER DIVIDER LINE & LEGEND (Hidden when height < 195dp to preserve grid legibility) ---
            if (!isVeryShort) {
                Box(
                    GlanceModifier.fillMaxWidth().padding(vertical = dividerVPad),
                ) {
                    Box(
                        GlanceModifier.fillMaxWidth().height(1.dp).background(p.secondary),
                    ) {}
                }

                // --- FOOTER LEGEND ROW (Footnote notes) ---
                val legendTextSize = if (isCompactHeight) 9.5f else 10.5f
                val legendHolidayMarker = if (isCompactHeight) 10f else 13f
                val legendOtherMarker = if (isCompactHeight) 8f else 10f
                val legendItemGap = if (effectiveWidth < 380f) 5.dp else 8.dp

                Row(
                    GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Spacer(GlanceModifier.defaultWeight())

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (snapshot.settings.widgetShowHolidays) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                WText("●", p.holiday, legendHolidayMarker, scale)
                                Spacer(GlanceModifier.width(3.dp))
                                WText(L.text("ui.holiday.253332", s.khmer), p.secondary, legendTextSize, scale, bold = false, lines = 1)
                            }
                        }
                        if (snapshot.settings.showHolyDaysInCalendar) {
                            Spacer(GlanceModifier.width(legendItemGap))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                WText("▲", p.holy, legendOtherMarker, scale)
                                Spacer(GlanceModifier.width(3.dp))
                                WText(L.text("ui.holy_day.28786d", s.khmer), p.secondary, legendTextSize, scale, bold = false, lines = 1)
                            }
                        }
                        if (snapshot.settings.showObservances && snapshot.settings.widgetShowObservances) {
                            Spacer(GlanceModifier.width(legendItemGap))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                WText("■", p.accent, legendOtherMarker, scale)
                                Spacer(GlanceModifier.width(3.dp))
                                WText(L.text("ui.observance.5b9a87", s.khmer), p.secondary, legendTextSize, scale, bold = false, lines = 1)
                            }
                        }
                        if (snapshot.settings.widgetShowPersonal) {
                            Spacer(GlanceModifier.width(legendItemGap))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                WText("★", p.personal, legendOtherMarker, scale)
                                Spacer(GlanceModifier.width(3.dp))
                                WText(L.text("ui.custom.917053", s.khmer), p.secondary, legendTextSize, scale, bold = false, lines = 1)
                            }
                        }
                    }

                    Spacer(GlanceModifier.defaultWeight())
                }
            }
        }
    }
}
}

@Composable
internal fun WText(
    text: String, color: ColorProvider, sizeSp: Float, scale: Float,
    modifier: GlanceModifier = GlanceModifier, bold: Boolean = false, medium: Boolean = false,
    lines: Int = 1,
) {
    Text(
        text = text, modifier = modifier, maxLines = lines,
        style = TextStyle(
            color = color,
            fontSize = (sizeSp * scale).sp,
            fontWeight = when {
                bold -> FontWeight.Bold
                medium -> FontWeight.Medium
                else -> FontWeight.Normal
            },
        ),
    )
}

private fun daySummary(day: WidgetDay, s: WidgetStrings): String = when {
    !day.supported -> s(R.string.widget_unsupported_date)
    day.unavailable -> s(R.string.widget_events_unavailable)
    day.eventCount == 0 -> s(R.string.widget_no_events)
    else -> s(if (day.eventCount == 1) R.string.widget_event_count_one else R.string.widget_event_count, s.number(day.eventCount))
}
