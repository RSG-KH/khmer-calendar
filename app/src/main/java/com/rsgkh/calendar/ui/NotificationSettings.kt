// Copyright (c) 2026 RSG-KH | Apache-2.0 License
@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.rsgkh.calendar.ui

import com.rsgkh.calendar.i18n.L

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.rsgkh.calendar.data.AppSettings
import kotlinx.coroutines.launch
import java.time.LocalTime

data class NotificationAccess(val canPost: Boolean = true, val exact: Boolean = true)

@Composable internal fun NotificationSettingsCard(settings: AppSettings, onChange: (AppSettings) -> Unit,
    access: NotificationAccess, onSystemSettings: () -> Unit, onAllowExact: () -> Unit) {
    val k = settings.khmer
    var timePicker by remember(settings.notificationsEnabled) { mutableStateOf(false) }
    fun intervalLabel(hours: Int) = if (hours == 0) L.text("ui.off.9af9e6", k) else L.text("common.reminder_hours", k, "hours" to hours)
    val notificationsActive = settings.notificationsEnabled && access.canPost
    val fullyGranted = notificationsActive && access.exact

    SettingsCard(L.text("ui.notifications.ddc781", k)) {
        SettingSwitch(L.text("ui.enable_event_notifications.b4d4f8", k),
            L.text("ui.reminders_for_holidays_and_events.fe018c", k),
            checked = notificationsActive) { on ->
            if (on) {
                if (!access.canPost) onSystemSettings()
                else onChange(settings.copy(notificationsEnabled = true))
            } else {
                onChange(settings.copy(notificationsEnabled = false))
            }
        }
        if (notificationsActive && !access.exact) {
            val warningColor = if (MaterialTheme.colorScheme.surface.luminance() > .5f) Color(0xFFB45309) else Color(0xFFFFB36B)
            Text(L.text("ui.the_app_won_t_guarantee_to_fire_notifications_correctly.b4cad2", k),
                modifier = Modifier.padding(top = 8.dp), fontSize = 12.readableSp, fontWeight = FontWeight.Bold, color = warningColor)
            Button(onClick = onAllowExact, modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                Text(L.text("ui.allow_precise_reminders.733d9d", k), fontSize = 14.readableSp,
                    fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
            }
        }
        if (fullyGranted) {
            SettingSwitch(L.text("notifications.push_custom", k), L.text("notifications.push_custom_subtitle", k), checked = settings.pushCustomEvents) {
                onChange(settings.copy(pushCustomEvents = it))
            }
            SettingSwitch(L.text("notifications.push_holidays", k), L.text("notifications.push_holidays_subtitle", k), checked = settings.pushHolidays) {
                onChange(settings.copy(pushHolidays = it))
            }
            if (settings.showObservances) {
                SettingSwitch(L.text("notifications.push_observances", k), L.text("notifications.push_observances_subtitle", k), checked = settings.pushObservances) {
                    onChange(settings.copy(pushObservances = it))
                }
            }
            if (settings.showHolyDaysInEvents) {
                SettingSwitch(L.text("notifications.push_holy_days", k), L.text("notifications.push_holy_days_subtitle", k), checked = settings.pushHolyDays) {
                    onChange(settings.copy(pushHolyDays = it))
                }
            }
            SettingsRow(L.text("ui.push_time.8421c3", k),
                L.text("ui.time_to_deliver_daily_reminders.7806df", k)) {
                TextButton(onClick = { timePicker = true },
                    modifier = Modifier.testTag("choose-push-time").semantics { contentDescription = L.text("notifications.choose_push_time", k) }) {
                    Text(LocalTime.of(settings.pushMinutes / 60, settings.pushMinutes % 60).toString(), fontSize = 13.readableSp)
                }
            }
            SettingsRow(L.text("ui.remind_every.a38a5d", k),
                L.text("ui.repeat_interval_throughout_the_day.46e808", k)) {
                SettingDropdown(settings.repeatHours, listOf(0, 2, 4, 6, 8, 12), ::intervalLabel,
                    "reminder-interval", L.text("notifications.reminder_interval", k)) { onChange(settings.copy(repeatHours = it)) }
            }
        }
    }
    if (fullyGranted && timePicker) TimeSelectionDialog(
        initial = LocalTime.of(settings.pushMinutes / 60, settings.pushMinutes % 60),
        k = k,
        onDismiss = { timePicker = false },
        zoneLabel = timeSelectionZoneLabel(settings.todayTimeZone, k)
    ) {
        onChange(settings.copy(pushMinutes = it.hour * 60 + it.minute)); timePicker = false
    }
}

private const val WheelCycles = 200

@Composable
private fun WheelBox(
    initialValue: Int,
    count: Int,
    onValueChange: (Int) -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
    val itemHeight = 44.dp
    val itemHeightPx = with(density) { itemHeight.toPx() }
    val startIndex = (WheelCycles / 2) * count + ((initialValue % count + count) % count)
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = startIndex - 1)
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState, snapPosition = SnapPosition.Center)

    val selectedIndex by remember {
        derivedStateOf {
            val offset = listState.firstVisibleItemScrollOffset
            val base = listState.firstVisibleItemIndex
            val centerIndex = if (offset > itemHeightPx / 2f) base + 2 else base + 1
            (centerIndex % count + count) % count
        }
    }

    LaunchedEffect(selectedIndex) {
        onValueChange(selectedIndex)
    }

    Box(
        modifier = modifier
            .width(88.dp)
            .height(132.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(itemHeight)
                .align(Alignment.Center)
                .padding(horizontal = 6.dp)
                .background(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(10.dp)
                )
                .border(
                    width = 1.5.dp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.65f),
                    shape = RoundedCornerShape(10.dp)
                )
        )

        LazyColumn(
            state = listState,
            flingBehavior = flingBehavior,
            modifier = Modifier
                .fillMaxSize()
                .testTag(testTag),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            items(count = count * WheelCycles, key = { it }) { index ->
                val itemValue = (index % count + count) % count
                val isSelected = itemValue == selectedIndex
                Box(
                    modifier = Modifier
                        .height(itemHeight)
                        .fillMaxWidth()
                        .clickable {
                            coroutineScope.launch {
                                listState.animateScrollToItem(index - 1)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "%02d".format(itemValue),
                        fontSize = if (isSelected) 30.sp else 20.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                    )
                }
            }
        }
    }
}

