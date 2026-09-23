// SPDX-License-Identifier: Apache-2.0
package com.rsgkh.calendar.widgets

import android.content.Context
import android.util.Log
import com.rsgkh.calendar.R
import com.rsgkh.calendar.data.AppPreferences
import com.rsgkh.calendar.data.AppSettings
import com.rsgkh.calendar.data.CalendarEvent
import com.rsgkh.calendar.data.CustomEventRepository
import com.rsgkh.calendar.data.EventKind
import com.rsgkh.calendar.data.EventRepository
import com.rsgkh.calendar.domain.KhmerDateDetails
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth

internal data class WidgetItem(
    val title: String,
    val time: String?,
    val kind: EventKind,
    val eventId: String?,
    val representedEvents: Int = 1,
)

internal data class WidgetDay(
    val date: LocalDate,
    val items: List<WidgetItem>,
    val eventCount: Int,
    val unavailable: Boolean = false,
    val supported: Boolean = true,
)

internal data class MonthDayInfo(
    val date: LocalDate,
    val dayNumber: Int,
    val lunarLabel: String,
    val isHolyDay: Boolean,
    val isShavingDay: Boolean,
    val hasHoliday: Boolean,
    val hasObservance: Boolean,
    val hasPersonal: Boolean,
    /** Lotus watermark drawable for holy days, mirroring the in-app month grid. */
    val lotusRes: Int? = null,
)

internal data class WidgetSnapshot(
    val settings: AppSettings,
    val today: LocalDate,
    val details: KhmerDateDetails?,
    val yesterday: WidgetDay,
    val current: WidgetDay,
    val tomorrow: WidgetDay,
    val holidayTitle: String?,
    val monthDays: List<MonthDayInfo> = emptyList(),
    val plannerDays: List<WidgetDay> = emptyList(),
)

