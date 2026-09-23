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
    @Test fun ganzhiTableUsesEmojiAndEnglishAnimalNamesOnDevice() {
        start(com.rsgkh.calendar.data.AppSettings(khmer = false, useEmojiForGanzhiAnimals = true))
        compose.onNode(hasContentDescription("Thursday, 24 September", substring = true)).performClick()
        compose.onNodeWithTag("ganzhi-sign-day").assertTextEquals("🐮")
        compose.onNodeWithTag("ganzhi-clash-day").assertTextEquals("🐐")
        compose.onNodeWithTag("ganzhi-header-hour").assertDoesNotExist()
        screenshot("ganzhi-device-emoji")
        compose.onNodeWithText("Close").performClick()

        compose.onNodeWithText("Settings").performClick()
        val emojiToggle = L.text("ui.ganzhi_emoji_toggle", false)
        compose.onNodeWithTag("settings-scroll").performScrollToNode(hasContentDescription(emojiToggle))
        compose.onNodeWithContentDescription(emojiToggle).assertIsOn().performClick().assertIsOff()
        compose.onNode(hasText("Calendar") and hasClickAction()).performClick()
        compose.onNode(hasContentDescription("Thursday, 24 September", substring = true)).performClick()
        compose.onNodeWithTag("ganzhi-sign-day").assertTextEquals("Ox")
        compose.onNodeWithTag("ganzhi-clash-day").assertTextEquals("Goat")
        screenshot("ganzhi-device-english-names")
    }

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
            val holidayText = L.text("about.public_holiday_source", k)
            val holidayCandidates = if (k) listOf("ឯកសារផ្លូវការរបស់រដ្ឋ", "គេហទំព័រផ្លូវការរបស់រដ្ឋាភិបាល") else listOf("official government publications", "official government websites")
            val holidayName = holidayCandidates.firstOrNull { holidayText.contains(it) } ?: holidayCandidates[0]
            compose.onNodeWithText(holidayText).performScrollTo().assertIsDisplayed().performFirstLinkClick {
                holidayText.substring(it.start, it.end) == holidayName
            }
            compose.onNodeWithText("https://www.ocm.gov.kh/", substring = true).assertIsDisplayed()
            compose.onNodeWithText(L.text("ui.copy", k)).assertIsDisplayed().performClick()
            compose.onNodeWithText("https://www.ocm.gov.kh/", substring = true).assertDoesNotExist()
            val engineText = L.text("about.calendar_engine", k)
            compose.onNodeWithText(engineText).performScrollTo().assertIsDisplayed().performFirstLinkClick {
                engineText.substring(it.start, it.end) == "Khmer Calendar Engine"
            }
            compose.runOnIdle {
                assertEquals(listOf("https://github.com/RSG-KH/khmer-calendar-engine"), openedUrls)
                openedUrls.clear()
            }
            compose.onNodeWithText("Khmer Calendar Engine").assertDoesNotExist()
            screenshot("engine-sources-$k")
            val licenseHeader = compose.onNodeWithText(L.text("ui.open_source_license.ab00af", k))
            val appNotice = compose.onNodeWithTag("app-license-text", useUnmergedTree = true)
            val engineNotice = compose.onNodeWithTag("engine-license-text", useUnmergedTree = true)
            licenseHeader.assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, L.text("ui.collapsed", k)))
            appNotice.assertDoesNotExist()
            engineNotice.assertDoesNotExist()
            licenseHeader.performScrollTo().performClick()
            licenseHeader.assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, L.text("ui.expanded", k)))
            appNotice.assertExists().assert(hasText("Apache License", substring = true))
            engineNotice.assertExists().assert(hasText("MIT License", substring = true))
            licenseHeader.performScrollTo()
            screenshot("engine-licenses-expanded-$k")
            licenseHeader.performClick()
            licenseHeader.assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, L.text("ui.collapsed", k)))
            appNotice.assertDoesNotExist()
            engineNotice.assertDoesNotExist()
            compose.onNodeWithText(L.text("ui.close.7df7dc", k)).performClick()
            if (!k) {
                compose.onNodeWithTag("settings-scroll").performScrollToNode(hasText("ខ្មែរ"))
                compose.onNodeWithText("ខ្មែរ").performClick()
            }
        }
    }
}
