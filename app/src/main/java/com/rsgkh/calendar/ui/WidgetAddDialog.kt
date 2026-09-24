// SPDX-License-Identifier: Apache-2.0
package com.rsgkh.calendar.ui

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.rsgkh.calendar.R
import com.rsgkh.calendar.widgets.*
import kotlinx.coroutines.launch

private data class WidgetChoice(
    val receiver: Class<out AppWidgetProvider>,
    @StringRes val name: Int,
    @DrawableRes val preview: Int,
    val columns: Int,
    val rows: Int,
)

private val widgetChoices = listOf(
    WidgetChoice(GlanceWidgetReceiver::class.java, R.string.widget_glance_name, R.drawable.widget_glance, 4, 1),
    WidgetChoice(PlannerWidgetReceiver::class.java, R.string.widget_planner_name, R.drawable.widget_planner, 4, 2),
    WidgetChoice(MonthWidgetReceiver::class.java, R.string.widget_month_name, R.drawable.widget_month, 4, 3),
    WidgetChoice(FocusWidgetReceiver::class.java, R.string.widget_focus_name, R.drawable.widget_focus, 4, 2),
    WidgetChoice(ProductivityWidgetReceiver::class.java, R.string.widget_productivity_name,
        R.drawable.widget_productivity, 4, 2),
)

// Reverse neutral tones for dark mode while retaining each preview's accent and event colors.
// Alpha is untouched, so the screenshots' rounded transparent corners remain transparent.
private val darkPreviewFilter = ColorFilter.colorMatrix(ColorMatrix(floatArrayOf(
    0.30236f, -1.00128f, -0.10108f, 0f, 229.5f,
    -0.29764f, -0.40128f, -0.10108f, 0f, 229.5f,
    -0.29764f, -1.00128f, 0.49892f, 0f, 229.5f,
    0f, 0f, 0f, 1f, 0f,
)))

@Composable
internal fun WidgetAddDialog(khmer: Boolean, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val strings = remember(context, khmer) { WidgetStrings(context, khmer) }
    val manager = remember(context) { AppWidgetManager.getInstance(context) }
    val canPin = remember(manager) { runCatching { manager.isRequestPinAppWidgetSupported }.getOrDefault(false) }
    val pagerState = rememberPagerState(pageCount = { widgetChoices.size })
    val scope = rememberCoroutineScope()
    val screenHeight = LocalConfiguration.current.screenHeightDp
    val previewHeight = (screenHeight * 0.27f).coerceIn(100f, 260f).dp
    val dark = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    CalendarBasicAlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).widthIn(max = 560.dp).fillMaxWidth(),
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface) {
            Column(Modifier.fillMaxWidth().heightIn(max = (screenHeight * 0.88f).dp)
                .verticalScroll(rememberScrollState()).padding(20.dp).testTag("widget-chooser"),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(strings(R.string.widget_add_title), style = MaterialTheme.typography.titleLarge)

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxWidth().height(previewHeight + 150.dp).testTag("widget-pages"),
                    pageSpacing = 12.dp,
                ) { page ->
                    val choice = widgetChoices[page]
                    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center) {
                        val preview = painterResource(choice.preview)
                        val ratio = preview.intrinsicSize.width / preview.intrinsicSize.height
                        BoxWithConstraints(Modifier.fillMaxWidth()) {
                            val imageHeight = minOf(previewHeight, (maxWidth - 24.dp) / ratio)
                            Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                                Image(painter = preview, contentDescription = null,
                                    modifier = Modifier.fillMaxWidth().padding(12.dp).height(imageHeight),
                                    contentScale = ContentScale.Fit,
                                    colorFilter = if (dark) darkPreviewFilter else null)
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Text(strings(choice.name).substringAfter(" · "), style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(8.dp))
                        Text("${choice.columns} × ${choice.rows}", style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically) {
                    TextButton(enabled = pagerState.currentPage > 0,
                        onClick = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) } },
                        modifier = Modifier.semantics { contentDescription = if (khmer) "វីដជិតមុន" else "Previous widget" }) {
                        Text("‹", style = MaterialTheme.typography.headlineSmall)
                    }
                    widgetChoices.indices.forEach { index ->
                        Box(Modifier.padding(horizontal = 4.dp).size(7.dp).background(
                            if (index == pagerState.currentPage) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outlineVariant, CircleShape))
                    }
                    TextButton(enabled = pagerState.currentPage < widgetChoices.lastIndex,
                        onClick = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) } },
                        modifier = Modifier.semantics { contentDescription = if (khmer) "វីដជិតបន្ទាប់" else "Next widget" }) {
                        Text("›", style = MaterialTheme.typography.headlineSmall)
                    }
                }

                if (!canPin) {
                    Text(strings(R.string.widget_add_unsupported), style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onDismiss) { Text(strings(R.string.widget_add_close)) }
                    Spacer(Modifier.width(16.dp))
                    Button(enabled = canPin, onClick = {
                        val choice = widgetChoices[pagerState.currentPage]
                        val sent = runCatching {
                            manager.requestPinAppWidget(ComponentName(context, choice.receiver), null, null)
                        }.getOrDefault(false)
                        if (sent) onDismiss() else Toast.makeText(context,
                            strings(R.string.widget_add_failed), Toast.LENGTH_SHORT).show()
                    }, modifier = Modifier.testTag("add-selected-widget")) {
                        Text(strings(R.string.widget_add_button))
                    }
                }
            }
        }
    }
}
