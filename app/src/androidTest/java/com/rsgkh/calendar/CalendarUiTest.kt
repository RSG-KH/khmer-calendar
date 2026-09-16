// Copyright (c) 2026 RSG-KH | Apache-2.0 License
package com.rsgkh.calendar

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.compose.ui.test.*
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.semantics.SemanticsProperties
import com.rsgkh.calendar.i18n.L
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CalendarUiTest : CalendarUiScenarios() {
    // Device coverage for inline source links and packaged license assets.
    @Test fun aboutLinksAndEngineSourcesWorkInBothLanguages() {
        val openedUrls = mutableListOf<String>()
        start(uriHandler = object : UriHandler {
            override fun openUri(uri: String) { openedUrls.add(uri) }
        })
        compose.onNodeWithText("Settings").performClick()
        for (k in listOf(false, true)) {
            val version = L.text("about.version", k, "version" to BuildConfig.VERSION_NAME)
            compose.onNodeWithTag("settings-scroll").performScrollToNode(hasText(version))
            compose.onNodeWithText(version).assertIsDisplayed()
            compose.onNodeWithText("PWA · RSG-KH/khmer-calendar-pwa").assertIsDisplayed().assertHasClickAction()
            compose.onNodeWithText("Android · RSG-KH/khmer-calendar").assertIsDisplayed().assertHasClickAction()
            screenshot("about-$k")
            compose.onNodeWithText(L.text("ui.calendar_sources_licenses.c2bdb3", k)).performScrollTo().performClick()
            val engineText = L.text("about.calendar_engine", k)
            compose.onNodeWithText(engineText).performScrollTo().assertIsDisplayed().performFirstLinkClick {
                engineText.substring(it.start, it.end) == "Khmer Calendar Engine"
            }
            compose.runOnIdle {
                assertEquals(listOf("https://github.com/RSG-KH/khmer-calendar-engine"), openedUrls)
                openedUrls.clear()
            }
            compose.onNodeWithText("Khmer Calendar Engine").assertDoesNotExist()
            compose.onNodeWithText(if (k) "ព្រឹត្តិការណ៍សម្រាប់ឆ្នាំ ២០០០" else "Events for 2000", substring = true).assertDoesNotExist()
            screenshot("engine-sources-$k")
            val licenseHeader = compose.onNodeWithText(L.text("ui.open_source_license.ab00af", k))
            val notice = compose.onNodeWithText("Apache License", substring = true)
            licenseHeader.assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, L.text("ui.collapsed", k)))
            notice.assertDoesNotExist()
            licenseHeader.performScrollTo().performClick()
            licenseHeader.assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, L.text("ui.expanded", k)))
            notice.assertExists().assert(hasText("MIT License", substring = true))
            licenseHeader.performScrollTo()
            screenshot("engine-licenses-expanded-$k")
            licenseHeader.performClick()
            licenseHeader.assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, L.text("ui.collapsed", k)))
            notice.assertDoesNotExist()
            compose.onNodeWithText(L.text("ui.close.7df7dc", k)).performClick()
            if (!k) {
                compose.onNodeWithTag("settings-scroll").performScrollToNode(hasText("ខ្មែរ"))
                compose.onNodeWithText("ខ្មែរ").performClick()
            }
        }
    }
}