@Composable internal fun TimeSelectionDialog(
    initial: LocalTime,
    k: Boolean,
    onDismiss: () -> Unit,
    zoneLabel: String = L.text("ui.cambodia_time_utc_7.6b9f2d", k),
    onSelect: (LocalTime) -> Unit
) {
    var selectedHour by rememberSaveable(initial) { mutableIntStateOf(initial.hour) }
    var selectedMinute by rememberSaveable(initial) { mutableIntStateOf(initial.minute) }

    CalendarAlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .padding(horizontal = 24.dp)
            .widthIn(max = 320.dp)
            .fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(
                L.text("ui.select_time.eacac3", k),
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    WheelBox(
                        initialValue = initial.hour,
                        count = 24,
                        onValueChange = { selectedHour = it },
                        testTag = "wheel-hour"
                    )
                    Box(
                        modifier = Modifier.width(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = ":",
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.offset(y = (-2).dp)
                        )
                    }
                    WheelBox(
                        initialValue = initial.minute,
                        count = 60,
                        onValueChange = { selectedMinute = it },
                        testTag = "wheel-minute"
                    )
                }
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = L.text("ui.hour.5f4163", k),
                        fontSize = 12.readableSp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.width(88.dp)
                    )
                    Spacer(Modifier.width(24.dp))
                    Text(
                        text = L.text("ui.minute.823acf", k),
                        fontSize = 12.readableSp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.width(88.dp)
                    )
                }
                Spacer(Modifier.height(18.dp))
                Text(
                    text = L.text("common.clock_label", k, "zone" to zoneLabel),
                    fontSize = 12.readableSp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(L.text("ui.cancel.5bf834", k))
            }
        },
        confirmButton = {
            TextButton(onClick = { onSelect(LocalTime.of(selectedHour, selectedMinute)) }) {
                Text(L.text("ui.ok.04c4aa", k))
            }
        }
    )
}
