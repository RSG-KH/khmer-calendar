// Copyright (c) 2026 RSG-KH | Apache-2.0 License
package com.rsgkh.calendar.notifications

import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import com.rsgkh.calendar.MainActivity
import com.rsgkh.calendar.R
import com.rsgkh.calendar.data.*
import com.rsgkh.calendar.i18n.L
import java.time.Duration
import java.time.Instant

object EventNotifications {
    const val CHANNEL = "calendar-events"
    const val ACTION_FIRE = "com.rsgkh.calendar.REMIND_EVENTS"
    const val EXTRA_DATE = "event_date"
    private const val NOTIFICATION_ID = 2001

    fun createChannel(context: Context) {
        val k = AppPreferences(context).read().khmer
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL, L.text("notifications.channel", k), NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = L.text("notifications.channel_description", k)
            })
    }
    fun canPost(context: Context): Boolean {
        val permission = Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        val manager = NotificationManagerCompat.from(context)
        return permission && manager.areNotificationsEnabled() && manager.getNotificationChannel(CHANNEL)?.importance != NotificationManager.IMPORTANCE_NONE
    }
    fun canBeExact(context: Context) = context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()
    fun clearDisplayed(context: Context) { NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID) }
    private fun pending(context: Context, at: Long = 0): PendingIntent = PendingIntent.getBroadcast(context, 0,
        Intent(context, EventReminderReceiver::class.java).setAction(ACTION_FIRE).putExtra("at", at),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

    fun rescheduleAsync(context: Context) = ReminderWork.submit(context) { reschedule(it) }
    fun clearDisplayedAsync(context: Context) = ReminderWork.submit(context) { clearDisplayed(it) }

    @Synchronized fun reschedule(context: Context, now: Instant = Instant.now()) {
        createChannel(context)
        val alarm = context.getSystemService(AlarmManager::class.java)
        alarm.cancel(pending(context))
        val settings = AppPreferences(context).read()
        val state = context.getSharedPreferences("reminder-state", Context.MODE_PRIVATE)
        state.edit { remove("scheduled") }
        if (!settings.notificationsEnabled || !canPost(context)) {
            NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
            return
        }
        val custom = CustomEventRepository(context).use { it.all() }
        val batch = ReminderPlanner.next(now, settings, custom) ?: return
        val millis = batch.at.toEpochMilli()
        state.edit { putLong("scheduled", millis) }
        val intent = pending(context, millis)
        try {
            if (canBeExact(context)) alarm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, intent)
            else alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, intent)
        } catch (_: SecurityException) {
            // Exact-alarm access can be revoked while the app is scheduling.
            alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, intent)
        }
    }

    @Synchronized fun deliver(context: Context, scheduled: Long, now: Instant = Instant.now()) {
        val state = context.getSharedPreferences("reminder-state", Context.MODE_PRIVATE)
        if (scheduled != state.getLong("scheduled", -1) || scheduled == state.getLong("delivered", -1)) return
        val at = Instant.ofEpochMilli(scheduled)
        val settings = AppPreferences(context).read()
        val batch = if (settings.notificationsEnabled && canPost(context))
            CustomEventRepository(context).use { ReminderPlanner.next(at.minusMillis(1), settings, it.all()) } else null
        if (batch?.at == at && now >= at &&
            (settings.repeatHours == 0 || Duration.between(at, now).toHours() < settings.repeatHours)) {
            val events = batch.events.filter { now < batch.expiresAt.getValue(it.key) }
            if (events.isNotEmpty()) {
                val date = events.first().date
                val k = settings.khmer
                val multipleDates = events.any { it.date != date }
                val lines = events.map { (if (multipleDates) "${it.date} · " else "") + (it.time?.let { time -> "$time · " } ?: "") + it.title(k) }
                val open = Intent(context, MainActivity::class.java).setData(Uri.parse("khmercalendar://date/$date"))
                    .putExtra(EXTRA_DATE, date.toString()).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                val content = PendingIntent.getActivity(context, 0, open, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                val notification = NotificationCompat.Builder(context, CHANNEL)
                    .setSmallIcon(R.drawable.ic_notification_calendar)
                    .setContentTitle(L.text("notifications.title", k, "date" to date))
                    .setContentText(lines.joinToString(" · "))
                    .setStyle(NotificationCompat.BigTextStyle().bigText(lines.joinToString("\n")))
                    .setCategory(NotificationCompat.CATEGORY_EVENT).setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                    .setContentIntent(content).setAutoCancel(true).build()
                try {
                    NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
                    state.edit { putLong("delivered", scheduled) }
                } catch (_: SecurityException) { /* Permission revoked during delivery. */ }
            }
        }
        reschedule(context, now)
    }
}

class EventReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == EventNotifications.ACTION_FIRE) {
            val scheduled = intent.getLongExtra("at", -1)
            val result = goAsync()
            ReminderWork.submit(context, onFinished = { result?.finish() }) {
                EventNotifications.deliver(it, scheduled)
            }
        }
    }
}
class ReminderRestoreReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action in listOf(Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED,
                Intent.ACTION_TIME_CHANGED, Intent.ACTION_TIMEZONE_CHANGED, AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED)) {
            val result = goAsync()
            ReminderWork.submit(context, onFinished = { result?.finish() }) {
                EventNotifications.reschedule(it)
            }
        }
    }
}
