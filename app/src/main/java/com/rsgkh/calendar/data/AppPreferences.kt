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
    PERCENT_150(1.50f, "150%"),
    PERCENT_175(1.75f, "175%"),
    PERCENT_200(2.00f, "200%")
}
enum class TodayTimeZone {
    LOCAL, CAMBODIA;

    fun zone(localZone: ZoneId = ZoneId.systemDefault()): ZoneId = if (this == LOCAL) localZone else CAMBODIA_ZONE

    fun today(now: Instant = Instant.now(), localZone: ZoneId = ZoneId.systemDefault()): LocalDate =
        now.atZone(zone(localZone)).toLocalDate()

    fun hour(now: Instant = Instant.now(), localZone: ZoneId = ZoneId.systemDefault()): Int =
        now.atZone(zone(localZone)).hour

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
    val showObservances: Boolean = true,
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
    val widgetFontScale: FontScale = FontScale.PERCENT_100,
    val enableAstrologyAndZodiac: Boolean = true,
    val astrologyMinutes: Int = 12 * 60,
    val risingPlace: Birthplace = Birthplace.DEFAULT,
    val showWesternZodiac: Boolean = true,
    val useEmojiForWesternZodiac: Boolean = false,
    val showGanzhi: Boolean = true,
    val useEmojiForGanzhiAnimals: Boolean = false,
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
        pushObservances != other.pushObservances || showObservances != other.showObservances || pushHolyDays != other.pushHolyDays ||
        showHolyDaysInEvents != other.showHolyDaysInEvents || pushMinutes != other.pushMinutes ||
        repeatHours != other.repeatHours || todayTimeZone != other.todayTimeZone

class AppPreferences(context: Context) {
    private val prefs = context.getSharedPreferences("appearance", Context.MODE_PRIVATE)
    fun read(): AppSettings {
        val values = prefs.all
        fun boolean(key: String, fallback: Boolean) = values[key] as? Boolean ?: fallback
        fun integer(key: String, fallback: Int) = values[key] as? Int ?: fallback
        fun string(key: String, fallback: String?) = values[key] as? String ?: fallback
        val legacyHolyDays = boolean("showHolyDays", true)
        val showHolyDaysInEvents = boolean("showHolyDaysInEvents", false)
        val showWesternZodiac = if (prefs.contains("showWesternZodiac")) {
            boolean("showWesternZodiac", true)
        } else {
            !boolean("hideWesternZodiac", false)
        }
        return AppSettings(
            theme = ThemeMode.entries.firstOrNull { it.name == string("theme", "SYSTEM") } ?: ThemeMode.SYSTEM,
            accent = Accent.entries.firstOrNull { it.name == string("accent", "BLUE") } ?: Accent.BLUE,
            backgroundAccent = boolean("backgroundAccent", true),
            khmer = boolean("khmer", true),
            mondayFirst = boolean("mondayFirst", false),
            showLongerWeekdayNames = boolean("showLongerWeekdayNames", false),
            showObservances = boolean("showObservances", true),
            showCopyButtons = boolean("showCopyButtons", false),
            highlightWeekdayNames = boolean("highlightWeekdayNames", true),
            showLunar = boolean("showLunar", true),
            showHolyDaysInCalendar = boolean("showHolyDaysInCalendar", legacyHolyDays),
            showHolyDaysInEvents = showHolyDaysInEvents,
            highlightSunday = boolean("highlightSunday", true),
            notificationsEnabled = boolean("notificationsEnabled", false),
            pushCustomEvents = boolean("pushCustomEvents", true),
            pushHolidays = boolean("pushHolidays", true),
            pushObservances = boolean("pushObservances", true),
            pushHolyDays = boolean("pushHolyDays", showHolyDaysInEvents),
            pushMinutes = integer("pushMinutes", 300).coerceIn(0, 1439),
            repeatHours = integer("repeatHours", 0).takeIf { it in listOf(0, 2, 4, 6, 8, 12) } ?: 0,
            todayTimeZone = TodayTimeZone.entries.firstOrNull { it.name == string("todayTimeZone", "LOCAL") } ?: TodayTimeZone.LOCAL,
            fontScale = FontScale.entries.firstOrNull { it.name == string("fontScale", "PERCENT_100") } ?: FontScale.PERCENT_100,
            widgetFontScale = FontScale.entries.firstOrNull { it.name == string("widgetFontScale", "PERCENT_100") } ?: FontScale.PERCENT_100,
            showWesternZodiac = showWesternZodiac,
            enableAstrologyAndZodiac = boolean("enableAstrologyAndZodiac", true),
            astrologyMinutes = integer("astrologyMinutes", 720).takeIf { it in 0..1439 } ?: 720,
            risingPlace = Birthplace.fromJson(string("risingPlace", null)) ?: Birthplace.DEFAULT,
            useEmojiForWesternZodiac = boolean("useEmojiForWesternZodiac", false),
            showGanzhi = boolean("showGanzhi", true),
            useEmojiForGanzhiAnimals = boolean("useEmojiForGanzhiAnimals", false),
            widgetsEnabled = boolean("widgetsEnabled", false),
            widgetShowPersonal = boolean("widgetShowPersonal", true),
            widgetShowHolidays = boolean("widgetShowHolidays", true),
            widgetShowObservances = boolean("widgetShowObservances", true),
            widgetHidePersonalDetails = boolean("widgetHidePersonalDetails", false),
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
            putBoolean("showObservances", settings.showObservances)
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
            putString("widgetFontScale", settings.widgetFontScale.name)
            putBoolean("showWesternZodiac", settings.showWesternZodiac)
            putBoolean("enableAstrologyAndZodiac", settings.enableAstrologyAndZodiac)
            putInt("astrologyMinutes", settings.astrologyMinutes)
            putString("risingPlace", settings.risingPlace.toJson())
            putBoolean("useEmojiForWesternZodiac", settings.useEmojiForWesternZodiac)
            putBoolean("showGanzhi", settings.showGanzhi)
            putBoolean("useEmojiForGanzhiAnimals", settings.useEmojiForGanzhiAnimals)
            putBoolean("widgetsEnabled", settings.widgetsEnabled)
            putBoolean("widgetShowPersonal", settings.widgetShowPersonal)
            putBoolean("widgetShowHolidays", settings.widgetShowHolidays)
            putBoolean("widgetShowObservances", settings.widgetShowObservances)
            putBoolean("widgetHidePersonalDetails", settings.widgetHidePersonalDetails)
            remove("hideWesternZodiac")
        }
    }
}
