// Copyright (c) 2026 RSG-KH | Apache-2.0 License
package com.rsgkh.calendar

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.rsgkh.calendar.data.AppSettings
import com.rsgkh.calendar.data.FontScale
import com.rsgkh.calendar.ui.CalendarTheme
import com.rsgkh.calendar.ui.CalendarWeekdayHeader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.annotation.LooperMode
import kotlin.math.abs

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [32], qualifiers = "w800dp-h1280dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@LooperMode(LooperMode.Mode.PAUSED)
class WeekdayHeaderTest {
    @get:Rule val compose = createComposeRule()

    @Test fun headingsRefitAcrossWidthsFontsLanguagesAndWeekStartsWithoutOverlap() {
        val settings = mutableStateOf(AppSettings())
        val width = mutableStateOf(320.dp)
        val family = mutableStateOf(FontFamily.Default)
        lateinit var density: Density
        compose.setContent {
            CalendarTheme(settings.value) {
                density = LocalDensity.current
                ProvideTextStyle(LocalTextStyle.current.copy(fontFamily = family.value)) {
                    Box(Modifier.width(width.value).testTag("headings")) {
                        CalendarWeekdayHeader(settings.value)
                    }
                }
            }
        }
        for (font in listOf(FontFamily.Default, FontFamily.Serif, FontFamily.Monospace)) {
            for (khmer in listOf(false, true)) for (mondayFirst in listOf(false, true)) {
                for (longNames in listOf(false, true)) for (scale in listOf(FontScale.PERCENT_80, FontScale.PERCENT_150)) {
                    for (rowWidth in listOf(240.dp, 320.dp, 600.dp)) {
                        compose.runOnIdle {
                            family.value = font
                            width.value = rowWidth
                            settings.value = settings.value.copy(khmer = khmer, mondayFirst = mondayFirst,
                                showLongerWeekdayNames = longNames, fontScale = scale)
                        }
                        val context = "$font khmer=$khmer monday=$mondayFirst long=$longNames scale=$scale width=$rowWidth"
                        val row = compose.onNodeWithTag("headings").fetchSemanticsNode().boundsInRoot
                        val days = if (mondayFirst) (1..7).toList() else listOf(7, 1, 2, 3, 4, 5, 6)
                        val sizes = mutableListOf<Float>()
                        val bounds = days.mapIndexed { index, day ->
                            val node = compose.onNodeWithTag("weekday-header-$day")
                            val layouts = mutableListOf<TextLayoutResult>()
                            node.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
                            val layout = layouts.single()
                            assertFalse("Clipped heading: $context day=$day size=${layout.size} paragraph=${layout.multiParagraph.width}x${layout.multiParagraph.height} overflow=${layout.didOverflowWidth}/${layout.didOverflowHeight}", layout.hasVisualOverflow)
                            sizes.add(layouts.single().layoutInput.style.fontSize.value)
                            node.fetchSemanticsNode().boundsInRoot.also { label ->
                                val center = row.left + row.width * (index + 0.5f) / 7
                                assertTrue("Off-center heading: $context day=$day", abs(label.center.x - center) <= 1f)
                                val inset = with(density) { 2.dp.toPx() }
                                assertTrue("Heading outside row: $context day=$day", label.left >= row.left + inset && label.right <= row.right - inset)
                            }
                        }
                        assertEquals("Headings must share a size: $context", 1, sizes.distinct().size)
                        bounds.zipWithNext().forEach { (left, right) ->
                            assertTrue("Headings too close: $context", right.left - left.right >= with(density) { 4.dp.toPx() })
                        }
                    }
                }
            }
        }
    }
}
