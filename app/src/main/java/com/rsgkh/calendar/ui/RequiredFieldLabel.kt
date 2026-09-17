// Copyright (c) 2026 RSG-KH | Apache-2.0 License
package com.rsgkh.calendar.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle

@Composable internal fun RequiredFieldLabel(label: String) {
    val accent = MaterialTheme.colorScheme.primary
    Text(buildAnnotatedString {
        append(label)
        append(" ")
        withStyle(SpanStyle(color = accent)) { append("*") }
    })
}
