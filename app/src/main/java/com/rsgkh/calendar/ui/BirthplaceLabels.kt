// Copyright (c) 2026 RSG-KH | Apache-2.0 License
package com.rsgkh.calendar.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.rsgkh.calendar.data.Birthplace
import com.rsgkh.calendar.data.BirthplaceCatalog
import com.rsgkh.calendar.data.PlaceDivision
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
internal fun rememberBirthplaceLabel(place: Birthplace, khmer: Boolean): String {
    if (place.source != "CambodiaDivisions" || place.countryCode != "KH") return place.label
    val context = LocalContext.current.applicationContext
    val catalog = remember(context) { BirthplaceCatalog(context) }
    return key(place.id) {
        // Resolve both names once per place; a language change needs no additional asset read.
        // The effect is cancelled with the row, and a new place cannot show the previous name.
        val division by produceState<PlaceDivision?>(null, catalog, place.id) {
            value = withContext(Dispatchers.IO) {
                runCatching { catalog.divisions("KH").divisions.firstOrNull { it.id == place.id } }.getOrNull()
            }
        }
        division?.label(khmer) ?: place.label
    }
}
