// Copyright (c) 2026 RSG-KH | Apache-2.0 License
package com.rsgkh.calendar

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.compose.ui.test.*
import com.rsgkh.calendar.data.EventRepository
import com.rsgkh.calendar.i18n.L
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CalendarUiTest : CalendarUiScenarios() {
    // Device coverage for inline source links and packaged license assets.
    @Test fun aboutLinksAndEngineSourcesWorkInBothLanguages() {
        start()
        compose.onNodeWithText("Settings").performClick()
        for (k in listOf(false, true)) {
            val version = L.text("about.version", k, "version" to BuildConfig.VERSION_NAME)
            compose.onNodeWithTag("settings-scroll").performScrollToNode(hasText(version))
            compose.onNodeWithText(version).assertIsDisplayed()
            compose.onNodeWithText("PWA · RSG-KH/khmer-calendar-pwa").assertIsDisplayed().assertHasClickAction()
            compose.onNodeWithText("Android · RSG-KH/khmer-calendar").assertIsDisplayed().assertHasClickAction()
            screenshot("about-$k")
            compose.onNodeWithText(L.text("ui.calendar_sources_licenses.c2bdb3", k)).performScrollTo().performClick()
            val eventSourceText = L.text("ui.events_2000_2030_from_khmer_lunar_calendar_available_of.93ee10", k)
            val sourceLinks = listOf(
                (if (k) "គេហទំព័រផ្លូវការមួយចំនួនរបស់រដ្ឋាភិបាល" else "some official government websites") to EventRepository.GOVERNMENT_SOURCE_URLS,
                (if (k) "ប្រតិទិនចន្ទគតិខ្មែរ" else "Khmer Chhankitek Calendar") to listOf(EventRepository.SOURCE_URL),
            )
            for ((label, urls) in sourceLinks) {
                compose.onNodeWithText(eventSourceText).performScrollTo().performFirstLinkClick {
                    eventSourceText.substring(it.start, it.end) == label
                }
                urls.forEach { compose.onNodeWithText(it).assertIsDisplayed() }
                compose.onNodeWithText(if (k) "ចម្លង" else "Copy").performClick()
            }
            compose.onNodeWithText(L.text("about.calendar_engine", k)).performScrollTo().assertIsDisplayed()
            compose.onNodeWithText("Khmer Calendar Engine").performScrollTo().assertIsDisplayed().assertHasClickAction()
            screenshot("engine-sources-$k")
            compose.onNodeWithText(L.text("ui.open_source_license.ab00af", k)).performScrollTo().performClick()
            val notice = compose.onNodeWithText("Apache License", substring = true)
            notice.assertExists().assert(hasText("MIT License", substring = true))
            compose.onNodeWithText(L.text("ui.close.7df7dc", k)).performClick()
            if (!k) {
                compose.onNodeWithTag("settings-scroll").performScrollToNode(hasText("ខ្មែរ"))
                compose.onNodeWithText("ខ្មែរ").performClick()
            }
        }
    }
}
