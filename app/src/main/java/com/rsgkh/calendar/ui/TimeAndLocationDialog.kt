// Copyright (c) 2026 RSG-KH | Apache-2.0 License
package com.rsgkh.calendar.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.rsgkh.calendar.data.*
import com.rsgkh.calendar.domain.parseCoordinate
import com.rsgkh.calendar.domain.parseCoordinatePair
import com.rsgkh.calendar.i18n.L
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalTime
import java.time.ZoneId

internal enum class LocationPickerMode { BOTH, TIME, LOCATION }
internal fun timeAndLocationTitle(k: Boolean) = L.text("location.title", k)
internal fun placeFlag(code: String): String = if (code.matches(Regex("[A-Z]{2}")))
    code.map { String(Character.toChars(0x1F1E6 + it.code - 'A'.code)) }.joinToString("") else "📍"

/** Bounded lazy suggestions anchored above or below the focused field. */
@Composable private fun PlaceSearch(
    label: String, tag: String, value: String, choices: List<PlaceOption>, k: Boolean,
    showOnFocus: Boolean = false, onEdit: (String) -> Unit, onSelect: (PlaceOption) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    var edited by remember { mutableStateOf(false) }
    // Dialogs measure fields at intermediate widths before placing them. Retain the
    // final anchor width without feeding those measurements back into composition.
    val anchorWidth = remember { intArrayOf(1) }
    val windowWidth = LocalWindowInfo.current.containerSize.width
    LaunchedEffect(windowWidth) { expanded = false }
    val focus = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    val needle = if (edited) normalizedPlaceSearch(value) else ""
    val prepared = remember(choices) { PlaceSearchIndex(choices) }
    val matches = remember(prepared, needle) { prepared.matching(needle) }
    val provider = remember { object : PopupPositionProvider {
        override fun calculatePosition(anchorBounds: IntRect, windowSize: IntSize, layoutDirection: LayoutDirection, popupContentSize: IntSize): IntOffset {
            val x = anchorBounds.left.coerceIn(0, (windowSize.width - popupContentSize.width).coerceAtLeast(0))
            val y = if (anchorBounds.bottom + popupContentSize.height <= windowSize.height) anchorBounds.bottom
                else (anchorBounds.top - popupContentSize.height).coerceAtLeast(0)
            return IntOffset(x, y)
        }
    } }
    Box(Modifier.fillMaxWidth().onGloballyPositioned { anchorWidth[0] = it.size.width }) {
        OutlinedTextField(value, {
            edited = true; expanded = true; onEdit(it)
        }, modifier = Modifier.fillMaxWidth().testTag(tag).focusRequester(focusRequester).onFocusChanged {
            expanded = it.isFocused && (showOnFocus || edited)
        }, singleLine = true, label = { Text(label, fontSize = 13.readableSp) },
            textStyle = LocalTextStyle.current.copy(fontSize = 14.readableSp),
            trailingIcon = { if (value.isNotEmpty()) IconButton(onClick = {
                edited = true; expanded = true; onEdit(""); focusRequester.requestFocus()
            }, modifier = Modifier.semantics { contentDescription = "${L.text("ui.clear.7d76fd", k)} $label" }) { Text("×") } })
        if (expanded && (showOnFocus || needle.isNotEmpty())) Popup(provider, onDismissRequest = { expanded = false }, properties = PopupProperties(focusable = false)) {
            Surface(Modifier.width(with(LocalDensity.current) { anchorWidth[0].toDp() }).heightIn(max = 178.dp),
                shadowElevation = 6.dp, shape = MaterialTheme.shapes.small) {
                LazyColumn(Modifier.testTag("$tag-options")) {
                    if (matches.isEmpty()) item { Text(L.text("location.no_matches", k), Modifier.padding(12.dp), fontSize = 13.readableSp) }
                    items(matches, key = { it.id }) { option ->
                        Column(Modifier.fillMaxWidth().heightIn(min = 44.dp).clickable {
                            expanded = false; edited = false; focus.clearFocus(); onSelect(option)
                        }.padding(horizontal = 12.dp, vertical = 8.dp), verticalArrangement = Arrangement.Center) {
                            Text(option.label, fontSize = 13.readableSp)
                            option.detail?.let { Text(it, fontSize = 11.readableSp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        }
                    }
                }
            }
        }
    }
}

@Composable internal fun TimeAndLocationDialog(
    initialTime: LocalTime, initialPlace: Birthplace?, k: Boolean,
    mode: LocationPickerMode = LocationPickerMode.BOTH,
    onDismiss: () -> Unit, onSave: (LocalTime, Birthplace?) -> Unit,
) {
    val context = LocalContext.current
    val focus = LocalFocusManager.current
    val catalog = remember { BirthplaceCatalog(context) }
    val saved = remember { SavedBirthplaces(context) }
    var chips by remember { mutableStateOf(saved.read()) }
    var timeText by rememberSaveable { mutableStateOf(initialTime.withSecond(0).withNano(0).toString()) }
    var pickingTime by rememberSaveable { mutableStateOf(false) }
    var manual by rememberSaveable { mutableStateOf(initialPlace?.source == "manual") }
    var placeJson by rememberSaveable { mutableStateOf(initialPlace?.takeUnless { it.source == "manual" }?.toJson()) }
    var editingManualId by rememberSaveable { mutableStateOf(initialPlace?.takeIf { it.source == "manual" }?.id) }
    val place = remember(placeJson) { Birthplace.fromJson(placeJson) }
    var country by rememberSaveable { mutableStateOf(initialPlace?.countryCode.orEmpty()) }
    var adm1 by rememberSaveable { mutableStateOf("") }
    var countryText by rememberSaveable { mutableStateOf("") }
    var adm1Text by rememberSaveable { mutableStateOf("") }
    var lowerText by rememberSaveable { mutableStateOf("") }
    var name by rememberSaveable { mutableStateOf(if (manual) initialPlace?.label.orEmpty() else "") }
    var latitude by rememberSaveable { mutableStateOf(if (manual) initialPlace?.latitude?.toString().orEmpty() else "") }
    var longitude by rememberSaveable { mutableStateOf(if (manual) initialPlace?.longitude?.toString().orEmpty() else "") }
    var zone by rememberSaveable { mutableStateOf(if (manual) initialPlace?.timeZone.orEmpty() else "") }
    var countries by remember { mutableStateOf(emptyList<PlaceCountry>()) }
    var zones by remember { mutableStateOf(emptyList<Pair<String, String>>()) }
    var document by remember { mutableStateOf<PlaceDocument?>(null) }
    var countriesLoaded by remember { mutableStateOf(false) }
    var countriesFailed by remember { mutableStateOf(false) }
    var countryFailed by remember { mutableStateOf(false) }
    var selectionProblem by rememberSaveable { mutableStateOf<String?>(null) }
    val withLocation = mode != LocationPickerMode.TIME
    val withTime = mode != LocationPickerMode.LOCATION
    val countryChoices = remember(countries, k) { countryOptions(countries, k) }
    LaunchedEffect(withLocation, k) {
        if (withLocation) {
            countriesLoaded = false; countriesFailed = false
            val result = withContext(Dispatchers.IO) { runCatching { catalog.countries() to catalog.timeZones(k) } }
            result.onSuccess { countries = it.first; zones = it.second }.onFailure { countriesFailed = true }
            countriesLoaded = true
        }
    }
    LaunchedEffect(country, withLocation) {
        document = null
        countryFailed = false
        if (withLocation && country.isNotEmpty()) {
            val result = withContext(Dispatchers.IO) { runCatching { catalog.divisions(country) } }
            result.onSuccess { document = it }.onFailure { countryFailed = true }
        }
    }
    LaunchedEffect(document, placeJson, countries, k) {
        if (place != null && place.source != "manual") {
            countryText = countryChoices.firstOrNull { it.id == place.countryCode }?.label.orEmpty()
            val doc = document?.takeIf { it.code == place.countryCode } ?: return@LaunchedEffect
            val divisions = doc.divisions.associateBy { it.id }
            val chosen = divisions[place.id] ?: return@LaunchedEffect
            var top = chosen
            while (top.level > 1) { top = divisions[top.parentId] ?: break }
            adm1 = top.id; adm1Text = top.label(k)
            lowerText = if (chosen.level == 1) "" else if (chosen.level == 3)
                listOfNotNull(divisions[chosen.parentId]?.label(k), chosen.label(k)).joinToString(" · ") else chosen.label(k)
        }
    }
    fun chooseSaved(value: Birthplace) {
        focus.clearFocus(); selectionProblem = null
        if (value.source == "manual") {
            name = value.label; latitude = value.latitude.toString(); longitude = value.longitude.toString(); zone = value.timeZone
            editingManualId = value.id
        } else {
            placeJson = value.toJson(); country = value.countryCode
        }
    }
    fun removeSaved(value: Birthplace) {
        // Saved chips are shortcuts; deleting one must not change the active draft.
        saved.remove(value); chips = saved.read()
    }
    val adm1Choices = remember(document, k) { administrativeOptions(document, null, k) }
    val lowerChoices = remember(document, adm1, k) { if (adm1.isEmpty()) emptyList() else administrativeOptions(document, adm1, k) }
    val lat = parseCoordinate(latitude, true)
    val lon = parseCoordinate(longitude, false)
    val time = parseTimeInput(timeText)
    val manualValid = name.isNotBlank() && name.length <= 160 && lat != null && lon != null && runCatching { ZoneId.of(zone) }.isSuccess
    val locationValid = !withLocation || if (manual) manualValid else place != null
    val valid = locationValid && (!withTime || time != null)
    val title = when (mode) {
        LocationPickerMode.BOTH -> timeAndLocationTitle(k)
        LocationPickerMode.TIME -> L.text("ui.select_time.eacac3", k)
        LocationPickerMode.LOCATION -> L.text("location.location", k)
    }
    // A fixed available width keeps the native dialog's intrinsic measurement
    // from repeatedly remeasuring text fields at alternating widths.
    val dialogWidth = with(LocalDensity.current) { LocalWindowInfo.current.containerSize.width.toDp() - 32.dp }
        .coerceIn(1.dp, 560.dp)
    CalendarAlertDialog(onDismissRequest = onDismiss, modifier = Modifier.width(dialogWidth),
        properties = DialogProperties(usePlatformDefaultWidth = false),
        title = { Text(title, fontSize = 16.readableSp) }, text = {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).testTag("time-location-content"), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            HorizontalDivider()
            if (withTime) TimeInput(timeText, { timeText = it }, k, Modifier.fillMaxWidth().testTag("location-time"),
                isError = time == null, onPick = { focus.clearFocus(); pickingTime = true })
            if (withLocation) {
                val visibleChips = chips.filter { (it.source == "manual") == manual }
                if (visibleChips.isNotEmpty()) key(manual) {
                    SavedLocationChips(visibleChips, k, onSelect = ::chooseSaved, onRemove = ::removeSaved)
                }
                if (manual) {
                    fun coordinateEdit(value: String, isLatitude: Boolean) {
                        val pair = parseCoordinatePair(value)
                        if (pair != null) { latitude = pair.first.toString(); longitude = pair.second.toString() }
                        else if (isLatitude) latitude = value else longitude = value
                    }
                    for (field in listOf("name", "latitude", "longitude")) {
                        val label = L.text("location.$field", k)
                        val value = when (field) { "name" -> name; "latitude" -> latitude; else -> longitude }
                        val edit: (String) -> Unit = { if (field == "name") name = it else coordinateEdit(it, field == "latitude") }
                        OutlinedTextField(value, edit, modifier = Modifier.fillMaxWidth().testTag("location-$field"), singleLine = true,
                            label = { Text(label, fontSize = 13.readableSp) }, textStyle = LocalTextStyle.current.copy(fontSize = 14.readableSp),
                            trailingIcon = { if (value.isNotEmpty()) IconButton(onClick = { edit("") }, modifier = Modifier.semantics { contentDescription = "${L.text("ui.clear.7d76fd", k)} $label" }) { Text("×") } })
                    }
                    PlaceSearch(L.text("location.zone", k), "location-zone", zone,
                        remember(zones) { zones.map { PlaceOption(it.first, it.first, listOf(it.first.replace('_', ' '), it.second)) } }, k,
                        onEdit = { zone = it }, onSelect = { zone = it.id })
                } else {
                    // Reserve all three field slots while country data loads or choices are cleared.
                    Column(Modifier.heightIn(min = 204.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        PlaceSearch(L.text("location.country", k), "location-country", countryText, countryChoices, k,
                            onEdit = { countryText = it; country = ""; adm1 = ""; adm1Text = ""; lowerText = ""; placeJson = null; selectionProblem = null },
                            onSelect = { country = it.id; countryText = it.label; adm1 = ""; adm1Text = ""; lowerText = ""; placeJson = null; selectionProblem = null })
                        if (adm1Choices.isNotEmpty()) PlaceSearch(L.text(if (country == "KH") "location.capital_province" else "location.province_state", k),
                            "location-adm1", adm1Text, adm1Choices, k,
                            onEdit = { adm1Text = it; adm1 = ""; lowerText = ""; placeJson = null; selectionProblem = null },
                            onSelect = { option -> adm1 = option.id; adm1Text = option.label; lowerText = ""
                                placeJson = document?.let { doc -> doc.divisions.firstOrNull { it.id == option.id }?.let { doc.selection(it, k)?.toJson() } }
                                selectionProblem = if (placeJson == null && country != "KH") option.detail else null })
                        if (lowerChoices.isNotEmpty()) PlaceSearch(L.text("location.lower_divisions", k), "location-lower", lowerText, lowerChoices, k, showOnFocus = true,
                            onEdit = { lowerText = it; placeJson = null; selectionProblem = null }, onSelect = { option ->
                                lowerText = option.label
                                placeJson = document?.let { doc -> doc.divisions.firstOrNull { it.id == option.id }?.let { doc.selection(it, k)?.toJson() } }
                                selectionProblem = if (placeJson == null) option.detail else null
                            })
                    }
                    val status = when {
                        countriesFailed || countryFailed -> L.text("location.load_failed", k)
                        !countriesLoaded -> L.text("location.loading_countries", k)
                        country.isNotEmpty() && document == null -> L.text("location.loading_places", k)
                        document != null && adm1Choices.isEmpty() -> L.text("location.no_points", k)
                        else -> selectionProblem
                    }
                    status?.let { Text(it, Modifier.testTag("location-status"), fontSize = 12.readableSp,
                        color = if (countriesFailed || countryFailed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant) }
                }
            }
        }
    }, confirmButton = {
        LocationPickerActions(secondaryAction = {
            if (withLocation) TextButton(onClick = { focus.clearFocus(); manual = !manual }, modifier = Modifier.testTag("location-mode")) {
                Text(L.text(if (manual) "location.pick" else "location.custom", k))
            }
        }) {
            TextButton(onClick = onDismiss, modifier = Modifier.testTag("location-cancel")) { Text(L.text("ui.cancel.5bf834", k)) }
            TextButton(enabled = valid, onClick = {
                val selection = if (!withLocation) initialPlace else if (!manual) place
                    else Birthplace("manual", name.trim(), name.trim(), "", lat!!, lon!!, zone.trim())
                if (withLocation && selection != null) saved.save(selection, if (manual) editingManualId else null)
                onSave(if (withTime) time!! else initialTime, selection)
            }, modifier = Modifier.testTag("location-save")) { Text(L.text("ui.save.1b0623", k)) }
        }
    })
    if (pickingTime) TimeSelectionDialog(time ?: initialTime, k, onDismiss = { pickingTime = false },
        zoneLabel = null, onSelect = { timeText = it.toString(); pickingTime = false })
}

/** Keep the mode action left and the primary pair right; wrap the pair together. */
@Composable private fun LocationPickerActions(secondaryAction: @Composable () -> Unit, primaryActions: @Composable RowScope.() -> Unit) {
    Layout(modifier = Modifier.fillMaxWidth().padding(top = 10.dp).testTag("location-actions"), content = {
        Box { secondaryAction() }
        Row(content = primaryActions)
    }) { measurables, constraints ->
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val secondary = measurables[0].measure(loose)
        val primary = measurables[1].measure(loose)
        val gap = 8.dp.roundToPx()
        val wrap = secondary.width > 0 && secondary.width + gap + primary.width > constraints.maxWidth
        val height = if (wrap) secondary.height + gap + primary.height else maxOf(secondary.height, primary.height)
        layout(constraints.maxWidth, height) {
            secondary.placeRelative(0, if (wrap) 0 else (height - secondary.height) / 2)
            primary.placeRelative(constraints.maxWidth - primary.width, if (wrap) secondary.height + gap else (height - primary.height) / 2)
        }
    }
}
