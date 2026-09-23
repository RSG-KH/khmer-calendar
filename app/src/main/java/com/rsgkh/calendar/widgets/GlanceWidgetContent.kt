// SPDX-License-Identifier: Apache-2.0
package com.rsgkh.calendar.widgets

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.appwidget.AndroidRemoteViews
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.action.clickable
import androidx.glance.unit.ColorProvider
import com.rsgkh.calendar.BuildConfig
import com.rsgkh.calendar.R
import com.rsgkh.calendar.domain.ganzhiAnimalLabel
import com.rsgkh.calendar.engine.ChineseZodiacCalculator
import com.rsgkh.calendar.i18n.CalendarWords
import com.rsgkh.calendar.i18n.L

/** A single launcher row: date tile, three concise detail lines, and one refresh control. */
@Composable
internal fun GlanceWidgetContent(snapshot: WidgetSnapshot, id: Int) {
    val context = LocalContext.current
    val size = LocalSize.current
    val strings = WidgetStrings(context, snapshot.settings.khmer)
    val palette = WidgetPalette(snapshot.settings)
    val date = snapshot.today
    val compact = size.width.value < 220f
    // Three lines and a refresh button must fit in one launcher row even at 200% widget zoom.
    val scale = snapshot.settings.widgetFontScale.multiplier.coerceAtMost(if (compact) 1.1f else 1.35f)
    val dateScale = snapshot.settings.widgetFontScale.multiplier.coerceAtMost(if (compact) 1.25f else 1.5f)
    val labels = glanceLabels(snapshot, strings, compact)
    val open = WidgetNavigation.openDate(context, id, date)
    val animalSide = (minOf(size.width.value, size.height.value) - 16f).coerceAtLeast(0f) * 0.6f

    Box(
        GlanceModifier.fillMaxSize().appWidgetBackground().background(palette.background)
            .cornerRadius(22.dp).clickable(open),
    ) {
        snapshot.details?.let { details ->
            Box(GlanceModifier.fillMaxSize().padding(end = 8.dp, bottom = 8.dp),
                contentAlignment = Alignment.BottomEnd) {
                Image(ImageProvider(zodiacDrawable(details.animalYear, compact = true)),
                    contentDescription = null,
                    modifier = GlanceModifier.size(animalSide.dp),
                    contentScale = ContentScale.Fit,
                    alpha = resolveAnimalAlpha(context, snapshot),
                    colorFilter = androidx.glance.ColorFilter.tint(palette.accent))
            }
        }
        Row(
            GlanceModifier.fillMaxSize().padding(if (compact) 6.dp else 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                GlanceModifier.width((size.width.value * 0.24f).coerceIn(48f, 96f).dp).fillMaxHeight()
                    .background(palette.surfaceVariant).cornerRadius(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                WText(strings.plannerWeekday(date), palette.weekdayLabelColor(date.dayOfWeek),
                    if (compact) 12f else 13f, scale, bold = true)
                WText(strings.number(date.dayOfMonth), palette.accent,
                    if (compact) 22f else 30f, dateScale, bold = true)
                WText(WidgetPolicy.timezoneLabel(snapshot.settings.todayTimeZone, strings.khmer,
                    emojiOnly = true), palette.secondary, 11f, scale)
            }
            Spacer(GlanceModifier.width(if (compact) 7.5.dp else 8.dp))
            Column(
                GlanceModifier.defaultWeight().fillMaxHeight(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                WText(labels.solar, palette.text, if (compact) 11.5f else 12.5f, scale, medium = true,
                    modifier = GlanceModifier.fillMaxWidth().padding(end = 34.dp))
                Spacer(GlanceModifier.height(3.dp))
                WText(labels.lunar, palette.secondary, if (compact) 11f else 12f, scale,
                    modifier = GlanceModifier.fillMaxWidth())
                Spacer(GlanceModifier.height(3.dp))
                Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    if (labels.holy) {
                        GlanceBadge("🪷", palette.holy, palette.holyBackground,
                            if (compact) 11f else 14f, scale, if (compact) 2.dp else 4.dp)
                        Spacer(GlanceModifier.width(if (compact) 2.dp else 4.dp))
                    }
                    if (compact) {
                        WText(if (labels.showAppVersion) glanceAppVersionLabel(strings.khmer) else labels.traditional,
                            if (labels.showAppVersion) palette.secondary else palette.text,
                            11f, scale, modifier = GlanceModifier.defaultWeight())
                    } else {
                        labels.zodiac?.let {
                            GlanceBadge(it, palette.text, palette.surfaceVariant, 14f, scale, 4.dp)
                            if (labels.ganzhi != null) Spacer(GlanceModifier.width(4.dp))
                        }
                        labels.ganzhi?.let {
                            GlanceBadge(it, palette.text, palette.surfaceVariant, 14f, scale, 4.dp)
                            labels.clash?.let { clash ->
                                Spacer(GlanceModifier.width(4.dp))
                                WText("×", palette.secondary, 14f, scale)
                                Spacer(GlanceModifier.width(4.dp))
                                GlanceBadge(clash, palette.text, palette.surfaceVariant, 14f, scale, 4.dp)
                            }
                        }
                        if (labels.showAppVersion) {
                            WText(glanceAppVersionLabel(strings.khmer), palette.secondary, 11f, scale)
                        }
                    }
                }
            }
        }
        Box(GlanceModifier.fillMaxSize().padding(top = 8.dp, end = 11.dp),
            contentAlignment = Alignment.TopEnd) {
            AndroidRemoteViews(
                WidgetRefreshControl.views(context, id, strings(R.string.widget_refresh), palette),
                modifier = GlanceModifier.size(30.dp),
            )
        }
    }
}

@Composable
private fun GlanceBadge(text: String, color: ColorProvider, background: ColorProvider,
    size: Float, scale: Float, horizontalPadding: androidx.compose.ui.unit.Dp) {
    Box(GlanceModifier.background(background).cornerRadius(8.dp)
        .padding(horizontal = horizontalPadding, vertical = 1.dp)) {
        WText(text, color, size, scale)
    }
}

internal fun glanceAppVersionLabel(khmer: Boolean): String =
    "${L.text("app.name", khmer)} ${BuildConfig.VERSION_NAME}"

internal data class GlanceLabels(
    val solar: String,
    val lunar: String,
    val traditional: String,
    val holy: Boolean,
    val zodiac: String?,
    val ganzhi: String?,
    val clash: String?,
    val showAppVersion: Boolean,
)

internal fun glanceLabels(snapshot: WidgetSnapshot, strings: WidgetStrings, compact: Boolean): GlanceLabels {
    val date = snapshot.today
    val details = snapshot.details
    val khmer = strings.khmer
    val rawMonth = CalendarWords.month(date.monthValue, khmer, short = compact)
    val month = if (compact) {
        if (khmer) rawMonth.removePrefix("ខែ") else rawMonth.take(3)
    } else rawMonth
    val solar = buildString {
        append(month)
        if (!compact) {
            append(' ')
            append(strings.number(date.year))
        }
        details?.let {
            append(" · ")
            val beYear = strings.number(it.lunar.buddhistYear)
            append(if (compact) beYear else strings(R.string.widget_buddhist_year, beYear))
        }
    }
    val sak = details?.let { L.text("calendar.sak.${it.sak}", khmer) }.orEmpty()
    val animal = details?.animalLabel(khmer).orEmpty()
    val traditional = if (details == null) "" else if (khmer) "ឆ្នាំ$animal · $sak" else "$animal · $sak"
    val lunar = details?.let {
        val monthName = L.text("calendar.lunar_month.${it.lunar.month}", khmer)
        val dayAndMonth = "${it.lunar.shortLabel(khmer)} · $monthName"
        if (compact) dayAndMonth else "$dayAndMonth · $traditional"
    } ?: strings(R.string.widget_lunar_unavailable)
    val holy = (snapshot.settings.showHolyDaysInCalendar || snapshot.settings.showHolyDaysInEvents) &&
        details?.lunar?.isHolyDay == true
    val zodiac = if (!compact && snapshot.settings.showWesternZodiac) details?.zodiac?.emoji?.trim() else null
    val animalPillars = if (!compact && snapshot.settings.showGanzhi && details != null && date.year in 1900..2100) {
        runCatching {
            val yearPillar = ChineseZodiacCalculator.getYearPillar(date.year, date.monthValue, date.dayOfMonth)
            val monthPillar = ChineseZodiacCalculator.getMonthPillar(date.year, date.monthValue, date.dayOfMonth)
            val pillars = listOf(yearPillar, monthPillar, details.ganzhiDay)
            val animals = pillars.joinToString("") {
                it.branch.ganzhiAnimalLabel(khmer, useEmoji = true)
            }
            val clashes = pillars.joinToString("") {
                it.clashBranch.ganzhiAnimalLabel(khmer, useEmoji = true)
            }
            animals to clashes
        }.getOrNull()
    } else null
    return GlanceLabels(solar, lunar, traditional, holy, zodiac,
        animalPillars?.first, animalPillars?.second,
        !holy && (if (compact) traditional.isBlank() else zodiac == null && animalPillars == null))
}
