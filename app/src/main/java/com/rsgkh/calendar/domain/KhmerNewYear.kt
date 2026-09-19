// Copyright (c) 2026 RSG-KH | Apache-2.0 License
package com.rsgkh.calendar.domain

import com.rsgkh.calendar.engine.ArrivalEstimate
import java.time.LocalDate

/** Traditional festival dates from the shared engine; official leave is separate event data. */
data class NewYearCelebration(
    val start: LocalDate,
    val days: Int,
    val dates: List<LocalDate>,
    val arrivalEstimate: ArrivalEstimate? = null,
)

object KhmerNewYear {
    fun forYear(year: Int): NewYearCelebration {
        val result = KhmerCalendar.engine.newYear(year)
        return NewYearCelebration(
            start = result.start.toLocalDate(),
            days = result.days,
            dates = result.dates.map { it.toLocalDate() },
            arrivalEstimate = result.arrivalEstimate,
        )
    }
}
