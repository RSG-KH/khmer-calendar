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

/** Captured records take precedence; engine dates are bundled for 1980–2050. */
object EventRepository {
    val coveredYears = 1980..2050
    val capturedYears = 2000..2030
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
                check(date.year in capturedYears && km.isNotBlank() && en.isNotBlank())
                CalendarEvent("website:$id", date, L.eventTitle(id, true, km), L.eventTitle(id, false, en),
                    if (officialUrl.isEmpty()) EventKind.OBSERVANCE else EventKind.HOLIDAY,
                    DateBasis.WEBSITE, officialSourceUrl = officialUrl.ifEmpty { null })
            }.toList()
        }
        check(events.isNotEmpty() && events.map { it.key }.distinct().size == events.size)
        events.groupBy { it.date.year }.also { check(it.keys == capturedYears.toSet()) }
    }
    // Only IDs and dates are stored: translations and anniversary titles stay live.
    private val bundledDates by lazy {
        val rules = RecurringEvents.rules.associateBy { "calculated:${it.id}" }
        val stream = checkNotNull(EventRepository::class.java.getResourceAsStream("/engine-event-dates.tsv")) {
            "Bundled engine event dates are missing"
        }
        val rows = stream.bufferedReader(Charsets.UTF_8).useLines { lines ->
            lines.filterNot { it.startsWith('#') || it.isBlank() }.map { line ->
                val fields = line.split('\t')
                check(fields.size == 2) { "Invalid engine event date row" }
                val (id, dateText) = fields
                val date = LocalDate.parse(dateText)
                check(date.year in coveredYears)
                if (id != "sil") {
                    val rule = checkNotNull(rules[id]) { "Unknown bundled recurrence: $id" }
                    check(date.year !in capturedYears && date.year in rule.fromYear..rule.throughYear)
                }
                id to date
            }.toList()
        }
        check(rows.distinct().size == rows.size) { "Duplicate engine event date" }
        rows.groupBy { it.second.year }.mapValues { (_, dates) ->
            dates.groupBy({ it.first }, { it.second }).also { check(it.getValue("sil").isNotEmpty()) }
        }.also { check(it.keys == coveredYears.toSet()) }
    }
    fun hasBundledYear(year: Int) = year in coveredYears
    fun forMonth(month: YearMonth): List<CalendarEvent> = forYear(month.year).filter { it.date.month == month.month }
    fun forDate(date: LocalDate): List<CalendarEvent> = forYear(date.year).filter { it.date == date }
    fun forYear(year: Int): List<CalendarEvent> {
        require(year in 1800..2200)
        return cache.getOrPut(year) { buildYear(year) }
    }

    private fun buildYear(year: Int): List<CalendarEvent> {
        val dates = if (hasBundledYear(year)) bundledDates.getValue(year) else null
        val events = when {
            year in capturedYears -> snapshot.getValue(year)
            dates != null -> RecurringEvents.fromDates(year, RecurringEvents.rules
                .filter { year in it.fromYear..it.throughYear }
                .associateWith { dates.getValue("calculated:${it.id}") })
            else -> RecurringEvents.forYear(year)
        }.toMutableList()
        val holyDays = dates?.getValue("sil") ?: generateSequence(LocalDate.of(year, 1, 1)) { it.plusDays(1) }
            .takeWhile { it.year == year }.filter { KhmerCalendar.fromGregorian(it).isHolyDay }.toList()
        holyDays.forEach { date ->
            events.add(CalendarEvent("sil", date, L.text("event.holy_day", true), L.text("event.holy_day", false), EventKind.HOLY_DAY, DateBasis.KHMER_LUNAR))
        }
        return events.sortedWith(compareBy({ it.date }, { it.kind.ordinal }, { it.id }))
    }
}