/** Blocking reads; callers MUST run this on Dispatchers.IO, never in a composable body. */
internal object WidgetDataSource {
    fun load(context: Context, widgetId: Int = 0, now: Instant = Instant.now(), includePlanner: Boolean = false): WidgetSnapshot {
        val settings = AppPreferences(context).read()
        val strings = WidgetStrings(context, settings.khmer)
        val zone = settings.todayTimeZone.zone()
        val today = settings.todayTimeZone.today(now)
        val dates = if (includePlanner) WidgetPolicy.plannerWindow(today) else WidgetPolicy.window(today)
        var personalUnavailable = false
        val personal = if (!settings.widgetShowPersonal) emptyList() else try {
            CustomEventRepository(context).use { it.all() }.flatMap { event ->
                try {
                    event.occurrences(dates.first(), dates.last(), zone)
                } catch (error: Exception) {
                    personalUnavailable = true
                    // Do not put a user's title/notes in the log.
                    Log.w("CalendarWidgets", "An event occurrence could not be expanded: ${error.javaClass.simpleName}")
                    emptyList()
                }
            }
        } catch (error: Exception) {
            personalUnavailable = true
            Log.w("CalendarWidgets", "Personal events could not be read: ${error.javaClass.simpleName}")
            emptyList()
        }
        var todayHoliday: String? = null
        val days = dates.map { date ->
            val supported = date.year in EventRepository.coveredYears
            var builtInsUnavailable = false
            val builtIn = if (!supported) emptyList() else try {
                EventRepository.forDate(date)
            } catch (error: Exception) {
                builtInsUnavailable = true
                Log.w("CalendarWidgets", "Built-in events could not be read: ${error.javaClass.simpleName}")
                emptyList()
            }
            val visibleEvents = (builtIn + personal.filter { it.date == date })
                    .filter { event ->
                        when (event.kind) {
                            EventKind.CUSTOM -> settings.widgetShowPersonal
                            EventKind.HOLIDAY -> settings.widgetShowHolidays
                            EventKind.OBSERVANCE -> settings.showObservances && settings.widgetShowObservances
                            EventKind.HOLY_DAY -> settings.showHolyDaysInEvents
                        }
                    }
            val events = if (includePlanner) WidgetPolicy.plannerSorted(visibleEvents, settings.khmer)
                else WidgetPolicy.sorted(visibleEvents, settings.khmer)
            if (date == today) {
                todayHoliday = events.firstOrNull { it.kind == EventKind.HOLIDAY }?.title(settings.khmer)
            }
            WidgetDay(
                date = date,
                items = displayItems(events, settings.widgetHidePersonalDetails, strings,
                    if (includePlanner) strings::plannerTime else strings::time),
                eventCount = events.size,
                unavailable = personalUnavailable || builtInsUnavailable,
                supported = supported,
            )
        }
        val details = if (today.year !in EventRepository.coveredYears) null else try {
            KhmerDateDetails.fromGregorian(today)
        } catch (error: Exception) {
            Log.w("CalendarWidgets", "Lunar date could not be read: ${error.javaClass.simpleName}")
            null
        }

        val month = YearMonth.from(today)
        val monthStart = month.atDay(1)
        val monthEnd = month.atEndOfMonth()
        val monthPersonal = if (!settings.widgetShowPersonal) emptyList() else try {
            CustomEventRepository(context).use { it.all() }.flatMap { event ->
                try {
                    event.occurrences(monthStart, monthEnd, zone)
                } catch (_: Exception) {
                    emptyList()
                }
            }
        } catch (_: Exception) {
            emptyList()
        }

        val monthDays = (1..month.lengthOfMonth()).map { dayNum ->
            val date = month.atDay(dayNum)
            val d = if (date.year in EventRepository.coveredYears) try { KhmerDateDetails.fromGregorian(date) } catch (_: Exception) { null } else null
            val lunar = d?.lunar
            val isHoly = settings.showHolyDaysInCalendar && (lunar?.isHolyDay == true)
            val isShaving = settings.showHolyDaysInCalendar && (lunar?.isShavingDay == true)
            val builtIn = if (date.year in EventRepository.coveredYears) try { EventRepository.forDate(date) } catch (_: Exception) { emptyList() } else emptyList()
            val hasHoliday = settings.widgetShowHolidays && builtIn.any { it.kind == EventKind.HOLIDAY }
            val hasObservance = settings.showObservances && settings.widgetShowObservances && builtIn.any { it.kind == EventKind.OBSERVANCE }
            val hasPersonal = settings.widgetShowPersonal && monthPersonal.any { it.date == date }

            MonthDayInfo(
                date = date,
                dayNumber = dayNum,
                lunarLabel = lunar?.shortLabel(settings.khmer) ?: "",
                isHolyDay = isHoly,
                isShavingDay = isShaving,
                hasHoliday = hasHoliday,
                hasObservance = hasObservance,
                hasPersonal = hasPersonal,
                lotusRes = if (isHoly) {
                    // Day 8 and its shaving day use the bud; phase-end holy days use the blossom.
                    if ((lunar?.day ?: 0) <= 8) R.drawable.holy_day_lotus else R.drawable.holy_day_lotus_blossom
                } else null,
            )
        }

        val center = if (includePlanner) 14 else 1
        return WidgetSnapshot(settings, today, details, days[center - 1], days[center], days[center + 1],
            todayHoliday, monthDays, if (includePlanner) days else emptyList())
    }

    internal fun displayItems(
        events: List<CalendarEvent>, private: Boolean, strings: WidgetStrings,
        timeFormatter: (LocalTime) -> String = strings::time,
    ): List<WidgetItem> {
        val visible = events.filterNot { private && it.kind == EventKind.CUSTOM }.map {
            WidgetItem(it.title(strings.khmer), it.time?.let(timeFormatter), it.kind, it.id)
        }
        val privateCount = if (private) events.count { it.kind == EventKind.CUSTOM } else 0
        return if (privateCount == 0) visible else visible + WidgetItem(
            strings(if (privateCount == 1) R.string.widget_personal_count_one else R.string.widget_personal_count, strings.number(privateCount)),
            time = null, kind = EventKind.CUSTOM, eventId = null, representedEvents = privateCount,
        )
    }
}
