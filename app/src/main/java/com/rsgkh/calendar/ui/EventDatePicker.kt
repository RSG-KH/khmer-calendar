// Copyright (c) 2026 RSG-KH | Apache-2.0 License
@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.rsgkh.calendar.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rsgkh.calendar.i18n.CalendarWords
import com.rsgkh.calendar.i18n.L
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset
import java.time.temporal.WeekFields
import kotlinx.coroutines.launch
import androidx.compose.material3.R as MaterialR

/** Material's weekday row always uses NARROW_STANDALONE, which duplicates Khmer labels.
 * Keep its state, localized header and numeric input; supply a calendar with readable weekdays.
 */
@Composable internal fun EventDatePicker(state: DatePickerState, khmer: Boolean) {
    if (!khmer || state.displayMode == DisplayMode.Input) {
        DatePicker(state)
        return
    }
    val colors = DatePickerDefaults.colors()
    val formatter = remember { DatePickerDefaults.dateFormatter() }
    var choosingYear by rememberSaveable { mutableStateOf(false) }
    val firstMonth = remember(state.yearRange) { YearMonth.of(state.yearRange.first, 1) }
    val initialMonth = YearMonth.from(Instant.ofEpochMilli(state.displayedMonthMillis).atZone(ZoneOffset.UTC))
    val pager = rememberPagerState(
        initialPage = (initialMonth.year - firstMonth.year) * 12 + initialMonth.monthValue - 1,
        pageCount = { state.yearRange.count() * 12 }
    )
    val month = firstMonth.plusMonths(pager.currentPage.toLong())
    val scope = rememberCoroutineScope()
    LaunchedEffect(pager.settledPage) {
        state.displayedMonthMillis = firstMonth.plusMonths(pager.settledPage.toLong()).atDay(1).utcMillis()
    }
    Column(Modifier.fillMaxWidth().testTag("event-date-picker")) {
        DatePickerDefaults.DatePickerTitle(state.displayMode,
            Modifier.padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 20.dp), colors.titleContentColor)
        Row(Modifier.fillMaxWidth().padding(start = 24.dp, end = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically) {
            ProvideTextStyle(MaterialTheme.typography.headlineLarge) {
                DatePickerDefaults.DatePickerHeadline(state.selectedDateMillis, state.displayMode,
                    formatter, Modifier.weight(1f), colors.headlineContentColor)
            }
            val inputLabel = stringResource(MaterialR.string.m3c_date_picker_switch_to_input_mode)
            IconButton(onClick = { state.displayMode = DisplayMode.Input },
                modifier = Modifier.semantics { contentDescription = inputLabel }) {
                val color = colors.headlineContentColor
                Canvas(Modifier.size(24.dp)) {
                    withTransform({ scale(size.width / 24, size.height / 24, Offset.Zero) }) {
                        drawPath(Path().apply {
                            moveTo(3f, 17.25f); lineTo(3f, 21f); lineTo(6.75f, 21f)
                            lineTo(17.8f, 9.95f); lineTo(14.05f, 6.2f); close()
                            moveTo(15.45f, 4.8f); lineTo(17.25f, 3f); lineTo(21f, 6.75f)
                            lineTo(19.2f, 8.55f); close()
                        }, color)
                    }
                }
            }
        }
        HorizontalDivider(color = colors.dividerColor)
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            val yearLabel = stringResource(if (choosingYear) MaterialR.string.m3c_date_picker_switch_to_day_selection
                else MaterialR.string.m3c_date_picker_switch_to_year_selection)
            TextButton(onClick = { choosingYear = !choosingYear },
                modifier = Modifier.weight(1f).testTag("picker-year-toggle").semantics { contentDescription = yearLabel },
                contentPadding = PaddingValues(horizontal = 12.dp)) {
                Text(formatter.formatMonthYear(month.atDay(1).utcMillis(), state.locale).orEmpty(),
                    Modifier.weight(1f), color = colors.navigationContentColor)
                Text(if (choosingYear) "▴" else "▾", color = colors.navigationContentColor)
            }
            if (!choosingYear) {
                PickerMonthArrow(false, pager.currentPage > 0, colors.navigationContentColor) {
                    scope.launch { pager.animateScrollToPage(pager.currentPage - 1) }
                }
                PickerMonthArrow(true, pager.currentPage < pager.pageCount - 1, colors.navigationContentColor) {
                    scope.launch { pager.animateScrollToPage(pager.currentPage + 1) }
                }
            }
        }
        if (choosingYear) {
            val years = remember(state.yearRange) { state.yearRange.toList() }
            val grid = rememberLazyGridState(initialFirstVisibleItemIndex = (month.year - state.yearRange.first - 3).coerceAtLeast(0))
            LazyVerticalGrid(GridCells.Fixed(3), Modifier.fillMaxWidth().height(336.dp).padding(horizontal = 12.dp)
                .testTag("picker-years"), state = grid) {
                items(years, key = { it }) { year ->
                    val description = stringResource(MaterialR.string.m3c_date_picker_navigate_to_year_description, year.toString())
                    TextButton(enabled = state.selectableDates.isSelectableYear(year), onClick = {
                        scope.launch { pager.scrollToPage((year - firstMonth.year) * 12 + month.monthValue - 1) }
                        choosingYear = false
                    }, modifier = Modifier.padding(4.dp).semantics {
                        selected = year == month.year
                        contentDescription = description
                    }, colors = ButtonDefaults.textButtonColors(
                        containerColor = if (year == month.year) colors.selectedYearContainerColor else Color.Transparent,
                        contentColor = when (year) {
                            month.year -> colors.selectedYearContentColor
                            LocalDate.now().year -> colors.currentYearContentColor
                            else -> colors.yearContentColor
                        }, disabledContentColor = colors.disabledYearContentColor)) { Text(year.toString()) }
                }
            }
        } else {
            val firstDay = WeekFields.of(state.locale).firstDayOfWeek.value
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp).heightIn(min = 48.dp),
                verticalAlignment = Alignment.CenterVertically) {
                repeat(7) { column ->
                    val day = (firstDay + column) % 7
                    val fullName = CalendarWords.weekday(day, true)
                    Text(CalendarWords.weekday(day, true, "short"),
                        Modifier.weight(1f).testTag("picker-weekday-$day")
                            .semantics { contentDescription = fullName },
                        color = colors.weekdayContentColor, style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center, maxLines = 1,
                        autoSize = TextAutoSize.StepBased(minFontSize = 12.sp, maxFontSize = 14.sp))
                }
            }
            HorizontalPager(pager, Modifier.fillMaxWidth().testTag("picker-months")) { page ->
                PickerMonth(firstMonth.plusMonths(page.toLong()), firstDay, state, formatter, colors)
            }
        }
    }
}

