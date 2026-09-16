// Copyright (c) 2026 RSG-KH | Apache-2.0 License
package com.rsgkh.calendar.domain

import java.time.LocalDate

/** Traditional festival dates from the shared engine; official leave is separate event data. */
data class NewYearCelebration(val start: LocalDate, val days: Int, val dates: List<LocalDate>)

object KhmerNewYear {
    fun forYear(year: Int): NewYearCelebration {
        val result = KhmerCalendar.engine.newYear(year)
        return NewYearCelebration(result.start.toLocalDate(), result.days, result.dates.map { it.toLocalDate() })
    }
}
