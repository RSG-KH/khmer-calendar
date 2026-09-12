// Copyright (c) 2026 RSG-KH | Apache-2.0 License
package com.rsgkh.calendar.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.rsgkh.calendar.data.AppSettings
import com.rsgkh.calendar.data.ThemeMode
import java.time.LocalDate

@Preview(name = "English · light", showBackground = true, widthDp = 390, heightDp = 844)
@Composable private fun EnglishLightPreview() {
    CalendarApp(AppSettings(theme = ThemeMode.LIGHT, khmer = false), LocalDate.of(2026, 9, 10)) {}
}
@Preview(name = "Khmer · dark", showBackground = true, widthDp = 390, heightDp = 844)
@Composable private fun KhmerDarkPreview() {
    CalendarApp(AppSettings(theme = ThemeMode.DARK, khmer = true), LocalDate.of(2026, 9, 10)) {}
}
