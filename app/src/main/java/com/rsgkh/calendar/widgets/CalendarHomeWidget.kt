// SPDX-License-Identifier: Apache-2.0
package com.rsgkh.calendar.widgets

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.annotation.Keep
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.currentState
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.rsgkh.calendar.MainActivity
import com.rsgkh.calendar.R
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

abstract class CalendarHomeWidget : GlanceAppWidget(errorUiLayout = R.layout.widget_error) {
    // Android 12+ reports actual supported sizes. Do not assume every launcher's four cells
    // have the same dp dimensions. The content budget switches compact/expanded sections.
    override val sizeMode: SizeMode = SizeMode.Exact
    override val stateDefinition = PreferencesGlanceStateDefinition

    final override suspend fun provideGlance(context: Context, id: GlanceId) {
        val appId = GlanceAppWidgetManager(context).getAppWidgetId(id)
        val initial = readSnapshot(context, appId)
        withContext(Dispatchers.IO) { WidgetUpdater.ensureScheduled(context) }
        provideContent {
            val revision = currentState<Preferences>()[WidgetUpdater.REVISION] ?: "initial"
            // update() does not restart an already-running Glance session. Observing this
            // revision is essential for repeated edits while that session remains alive.
            val snapshot by produceState(initialValue = initial, key1 = revision) {
                value = readSnapshot(context, appId)
            }
            snapshot?.let { Content(it, appId) } ?: Column(
                GlanceModifier.fillMaxSize().padding(16.dp).clickable(
                    actionStartActivity(Intent(context, MainActivity::class.java)),
                ),
            ) {
                Text(context.getString(R.string.widget_load_failed), style = TextStyle(fontSize = 15.sp))
            }
        }
    }

    @Composable
    internal abstract fun Content(snapshot: WidgetSnapshot, id: Int)

    private suspend fun readSnapshot(context: Context, id: Int): WidgetSnapshot? = try {
        withContext(Dispatchers.IO) { WidgetDataSource.load(context, id) }
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        Log.w("CalendarWidgets", "Widget snapshot unavailable: ${error.javaClass.simpleName}")
        null
    }
}

@Keep
class FocusWidget : CalendarHomeWidget() {
    @Composable
    override fun Content(snapshot: WidgetSnapshot, id: Int) = FocusWidgetContent(snapshot, id)
}

@Keep
class ProductivityWidget : CalendarHomeWidget() {
    @Composable
    override fun Content(snapshot: WidgetSnapshot, id: Int) = ProductivityWidgetContent(snapshot, id)
}

@Keep
class MonthWidget : CalendarHomeWidget() {
    @Composable
    override fun Content(snapshot: WidgetSnapshot, id: Int) = MonthWidgetContent(snapshot, id)
}
