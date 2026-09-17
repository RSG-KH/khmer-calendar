// Copyright (c) 2026 RSG-KH | Apache-2.0 License
package com.rsgkh.calendar.domain

import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

enum class RepeatFrequency(val key: String) { DAYS("days"), WEEKLY("weekly"), MONTHLY("monthly"), YEARLY("yearly") }

data class EventRepeat(
    val frequency: RepeatFrequency,
    val until: LocalDate,
    val interval: Long = 3,
    val includeThirty: Boolean = false,
    val includeFebruary: Boolean = false,
) {
    fun isValid(start: LocalDate) = start.year in 1800..2200 && until.year in 1800..2200 && until >= start &&
        (frequency != RepeatFrequency.DAYS || interval in 1..9_007_199_254_740_991L)

    /** Lazy, range-limited civil dates. Each month keeps the original day anchor. */
    fun dates(start: LocalDate, from: LocalDate = start, through: LocalDate = until): Sequence<LocalDate> =
        candidates(start, from, through).filter { it.included }.map { it.date }

    fun preview(start: LocalDate): RepeatPreview {
        val dates = mutableListOf<LocalDate>()
        val skipped = mutableListOf<LocalDate>()
        var thirty = false
        var february = false
        candidates(start, start, until).forEach {
            if (it.missingDay) {
                if (it.date.monthValue == 2) february = true else thirty = true
            }
            if (it.included) dates.add(it.date) else skipped.add(it.date)
        }
        return RepeatPreview(dates, skipped, thirty, february)
    }

    private data class Candidate(val date: LocalDate, val included: Boolean = true, val missingDay: Boolean = false)

    private fun candidates(start: LocalDate, from: LocalDate, through: LocalDate) = sequence {
        if (!isValid(start)) return@sequence
        val first = maxOf(start, from)
        val last = minOf(until, through)
        if (first > last) return@sequence
        if (frequency == RepeatFrequency.DAYS || frequency == RepeatFrequency.WEEKLY) {
            val step = if (frequency == RepeatFrequency.WEEKLY) 7L else interval
            val elapsed = ChronoUnit.DAYS.between(start, first)
            var offset = (elapsed / step + if (elapsed % step == 0L) 0 else 1) * step
            val limit = ChronoUnit.DAYS.between(start, last)
            while (offset <= limit) {
                yield(Candidate(start.plusDays(offset)))
                if (step > limit - offset) break
                offset += step
            }
        } else {
            var month = if (frequency == RepeatFrequency.MONTHLY) YearMonth.from(first) else YearMonth.of(first.year, start.monthValue)
            val end = YearMonth.from(last)
            while (month <= end) {
                val missing = start.dayOfMonth > month.lengthOfMonth()
                val date = month.atDay(minOf(start.dayOfMonth, month.lengthOfMonth()))
                if (date in first..last) yield(Candidate(date,
                    !missing || if (month.monthValue == 2) includeFebruary else includeThirty, missing))
                month = month.plusMonths(if (frequency == RepeatFrequency.MONTHLY) 1 else 12)
            }
        }
    }
}

data class RepeatPreview(val dates: List<LocalDate>, val skipped: List<LocalDate>, val affectsThirty: Boolean, val affectsFebruary: Boolean)
