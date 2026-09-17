// Copyright (c) 2026 RSG-KH | Apache-2.0 License
package com.rsgkh.calendar.data

import com.rsgkh.calendar.domain.EventRepeat
import com.rsgkh.calendar.domain.KhmerCalendar
import com.rsgkh.calendar.domain.khmerNumber
import com.rsgkh.calendar.engine.EventDateOverride
import com.rsgkh.calendar.engine.GregorianDate
import com.rsgkh.calendar.i18n.L
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.util.concurrent.ConcurrentHashMap

enum class EventKind { HOLIDAY, OBSERVANCE, HOLY_DAY, CUSTOM }
enum class DateBasis {
    OFFICIAL,
    CALCULATED,
    CORRECTED,
    RECORDED,
    KHMER_LUNAR,
    USER,
    WEBSITE;
}

data class CalendarEvent(
    val id: String,
    val date: LocalDate,
    val titleKm: String,
    val titleEn: String,
    val kind: EventKind,
    val basis: DateBasis,
    val time: LocalTime? = null,
    val notes: String = "",
    val officialSourceUrl: String? = null,
    val citation: String? = null,
    val citationEn: String? = null,
    val citationKm: String? = null,
    val sourceIds: List<String> = emptyList(),
    val customSeriesId: String? = null,
    val repeat: EventRepeat? = null,
) {
    fun title(khmer: Boolean) = if (khmer) titleKm else titleEn
    val key get() = "$id:$date"
}

data class SourceInfo(
    val url: String?,
    val citationEn: String?,
    val citationKm: String?,
)

object EventRepository {
    val coveredYears = 1800..2200
    private val cache = ConcurrentHashMap<Int, List<CalendarEvent>>()

    private val sourcesMap: Map<String, CatalogSource> by lazy {
        RecurringEvents.catalog.sources.associateBy { it.id }
    }
    private val eventsMap: Map<String, CatalogEvent> by lazy {
        RecurringEvents.catalog.events.associateBy { it.id }
    }
    private val staticEvents: List<CatalogEvent> by lazy {
        RecurringEvents.catalog.events.filter { !it.dates.isNullOrEmpty() }
    }
    private val holidayCalendars: Map<Int, CatalogHolidayCalendar> by lazy {
        RecurringEvents.catalog.holidayCalendars.associateBy { it.year }
    }
    private val overridesByYear: Map<Int, List<CatalogOverride>> by lazy {
        RecurringEvents.catalog.overrides.groupBy { it.year }
    }

    private fun getSourceInfo(sourceIds: List<String>): SourceInfo {
        val sources = sourceIds.mapNotNull { sourcesMap[it] }
        val govSources = sources.filter { it.kind == "government" }
        if (govSources.isEmpty()) {
            return SourceInfo(url = null, citationEn = null, citationKm = null)
        }
        val withUrl = govSources.firstOrNull { !it.url.isNullOrBlank() }
        val withRef = govSources.firstOrNull { !it.reference.isNullOrBlank() }
        val primary = withRef ?: govSources.first()

        val url = withUrl?.url ?: primary.url
        val citationEn = primary.reference ?: primary.title
        val citationKm = primary.notes ?: primary.reference ?: primary.title
        return SourceInfo(url = url, citationEn = citationEn, citationKm = citationKm)
    }

    private fun formatHolidayNames(h: CatalogHoliday, year: Int): CatalogNames {
        var km = h.names.km
        var en = h.names.en
        if (km.contains("{anniversary}") || en.contains("{anniversary}")) {
            val ev = eventsMap[h.eventId ?: h.id]
            val base = ev?.anniversaryBase
            if (base != null) {
                val anniversary = year - base
                km = km.replace("{anniversary}", khmerNumber(anniversary))
                en = en.replace("{anniversary}", anniversary.toString())
            }
        }
        return CatalogNames(en = en, km = km)
    }

    fun hasBundledYear(year: Int): Boolean = year in coveredYears

    fun clearCache() {
        cache.clear()
    }

    fun forMonth(month: YearMonth): List<CalendarEvent> =
        forYear(month.year).filter { it.date.month == month.month }

    fun forDate(date: LocalDate): List<CalendarEvent> =
        forYear(date.year).filter { it.date == date }

    fun forYear(year: Int): List<CalendarEvent> {
        require(year in coveredYears) { "Supported years: 1800–2200." }
        return cache.getOrPut(year) { buildYear(year) }
    }

