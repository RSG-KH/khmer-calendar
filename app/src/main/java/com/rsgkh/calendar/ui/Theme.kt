// Copyright (c) 2026 RSG-KH | Apache-2.0 License
package com.rsgkh.calendar.ui

import android.app.Activity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import androidx.core.view.WindowCompat
import com.rsgkh.calendar.data.*

val LocalFontScaleMultiplier = compositionLocalOf { 1.0f }

@Composable
fun ProvideDialogDensity(content: @Composable () -> Unit) {
    val currentDensity = LocalDensity.current
    val multiplier = LocalFontScaleMultiplier.current
    CompositionLocalProvider(
        LocalDensity provides Density(
            density = currentDensity.density,
            fontScale = currentDensity.fontScale * multiplier
        ),
        content = content
    )
}

@Composable
fun CalendarAlertDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: @Composable (() -> Unit)? = null,
    icon: @Composable (() -> Unit)? = null,
    title: @Composable (() -> Unit)? = null,
    text: @Composable (() -> Unit)? = null,
    shape: Shape = AlertDialogDefaults.shape,
    containerColor: Color = AlertDialogDefaults.containerColor,
    iconContentColor: Color = AlertDialogDefaults.iconContentColor,
    titleContentColor: Color = AlertDialogDefaults.titleContentColor,
    textContentColor: Color = AlertDialogDefaults.textContentColor,
    tonalElevation: Dp = AlertDialogDefaults.TonalElevation,
    properties: DialogProperties = DialogProperties()
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = { ProvideDialogDensity(confirmButton) },
        modifier = modifier,
        dismissButton = dismissButton?.let { d -> { ProvideDialogDensity(d) } },
        icon = icon?.let { i -> { ProvideDialogDensity(i) } },
        title = title?.let { t -> { ProvideDialogDensity(t) } },
        text = text?.let { x -> { ProvideDialogDensity(x) } },
        shape = shape,
        containerColor = containerColor,
        iconContentColor = iconContentColor,
        titleContentColor = titleContentColor,
        textContentColor = textContentColor,
        tonalElevation = tonalElevation,
        properties = properties
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarBasicAlertDialog(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    properties: DialogProperties = DialogProperties(),
    content: @Composable () -> Unit
) {
    BasicAlertDialog(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        properties = properties
    ) {
        ProvideDialogDensity(content)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarDatePickerDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: @Composable (() -> Unit)? = null,
    shape: Shape = DatePickerDefaults.shape,
    tonalElevation: Dp = DatePickerDefaults.TonalElevation,
    colors: DatePickerColors = DatePickerDefaults.colors(),
    properties: DialogProperties = DialogProperties(usePlatformDefaultWidth = false),
    content: @Composable ColumnScope.() -> Unit
) {
    DatePickerDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = { ProvideDialogDensity(confirmButton) },
        modifier = modifier,
        dismissButton = dismissButton?.let { d -> { ProvideDialogDensity(d) } },
        shape = shape,
        tonalElevation = tonalElevation,
        colors = colors,
        properties = properties,
        content = {
            ProvideDialogDensity {
                this.content()
            }
        }
    )
}

private class DropdownMenuEndPositionProvider(
    private val density: Density,
    private val offset: DpOffset = DpOffset(0.dp, 0.dp)
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize
    ): IntOffset {
        val margin = with(density) { 8.dp.roundToPx() }
        val offsetX = with(density) { offset.x.roundToPx() }
        val offsetY = with(density) { offset.y.roundToPx() }

        val targetX = if (layoutDirection == LayoutDirection.Ltr) {
            anchorBounds.right - popupContentSize.width + offsetX
        } else {
            anchorBounds.left + offsetX
        }
        val x = targetX.coerceIn(margin, (windowSize.width - popupContentSize.width - margin).coerceAtLeast(margin))

        val yBelow = anchorBounds.bottom + offsetY
        val y = if (yBelow + popupContentSize.height <= windowSize.height - margin) {
            yBelow
        } else {
            (anchorBounds.top - popupContentSize.height - offsetY).coerceAtLeast(margin)
        }
        return IntOffset(x, y)
    }
}

@Composable
fun CalendarDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    offset: DpOffset = DpOffset(0.dp, 0.dp),
    scrollState: ScrollState = rememberScrollState(),
    properties: PopupProperties = PopupProperties(focusable = true),
    shape: Shape = MenuDefaults.shape,
    containerColor: Color = MenuDefaults.containerColor,
    tonalElevation: Dp = MenuDefaults.TonalElevation,
    shadowElevation: Dp = MenuDefaults.ShadowElevation,
    border: BorderStroke? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    if (expanded) {
        val density = LocalDensity.current
        val positionProvider = remember(density, offset) { DropdownMenuEndPositionProvider(density, offset) }
        Popup(
            onDismissRequest = onDismissRequest,
            popupPositionProvider = positionProvider,
            properties = properties
        ) {
            ProvideDialogDensity {
                Surface(
                    modifier = modifier.widthIn(min = 112.dp, max = 280.dp),
                    shape = shape,
                    color = containerColor,
                    tonalElevation = tonalElevation,
                    shadowElevation = shadowElevation,
                    border = border
                ) {
                    Column(
                        modifier = Modifier
                            .padding(vertical = 8.dp)
                            .width(IntrinsicSize.Max)
                            .verticalScroll(scrollState)
                    ) {
                        content()
                    }
                }
            }
        }
    }
}

