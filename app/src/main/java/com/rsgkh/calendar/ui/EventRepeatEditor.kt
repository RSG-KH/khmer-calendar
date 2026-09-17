// Copyright (c) 2026 RSG-KH | Apache-2.0 License
package com.rsgkh.calendar.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.rsgkh.calendar.domain.EventRepeat
import com.rsgkh.calendar.domain.RepeatFrequency
import com.rsgkh.calendar.i18n.CalendarWords
import com.rsgkh.calendar.i18n.L
import java.time.LocalDate

internal data class RepeatDraft(
    val frequency: RepeatFrequency? = null, val end: String = "", val interval: String = "3",
    val thirty: Boolean = false, val february: Boolean = false,
) {
    fun value(): EventRepeat? = frequency?.let { frequency ->
        val until = runCatching { LocalDate.parse(end) }.getOrNull() ?: return null
        EventRepeat(frequency, until, if (frequency == RepeatFrequency.DAYS) interval.toLongOrNull() ?: 0 else 3,
            frequency == RepeatFrequency.MONTHLY && thirty,
            frequency in listOf(RepeatFrequency.MONTHLY, RepeatFrequency.YEARLY) && february)
    }
    fun valid(start: LocalDate?) = frequency == null || (start != null && value()?.isValid(start) == true)

    companion object {
        fun from(repeat: EventRepeat?) = if (repeat == null) RepeatDraft() else RepeatDraft(repeat.frequency,
            repeat.until.toString(), repeat.interval.toString(), repeat.includeThirty, repeat.includeFebruary)
        val Saver = listSaver<RepeatDraft, String>(
            save = { listOf(it.frequency?.name.orEmpty(), it.end, it.interval, it.thirty.toString(), it.february.toString()) },
            restore = { RepeatDraft(it[0].takeIf(String::isNotEmpty)?.let(RepeatFrequency::valueOf), it[1], it[2], it[3].toBoolean(), it[4].toBoolean()) })
    }
}

internal fun repeatDateLabel(date: LocalDate, k: Boolean): String {
    val month = CalendarWords.month(date.monthValue, k, short = true)
    val day = CalendarWords.number(date.dayOfMonth, k)
    val year = CalendarWords.number(date.year, k)
    return if (k) "$day $month $year" else "$month $day, $year"
}

@Composable internal fun EventRepeatEditor(draft: RepeatDraft, start: LocalDate?, k: Boolean,
    onChange: (RepeatDraft) -> Unit, onPickEnd: () -> Unit) {
    val rule = draft.value()
    val preview = remember(start, rule) { if (start != null && rule?.isValid(start) == true) rule.preview(start) else null }
    Column(Modifier.fillMaxWidth().testTag("custom-repeat"), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(L.text("repeat.label", k), fontSize = 14.readableSp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            (listOf<RepeatFrequency?>(null) + RepeatFrequency.entries).forEach { frequency ->
                SelectionChip(selected = draft.frequency == frequency, onClick = { onChange(draft.copy(frequency = frequency)) },
                    modifier = Modifier.testTag("repeat-${frequency?.key ?: "none"}"),
                    label = { Text(L.text("repeat.${frequency?.key ?: "none"}", k), fontSize = 13.readableSp) })
            }
        }
        if (draft.frequency == RepeatFrequency.DAYS) {
            val invalid = draft.interval.toLongOrNull()?.let { it in 1..9_007_199_254_740_991L } != true
            OutlinedTextField(draft.interval, { onChange(draft.copy(interval = it)) },
                Modifier.fillMaxWidth().testTag("repeat-interval"), singleLine = true,
                label = { Text(L.text("repeat.interval", k)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), isError = invalid,
                supportingText = if (invalid) ({ Text(L.text("repeat.invalid_interval", k)) }) else null)
        }
        if (draft.frequency != null) {
            val until = runCatching { LocalDate.parse(draft.end) }.getOrNull()
            val invalidEnd = draft.end.isNotEmpty() && (until == null || until.year !in 1800..2200 || start?.let { until < it } == true)
            OutlinedTextField(draft.end, { onChange(draft.copy(end = it.take(10))) },
                Modifier.fillMaxWidth().testTag("repeat-end"), singleLine = true,
                label = { RequiredFieldLabel(L.text("repeat.end", k)) }, isError = invalidEnd,
                supportingText = if (invalidEnd) ({ Text(L.text("repeat.invalid_end", k)) }) else null,
                trailingIcon = { TextButton(onClick = onPickEnd) { Text(L.text("ui.pick.971faf", k)) } })
        }
        if (preview != null) {
            if (preview.affectsThirty || preview.affectsFebruary) {
                Column {
                    if (preview.affectsThirty) RepeatIncludeSwitch(L.text("repeat.include_thirty", k), checked = draft.thirty,
                        onChange = { onChange(draft.copy(thirty = it)) })
                    if (preview.affectsFebruary) RepeatIncludeSwitch(L.text(if (start?.dayOfMonth == 29) "repeat.include_february_28" else "repeat.include_february", k),
                        checked = draft.february, onChange = { onChange(draft.copy(february = it)) })
                }
            }
            Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.fillMaxWidth().testTag("repeat-preview")) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(L.text("repeat.scheduled", k), fontSize = 14.readableSp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    val dates = preview.dates
                    val shown = if (dates.size > 5) dates.take(3) + listOf(null, dates.last()) else dates
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        shown.forEach { date ->
                            Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surface) {
                                Text(date?.let { repeatDateLabel(it, k) } ?: "…", Modifier.padding(horizontal = 8.dp, vertical = 4.dp), fontSize = 12.readableSp)
                            }
                        }
                    }
                    dates.lastOrNull()?.let { last ->
                        Text(L.text(if (dates.size == 1) "repeat.count_one" else "repeat.count_many", k,
                            "count" to CalendarWords.number(dates.size, k), "date" to repeatDateLabel(last, k)),
                            Modifier.testTag("repeat-count"), fontSize = 12.readableSp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (preview.skipped.isNotEmpty()) {
                        val skipped = preview.skipped.take(3).joinToString(", ") { date ->
                            val year = CalendarWords.number(date.year, k)
                            if (draft.frequency == RepeatFrequency.YEARLY) year else "${CalendarWords.month(date.monthValue, k, true)} $year"
                        } + if (preview.skipped.size > 3) " …" else ""
                        Text(L.text("repeat.skipped", k, "dates" to skipped), Modifier.testTag("repeat-skipped"),
                            fontSize = 12.readableSp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable private fun RepeatIncludeSwitch(title: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(title, Modifier.weight(1f), fontSize = 14.readableSp, lineHeight = 22.readableSp,
            fontWeight = FontWeight.Normal, color = MaterialTheme.colorScheme.onSurface)
        Switch(checked = checked, onCheckedChange = onChange, modifier = Modifier.semantics { contentDescription = title })
    }
}
