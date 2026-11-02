// Copyright (c) 2026 RSG-KH | Apache-2.0 License
package com.rsgkh.calendar

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.inspector.WindowInspector
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import androidx.test.platform.app.InstrumentationRegistry
import com.rsgkh.calendar.data.AppSettings
import com.rsgkh.calendar.data.CustomEvent
import com.rsgkh.calendar.data.EventKind
import com.rsgkh.calendar.data.EventRepository
import com.rsgkh.calendar.data.FontScale
import com.rsgkh.calendar.data.ThemeMode
import com.rsgkh.calendar.domain.KhmerCalendar
import com.rsgkh.calendar.ui.CalendarApp
import com.rsgkh.calendar.ui.readableSp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.annotation.LooperMode
import java.io.File
import java.time.LocalDate
import java.time.LocalTime
import kotlin.math.abs

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [32], qualifiers = "w411dp-h891dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@LooperMode(LooperMode.Mode.PAUSED)
class CalendarCellMarkerTest {
    @get:Rule val compose = createComposeRule()

    private fun saveScreenshotIfRequested(name: String) {
        val dir = System.getProperty("calendar.screenshots") ?: return
        lateinit var image: Bitmap
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val view = WindowInspector.getGlobalWindowViews().last { it.isShown }
            image = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            view.draw(Canvas(image))
        }
        val folder = File(dir).apply { mkdirs() }
        File(folder, "$name.png").outputStream().use { image.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private fun findQuadAndSoloDates(): Pair<LocalDate, LocalDate> {
        var d = LocalDate.of(2024, 1, 1)
        val end = LocalDate.of(2028, 12, 31)
        while (!d.isAfter(end)) {
            val kinds = EventRepository.forDate(d).map { it.kind }.toSet()
            if (EventKind.HOLIDAY in kinds && EventKind.OBSERVANCE in kinds && EventKind.HOLY_DAY in kinds) {
                val solo = (1..d.lengthOfMonth())
                    .map { d.withDayOfMonth(it) }
                    .first { it != d && EventRepository.forDate(it).isEmpty() }
                return d to solo
            }
            d = d.plusDays(1)
        }
        error("Expected a month in 2024..2028 containing both a 3-kind date and an empty date")
    }

    private fun verifyMarkerLayout(
        baseCellHeight: Dp,
        legacyMarkSize: Dp,
        expectedBaseMarkSize: Dp,
        minCornerInset: Dp,
        isPhoneLandscape: Boolean = false,
        screenshotTag: String? = null,
    ) {
        val (quadDate, soloDate) = findQuadAndSoloDates()
        val customsState = mutableStateOf(
            listOf(
                CustomEvent(title = "Solo custom", date = soloDate, time = LocalTime.NOON),
            )
        )
        val settingsState = mutableStateOf(
            AppSettings(khmer = false, theme = ThemeMode.LIGHT, fontScale = FontScale.PERCENT_100)
        )
        compose.setContent {
            CalendarApp(settings = settingsState.value, today = quadDate, customEvents = customsState.value) {
                settingsState.value = it
            }
        }
        compose.waitForIdle()
        val initialCardBounds = compose.onNodeWithTag("calendar-month-card").getUnclippedBoundsInRoot()

        compose.runOnIdle {
            customsState.value = listOf(
                CustomEvent(title = "Solo custom", date = soloDate, time = LocalTime.NOON),
                CustomEvent(title = "Quad custom", date = quadDate, time = LocalTime.NOON),
            )
        }
        compose.waitForIdle()
        val quadCardBounds = compose.onNodeWithTag("calendar-month-card").getUnclippedBoundsInRoot()
        assertEquals(initialCardBounds.width.value, quadCardBounds.width.value, 0.01f)
        assertEquals(initialCardBounds.height.value, quadCardBounds.height.value, 0.01f)

        for (khmer in listOf(false, true)) {
            for (scale in listOf(FontScale.PERCENT_80, FontScale.PERCENT_100, FontScale.PERCENT_120, FontScale.PERCENT_150)) {
                compose.runOnIdle {
                    settingsState.value = settingsState.value.copy(khmer = khmer, fontScale = scale)
                }
                compose.waitForIdle()

                val gridBounds = compose.onNodeWithTag("month-grid").getUnclippedBoundsInRoot()
                val cellBounds = compose.onNodeWithTag("calendar-cell-$quadDate").getUnclippedBoundsInRoot()
                val soloCellBounds = compose.onNodeWithTag("calendar-cell-$soloDate").getUnclippedBoundsInRoot()

                val effectiveFontScale = scale.multiplier.coerceAtLeast(1f)
                val expectedCellHeight = baseCellHeight * effectiveFontScale
                val expectedCellWidth = gridBounds.width / 7 - 2.dp

                assertTrue(
                    "Cell height changed at khmer=$khmer scale=$scale: expected $expectedCellHeight, actual ${cellBounds.height}",
                    abs((cellBounds.height - expectedCellHeight).value) <= 1.0f
                )
                assertTrue(
                    "Cell width changed at khmer=$khmer scale=$scale: expected $expectedCellWidth, actual ${cellBounds.width}",
                    abs((cellBounds.width - expectedCellWidth).value) <= 1.0f
                )
                assertEquals(soloCellBounds.height.value, cellBounds.height.value, 0.01f)
                assertEquals(soloCellBounds.width.value, cellBounds.width.value, 0.5f)

                val soloLunar = KhmerCalendar.fromGregorian(soloDate)
                val lunarLayouts = mutableListOf<TextLayoutResult>()
                val lunarMatcher = (hasText(soloLunar.shortLabel(khmer)) or hasText(soloLunar.compactLabel(khmer))) and
                    hasAnyAncestor(hasTestTag("calendar-cell-$soloDate"))
                compose.onAllNodes(lunarMatcher, useUnmergedTree = true).onFirst()
                    .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(lunarLayouts) }
                val expectedLunarSp = (if (isPhoneLandscape) 7 else 9).readableSp
                assertEquals(expectedLunarSp.value, lunarLayouts.single().layoutInput.style.fontSize.value, 0.01f)

                val soloMark = compose.onNodeWithTag("calendar-mark-$soloDate-custom", useUnmergedTree = true)
                    .getUnclippedBoundsInRoot()
                assertTrue(
                    "Solo marker clamped vertically at khmer=$khmer scale=$scale: width=${soloMark.width}, height=${soloMark.height}",
                    abs((soloMark.width - soloMark.height).value) <= 0.6f
                )
                assertTrue(
                    "Solo marker (${soloMark.width}) not larger than legacy $legacyMarkSize at khmer=$khmer scale=$scale",
                    soloMark.width >= expectedBaseMarkSize - 0.6.dp && soloMark.width > legacyMarkSize + 1.0.dp
                )
                if (scale == FontScale.PERCENT_100) {
                    assertTrue(
                        "Expected base mark size $expectedBaseMarkSize at 100%, got ${soloMark.width}",
                        abs((soloMark.width - expectedBaseMarkSize).value) <= 0.6f
                    )
                }

                val quadMarks = EventKind.entries.map { kind ->
                    compose.onNodeWithTag("calendar-mark-$quadDate-${kind.name.lowercase()}", useUnmergedTree = true)
                        .getUnclippedBoundsInRoot()
                }
                assertEquals(4, quadMarks.size)
                val firstQuadWidth = quadMarks.first().width
                for ((idx, mb) in quadMarks.withIndex()) {
                    assertTrue(
                        "Quad marker $idx clamped vertically at khmer=$khmer scale=$scale: width=${mb.width}, height=${mb.height}",
                        abs((mb.width - mb.height).value) <= 0.6f
                    )
                    assertTrue(
                        "Quad marker $idx inconsistent width at khmer=$khmer scale=$scale: ${mb.width} vs $firstQuadWidth",
                        abs((mb.width - firstQuadWidth).value) <= 0.6f
                    )
                    assertTrue(
                        "Quad marker $idx (${mb.width}) smaller than legacy $legacyMarkSize at khmer=$khmer scale=$scale",
                        mb.width > legacyMarkSize + 0.1.dp
                    )
                    assertTrue(
                        "Quad marker $idx overflows cell vertically at khmer=$khmer scale=$scale: mark=${mb.top}..${mb.bottom}, cell=${cellBounds.top}..${cellBounds.bottom}",
                        mb.top >= cellBounds.top + 1.dp && mb.bottom <= cellBounds.bottom - 1.dp
                    )
                }

                val leftInset = quadMarks.first().left - cellBounds.left
                val rightInset = cellBounds.right - quadMarks.last().right
                assertTrue(
                    "4 markers lack rounded-corner horizontal clearance at khmer=$khmer scale=$scale: leftInset=$leftInset, rightInset=$rightInset (min $minCornerInset)",
                    leftInset >= minCornerInset && rightInset >= minCornerInset
                )

                if (screenshotTag != null && scale == FontScale.PERCENT_100) {
                    saveScreenshotIfRequested("after-quad-$screenshotTag-${if (khmer) "km" else "en"}")
                }
                if (screenshotTag == "phone-port" && khmer && scale == FontScale.PERCENT_150) {
                    saveScreenshotIfRequested("after-quad-phone-port-km-150")
                }
            }
        }
    }

