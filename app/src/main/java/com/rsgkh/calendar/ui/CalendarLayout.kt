// Copyright (c) 2026 RSG-KH | Apache-2.0 License
package com.rsgkh.calendar.ui

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.time.YearMonth

private enum class CalendarSlot { Sizing, Content }
internal val CalendarCellPadding = 1.dp
internal val CalendarColumnGap = 12.dp
internal val CalendarSideMargin = 12.dp
private val CalendarPortraitMaxWidth = 640.dp
private val CalendarPortraitPadding = 20.dp
private const val CalendarReferenceWeekRows = 5

/** Size from a fixed five-row reference, independent of the displayed month's row/event count. */
@Composable
internal fun CalendarContentLayout(
    isLandscape: Boolean,
    isTablet: Boolean,
    referenceCard: @Composable () -> Unit,
    content: @Composable (calendarWidth: Dp, contentWidth: Dp) -> Unit,
) {
    SubcomposeLayout(Modifier.fillMaxSize().testTag("calendar-content")) { constraints ->
        val sideMargins = if (isTablet && isLandscape) CalendarSideMargin * 2 else 0.dp
        val referenceWidth = if (isLandscape) {
            (constraints.maxWidth - sideMargins.roundToPx() - CalendarColumnGap.roundToPx()) / 2
        } else {
            minOf(constraints.maxWidth, CalendarPortraitMaxWidth.roundToPx()) - CalendarPortraitPadding.roundToPx()
        }
        // This slot contains shared weekday/standard-legend layout and a five-row spacer.
        // It is never placed, drawn or exposed to accessibility; dates and artwork compose once.
        val sizing = subcompose(CalendarSlot.Sizing) {
            Box(Modifier.clearAndSetSemantics {}) { referenceCard() }
        }.single().measure(Constraints.fixedWidth(referenceWidth.coerceAtLeast(0)))
        val columnCap = (sizing.height * 1.25f).toDp()
        val availableWidth = constraints.maxWidth.toDp()
        val calendarWidth = minOf(columnCap, referenceWidth.coerceAtLeast(0).toDp())
        val contentWidth = if (isLandscape) {
            // A phone has equal columns; tablet events can grow to twice the calendar width.
            minOf(availableWidth, calendarWidth * (if (isTablet) 3 else 2) + CalendarColumnGap + sideMargins)
        } else {
            minOf(availableWidth, CalendarPortraitMaxWidth, columnCap + CalendarPortraitPadding)
        }
        val visible = subcompose(CalendarSlot.Content) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                content(calendarWidth, contentWidth)
            }
        }.single().measure(constraints)
        layout(visible.width, visible.height) { visible.placeRelative(0, 0) }
    }
}

@Composable
internal fun calendarReferenceGridHeight(): Dp {
    val cellHeight = calendarCellHeight()
    return with(LocalDensity.current) {
        ((cellHeight.roundToPx() + CalendarCellPadding.roundToPx() * 2) * CalendarReferenceWeekRows).toDp()
    }
}

@Composable
internal fun calendarCellHeight(): Dp {
    val config = LocalConfiguration.current
    val landscape = config.orientation == Configuration.ORIENTATION_LANDSCAPE
    val tablet = config.smallestScreenWidthDp >= 600
    val baseHeight = when {
        tablet && landscape -> 52.dp
        tablet -> 64.dp
        landscape -> 44.dp
        else -> 56.dp
    }
    return baseHeight * LocalDensity.current.fontScale.coerceAtLeast(1f)
}

internal fun calendarFirstDayOffset(month: YearMonth, mondayFirst: Boolean): Int =
    if (mondayFirst) month.atDay(1).dayOfWeek.value - 1 else month.atDay(1).dayOfWeek.value % 7

internal fun calendarWeekCount(month: YearMonth, mondayFirst: Boolean): Int =
    (calendarFirstDayOffset(month, mondayFirst) + month.lengthOfMonth() + 6) / 7
