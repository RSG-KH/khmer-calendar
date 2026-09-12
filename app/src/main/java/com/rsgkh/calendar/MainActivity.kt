// Copyright (c) 2026 RSG-KH | Apache-2.0 License
package com.rsgkh.calendar

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import com.rsgkh.calendar.data.*
import com.rsgkh.calendar.notifications.EventNotifications
import com.rsgkh.calendar.ui.CalendarApp
import com.rsgkh.calendar.ui.NotificationAccess
import kotlinx.coroutines.delay
import java.time.LocalDate

class MainActivity : ComponentActivity() {
    private val preferences by lazy { AppPreferences(this) }
    private val customRepository by lazy { CustomEventRepository(this) }
    private var revision by mutableIntStateOf(0)
    private var openDateRequest by mutableStateOf<Pair<LocalDate, Long>?>(null)
    private var awaitingNotificationPermission = false
    private val permission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        awaitingNotificationPermission = false
        preferences.write(preferences.read().copy(notificationsEnabled = granted))
        revision++
        EventNotifications.reschedule(this)
    }
    private fun requestNotificationAccess() {
        awaitingNotificationPermission = true
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            val prefs = getSharedPreferences("permission_state", Context.MODE_PRIVATE)
            val alreadyRequested = prefs.getBoolean("post_notifications_requested", false)
            if (!alreadyRequested) {
                prefs.edit().putBoolean("post_notifications_requested", true).apply()
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
            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        EventNotifications.createChannel(this)
        readDateIntent(intent)
        setContent {
            val settings = remember(revision) { preferences.read() }
            val custom = remember(revision) { customRepository.all() }
            val access = remember(revision) { NotificationAccess(EventNotifications.canPost(this), EventNotifications.canBeExact(this)) }
            val today by produceState(settings.todayTimeZone.today(), settings.todayTimeZone, revision) {
                while (true) { value = settings.todayTimeZone.today(); delay(30_000) }
            }
            CalendarApp(settings, today, customEvents = custom,
                onSaveCustom = { customRepository.save(it); EventNotifications.clearDisplayed(this); changed() },
                onDeleteCustom = { customRepository.delete(it); EventNotifications.clearDisplayed(this); changed() },
                notificationAccess = access,
                onOpenNotificationSettings = { requestNotificationAccess() },
                onAllowExact = { startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:$packageName"))) },
                openDateRequest = openDateRequest,
            ) { next ->
                preferences.write(next)
                changed()
                if (next.notificationsEnabled && !EventNotifications.canPost(this)) {
                    requestNotificationAccess()
                }
            }
        }
    }
    private fun changed() { revision++; EventNotifications.reschedule(this) }
    override fun onResume() {
        super.onResume()
        if (awaitingNotificationPermission) {
            awaitingNotificationPermission = false
            if (EventNotifications.canPost(this)) {
                preferences.write(preferences.read().copy(notificationsEnabled = true))
            }
        }
        changed()
    }
    override fun onNewIntent(intent: Intent) { super.onNewIntent(intent); setIntent(intent); readDateIntent(intent) }
    private fun readDateIntent(intent: Intent) {
        val date = runCatching { LocalDate.parse(intent.getStringExtra(EventNotifications.EXTRA_DATE)) }.getOrNull()
        if (date != null && date.year in 1800..2200) openDateRequest = date to System.nanoTime()
    }
    override fun onDestroy() { customRepository.close(); super.onDestroy() }
}
