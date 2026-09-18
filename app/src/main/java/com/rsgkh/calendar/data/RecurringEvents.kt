// Copyright (c) 2026 RSG-KH | Apache-2.0 License
package com.rsgkh.calendar.data

import com.rsgkh.calendar.domain.KhmerCalendar
import com.rsgkh.calendar.domain.khmerNumber
import com.rsgkh.calendar.domain.toLocalDate
import com.rsgkh.calendar.engine.RecurrenceRule
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

data class CatalogNames(
    val en: String,
    val km: String,
)

data class CatalogSource(
    val id: String,
    val kind: String,
    val title: String,
    val publisher: String,
    val url: String? = null,
    val reference: String? = null,
    val publishedOn: String? = null,
    val notes: String? = null,
)

data class CatalogEvent(
    val id: String,
    val kind: String,
    val names: CatalogNames,
    val sourceIds: List<String>,
    val description: CatalogNames? = null,
    val originalDate: String? = null,
    val dates: List<String>? = null,
    val rule: RecurrenceRule? = null,
    val anniversaryBase: Int? = null,
)

data class CatalogHoliday(
    val id: String,
    val names: CatalogNames,
    val dates: List<String>,
    val status: String,
    val sourceIds: List<String>,
    val eventId: String? = null,
    val note: String? = null,
)

data class CatalogHolidayCalendar(
    val year: Int,
    val coverage: String,
    val sourceIds: List<String>,
    val holidays: List<CatalogHoliday>,
)

data class CatalogOverride(
    val eventId: String,
    val year: Int,
    val dates: List<String>,
    val sourceId: String,
    val reason: String,
)

data class CalendarCatalog(
    val schemaVersion: Int,
    val dataVersion: String,
    val sources: List<CatalogSource>,
    val events: List<CatalogEvent>,
    val holidayCalendars: List<CatalogHolidayCalendar>,
    val overrides: List<CatalogOverride>,
)

fun eventNames(event: CatalogEvent, year: Int): CatalogNames {
    val base = event.anniversaryBase ?: return event.names
    val anniversary = year - base
    return CatalogNames(
        en = event.names.en.replace("{anniversary}", anniversary.toString()),
        km = event.names.km.replace("{anniversary}", khmerNumber(anniversary)),
    )
}

/** Evaluates canonical Schema v2 recurrence rules dynamically for 1800–2200. */
internal object RecurringEvents {
    val catalog: CalendarCatalog by lazy { parseCatalog() }
    val recurrenceEvents: List<CatalogEvent> by lazy { catalog.events.filter { it.rule != null } }
    val rules: List<RecurrenceRule> by lazy { recurrenceEvents.mapNotNull { it.rule } }

    private fun parseNames(obj: JSONObject): CatalogNames =
        CatalogNames(en = obj.getString("en"), km = obj.getString("km"))

    private fun parseStringList(arr: JSONArray?): List<String> {
        if (arr == null) return emptyList()
        return (0 until arr.length()).map { arr.getString(it) }
    }

    private fun parseRule(obj: JSONObject): RecurrenceRule {
        val id = obj.getString("id")
        val type = obj.getString("type")
        val defaultMonthDay = if (type.startsWith("new_year_") || type == "chinese_festival") 1 else 0
        val month = obj.optInt("month", defaultMonthDay)
        val day = obj.optInt("day", defaultMonthDay)
        val waxing = obj.optBoolean("waxing", true)
        val offset = obj.optInt("offset", 0)
        val duration = obj.optInt("duration", 1)
        val fromYear = obj.optInt("fromYear", 1800)
        val throughYear = obj.optInt("throughYear", 2200)
        val monthPolicy = if (obj.has("monthPolicy") && !obj.isNull("monthPolicy")) obj.getString("monthPolicy") else "exact"
        val occurrence = obj.optInt("occurrence", 1)
        return RecurrenceRule(
            id = id,
            type = type,
            month = month,
            day = day,
            waxing = waxing,
            offset = offset,
            duration = duration,
            fromYear = fromYear,
            throughYear = throughYear,
            monthPolicy = monthPolicy,
            occurrence = occurrence,
        )
    }

    private fun parseHoliday(obj: JSONObject): CatalogHoliday = CatalogHoliday(
        id = obj.getString("id"),
        names = parseNames(obj.getJSONObject("names")),
        dates = parseStringList(obj.getJSONArray("dates")),
        status = obj.optString("status", "active"),
        sourceIds = parseStringList(obj.optJSONArray("sourceIds")),
        eventId = if (obj.has("eventId") && !obj.isNull("eventId")) obj.getString("eventId") else null,
        note = if (obj.has("note") && !obj.isNull("note")) obj.getString("note") else null,
    )

