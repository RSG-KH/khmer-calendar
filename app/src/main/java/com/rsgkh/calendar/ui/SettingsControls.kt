// Copyright (c) 2026 RSG-KH | Apache-2.0 License
package com.rsgkh.calendar.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** Shared title style, control alignment and row spacing for every preference. */
@Composable internal fun SettingsRow(title: String, subtitle: String? = null, enabled: Boolean = true, control: @Composable () -> Unit) {
    val hasSubtitle = !subtitle.isNullOrBlank()
    Row(Modifier.fillMaxWidth().heightIn(min = if (hasSubtitle) 64.dp else 48.dp).padding(vertical = if (hasSubtitle) 8.dp else 4.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            SettingsTitle(title, enabled)
            subtitle?.let { Text(it, fontSize = 11.readableSp, lineHeight = 17.readableSp,
                color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface.copy(alpha = .38f)) }
        }
        control()
    }
}

@Composable internal fun SettingsTitle(title: String, enabled: Boolean = true) {
    Text(title, fontSize = 14.readableSp, lineHeight = 22.readableSp, fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (enabled) 1f else .38f))
}

@Composable internal fun SettingSwitch(title: String, subtitle: String? = null, checked: Boolean, onChange: (Boolean) -> Unit) {
    SettingsRow(title, subtitle) {
        Switch(checked = checked, onCheckedChange = onChange, modifier = Modifier.semantics { contentDescription = title })
    }
}

@Composable internal fun SettingChoice(
    label: String,
    selected: Boolean,
    contentPadding: PaddingValues = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
    onClick: () -> Unit
) {
    TextButton(onClick = onClick, modifier = Modifier.defaultMinSize(minWidth = 1.dp).semantics { this.selected = selected },
        shape = RoundedCornerShape(10.dp), contentPadding = contentPadding,
        colors = ButtonDefaults.textButtonColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else androidx.compose.ui.graphics.Color.Transparent,
            contentColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)) {
        Text(label, fontSize = 13.readableSp)
    }
}

@Composable internal fun <T> SettingDropdown(value: T, choices: List<T>, label: (T) -> String,
    tag: String, description: String, enabled: Boolean = true,
    itemColor: ((T, Boolean) -> Color)? = null,
    onSelect: (T) -> Unit) {
    var expanded by remember(enabled) { mutableStateOf(false) }
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    Box {
        TextButton(onClick = { expanded = true }, enabled = enabled, modifier = Modifier.testTag(tag).widthIn(max = 192.dp)
            .semantics { contentDescription = description }, contentPadding = PaddingValues(horizontal = 8.dp)) {
            Text(label(value), fontSize = 13.readableSp, modifier = Modifier.weight(1f, fill = false),
                color = itemColor?.invoke(value, isDark) ?: Color.Unspecified,
                fontWeight = if (itemColor != null) FontWeight.Medium else FontWeight.Normal)
        }
        CalendarDropdownMenu(
            expanded = enabled && expanded,
            onDismissRequest = { expanded = false },
            containerColor = if (isDark) Color(0xFF242833) else MenuDefaults.containerColor,
            border = if (isDark) BorderStroke(1.dp, Color(0xFF363B48)) else null
        ) {
            choices.forEach { choice ->
                DropdownMenuItem(text = {
                    Text(label(choice), fontSize = 14.readableSp,
                        color = itemColor?.invoke(choice, isDark) ?: Color.Unspecified,
                        fontWeight = if (itemColor != null) FontWeight.Medium else FontWeight.Normal)
                },
                    modifier = Modifier.height(42.dp).semantics { selected = choice == value },
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                    onClick = { onSelect(choice); expanded = false })
            }
        }
    }
}