@Composable private fun PickerMonthArrow(next: Boolean, enabled: Boolean, color: Color, onClick: () -> Unit) {
    val label = L.text(if (next) "ui.next_month.d2d40f" else "ui.previous_month.c03e1f", true)
    IconButton(onClick, enabled = enabled, modifier = Modifier.testTag(if (next) "picker-next" else "picker-previous")
        .semantics { contentDescription = label }) {
        Canvas(Modifier.size(24.dp)) {
            val ink = if (enabled) color else color.copy(alpha = .38f)
            val outer = size.width * if (next) .4f else .6f
            val inner = size.width * if (next) .6f else .4f
            drawLine(ink, Offset(outer, size.height * .3f), Offset(inner, size.height * .5f), 2.dp.toPx(), StrokeCap.Round)
            drawLine(ink, Offset(inner, size.height * .5f), Offset(outer, size.height * .7f), 2.dp.toPx(), StrokeCap.Round)
        }
    }
}

@Composable private fun PickerMonth(month: YearMonth, firstDay: Int, state: DatePickerState,
    formatter: DatePickerFormatter, colors: DatePickerColors) {
    val offset = (month.atDay(1).dayOfWeek.value - firstDay + 7) % 7
    val today = LocalDate.now()
    val todayLabel = stringResource(MaterialR.string.m3c_date_picker_today_description)
    Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp).semantics { collectionInfo = CollectionInfo(6, 7) }) {
        repeat(6) { row ->
            Row(Modifier.fillMaxWidth()) {
                repeat(7) { column ->
                    val day = row * 7 + column - offset + 1
                    Box(Modifier.weight(1f).heightIn(min = 48.dp), contentAlignment = Alignment.Center) {
                        if (day in 1..month.lengthOfMonth()) {
                            val date = month.atDay(day)
                            val millis = date.utcMillis()
                            val selected = state.selectedDateMillis == millis
                            val enabled = state.selectableDates.isSelectableYear(month.year) && state.selectableDates.isSelectableDate(millis)
                            val description = listOfNotNull(todayLabel.takeIf { date == today },
                                formatter.formatDate(millis, state.locale, forContentDescription = true)).joinToString(", ")
                            Surface(onClick = { state.selectedDateMillis = millis }, enabled = enabled,
                                modifier = Modifier.size(40.dp).testTag("picker-day-$date").semantics {
                                    this.selected = selected
                                    contentDescription = description
                                    collectionItemInfo = CollectionItemInfo(row, 1, column, 1)
                                }, shape = CircleShape,
                                color = if (selected) colors.selectedDayContainerColor else Color.Transparent,
                                contentColor = when {
                                    !enabled -> colors.disabledDayContentColor
                                    selected -> colors.selectedDayContentColor
                                    date == today -> colors.todayContentColor
                                    else -> colors.dayContentColor
                                }, border = if (date == today && !selected) BorderStroke(1.dp, colors.todayDateBorderColor) else null) {
                                Box(contentAlignment = Alignment.Center) { Text(day.toString(), style = MaterialTheme.typography.bodyLarge) }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun LocalDate.utcMillis() = atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