private const val RegularTextScale = 1.125f
private const val SmallTextScale = 1.1f
private fun textScale(size: Float) = RegularTextScale * if (size <= 11f) SmallTextScale else 1f
internal val Int.readableSp get() = (this * textScale(toFloat())).sp
private fun TextStyle.enlargeRegularText(): TextStyle {
    val scale = textScale(fontSize.value)
    return copy(fontSize = fontSize * scale, lineHeight = lineHeight * scale)
}
private val CalendarTypography = Typography().let { base ->
    base.copy(
        bodyLarge = base.bodyLarge.enlargeRegularText(),
        bodyMedium = base.bodyMedium.enlargeRegularText(),
        bodySmall = base.bodySmall.enlargeRegularText(),
        labelLarge = base.labelLarge.enlargeRegularText(),
        labelMedium = base.labelMedium.enlargeRegularText(),
        labelSmall = base.labelSmall.enlargeRegularText(),
        titleMedium = base.titleMedium.enlargeRegularText(),
        titleSmall = base.titleSmall.enlargeRegularText(),
    )
}

fun accentColor(accent: Accent, dark: Boolean = false): Color = when (accent) {
    Accent.BLUE -> if (dark) Color(0xFFA8BFFF) else Color(0xFF4564B5)
    Accent.LAVENDER -> if (dark) Color(0xFFCDB6FF) else Color(0xFF7755A8)
    Accent.ROSE -> if (dark) Color(0xFFFFB0C2) else Color(0xFFA84465)
    Accent.AMBER -> if (dark) Color(0xFFF1C17C) else Color(0xFF8A5E18)
    Accent.LIME -> if (dark) Color(0xFFCDDF70) else Color(0xFF647500)
}

@Composable
fun CalendarTheme(settings: AppSettings, content: @Composable () -> Unit) {
    val dark = when (settings.theme) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val accent = accentColor(settings.accent, dark)
    val colors = if (dark) darkColorScheme(
        primary = accent, onPrimary = Color(0xFF182139), primaryContainer = accent.copy(alpha = .14f),
        onPrimaryContainer = accent, background = Color(0xFF0C0E12), onBackground = Color(0xFFE9EAF0),
        surface = Color(0xFF1A1D24), onSurface = Color(0xFFE9EAF0),
        surfaceTint = Color.Transparent,
        surfaceDim = Color(0xFF11141A), surfaceBright = Color(0xFF343840),
        surfaceContainerLowest = Color(0xFF0C0E12), surfaceContainerLow = Color(0xFF16191F),
        surfaceContainer = Color(0xFF1A1D24), surfaceContainerHigh = Color(0xFF252932), surfaceContainerHighest = Color(0xFF303540),
        surfaceVariant = Color(0xFF252932), onSurfaceVariant = Color(0xFFA2A8B7),
        outline = Color(0xFF666D7D), outlineVariant = Color(0xFF303540),
        secondary = Color(0xFFECAF80), secondaryContainer = Color(0xFF382C24),
        tertiary = Color(0xFFEEA0A7), error = Color(0xFFEEA0A7),
    ) else lightColorScheme(
        primary = accent, onPrimary = Color.White, primaryContainer = accent.copy(alpha = .09f),
        onPrimaryContainer = accent, background = Color(0xFFF3F4F8), onBackground = Color(0xFF222632),
        surface = Color.White, onSurface = Color(0xFF222632), surfaceVariant = Color(0xFFF0F2F7),
        surfaceTint = Color.Transparent,
        surfaceDim = Color(0xFFE3E5EA), surfaceBright = Color.White,
        surfaceContainerLowest = Color.White, surfaceContainerLow = Color(0xFFF8F9FB),
        surfaceContainer = Color(0xFFF3F4F8), surfaceContainerHigh = Color(0xFFEEF0F4), surfaceContainerHighest = Color(0xFFE8EBF0),
        onSurfaceVariant = Color(0xFF6D7485), outline = Color(0xFF818899), outlineVariant = Color(0xFFE6E8F0),
        secondary = Color(0xFF99601D), secondaryContainer = Color(0xFFFFF3E4),
        tertiary = Color(0xFFB34B5D), error = Color(0xFFB34B5D),
    )
    val view = LocalView.current
    if (!view.isInEditMode) SideEffect {
        val window = (view.context as Activity).window
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = !dark
            isAppearanceLightNavigationBars = !dark
        }
    }
    val currentDensity = LocalDensity.current
    val fontMultiplier = settings.fontScale.multiplier
    CompositionLocalProvider(
        LocalFontScaleMultiplier provides fontMultiplier,
        LocalDensity provides Density(
            density = currentDensity.density,
            fontScale = currentDensity.fontScale * fontMultiplier
        )
    ) {
        MaterialTheme(colorScheme = colors, typography = CalendarTypography, content = content)
    }
}
