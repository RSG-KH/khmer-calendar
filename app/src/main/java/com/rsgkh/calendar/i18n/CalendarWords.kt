// Copyright (c) 2026 RSG-KH | Apache-2.0 License
package com.rsgkh.calendar.i18n

import com.rsgkh.calendar.domain.khmerNumber
import java.time.LocalDate

object CalendarWords {
    fun month(month: Int, khmer: Boolean, short: Boolean = false) = L.text("calendar.month.${if (short) "short" else "full"}.$month", khmer)
    fun weekday(day: Int, khmer: Boolean, style: String = "full") = L.text("calendar.weekday.$style.${day % 7}", khmer)
    fun number(value: Int, khmer: Boolean) = if (khmer) khmerNumber(value) else value.toString()
    fun date(date: LocalDate, khmer: Boolean) = L.text("calendar.date_label", khmer,
        "weekday" to weekday(date.dayOfWeek.value, khmer), "day" to number(date.dayOfMonth, khmer), "month" to month(date.monthValue, khmer))
}
