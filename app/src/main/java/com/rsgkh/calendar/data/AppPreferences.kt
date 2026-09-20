// Copyright (c) 2026 RSG-KH | Apache-2.0 License
package com.rsgkh.calendar.data

import android.content.Context
import androidx.core.content.edit
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.abs

enum class ThemeMode { SYSTEM, LIGHT, DARK }
enum class Accent { BLUE, LAVENDER, ROSE, AMBER, LIME }
enum class FontScale(val multiplier: Float, val label: String) {
    PERCENT_80(0.80f, "80%"),
    PERCENT_90(0.90f, "90%"),
    PERCENT_100(1.00f, "100%"),
    PERCENT_110(1.10f, "110%"),
    PERCENT_120(1.20f, "120%"),
    PERCENT_130(1.30f, "130%"),
    PERCENT_140(1.40f, "140%"),
    PERCENT_150(1.50f, "150%")
}
enum class TodayTimeZone {
    LOCAL, CAMBODIA;

    fun zone(localZone: ZoneId = ZoneId.systemDefault()): ZoneId = if (this == LOCAL) localZone else CAMBODIA_ZONE

    fun today(now: Instant = Instant.now(), localZone: ZoneId = ZoneId.systemDefault()): LocalDate =
        now.atZone(zone(localZone)).toLocalDate()

    fun offsetLabel(now: Instant = Instant.now(), localZone: ZoneId = ZoneId.systemDefault()): String {
        val offset = zone(localZone).rules.getOffset(now)
        val totalSeconds = offset.totalSeconds
        val sign = if (totalSeconds >= 0) "+" else "-"
        val absSeconds = abs(totalSeconds)
        val hours = absSeconds / 3600
        val minutes = (absSeconds % 3600) / 60
        return if (minutes == 0) "UTC$sign$hours" else "UTC$sign$hours:${minutes.toString().padStart(2, '0')}"
    }
}
data class AppSettings(
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val accent: Accent = Accent.BLUE,
    val backgroundAccent: Boolean = true,
    val khmer: Boolean = true,
    val mondayFirst: Boolean = false,
    val showLongerWeekdayNames: Boolean = false,
    val showCopyButtons: Boolean = false,
    val highlightWeekdayNames: Boolean = true,
    val showLunar: Boolean = true,
    val showHolyDaysInCalendar: Boolean = true,
    val showHolyDaysInEvents: Boolean = false,
    val highlightSunday: Boolean = true,
    val notificationsEnabled: Boolean = false,
    val pushCustomEvents: Boolean = true,
    val pushHolidays: Boolean = true,
    val pushObservances: Boolean = true,
    val pushHolyDays: Boolean = showHolyDaysInEvents,
    val pushMinutes: Int = 5 * 60,
    val repeatHours: Int = 0,
    val todayTimeZone: TodayTimeZone = TodayTimeZone.LOCAL,
    val fontScale: FontScale = FontScale.PERCENT_100,
    val showWesternZodiac: Boolean = true,
    val widgetsEnabled: Boolean = false,
    val widgetShowPersonal: Boolean = true,
    val widgetShowHolidays: Boolean = true,
    val widgetShowObservances: Boolean = true,
    val widgetHidePersonalDetails: Boolean = false,
)

/** Only settings used to select an alarm's events or time belong here. */
internal fun AppSettings.remindersDifferFrom(other: AppSettings): Boolean =
    notificationsEnabled != other.notificationsEnabled ||
        pushCustomEvents != other.pushCustomEvents || pushHolidays != other.pushHolidays ||
        pushObservances != other.pushObservances || pushHolyDays != other.pushHolyDays ||
        showHolyDaysInEvents != other.showHolyDaysInEvents || pushMinutes != other.pushMinutes ||
        repeatHours != other.repeatHours || todayTimeZone != other.todayTimeZone

