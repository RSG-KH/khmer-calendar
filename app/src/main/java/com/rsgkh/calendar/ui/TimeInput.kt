// Copyright (c) 2026 RSG-KH | Apache-2.0 License
package com.rsgkh.calendar.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.rsgkh.calendar.i18n.L
import java.time.LocalTime

internal fun parseTimeInput(value: String): LocalTime? =
    if (value.length == 5) runCatching { LocalTime.parse(value) }.getOrNull() else null

@Composable internal fun TimeInput(
    value: String, onValueChange: (String) -> Unit, k: Boolean,
    modifier: Modifier = Modifier, isError: Boolean = false, onPick: () -> Unit,
) {
    OutlinedTextField(value, { onValueChange(it.take(5)) }, modifier, singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge,
        label = { Text(L.text("ui.time_hh_mm.8cf351", k)) }, isError = isError,
        trailingIcon = { TextButton(onClick = onPick) { Text(L.text("ui.pick.971faf", k)) } })
}
