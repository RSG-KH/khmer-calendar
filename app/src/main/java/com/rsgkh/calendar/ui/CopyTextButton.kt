// Copyright (c) 2026 RSG-KH | Apache-2.0 License
package com.rsgkh.calendar.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.rsgkh.calendar.R
import kotlinx.coroutines.delay

@Composable
internal fun CopyTextButton(text: String, label: String, copiedMessage: String, firstLineHeight: TextUnit) {
    val context = LocalContext.current
    var copied by remember(text, label) { mutableStateOf(false) }
    var copyRequest by remember(text, label) { mutableIntStateOf(0) }
    LaunchedEffect(text, label, copyRequest) {
        if (copyRequest > 0) {
            delay(2_000)
            copied = false
        }
    }
    val buttonOffsetY = with(LocalDensity.current) { (firstLineHeight.toDp() - 48.dp) / 2 }
    IconButton(
        onClick = {
            val clipboard = context.getSystemService(ClipboardManager::class.java)
            clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
            copied = true
            copyRequest++ // Another click restarts the confirmation period.
            // Android 13+ supplies its own clipboard confirmation.
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                Toast.makeText(context, copiedMessage, Toast.LENGTH_SHORT).show()
            }
        },
        // Move the whole button so the icon, ripple and touch target share one center.
        modifier = Modifier.offset(x = 12.dp, y = buttonOffsetY).size(48.dp).semantics {
            if (copied) {
                stateDescription = copiedMessage
                liveRegion = LiveRegionMode.Polite
            }
        },
    ) {
        Icon(
            painter = painterResource(if (copied) R.drawable.ic_check else R.drawable.ic_copy),
            contentDescription = label,
            modifier = Modifier.size(20.dp),
            tint = if (copied) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
