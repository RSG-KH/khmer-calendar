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
    val khmer: Boolean = true,
    val mondayFirst: Boolean = false,
    val showCopyButtons: Boolean = false,
    val showLunar: Boolean = true,
    val showHolyDaysInCalendar: Boolean = true,
    val showHolyDaysInEvents: Boolean = false,
    val highlightSunday: Boolean = true,
    val notificationsEnabled: Boolean = false,
    val pushMinutes: Int = 5 * 60,
    val repeatHours: Int = 0,
    val todayTimeZone: TodayTimeZone = TodayTimeZone.LOCAL,
    val fontScale: FontScale = FontScale.PERCENT_100,
)

class AppPreferences(context: Context) {
    private val prefs = context.getSharedPreferences("appearance", Context.MODE_PRIVATE)
    fun read(): AppSettings {
        val legacyHolyDays = prefs.getBoolean("showHolyDays", true)
        return AppSettings(
            theme = ThemeMode.entries.firstOrNull { it.name == prefs.getString("theme", "SYSTEM") } ?: ThemeMode.SYSTEM,
            accent = Accent.entries.firstOrNull { it.name == prefs.getString("accent", "BLUE") } ?: Accent.BLUE,
            khmer = prefs.getBoolean("khmer", true),
            mondayFirst = prefs.getBoolean("mondayFirst", false),
            showCopyButtons = prefs.getBoolean("showCopyButtons", false),
            showLunar = prefs.getBoolean("showLunar", true),
            showHolyDaysInCalendar = prefs.getBoolean("showHolyDaysInCalendar", legacyHolyDays),
            showHolyDaysInEvents = prefs.getBoolean("showHolyDaysInEvents", false),
            highlightSunday = prefs.getBoolean("highlightSunday", true),
            notificationsEnabled = prefs.getBoolean("notificationsEnabled", false),
            pushMinutes = prefs.getInt("pushMinutes", 300).coerceIn(0, 1439),
            repeatHours = prefs.getInt("repeatHours", 0).takeIf { it in listOf(0, 2, 4, 6, 8, 12) } ?: 0,
            todayTimeZone = TodayTimeZone.entries.firstOrNull { it.name == prefs.getString("todayTimeZone", "LOCAL") } ?: TodayTimeZone.LOCAL,
            fontScale = FontScale.entries.firstOrNull { it.name == prefs.getString("fontScale", "PERCENT_100") } ?: FontScale.PERCENT_100,
        )
    }
    fun write(settings: AppSettings) {
        prefs.edit {
            putString("theme", settings.theme.name)
            putString("accent", settings.accent.name)
            putBoolean("khmer", settings.khmer)
            putBoolean("mondayFirst", settings.mondayFirst)
            putBoolean("showCopyButtons", settings.showCopyButtons)
            putBoolean("showLunar", settings.showLunar)
            putBoolean("showHolyDaysInCalendar", settings.showHolyDaysInCalendar)
            putBoolean("showHolyDaysInEvents", settings.showHolyDaysInEvents)
            putBoolean("showHolyDays", settings.showHolyDaysInCalendar && settings.showHolyDaysInEvents)
            putBoolean("highlightSunday", settings.highlightSunday)
            putBoolean("notificationsEnabled", settings.notificationsEnabled)
            putInt("pushMinutes", settings.pushMinutes)
            putInt("repeatHours", settings.repeatHours)
            putString("todayTimeZone", settings.todayTimeZone.name)
            putString("fontScale", settings.fontScale.name)
        }
    }
}
