// Copyright (c) 2026 RSG-KH | Apache-2.0 License
package com.rsgkh.calendar.domain

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import com.rsgkh.calendar.i18n.L

data class LunarDate(val day: Int, val waxing: Boolean, val month: Int, val buddhistYear: Int, val monthLength: Int) {
    val isHolyDay: Boolean get() = day == 8 || (waxing && day == 15) || (!waxing && day == monthLength - 15)
    val isShavingDay: Boolean get() = day == 7 || (waxing && day == 14) || (!waxing && day == monthLength - 16)
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

/**
 * Khmer civil lunar calendar, 1900–2100. Calendar arithmetic is adapted from
 * MetheaX/khmer-chhankitek-calendar (MIT), based on Phylypo Tum and Thyrith Sor.
 * See assets/NOTICE.txt. This is the traditional calendar, not an astronomical
 * moon-illumination approximation. Immutable month starts make lookups inexpensive.
 */
interface KhmerCalendarEngine {
    fun fromGregorian(date: LocalDate): LunarDate
}

object KhmerCalendar : KhmerCalendarEngine {
    val minDate: LocalDate = LocalDate.of(1800, 1, 1)
    val maxDate: LocalDate = LocalDate.of(2200, 12, 31)
    private data class MonthStart(val date: LocalDate, val month: Int, val length: Int)
    private fun approximateYear(date: LocalDate) = date.year + if (date.monthValue <= 4) 543 else 544
    private fun aharkun(year: Int) = (year * 292207L + 499) / 800 + 4
    private fun avoman(year: Int) = ((11 * aharkun(year) + 25) % 692).toInt()
    private fun bodithey(year: Int): Int {
        val ah = aharkun(year)
        return ((11 * ah + 25) / 692 + ah + 29).rem(30).toInt()
    }
    private fun rawLeap(year: Int): Int {
        val b = bodithey(year)
        val a = avoman(year)
        val leapMonth = if (b == 25 && bodithey(year + 1) == 5) false
            else (b == 24 && bodithey(year + 1) == 6) || b >= 25 || b <= 5
        val solarLeap = 800 - (year * 292207L + 499) % 800 <= 207
        val leapDay = if (solarLeap) a <= 126 else a <= 137 && !(a == 137 && avoman(year + 1) == 0)
        return (if (leapMonth) 1 else 0) + (if (leapDay) 2 else 0)
    }
    internal fun leapType(year: Int): Int {
        val type = rawLeap(year)
        if (type and 1 != 0) return 1
        if (type and 2 != 0) return 2
        // A leap day deferred by one or more consecutive leap-month years carries forward.
        // Matches MomentKH ff2bfd5; the older Java port only examined one prior year.
        var previous = year - 1
        while (rawLeap(previous) and 1 != 0) {
            if (rawLeap(previous) and 2 != 0) return 2
            previous--
        }
        return 0
    }
    private fun monthLength(month: Int, year: Int): Int = when {
        month == 6 && leapType(year) == 2 -> 30
        month >= 12 -> 30
        month % 2 == 0 -> 29
        else -> 30
    }
    private fun nextMonth(month: Int, year: Int): Int = when (month) {
        6 -> if (leapType(year) == 1) 12 else 7
        11 -> 0
        12 -> 13
        13 -> 8
        else -> month + 1
    }
    private val months: List<MonthStart> by lazy {
        buildList {
            var start = LocalDate.of(1799, 12, 27)
            var month = 1 // 27 December 1799: 1 Koeut, Boss. Reaches 1 January 1900 exactly at 1 Koeut, Boss.
            while (start <= maxDate.plusMonths(1)) {
                val length = monthLength(month, approximateYear(start))
                add(MonthStart(start, month, length))
                start = start.plusDays(length.toLong())
                month = nextMonth(month, approximateYear(start))
            }
        }
    }
    private val buddhistNewYears: Map<Int, LocalDate> by lazy {
        months.filter { it.month == 5 }.associate { it.date.year to it.date.plusDays(15) }
    }
    override fun fromGregorian(date: LocalDate): LunarDate {
        require(date in minDate..maxDate) { "Supported dates: 1800–2200" }
        val search = months.binarySearchBy(date) { it.date }
        val start = months[if (search >= 0) search else -search - 2]
        val offset = ChronoUnit.DAYS.between(start.date, date).toInt()
        val beYear = date.year + if (date < buddhistNewYears.getValue(date.year)) 543 else 544
        return LunarDate(offset % 15 + 1, offset < 15, start.month, beYear, start.length)
    }
}
