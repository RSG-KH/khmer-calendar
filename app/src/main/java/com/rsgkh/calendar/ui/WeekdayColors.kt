// Copyright (c) 2026 RSG-KH | Apache-2.0 License
package com.rsgkh.calendar.ui

import androidx.compose.ui.graphics.Color
import java.time.DayOfWeek

// Traditional color families: https://cambodiandevelopmentfoundation.org/7-colors-of-the-week/
// Shades are adapted for readable text on the app's light and dark calendar surfaces.
internal fun weekdayNameColor(day: DayOfWeek, dark: Boolean): Color = when (day) {
    DayOfWeek.MONDAY -> if (dark) Color(0xFFEDD271) else Color(0xFF8A6800) // Yellow
    DayOfWeek.TUESDAY -> if (dark) Color(0xFFDFA1F0) else Color(0xFF9639AE) // Violet/purple
    DayOfWeek.WEDNESDAY -> if (dark) Color(0xFFB1DB72) else Color(0xFF53752D) // Light green
    DayOfWeek.THURSDAY -> if (dark) Color(0xFF69C591) else Color(0xFF187543) // Green
    DayOfWeek.FRIDAY -> if (dark) Color(0xFF8BC7F1) else Color(0xFF2675A3) // Baby blue
    DayOfWeek.SATURDAY -> if (dark) Color(0xFFB29ACF) else Color(0xFF56328C) // Dark purple
    DayOfWeek.SUNDAY -> if (dark) Color(0xFFFF8088) else Color(0xFFD02C3A) // Bright red
}
