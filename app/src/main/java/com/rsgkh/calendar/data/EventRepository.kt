// Copyright (c) 2026 RSG-KH | Apache-2.0 License
package com.rsgkh.calendar.data

import com.rsgkh.calendar.domain.KhmerCalendar
import com.rsgkh.calendar.i18n.L
import java.time.LocalDate
import java.time.YearMonth
import java.time.LocalTime
import java.util.concurrent.ConcurrentHashMap

enum class EventKind { HOLIDAY, OBSERVANCE, HOLY_DAY, CUSTOM }
enum class DateBasis { WEBSITE, KHMER_LUNAR, USER, CALCULATED }
data class CalendarEvent(
    val id: String, val date: LocalDate, val titleKm: String, val titleEn: String,
    val kind: EventKind, val basis: DateBasis,
    val time: LocalTime? = null, val notes: String = "",
    val officialSourceUrl: String? = null,
) {
    fun title(khmer: Boolean) = if (khmer) titleKm else titleEn
    val key get() = "$id:$date"
}

/** Dated records take precedence for entire covered years; recurrence fills uncovered years. */
object EventRepository {
    val coveredYears = 2000..2030
    private val cache = ConcurrentHashMap<Int, List<CalendarEvent>>()
    private val snapshot by lazy {
        val stream = checkNotNull(EventRepository::class.java.getResourceAsStream("/calendar-events.tsv")) {
            "Bundled calendar event snapshot is missing"
        }
        val events = stream.bufferedReader(Charsets.UTF_8).useLines { lines ->
            lines.filterNot { it.startsWith('#') || it.isBlank() }.map { line ->
                val fields = line.split('\t')
                check(fields.size == 5) { "Invalid event snapshot row" }
                val (id, dateText, km, en, officialUrl) = fields
                val date = LocalDate.parse(dateText)
                check(date.year in coveredYears && km.isNotBlank() && en.isNotBlank())
                CalendarEvent("website:$id", date, L.eventTitle(id, true, km), L.eventTitle(id, false, en),
                    if (officialUrl.isEmpty()) EventKind.OBSERVANCE else EventKind.HOLIDAY,
                    DateBasis.WEBSITE, officialSourceUrl = officialUrl.ifEmpty { null })
            }.toList()
        }
        check(events.isNotEmpty() && events.map { it.key }.distinct().size == events.size)
        events.groupBy { it.date.year }.also { check(it.keys == coveredYears.toSet()) }
    }
    fun hasBundledYear(year: Int) = year in coveredYears
    fun forMonth(month: YearMonth): List<CalendarEvent> = forYear(month.year).filter { it.date.month == month.month }
    fun forDate(date: LocalDate): List<CalendarEvent> = forYear(date.year).filter { it.date == date }
    fun forYear(year: Int): List<CalendarEvent> {
        require(year in 1800..2200)
        return cache.getOrPut(year) { buildYear(year) }
    }

    private fun buildYear(year: Int): List<CalendarEvent> {
        val events = (snapshot[year] ?: RecurringEvents.forYear(year)).toMutableList()
        var date = LocalDate.of(year, 1, 1)
        while (date.year == year) {
            if (KhmerCalendar.fromGregorian(date).isHolyDay) {
                events.add(CalendarEvent("sil", date, L.text("event.holy_day", true), L.text("event.holy_day", false), EventKind.HOLY_DAY, DateBasis.KHMER_LUNAR))
            }
            date = date.plusDays(1)
        }
        return events.sortedWith(compareBy({ it.date }, { it.kind.ordinal }, { it.id }))
    }
}
