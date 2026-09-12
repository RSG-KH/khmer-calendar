// Copyright (c) 2026 RSG-KH | Apache-2.0 License
package com.rsgkh.calendar.data

import com.rsgkh.calendar.domain.KhmerCalendar
import com.rsgkh.calendar.domain.KhmerNewYear
import com.rsgkh.calendar.domain.LunarDate
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
        fun matches(lunar: LunarDate): Boolean = lunar.day == day && lunar.waxing == waxing &&
            (lunar.month == month || (secondAsadh && lunar.month == 13))
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
        require(year in 1800..2200)
        val active = rules.filter { year in it.fromYear..it.throughYear }
        val lunarRules = active.filter { it.type == "khmer_lunar" }
        val anchors = mutableMapOf<Rule, LocalDate>()
        var date = LocalDate.of(year, 1, 1)
        while (date.year == year) {
            val lunar = KhmerCalendar.fromGregorian(date)
            for (rule in lunarRules) if (rule.matches(lunar)) {
                check(anchors.put(rule, date) == null) { "Multiple lunar anchors for ${rule.id}: $year" }
            }
            date = date.plusDays(1)
        }
        val newYear = KhmerNewYear.forYear(year)
        return active.associateWith { rule ->
            val dates = when (rule.type) {
                "new_year_first" -> listOf(newYear.start)
                "new_year_middle" -> newYear.dates.drop(1).dropLast(1)
                "new_year_last" -> listOf(newYear.dates.last())
                "solar_nth_weekday" -> {
                    val first = LocalDate.of(year, rule.month, 1)
                    val daysToAdd = (rule.day - first.dayOfWeek.value + 7) % 7 + (rule.offset - 1) * 7
                    val anchor = first.plusDays(daysToAdd.toLong())
                    (0 until rule.duration).map { anchor.plusDays(it.toLong()) }
                }
                else -> {
                    val anchor = if (rule.type == "solar") LocalDate.of(year, rule.month, rule.day)
                        else checkNotNull(anchors[rule]) { "Missing lunar anchor for ${rule.id}: $year" }
                    (0 until rule.duration).map { anchor.plusDays(rule.offset.toLong() + it) }
                }
            }
            check(dates.isNotEmpty() && dates.all { it.year == year })
            dates
        }
    }

    fun forYear(year: Int): List<CalendarEvent> = dates(year).flatMap { (rule, dates) ->
        val anniversary = rule.anniversaryBase?.let { year - it }
        fun title(khmer: Boolean): String = if (anniversary == null) L.text(rule.titleKey, khmer)
            else L.text(rule.titleKey, khmer, "anniversary" to if (khmer) khmerNumber(anniversary) else anniversary.toString())
        dates.map { date -> CalendarEvent("calculated:${rule.id}", date, title(true), title(false),
            EventKind.OBSERVANCE, DateBasis.CALCULATED) }
    }
}
