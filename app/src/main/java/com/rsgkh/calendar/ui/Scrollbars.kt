// Copyright (c) 2026 RSG-KH | Apache-2.0 License
package com.rsgkh.calendar.ui

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Adds a modern, subtle vertical scrollbar that only appears when content overflows.
 */
@Composable
fun Modifier.verticalScrollbar(
    state: ScrollState,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
    width: Dp = 4.dp,
    paddingEnd: Dp = 2.dp,
    minThumbHeight: Dp = 24.dp,
): Modifier = this.drawWithContent {
    drawContent()
    val maxValue = state.maxValue
    if (maxValue > 0 && size.height > 0) {
        val totalHeight = size.height + maxValue
        val thumbHeightPx = (size.height / totalHeight * size.height)
            .coerceIn(minThumbHeight.toPx(), size.height)
        val scrollableRange = size.height - thumbHeightPx
        val thumbOffset = (state.value.toFloat() / maxValue.toFloat()) * scrollableRange

        val widthPx = width.toPx()
        val paddingEndPx = paddingEnd.toPx()
        val x = size.width - widthPx - paddingEndPx

        drawRoundRect(
            color = color,
            topLeft = Offset(x, thumbOffset.coerceIn(0f, scrollableRange)),
            size = Size(widthPx, thumbHeightPx),
            cornerRadius = CornerRadius(widthPx / 2f, widthPx / 2f)
        )
    }
}

/**
 * Vertical scrollbar for LazyVerticalGrid, shown only when content overflows the viewport.
 */
@Composable
fun Modifier.verticalScrollbar(
    state: LazyGridState,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
    width: Dp = 4.dp,
    paddingEnd: Dp = 2.dp,
    minThumbHeight: Dp = 24.dp,
): Modifier = this.drawWithContent {
    drawContent()
    val layoutInfo = state.layoutInfo
    val totalItems = layoutInfo.totalItemsCount
    val visibleItems = layoutInfo.visibleItemsInfo
    if (totalItems > 0 && visibleItems.isNotEmpty() && size.height > 0) {
        val firstItem = visibleItems.first()
        val lastItem = visibleItems.last()
        val visibleCount = lastItem.index - firstItem.index + 1
        if (visibleCount < totalItems) {
            val totalEstimated = size.height * (totalItems.toFloat() / visibleCount.toFloat())
            val thumbHeightPx = (size.height / totalEstimated * size.height)
                .coerceIn(minThumbHeight.toPx(), size.height)
            val scrollableRange = size.height - thumbHeightPx
            val maxIndex = (totalItems - visibleCount).coerceAtLeast(1)
            val progress = (firstItem.index.toFloat() / maxIndex.toFloat()).coerceIn(0f, 1f)
            val thumbOffset = progress * scrollableRange

            val widthPx = width.toPx()
            val paddingEndPx = paddingEnd.toPx()
            val x = size.width - widthPx - paddingEndPx

            drawRoundRect(
                color = color,
                topLeft = Offset(x, thumbOffset.coerceIn(0f, scrollableRange)),
                size = Size(widthPx, thumbHeightPx),
                cornerRadius = CornerRadius(widthPx / 2f, widthPx / 2f)
            )
        }
    }
}

/**
 * Vertical scrollbar for LazyColumn, shown only when content overflows the viewport.
 */
@Composable
fun Modifier.verticalScrollbar(
    state: LazyListState,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
    width: Dp = 4.dp,
    paddingEnd: Dp = 2.dp,
    minThumbHeight: Dp = 24.dp,
): Modifier = this.drawWithContent {
    drawContent()
    val layoutInfo = state.layoutInfo
    val totalItems = layoutInfo.totalItemsCount
    val visibleItems = layoutInfo.visibleItemsInfo
    if (totalItems > 0 && visibleItems.isNotEmpty() && size.height > 0) {
        val firstItem = visibleItems.first()
        val lastItem = visibleItems.last()
        val visibleCount = lastItem.index - firstItem.index + 1
        if (visibleCount < totalItems) {
            val totalEstimated = size.height * (totalItems.toFloat() / visibleCount.toFloat())
            val thumbHeightPx = (size.height / totalEstimated * size.height)
                .coerceIn(minThumbHeight.toPx(), size.height)
            val scrollableRange = size.height - thumbHeightPx
            val maxIndex = (totalItems - visibleCount).coerceAtLeast(1)
            val progress = (firstItem.index.toFloat() / maxIndex.toFloat()).coerceIn(0f, 1f)
            val thumbOffset = progress * scrollableRange

            val widthPx = width.toPx()
            val paddingEndPx = paddingEnd.toPx()
            val x = size.width - widthPx - paddingEndPx

            drawRoundRect(
                color = color,
                topLeft = Offset(x, thumbOffset.coerceIn(0f, scrollableRange)),
                size = Size(widthPx, thumbHeightPx),
                cornerRadius = CornerRadius(widthPx / 2f, widthPx / 2f)
            )
        }
    }
}
