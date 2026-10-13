// Copyright (c) 2026 RSG-KH | Apache-2.0 License
package com.rsgkh.calendar.ui

import android.content.res.Configuration
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.rsgkh.calendar.data.AppSettings
import com.rsgkh.calendar.i18n.CalendarWords
import java.time.DayOfWeek
import kotlin.math.roundToInt

@Composable
internal fun CalendarWeekdayHeader(settings: AppSettings, includeTestTags: Boolean = true) {
    val config = LocalConfiguration.current
    val isPhoneLandscape = config.orientation == Configuration.ORIENTATION_LANDSCAPE && config.smallestScreenWidthDp < 600
    val weekdays = (0..6).map { CalendarWords.weekday(it, settings.khmer, if (settings.showLongerWeekdayNames) "grid_long" else "grid") }
    val ordered = if (settings.mondayFirst) weekdays.drop(1) + weekdays.first() else weekdays
    val colors = MaterialTheme.colorScheme
    val measurer = rememberTextMeasurer()
    BoxWithConstraints(Modifier.fillMaxWidth().padding(bottom = if (isPhoneLandscape) 3.dp else 8.dp)) {
        val density = LocalDensity.current
        val rowWidth = with(density) { maxWidth.toPx() }
        // A pixel of slack accounts for integer placement and glyph-width rounding.
        val edgeInset = with(density) { 2.dp.toPx() } + 1f
        val gap = with(density) { 4.dp.toPx() } + 1f
        var style = LocalTextStyle.current.copy(fontSize = 11.readableSp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
        // Fit actual text, allowing a long name to borrow empty space from its neighbors.
        // Remeasure after shrinking because font hinting need not scale perfectly linearly.
        for (attempt in 0..3) {
            val widths = ordered.map { measurer.measure(it, style, softWrap = false, maxLines = 1).size.width.coerceAtLeast(1) }
            var scale = 1f
            widths.forEachIndexed { index, width ->
                val center = rowWidth * (index + 0.5f) / ordered.size
                scale = minOf(scale, 2 * (minOf(center, rowWidth - center) - edgeInset) / width)
                if (index > 0) {
                    scale = minOf(scale, 2 * (rowWidth / ordered.size - gap) / (widths[index - 1] + width))
                }
            }
            if (scale >= 1f || scale <= 0f) break
            style = style.copy(fontSize = style.fontSize * scale)
        }
        val labelWidths = ordered.map { measurer.measure(it, style, softWrap = false, maxLines = 1).size.width }
        Layout(modifier = Modifier.fillMaxWidth(), content = {
            ordered.forEachIndexed { index, name ->
                val weekday = DayOfWeek.of(if (settings.mondayFirst) index + 1 else if (index == 0) 7 else index)
                val color = when {
                    settings.highlightWeekdayNames -> weekdayNameColor(weekday, colors.surface.luminance() < 0.5f)
                    settings.highlightSunday && weekday == DayOfWeek.SUNDAY -> colors.tertiary
                    else -> colors.onSurfaceVariant
                }
                Text(name, if (includeTestTags) Modifier.testTag("weekday-header-${weekday.value}") else Modifier,
                    style = style, color = color, maxLines = 1, softWrap = false)
            }
        }) { measurables, constraints ->
            val labels = measurables.mapIndexed { index, label ->
                label.measure(Constraints.fixedWidth(labelWidths[index]))
            }
            val height = labels.maxOf { it.height }
            layout(constraints.maxWidth, height) {
                labels.forEachIndexed { index, label ->
                    val center = constraints.maxWidth * (index + 0.5f) / labels.size
                    label.placeRelative((center - label.width / 2f).roundToInt(), (height - label.height) / 2)
                }
            }
        }
    }
}