    private fun buildYear(year: Int): List<CalendarEvent> {
        val events = mutableListOf<CalendarEvent>()

        // 1. Static date-backed events (Chinese festivals & UNESCO milestones)
        for (event in staticEvents) {
            val dates = event.dates ?: continue
            for (dateStr in dates) {
                if (!dateStr.startsWith("$year-")) continue
                val date = LocalDate.parse(dateStr)
                events.add(
                    CalendarEvent(
                        id = event.id,
                        date = date,
                        titleKm = event.names.km,
                        titleEn = event.names.en,
                        kind = EventKind.OBSERVANCE,
                        basis = DateBasis.RECORDED,
                        sourceIds = event.sourceIds,
                    )
                )
            }
        }

        // 2. Recurring events evaluated via engine, with historical date overrides
        val yearOverrides = overridesByYear[year].orEmpty()
        for (event in RecurringEvents.recurrenceEvents) {
            val rule = event.rule ?: continue
            if (year < rule.fromYear || year > rule.throughYear) continue

            val override = yearOverrides.firstOrNull { it.eventId == event.id }
            val replacement = override?.let { o ->
                EventDateOverride(
                    o.eventId,
                    year,
                    o.dates.map { iso ->
                        val parts = iso.split('-').map { it.toInt() }
                        GregorianDate(parts[0], parts[1], parts[2])
                    }.toTypedArray(),
                    o.sourceId,
                    o.reason,
                )
            }

            val occurrences = KhmerCalendar.engine.evaluateRule(year, rule, replacement)
            val names = eventNames(event, year)

            for (occ in occurrences) {
                if (occ.date.year != year) continue
                val date = LocalDate.of(occ.date.year, occ.date.month, occ.date.day)
                if (event.kind == "historical" && event.originalDate != null) {
                    if (date < LocalDate.parse(event.originalDate)) continue
                }

                val isCorrected = occ.basis == "source_override"
                val sourceIds = if (isCorrected && !occ.sourceId.isNullOrEmpty()) listOf(occ.sourceId!!) else event.sourceIds

                events.add(
                    CalendarEvent(
                        id = event.id,
                        date = date,
                        titleKm = names.km,
                        titleEn = names.en,
                        kind = EventKind.OBSERVANCE,
                        basis = if (isCorrected) DateBasis.CORRECTED else DateBasis.CALCULATED,
                        sourceIds = sourceIds,
                    )
                )
            }
        }

        // 3. Official public holiday calendars (2020–2027)
        val holidayCalendar = holidayCalendars[year]
        if (holidayCalendar != null) {
            for (h in holidayCalendar.holidays) {
                if (h.status == "cancelled") continue
                val (url, citationEn, citationKm) = getSourceInfo(h.sourceIds)
                val candidateIds = setOfNotNull(h.id, h.eventId)
                val holidayNames = formatHolidayNames(h, year)

                for (dateStr in h.dates) {
                    val date = LocalDate.parse(dateStr)
                    val existingIndex = events.indexOfFirst { it.date == date && it.id in candidateIds }
                    if (existingIndex >= 0) {
                        val existing = events[existingIndex]
                        events[existingIndex] = existing.copy(
                            kind = EventKind.HOLIDAY,
                            basis = DateBasis.OFFICIAL,
                            titleKm = holidayNames.km.ifBlank { existing.titleKm },
                            titleEn = holidayNames.en.ifBlank { existing.titleEn },
                            sourceIds = (existing.sourceIds + h.sourceIds).distinct(),
                            officialSourceUrl = url ?: existing.officialSourceUrl,
                            citation = citationEn ?: existing.citation,
                            citationEn = citationEn ?: existing.citationEn,
                            citationKm = citationKm ?: existing.citationKm,
                        )
                    } else {
                        events.add(
                            CalendarEvent(
                                id = h.id,
                                date = date,
                                titleKm = holidayNames.km,
                                titleEn = holidayNames.en,
                                kind = EventKind.HOLIDAY,
                                basis = DateBasis.OFFICIAL,
                                officialSourceUrl = url,
                                citation = citationEn,
                                citationEn = citationEn,
                                citationKm = citationKm,
                                sourceIds = h.sourceIds,
                            )
                        )
                    }
                }
            }
        }

        // 4. Buddhist Holy Days (Thngai Sil)
        val holyDays = generateSequence(LocalDate.of(year, 1, 1)) { it.plusDays(1) }
            .takeWhile { it.year == year }
            .filter { KhmerCalendar.fromGregorian(it).isHolyDay }
        for (date in holyDays) {
            events.add(
                CalendarEvent(
                    id = "sil",
                    date = date,
                    titleKm = L.text("event.holy_day", true),
                    titleEn = L.text("event.holy_day", false),
                    kind = EventKind.HOLY_DAY,
                    basis = DateBasis.KHMER_LUNAR,
                )
            )
        }

        events.sortWith(compareBy({ it.date }, { it.kind.ordinal }, { it.id }))
        return events
    }
}
