// Copyright (c) 2026 RSG-KH | Apache-2.0 License
package com.rsgkh.calendar.domain

import java.time.LocalDate
import com.rsgkh.calendar.engine.CalendarDate
import com.rsgkh.calendar.engine.GregorianDate
import com.rsgkh.calendar.engine.KhmerCalendarEngine
import com.rsgkh.calendar.i18n.L

/** Android labels around the shared engine's lunar result. */
class LunarDate internal constructor(private val value: com.rsgkh.calendar.engine.LunarDate) {
    val day: Int get() = value.day
    val waxing: Boolean get() = value.waxing
    val month: Int get() = value.month
    val buddhistYear: Int get() = value.buddhistYear
    val monthLength: Int get() = value.monthLength
    val isHolyDay: Boolean get() = value.isHolyDay
    val isShavingDay: Boolean get() = value.isShavingDay
    fun shortLabel(khmer: Boolean): String = L.text("calendar.lunar_short", khmer,
        "day" to if (khmer) khmerNumber(day) else day.toString(),
        "phase" to L.text("calendar.phase.${if (waxing) "waxing" else "waning"}_short", khmer))
    fun fullLabel(khmer: Boolean): String = L.text("calendar.lunar_full", khmer,
        "short" to shortLabel(khmer), "day" to if (khmer) khmerNumber(day) else day.toString(),
        "phase" to L.text("calendar.phase.${if (waxing) "waxing" else "waning"}", khmer),
        "month" to L.text("calendar.lunar_month.$month", khmer))
    companion object {
        val MONTHS_KM get() = (0..13).map { L.text("calendar.lunar_month.$it", true) }
        val MONTHS_EN get() = (0..13).map { L.text("calendar.lunar_month.$it", false) }
    }
}

fun khmerNumber(value: Int): String = value.toString().map { if (it in '0'..'9') '០' + (it - '0') else it }.joinToString("")

/** Civil-date adapter; all Khmer calendar calculations come from the released engine. */
object KhmerCalendar {
    internal val engine = KhmerCalendarEngine()
    val minDate: LocalDate = LocalDate.of(engine.minYear, 1, 1)
    val maxDate: LocalDate = LocalDate.of(engine.maxYear, 12, 31)

    internal fun details(date: LocalDate): CalendarDate =
        engine.fromGregorian(date.year, date.monthValue, date.dayOfMonth)

    fun fromGregorian(date: LocalDate): LunarDate = LunarDate(details(date).lunar)
}

internal fun GregorianDate.toLocalDate(): LocalDate = LocalDate.of(year, month, day)
