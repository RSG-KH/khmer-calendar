// Copyright (c) 2026 RSG-KH | Apache-2.0 License
package com.rsgkh.calendar.data

import com.rsgkh.calendar.domain.KhmerCalendar
import com.rsgkh.calendar.domain.toLocalDate
import com.rsgkh.calendar.engine.RecurrenceRule
import com.rsgkh.calendar.domain.khmerNumber
import com.rsgkh.calendar.i18n.L
import java.time.LocalDate

/** Reviewed calendar patterns, not historical records or official holiday declarations. */
internal object RecurringEvents {
    internal data class Rule(
        val id: String, val titleKey: String, val comparisonKey: String, val type: String,
        val month: Int, val day: Int, val waxing: Boolean, val offset: Int, val duration: Int,
        val fromYear: Int, val throughYear: Int, val anniversaryBase: Int?, val secondAsadh: Boolean,
    ) {
        // The TSV stores weekday occurrence in offset, and unused fields as zero/false.
        // Translate that legacy data representation into the engine's validated contract.
        val calculation = RecurrenceRule(
            id = id, type = type,
            month = if (type.startsWith("new_year_")) 1 else month,
            day = if (type.startsWith("new_year_")) 1 else day,
            waxing = if (type == "khmer_lunar") waxing else true,
            offset = if (type == "solar_nth_weekday") 0 else offset,
            occurrence = if (type == "solar_nth_weekday") offset else 1,
            duration = duration, fromYear = fromYear, throughYear = throughYear,
            monthPolicy = if (secondAsadh) "ordinary_or_second_asadh" else "exact",
        )
    }

    internal val rules: List<Rule> by lazy {
        val stream = checkNotNull(RecurringEvents::class.java.getResourceAsStream("/recurrence-rules.tsv"))
        stream.bufferedReader(Charsets.UTF_8).useLines { lines ->
            lines.filter { it.isNotBlank() && !it.startsWith('#') }.map { line ->
                val f = line.split('\t')
                check(f.size == 13) { "Invalid recurrence rule" }
                Rule(f[0], f[1], f[2], f[3], f[4].toInt(), f[5].toInt(), f[6].equals("true", true),
                    f[7].toInt(), f[8].toInt(), f[9].toInt(), f[10].toInt(), f[11].toIntOrNull(),
                    f[12] == "ordinary_or_second_asadh")
            }.toList().also { check(it.map(Rule::id).distinct().size == it.size) }
        }
    }

    internal fun dates(year: Int): Map<Rule, List<LocalDate>> {
        require(year in KhmerCalendar.engine.minYear..KhmerCalendar.engine.maxYear)
        return rules.filter { year in it.fromYear..it.throughYear }.associateWith { rule ->
            KhmerCalendar.engine.evaluateRule(year, rule.calculation).map { it.date.toLocalDate() }
                .also { dates -> check(dates.isNotEmpty() && dates.all { it.year == year }) }
        }
    }

    fun forYear(year: Int): List<CalendarEvent> = fromDates(year, dates(year))

    internal fun fromDates(year: Int, dates: Map<Rule, List<LocalDate>>): List<CalendarEvent> = dates.flatMap { (rule, dates) ->
        val anniversary = rule.anniversaryBase?.let { year - it }
        fun title(khmer: Boolean): String = if (anniversary == null) L.text(rule.titleKey, khmer)
            else L.text(rule.titleKey, khmer, "anniversary" to if (khmer) khmerNumber(anniversary) else anniversary.toString())
        dates.map { date -> CalendarEvent("calculated:${rule.id}", date, title(true), title(false),
            EventKind.OBSERVANCE, DateBasis.CALCULATED) }
    }
}
