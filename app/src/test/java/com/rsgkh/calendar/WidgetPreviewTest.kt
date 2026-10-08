// SPDX-License-Identifier: Apache-2.0
package com.rsgkh.calendar

import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.rsgkh.calendar.data.AppSettings
import com.rsgkh.calendar.data.ThemeMode
import com.rsgkh.calendar.ui.CalendarTheme
import com.rsgkh.calendar.ui.WidgetAddDialog
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [32], qualifiers = "w411dp-h891dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@LooperMode(LooperMode.Mode.PAUSED)
class WidgetPreviewTest {
    @get:Rule val compose = createComposeRule()

    @Test @Config(qualifiers = "w411dp-h891dp-notnight-xhdpi")
    fun previewsFollowAppThemeOnLightDevices() = checkPreviewThemes()

    @Test @Config(qualifiers = "w411dp-h891dp-night-xhdpi")
    fun previewsFollowAppThemeOnDarkDevices() = checkPreviewThemes()

    private fun checkPreviewThemes() {
        val theme = mutableStateOf(ThemeMode.LIGHT)
        compose.setContent {
            CalendarTheme(AppSettings(khmer = false, theme = theme.value)) {
                key(theme.value) { WidgetAddDialog(false) {} }
            }
        }
        for (mode in listOf(ThemeMode.LIGHT, ThemeMode.DARK)) {
            compose.runOnIdle { theme.value = mode }
            repeat(5) { page ->
                val pixels = compose.onNodeWithTag("widget-preview-$page").assertIsDisplayed()
                    .captureToImage().toPixelMap()
                var brightness = 0f
                var samples = 0
                for (y in 0 until pixels.height step 4) {
                    for (x in 0 until pixels.width step 4) {
                        brightness += pixels[x, y].luminance()
                        samples++
                    }
                }
                brightness /= samples
                assertTrue("Preview $page in $mode has brightness $brightness",
                    if (mode == ThemeMode.DARK) brightness < 0.25f else brightness > 0.6f)
                if (page < 4) compose.onNodeWithContentDescription("Next widget").performClick()
            }
        }
    }
}
