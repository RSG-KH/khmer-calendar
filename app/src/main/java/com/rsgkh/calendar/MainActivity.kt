// Copyright (c) 2026 RSG-KH | Apache-2.0 License
package com.rsgkh.calendar

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import com.rsgkh.calendar.data.*
import com.rsgkh.calendar.notifications.EventNotifications
import com.rsgkh.calendar.ui.CalendarApp
import com.rsgkh.calendar.ui.NotificationAccess
import com.rsgkh.calendar.widgets.WidgetUpdater
import java.time.LocalDate

class MainActivity : ComponentActivity() {
    private val preferences by lazy { AppPreferences(this) }
    private val customRepository by lazy { CustomEventRepository(this) }
    private var revision by mutableIntStateOf(0)
    private var customRevision by mutableIntStateOf(0)
    private var openDateRequest by mutableStateOf<Pair<LocalDate, Long>?>(null)
    private var openWidgetEventRequest by mutableStateOf<com.rsgkh.calendar.widgets.WidgetEventRequest?>(null)
    private var awaitingNotificationPermission = false
    private val permission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        awaitingNotificationPermission = false
        preferences.write(preferences.read().copy(notificationsEnabled = granted))
        revision++
        EventNotifications.rescheduleAsync(this)
    }
    private fun requestNotificationAccess() {
        awaitingNotificationPermission = true
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            val prefs = getSharedPreferences("permission_state", Context.MODE_PRIVATE)
            val alreadyRequested = prefs.getBoolean("post_notifications_requested", false)
            if (!alreadyRequested) {
                prefs.edit { putBoolean("post_notifications_requested", true) }
                permission.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                permission.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                openAppNotificationSettings()
            }
        } else {
            openAppNotificationSettings()
        }
    }

    private fun openAppNotificationSettings() {
        try {
            startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, packageName))
        } catch (_: Exception) {
            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, "package:$packageName".toUri()))
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // minSdk 31: no older cutout compatibility is needed. Pre-35 transparent
        // system bars are configured in the theme; Android 15+ provides them.
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.attributes = window.attributes.apply {
            layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
        }
        EventNotifications.createChannel(this)
        WidgetUpdater.setWidgetsEnabled(this, preferences.read().widgetsEnabled)
        readDateIntent(intent)
        setContent {
            val settings = remember(revision) { preferences.read() }
            val custom = remember(customRevision) { customRepository.all() }
            val access = remember(revision) { NotificationAccess(EventNotifications.canPost(this), EventNotifications.canBeExact(this)) }
            val today by produceState(settings.todayTimeZone.today(), settings.todayTimeZone, revision) {
                lifecycle.refreshTodayWhileVisible({ settings.todayTimeZone.today() }) { value = it }
            }
            CalendarApp(settings, today, customEvents = custom,
                onSaveCustom = { customRepository.save(it); customRevision++; EventNotifications.clearDisplayedAsync(this); changed() },
                onDeleteCustom = { customRepository.delete(it); customRevision++; EventNotifications.clearDisplayedAsync(this); changed() },
                notificationAccess = access,
                onOpenNotificationSettings = { requestNotificationAccess() },
                onAllowExact = { startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, "package:$packageName".toUri())) },
                openDateRequest = openDateRequest,
                openWidgetEventRequest = openWidgetEventRequest,
            ) { updateSettings(it) }
        }
    }
    internal fun updateSettings(next: AppSettings) {
        val previous = preferences.read()
        preferences.write(next)
        revision++
        com.rsgkh.calendar.widgets.WidgetUpdater.requestUpdate(this)
        if (next.widgetsEnabled != previous.widgetsEnabled) {
            WidgetUpdater.setWidgetsEnabled(this, next.widgetsEnabled)
        }
        if (next.remindersDifferFrom(previous)) EventNotifications.rescheduleAsync(this)
        // Delivery reads the current language; updating channel labels needs no new alarm.
        if (next.khmer != previous.khmer) EventNotifications.createChannel(this)
        if (next.notificationsEnabled && !previous.notificationsEnabled && !EventNotifications.canPost(this)) {
            requestNotificationAccess()
        }
    }
    private fun changed() {
        revision++
        EventNotifications.rescheduleAsync(this)
        com.rsgkh.calendar.widgets.WidgetUpdater.requestUpdate(this)
    }
    override fun onResume() {
        super.onResume()
        if (awaitingNotificationPermission) {
            awaitingNotificationPermission = false
            if (EventNotifications.canPost(this)) {
                preferences.write(preferences.read().copy(notificationsEnabled = true))
            }
        }
        customRevision++
        changed()
    }
    override fun onNewIntent(intent: Intent) { super.onNewIntent(intent); setIntent(intent); readDateIntent(intent) }
    private fun readDateIntent(intent: Intent) {
        val date = runCatching { LocalDate.parse(intent.getStringExtra(EventNotifications.EXTRA_DATE)) }.getOrNull()
        if (date != null && date.year in 1800..2200) {
            val nonce = System.nanoTime()
            openDateRequest = date to nonce
            openWidgetEventRequest = intent.getStringExtra(com.rsgkh.calendar.widgets.WidgetNavigation.EXTRA_EVENT_ID)
                ?.takeIf { it.isNotBlank() }
                ?.let { com.rsgkh.calendar.widgets.WidgetEventRequest(date, it, nonce) }
        }
    }
    override fun onDestroy() { customRepository.close(); super.onDestroy() }
}
