// Copyright (c) 2026 RSG-KH | Apache-2.0 License
package com.rsgkh.calendar.domain

import java.time.LocalDate
import com.rsgkh.calendar.i18n.L
import com.rsgkh.calendar.i18n.CalendarWords

/** Date-level traditional year labels; the animal year can change during New Year's first day.
 * Sak changes on Lerng Sak (the last festival day), separately from Buddhist Era rollover.
 * Names and transition conventions follow MomentKH; see the bundled MIT notice.
 */
data class KhmerDateDetails(
    val date: LocalDate, val lunar: LunarDate, val animalYear: Int, val sak: Int,
    val animalYearChangesToday: Boolean,
) {
    fun animalLabel(khmer: Boolean): String {
        val current = L.text("calendar.animal.$animalYear", khmer)
        return if (animalYearChangesToday) "${L.text("calendar.animal.${Math.floorMod(animalYear - 1, 12)}", khmer)} → $current" else current
    }
    private fun fullDate(khmer: Boolean): String {
        val month = CalendarWords.month(date.monthValue, khmer)
        // A reviewed month name may include ខែ; don't repeat it when the sentence supplies it.
        val monthInSentence = if (khmer && L.template("calendar.full_date", true).contains("ខែ{month}")) month.removePrefix("ខែ") else month
        return L.text("calendar.full_date", khmer,
        "weekday" to CalendarWords.weekday(date.dayOfWeek.value, khmer), "lunar" to lunar.fullLabel(khmer),
        "animal" to animalLabel(khmer), "sak" to L.text("calendar.sak.$sak", khmer),
        "buddhist_year" to CalendarWords.number(lunar.buddhistYear, khmer),
        "day" to CalendarWords.number(date.dayOfMonth, khmer), "month" to monthInSentence,
        "year" to CalendarWords.number(date.year, khmer))
    }
    fun fullKhmerDate(): String = fullDate(true)
    fun fullEnglishDate(): String = fullDate(false)
    val gregorianLabel: String get() = L.text("calendar.gregorian_label", false,
        "month" to CalendarWords.month(date.monthValue, false), "day" to date.dayOfMonth, "year" to date.year)
    val zodiac: ZodiacSign get() = Zodiac.forDate(date)

    companion object {

        fun fromGregorian(date: LocalDate): KhmerDateDetails {
            val lunar = KhmerCalendar.fromGregorian(date)
            val newYear = KhmerNewYear.forYear(date.year)
            val lerngSak = newYear.start.plusDays(newYear.days - 1L)
            val animalYear = Math.floorMod(date.year - 4 - if (date < newYear.start) 1 else 0, 12)
            val sak = Math.floorMod(date.year - 638 - if (date < lerngSak) 1 else 0, 10)
            return KhmerDateDetails(date, lunar, animalYear, sak, date == newYear.start)
        }
    }
}
