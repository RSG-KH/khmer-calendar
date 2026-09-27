// Copyright (c) 2026 RSG-KH | Apache-2.0 License
package com.rsgkh.calendar.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rsgkh.calendar.data.Birthplace
import com.rsgkh.calendar.i18n.L

@Composable
internal fun SavedLocationChips(
    places: List<Birthplace>, k: Boolean,
    onSelect: (Birthplace) -> Unit, onRemove: (Birthplace) -> Unit,
) {
    val scroll = rememberScrollState()
    val lineHeight = 18.readableSp
    // Compact, uniform rows keep the viewport at three rows and grow for larger text.
    val rowHeight = maxOf(34.dp, with(LocalDensity.current) { lineHeight.toDp() } + 4.dp)
    val gap = 6.dp
    FlowRow(
        modifier = Modifier.fillMaxWidth().heightIn(max = rowHeight * 3 + gap * 2)
            .testTag("saved-location-chips").verticalScrollbar(scroll).verticalScroll(scroll)
            .padding(end = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(gap),
        verticalArrangement = Arrangement.spacedBy(gap),
    ) {
        places.forEach { place ->
            key(place.source, place.id) {
                Surface(
                    modifier = Modifier.height(rowHeight),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.weight(1f, fill = false).widthIn(min = 48.dp).fillMaxHeight()
                                .clickable(role = Role.Button) { onSelect(place) }
                                .padding(start = 12.dp, end = 2.dp),
                            contentAlignment = Alignment.CenterStart,
                        ) {
                            Text(
                                listOf(place.label, place.countryCode).filter(String::isNotBlank).joinToString(" · "),
                                color = MaterialTheme.colorScheme.primary, fontSize = 12.readableSp,
                                lineHeight = lineHeight, maxLines = 1, overflow = TextOverflow.Ellipsis,
                            )
                        }
                        IconButton(
                            onClick = { onRemove(place) },
                            modifier = Modifier.width(36.dp).fillMaxHeight().semantics {
                                contentDescription = "${L.text("ui.delete.4708f4", k)} ${place.label}"
                            },
                        ) { Text("×", color = MaterialTheme.colorScheme.error) }
                    }
                }
            }
        }
    }
}
