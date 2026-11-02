// SPDX-License-Identifier: Apache-2.0
package com.rsgkh.calendar.widgets

import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.core.view.children
import androidx.core.view.descendants
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.compose
import androidx.glance.appwidget.provideContent
import androidx.test.core.app.ApplicationProvider
import com.rsgkh.calendar.data.Accent
import com.rsgkh.calendar.data.AppSettings
import com.rsgkh.calendar.data.FontScale
import com.rsgkh.calendar.data.ThemeMode
import com.rsgkh.calendar.domain.KhmerDateDetails
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate
import kotlin.math.abs

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class MonthWidgetMarkerAlignmentTest {

    private fun buildSnapshot(
        khmer: Boolean = true,
        scale: FontScale = FontScale.PERCENT_100,
    ): WidgetSnapshot {
        val today = LocalDate.of(2026, 11, 2)
        // Row 4 (Nov 22..28, Sunday-first) places combinations & solo markers in the same week row:
        // 23: holiday + observance, 24: holiday + holy day, 25: all 4 markers, 26: personal, 28: observance.
        val monthDays = (1..30).map { d ->
            val date = LocalDate.of(2026, 11, d)
            MonthDayInfo(
                date = date,
                dayNumber = d,
                lunarLabel = "",
                isHolyDay = d in setOf(3, 9, 17, 24, 25),
                isShavingDay = false,
                hasHoliday = d in setOf(9, 23, 24, 25),
                hasObservance = d in setOf(7, 23, 25, 28),
                hasPersonal = d in setOf(12, 25, 26),
            )
        }
        val emptyDay = WidgetDay(today, emptyList(), 0)
        return WidgetSnapshot(
            settings = AppSettings(
                khmer = khmer,
                theme = ThemeMode.DARK,
                accent = Accent.LIME,
                widgetFontScale = scale,
            ),
            today = today,
            details = KhmerDateDetails.fromGregorian(today),
            yesterday = emptyDay,
            current = emptyDay,
            tomorrow = emptyDay,
            holidayTitle = null,
            monthDays = monthDays,
        )
    }

    private suspend fun composeAndLayout(
        context: Context,
        snapshot: WidgetSnapshot,
        widthDp: Int,
        heightDp: Int,
    ): ViewGroup {
        val widget = object : GlanceAppWidget() {
            override suspend fun provideGlance(context: Context, id: GlanceId) {
                provideContent { MonthWidgetContent(snapshot, 0) }
            }
        }
        val views = withTimeout(10_000) { widget.compose(context, size = DpSize(widthDp.dp, heightDp.dp)) }
        val root = views.apply(context, FrameLayout(context)) as ViewGroup
        val density = context.resources.displayMetrics.density
        root.measure(
            View.MeasureSpec.makeMeasureSpec((widthDp * density).toInt(), View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec((heightDp * density).toInt(), View.MeasureSpec.EXACTLY),
        )
        root.layout(0, 0, root.measuredWidth, root.measuredHeight)
        return root
    }

    private fun View.topInRoot(root: View): Int {
        var y = top
        var p = parent
        while (p is View && p !== root) {
            y += p.top
            p = p.parent
        }
        return y
    }

    @Test
    fun cellMarkersAlignVerticallyWithoutOverflowAcrossSizesAndFontScales() = runBlocking {
        val baseContext = ApplicationProvider.getApplicationContext<Context>()
        data class Case(val widthDp: Int, val heightDp: Int, val scale: FontScale, val systemScale: Float = 1f)
        val cases = listOf(
            Case(238, 185, FontScale.PERCENT_100),
            Case(238, 196, FontScale.PERCENT_100),
            Case(238, 201, FontScale.PERCENT_100),
            Case(320, 270, FontScale.PERCENT_100),
            Case(320, 270, FontScale.PERCENT_130),
            Case(360, 310, FontScale.PERCENT_150),
            Case(360, 310, FontScale.PERCENT_120, systemScale = 1.15f),
        )

        for (khmer in listOf(false, true)) for (case in cases) {
            val context = baseContext.createConfigurationContext(
                Configuration(baseContext.resources.configuration).apply { fontScale = case.systemScale },
            )
            val density = context.resources.displayMetrics.density
            val maxMarkerPx = (18f * case.scale.multiplier * case.systemScale * density).toInt()
            val root = composeAndLayout(context, buildSnapshot(khmer, case.scale), case.widthDp, case.heightDp)

            val markerRows = root.descendants.filterIsInstance<LinearLayout>().filter { row ->
                row.orientation == LinearLayout.HORIZONTAL &&
                    row.childCount in 1..4 &&
                    row.children.all { it is ImageView && it.height in 4..maxMarkerPx && it.width in 4..maxMarkerPx }
            }.toList()

            val label = "$case, khmer=$khmer"
            // Days with markers: 3, 7, 9, 12, 17, 23, 24, 25, 26, 28 -> 10 cells.
            assertEquals("$label: expected 10 cells with markers", 10, markerRows.size)

            val fourMarkerRow = markerRows.single { it.childCount == 4 }
            val expectedTop = fourMarkerRow.getChildAt(0).top
            val expectedBottom = fourMarkerRow.getChildAt(0).bottom
            val expectedSize = fourMarkerRow.getChildAt(0).height
            for (i in 0 until 4) {
                val child = fourMarkerRow.getChildAt(i)
                assertEquals("$label: marker $i top mismatch", expectedTop, child.top)
                assertEquals("$label: marker $i bottom mismatch", expectedBottom, child.bottom)
                assertEquals("$label: marker $i square width!=height", expectedSize, child.width)
            }

            // Group marker rows by their week row (screen Y within 4px) and assert identical vertical center
            // across solo-marker and multi-marker cells, and zero vertical clipping inside each cell.
            for (row in markerRows) {
                val cellBox = row.parent?.parent as? View ?: continue
                val cellTop = cellBox.topInRoot(root)
                val cellBottom = cellTop + cellBox.height
                for (child in row.children) {
                    val childTop = child.topInRoot(root)
                    val childBottom = childTop + child.height
                    assertTrue(
                        "$label: marker [$childTop..$childBottom] overflows cell [$cellTop..$cellBottom]",
                        childTop >= cellTop && childBottom <= cellBottom,
                    )
                }
            }

            // Week row 4 has 5 marker cells (days 23, 24, 25, 26, 28): all must share the exact vertical center.
            val centers = markerRows.map { row ->
                val child = row.getChildAt(0)
                child.topInRoot(root) + child.height / 2f
            }.sorted()
            // Last 5 marker cells belong to week row 4 (Nov 23, 24, 25, 26, 28).
            val week4Centers = centers.takeLast(5)
            val minCenter = week4Centers.minOrNull()!!
            val maxCenter = week4Centers.maxOrNull()!!
            assertTrue(
                "$label: week 4 marker centers drifted across cells: $week4Centers",
                maxCenter - minCenter <= 1f,
            )
        }
    }

    @Test
    fun renderedHolidayDotAlignsAtSameVerticalLevelWithoutAlteringMarkerSizes() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        for ((tag, wDp, hDp) in listOf(
            Triple("tight-compact", 238, 196),
            Triple("compact", 238, 201),
            Triple("regular", 320, 270),
        )) {
            val root = composeAndLayout(context, buildSnapshot(khmer = true), wDp, hDp)
            val bmp = Bitmap.createBitmap(root.measuredWidth, root.measuredHeight, Bitmap.Config.ARGB_8888)
            root.draw(Canvas(bmp))

            // Scan week row 4's marker band (lower third of row 4, excluding day numerals above):
            // Day 23 (col 1): holiday dot + observance square
            // Day 24 (col 2): holiday dot + holy triangle
            // Day 26 (col 4): personal star
            // Day 28 (col 6): observance square
            val markerRows = root.descendants.filterIsInstance<LinearLayout>().filter { row ->
                row.orientation == LinearLayout.HORIZONTAL &&
                    row.childCount in 1..4 &&
                    row.children.all { it is ImageView && it.height in 4..30 }
            }.toList()
            val row4Sample = markerRows.last()
            val bandTop = (row4Sample.getChildAt(0).topInRoot(root) - 2).coerceAtLeast(0)
            val bandBottom = (bandTop + row4Sample.getChildAt(0).height + 4).coerceAtMost(bmp.height - 1)

            fun measureShapeInBand(
                predicate: (r: Int, g: Int, b: Int) -> Boolean,
                xMin: Int = 0,
                xMax: Int = bmp.width - 1,
            ): Triple<Float, Int, Int> {
                var minY = Int.MAX_VALUE
                var maxY = Int.MIN_VALUE
                var minX = Int.MAX_VALUE
                var maxX = Int.MIN_VALUE
                for (y in bandTop..bandBottom) {
                    for (x in xMin..xMax) {
                        val c = bmp.getPixel(x, y)
                        val r = Color.red(c)
                        val g = Color.green(c)
                        val b = Color.blue(c)
                        if (predicate(r, g, b)) {
                            if (y < minY) minY = y
                            if (y > maxY) maxY = y
                            if (x < minX) minX = x
                            if (x > maxX) maxX = x
                        }
                    }
                }
                assertTrue("$tag: expected marker pixels in x=[$xMin..$xMax], y=[$bandTop..$bandBottom]", minY <= maxY)
                return Triple((minY + maxY) / 2f, maxX - minX + 1, maxY - minY + 1)
            }

            val colWidth = bmp.width / 7
            // Col 1 (day 23): pink holiday dot on left, lime observance square on right
            val (dotMid, dotW, dotH) = measureShapeInBand(
                predicate = { r, g, b -> r > 210 && g in 106..174 && b > 125 && r - g > 60 },
                xMin = colWidth * 1,
                xMax = colWidth * 2,
            )
            // Col 2 (day 24): amber holy-day triangle
            val (triMid, triW, triH) = measureShapeInBand(
                predicate = { r, g, b -> r > 210 && g > 145 && b < 125 && r - b > 100 },
                xMin = colWidth * 2,
                xMax = colWidth * 3,
            )
            // Col 4 (day 26): teal personal star
            val (starMid, starW, starH) = measureShapeInBand(
                predicate = { r, g, b -> r < 120 && g > 180 && b > 170 },
                xMin = colWidth * 4,
                xMax = colWidth * 5,
            )
            // Col 6 (day 28): solo lime observance square
            val (sqMid, sqW, sqH) = measureShapeInBand(
                predicate = { r, g, b -> r > 150 && g > 195 && b < 110 && g - b > 100 },
                xMin = colWidth * 6,
                xMax = bmp.width - 1,
            )

            assertTrue("$tag: holiday dot ($dotMid) not aligned with triangle ($triMid)", abs(dotMid - triMid) <= 1.0f)
            assertTrue("$tag: holiday dot ($dotMid) not aligned with square ($sqMid)", abs(dotMid - sqMid) <= 1.0f)
            assertTrue("$tag: holiday dot ($dotMid) not aligned with star ($starMid)", abs(dotMid - starMid) <= 1.0f)

            if (tag == "regular") {
                assertTrue("$tag: dot size ${dotW}x$dotH altered", dotW in 8..11 && dotH in 8..11)
                assertTrue("$tag: triangle size ${triW}x$triH altered", triW in 8..10 && triH in 7..10)
                assertTrue("$tag: square size ${sqW}x$sqH altered", sqW in 6..9 && sqH in 6..9)
                assertTrue("$tag: star size ${starW}x$starH altered", starW in 9..12 && starH in 8..12)
            } else {
                assertTrue("$tag: dot size ${dotW}x$dotH altered", dotW in 7..9 && dotH in 7..9)
                assertTrue("$tag: triangle size ${triW}x$triH altered", triW in 7..9 && triH in 6..9)
                assertTrue("$tag: square size ${sqW}x$sqH altered", sqW in 5..7 && sqH in 5..7)
                assertTrue("$tag: star size ${starW}x$starH altered", starW in 7..10 && starH in 6..9)
            }
        }
    }
}
