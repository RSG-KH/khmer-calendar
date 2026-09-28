// Copyright (c) 2026 RSG-KH | Apache-2.0 License
package com.rsgkh.calendar.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.rsgkh.calendar.domain.KhmerDateDetails
import com.rsgkh.calendar.domain.WesternBig3Signs
import com.rsgkh.calendar.domain.ZodiacSign
import com.rsgkh.calendar.domain.ganzhiAnimalLabel
import com.rsgkh.calendar.engine.western.WesternZodiacSign
import com.rsgkh.calendar.i18n.L

internal enum class AstrologyDetail(val titleKey: String) {
    BIG_3("ui.zodiac_big3_title"), GANZHI("ui.chinese_ganzhi_title")
}

/** Match the engine's calculated signs to the app's existing names/elements/rulers. */
internal fun WesternZodiacSign.displaySign(): ZodiacSign = ZodiacSign.valueOf(name)

internal fun astrologyBackground(kind: AstrologyDetail, horoscope: WesternBig3Signs?, pillars: List<GanzhiColumn>): Int? =
    when (kind) {
        AstrologyDetail.BIG_3 -> horoscope?.sun?.displaySign()?.let(::westernZodiacDrawable)
        AstrologyDetail.GANZHI -> pillars.firstOrNull { it.key == "year" }?.pillar?.branch?.index
            ?.let { zodiacDrawable(it, compact = true) }
    }

internal fun astrologySearchQuery(
    kind: AstrologyDetail, horoscope: WesternBig3Signs?, pillars: List<GanzhiColumn>, khmer: Boolean,
): String {
    val details = when (kind) {
        AstrologyDetail.BIG_3 -> listOf(
            "ui.western_sun" to horoscope?.sun,
            "ui.western_moon" to horoscope?.moon,
            "ui.western_rising_sign" to horoscope?.rising,
        ).mapNotNull { (key, sign) -> sign?.let { "${L.text(key, khmer)}: ${it.englishName}" } }
        AstrologyDetail.GANZHI -> pillars.mapNotNull { column -> column.pillar?.let {
            "${column.label}: ${it.nameZh} ${it.branch.ganzhiAnimalLabel(khmer, false)} " +
                "(${L.text("ui.ganzhi_clash", khmer)}: ${it.clashBranch.ganzhiAnimalLabel(khmer, false)})"
        } }
    }.joinToString("; ")
    return L.text("ui.astrology_ai_query", khmer,
        "details" to "${L.text(kind.titleKey, khmer)}: $details")
}

@Composable internal fun AstrologyDetailsDialog(
    kind: AstrologyDetail, info: KhmerDateDetails, horoscope: WesternBig3Signs?,
    pillars: List<GanzhiColumn>, khmer: Boolean, useWesternEmoji: Boolean,
    useGanzhiEmoji: Boolean, symbolSlot: Dp, onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val background = astrologyBackground(kind, horoscope, pillars)
    CalendarBasicAlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.padding(horizontal = 20.dp).widthIn(max = 520.dp).fillMaxWidth(),
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp) {
            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).testTag("astrology-details-dialog")) {
                background?.let { drawable ->
                    // Fit all of the artwork within the content-sized popup, clear of its rounded corners.
                    Box(Modifier.matchParentSize().padding(12.dp)) {
                        Image(painterResource(drawable), contentDescription = null,
                            modifier = Modifier.align(Alignment.BottomEnd).fillMaxWidth(.60f).fillMaxHeight()
                                .testTag("astrology-watermark"),
                            alignment = Alignment.BottomEnd, contentScale = ContentScale.Fit,
                            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary),
                            alpha = zodiacAlpha())
                    }
                }
                Column(Modifier.fillMaxWidth().padding(start = 24.dp, top = 16.dp, end = 24.dp, bottom = 12.dp)) {
                    Text(L.text(kind.titleKey, khmer), Modifier.testTag("astrology-details-title"),
                        color = MaterialTheme.colorScheme.primary, fontSize = 16.readableSp,
                        lineHeight = 24.readableSp, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(12.dp))
                    val scroll = rememberScrollState()
                    Column(Modifier.weight(1f, fill = false).verticalScrollbar(scroll).verticalScroll(scroll)
                        .testTag("astrology-details-content")) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Spacer(Modifier.height(10.dp))
                        when (kind) {
                            AstrologyDetail.BIG_3 -> {
                                WesternZodiacTable(info, horoscope, khmer, useWesternEmoji, symbolSlot)
                                Spacer(Modifier.height(18.dp))
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    listOf("sun" to horoscope?.sun, "moon" to horoscope?.moon, "rising" to horoscope?.rising)
                                        .forEach { (key, sign) ->
                                            val label = sign?.displaySign()?.label ?: "—"
                                            val role = L.text(if (key == "rising") "ui.western_rising_sign" else "ui.western_$key", khmer)
                                            Text(label, Modifier.testTag("big3-detail-$key")
                                                .semantics { contentDescription = "$role: $label" },
                                                fontSize = 12.readableSp, lineHeight = 20.readableSp)
                                        }
                                }
                            }
                            AstrologyDetail.GANZHI -> GanzhiTable(info, pillars, khmer, useGanzhiEmoji, symbolSlot)
                        }
                        Spacer(Modifier.height(14.dp))
                        Text(
                            L.text("astrology.engine_calculations", khmer, "version" to CalendarEngineVersion),
                            fontSize = 10.readableSp,
                            lineHeight = 16.readableSp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.testTag("astrology-engine-calculations")
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    FlowRow(Modifier.fillMaxWidth().testTag("astrology-details-actions"),
                        horizontalArrangement = Arrangement.SpaceBetween, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        AskAiButton(khmer, Modifier.testTag("astrology-ask-ai")) {
                            launchAiSearch(context, astrologySearchQuery(kind, horoscope, pillars, khmer), khmer)
                        }
                        TextButton(onClick = onDismiss, modifier = Modifier.testTag("astrology-close")) {
                            Text(L.text("ui.close.7df7dc", khmer))
                        }
                    }
                }
            }
        }
    }
}
