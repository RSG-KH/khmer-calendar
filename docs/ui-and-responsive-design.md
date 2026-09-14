# UI & Responsive Design System

Khmer Calendar uses a custom responsive layout system built on Jetpack Compose and Material 3 design tokens. The interface adapts intelligently across compact phones, foldables, and large-screen tablets in both portrait and landscape orientations.

---

## 1. Adaptive Multi-Form-Factor Layouts

The application continuously evaluates configuration metrics (`smallestScreenWidthDp`, `orientation`, and window size classes) to deliver an optimized user experience.

| Form Factor & Orientation | Navigation Structure | Main Content Layout |
| :--- | :--- | :--- |
| **Phone Portrait** | Bottom Navigation Bar (64 dp) | Vertical scroll: Header → Month Grid → Today's Events / Details |
| **Phone Landscape** | Navigation Rail (full vertical distribution) | 2 Columns: Calendar (left, weight 0.9) + Monthly Events list (right, weight 1.1) |
| **Tablet Portrait** | Bottom Navigation Bar (64 dp) | Centered spacious grid with extended event preview cards |
| **Tablet Landscape** | Compact Navigation Rail (top-aligned) | 2 Columns: Calendar & Date Info Card (left, weight 1.0) + Month Events list (right, weight 1.0) |

---

### Phone Landscape: Full-Height Navigation Rail

In phone landscape mode, vertical space is constrained (~400 dp). Placing the navigation rail items at the top leaves awkward empty space at the bottom and produces small tap targets.

```kotlin
NavigationRail(
    modifier = Modifier.fillMaxHeight().testTag("navigation-rail"),
    containerColor = MaterialTheme.colorScheme.background,
    windowInsets = WindowInsets(0, 0, 0, 0)
) {
    if (isTablet) {
        Spacer(Modifier.height(8.dp))
    }
    listOf(Calendar, Events, Settings).forEachIndexed { index, title ->
        NavigationRailItem(
            modifier = if (!isTablet) Modifier.weight(1f) else Modifier,
            selected = page == index,
            onClick = { page = index },
            icon = { AppIcon(index) },
            label = { Text(title, fontSize = 11.readableSp) }
        )
    }
}
```

- **Phone Screens (`!isTablet`)**: Each `NavigationRailItem` receives `Modifier.weight(1f)`. The 3 tabs distribute evenly across the full vertical height, providing large, ergonomic thumb targets.
- **Tablet Screens (`isTablet`)**: Items remain unweighted with standard compact height and a top spacer, preserving the conventional tablet desktop rail aesthetic.

At tablet font settings of 130%, 140%, and 150%, navigation grows by 10%, 15%, and 20%, respectively. Landscape increases only the rail width (80 dp to 88/92/96 dp); portrait increases only the bottom bar height (64 dp to 70.4/73.6/76.8 dp). Phone navigation and tablet font settings of 120% or lower retain their existing dimensions. System insets are not scaled.

---

### Tablet Landscape: 2-Column Date Card

On tablets in landscape mode, the left column displays the month grid followed immediately by the selected date card. Rather than stacking all date attributes vertically, the card uses an efficient 2-column internal layout:

```
┌─────────────────────────────────────────────────────────────┐
│ 15 Koeut (Waxing) · Kattik                November 5, 2025  │
│ Year of the Snake · Saptasak                                │
│ Buddhist Era 2569                  ♏ Scorpio (Water · Pluto) │
└─────────────────────────────────────────────────────────────┘
```

- **Left Side**: Traditional Khmer lunar date, Animal year, Sak, and Buddhist Era rendered in regular font weight (`13.readableSp`, line height `18.readableSp`).
- **Right Side**: Gregorian date in secondary variant (`12.readableSp`) stacked above the Western Zodiac sign styled in the active theme accent color (`12.readableSp, FontWeight.Medium`).
- **Ergonomics**: Reduces card height by ~30%, eliminating empty dead space and leaving ample room for the "Events on the day" section directly below.

---

### Landscape Month Picker: 6x2 Grid

The Calendar month picker and Events year picker open as centered modal dialogs above the current tab. Cancel, Back, or tapping outside dismisses the dialog; the tab retains its current state. Both year fields include the supported range in the label, `Year (1800–2200)`, with the existing year validation. Picker content scrolls when available height is limited.

When jumping to a specific month in landscape mode:
- Arranged into a clean **6-column by 2-row grid** (January–June in row 1, July–December in row 2).
- Top padding is minimized, ensuring that the entire year selector and all 12 month buttons fit on screen without vertical clipping or awkward dialog scrolling.

### Headers and landscape spacing

The Events header retains its original Android layout: the title is on the left and the previous-year, year-picker, and next-year controls are on the right. The add action is a floating button at the bottom-right, inset 32 dp from the content edges, and stays visible while the list scrolls. The Events list keeps enough bottom padding for the final row to scroll above the button. Narrow calendar headers use abbreviated English month names (Jan–Dec), including when larger fonts reduce the available space.

