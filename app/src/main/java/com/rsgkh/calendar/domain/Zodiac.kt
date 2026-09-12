// Copyright (c) 2026 RSG-KH | Apache-2.0 License
package com.rsgkh.calendar.domain

import java.time.LocalDate

enum class ZodiacSign(
    val symbol: String,
    val signName: String,
    val element: String,
    val planet: String,
    val signNameKm: String = "",
    val elementKm: String = "",
    val planetKm: String = "",
) {
    ARIES("♈︎", "Aries", "Fire", "Mars", "មេស", "ភ្លើង", "ព្រះអង្គារ"),
    TAURUS("♉︎", "Taurus", "Earth", "Venus", "ឧសភ", "ដី", "ព្រះសុក្រ"),
    GEMINI("♊︎", "Gemini", "Air", "Mercury", "មិថុន", "ខ្យល់", "ព្រះពុធ"),
    CANCER("♋︎", "Cancer", "Water", "Moon", "កក្កដា", "ទឹក", "ព្រះច័ន្ទ"),
    LEO("♌︎", "Leo", "Fire", "Sun", "សីហា", "ភ្លើង", "ព្រះអាទិត្យ"),
    VIRGO("♍︎", "Virgo", "Earth", "Mercury", "កញ្ញា", "ដី", "ព្រះពុធ"),
    LIBRA("♎︎", "Libra", "Air", "Venus", "តុលា", "ខ្យល់", "ព្រះសុក្រ"),
    SCORPIO("♏︎", "Scorpio", "Water", "Pluto", "វិច្ឆិកា", "ទឹក", "ភ្លុយតូ"),
    SAGITTARIUS("♐︎", "Sagittarius", "Fire", "Jupiter", "ធ្នូ", "ភ្លើង", "ព្រះព្រហស្បតិ៍"),
    CAPRICORN("♑︎", "Capricorn", "Earth", "Saturn", "មករា", "ដី", "ព្រះសៅរ៍"),
    AQUARIUS("♒︎", "Aquarius", "Air", "Uranus", "កុម្ភៈ", "ខ្យល់", "អ៊ុយរ៉ានុស"),
    PISCES("♓︎", "Pisces", "Water", "Neptune", "មីនា", "ទឹក", "ណិបទូន");

    val label: String get() = "$symbol $signName ($element · $planet)"
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
