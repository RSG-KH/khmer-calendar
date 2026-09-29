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
import com.rsgkh.calendar.i18n.CalendarWords
import com.rsgkh.calendar.i18n.L
import java.time.LocalDate
import java.time.LocalTime

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
    kind: AstrologyDetail,
    horoscope: WesternBig3Signs?,
    pillars: List<GanzhiColumn>,
    khmer: Boolean,
    date: LocalDate? = null,
    time: LocalTime? = null,
): String {
    return when (kind) {
        AstrologyDetail.BIG_3 -> {
            if (khmer) {
                val targetLabelKm = if (horoscope?.rising != null) "នៃធាតុសំខាន់ទាំង ៣ (Big 3)" else "នៃព្រះអាទិត្យ និងព្រះចន្ទ (Sun and Moon)"
                val header = if (date != null) {
                    val dayStr = CalendarWords.number(date.dayOfMonth, true)
                    val monthStr = CalendarWords.month(date.monthValue, true)
                    val yearStr = CalendarWords.number(date.year, true)
                    val timeStr = if (time != null) {
                        val hourKm = time.hour.toString().padStart(2, '0').map { ('\u17E0' + (it - '0')) }.joinToString("")
                        val minuteKm = time.minute.toString().padStart(2, '0').map { ('\u17E0' + (it - '0')) }.joinToString("")
                        " ម៉ោង $hourKm:$minuteKm"
                    } else ""
                    "ចូរពន្យល់ពីអត្ថន័យតាមក្បួនហោរាសាស្ត្រលោកខាងលិច $targetLabelKm សម្រាប់ថ្ងៃទី $dayStr $monthStr ឆ្នាំ $yearStr$timeStr (ទីតាំងមិនបានបញ្ជាក់)៖"
                } else {
                    "ចូរពន្យល់ពីអត្ថន័យតាមក្បួនហោរាសាស្ត្រលោកខាងលិច $targetLabelKm៖"
                }
                val items = listOfNotNull(
                    horoscope?.sun?.let { "ព្រះអាទិត្យ (Sun)៖ ${it.englishName}" },
                    horoscope?.moon?.let { "ព្រះចន្ទ (Moon)៖ ${it.englishName}" },
                    horoscope?.rising?.let { "រះ (Rising)៖ ${it.englishName}" },
                )
                val lines = items.mapIndexed { index, text ->
                    val suffix = if (index == items.size - 1) " ។" else ""
                    "- $text$suffix"
                }
                if (lines.isEmpty()) header else "$header\n${lines.joinToString("\n")}"
            } else {
                val targetLabelEn = if (horoscope?.rising != null) "the Big 3 (Sun, Moon, and Rising)" else "the Sun and Moon"
                val header = if (date != null) {
                    val monthStr = CalendarWords.month(date.monthValue, false)
                    val timeStr = if (time != null) {
                        val hourStr = time.hour.toString().padStart(2, '0')
                        val minuteStr = time.minute.toString().padStart(2, '0')
                        ", at $hourStr:$minuteStr"
                    } else ""
                    "Please explain the traditional astrological meanings of $targetLabelEn for $monthStr ${date.dayOfMonth}, ${date.year}$timeStr (unspecified location):"
                } else {
                    "Please explain the traditional astrological meanings of $targetLabelEn:"
                }
                val items = listOfNotNull(
                    horoscope?.sun?.let { "Sun: ${it.englishName}" },
                    horoscope?.moon?.let { "Moon: ${it.englishName}" },
                    horoscope?.rising?.let { "Rising: ${it.englishName}" },
                )
                val lines = items.mapIndexed { index, text ->
                    val suffix = if (index == items.size - 1) "." else ";"
                    "- $text$suffix"
                }
                if (lines.isEmpty()) header else "$header\n${lines.joinToString("\n")}"
            }
        }
        AstrologyDetail.GANZHI -> {
            val available = pillars.filter { it.pillar != null }
            if (khmer) {
                val header = if (date != null) {
                    val dayStr = CalendarWords.number(date.dayOfMonth, true)
                    val monthStr = CalendarWords.month(date.monthValue, true)
                    val yearStr = CalendarWords.number(date.year, true)
                    val timeStr = if (time != null) {
                        val hourStr = CalendarWords.number(time.hour, true)
                        val minuteStr = CalendarWords.number(time.minute, true)
                        " ម៉ោង $hourStr និង $minuteStr នាទី"
                    } else ""
                    "ចូរពន្យល់អត្ថន័យតាមហោរាសាស្ត្រចិន(干支) ដែលត្រូវនឹងថ្ងៃទី $dayStr $monthStr ឆ្នាំ $yearStr$timeStr៖"
                } else {
                    "ចូរពន្យល់អត្ថន័យតាមហោរាសាស្ត្រចិន(干支)៖"
                }
                val lines = available.mapIndexed { index, column ->
                    val pillar = column.pillar!!
                    val animal = pillar.branch.ganzhiAnimalLabel(true, false)
                    val clash = pillar.clashBranch.ganzhiAnimalLabel(true, false)
                    val suffix = if (index == available.size - 1) "។" else ""
                    "- ${column.label}៖ ${pillar.nameZh} $animal (ឆុង៖ $clash)$suffix"
                }
                if (lines.isEmpty()) header else "$header\n${lines.joinToString("\n")}"
            } else {
                val header = if (date != null) {
                    val monthStr = CalendarWords.month(date.monthValue, false)
                    val timeStr = if (time != null) {
                        val hourStr = time.hour.toString().padStart(2, '0')
                        val minuteStr = time.minute.toString().padStart(2, '0')
                        ", at $hourStr:$minuteStr"
                    } else ""
                    "Please explain the traditional astrological meanings of the Chinese Ganzhi (干支) for $monthStr ${date.dayOfMonth}, ${date.year}$timeStr:"
                } else {
                    "Please explain the traditional astrological meanings of the Chinese Ganzhi (干支):"
                }
                val lines = available.mapIndexed { index, column ->
                    val pillar = column.pillar!!
                    val animal = pillar.branch.ganzhiAnimalLabel(false, false)
                    val clash = pillar.clashBranch.ganzhiAnimalLabel(false, false)
                    val suffix = if (index == available.size - 1) "." else ";"
                    "- ${column.label}: ${pillar.nameZh} $animal (Clash: $clash)$suffix"
                }
                if (lines.isEmpty()) header else "$header\n${lines.joinToString("\n")}"
            }
        }
    }
}

@Composable internal fun AstrologyDetailsDialog(
    kind: AstrologyDetail, info: KhmerDateDetails, horoscope: WesternBig3Signs?,
    pillars: List<GanzhiColumn>, khmer: Boolean, useWesternEmoji: Boolean,
    useGanzhiEmoji: Boolean, symbolSlot: Dp, time: LocalTime? = null, onDismiss: () -> Unit,
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
                            launchAiSearch(context, astrologySearchQuery(kind, horoscope, pillars, khmer, info.date, time), khmer)
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
