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

data class KnowledgeEntry(
    val id: String,
    val category: String,
    val nameKm: String,
    val nameEn: String,
    val summaryKm: String,
    val summaryEn: String,
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

data class CatalogNewYearArrival(
    val year: Int,
    val localDate: String,
    val localTime: String,
    val minuteOfDay: Int,
    val second: Int? = null,
    val status: String,
    val grade: String,
    val sourceIds: List<String>,
    val precision: String? = null,
    val interpretedZone: String? = null,
    val interpretedOffset: String? = null,
    val zoneStated: Boolean = false,
    val zoneBasis: String? = null,
    val role: String? = null,
    val retrieved: String? = null,
)

data class CalendarCatalog(
    val schemaVersion: Int,
    val dataVersion: String,
    val sources: List<CatalogSource>,
    val events: List<CatalogEvent>,
    val holidayCalendars: List<CatalogHolidayCalendar>,
    val overrides: List<CatalogOverride>,
    val newYearArrivals: List<CatalogNewYearArrival> = emptyList(),
)

data class NewYearArrivalDisplay(
    val hour24: Int,
    val minute: Int,
    val second: Int?,
    val isOfficial: Boolean,
    val titleKm: String,
    val titleEn: String,
    val sourceIds: List<String>,
)

fun getKhmerPeriod(hour24: Int, minute: Int): String = when {
    hour24 < 3 -> "រំលងអធ្រាត្រ"
    hour24 < 12 -> "ព្រឹក"
    hour24 == 12 && minute == 0 -> "ថ្ងៃត្រង់"
    hour24 < 15 -> "រសៀល"
    hour24 < 20 -> "ល្ងាច"
    else -> "យប់"
}

fun String.toKhmerNumerals(): String =
    map { if (it in '0'..'9') '០' + (it - '0') else it }.joinToString("")

fun formatNewYearArrivalTime(
    hour24: Int,
    minute: Int,
    second: Int? = null,
    isOfficial: Boolean,
    isKhmer: Boolean
): String {
    val hour12 = if (hour24 % 12 == 0) 12 else hour24 % 12
    return if (isKhmer) {
        val period = getKhmerPeriod(hour24, minute)
        val status = if (isOfficial) "(ម៉ោងផ្លូវការ)" else "(ម៉ោងប៉ាន់ស្មាន)"
        val timeDigits = if (second != null) {
            "%02d:%02d:%02d".format(hour12, minute, second)
        } else {
            "%02d:%02d".format(hour12, minute)
        }.toKhmerNumerals()
        "ម៉ោង $timeDigits $period $status"
    } else {
        val amPm = if (hour24 < 12) "AM" else "PM"
        val status = if (isOfficial) "(Official time)" else "(Estimated time)"
        val timeStr = if (second != null) {
            "%d:%02d:%02d %s".format(hour12, minute, second, amPm)
        } else {
            "%d:%02d %s".format(hour12, minute, amPm)
        }
        "$timeStr $status"
    }
}

fun resolveNewYearArrival(year: Int): NewYearArrivalDisplay {
    val record = RecurringEvents.newYearArrivalsByYear[year]
    return if (record != null) {
        val timeParts = record.localTime.split(':').map { it.toInt() }
        val hour24 = timeParts[0]
        val minute = timeParts[1]
        val second = record.second ?: timeParts.getOrNull(2)
        NewYearArrivalDisplay(
            hour24 = hour24,
            minute = minute,
            second = second,
            isOfficial = true,
            titleKm = formatNewYearArrivalTime(hour24, minute, second, isOfficial = true, isKhmer = true),
            titleEn = formatNewYearArrivalTime(hour24, minute, second, isOfficial = true, isKhmer = false),
            sourceIds = record.sourceIds,
        )
    } else {
        val arrival = KhmerCalendar.engine.newYear(year).arrivalEstimate
        val hour24 = arrival.hour
        val minute = arrival.minute
        NewYearArrivalDisplay(
            hour24 = hour24,
            minute = minute,
            second = null,
            isOfficial = false,
            titleKm = formatNewYearArrivalTime(hour24, minute, null, isOfficial = false, isKhmer = true),
            titleEn = formatNewYearArrivalTime(hour24, minute, null, isOfficial = false, isKhmer = false),
            sourceIds = emptyList(),
        )
    }
}

internal fun ordinalSuffix(n: Int): String {
    val hundredRem = n % 100
    val tenRem = n % 10
    if (hundredRem in 11..13) return "th"
    return when (tenRem) {
        1 -> "st"
        2 -> "nd"
        3 -> "rd"
        else -> "th"
    }
}

fun eventNames(event: CatalogEvent, year: Int): CatalogNames {
    val base = event.anniversaryBase
    var names = if (base != null) {
        val anniversary = year - base
        CatalogNames(
            en = event.names.en.replace("{anniversary}", "$anniversary${ordinalSuffix(anniversary)}"),
            km = event.names.km.replace("{anniversary}", khmerNumber(anniversary)),
        )
    } else {
        event.names
    }
    if (event.id == "khmer_new_year_1") {
        val arrival = resolveNewYearArrival(year)
        names = CatalogNames(
            en = "${names.en} ${arrival.titleEn}",
            km = "${names.km} ${arrival.titleKm}",
        )
    }
    return names
}

/** Evaluates canonical Schema v2 recurrence rules dynamically for 1800–2200. */
internal object RecurringEvents {
    val catalog: CalendarCatalog by lazy { parseCatalog() }
    val newYearArrivalsByYear: Map<Int, CatalogNewYearArrival> by lazy {
        catalog.newYearArrivals.associateBy { it.year }
    }
    val recurrenceEvents: List<CatalogEvent> by lazy { catalog.events.filter { it.rule != null } }
    val rules: List<RecurrenceRule> by lazy { recurrenceEvents.mapNotNull { it.rule } }
    val knowledgeById: Map<String, KnowledgeEntry> by lazy { parseKnowledge() }

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

    private fun parseKnowledge(): Map<String, KnowledgeEntry> {
        val stream = checkNotNull(RecurringEvents::class.java.getResourceAsStream("/event-knowledge.json")) {
            "event-knowledge.json is missing from the bundled resources"
        }
        stream.bufferedReader().use { reader ->
            val entries = JSONObject(reader.readText()).getJSONArray("entries")
            return (0 until entries.length()).map { index ->
                val obj = entries.getJSONObject(index)
                KnowledgeEntry(
                    id = obj.getString("id"),
                    category = obj.getString("category"),
                    nameKm = obj.getString("name_km"),
                    nameEn = obj.getString("name_en"),
                    summaryKm = obj.getString("summary_km"),
                    summaryEn = obj.getString("summary_en"),
                )
            }.associateBy { it.id }
        }
    }

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

        val arrivalsArr = root.optJSONArray("newYearArrivals") ?: JSONArray()
        val newYearArrivals = (0 until arrivalsArr.length()).map { i ->
            val obj = arrivalsArr.getJSONObject(i)
            CatalogNewYearArrival(
                year = obj.getInt("year"),
                localDate = obj.getString("localDate"),
                localTime = obj.getString("localTime"),
                minuteOfDay = obj.getInt("minuteOfDay"),
                second = if (obj.has("second") && !obj.isNull("second")) obj.getInt("second") else null,
                status = obj.optString("status", "evidenced"),
                grade = obj.optString("grade", "A"),
                sourceIds = parseStringList(obj.optJSONArray("sourceIds")),
                precision = if (obj.has("precision") && !obj.isNull("precision")) obj.getString("precision") else null,
                interpretedZone = if (obj.has("interpretedZone") && !obj.isNull("interpretedZone")) obj.getString("interpretedZone") else null,
                interpretedOffset = if (obj.has("interpretedOffset") && !obj.isNull("interpretedOffset")) obj.getString("interpretedOffset") else null,
                zoneStated = obj.optBoolean("zoneStated", false),
                zoneBasis = if (obj.has("zoneBasis") && !obj.isNull("zoneBasis")) obj.getString("zoneBasis") else null,
                role = if (obj.has("role") && !obj.isNull("role")) obj.getString("role") else null,
                retrieved = if (obj.has("retrieved") && !obj.isNull("retrieved")) obj.getString("retrieved") else null,
            )
        }

        return CalendarCatalog(
            schemaVersion = root.getInt("schemaVersion"),
            dataVersion = root.getString("dataVersion"),
            sources = sources,
            events = events,
            holidayCalendars = holidayCalendars,
            overrides = overrides,
            newYearArrivals = newYearArrivals,
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
            val sourceIds = if (event.id == "khmer_new_year_1") {
                (event.sourceIds + resolveNewYearArrival(year).sourceIds).distinct()
            } else {
                event.sourceIds
            }
            eventDates.map { date ->
                CalendarEvent(
                    id = event.id,
                    date = date,
                    titleKm = names.km,
                    titleEn = names.en,
                    kind = EventKind.OBSERVANCE,
                    basis = DateBasis.CALCULATED,
                    sourceIds = sourceIds,
                    anniversaryBase = event.anniversaryBase,
                )
            }
        }
}