    private fun parseCatalog(): CalendarCatalog {
        val stream = checkNotNull(RecurringEvents::class.java.getResourceAsStream("/khmer-calendar-data.json")) {
            "Canonical calendar data JSON bundle is missing"
        }
        val text = stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        val root = JSONObject(text)

        val sourcesArr = root.optJSONArray("sources") ?: JSONArray()
        val sources = (0 until sourcesArr.length()).map { i ->
            val obj = sourcesArr.getJSONObject(i)
            CatalogSource(
                id = obj.getString("id"),
                kind = obj.getString("kind"),
                title = obj.getString("title"),
                publisher = obj.getString("publisher"),
                url = if (obj.has("url") && !obj.isNull("url")) obj.getString("url") else null,
                reference = if (obj.has("reference") && !obj.isNull("reference")) obj.getString("reference") else null,
                publishedOn = if (obj.has("publishedOn") && !obj.isNull("publishedOn")) obj.getString("publishedOn") else null,
                notes = if (obj.has("notes") && !obj.isNull("notes")) obj.getString("notes") else null,
            )
        }

        val eventsArr = root.getJSONArray("events")
        val events = (0 until eventsArr.length()).map { i ->
            val obj = eventsArr.getJSONObject(i)
            val descObj = obj.optJSONObject("description")
            val ruleObj = obj.optJSONObject("rule")
            CatalogEvent(
                id = obj.getString("id"),
                kind = obj.getString("kind"),
                names = parseNames(obj.getJSONObject("names")),
                sourceIds = parseStringList(obj.optJSONArray("sourceIds")),
                description = if (descObj != null) parseNames(descObj) else null,
                originalDate = if (obj.has("originalDate") && !obj.isNull("originalDate")) obj.getString("originalDate") else null,
                dates = if (obj.has("dates") && !obj.isNull("dates")) parseStringList(obj.getJSONArray("dates")) else null,
                rule = if (ruleObj != null) parseRule(ruleObj) else null,
                anniversaryBase = if (obj.has("anniversaryBase") && !obj.isNull("anniversaryBase")) obj.getInt("anniversaryBase") else null,
            )
        }

        val holidayCalendarsArr = root.optJSONArray("holidayCalendars") ?: JSONArray()
        val holidayCalendars = (0 until holidayCalendarsArr.length()).map { i ->
            val obj = holidayCalendarsArr.getJSONObject(i)
            val holidaysArr = obj.getJSONArray("holidays")
            val holidays = (0 until holidaysArr.length()).map { h -> parseHoliday(holidaysArr.getJSONObject(h)) }
            CatalogHolidayCalendar(
                year = obj.getInt("year"),
                coverage = obj.optString("coverage", "complete"),
                sourceIds = parseStringList(obj.optJSONArray("sourceIds")),
                holidays = holidays,
            )
        }

        val overridesArr = root.optJSONArray("overrides") ?: JSONArray()
        val overrides = (0 until overridesArr.length()).map { i ->
            val obj = overridesArr.getJSONObject(i)
            CatalogOverride(
                eventId = obj.getString("eventId"),
                year = obj.getInt("year"),
                dates = parseStringList(obj.getJSONArray("dates")),
                sourceId = obj.getString("sourceId"),
                reason = obj.optString("reason", ""),
            )
        }

        return CalendarCatalog(
            schemaVersion = root.getInt("schemaVersion"),
            dataVersion = root.getString("dataVersion"),
            sources = sources,
            events = events,
            holidayCalendars = holidayCalendars,
            overrides = overrides,
        )
    }

    internal fun dates(year: Int): Map<CatalogEvent, List<LocalDate>> {
        require(year in KhmerCalendar.engine.minYear..KhmerCalendar.engine.maxYear)
        return recurrenceEvents
            .filter { event ->
                val rule = event.rule!!
                year >= rule.fromYear && year <= rule.throughYear
            }
            .associateWith { event ->
                val rule = event.rule!!
                KhmerCalendar.engine.evaluateRule(year, rule).map { it.date.toLocalDate() }
                    .also { dates -> check(dates.isNotEmpty() && dates.all { it.year == year }) }
            }
    }

    fun forYear(year: Int): List<CalendarEvent> = fromDates(year, dates(year))

    internal fun fromDates(year: Int, dates: Map<CatalogEvent, List<LocalDate>>): List<CalendarEvent> =
        dates.flatMap { (event, eventDates) ->
            val names = eventNames(event, year)
            eventDates.map { date ->
                CalendarEvent(
                    id = event.id,
                    date = date,
                    titleKm = names.km,
                    titleEn = names.en,
                    kind = EventKind.OBSERVANCE,
                    basis = DateBasis.CALCULATED,
                    sourceIds = event.sourceIds,
                )
            }
        }
}