    @Test @Config(qualifiers = "w411dp-h891dp-port-xxhdpi")
    fun phonePortraitMarkersStayProportionalWithoutChangingCellSize() =
        verifyMarkerLayout(56.dp, 6.5.dp, 8.0.dp, minCornerInset = 3.75.dp, screenshotTag = "phone-port")

    @Test @Config(qualifiers = "w320dp-h640dp-port-xhdpi")
    fun compactPhonePortraitMarkersClearRoundedSelectionBorder() =
        verifyMarkerLayout(56.dp, 6.5.dp, 8.0.dp, minCornerInset = 3.75.dp, screenshotTag = "phone-320")

    @Test @Config(qualifiers = "w800dp-h400dp-land-xhdpi")
    fun phoneLandscapeMarkersStayProportionalWithoutChangingCellSize() =
        verifyMarkerLayout(44.dp, 5.5.dp, 7.0.dp, minCornerInset = 3.0.dp, isPhoneLandscape = true, screenshotTag = "phone-land")

    @Test @Config(qualifiers = "w640dp-h320dp-land-xhdpi")
    fun compactPhoneLandscapeMarkersClearRoundedSelectionBorder() =
        verifyMarkerLayout(44.dp, 5.5.dp, 7.0.dp, minCornerInset = 3.0.dp, isPhoneLandscape = true, screenshotTag = "phone-640x320")

    @Test @Config(qualifiers = "sw800dp-w800dp-h1280dp-port-xhdpi")
    fun tabletPortraitMarkersStayProportionalWithoutChangingCellSize() =
        verifyMarkerLayout(64.dp, 8.5.dp, 10.0.dp, minCornerInset = 3.75.dp, screenshotTag = "tablet-port")

    @Test @Config(qualifiers = "sw800dp-w1280dp-h800dp-land-xhdpi")
    fun tabletLandscapeMarkersStayUnclippedInKhmerAndEnglish() =
        verifyMarkerLayout(52.dp, 7.0.dp, 8.25.dp, minCornerInset = 3.75.dp, screenshotTag = "tablet-land")
}