Phone landscape calendar rows use a 44 dp base height. Tablet rows remain 52 dp in landscape and 64 dp in portrait; phone portrait rows remain 56 dp. Existing font scaling, cell gaps, and the 12 dp gap between landscape columns are retained. The landscape event list ends with 6 dp of content padding, inside the existing system insets.

### Detail backgrounds and credits

All event details share the date-details zodiac background: the animal illustration occupies 60% of the width at bottom-right; the Western zodiac occupies approximately 20% at bottom-left with a 20 dp inset. Both use the accent tint at 5% opacity in light mode and 3% in dark mode.

Settings shows the version on a separate line and groups clickable Android and PWA repository labels below it. The Sources dialog retains its original layout, inline links that open URL dialogs with a Copy action, and expandable MIT notice. Its source description omits the technical Buddhist Era rollover wording, which is documented in `calendar-source-review.md`.

---

## 2. Custom Components

### Dynamic Vertical Scrollbars (`Scrollbars.kt`)

Dialogs and popups display a vertical scrollbar only when content overflows its container, avoiding visual clutter when content fits naturally.

```kotlin
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
    if (maxValue > 0 && size.height > 0) { // strictly dynamic: only renders on overflow
        val totalHeight = size.height + maxValue
        val thumbHeightPx = (size.height / totalHeight * size.height)
            .coerceIn(minThumbHeight.toPx(), size.height)
        val scrollableRange = size.height - thumbHeightPx
        val thumbOffset = (state.value.toFloat() / maxValue.toFloat()) * scrollableRange
        val widthPx = width.toPx()
        drawRoundRect(
            color = color,
            topLeft = Offset(size.width - widthPx - paddingEnd.toPx(), thumbOffset.coerceIn(0f, scrollableRange)),
            size = Size(widthPx, thumbHeightPx),
            cornerRadius = CornerRadius(widthPx / 2f, widthPx / 2f)
        )
    }
}
```
- **Zero Recomposition Overhead**: Operates entirely within `drawWithContent`. Scrolling does not cause parent layout recompositions.
- **Support**: Available for standard `Column(Modifier.verticalScroll())` (`ScrollState`) and for `LazyVerticalGrid` / `LazyColumn` (`LazyGridState` / `LazyListState` overloads).

---

### Optical Mark Balancing (`EventMark`)

Calendar cells mark event occurrences with miniature geometric indicators:
- `HOLIDAY`: Filled circle.
- `HOLY_DAY`: Equilateral triangle.
- `OBSERVANCE`: Rounded rectangle.
- `CUSTOM`: 5-pointed star in vibrant red.

To prevent irregular geometries from appearing disproportionate or distorted by parent constraints, all shapes are strictly normalized to `dim = size.minDimension`:
- **Circle**: Radius = $0.44 \times \text{dim}$.
- **Triangle**: Base width = $0.86 \times \text{dim}$, Height = $\frac{\sqrt{3}}{2} \times \text{width} = 0.745 \times \text{dim}$.
- **Rounded Square**: Side = $0.80 \times \text{dim}$, Corner radius = $0.22 \times \text{side}$.
- **5-Pointed Star**: Outer radius $R = 0.48 \times \text{dim}$, Inner radius $r = 0.44 \times R$, optical vertical center offset $+0.035 \times \text{dim}$, 10 alternating radial vertices from angle $-\frac{\pi}{2}$.

All shapes are drawn with balanced optical center and volume, guaranteeing consistent visual weight side-by-side in day cells, event list rows, date details popups, and the calendar month legend.

---

## 3. Theming, Typography & Accessibility

### Color Palette & Accents
The app features deep, calibrated surface backgrounds for maximum battery efficiency and readability:
- **Dark Mode**: High-contrast slate `#0C0E12` with elevated surface `#161920`.
- **Light Mode**: Clean daylight `#F3F4F8` with pure white surface `#FFFFFF`.
- **Theme Accents**: 5 curated accent colors selectable in Settings:
  - **Blue** (Default)
  - **Lavender**
  - **Rose**
  - **Amber**
  - **Lime** (*បៃតងចាស់*)

### Dynamic Font Size Scaling
Users can adjust the application's base text scaling in **Settings → Appearance → Font size** across five phone presets:
- **80%** (Very Compact)
- **90%** (Compact)
- **100%** (Standard Default)
- **110%** (Comfortable)
- **120%** (Large)

Tablets (`smallestScreenWidthDp >= 600`) also offer **130%**, **140%**, and **150%**. The phone picker remains 80%–120%; rotating a device does not change its available range. Font choices are saved by enum name in the existing preferences.

The custom `.readableSp` extension automatically recalculates typographic tokens:
```kotlin
internal val Int.readableSp get() = (this * textScale(toFloat())).sp
```
This ensures legibility for elderly users or small screens while preserving fixed grid proportions and calendar cell alignments.
