// Copyright (c) 2026 RSG-KH | Apache-2.0 License
@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.rsgkh.calendar.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.os.Build
import android.widget.Toast
import com.rsgkh.calendar.i18n.L
import com.rsgkh.calendar.i18n.CalendarWords

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.activity.compose.BackHandler
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import android.content.res.Configuration
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalContext
import androidx.browser.customtabs.CustomTabsIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.net.toUri
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.*
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.annotation.DrawableRes
import androidx.compose.ui.window.DialogProperties
import com.rsgkh.calendar.BuildConfig
import com.rsgkh.calendar.R
import com.rsgkh.calendar.data.*
import com.rsgkh.calendar.domain.*
import com.rsgkh.calendar.engine.ChineseZodiacCalculator
import com.rsgkh.calendar.engine.GanzhiPillar
import com.rsgkh.calendar.widgets.WidgetUpdater
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.abs

private val CardShape = RoundedCornerShape(24.dp)
private val DetailSymbolSlot = 24.dp
private val DetailSymbolGap = 6.dp
internal fun timeZoneLabel(choice: TodayTimeZone, k: Boolean) = if (choice == TodayTimeZone.LOCAL)
    L.text("ui.local_time.541b44", k) else L.text("ui.cambodia_time_utc_7.6b9f2d", k)
internal fun timeSelectionZoneLabel(choice: TodayTimeZone, k: Boolean) = if (choice == TodayTimeZone.LOCAL)
    "${L.text("ui.local_time.541b44", k)} (${choice.offsetLabel()})" else L.text("ui.cambodia_time_utc_7.6b9f2d", k)
private fun number(n: Int, k: Boolean) = if (k) khmerNumber(n) else n.toString()
private fun searchText(text: String) = text.filterNot { it.isWhitespace() || it == '\u200B' }.lowercase(Locale.ROOT)
private fun monthName(month: YearMonth, k: Boolean, short: Boolean = k) = CalendarWords.month(month.monthValue, k, short = short)
private fun dateLabel(date: LocalDate, k: Boolean) = CalendarWords.date(date, k)
private fun kindLabel(kind: EventKind, k: Boolean) = when (kind) {
    EventKind.HOLIDAY -> L.text("ui.public_holiday.5bd66a", k)
    EventKind.OBSERVANCE -> L.text("ui.observance.5b9a87", k)
    EventKind.HOLY_DAY -> L.text("ui.buddhist_holy_day.829195", k)
    EventKind.CUSTOM -> L.text("ui.custom.917053", k)
}
private val CustomEventRed = Color(0xFFE53935)
private val CustomEventRedDark = Color(0xFFFF5252)

@Composable private fun eventColor(kind: EventKind): Color = when (kind) {
    EventKind.HOLIDAY -> MaterialTheme.colorScheme.tertiary
    EventKind.OBSERVANCE -> MaterialTheme.colorScheme.primary
    EventKind.HOLY_DAY -> MaterialTheme.colorScheme.secondary
    EventKind.CUSTOM -> if (MaterialTheme.colorScheme.surface.luminance() > .5f) CustomEventRed else CustomEventRedDark
}

@DrawableRes
private fun holyDayLotusDrawable(lunar: LunarDate): Int =
    // Day 8 and its shaving day use the bud; phase-end holy days and their eves use the blossom.
    if (lunar.day <= 8) R.drawable.holy_day_lotus else R.drawable.holy_day_lotus_blossom

@DrawableRes
private fun zodiacDrawable(animalYear: Int, compact: Boolean = false): Int = when (Math.floorMod(animalYear, 12)) {
    0 -> if (compact) R.drawable.zodiac_rat_400 else R.drawable.zodiac_rat
    1 -> if (compact) R.drawable.zodiac_ox_400 else R.drawable.zodiac_ox
    2 -> if (compact) R.drawable.zodiac_tiger_400 else R.drawable.zodiac_tiger
    3 -> if (compact) R.drawable.zodiac_rabbit_400 else R.drawable.zodiac_rabbit
    4 -> if (compact) R.drawable.zodiac_dragon_400 else R.drawable.zodiac_dragon
    5 -> if (compact) R.drawable.zodiac_snake_400 else R.drawable.zodiac_snake
    6 -> if (compact) R.drawable.zodiac_horse_400 else R.drawable.zodiac_horse
    7 -> if (compact) R.drawable.zodiac_goat_400 else R.drawable.zodiac_goat
    8 -> if (compact) R.drawable.zodiac_monkey_400 else R.drawable.zodiac_monkey
    9 -> if (compact) R.drawable.zodiac_rooster_400 else R.drawable.zodiac_rooster
    10 -> if (compact) R.drawable.zodiac_dog_400 else R.drawable.zodiac_dog
    11 -> if (compact) R.drawable.zodiac_pig_400 else R.drawable.zodiac_pig
    else -> if (compact) R.drawable.zodiac_rat_400 else R.drawable.zodiac_rat
}

@DrawableRes
private fun westernZodiacDrawable(sign: ZodiacSign): Int = when (sign) {
    ZodiacSign.ARIES -> R.drawable.western_zodiac_aries
    ZodiacSign.TAURUS -> R.drawable.western_zodiac_taurus
    ZodiacSign.GEMINI -> R.drawable.western_zodiac_gemini
    ZodiacSign.CANCER -> R.drawable.western_zodiac_cancer
    ZodiacSign.LEO -> R.drawable.western_zodiac_leo
    ZodiacSign.VIRGO -> R.drawable.western_zodiac_virgo
    ZodiacSign.LIBRA -> R.drawable.western_zodiac_libra
    ZodiacSign.SCORPIO -> R.drawable.western_zodiac_scorpio
    ZodiacSign.SAGITTARIUS -> R.drawable.western_zodiac_sagittarius
    ZodiacSign.CAPRICORN -> R.drawable.western_zodiac_capricorn
    ZodiacSign.AQUARIUS -> R.drawable.western_zodiac_aquarius
    ZodiacSign.PISCES -> R.drawable.western_zodiac_pisces
}

@Composable
private fun zodiacAlpha(): Float = if (MaterialTheme.colorScheme.surface.luminance() < 0.5f) 0.03f else 0.05f

@Composable
private fun BoxScope.DetailsZodiacBackground(info: KhmerDateDetails, showWesternZodiac: Boolean = true) {
    Image(
        painter = painterResource(zodiacDrawable(info.animalYear, compact = true)),
        contentDescription = null,
        modifier = Modifier.align(Alignment.BottomEnd).fillMaxWidth(0.60f).aspectRatio(1f),
        contentScale = ContentScale.Fit,
        colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary),
        alpha = zodiacAlpha(),
    )
    if (showWesternZodiac) {
        Image(
            painter = painterResource(westernZodiacDrawable(info.zodiac)),
            contentDescription = null,
            modifier = Modifier.align(Alignment.BottomStart).padding(start = 20.dp, bottom = 20.dp)
                .fillMaxWidth(0.20f).aspectRatio(1f),
            contentScale = ContentScale.Fit,
            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary),
            alpha = zodiacAlpha(),
        )
    }
}

@Composable
fun CalendarApp(settings: AppSettings, today: LocalDate,
    customEvents: List<CustomEvent> = emptyList(), onSaveCustom: (CustomEvent) -> Unit = {}, onDeleteCustom: (String) -> Unit = {},
    notificationAccess: NotificationAccess = NotificationAccess(), onOpenNotificationSettings: () -> Unit = {}, onAllowExact: () -> Unit = {},
    openDateRequest: Pair<LocalDate, Long>? = null,
    openWidgetEventRequest: com.rsgkh.calendar.widgets.WidgetEventRequest? = null,
    onSettings: (AppSettings) -> Unit) {
    CalendarTheme(settings) {
        var page by rememberSaveable { mutableIntStateOf(0) }
        var selectedText by rememberSaveable { mutableStateOf(today.coerceIn(KhmerCalendar.minDate, KhmerCalendar.maxDate).toString()) }
        val selected = LocalDate.parse(selectedText)
        var monthText by rememberSaveable { mutableStateOf(YearMonth.from(selected).toString()) }
        val month = YearMonth.parse(monthText)
        var eventYear by rememberSaveable { mutableIntStateOf(selected.year) }
        var jump by rememberSaveable { mutableStateOf(false) }
        var detail by remember { mutableStateOf<CalendarEvent?>(null) }
        var dateDetailText by rememberSaveable { mutableStateOf<String?>(null) }
        var editingId by rememberSaveable { mutableStateOf<String?>(null) }
        var creating by rememberSaveable { mutableStateOf(false) }
        var customFocus by rememberSaveable { mutableIntStateOf(0) }
        val displayZone = settings.todayTimeZone.zone()
        val allCustom = remember(customEvents, displayZone, month.year, eventYear, selected.year, dateDetailText) {
            val years = setOf(month.year, eventYear, selected.year, dateDetailText?.let { LocalDate.parse(it).year } ?: selected.year)
            years.flatMap { year -> customEvents.flatMap { it.occurrences(LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31), displayZone) } }
        }
        LaunchedEffect(openDateRequest, openWidgetEventRequest) {
            openDateRequest?.first?.let { date ->
                if (date in KhmerCalendar.minDate..KhmerCalendar.maxDate) {
                    page = 0; jump = false; creating = false; editingId = null; detail = null
                    selectedText = date.toString(); monthText = YearMonth.from(date).toString(); dateDetailText = date.toString()
                    val request = openWidgetEventRequest?.takeIf {
                        it.date == date && it.nonce == openDateRequest?.second
                    }
                    if (request != null) {
                        detail = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
                            runCatching {
                                com.rsgkh.calendar.widgets.WidgetNavigation.resolve(request, customEvents, displayZone)
                            }.getOrNull()
                        }?.takeIf { settings.showObservances || it.kind != EventKind.OBSERVANCE }
                    }
                }
            }
        }
        val k = settings.khmer
        BackHandler(enabled = jump || creating || editingId != null) { jump = false; creating = false; editingId = null }
        fun navigate(target: YearMonth) {
            if (target.year in 1800..2200) {
                monthText = target.toString()
                selectedText = target.atDay(1).toString()
            }
        }
        val configuration = LocalConfiguration.current
        val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        val isTablet = configuration.smallestScreenWidthDp >= 600
        val navigationScale = when (settings.fontScale) {
            FontScale.PERCENT_130 -> 1.10f
            FontScale.PERCENT_140 -> 1.15f
            FontScale.PERCENT_150 -> 1.20f
            else -> 1f
        }
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            // Keep system-bar spacing without reserving a strip for the camera cutout.
            contentWindowInsets = WindowInsets.systemBars,
            bottomBar = {
                if (!isLandscape) {
                    Surface(color = MaterialTheme.colorScheme.background) {
                        val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() / 2
                        NavigationBar(modifier = Modifier
                            .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Horizontal))
                            .padding(bottom = navBottom)
                            .height(64.dp * navigationScale)
                            .testTag("bottom-navigation"),
                            windowInsets = WindowInsets(0, 0, 0, 0), containerColor = MaterialTheme.colorScheme.background, tonalElevation = 0.dp) {
                            listOf(L.text("ui.calendar.ee8bd9", k), L.text("ui.events.11d867", k), L.text("ui.settings.0e0a4f", k)).forEachIndexed { index, title ->
                                NavigationBarItem(selected = page == index, onClick = { jump = false; creating = false; editingId = null; page = index },
                                    icon = { AppIcon(index) }, label = { Text(title, modifier = Modifier.offset(y = (-2).dp), fontSize = 12.readableSp) },
                                    colors = NavigationBarItemDefaults.colors(indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedIconColor = MaterialTheme.colorScheme.primary, selectedTextColor = MaterialTheme.colorScheme.primary))
                            }
                        }
                    }
                }
            },
        ) { padding ->
            val layoutDirection = LocalLayoutDirection.current
            val contentPadding = if (isLandscape) {
                PaddingValues(
                    start = padding.calculateStartPadding(layoutDirection),
                    top = padding.calculateTopPadding() * 0.68f,
                    end = padding.calculateEndPadding(layoutDirection),
                    bottom = padding.calculateBottomPadding()
                )
            } else padding
            Row(Modifier.fillMaxSize().padding(contentPadding)) {
                if (isLandscape) {
                    NavigationRail(
                        modifier = Modifier.fillMaxHeight()
                            .then(if (navigationScale > 1f) Modifier.width(80.dp * navigationScale) else Modifier)
                            .testTag("navigation-rail"),
                        containerColor = MaterialTheme.colorScheme.background,
                        windowInsets = WindowInsets(0, 0, 0, 0)
                    ) {
                        if (isTablet) {
                            Spacer(Modifier.height(8.dp))
                        }
                        listOf(L.text("ui.calendar.ee8bd9", k), L.text("ui.events.11d867", k), L.text("ui.settings.0e0a4f", k)).forEachIndexed { index, title ->
                            NavigationRailItem(
                                modifier = if (!isTablet) Modifier.weight(1f) else Modifier.padding(vertical = 12.dp),
                                selected = page == index,
                                onClick = { jump = false; creating = false; editingId = null; page = index },
                                icon = { AppIcon(index) },
                                label = { Text(title, fontSize = 11.readableSp) },
                                colors = NavigationRailItemDefaults.colors(
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    }
                }
                Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.TopCenter) {
                if (creating || editingId != null) CustomEventEditor(customEvents.firstOrNull { it.id == editingId }, selected, k, settings.todayTimeZone,
                    onCancel = { creating = false; editingId = null },
                    onSave = { event ->
                        onSaveCustom(event); creating = false; editingId = null; detail = null; dateDetailText = null
                        eventYear = event.asCalendarEvent(displayZone).date.year.coerceIn(1800, 2200); page = 1; customFocus++
                    })
                else when (page) {
                    0 -> CalendarScreen(settings, today, month, selected, allCustom,
                        onSelect = { date ->
                            val dateStr = date.toString()
                            if (selectedText == dateStr) {
                                dateDetailText = dateStr
                            } else {
                                selectedText = dateStr
                                if (!isLandscape || !isTablet) dateDetailText = dateStr
                            }
                        }, onPrevious = { navigate(month.minusMonths(1)) },
                        onNext = { navigate(month.plusMonths(1)) }, onJump = { jump = true },
                        onToday = {
                            val date = today.coerceIn(KhmerCalendar.minDate, KhmerCalendar.maxDate)
                            selectedText = date.toString(); monthText = YearMonth.from(date).toString()
                        }, onEvent = { detail = it },
                        onOpenDateDetails = { dateDetailText = it.toString() })
                    1 -> EventsScreen(settings, today, eventYear, allCustom, customFocus, { eventYear = it }, { detail = it }, { creating = true })
                    else -> SettingsScreen(settings, onSettings, notificationAccess, onOpenNotificationSettings, onAllowExact)
                }
            }
        }
    }
        if (jump) MonthPicker(month, today.year, k, { jump = false }) { navigate(it); jump = false }
        if (detail == null) dateDetailText?.let { dateString ->
            val date = LocalDate.parse(dateString)
            DateDetailsDialog(
                date = date,
                today = today,
                k = k,
                custom = allCustom,
                showHolyDays = settings.showHolyDaysInEvents,
                showObservances = settings.showObservances,
                showHolyDaysInCalendar = settings.showHolyDaysInCalendar,
                showCopyButtons = settings.showCopyButtons,
                showWesternZodiac = settings.showWesternZodiac,
                showGanzhi = settings.showGanzhi,
                useEmojiForGanzhiAnimals = settings.useEmojiForGanzhiAnimals,
                todayTimeZone = settings.todayTimeZone,
                onEvent = { detail = it },
                onAddEvent = {
                    selectedText = date.toString()
                    dateDetailText = null
                    creating = true
                },
                onDismiss = { dateDetailText = null }
            )
        }
        detail?.let { original ->
            val event = if (original.kind == EventKind.CUSTOM) allCustom.firstOrNull { it.id == original.id } ?: original else original
            EventDialog(event, k, timeZoneLabel(settings.todayTimeZone, k), settings.showCopyButtons, settings.showWesternZodiac, onEdit = {
            editingId = event.customSeriesId ?: event.id.removePrefix("custom:"); detail = null; dateDetailText = null
        }, onDelete = {
            onDeleteCustom(event.customSeriesId ?: event.id.removePrefix("custom:")); detail = null
        }) { detail = null } }
    }
}

