// Copyright (c) 2026 RSG-KH | Apache-2.0 License
package com.rsgkh.calendar.domain

import java.time.LocalDate

/**
 * Western zodiac names are proper sign names and stay in English in every app language.
 * Do not substitute Khmer month-like labels (for example, តុលា for Libra): that produced
 * incorrect Productivity widget text. Date details also display these English names.
 */
enum class ZodiacSign(
    val emoji: String,
    val signName: String,
    val element: String,
    val planet: String,
) {
    ARIES("♈️", "Aries", "Fire", "Mars"),
    TAURUS("♉️", "Taurus", "Earth", "Venus"),
    GEMINI("♊️", "Gemini", "Air", "Mercury"),
    CANCER("♋️", "Cancer", "Water", "Moon"),
    LEO("♌️", "Leo", "Fire", "Sun"),
    VIRGO("♍️", "Virgo", "Earth", "Mercury"),
    LIBRA("♎️", "Libra", "Air", "Venus"),
    SCORPIO("♏️", "Scorpio", "Water", "Pluto"),
    SAGITTARIUS("♐️", "Sagittarius", "Fire", "Jupiter"),
    CAPRICORN("♑️", "Capricorn", "Earth", "Saturn"),
    AQUARIUS("♒️", "Aquarius", "Air", "Uranus"),
    PISCES("♓️", "Pisces", "Water", "Neptune");

    val symbol: String get() = emoji
    val label: String get() = "$emoji $signName ($element · $planet)"
}

object Zodiac {
    fun forDate(date: LocalDate): ZodiacSign = forMonthDay(date.monthValue, date.dayOfMonth)

    fun forMonthDay(month: Int, day: Int): ZodiacSign {
        return when (month) {
            1 -> if (day <= 19) ZodiacSign.CAPRICORN else ZodiacSign.AQUARIUS
            2 -> if (day <= 18) ZodiacSign.AQUARIUS else ZodiacSign.PISCES
            3 -> if (day <= 20) ZodiacSign.PISCES else ZodiacSign.ARIES
            4 -> if (day <= 19) ZodiacSign.ARIES else ZodiacSign.TAURUS
            5 -> if (day <= 20) ZodiacSign.TAURUS else ZodiacSign.GEMINI
            6 -> if (day <= 20) ZodiacSign.GEMINI else ZodiacSign.CANCER
            7 -> if (day <= 22) ZodiacSign.CANCER else ZodiacSign.LEO
            8 -> if (day <= 22) ZodiacSign.LEO else ZodiacSign.VIRGO
            9 -> if (day <= 22) ZodiacSign.VIRGO else ZodiacSign.LIBRA
            10 -> if (day <= 22) ZodiacSign.LIBRA else ZodiacSign.SCORPIO
            11 -> if (day <= 21) ZodiacSign.SCORPIO else ZodiacSign.SAGITTARIUS
            12 -> if (day <= 21) ZodiacSign.SAGITTARIUS else ZodiacSign.CAPRICORN
            else -> error("Invalid month: $month")
        }
    }
}