class AppPreferences(context: Context) {
    private val prefs = context.getSharedPreferences("appearance", Context.MODE_PRIVATE)
    fun read(): AppSettings {
        val legacyHolyDays = prefs.getBoolean("showHolyDays", true)
        val showHolyDaysInEvents = prefs.getBoolean("showHolyDaysInEvents", false)
        val showWesternZodiac = if (prefs.contains("showWesternZodiac")) {
            prefs.getBoolean("showWesternZodiac", true)
        } else {
            !prefs.getBoolean("hideWesternZodiac", false)
        }
        return AppSettings(
            theme = ThemeMode.entries.firstOrNull { it.name == prefs.getString("theme", "SYSTEM") } ?: ThemeMode.SYSTEM,
            accent = Accent.entries.firstOrNull { it.name == prefs.getString("accent", "BLUE") } ?: Accent.BLUE,
            backgroundAccent = prefs.getBoolean("backgroundAccent", true),
            khmer = prefs.getBoolean("khmer", true),
            mondayFirst = prefs.getBoolean("mondayFirst", false),
            showLongerWeekdayNames = prefs.getBoolean("showLongerWeekdayNames", false),
            showCopyButtons = prefs.getBoolean("showCopyButtons", false),
            highlightWeekdayNames = prefs.getBoolean("highlightWeekdayNames", true),
            showLunar = prefs.getBoolean("showLunar", true),
            showHolyDaysInCalendar = prefs.getBoolean("showHolyDaysInCalendar", legacyHolyDays),
            showHolyDaysInEvents = showHolyDaysInEvents,
            highlightSunday = prefs.getBoolean("highlightSunday", true),
            notificationsEnabled = prefs.getBoolean("notificationsEnabled", false),
            pushCustomEvents = prefs.getBoolean("pushCustomEvents", true),
            pushHolidays = prefs.getBoolean("pushHolidays", true),
            pushObservances = prefs.getBoolean("pushObservances", true),
            pushHolyDays = prefs.getBoolean("pushHolyDays", showHolyDaysInEvents),
            pushMinutes = prefs.getInt("pushMinutes", 300).coerceIn(0, 1439),
            repeatHours = prefs.getInt("repeatHours", 0).takeIf { it in listOf(0, 2, 4, 6, 8, 12) } ?: 0,
            todayTimeZone = TodayTimeZone.entries.firstOrNull { it.name == prefs.getString("todayTimeZone", "LOCAL") } ?: TodayTimeZone.LOCAL,
            fontScale = FontScale.entries.firstOrNull { it.name == prefs.getString("fontScale", "PERCENT_100") } ?: FontScale.PERCENT_100,
            showWesternZodiac = showWesternZodiac,
            widgetsEnabled = prefs.getBoolean("widgetsEnabled", false),
            widgetShowPersonal = prefs.getBoolean("widgetShowPersonal", true),
            widgetShowHolidays = prefs.getBoolean("widgetShowHolidays", true),
            widgetShowObservances = prefs.getBoolean("widgetShowObservances", true),
            widgetHidePersonalDetails = prefs.getBoolean("widgetHidePersonalDetails", false),
        )
    }
    fun write(settings: AppSettings) {
        prefs.edit {
            putString("theme", settings.theme.name)
            putString("accent", settings.accent.name)
            putBoolean("backgroundAccent", settings.backgroundAccent)
            putBoolean("khmer", settings.khmer)
            putBoolean("mondayFirst", settings.mondayFirst)
            putBoolean("showLongerWeekdayNames", settings.showLongerWeekdayNames)
            putBoolean("showCopyButtons", settings.showCopyButtons)
            putBoolean("highlightWeekdayNames", settings.highlightWeekdayNames)
            putBoolean("showLunar", settings.showLunar)
            putBoolean("showHolyDaysInCalendar", settings.showHolyDaysInCalendar)
            putBoolean("showHolyDaysInEvents", settings.showHolyDaysInEvents)
            putBoolean("showHolyDays", settings.showHolyDaysInCalendar && settings.showHolyDaysInEvents)
            putBoolean("highlightSunday", settings.highlightSunday)
            putBoolean("notificationsEnabled", settings.notificationsEnabled)
            putBoolean("pushCustomEvents", settings.pushCustomEvents)
            putBoolean("pushHolidays", settings.pushHolidays)
            putBoolean("pushObservances", settings.pushObservances)
            putBoolean("pushHolyDays", settings.pushHolyDays)
            putInt("pushMinutes", settings.pushMinutes)
            putInt("repeatHours", settings.repeatHours)
            putString("todayTimeZone", settings.todayTimeZone.name)
            putString("fontScale", settings.fontScale.name)
            putBoolean("showWesternZodiac", settings.showWesternZodiac)
            putBoolean("widgetsEnabled", settings.widgetsEnabled)
            putBoolean("widgetShowPersonal", settings.widgetShowPersonal)
            putBoolean("widgetShowHolidays", settings.widgetShowHolidays)
            putBoolean("widgetShowObservances", settings.widgetShowObservances)
            putBoolean("widgetHidePersonalDetails", settings.widgetHidePersonalDetails)
            remove("hideWesternZodiac")
        }
    }
}