@Composable
private fun CalendarHeader(
    month: YearMonth, selected: LocalDate, today: LocalDate, k: Boolean,
    onJump: () -> Unit, onPrevious: () -> Unit, onNext: () -> Unit, onToday: () -> Unit
) {
    val config = LocalConfiguration.current
    val isPhonePortrait = config.orientation == Configuration.ORIENTATION_PORTRAIT && config.smallestScreenWidthDp < 600
    val yearLabel = number(month.year, k)
    val buddhistYearLabel = "${L.text("ui.be.623a78", k)} ${number(KhmerCalendar.fromGregorian(selected).buddhistYear, k)}"
    val measurer = rememberTextMeasurer()
    val yearWidth = measurer.measure(yearLabel, LocalTextStyle.current.copy(fontSize = 23.sp, fontWeight = FontWeight.SemiBold), maxLines = 1).size.width
    val buddhistYearWidth = measurer.measure(buddhistYearLabel, LocalTextStyle.current.copy(fontSize = 12.readableSp), maxLines = 1).size.width
    val sideWidth = (with(LocalDensity.current) { maxOf(yearWidth, buddhistYearWidth).toDp() } + 15.dp).coerceAtLeast(74.dp)
    BoxWithConstraints(
        Modifier
            .fillMaxWidth()
            .testTag("calendar-header")
            .then(if (isPhonePortrait) Modifier.padding(horizontal = 4.dp) else Modifier)
            .padding(bottom = 2.dp)
    ) {
        val fontScale = LocalDensity.current.fontScale.coerceAtLeast(1f)
        val narrowHeader = maxWidth < 400.dp * fontScale
        Column(
            Modifier
                .align(Alignment.CenterStart)
                .width(sideWidth)
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onJump)
                .padding(start = 10.dp, top = 4.dp, end = 5.dp, bottom = 4.dp)
                .semantics { contentDescription = L.text("ui.choose_month_and_year.252299", k) }
        ) {
            Text(yearLabel, fontSize = 23.sp, lineHeight = if (k) 20.sp else 22.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
            Text(
                buddhistYearLabel,
                modifier = Modifier.offset(y = if (k) (-5).dp else (-3).dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.readableSp, lineHeight = 13.sp, maxLines = 1
            )
        }
        Row(
            Modifier.align(Alignment.Center).widthIn(max = (maxWidth - sideWidth * 2).coerceAtLeast(0.dp)),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ArrowButton(false, month > YearMonth.of(1800, 1), L.text("ui.previous_month.c03e1f", k)) { onPrevious() }
            Text(
                monthName(month, k, short = k || narrowHeader),
                Modifier
                    .weight(1f, fill = false)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(role = Role.Button, onClick = onJump)
                    .padding(vertical = 6.dp, horizontal = 2.dp),
                textAlign = TextAlign.Center, fontSize = 19.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            ArrowButton(true, month < YearMonth.of(2200, 12), L.text("ui.next_month.d2d40f", k)) { onNext() }
        }
        val isTodaySelected = (selected == today)
        TextButton(
            onClick = onToday,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .width(74.dp),
            contentPadding = PaddingValues(0.dp),
            colors = ButtonDefaults.textButtonColors(
                contentColor = if (isTodaySelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.primary
            )
        ) {
            Text(L.text("ui.today.d71ac6", k), fontSize = 13.readableSp)
        }
    }
}

@Composable
private fun CalendarMonthCard(
    month: YearMonth, selected: LocalDate, today: LocalDate, gridEvents: List<CalendarEvent>,
    settings: AppSettings, onSelect: (LocalDate) -> Unit, onPrevious: () -> Unit, onNext: () -> Unit
) {
    val k = settings.khmer
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    Surface(modifier = Modifier.padding(bottom = if (isLandscape) 4.dp else 10.dp), shape = CardShape, color = MaterialTheme.colorScheme.surface) {
        Box(Modifier.fillMaxWidth().clip(CardShape)) {
            if (month.monthValue == 4) {
                val oldAnimal = Math.floorMod(month.year - 4 - 1, 12)
                val newAnimal = Math.floorMod(month.year - 4, 12)
                val alpha = zodiacAlpha()
                Image(
                    painter = painterResource(zodiacDrawable(oldAnimal, compact = true)),
                    contentDescription = null,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = 10.dp, top = 10.dp)
                        .fillMaxWidth(3f / 7f)
                        .aspectRatio(1f),
                    contentScale = ContentScale.Fit,
                    colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary),
                    alpha = alpha,
                )
                Image(
                    painter = painterResource(zodiacDrawable(newAnimal, compact = false)),
                    contentDescription = null,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 10.dp, bottom = 10.dp)
                        .fillMaxWidth(4f / 7f)
                        .aspectRatio(1f),
                    contentScale = ContentScale.Fit,
                    colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary),
                    alpha = alpha,
                )
            } else {
                val animalYear = remember(month) { KhmerDateDetails.fromGregorian(month.atDay(15)).animalYear }
                Image(
                    painter = painterResource(zodiacDrawable(animalYear, compact = false)),
                    contentDescription = null,
                    modifier = Modifier.matchParentSize().padding(16.dp),
                    contentScale = ContentScale.Fit,
                    colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary),
                    alpha = zodiacAlpha(),
                )
            }
            Column(Modifier.padding(start = 10.dp, end = 10.dp, top = if (isLandscape) 8.dp else 16.dp, bottom = if (isLandscape) 6.dp else 10.dp)) {
                MonthGrid(month, selected, today, gridEvents, settings, onSelect, onPrevious, onNext)
                HorizontalDivider(Modifier.padding(horizontal = 8.dp, vertical = if (isLandscape) 3.dp else 5.dp), color = MaterialTheme.colorScheme.outlineVariant)
                val hasCustom = gridEvents.any { it.kind == EventKind.CUSTOM }
                val spacing = if (hasCustom && settings.showHolyDaysInCalendar) (if (isLandscape) 6.dp else 8.dp) else (if (isLandscape) 10.dp else 14.dp)
                FlowRow(
                    Modifier.fillMaxWidth().padding(vertical = if (isLandscape) 2.dp else 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(spacing, Alignment.CenterHorizontally),
                    verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
                ) {
                    Legend(EventKind.HOLIDAY, L.text("ui.holiday.253332", k), eventColor(EventKind.HOLIDAY))
                    if (settings.showHolyDaysInCalendar) {
                        Legend(EventKind.HOLY_DAY, L.text("ui.holy_day.28786d", k), eventColor(EventKind.HOLY_DAY))
                    }
                    if (settings.showObservances) {
                        Legend(EventKind.OBSERVANCE, L.text("ui.observance.5b9a87", k), eventColor(EventKind.OBSERVANCE))
                    }
                    if (hasCustom) {
                        Legend(EventKind.CUSTOM, L.text("ui.custom.917053", k), eventColor(EventKind.CUSTOM))
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarScreen(
    settings: AppSettings, today: LocalDate, month: YearMonth, selected: LocalDate, custom: List<CalendarEvent>,
    onSelect: (LocalDate) -> Unit, onPrevious: () -> Unit, onNext: () -> Unit,
    onJump: () -> Unit, onToday: () -> Unit, onEvent: (CalendarEvent) -> Unit,
    onOpenDateDetails: (LocalDate) -> Unit = {},
) {
    val k = settings.khmer
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val isTablet = configuration.smallestScreenWidthDp >= 600
    val allMonthEvents = remember(month, custom) {
        (EventRepository.forMonth(month) + custom.filter { YearMonth.from(it.date) == month })
            .sortedWith(compareBy({ it.date }, { it.time ?: LocalTime.MIN }, { it.id }))
    }
    val gridEvents = remember(allMonthEvents, settings.showHolyDaysInCalendar, settings.showObservances) {
        allMonthEvents.filter {
            (settings.showHolyDaysInCalendar || it.kind != EventKind.HOLY_DAY) &&
                (settings.showObservances || it.kind != EventKind.OBSERVANCE)
        }
    }
    val listEvents = remember(allMonthEvents, settings.showHolyDaysInEvents, settings.showObservances) {
        allMonthEvents.filter {
            (settings.showHolyDaysInEvents || it.kind != EventKind.HOLY_DAY) &&
                (settings.showObservances || it.kind != EventKind.OBSERVANCE)
        }
    }
    if (isLandscape) {
        val leftWeight = if (isTablet) 1.0f else 0.9f
        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(leftWeight).fillMaxHeight().verticalScroll(rememberScrollState())) {
                CalendarHeader(month, selected, today, k, onJump, onPrevious, onNext, onToday)
                CalendarMonthCard(month, selected, today, gridEvents, settings, onSelect, onPrevious, onNext)
                if (isTablet) {
                    val selectedEvents = remember(selected, listEvents) { listEvents.filter { it.date == selected } }
                    val selectedInfo = remember(selected) { KhmerDateDetails.fromGregorian(selected) }
                    Spacer(Modifier.height(4.dp))
                    Surface(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surface) {
                        Row(
                            modifier = Modifier
                                .clickable { onOpenDateDetails(selected) }
                                .padding(12.dp)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(
                                if (k) selectedInfo.fullKhmerDate() else selectedInfo.fullEnglishDate(),
                                modifier = Modifier.weight(1f),
                                fontSize = 13.readableSp,
                                lineHeight = 20.readableSp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(
                                horizontalAlignment = Alignment.End,
                                verticalArrangement = Arrangement.spacedBy(0.dp)
                            ) {
                                Text(
                                    selectedInfo.gregorianLabel,
                                    fontSize = 13.readableSp,
                                    lineHeight = 20.readableSp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.End
                                )
                                if (settings.showWesternZodiac) {
                                    Text(
                                        selectedInfo.zodiac.label,
                                        fontSize = 12.readableSp,
                                        lineHeight = 20.readableSp,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Medium,
                                        textAlign = TextAlign.End
                                    )
                                }
                            }
                        }
                    }
                    if (selectedEvents.isNotEmpty()) {
                        Text(L.text("ui.events_on_the_day.a174fc", k), Modifier.padding(start = 10.dp, top = 4.dp, bottom = 4.dp),
                            fontSize = 12.readableSp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        selectedEvents.forEach { EventRow(it, k, Modifier.padding(bottom = 6.dp)) { onEvent(it) } }
                    }
                }
            }
            LazyColumn(Modifier.weight(1f).fillMaxHeight().testTag("calendar-scroll"), contentPadding = PaddingValues(top = 4.dp, end = 12.dp, bottom = 6.dp)) {
                if (listEvents.isNotEmpty()) {
                    item {
                        Text(L.text("ui.all_events_in_month.ab923a", k, "month" to monthName(month, k)), Modifier.padding(start = 10.dp, top = 4.dp, bottom = 4.dp),
                            fontSize = 12.readableSp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    items(listEvents, key = { it.key }) { EventRow(it, k, Modifier.padding(bottom = 6.dp)) { onEvent(it) } }
                }
            }
        }
    } else {
        Column(Modifier.widthIn(max = 640.dp).fillMaxSize()) {
            CalendarHeader(month, selected, today, k, onJump, onPrevious, onNext, onToday)
            LazyColumn(Modifier.weight(1f).fillMaxWidth().testTag("calendar-scroll"), contentPadding = PaddingValues(10.dp, 0.dp, 10.dp, 24.dp)) {
                item {
                    CalendarMonthCard(month, selected, today, gridEvents, settings, onSelect, onPrevious, onNext)
                }
                items(listEvents, key = { it.key }) { EventRow(it, k, Modifier.padding(bottom = 8.dp)) { onEvent(it) } }
            }
        }
    }
}

@Composable
private fun MonthGrid(month: YearMonth, selected: LocalDate, today: LocalDate, events: List<CalendarEvent>, settings: AppSettings, onSelect: (LocalDate) -> Unit, onPrevious: () -> Unit, onNext: () -> Unit) {
    val k = settings.khmer
    val config = LocalConfiguration.current
    val isLandscape = config.orientation == Configuration.ORIENTATION_LANDSCAPE
    val isTablet = config.smallestScreenWidthDp >= 600
    val isPhoneLandscape = isLandscape && !isTablet
    val fontScale = LocalDensity.current.fontScale.coerceAtLeast(1f)
    val baseHeight = when {
        isTablet && isLandscape -> 52.dp
        isTablet -> 64.dp
        isPhoneLandscape -> 44.dp
        else -> 56.dp
    }
    val cellHeight = (baseHeight * fontScale)
    val firstOffset = if (settings.mondayFirst) month.atDay(1).dayOfWeek.value - 1 else month.atDay(1).dayOfWeek.value % 7
    val weekdays = (0..6).map { CalendarWords.weekday(it, k, if (settings.showLongerWeekdayNames) "grid_long" else "grid") }
    val ordered = if (settings.mondayFirst) weekdays.drop(1) + weekdays.first() else weekdays
    val byDate = remember(events) { events.groupBy { it.date } }
    Column(Modifier.testTag("month-grid").pointerInput(month) {
        var drag = 0f
        detectHorizontalDragGestures(onDragStart = { drag = 0f }, onHorizontalDrag = { change, amount -> change.consume(); drag += amount }, onDragEnd = {
            if (abs(drag) > 80.dp.toPx()) { if (drag < 0) onNext() else onPrevious() }
        })
    }) {
        BoxWithConstraints(Modifier.fillMaxWidth().padding(bottom = if (isPhoneLandscape) 3.dp else 8.dp)) {
            val preferredSize = 11.readableSp
            val headerSize = if (settings.showLongerWeekdayNames) {
                // Keep all seven names at one size, fitting the longest without truncating Khmer.
                val measurer = rememberTextMeasurer()
                val style = LocalTextStyle.current.copy(fontSize = preferredSize, fontWeight = FontWeight.Medium)
                val widest = ordered.maxOf { measurer.measure(it, style, softWrap = false, maxLines = 1).size.width }.coerceAtLeast(1)
                val available = with(LocalDensity.current) { (maxWidth / 7 - 2.dp).toPx() }.coerceAtLeast(1f)
                preferredSize * (available / widest).coerceAtMost(1f)
            } else preferredSize
            val dark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
            Row(Modifier.fillMaxWidth()) {
                ordered.forEachIndexed { index, it ->
                    val weekday = DayOfWeek.of(if (settings.mondayFirst) index + 1 else if (index == 0) 7 else index)
                    val headerColor = when {
                        settings.highlightWeekdayNames -> weekdayNameColor(weekday, dark)
                        settings.highlightSunday && weekday == DayOfWeek.SUNDAY -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    Text(it, Modifier.weight(1f).testTag("weekday-header-${weekday.value}"), textAlign = TextAlign.Center, fontSize = headerSize,
                        fontWeight = FontWeight.Medium, color = headerColor, maxLines = 1, softWrap = false)
                }
            }
        }
        repeat((firstOffset + month.lengthOfMonth() + 6) / 7) { week ->
            Row(Modifier.fillMaxWidth()) {
                repeat(7) { column ->
                    val day = week * 7 + column - firstOffset + 1
                    if (day !in 1..month.lengthOfMonth()) Spacer(Modifier.weight(1f)) else {
                        val date = month.atDay(day)
                        val lunar = remember(date) { KhmerCalendar.fromGregorian(date) }
                        val entries = byDate[date].orEmpty()
                        val active = date == selected
                        val isToday = date == today
                        val holiday = entries.any { it.kind == EventKind.HOLIDAY }
                        val isSunday = date.dayOfWeek == DayOfWeek.SUNDAY
                        val isHoliday = holiday || (settings.highlightSunday && isSunday)
                        val colors = MaterialTheme.colorScheme
                        Box(
                            Modifier.weight(1f).padding(1.dp).clip(RoundedCornerShape(if (isPhoneLandscape) 8.dp else 11.dp))
                                .background(if (isToday) colors.primary else Color.Transparent)
                                .then(if (active && !isToday) Modifier.border(1.dp, colors.primary, RoundedCornerShape(if (isPhoneLandscape) 8.dp else 11.dp)) else Modifier)
                                .clickable { onSelect(date) }
                                .semantics(mergeDescendants = true) {
                                    contentDescription = if (k) {
                                        "${dateLabel(date, true)} ${lunar.fullLabel(true)}. ${entries.joinToString(" ") { it.title(true) }}"
                                    } else {
                                        "${dateLabel(date, false)}, ${lunar.fullLabel(false)}. ${entries.joinToString { it.title(false) }}"
                                    }
                                    this.selected = active
                                }
                                .height(cellHeight),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (settings.showHolyDaysInCalendar && lunar.isHolyDay) {
                                Image(painterResource(holyDayLotusDrawable(lunar)), contentDescription = null,
                                    modifier = Modifier.matchParentSize().padding(1.dp).scale(if (isPhoneLandscape) 0.90f else 1.0f), contentScale = ContentScale.Fit, alpha = 0.25f)
                            }
                            Column(Modifier.fillMaxWidth().offset(y = if (isPhoneLandscape) (-0.5).dp else (-1.0).dp).padding(vertical = if (isPhoneLandscape) 0.5.dp else 1.dp),
                                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(0.dp)) {
                                Text(number(day, k), color = if (isToday) colors.onPrimary else if (isHoliday) colors.tertiary else colors.onSurface,
                                    fontSize = (if (isPhoneLandscape) 13 else 16).readableSp, lineHeight = (if (isPhoneLandscape) 15 else 17).sp, maxLines = 1, fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium)
                                if (settings.showLunar) Text(lunar.shortLabel(k), modifier = Modifier.offset(y = if (isPhoneLandscape) (-1.5).dp else (-1).dp), color = if (isToday) colors.onPrimary.copy(alpha = .85f) else colors.onSurfaceVariant,
                                    fontSize = (if (isPhoneLandscape) 8 else 10).readableSp, lineHeight = (if (isPhoneLandscape) 10 else 12).readableSp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                val markSize = when {
                                    isTablet && isLandscape -> 7.0.dp
                                    isTablet -> 8.5.dp
                                    isLandscape -> 5.5.dp
                                    else -> 6.5.dp
                                }
                                val markRowHeight = when {
                                    isTablet && isLandscape -> 8.0.dp
                                    isTablet -> 9.5.dp
                                    isLandscape -> 6.dp
                                    else -> 7.dp
                                }
                                val markSpacing = if (isTablet) 2.5.dp else 2.dp
                                Row(Modifier.height(markRowHeight), horizontalArrangement = Arrangement.spacedBy(markSpacing), verticalAlignment = Alignment.CenterVertically) {
                                    entries.map { it.kind }.distinct().take(4).forEach { EventMark(it, if (isToday) colors.onPrimary else eventColor(it), sizeDp = markSize) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EventsScreen(settings: AppSettings, today: LocalDate, year: Int, custom: List<CalendarEvent>, customFocus: Int, onYear: (Int) -> Unit, onEvent: (CalendarEvent) -> Unit, onAdd: () -> Unit) {
    val k = settings.khmer
    var query by rememberSaveable(customFocus) { mutableStateOf("") }
    var filter by rememberSaveable(customFocus) { mutableIntStateOf(if (customFocus > 0) 4 else 0) }
    var showYearPicker by rememberSaveable { mutableStateOf(false) }
    BackHandler(enabled = showYearPicker) { showYearPicker = false }
    val filterScroll = rememberScrollState()
    val eventsListState = rememberLazyListState()
    val searchFocus = remember { FocusRequester() }
    val scope = rememberCoroutineScope()
    val searchScrolledPast by remember { derivedStateOf { eventsListState.firstVisibleItemIndex >= 2 } }
    LaunchedEffect(filter) { if (filter == 4) filterScroll.scrollTo(0) }
    LaunchedEffect(settings.showHolyDaysInEvents) { if (!settings.showHolyDaysInEvents && filter == 3) filter = 0 }
    LaunchedEffect(settings.showObservances) { if (!settings.showObservances && filter == 2) filter = 0 }
    val events = remember(year, query, filter, settings.showHolyDaysInEvents, settings.showObservances, custom) {
        (EventRepository.forYear(year) + custom.filter { it.date.year == year }).filter { event ->
            (settings.showHolyDaysInEvents || event.kind != EventKind.HOLY_DAY) &&
                (settings.showObservances || event.kind != EventKind.OBSERVANCE) &&
                (filter == 0 || (filter == 1 && event.kind == EventKind.HOLIDAY) || (filter == 2 && event.kind == EventKind.OBSERVANCE) || (filter == 3 && event.kind == EventKind.HOLY_DAY) || (filter == 4 && event.kind == EventKind.CUSTOM)) &&
                (query.isBlank() || searchText("${event.titleEn} ${event.titleKm} ${event.date} ${event.notes}").contains(searchText(query)))
        }.sortedWith(compareBy({ it.date }, { it.time ?: LocalTime.MIN }, { it.id }))
    }
    if (showYearPicker) {
        EventYearPicker(year, today.year, k, onDismiss = { showYearPicker = false }) {
            onYear(it)
            showYearPicker = false
        }
    }
    Box(Modifier.widthIn(max = 640.dp).fillMaxSize().testTag("events-content")) {
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().testTag("events-header").padding(10.dp, 2.dp, 10.dp, 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(L.text("ui.events.11d867", k), Modifier.padding(start = 10.dp).weight(1f), fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                ArrowButton(false, year > 1800, L.text("ui.previous_year.a0618a", k)) { onYear(year - 1) }
                Text(number(year, k), Modifier.widthIn(min = 64.dp).testTag("event-year").clip(RoundedCornerShape(8.dp))
                    .clickable(role = Role.Button) { showYearPicker = true }.padding(vertical = 8.dp)
                    .semantics { contentDescription = L.text("ui.choose_year.0853a0", k) },
                    textAlign = TextAlign.Center, fontSize = 23.sp, fontWeight = FontWeight.SemiBold)
                ArrowButton(true, year < 2200, L.text("ui.next_year.1f632d", k)) { onYear(year + 1) }
            }
            LazyColumn(Modifier.weight(1f).fillMaxWidth().testTag("events-scroll"), state = eventsListState, contentPadding = PaddingValues(10.dp, 0.dp, 10.dp, 112.dp)) {
                item {
                    MaterialTheme(typography = MaterialTheme.typography.copy(bodyLarge = MaterialTheme.typography.bodyLarge.copy(fontSize = 14.readableSp, lineHeight = 16.readableSp))) {
                        OutlinedTextField(value = query, onValueChange = { query = it }, modifier = Modifier.fillMaxWidth().testTag("event-search").focusRequester(searchFocus).padding(bottom = 16.dp), singleLine = true,
                            textStyle = LocalTextStyle.current.copy(fontSize = 14.readableSp, lineHeight = 16.readableSp),
                            label = { Text(L.text("ui.search_events.08c608", k)) }, shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                disabledContainerColor = MaterialTheme.colorScheme.surface,
                            ),
                            trailingIcon = { if (query.isNotEmpty()) TextButton(onClick = { query = "" }) { Text(L.text("ui.clear.7d76fd", k)) } })
                    }
                }
                item {
                    Row(Modifier.padding(bottom = 16.dp).testTag("event-filters").horizontalScroll(filterScroll), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val filters = listOf(0 to L.text("ui.all.c10205", k), 4 to L.text("ui.custom.917053", k), 1 to L.text("ui.holidays.8a894c", k), 2 to L.text("ui.observances.e4454c", k), 3 to L.text("ui.holy_days.9569a6", k))
                        filters.forEach { (id, text) ->
                            if ((settings.showHolyDaysInEvents || id != 3) && (settings.showObservances || id != 2)) SelectionChip(
                                selected = filter == id,
                                onClick = { filter = id },
                                label = { Text(text) },
                            )
                        }
                    }
                }
                if (events.isEmpty()) item { EmptyEvents(k) }
                events.groupBy { it.date.month }.values.forEachIndexed { index, monthEvents ->
                    item(key = "month:${monthEvents.first().date.monthValue}") {
                        Row(
                            Modifier.fillMaxWidth().padding(start = 10.dp, end = 10.dp, top = if (index == 0) 8.dp else 16.dp, bottom = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(monthName(YearMonth.from(monthEvents.first().date), k), fontSize = 18.readableSp, fontWeight = FontWeight.SemiBold)
                            Text(number(monthEvents.size, k), fontSize = 14.readableSp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    items(monthEvents, key = { it.key }) { EventRow(it, k, Modifier.padding(bottom = 8.dp)) { onEvent(it) } }
                }
            }
        }
        AnimatedVisibility(visible = searchScrolledPast, modifier = Modifier.align(Alignment.BottomEnd), enter = fadeIn(), exit = fadeOut()) {
            FloatingActionButton(
                onClick = {
                    scope.launch {
                        eventsListState.animateScrollToItem(0)
                        searchFocus.requestFocus()
                    }
                },
                modifier = Modifier.padding(end = 32.dp, bottom = 96.dp).size(48.dp).testTag("search-events-fab")
                    .semantics { contentDescription = L.text("ui.search_events.08c608", k) },
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) { SearchIcon(Modifier.size(24.dp)) }
        }
        FloatingActionButton(onClick = onAdd, modifier = Modifier.align(Alignment.BottomEnd).padding(end = 32.dp, bottom = 32.dp)
            .size(48.dp).testTag("add-event").semantics { contentDescription = L.text("ui.add_event.bf2f10", k) },
            shape = CircleShape,
            containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary) {
            Text("+", fontSize = 24.sp)
        }
    }
}

@Composable
private fun EventRow(event: CalendarEvent, k: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val color = eventColor(event.kind)
    val dateColor = if (event.kind == EventKind.HOLIDAY) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface
    Surface(onClick = onClick, modifier = modifier, shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface) {
        Box(Modifier.fillMaxWidth()) {
            if (event.kind == EventKind.CUSTOM) {
                Image(
                    painter = painterResource(R.drawable.custom_event_star),
                    contentDescription = null,
                    alignment = Alignment.CenterEnd,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .matchParentSize()
                        .padding(top = 2.dp, bottom = 2.dp, end = 21.dp),
                    colorFilter = ColorFilter.tint(color, BlendMode.Modulate),
                    alpha = 0.05f
                )
            }
            Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.width(42.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(number(event.date.dayOfMonth, k), color = dateColor, fontSize = 21.readableSp, lineHeight = 21.readableSp, fontWeight = FontWeight.SemiBold)
                    Text(CalendarWords.weekday(event.date.dayOfWeek.value, k, "short"), modifier = Modifier.offset(y = (-1.5).dp),
                        fontSize = 10.readableSp, lineHeight = 12.readableSp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Box(Modifier.padding(horizontal = 12.dp).width(3.dp).height(28.dp).background(color.copy(alpha = .65f), CircleShape))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(event.title(k), fontWeight = FontWeight.Medium, fontSize = 13.readableSp, lineHeight = 19.readableSp)
                    Text((if (event.basis == DateBasis.CALCULATED) L.text("rules.calculated_label", k) else kindLabel(event.kind, k)) +
                        (event.time?.let { " · $it" } ?: ""), color = color, fontSize = 11.readableSp, lineHeight = 14.readableSp)
                }
                Text("›", Modifier.padding(start = 8.dp), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 20.sp)
            }
        }
    }
}

@Composable
private fun SettingsScreen(settings: AppSettings, onChange: (AppSettings) -> Unit, access: NotificationAccess, onSystemSettings: () -> Unit, onAllowExact: () -> Unit) {
    val k = settings.khmer
    val fontScales = FontScale.entries.filter { it.multiplier <= 1.5f }
    var showSources by remember { mutableStateOf(false) }
    LazyColumn(Modifier.widthIn(max = 640.dp).fillMaxSize().testTag("settings-scroll"), contentPadding = PaddingValues(horizontal = 10.dp, vertical = 20.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
        item {
            SettingsCard(L.text("ui.appearance.23e609", k)) {
                SettingsRow(L.text("ui.language.b03320", k), L.text("ui.language_subtitle", k)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        SettingChoice(L.text("language.khmer", k), settings.khmer) { onChange(settings.copy(khmer = true)) }
                        SettingChoice(L.text("language.english", k), !settings.khmer) { onChange(settings.copy(khmer = false)) }
                    }
                }
                val fontSizeTitle = if (k) "ទំហំអក្សរ" else "Font size"
                SettingsRow(fontSizeTitle, L.text("ui.font_size_subtitle", k)) {
                    SettingDropdown(settings.fontScale, fontScales,
                        { it.label },
                        "font-scale", fontSizeTitle) { onChange(settings.copy(fontScale = it)) }
                }
                SettingsRow(L.text("ui.theme.99ca72", k), L.text("ui.theme_subtitle", k)) {
                    val dark = MaterialTheme.colorScheme.surface.luminance() < .5f
                    Row(Modifier.testTag("theme-mode"), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        SettingChoice(L.text("ui.light.aa790e", k), !dark) { onChange(settings.copy(theme = ThemeMode.LIGHT)) }
                        SettingChoice(L.text("ui.dark.4ae267", k), dark) { onChange(settings.copy(theme = ThemeMode.DARK)) }
                    }
                }
                fun accentName(accent: Accent) = when (accent) { Accent.BLUE -> L.text("ui.blue.cf6f1f", k); Accent.LAVENDER -> L.text("ui.lavender.b7c95a", k); Accent.ROSE -> L.text("ui.rose.ea1e14", k); Accent.AMBER -> L.text("ui.amber.195385", k); Accent.LIME -> L.text("ui.lime.46ea65", k) }
                SettingsRow(L.text("ui.accent_color.97e2af", k), L.text("ui.accent_color_subtitle", k)) {
                    SettingDropdown(settings.accent, Accent.entries,
                        ::accentName,
                        "accent-color", L.text("ui.accent_color.97e2af", k),
                        itemColor = ::accentColor) { onChange(settings.copy(accent = it)) }
                }
                SettingSwitch(L.text("ui.background_accent", k), L.text("ui.background_accent_subtitle", k), settings.backgroundAccent) {
                    onChange(settings.copy(backgroundAccent = it))
                }
            }
        }
        item {
            SettingsCard(L.text("ui.timezone.72f68d", k)) {
                SettingsRow(L.text("ui.today_follows.b52168", k), L.text("ui.set_timezone.cdea40", k)) {
                    SettingDropdown(settings.todayTimeZone, TodayTimeZone.entries,
                        { if (it == TodayTimeZone.LOCAL) "${L.text("ui.local_time.541b44", k)} (${it.offsetLabel()})" else L.text("ui.cambodia_time_utc_7.6b9f2d", k) },
                        "today-time-zone", L.text("ui.today_follows.b52168", k)) { onChange(settings.copy(todayTimeZone = it)) }
                }
            }
        }
        item {
            SettingsCard(L.text("ui.calendar.beb873", k)) {
                SettingSwitch(L.text("ui.show_copy_buttons", k), L.text("ui.show_copy_buttons_subtitle", k), settings.showCopyButtons) { onChange(settings.copy(showCopyButtons = it)) }
                SettingSwitch(L.text("ui.show_longer_weekday_names", k), L.text("ui.show_longer_weekday_names_subtitle", k), settings.showLongerWeekdayNames) { onChange(settings.copy(showLongerWeekdayNames = it)) }
                SettingSwitch(L.text("ui.show_observances", k), L.text("ui.show_observances_subtitle", k), settings.showObservances) { onChange(settings.copy(showObservances = it)) }
                SettingSwitch(L.text("ui.highlight_weekday_names", k), L.text("ui.highlight_weekday_names_subtitle", k), settings.highlightWeekdayNames) { onChange(settings.copy(highlightWeekdayNames = it)) }
                SettingSwitch(L.text("ui.highlight_sunday_column.549462", k), L.text("ui.show_sundays_in_red_like_holidays.245681", k), settings.highlightSunday) { onChange(settings.copy(highlightSunday = it)) }
                SettingSwitch(L.text("ui.lunar_dates_in_calendar.4dffed", k), L.text("ui.koeut_and_roach_under_each_date.f23bd7", k), settings.showLunar) { onChange(settings.copy(showLunar = it)) }
                SettingSwitch(L.text("ui.buddhist_holy_days_in_calendar.d1e9b6", k), L.text("ui.show_lotus_markers_and_holy_days.c9d0bc", k), settings.showHolyDaysInCalendar) { onChange(settings.copy(showHolyDaysInCalendar = it)) }
                SettingSwitch(L.text("ui.buddhist_holy_days_in_events.53e502", k), L.text("ui.show_in_the_events_list_and_filters.425758", k), settings.showHolyDaysInEvents) {
                    onChange(settings.copy(showHolyDaysInEvents = it))
                }
                SettingSwitch(L.text("ui.start_week_on_monday.5578c3", k), L.text("ui.sunday_when_turned_off.e40816", k), settings.mondayFirst) { onChange(settings.copy(mondayFirst = it)) }
            }
        }
        item {
            SettingsCard(L.text("ui.astrology_zodiac", k)) {
                SettingSwitch(L.text("ui.show_western_zodiac", k), L.text("ui.show_western_zodiac_subtitle", k), settings.showWesternZodiac) {
                    onChange(settings.copy(showWesternZodiac = it))
                }
                SettingSwitch(L.text("ui.show_chinese_ganzhi", k), L.text("ui.show_chinese_ganzhi_subtitle", k), settings.showGanzhi) {
                    onChange(settings.copy(showGanzhi = it))
                }
                if (settings.showGanzhi) {
                    SettingSwitch(L.text("ui.ganzhi_emoji_toggle", k), L.text("ui.ganzhi_emoji_subtitle", k), settings.useEmojiForGanzhiAnimals) {
                        onChange(settings.copy(useEmojiForGanzhiAnimals = it))
                    }
                }
            }
        }
        item { NotificationSettingsCard(settings, onChange, access, onSystemSettings, onAllowExact) }
        item { WidgetSettingsCard(settings, onChange) }
        item {
            SettingsCard(L.text("ui.for_everyone.b68901", k), spacedContent = true) {
                Text(L.text("ui.free_ad_free_yours.885c91", k), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Text(L.text("ui.no_account_no_internet_no_tracking_just_your_calendar_a.9bc7bf", k), fontSize = 14.readableSp, lineHeight = 23.readableSp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(onClick = { showSources = true }, contentPadding = PaddingValues(0.dp)) { Text(L.text("ui.calendar_sources_licenses.c2bdb3", k)) }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("RSG-KH · ${L.text("app.name", k)}", fontSize = 11.readableSp, lineHeight = 16.readableSp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(L.text("about.version", k, "version" to BuildConfig.VERSION_NAME), fontSize = 11.readableSp, lineHeight = 16.readableSp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Column(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        SourceLink("https://github.com/RSG-KH/khmer-calendar", "Android · RSG-KH/khmer-calendar", compact = true)
                        SourceLink("https://github.com/RSG-KH/khmer-calendar-pwa", "PWA · RSG-KH/khmer-calendar-pwa", compact = true)
                    }
                }
            }
        }
    }
    if (showSources) SourcesDialog(k) { showSources = false }
}

@Composable internal fun SettingsCard(title: String, spacedContent: Boolean = false, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(title, Modifier.padding(start = 10.dp), fontSize = 11.readableSp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Surface(shape = CardShape, color = MaterialTheme.colorScheme.surface) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(if (spacedContent) 12.dp else 0.dp), content = content)
        }
    }
}

@Composable
internal fun WidgetSettingsCard(
    settings: AppSettings,
    onChange: (AppSettings) -> Unit,
) {
    val k = settings.khmer
    val cardTitle = if (k) "ធាតុក្រាហ្វិក (វីដជិត)" else "WIDGETS"
    val title = if (k) "បើកដំណើរការវីដជិត" else "Enable widgets"
    val subtitle = if (k) "នៅលើអេក្រង់ដើម" else "On home screen"

    val personalTitle = if (k) "ព្រឹត្តិការណ៍ផ្ទាល់ខ្លួន" else "Personal events"
    val personalSubtitle = if (k) "បង្ហាញព្រឹត្តិការណ៍ផ្ទាល់ខ្លួន" else "Show personal events"

    val holidaysTitle = if (k) "ថ្ងៃឈប់សម្រាកផ្លូវការ" else "Public holidays"
    val holidaysSubtitle = if (k) "បង្ហាញថ្ងៃឈប់សម្រាកផ្លូវការ" else "Show official public holidays"

    val observancesTitle = if (k) "ទិវា និងពិធីបុណ្យ" else "Observances"
    val observancesSubtitle = if (k) "បង្ហាញទិវា និងពិធីបុណ្យនានា" else "Show festivals and others"

    val privacyTitle = if (k) "លាក់ព័ត៌មានលម្អិតនៃព្រឹត្តិការណ៍ផ្ទាល់ខ្លួន" else "Hide personal event details"
    val privacySubtitle = if (k) "បង្ហាញតែចំនួន ដោយលាក់មាតិកា" else "Show count with hidden contents"

    SettingsCard(cardTitle) {
        SettingSwitch(
            title = title,
            subtitle = subtitle,
            checked = settings.widgetsEnabled,
        ) { enabled ->
            onChange(settings.copy(widgetsEnabled = enabled))
        }

        if (settings.widgetsEnabled) {
            SettingSwitch(
                title = personalTitle,
                subtitle = personalSubtitle,
                checked = settings.widgetShowPersonal,
            ) { onChange(settings.copy(widgetShowPersonal = it)) }

            SettingSwitch(
                title = holidaysTitle,
                subtitle = holidaysSubtitle,
                checked = settings.widgetShowHolidays,
            ) { onChange(settings.copy(widgetShowHolidays = it)) }

            if (settings.showObservances) {
                SettingSwitch(
                    title = observancesTitle,
                    subtitle = observancesSubtitle,
                    checked = settings.widgetShowObservances,
                ) { onChange(settings.copy(widgetShowObservances = it)) }
            }

            if (settings.widgetShowPersonal) {
                SettingSwitch(
                    title = privacyTitle,
                    subtitle = privacySubtitle,
                    checked = settings.widgetHidePersonalDetails,
                ) { onChange(settings.copy(widgetHidePersonalDetails = it)) }
            }

            val fontSizeTitle = if (k) "ទំហំអក្សរ" else "Font size"
            SettingsRow(fontSizeTitle, L.text("ui.font_size_subtitle", k)) {
                SettingDropdown(settings.widgetFontScale, FontScale.entries,
                    { it.label },
                    "widget-font-scale", fontSizeTitle) { onChange(settings.copy(widgetFontScale = it)) }
            }
        }
    }
}

@Composable private fun EventYearPicker(year: Int, thisYear: Int, k: Boolean, onDismiss: () -> Unit, onChoose: (Int) -> Unit) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    var yearText by rememberSaveable { mutableStateOf(year.toString()) }
    val valid = yearText.toIntOrNull()?.let { it in 1800..2200 } == true
    val scrollState = rememberScrollState()
    CalendarBasicAlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            .widthIn(max = if (isLandscape) 680.dp else 640.dp).fillMaxWidth(),
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(shape = CardShape, color = MaterialTheme.colorScheme.surface) {
            Column(Modifier.fillMaxWidth().verticalScrollbar(scrollState).verticalScroll(scrollState)
                .padding(if (isLandscape) 18.dp else 24.dp).testTag("event-year-options"),
                verticalArrangement = Arrangement.spacedBy(if (isLandscape) 12.dp else 16.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(L.text("ui.choose_year.0853a0", k), fontSize = if (isLandscape) 20.sp else 22.sp, fontWeight = FontWeight.SemiBold)
                    TextButton(onClick = { onChoose(thisYear) }) { Text(L.text("ui.this_year.02e981", k)) }
                }
                OutlinedTextField(value = yearText, onValueChange = { yearText = it.filter(Char::isDigit).take(4) }, modifier = Modifier.fillMaxWidth().testTag("event-year-input"), singleLine = true, label = { Text("${L.text("ui.year.61d597", k)} (1800–2200)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), isError = !valid)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text(L.text("ui.cancel.5bf834", k)) }
                    TextButton(enabled = valid, onClick = { onChoose(yearText.toInt()) }) { Text(L.text("ui.go.ba4f19", k)) }
                }
            }
        }
    }
}

@Composable private fun MonthPicker(month: YearMonth, thisYear: Int, k: Boolean, onDismiss: () -> Unit, onConfirm: (YearMonth) -> Unit) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    var year by rememberSaveable { mutableStateOf(month.year.toString()) }
    var chosen by rememberSaveable { mutableIntStateOf(month.monthValue) }
    val valid = year.toIntOrNull()?.let { it in 1800..2200 } == true
    val scrollState = rememberScrollState()
    val columns = if (isLandscape) 6 else 3
    CalendarBasicAlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            .widthIn(max = if (isLandscape) 680.dp else 640.dp).fillMaxWidth(),
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(shape = CardShape, color = MaterialTheme.colorScheme.surface) {
            Column(Modifier.fillMaxWidth().verticalScrollbar(scrollState).verticalScroll(scrollState)
                .padding(if (isLandscape) 18.dp else 24.dp).testTag("month-picker"),
                verticalArrangement = Arrangement.spacedBy(if (isLandscape) 12.dp else 16.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(L.text("ui.jump_to_month.b37571", k), fontSize = if (isLandscape) 20.sp else 22.sp, fontWeight = FontWeight.SemiBold)
                    TextButton(onClick = { year = thisYear.toString() }) { Text(L.text("ui.this_year.02e981", k)) }
                }
                OutlinedTextField(value = year, onValueChange = { year = it.filter(Char::isDigit).take(4) }, modifier = Modifier.fillMaxWidth().testTag("month-year-input"), singleLine = true, label = { Text("${L.text("ui.year.61d597", k)} (1800–2200)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), isError = !valid)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    (1..12).chunked(columns).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            row.forEach { m ->
                                Surface(onClick = { chosen = m }, modifier = Modifier.weight(1f).semantics { selected = chosen == m }, shape = RoundedCornerShape(10.dp),
                                    color = if (chosen == m) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant) {
                                    Text(CalendarWords.month(m, k, short = true),
                                        Modifier.padding(vertical = if (isLandscape) 10.dp else 14.dp), textAlign = TextAlign.Center, fontSize = 12.readableSp,
                                        fontWeight = if (chosen == m) FontWeight.Bold else FontWeight.Normal,
                                        maxLines = 1,
                                        color = if (chosen == m) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
                                }
                            }
                        }
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text(L.text("ui.cancel.5bf834", k)) }
                    TextButton(enabled = valid, onClick = { onConfirm(YearMonth.of(year.toInt(), chosen)) }) { Text(L.text("ui.go.ba4f19", k)) }
                }
            }
        }
    }
}

@Composable private fun DateDetailsDialog(
    date: LocalDate, today: LocalDate, k: Boolean, custom: List<CalendarEvent>,
    showHolyDays: Boolean, showObservances: Boolean, showHolyDaysInCalendar: Boolean, showCopyButtons: Boolean, showWesternZodiac: Boolean = true,
    showGanzhi: Boolean = true, useEmojiForGanzhiAnimals: Boolean = false,
    todayTimeZone: TodayTimeZone,
    onEvent: (CalendarEvent) -> Unit, onAddEvent: () -> Unit, onDismiss: () -> Unit
) {
    val info = remember(date) { KhmerDateDetails.fromGregorian(date) }
    val title = if (k) info.gregorianLabel else "${CalendarWords.weekday(date.dayOfWeek.value, false)}, ${info.gregorianLabel}"
    val events = remember(date, custom, showHolyDays, showObservances) {
        (EventRepository.forDate(date) + custom.filter { it.date == date })
            .filter { (showHolyDays || it.kind != EventKind.HOLY_DAY) && (showObservances || it.kind != EventKind.OBSERVANCE) }
            .sortedWith(compareBy({ it.time ?: LocalTime.MIN }, { it.id }))
    }
    CalendarBasicAlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.padding(horizontal = 20.dp).widthIn(max = 520.dp).fillMaxWidth(),
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp,
        ) {
            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp))) {
                DetailsZodiacBackground(info, showWesternZodiac)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 24.dp, top = 24.dp, end = 12.dp, bottom = 24.dp)
                ) {
                    Row(Modifier.padding(end = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            title,
                            Modifier.weight(1f).testTag("date-details-title"),
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Normal,
                            fontSize = 14.readableSp,
                            lineHeight = 22.readableSp,
                        )
                        if (date == today) Text(
                            L.text("ui.today.d71ac6", k),
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 12.readableSp
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    val scrollState = rememberScrollState()
                    Column(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .verticalScrollbar(scrollState)
                            .verticalScroll(scrollState)
                            // Leave room inside the viewport for the copy button's full tap area.
                            .padding(end = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        val fullDate = if (k) info.fullKhmerDate() else info.fullEnglishDate()
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(fullDate, modifier = Modifier.weight(1f), fontSize = 18.readableSp, lineHeight = 32.readableSp, color = MaterialTheme.colorScheme.onSurface)
                            if (showCopyButtons) CopyTextButton(fullDate, L.text("ui.copy_full_date", k), L.text("ui.full_date_copied", k),
                                firstLineHeight = 32.readableSp)
                        }
                        val hasHolyDay = showHolyDaysInCalendar && (info.lunar.isHolyDay || info.lunar.isShavingDay)
                        if (hasHolyDay || showWesternZodiac || showGanzhi) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                if (hasHolyDay) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(DetailSymbolGap)
                                    ) {
                                        if (info.lunar.isHolyDay) {
                                            Image(
                                                painter = painterResource(holyDayLotusDrawable(info.lunar)),
                                                contentDescription = null,
                                                modifier = Modifier.size(DetailSymbolSlot).padding(2.dp),
                                                contentScale = ContentScale.Fit
                                            )
                                        } else {
                                            Box(
                                                modifier = Modifier.size(DetailSymbolSlot),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text("🙏", fontSize = 15.readableSp)
                                            }
                                        }
                                        Text(
                                            if (info.lunar.isHolyDay) L.text("ui.thngai_sil_buddhist_holy_day.89de73", k)
                                            else L.text("ui.thngai_kaor_before_a_holy_day.d02977", k),
                                            modifier = Modifier.testTag("date-details-holy-label"),
                                            color = MaterialTheme.colorScheme.secondary,
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 13.readableSp
                                        )
                                    }
                                }
                                if (showWesternZodiac) {
                                    Row(verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(DetailSymbolGap)) {
                                        Box(Modifier.size(DetailSymbolSlot), contentAlignment = Alignment.Center) {
                                            Text(info.zodiac.emoji, fontSize = 15.readableSp)
                                        }
                                        Text(info.zodiac.label.removePrefix("${info.zodiac.emoji} "),
                                            Modifier.testTag("date-details-zodiac-label"),
                                            fontSize = 13.readableSp, color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Medium)
                                    }
                                }
                                if (showGanzhi) {
                                    GanzhiTable(info, date == today, todayTimeZone, k, useEmojiForGanzhiAnimals)
                                }
                            }
                        }
                        if (events.isNotEmpty()) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            Column(Modifier.testTag("date-details-events"), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                events.forEach { event ->
                                    Surface(
                                        onClick = { onEvent(event) },
                                        shape = RoundedCornerShape(14.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            EventMark(event.kind, eventColor(event.kind), small = false)
                                            Spacer(Modifier.width(12.dp))
                                            Column(Modifier.weight(1f)) {
                                                Text(event.title(k), fontSize = 14.readableSp, lineHeight = 20.readableSp, fontWeight = FontWeight.Medium)
                                                Text(kindLabel(event.kind, k) + (event.time?.let { " · $it" } ?: ""), fontSize = 12.readableSp, lineHeight = 16.readableSp, color = eventColor(event.kind))
                                            }
                                            Text("›", fontSize = 20.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(end = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = onAddEvent) {
                            Text(L.text("ui.add_event.bf2f10", k))
                        }
                        TextButton(onClick = onDismiss) {
                            Text(L.text("ui.close.7df7dc", k))
                        }
                    }
                }
            }
        }
    }
}

private data class GanzhiColumn(val key: String, val label: String, val pillar: GanzhiPillar?)

@Composable private fun GanzhiTable(
    info: KhmerDateDetails, isToday: Boolean, timeZone: TodayTimeZone, khmer: Boolean, useEmoji: Boolean
) {
    val zone = timeZone.zone()
    val currentHour by produceState(timeZone.hour(localZone = zone), isToday, zone) {
        while (isToday) {
            val now = ZonedDateTime.now(zone)
            value = timeZone.hour(now.toInstant(), zone)
            val nextHour = now.truncatedTo(ChronoUnit.HOURS).plusHours(1)
            delay(Duration.between(now.toInstant(), nextHour.toInstant()).toMillis().coerceAtLeast(1_000))
        }
    }
    val date = info.date
    val solarSupported = date.year in 1900..2100
    val columns = listOf(
        GanzhiColumn("year", L.text("ui.ganzhi_year", khmer),
            if (solarSupported) ChineseZodiacCalculator.getYearPillar(date.year, date.monthValue, date.dayOfMonth) else null),
        GanzhiColumn("month", L.text("ui.ganzhi_month", khmer),
            if (solarSupported) ChineseZodiacCalculator.getMonthPillar(date.year, date.monthValue, date.dayOfMonth) else null),
        GanzhiColumn("day", L.text("ui.ganzhi_day_column", khmer), info.ganzhiDay),
    ) + if (isToday) listOf(GanzhiColumn("hour", L.text("ui.ganzhi_hour_column", khmer),
        ChineseZodiacCalculator.getHourPillarForDate(date.year, date.monthValue, date.dayOfMonth, currentHour))) else emptyList()
    val heading = "干支"
    val signLabel = L.text("ui.ganzhi_sign", khmer)
    val clashLabel = L.text("ui.ganzhi_clash", khmer)
    val animals = columns.map { column ->
        (column.pillar?.branch?.ganzhiAnimalLabel(khmer, useEmoji) ?: "—") to
            (column.pillar?.clashBranch?.ganzhiAnimalLabel(khmer, useEmoji) ?: "—")
    }
    val headingStyle = LocalTextStyle.current.copy(fontSize = 12.readableSp, fontWeight = FontWeight.Medium)
    val labelStyle = LocalTextStyle.current.copy(fontSize = 11.readableSp)
    val headerStyle = labelStyle.copy(fontWeight = FontWeight.Medium)
    val animalStyle = LocalTextStyle.current.copy(fontSize = (if (useEmoji) 19 else 12).readableSp)
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    fun textSize(value: String, style: TextStyle) =
        measurer.measure(value, style, maxLines = 1, softWrap = false).size
    val labelWidth = with(density) {
        maxOf(DetailSymbolSlot + DetailSymbolGap + textSize(heading, headingStyle).width.toDp(),
            textSize(signLabel, labelStyle).width.toDp(),
            textSize(clashLabel, labelStyle).width.toDp()) + 4.dp
    }
    val columnWidthFactor = if (khmer) 1.2f else 1f
    val columnWidths = columns.indices.map { index ->
        with(density) {
            maxOf(textSize(columns[index].label, headerStyle).width,
                textSize(animals[index].first, animalStyle).width,
                textSize(animals[index].second, animalStyle).width).toDp() + 8.dp
        } * columnWidthFactor
    }
    val headerHeight = with(density) {
        maxOf(textSize("☯️", LocalTextStyle.current.copy(fontSize = 15.readableSp)).height,
            textSize(heading, headingStyle).height,
            columns.maxOf { textSize(it.label, headerStyle).height }).toDp() + 8.dp
    }.coerceAtLeast(28.dp)
    val animalRowHeight = with(density) {
        maxOf(textSize(signLabel, labelStyle).height, textSize(clashLabel, labelStyle).height,
            animals.maxOf { maxOf(textSize(it.first, animalStyle).height,
                textSize(it.second, animalStyle).height) }).toDp() + 8.dp
    }.coerceAtLeast(34.dp)
    Column(Modifier.fillMaxWidth().testTag("ganzhi-table"), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth()) {
            Column(Modifier.width(labelWidth).testTag("ganzhi-row-labels")) {
                Row(Modifier.fillMaxWidth().height(headerHeight),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(DetailSymbolGap)) {
                    Box(Modifier.size(DetailSymbolSlot), contentAlignment = Alignment.Center) {
                        Text("☯️", fontSize = 15.readableSp)
                    }
                    Text(heading, Modifier.testTag("ganzhi-heading-label"),
                        fontSize = 12.readableSp, maxLines = 1, softWrap = false,
                        color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Text(signLabel, Modifier.fillMaxWidth().height(animalRowHeight).wrapContentHeight(),
                    fontSize = 11.readableSp, maxLines = 1, softWrap = false,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(clashLabel, Modifier.fillMaxWidth().height(animalRowHeight).wrapContentHeight(),
                    fontSize = 11.readableSp, maxLines = 1, softWrap = false,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(Modifier.weight(1f).horizontalScroll(rememberScrollState()).testTag("ganzhi-columns")) {
                columns.forEachIndexed { index, column ->
                    Column(Modifier.width(columnWidths[index])) {
                        Text(column.label, Modifier.fillMaxWidth().height(headerHeight)
                            .wrapContentHeight().testTag("ganzhi-header-${column.key}"),
                            fontSize = 11.readableSp, maxLines = 1, softWrap = false,
                            textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium)
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Text(animals[index].first, Modifier.fillMaxWidth().height(animalRowHeight)
                            .wrapContentHeight().testTag("ganzhi-sign-${column.key}"),
                            fontSize = (if (useEmoji) 19 else 12).readableSp,
                            maxLines = 1, softWrap = false, textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurface)
                        Text(animals[index].second, Modifier.fillMaxWidth().height(animalRowHeight)
                            .wrapContentHeight().testTag("ganzhi-clash-${column.key}"),
                            fontSize = (if (useEmoji) 19 else 12).readableSp,
                            maxLines = 1, softWrap = false, textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }
        if (!solarSupported) {
            Text(L.text("ui.ganzhi_solar_range", khmer), fontSize = 10.readableSp,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable private fun EventDialog(
    event: CalendarEvent,
    k: Boolean,
    zoneLabel: String,
    showCopyButtons: Boolean,
    showWesternZodiac: Boolean = true,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    val info = remember(event.date) { KhmerDateDetails.fromGregorian(event.date) }
    val lunar = info.lunar
    var deletePrompt by rememberSaveable(event.id) { mutableStateOf(false) }
    var showLearnMore by rememberSaveable(event.id) { mutableStateOf(false) }
    if (showLearnMore) {
        LearnMoreDialog(event, k) { showLearnMore = false }
        return
    }
    var deleteError by remember(event.id) { mutableStateOf(false) }
    if (deletePrompt && event.kind == EventKind.CUSTOM) {
        val scrollState = rememberScrollState()
        CalendarAlertDialog(onDismissRequest = { deletePrompt = false }, containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp,
            title = { Text(L.text(if (event.repeat != null) "repeat.delete_confirm" else "ui.delete_this_event.925263", k)) },
            text = {
                Column(
                    modifier = Modifier.verticalScrollbar(scrollState).verticalScroll(scrollState).padding(end = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(event.title(k))
                    if (deleteError) Text(L.text("ui.could_not_delete_the_event_please_try_again.c02587", k), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { deletePrompt = false }) { Text(L.text("ui.cancel.5bf834", k)) } },
            confirmButton = { TextButton(onClick = { runCatching(onDelete).onFailure { deleteError = true } }) { Text(L.text("ui.delete.4708f4", k), color = MaterialTheme.colorScheme.error) } })
        return
    }
    CalendarBasicAlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.padding(horizontal = 20.dp).widthIn(max = 520.dp).fillMaxWidth(),
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp,
        ) {
            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp))) {
                DetailsZodiacBackground(info, showWesternZodiac)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    var isSingleLine by remember(event.title(k)) { mutableStateOf(true) }
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            event.title(k),
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 18.readableSp,
                            lineHeight = 26.readableSp,
                            onTextLayout = { isSingleLine = it.lineCount == 1 }
                        )
                        if (showCopyButtons) CopyTextButton(event.title(k), L.text("ui.copy_event_title", k), L.text("ui.event_title_copied", k),
                            firstLineHeight = 26.readableSp)
                    }
                    Spacer(Modifier.height(if (showCopyButtons && isSingleLine) 2.dp else 12.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(14.dp))
                    val scrollState = rememberScrollState()
                    Column(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .verticalScrollbar(scrollState)
                            .verticalScroll(scrollState)
                            .padding(end = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(if (event.kind == EventKind.CUSTOM) 14.dp else 4.dp)) {
                            Text("${dateLabel(event.date, k)} ${number(event.date.year, k)}", fontWeight = FontWeight.Medium)
                            Text(
                                "${info.lunarSummary(k)}\n${L.text("ui.buddhist_era.ea617c", k)} ${number(lunar.buddhistYear, k)}",
                                lineHeight = 22.readableSp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            event.time?.let {
                                Text(
                                    "$it · $zoneLabel",
                                    fontWeight = FontWeight.Medium,
                                    color = if (event.kind == EventKind.CUSTOM) eventColor(event.kind) else LocalContentColor.current
                                )
                            }
                            if (event.notes.isNotBlank()) Text(event.notes)
                            event.repeat?.let { repeat ->
                                val frequencyLabel = if (repeat.frequency == RepeatFrequency.DAYS) {
                                    val count = CalendarWords.number(repeat.interval.toInt(), k)
                                    if (k) "រៀងរាល់ $count ថ្ងៃ" else "Every $count day${if (repeat.interval > 1) "s" else ""}"
                                } else {
                                    L.text("repeat.${repeat.frequency.key}", k)
                                }
                                Text(
                                    "$frequencyLabel · ${L.text("repeat.end", k)} ${repeatDateLabel(repeat.until, k)}",
                                    fontSize = 14.readableSp,
                                    lineHeight = 20.readableSp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        HorizontalDivider()
                        Text(if (event.basis == DateBasis.CALCULATED) L.text("rules.calculated_label", k) else kindLabel(event.kind, k),
                            color = eventColor(event.kind), fontWeight = FontWeight.SemiBold)
                        if (event.kind != EventKind.CUSTOM) {
                            val translatedTitle = if (k) event.titleEn else event.titleKm
                            val titleWithOriginYear = event.anniversaryBase
                                ?.let { "$translatedTitle (${number(it, !k)})" } ?: translatedTitle
                            Text(
                                titleWithOriginYear,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 14.readableSp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        val (description, isEngine) = when {
                            event.kind == EventKind.CUSTOM ->
                                L.text("ui.a_custom_event_saved_on_your_device.96d6e7", k) to false
                            event.kind == EventKind.HOLY_DAY ->
                                L.text("ui.a_buddhist_observance_on_the_8th_and_15th_waxing_days_t.4bac2c", k) to false
                            event.kind == EventKind.HOLIDAY && event.basis == DateBasis.OFFICIAL ->
                                L.text("ui.listed_in_cambodia_s_official_year_holiday_calendar.044398", k, "year" to number(event.date.year, k)) to false
                            else ->
                                L.text("events.engine_calculations", k) to true
                        }
                        Text(
                            description,
                            fontSize = if (isEngine) 12.readableSp else 14.readableSp,
                            lineHeight = if (isEngine) 18.readableSp else 23.readableSp,
                            color = if (isEngine) MaterialTheme.colorScheme.onSurfaceVariant else LocalContentColor.current,
                        )
                        if (event.kind == EventKind.HOLIDAY) {
                            val citation = if (k) (event.citationKm ?: event.citation ?: event.citationEn) else (event.citationEn ?: event.citation ?: event.citationKm)
                            if (!citation.isNullOrBlank()) {
                                Text(citation, fontSize = 12.readableSp, lineHeight = 18.readableSp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                    if (event.kind == EventKind.CUSTOM) {
                        if (event.repeat != null) {
                            FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                TextButton(onClick = { deleteError = false; deletePrompt = true }) { Text(L.text("repeat.delete_series", k), color = MaterialTheme.colorScheme.error) }
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.End)) {
                                    TextButton(onClick = onEdit) { Text(L.text("repeat.edit_series", k)) }
                                    TextButton(onClick = onDismiss) { Text(L.text("ui.close.7df7dc", k)) }
                                }
                            }
                        } else {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                TextButton(onClick = { deleteError = false; deletePrompt = true }) { Text(L.text("ui.delete.4708f4", k), color = MaterialTheme.colorScheme.error) }
                                Spacer(Modifier.weight(1f))
                                TextButton(onClick = onEdit) { Text(L.text("ui.edit.bbdcac", k)) }
                                TextButton(onClick = onDismiss) { Text(L.text("ui.close.7df7dc", k)) }
                            }
                        }
                    } else {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            TextButton(onClick = { showLearnMore = true }) {
                                LightBulbIcon(Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(L.text("ui.learn_more", k))
                            }
                            TextButton(onClick = onDismiss) { Text(L.text("ui.close.7df7dc", k)) }
                        }
                    }
                }
            }
        }
    }
}

@Composable private fun SourceLink(url: String, label: String = url, compact: Boolean = false) {
    val uriHandler = LocalUriHandler.current
    Text(
        label,
        modifier = Modifier.clickable(role = Role.Button) { uriHandler.openUri(url) },
        color = MaterialTheme.colorScheme.primary,
        textDecoration = TextDecoration.Underline,
        fontSize = (if (compact) 11 else 14).readableSp,
        lineHeight = (if (compact) 17 else 22).readableSp,
    )
}

@Composable private fun SourcesDialog(k: Boolean, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var license by rememberSaveable { mutableStateOf(false) }
    var urlDialogData by remember { mutableStateOf<Pair<String, String>?>(null) }
    val appLicenseText = remember {
        val notice = context.assets.open("NOTICE.txt").bufferedReader().use { it.readText() }
        val license = context.assets.open("app-LICENSE.txt").bufferedReader().use { it.readText() }
        "$notice\n\n$license"
    }
    val engineLicenseText = remember {
        val license = context.assets.open("engine-LICENSE.txt").bufferedReader().use { it.readText() }
        val notice = context.assets.open("engine-NOTICE.txt").bufferedReader().use { it.readText() }
        "$license\n\n$notice"
    }
    val orangeColor = if (MaterialTheme.colorScheme.surface.luminance() > .5f) Color(0xFFC45E00) else Color(0xFFFFB36B)
    val holidayText = L.text("about.public_holiday_source", k)
    val holidayCandidates = if (k) listOf("ឯកសារផ្លូវការរបស់រដ្ឋ", "គេហទំព័រផ្លូវការរបស់រដ្ឋាភិបាល") else listOf("official government publications", "official government websites")
    val holidayName = holidayCandidates.firstOrNull { holidayText.contains(it) } ?: holidayCandidates[0]
    val holidayTitle = L.text("about.government_websites_title", k)
    val holidayUrls = listOf(
        "https://library.ncdd.gov.kh/",
        "https://www.ocm.gov.kh/",
        "https://www.nbc.gov.kh/",
    ).joinToString("\n")
    val engineText = L.text("about.calendar_engine", k)
    val engineName = "Khmer Calendar Engine"
    val engineUrl = "https://github.com/RSG-KH/khmer-calendar-engine"
    val uriHandler = LocalUriHandler.current
    val linkColor = MaterialTheme.colorScheme.primary
    val holidaySourceAnnotated = remember(holidayText, holidayName, holidayTitle, linkColor) {
        buildAnnotatedString {
            val index = holidayText.indexOf(holidayName)
            if (index < 0) {
                append(holidayText)
            } else {
                append(holidayText.substring(0, index))
                withLink(
                    LinkAnnotation.Clickable(
                        tag = "holiday_source_url",
                        styles = TextLinkStyles(style = SpanStyle(
                            color = linkColor,
                            textDecoration = TextDecoration.Underline,
                            fontWeight = FontWeight.Medium
                        )),
                        linkInteractionListener = { urlDialogData = holidayTitle to holidayUrls }
                    )
                ) {
                    append(holidayName)
                }
                append(holidayText.substring(index + holidayName.length))
            }
        }
    }
    val engineSourceAnnotated = remember(engineText, linkColor, uriHandler) {
        buildAnnotatedString {
            val index = engineText.indexOf(engineName)
            if (index < 0) {
                append(engineText)
            } else {
                append(engineText.substring(0, index))
                withLink(
                    LinkAnnotation.Url(
                        url = engineUrl,
                        styles = TextLinkStyles(style = SpanStyle(
                            color = linkColor,
                            textDecoration = TextDecoration.Underline,
                            fontWeight = FontWeight.Medium
                        )),
                        linkInteractionListener = { uriHandler.openUri(engineUrl) }
                    )
                ) {
                    append(engineName)
                }
                append(engineText.substring(index + engineName.length))
            }
        }
    }
    val scrollState = rememberScrollState()
    CalendarAlertDialog(onDismissRequest = onDismiss, title = { Text(L.text("ui.calendar_sources.7f962e", k)) }, text = {
        Column(
            modifier = Modifier
                .verticalScrollbar(scrollState)
                .verticalScroll(scrollState)
                .padding(end = 4.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(L.text("ui.new_event_years_and_corrections_are_delivered_through_a.a6af2d", k), fontSize = 12.readableSp, color = orangeColor)
            Text(L.text("rules.source_summary", k), fontSize = 14.readableSp)
            Text(holidaySourceAnnotated, fontSize = 14.readableSp, lineHeight = 22.readableSp)
            Text(engineSourceAnnotated, fontSize = 14.readableSp, lineHeight = 22.readableSp)
            val licenseState = L.text(if (license) "ui.expanded" else "ui.collapsed", k)
            val headerColor = MaterialTheme.colorScheme.primary
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .clickable(role = Role.Button) { license = !license }
                    .semantics {
                        heading()
                        stateDescription = licenseState
                        if (license) collapse { license = false; true }
                        else expand { license = true; true }
                    }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Canvas(Modifier.size(16.dp)) {
                    val chevron = Path().apply {
                        if (license) {
                            moveTo(size.width * .25f, size.height * .375f)
                            lineTo(size.width * .5f, size.height * .625f)
                            lineTo(size.width * .75f, size.height * .375f)
                        } else {
                            moveTo(size.width * .375f, size.height * .25f)
                            lineTo(size.width * .625f, size.height * .5f)
                            lineTo(size.width * .375f, size.height * .75f)
                        }
                    }
                    drawPath(chevron, headerColor, style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round))
                }
                Text(L.text("ui.open_source_license.ab00af", k), color = headerColor,
                    fontSize = 14.readableSp, fontWeight = FontWeight.Medium)
                HorizontalDivider(Modifier.weight(1f), color = headerColor)
            }
            if (license) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "${L.text("app.name", k)} · Apache-2.0",
                        fontSize = 13.readableSp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        SelectionContainer {
                            Text(
                                text = appLicenseText,
                                fontSize = 11.readableSp,
                                lineHeight = 16.readableSp,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(14.dp).testTag("app-license-text")
                            )
                        }
                    }

                    Spacer(Modifier.height(4.dp))

                    Text(
                        "Khmer Calendar Engine · Apache-2.0 / MIT",
                        fontSize = 13.readableSp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        SelectionContainer {
                            Text(
                                text = engineLicenseText,
                                fontSize = 11.readableSp,
                                lineHeight = 16.readableSp,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(14.dp).testTag("engine-license-text")
                            )
                        }
                    }
                }
            }
        }
    }, confirmButton = { TextButton(onClick = onDismiss) { Text(L.text("ui.close.7df7dc", k)) } })

    urlDialogData?.let { (title, urlText) ->
        CalendarAlertDialog(
            onDismissRequest = { urlDialogData = null },
            title = { Text(title, fontSize = 16.sp, lineHeight = 22.sp) },
            text = {
                SelectionContainer {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        urlText.lines().forEach { url ->
                            Text(
                                url,
                                fontSize = 14.readableSp,
                                lineHeight = 20.readableSp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    context.getSystemService(ClipboardManager::class.java)
                        .setPrimaryClip(ClipData.newPlainText(title, urlText))
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                        Toast.makeText(context, L.text("ui.url_copied", k), Toast.LENGTH_SHORT).show()
                    }
                    urlDialogData = null
                }) {
                    Text(L.text("ui.copy", k))
                }
            },
            dismissButton = {
                TextButton(onClick = { urlDialogData = null }) {
                    Text(L.text("ui.close.7df7dc", k))
                }
            },
        )
    }
}

@Composable private fun EmptyEvents(k: Boolean) {
    Text(L.text("ui.no_matching_events_try_another_filter_or_search.57812b", k), Modifier.fillMaxWidth().padding(24.dp), textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
}
@Composable private fun ArrowButton(next: Boolean, enabled: Boolean, description: String, onClick: () -> Unit) {
    IconButton(onClick = onClick, enabled = enabled, modifier = Modifier.size(32.dp).semantics { contentDescription = description }) {
        Text(if (next) "›" else "‹", fontSize = 18.sp, lineHeight = 18.sp,
            color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outlineVariant,
            modifier = Modifier.offset(y = (-1.5).dp))
    }
}
@Composable private fun EventMark(kind: EventKind, color: Color, small: Boolean = false, sizeDp: Dp = if (small) 7.5.dp else 9.dp) {
    Canvas(Modifier.size(sizeDp)) {
        val dim = size.minDimension
        when (kind) {
            EventKind.HOLIDAY -> {
                drawCircle(color = color, radius = dim * 0.44f, center = Offset(size.width / 2f, size.height / 2f))
            }
            EventKind.HOLY_DAY -> {
                val triWidth = dim * 0.86f
                val triHeight = triWidth * 0.866f
                val xLeft = (size.width - triWidth) / 2f
                val yTop = (size.height - triHeight) / 2f
                val path = Path().apply {
                    moveTo(size.width / 2f, yTop)
                    lineTo(xLeft + triWidth, yTop + triHeight)
                    lineTo(xLeft, yTop + triHeight)
                    close()
                }
                drawPath(path, color = color)
            }
            EventKind.OBSERVANCE -> {
                val side = dim * 0.80f
                drawRoundRect(
                    color = color,
                    topLeft = Offset((size.width - side) / 2f, (size.height - side) / 2f),
                    size = Size(side, side),
                    cornerRadius = CornerRadius(side * 0.22f)
                )
            }
            EventKind.CUSTOM -> {
                val cx = size.width / 2f
                val cy = size.height / 2f + dim * 0.035f
                val outerR = dim * 0.48f
                val innerR = outerR * 0.44f
                val path = Path().apply {
                    for (i in 0 until 10) {
                        val angle = (-PI / 2.0 + i * PI / 5.0).toFloat()
                        val r = if (i % 2 == 0) outerR else innerR
                        val x = cx + r * cos(angle)
                        val y = cy + r * sin(angle)
                        if (i == 0) moveTo(x, y) else lineTo(x, y)
                    }
                    close()
                }
                drawPath(path, color = color)
            }
        }
    }
}
@Composable private fun Dot(color: Color, small: Boolean = false) { Box(Modifier.size(if (small) 4.dp else 6.dp).background(color, CircleShape)) }
@Composable private fun Legend(kind: EventKind, label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) { EventMark(kind, color, small = false); Text(label, fontSize = 10.readableSp, lineHeight = 14.readableSp, maxLines = 1, color = MaterialTheme.colorScheme.onSurfaceVariant) }
}

@Composable
private fun LearnMoreDialog(event: CalendarEvent, k: Boolean, onDismiss: () -> Unit) {
    val entry = remember(event.id) { RecurringEvents.knowledgeById[event.id] }
    val context = LocalContext.current
    CalendarBasicAlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.padding(horizontal = 20.dp).widthIn(max = 520.dp).fillMaxWidth(),
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp) {
            Column(Modifier.fillMaxWidth().padding(24.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LightBulbIcon(Modifier.size(18.dp), color = MaterialTheme.colorScheme.primary)
                    Text(
                        L.text("ui.learn_more", k),
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 18.readableSp,
                        lineHeight = 26.readableSp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Spacer(Modifier.height(14.dp))
                Text(event.title(k), fontSize = 16.readableSp, lineHeight = 22.readableSp, fontWeight = FontWeight.Medium)
                if (entry != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(if (k) entry.summaryKm else entry.summaryEn, fontSize = 14.readableSp, lineHeight = 23.readableSp)
                    Spacer(Modifier.height(12.dp))
                    Text(if (k) entry.summaryEn else entry.summaryKm, fontSize = 14.readableSp, lineHeight = 23.readableSp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.height(24.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { launchOnlineSearch(context, event, k) }) {
                        SearchIcon(Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(L.text("ui.search_online", k))
                        Spacer(Modifier.width(5.dp))
                        OpenInNewIcon(Modifier.size(14.dp), contentDescription = L.text("ui.opens_in_external_browser", k))
                    }
                    TextButton(onClick = onDismiss) { Text(L.text("ui.close.7df7dc", k)) }
                }
            }
        }
    }
}

@Composable
private fun LightBulbIcon(modifier: Modifier = Modifier, color: Color = LocalContentColor.current) {
    Canvas(modifier) {
        val s = size.width / 24
        drawCircle(color, radius = 5.4f * s, center = Offset(12f * s, 9f * s), style = Stroke(width = 1.9f * s, cap = StrokeCap.Round))
        drawLine(color, Offset(9.9f * s, 15.4f * s), Offset(9.9f * s, 17f * s), 1.9f * s, StrokeCap.Round)
        drawLine(color, Offset(14.1f * s, 15.4f * s), Offset(14.1f * s, 17f * s), 1.9f * s, StrokeCap.Round)
        drawLine(color, Offset(10.3f * s, 19.2f * s), Offset(13.7f * s, 19.2f * s), 1.9f * s, StrokeCap.Round)
        drawLine(color, Offset(11f * s, 9f * s), Offset(13f * s, 9f * s), 1.5f * s, StrokeCap.Round)
    }
}

/** Opens the search in a full-screen browser Custom Tab forced to Google AI (Gemini) mode; never opens a network connection inside this app. */
private fun launchOnlineSearch(context: Context, event: CalendarEvent, k: Boolean) {
    // Semantic, category-aware query: no raw dates (they steer AI mode toward dated posts), and
    // Cambodia-specific English searches append "Cambodia" so generic titles (Independence Day,
    // Constitution Day) do not collide with US or other countries' events.
    val category = RecurringEvents.knowledgeById[event.id]?.category
    val normalize: (String) -> String = { it.replace(Regex("\\s+"), " ").trim() }
    val query = if (k) {
        val title = normalize(event.titleKm)
        val suffix = when (category) {
            "unesco" -> "បេតិកភណ្ឌយូណេស្កូ ប្រវត្តិ"
            "national_history", "milestone" -> "ប្រវត្តិ សារៈសំខាន់"
            "royal" -> "ព្រះរាជពិធី ប្រវត្តិ"
            "lunar_buddhist", "cultural" -> "ប្រវត្តិ និងទំនៀមទម្លាប់"
            else -> "ប្រវត្តិ និងអត្ថន័យ"
        }
        if (title.isEmpty()) return else "$title $suffix"
    } else {
        val title = normalize(event.titleEn)
        if (title.isEmpty()) return
        val cambodiaSpecific = category in setOf("national_history", "royal", "unesco", "cultural", "lunar_buddhist", "milestone")
        val anchor = if (cambodiaSpecific) "Cambodia" else ""
        val suffix = when (category) {
            "unesco" -> "UNESCO heritage history"
            "national_history", "milestone" -> "history and significance"
            "royal" -> "royal ceremony history"
            "lunar_buddhist", "cultural" -> "tradition and history"
            else -> "history and significance"
        }
        listOf(title, anchor, suffix).filter { it.isNotBlank() }.joinToString(" ")
    }
    val searchUri = "https://www.google.com/search".toUri()
        .buildUpon()
        .appendQueryParameter("q", query)
        .appendQueryParameter("hl", if (k) "km" else "en") // Enforce the app's selected language for the UI and the AI summary.
        .appendQueryParameter("udm", "50")
        .build()
    val customTabsIntent = CustomTabsIntent.Builder()
        .setShowTitle(true)
        .setShareState(CustomTabsIntent.SHARE_STATE_ON)
        .build()
    if (context !is android.app.Activity) {
        customTabsIntent.intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    try {
        customTabsIntent.launchUrl(context, searchUri)
    } catch (e: Exception) {
        try {
            val fallbackIntent = Intent(Intent.ACTION_VIEW, searchUri).apply {
                if (context !is android.app.Activity) {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
            context.startActivity(fallbackIntent)
        } catch (ex: Exception) {
            Toast.makeText(context, L.text("ui.no_browser_or_search_app", k), Toast.LENGTH_SHORT).show()
        }
    }
}

@Composable
private fun OpenInNewIcon(modifier: Modifier = Modifier, color: Color = LocalContentColor.current, contentDescription: String? = null) {
    Canvas(modifier.semantics { contentDescription?.let { this.contentDescription = it } }) {
        val s = size.width / 24
        fun line(x1: Float, y1: Float, x2: Float, y2: Float) = drawLine(color, Offset(x1 * s, y1 * s), Offset(x2 * s, y2 * s), 1.9f * s, StrokeCap.Round)
        line(4f, 11f, 4f, 20f)
        line(4f, 20f, 13f, 20f)
        line(10.5f, 13.5f, 19.2f, 4.8f)
        line(13.6f, 4.8f, 19.2f, 4.8f)
        line(19.2f, 4.8f, 19.2f, 10.4f)
    }
}

@Composable
private fun SearchIcon(modifier: Modifier = Modifier, color: Color = LocalContentColor.current) {
    Canvas(modifier) {
        val scale = size.width / 24
        drawCircle(color, radius = 6.2f * scale, center = Offset(13.8f * scale, 10.2f * scale), style = Stroke(width = 1.9f * scale, cap = StrokeCap.Round))
        drawLine(color, Offset(9.42f * scale, 14.58f * scale), Offset(4.5f * scale, 19.5f * scale), strokeWidth = 1.9f * scale, cap = StrokeCap.Round)
    }
}

/** Small original vector marks, with no icon font or image download. */
@Composable private fun AppIcon(index: Int, color: Color = LocalContentColor.current) {
    Canvas(Modifier.size(22.dp)) {
        val scale = size.width / 24
        fun line(x: Float, y: Float, x2: Float, y2: Float) = drawLine(color, Offset(x * scale, y * scale), Offset(x2 * scale, y2 * scale), 1.7f * scale, StrokeCap.Round)
        when (index) {
            0 -> {
                drawRoundRect(color, Offset(3 * scale, 5 * scale), Size(18 * scale, 16 * scale), CornerRadius(3 * scale), style = Stroke(1.7f * scale))
                line(3f, 10f, 21f, 10f); line(8f, 3f, 8f, 7f); line(16f, 3f, 16f, 7f)
                drawCircle(color, 1.5f * scale, Offset(8 * scale, 15 * scale)); line(13f, 15f, 17f, 15f)
            }
            1 -> { for (y in listOf(6f, 12f, 18f)) { drawCircle(color, 1.3f * scale, Offset(4 * scale, y * scale)); line(9f, y, 21f, y) } }
            else -> {
                line(4f, 6f, 20f, 6f); line(4f, 12f, 20f, 12f); line(4f, 18f, 20f, 18f)
                drawCircle(color, 2.8f * scale, Offset(9 * scale, 6 * scale)); drawCircle(color, 2.8f * scale, Offset(16 * scale, 12 * scale)); drawCircle(color, 2.8f * scale, Offset(9 * scale, 18 * scale))
            }
        }
    }
}
