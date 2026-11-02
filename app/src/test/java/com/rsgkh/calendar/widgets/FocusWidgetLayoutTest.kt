// SPDX-License-Identifier: Apache-2.0
package com.rsgkh.calendar.widgets

import android.content.Context
import android.content.res.Configuration
import android.view.View
import android.widget.FrameLayout
import android.widget.ListView
import android.widget.TextView
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.core.view.descendants
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.compose
import androidx.glance.appwidget.provideContent
import androidx.test.core.app.ApplicationProvider
import com.rsgkh.calendar.data.AppSettings
import com.rsgkh.calendar.data.EventKind
import com.rsgkh.calendar.data.FontScale
import com.rsgkh.calendar.domain.KhmerDateDetails
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class FocusWidgetLayoutTest {
    @Test fun resizingAndFontScalingKeepTodaysEventsReadable() = runBlocking {
        val baseContext = ApplicationProvider.getApplicationContext<Context>()
        val date = LocalDate.of(2026, 10, 10)
        fun day(offset: Long) = WidgetDay(date.plusDays(offset),
            listOf(WidgetItem("Meeting", "09:00", EventKind.CUSTOM, "meeting:$offset")), 1)
        val baseSnapshot = WidgetSnapshot(AppSettings(), date, KhmerDateDetails.fromGregorian(date),
            day(-1), day(0), day(1), null)
        data class Case(val height: Int, val scale: FontScale, val systemScale: Float, val listCount: Int)
        val cases = listOf(
            Case(140, FontScale.PERCENT_100, 1f, 3),
            Case(190, FontScale.PERCENT_100, 1f, 3),
            Case(220, FontScale.PERCENT_100, 1f, 3),
            Case(240, FontScale.PERCENT_100, 1f, 3),
            Case(240, FontScale.PERCENT_150, 1f, 3),
            Case(400, FontScale.PERCENT_200, 1f, 3),
            Case(400, FontScale.PERCENT_150, 1.3f, 3),
        )
        for (khmer in listOf(false, true)) for (case in cases) {
            val context = baseContext.createConfigurationContext(Configuration(baseContext.resources.configuration).apply {
                fontScale = case.systemScale
            })
            val snapshot = baseSnapshot.copy(settings = baseSnapshot.settings.copy(khmer = khmer, widgetFontScale = case.scale))
            val widget = object : GlanceAppWidget() {
                override suspend fun provideGlance(context: Context, id: GlanceId) {
                    provideContent { FocusWidgetContent(snapshot, 0) }
                }
            }
            val views = withTimeout(10_000) { widget.compose(context, size = DpSize(280.dp, case.height.dp)) }
            val root = views.apply(context, FrameLayout(context)) as android.view.ViewGroup
            val density = context.resources.displayMetrics.density
            root.measure(View.MeasureSpec.makeMeasureSpec((280 * density).toInt(), View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec((case.height * density).toInt(), View.MeasureSpec.EXACTLY))
            root.layout(0, 0, root.measuredWidth, root.measuredHeight)
            val lists = root.descendants.filterIsInstance<ListView>().toList()
            val label = "$case, khmer=$khmer"
            assertEquals(label, case.listCount, lists.size)
            val minimumRow = 30 * case.scale.multiplier * case.systemScale * density
            assertTrue("$label: current-day viewport is only ${lists.first().height / density}dp", lists.first().height >= minimumRow)
            assertTrue("$label: adjacent viewport heights ${lists.drop(1).map { it.height / density }}", lists.drop(1).all { it.height >= 20 * density })
        }
    }

    @Test fun focusAndProductivityWidgetsRenderExpectedMoonPhaseEmoji() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val cases = listOf(
            LocalDate.of(2026, 10, 12) to "🌒", // 1 កើត (waxing crescent)
            LocalDate.of(2026, 10, 19) to "🌓", // 8 កើត (first quarter)
            LocalDate.of(2026, 10, 31) to "🌖", // 5 រោច (waning gibbous)
            LocalDate.of(2026, 11, 3) to "🌗",  // 8 រោច (last quarter)
            LocalDate.of(2026, 11, 9) to "🌑",  // 14 រោច of 29-day Assoch (new moon)
        )
        for ((date, expectedEmoji) in cases) {
            fun day(offset: Long) = WidgetDay(date.plusDays(offset), emptyList(), 0)
            val snapshot = WidgetSnapshot(AppSettings(), date, KhmerDateDetails.fromGregorian(date),
                day(-1), day(0), day(1), null)
            for ((name, render) in listOf<Pair<String, @androidx.compose.runtime.Composable () -> Unit>>(
                "Focus" to { FocusWidgetContent(snapshot, 0) },
                "Productivity" to { ProductivityWidgetContent(snapshot, 0) },
            )) {
                val widget = object : GlanceAppWidget() {
                    override suspend fun provideGlance(context: Context, id: GlanceId) {
                        provideContent { render() }
                    }
                }
                val views = withTimeout(10_000) { widget.compose(context, size = DpSize(360.dp, 180.dp)) }
                val root = views.apply(context, FrameLayout(context)) as android.view.ViewGroup
                val texts = root.descendants.filterIsInstance<TextView>().map { it.text.toString() }.toList()
                assertTrue("$name on $date expected $expectedEmoji in $texts",
                    texts.any { it.contains(expectedEmoji) })
            }
        }
    }
}
