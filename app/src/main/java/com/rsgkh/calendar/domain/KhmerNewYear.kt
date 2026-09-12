// Copyright (c) 2026 RSG-KH | Apache-2.0 License
package com.rsgkh.calendar.domain

import java.time.LocalDate

data class NewYearCelebration(val start: LocalDate, val days: Int) {
    val dates: List<LocalDate> get() = (0 until days).map { start.plusDays(it.toLong()) }
}

/** Traditional solar New Year, adapted from MomentKH ff2bfd5 (MIT; see NOTICE.txt).
 * These are calculated festival dates, not a declaration of public holidays.
 * Arrival times are deliberately omitted: they need separate almanac verification.
 */
object KhmerNewYear {
    private fun aharkun(year: Int) = (year * 292207L + 373) / 800 + 1
    private fun kromthupul(year: Int) = (800 - (year * 292207L + 373) % 800).toInt()
    private fun avoman(year: Int) = ((aharkun(year) * 11 + 650) % 692).toInt()
    private fun bodithey(year: Int): Int {
        val ah = aharkun(year)
        return ((ah + (11 * ah + 650) / 692) % 30).toInt()
    }
    private fun leapMonth(year: Int): Boolean {
        val b = bodithey(year)
        val next = bodithey(year + 1)
        if (b == 24 && next == 6) return true
        if (b == 25 && next == 5) return false
        return b > 24 || b < 6
    }
    private fun leapDay(year: Int): Boolean {
        val a = avoman(year)
        if (a == 0 && avoman(year - 1) == 137) return true
        if (kromthupul(year) <= 207) return a < 127
        if (a == 137 && avoman(year + 1) == 0) return false
        return a < 138
    }
    private fun solarDegree(year: Int, sotin: Int): Int {
        val r2 = 800 * sotin + kromthupul(year - 1)
        val average = 1800 * (r2 / 24350) + 60 * (r2 % 24350 / 811) + r2 % 24350 % 811 / 14 - 3
        val left = if (average < 4800) average - 4800 + 21600 else average - 4800
        val quadrant = left / 1800
        val remainder = when (quadrant) {
            in 0..2 -> quadrant
            in 3..5 -> 10800 - left
            in 6..8 -> left - 10800
            else -> 21600 - left
        }
        val angle = remainder % 1800 / 60
        val minute = remainder % 60
        val segment = 2 * (remainder / 1800) + if (angle >= 15) 1 else 0
        val portion = 60 * (if (angle >= 15) angle - 15 else angle) + minute
        val multipliers = intArrayOf(35, 32, 27, 22, 13, 5)
        val corrections = intArrayOf(0, 35, 67, 94, 116, 129)
        val correction = if (segment <= 5) portion * multipliers[segment] / 900 + corrections[segment] else 134
        val inauguration = if (quadrant <= 5) average - correction else average + correction
        return inauguration % 1800 / 60
    }
    fun forYear(year: Int, engine: KhmerCalendarEngine = KhmerCalendar): NewYearCelebration {
        require(year in 1800..2200)
        val jsYear = year - 638
        val firstSotin = if (kromthupul(jsYear - 1) <= 207) 363 else 362
        val days = if (solarDegree(jsYear, firstSotin) == 0) 4 else 3
        var b = bodithey(jsYear)
        if (leapMonth(jsYear - 1) && leapDay(jsYear - 1)) b = (b + 1) % 30
        val lerngSakMonth = if (b >= 6) 4 else 5
        val lerngSakDay = if (b >= 6) b - 1 else b
        val epoch = LocalDate.of(year, 4, 17)
        val lunar = engine.fromGregorian(epoch)
        val ordinal = lunar.day - 1 + if (lunar.waxing) 0 else 15
        val difference = (lunar.month - 4) * 29 + ordinal - ((lerngSakMonth - 4) * 29 + lerngSakDay)
        val calculated = epoch.minusDays((difference + days - 1).toLong())
        // Upstream's published date corrections within our supported range.
        val start = when (year) {
            in 2011..2015 -> LocalDate.of(year, 4, 14)
            2024 -> LocalDate.of(year, 4, 13)
            else -> calculated
        }
        return NewYearCelebration(start, days)
    }
}
