// Copyright (c) 2026 RSG-KH | Apache-2.0 License
package com.rsgkh.calendar.ui

import androidx.compose.ui.graphics.Color
import java.time.DayOfWeek

// Traditional color families: https://cambodiandevelopmentfoundation.org/7-colors-of-the-week/
// Shades are adapted for readable text on the app's light and dark calendar surfaces.
internal fun weekdayNameColor(day: DayOfWeek, dark: Boolean): Color = when (day) {
    DayOfWeek.MONDAY -> if (dark) Color(0xFFF3D45D) else Color(0xFF916600) // Yellow
    DayOfWeek.TUESDAY -> if (dark) Color(0xFFE28EF5) else Color(0xFFA026BA) // Violet/purple
    DayOfWeek.WEDNESDAY -> if (dark) Color(0xFFADE05A) else Color(0xFF487C16) // Light green
    DayOfWeek.THURSDAY -> if (dark) Color(0xFF54D18D) else Color(0xFF08783D) // Green
    DayOfWeek.FRIDAY -> if (dark) Color(0xFF72C7FA) else Color(0xFF0875AC) // Baby blue
    DayOfWeek.SATURDAY -> if (dark) Color(0xFFBA91E8) else Color(0xFF60279D) // Dark purple
    DayOfWeek.SUNDAY -> if (dark) Color(0xFFFF7380) else Color(0xFFD41E31) // Bright red
}
