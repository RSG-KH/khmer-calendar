// SPDX-License-Identifier: Apache-2.0
package com.rsgkh.calendar.widgets

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import com.rsgkh.calendar.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PlannerWidgetLayoutTest {
    @Test fun dateBadgeWrapsShortTextAndUsesAvailableHeaderSpaceForLongText() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val root = LayoutInflater.from(context).inflate(R.layout.widget_planner, null)
        val period = root.findViewById<TextView>(R.id.planner_period)
        val slot = period.parent as FrameLayout
        val timezone = root.findViewById<TextView>(R.id.planner_timezone)
        timezone.text = "🌐 Local"

        period.text = "Sep 2026"
        measure(root, context, 400)
        assertTrue(period.width < slot.width)

        period.text = "08/08/2026 - 10/03/2026"
        measure(root, context, 400)
        assertTrue(period.width < slot.width)
        assertTrue(period.width > 0)
        assertEquals(0, period.layout.getEllipsisCount(0))
        assertTrue(timezone.width > 0)

        measure(root, context, 280)
        assertTrue(period.width <= slot.width)
        assertTrue(timezone.width > 0)
    }

    private fun measure(root: View, context: Context, widthDp: Int) {
        val density = context.resources.displayMetrics.density
        root.measure(View.MeasureSpec.makeMeasureSpec((widthDp * density).toInt(), View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec((200 * density).toInt(), View.MeasureSpec.EXACTLY))
        root.layout(0, 0, root.measuredWidth, root.measuredHeight)
    }
}
