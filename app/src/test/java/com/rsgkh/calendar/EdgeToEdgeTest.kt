// Copyright (c) 2026 RSG-KH | Apache-2.0 License
package com.rsgkh.calendar

import android.graphics.Color
import android.os.Build
import android.view.ContextThemeWrapper
import android.view.WindowManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31, 34, 35])
class EdgeToEdgeTest {
    @Test
    @Config(qualifiers = "notnight")
    fun lightThemePreservesSystemBarBehaviorAcrossAndroidVersions() {
        verifyWindow(android.R.style.Theme_Material_Light_NoActionBar, 0xFFF3F4F8.toInt())
    }

    @Test
    @Config(qualifiers = "night")
    fun darkThemePreservesSystemBarBehaviorAcrossAndroidVersions() {
        verifyWindow(android.R.style.Theme_Material_NoActionBar, 0xFF0C0E12.toInt())
    }

    @Suppress("DEPRECATION") // Verify legacy behavior only where Android still supports it.
    private fun verifyWindow(platformTheme: Int, background: Int) {
        Robolectric.buildActivity(MainActivity::class.java).create().use { controller ->
            val activity = controller.get()
            val window = activity.window
            assertEquals(WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS,
                window.attributes.layoutInDisplayCutoutMode)
            assertTrue(window.isNavigationBarContrastEnforced)

            val splash = activity.obtainStyledAttributes(intArrayOf(android.R.attr.windowSplashScreenBackground))
            try {
                assertEquals(background, splash.getColor(0, Color.MAGENTA))
            } finally {
                splash.recycle()
            }

            if (Build.VERSION.SDK_INT < 35) {
                assertEquals(Color.TRANSPARENT, window.statusBarColor)
                assertEquals(Color.TRANSPARENT, window.navigationBarColor)
                assertFalse(window.isStatusBarContrastEnforced)
            } else {
                // The v35 theme must not inherit the pre-35 bar overrides, including
                // when night mode resolves the shared base to its dark variant.
                val attributes = intArrayOf(android.R.attr.statusBarColor,
                    android.R.attr.navigationBarColor, android.R.attr.enforceStatusBarContrast)
                val actual = activity.obtainStyledAttributes(attributes)
                val expected = ContextThemeWrapper(activity.application, platformTheme)
                    .obtainStyledAttributes(attributes)
                try {
                    assertEquals(expected.getColor(0, Color.MAGENTA), actual.getColor(0, Color.MAGENTA))
                    assertEquals(expected.getColor(1, Color.MAGENTA), actual.getColor(1, Color.MAGENTA))
                    assertEquals(expected.getBoolean(2, true), actual.getBoolean(2, true))
                } finally {
                    actual.recycle()
                    expected.recycle()
                }
            }
        }
    }
}
