// Copyright (c) 2026 RSG-KH | Apache-2.0 License
@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.rsgkh.calendar.ui

import com.rsgkh.calendar.i18n.L

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rsgkh.calendar.data.CustomEvent
import com.rsgkh.calendar.data.TodayTimeZone
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.UUID

@Composable internal fun CustomEventEditor(existing: CustomEvent?, initialDate: LocalDate, k: Boolean, timeZone: TodayTimeZone,
    onCancel: () -> Unit, onSave: (CustomEvent) -> Unit) {
    val id = rememberSaveable(existing?.id) { existing?.id ?: UUID.randomUUID().toString() }
    val zone = existing?.takeIf { it.repeat != null }?.zone ?: timeZone.zone()
    val displayed = remember(existing, zone) { existing?.instant?.atZone(zone) }
    val zoneLabel = if (zone == timeZone.zone()) timeZoneLabel(timeZone, k) else zone.id
    var title by rememberSaveable(id) { mutableStateOf(existing?.title.orEmpty()) }
    var dateText by rememberSaveable(id, zone.id) { mutableStateOf((displayed?.toLocalDate() ?: initialDate).toString()) }
    var timeText by rememberSaveable(id, zone.id) { mutableStateOf((displayed?.toLocalTime()?.withSecond(0)?.withNano(0) ?: LocalTime.of(9, 0)).toString()) }
    var notes by rememberSaveable(id) { mutableStateOf(existing?.notes.orEmpty()) }
    var repeat by rememberSaveable(id, stateSaver = RepeatDraft.Saver) { mutableStateOf(RepeatDraft.from(existing?.repeat)) }
    var datePicker by remember { mutableStateOf(false) }
    var endPicker by remember { mutableStateOf(false) }
    var timePicker by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf(false) }
    val date = runCatching { LocalDate.parse(dateText) }.getOrNull()?.takeIf { it.year in 1800..2200 }
    val time = runCatching { LocalTime.parse(timeText) }.getOrNull()?.takeIf { timeText.length == 5 }
    val clockGap = date != null && time != null && zone.rules.getValidOffsets(date.atTime(time)).isEmpty()
    Column(Modifier.widthIn(max = 640.dp).fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(horizontal = 10.dp, vertical = 20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(L.text(if (existing == null) "ui.add_event.bf2f10" else if (existing.repeat != null) "repeat.edit_series" else "ui.edit_event.c29d7a", k),
                fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
            val offset = ZonedDateTime.ofLocal((date ?: initialDate).atTime(time ?: LocalTime.NOON), zone, displayed?.offset).offset
            val offsetText = "UTC" + if (offset.totalSeconds == 0) "+0" else offset.id.replace(Regex(":00$"), "").replace(Regex("^([+-])0"), "$1")
            val headerZone = if (zone == timeZone.zone() && timeZone == TodayTimeZone.CAMBODIA) zoneLabel else "$zoneLabel ($offsetText)"
            Text(headerZone, Modifier.testTag("custom-time-zone"), fontSize = 12.readableSp, color = MaterialTheme.colorScheme.primary)
        }
        OutlinedTextField(title, { title = it.take(120) }, Modifier.fillMaxWidth().testTag("custom-title"), singleLine = true, label = { RequiredFieldLabel(L.text("ui.title.a4c172", k)) })
        OutlinedTextField(dateText, { dateText = it.take(10) }, Modifier.fillMaxWidth().testTag("custom-date"), singleLine = true,
            label = { Text(L.text("ui.event_date", k)) }, isError = date == null,
            supportingText = { Text("1800–2200") }, trailingIcon = { TextButton(onClick = { datePicker = true }) { Text(L.text("ui.pick.971faf", k)) } })
        OutlinedTextField(timeText, { timeText = it.take(5) }, Modifier.fillMaxWidth().testTag("custom-time"), singleLine = true,
            label = { Text(L.text("ui.time_hh_mm.8cf351", k)) }, isError = time == null || clockGap,
            trailingIcon = { TextButton(onClick = { timePicker = true }) { Text(L.text("ui.pick.971faf", k)) } })
        if (clockGap) Text(L.text("ui.this_time_does_not_exist_because_the_local_clock_change.49ab61", k), color = MaterialTheme.colorScheme.error)
        OutlinedTextField(notes, { notes = it.take(2000) }, Modifier.fillMaxWidth().testTag("custom-notes"), minLines = 2, maxLines = 2, label = { Text(L.text("ui.notes_optional.fde199", k)) })
        HorizontalDivider()
        EventRepeatEditor(repeat, date, k, { repeat = it }, { endPicker = true })
        if (existing?.repeat != null) Text(L.text("repeat.edit_hint", k), fontSize = 12.readableSp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (error) Text(L.text("ui.could_not_save_changes_please_try_again.140b3e", k), color = MaterialTheme.colorScheme.error)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onCancel) { Text(L.text("ui.cancel.5bf834", k)) }
            Button(enabled = title.isNotBlank() && date != null && time != null && !clockGap && repeat.valid(date), onClick = {
                runCatching {
                    val unchangedTime = displayed != null && displayed.toLocalDate() == date && displayed.toLocalTime().withSecond(0).withNano(0) == time
                    val event = if (existing != null && unchangedTime && !(existing.repeat == null && repeat.frequency != null)) existing.copy(title = title.trim(), notes = notes.trim()) else {
                        val chosen = ZonedDateTime.ofLocal(date!!.atTime(time!!), zone, displayed?.offset)
                        CustomEvent(id, title.trim(), date, time, notes.trim(), zoneId = zone.id, offsetSeconds = chosen.offset.totalSeconds)
                    }
                    onSave(event.copy(repeat = repeat.value()))
                }.onFailure { error = true }
            }) { Text(L.text("ui.save.1b0623", k)) }
        }
    }
    if (datePicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = (date ?: initialDate).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(), yearRange = 1800..2200)
        val pickerScrollState = rememberScrollState()
        CalendarDatePickerDialog(onDismissRequest = { datePicker = false }, confirmButton = {
            TextButton(enabled = state.selectedDateMillis != null, onClick = {
                dateText = Instant.ofEpochMilli(state.selectedDateMillis!!).atZone(ZoneOffset.UTC).toLocalDate().toString(); datePicker = false
            }) { Text(L.text("ui.ok.04c4aa", k)) }
        }, dismissButton = { TextButton(onClick = { datePicker = false }) { Text(L.text("ui.cancel.5bf834", k)) } }) {
            Column(Modifier.verticalScrollbar(pickerScrollState).verticalScroll(pickerScrollState)) {
                DatePicker(state)
            }
        }
    }
    if (timePicker) TimeSelectionDialog(time ?: LocalTime.of(9, 0), k, { timePicker = false }, zoneLabel) { timeText = it.toString(); timePicker = false }
    if (endPicker) {
        val minimum = date ?: LocalDate.of(1800, 1, 1)
        val selected = runCatching { LocalDate.parse(repeat.end) }.getOrNull()?.takeIf { it in minimum..LocalDate.of(2200, 12, 31) }
        val selectable = remember(minimum) { object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long) = Instant.ofEpochMilli(utcTimeMillis).atZone(ZoneOffset.UTC).toLocalDate() >= minimum
            override fun isSelectableYear(year: Int) = year >= minimum.year
        } }
        val state = rememberDatePickerState(initialSelectedDateMillis = selected?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli(),
            initialDisplayedMonthMillis = (selected ?: minimum).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
            yearRange = 1800..2200, selectableDates = selectable)
        val scroll = rememberScrollState()
        CalendarDatePickerDialog(onDismissRequest = { endPicker = false }, confirmButton = {
            TextButton(enabled = state.selectedDateMillis != null, onClick = {
                repeat = repeat.copy(end = Instant.ofEpochMilli(state.selectedDateMillis!!).atZone(ZoneOffset.UTC).toLocalDate().toString()); endPicker = false
            }) { Text(L.text("ui.ok.04c4aa", k)) }
        }, dismissButton = { TextButton(onClick = { endPicker = false }) { Text(L.text("ui.cancel.5bf834", k)) } }) {
            Column(Modifier.verticalScrollbar(scroll).verticalScroll(scroll)) { DatePicker(state) }
        }
    }
}
