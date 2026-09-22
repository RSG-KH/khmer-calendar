# UI & Responsive Design System

Khmer Calendar uses a custom responsive layout system built on Jetpack Compose and Material 3 design tokens. The interface adapts intelligently across compact phones, foldables, and large-screen tablets in both portrait and landscape orientations.

### Weekday headings

Settings → Calendar offers two independent options: longer names default off, while weekday colors default on. Existing saved choices are preserved.

- **Show longer weekday names** uses three-letter English headings and full Khmer names without repeating the word “day.” All seven headings share a font size that fits the longest name in the available column width.
- **Highlight weekday names** colors the calendar headings using the [Cambodian Development Foundation's traditional weekday color mapping](https://cambodiandevelopmentfoundation.org/7-colors-of-the-week/): Monday yellow, Tuesday violet/purple, Wednesday light green, Thursday green, Friday baby blue, Saturday dark purple and Sunday bright red.

`WeekdayColors.kt` defines readable shades for light and dark calendar surfaces. These are app color choices within the cited families; the source does not specify hex values. Colors follow the weekday when the start of the week changes and apply to both short and longer headings. The Sunday-column setting independently controls date-number highlighting. When weekday colors are off, that setting also controls the Sunday heading as before.

### Today and the selected date

Today always keeps its solid accent background and contrasting text and markers, even when another date is selected. A selected non-Today date has an accent-colored border with its usual background, text and event-marker colors. This distinction applies in every month and year and in both themes; selecting a date still updates the detail view and accessibility selection state.

Swiping, using the month arrows, or choosing a month and year selects day 1 of the destination month. Tapping Today selects the actual current date.

### Copy feedback

When Show copy buttons is enabled, date and event details display a copy icon aligned with the first text line near the right edge. The button retains a 48 dp tap area. After copying, an accent-colored checkmark appears for two seconds, then returns to the copy icon. Copying again restarts the timer. The confirmation is also exposed to accessibility services using the translated copied message.

### Western zodiac visibility

**Settings → Calendar → Show Western zodiac signs** is on by default and saved on the device. Turning it off removes the Western zodiac line from the selected-date card and from date details — the divider above that block also disappears unless a Buddhist holy day or shaving day is shown — and omits the zodiac glyph from event-detail backgrounds while keeping the animal-year artwork. Month-grid cells never show zodiac signs. An inverted `hideWesternZodiac` choice saved by an earlier build carries over when the setting is next written.

### Custom event repeats

`SelectionChip` defines the shared appearance for Events filters and Repeat choices:
unselected chips use the theme outline at 45% opacity, and selected chips use a 1 dp
accent border with the accent container and label colors. Both screens use this
component so light and dark theme borders remain consistent.

The event editor keeps Android's Material filter chips, switches, date picker and
time selection dialog. Date is labeled without a fixed format; manual input still
uses ISO dates, and Pick opens the existing calendar control. The selected date's
time-zone offset appears in accent color beside the title. Title and End by use an
asterisk; Notes has no optional suffix and occupies two lines,
followed by a divider and Repeat, without an enclosing border.

None hides repeat inputs and the schedule preview. Days defaults to 3 in an outlined
number field with a floating label; Weekly, Monthly and Yearly share the required End by field. The
end-date picker excludes dates before the start. Missing month-end options appear
only when affected dates fall within the chosen interval. Independent switches
include day 30 or February's last day; leaving them off skips those missing dates.
These two rows use regular-weight labels and compact spacing while retaining native
switch tap targets. Settings rows keep their existing styling and spacing.
The preview shows the first dates, final date, count and skipped months/years.
Repeat labels and messages match the PWA's English and Khmer text.

Repeat chips stay in one horizontally scrollable row, matching the Events filters.
Preview dates wrap on narrow screens. Series detail actions also wrap to
accommodate Khmer labels. Delete series stays on the left, with Edit series and
Close grouped on the right. Edit series and Delete series apply to all occurrences;
the delete confirmation makes that scope explicit.

### Holy-day lotus artwork

Calendar cells and date details use the supplied 300×300 PNGs unchanged: `lutos_03_300x300.png` as `holy_day_lotus.png` for day 8 of either lunar phase, and `lutos_03_blossom_300x300.png` as `holy_day_lotus_blossom.png` for the final holy day on day 14 or 15. The engine's holy-day flag controls which dates show a grid lotus, including 14 Roach in short months. Date-details shaving-day icons use the same lotus as the following holy day: closed before day 8 and blossomed before the phase end. Grid artwork keeps its 25% opacity and existing cell sizing; date-details icons remain 24 dp.

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

### Edge-to-edge and system insets

`MainActivity` calls `WindowCompat.setDecorFitsSystemWindows(window, false)` and sets the display cutout mode to `ALWAYS`, supported throughout the app's API 31+ range. The existing compact layout uses Scaffold's default content insets, 68% of its top padding in landscape, and half the bottom navigation-bar inset in portrait. The custom-event editor uses `imePadding()` for the keyboard.

The Compose theme updates system icon brightness with `WindowInsetsControllerCompat`. For Android 12–14, the XML theme sets transparent status/navigation bars and disables status-bar contrast enforcement. The `values-v35` theme inherits the shared light/dark base directly, without those legacy overrides; Android 15+ supplies the edge-to-edge bar backgrounds. Navigation-bar contrast enforcement remains enabled for three-button navigation. This follows [Android's manual setup guidance](https://developer.android.com/develop/ui/views/layout/edge-to-edge-manually) for older versions. See also [Android's Compose inset guidance](https://developer.android.com/develop/ui/compose/system/insets-ui) and [Android 15's edge-to-edge requirements](https://developer.android.com/about/versions/15/behavior-changes-15#edge-to-edge).

#### Play Console deprecation warning

The detailed Play report for release **9 (0.1.8)** names `x00.a` and `w00.b`. The archived release mapping, its matching APK and the mapping embedded in the release bundle identify both callers:

| Play caller | Original AndroidX method | Flagged reference |
| --- | --- | --- |
| `x00.a` | `androidx.activity.EdgeToEdgeApi29.setUp` | `Window.setStatusBarColor`, `Window.setNavigationBarColor` |
| `w00.b` | `androidx.activity.EdgeToEdgeApi28.adjustLayoutInDisplayCutoutMode` | `LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES` |

The release mapping ID is `13a3eb1d7f4a097f11ad9423f0a4ad077c330751c3d1dccb35667cd12d03f3f3`. Obfuscated names are specific to that build; do not decode this report using a newer build's mapping.

These methods belong to AndroidX Activity 1.10.1, used by `enableEdgeToEdge()`. Its API 30+ implementation overrides the cutout setting with `ALWAYS`; the older `SHORT_EDGES` method remains packaged even though the app's minimum API is 31. The report identifies packaged compatibility code and does not, by itself, demonstrate incorrect Android 15 layout.

The official [Activity 1.13.0 sources](https://dl.google.com/dl/android/maven2/androidx/activity/activity/1.13.0/activity-1.13.0-sources.jar) also contain all three references, including window-color calls in the API 35 implementation. An initial trial with Activity 1.13.0 and Core 1.18.0 retained them while `enableEdgeToEdge()` was still called. Updating dependencies alone does not eliminate the reported references.

On **2026-09-16**, the app replaced Activity's `enableEdgeToEdge()` helper with the API 31+ setup described above. Removing that call allows release optimization to remove its obsolete compatibility implementations. The subsequent lint cleanup updates Activity to 1.13.0 and Core to 1.19.0 while retaining this manual setup.

Local verification after the change:

- Both the optimized release APK and AAB have no DEX method references to `Window.setStatusBarColor` or `Window.setNavigationBarColor`.
- The release mapping contains no `androidx.activity.EdgeToEdge` implementations. The only DEX write to `layoutInDisplayCutoutMode` sets `3` (`ALWAYS`), not `1` (`SHORT_EDGES`); the APK and AAB contain identical DEX bytes.
- `EdgeToEdgeTest` passes six window/theme checks across API 31, 34 and 35 in light and dark mode, covering legacy transparency, navigation contrast, cutout mode and the Android 15 theme's independence from legacy overrides.
- `assembleRelease`, `bundleRelease` and `lintDebug` complete successfully. The subsequent lint cleanup addresses outdated dependencies, Kotlin extension usage and missing monochrome launcher layers.

This verifies removal of the reported bytecode references locally. Clearing the warning in Play Console still requires uploading and scanning a new release; the archived release 9 report is unchanged. Legacy bar-color attributes remain packaged for Android 12–14 and are not applied by the app's Android 15+ theme.

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

The Events header retains its original Android layout: the title is on the left and the previous-year, year-picker, and next-year controls are on the right. The add action is a floating button at the bottom-right, inset 32 dp from the content edges, and stays visible while the list scrolls. The Events list keeps enough bottom padding for the final row to scroll above the button. The search box and filter chips scroll with the list; once both have scrolled out of view, a floating search button fades in directly above the add button, matching its 48 dp size and primary color. Tapping it scrolls back to the top and focuses the search box, and it hides again once the search area is visible. Narrow calendar headers use abbreviated English month names (Jan–Dec), including when larger fonts reduce the available space.

Phone landscape calendar rows use a 44 dp base height. Tablet rows remain 52 dp in landscape and 64 dp in portrait; phone portrait rows remain 56 dp. Existing font scaling, cell gaps, and the 12 dp gap between landscape columns are retained. The landscape event list ends with 6 dp of content padding, inside the existing system insets.

### Detail backgrounds, layout and credits

All event details share the date-details zodiac background: the animal illustration occupies 60% of the width at bottom-right; the Western zodiac occupies approximately 20% at bottom-left with a 20 dp inset. Both use the accent tint at 5% opacity in light mode and 3% in dark mode.

Event detail popups (`EventDialog`) format information in a clear vertical structure:
- **Title**: Styled in the selected app Accent color (`MaterialTheme.colorScheme.primary`).
- **Date & Lunar Block**: The Gregorian date line (with Khmer day number prefix `ទី`, e.g., `ថ្ងៃអង្គារ ទី៨ ខែកញ្ញា ២០២៦`) is followed immediately by the traditional Khmer lunar details block (lunar phase, month, animal year, sak, and Buddhist Era).
- **Time**: Event time displayed below date details; custom events highlight time in Red (`CustomEventRed`).
- **Notes & Repeat Schedule**: Personal notes appear above the repeat schedule row (`Every {x} days · End by {date}` / `រៀងរាល់ {x} ថ្ងៃ · ផុតកំណត់ត្រឹមថ្ងៃ {date}`), rendered in matching 14 dp readable text size.
- **Category & Translation**: Shows event kind label and official citation if available; non-custom event translated titles are styled in bold (`FontWeight.Bold`).
- **Learn More Popup**: Built-in event dialogs show a 'Learn more' text button (light-bulb icon) on the left of the bottom action row, matching the day-details button styling. The popup opens with a 💡 'Learn more' / 'ស្វែងយល់បន្ថែម' header, the event title, then both curated knowledge summaries stacked — the app language first, the other language beneath in a muted tone. Its bottom row holds the relocated 'Search online' action and Close. The search query is category-aware (UNESCO heritage, royal ceremony, tradition, or history and significance phrases) and appends 'Cambodia' for Cambodia-specific categories in English so generic titles do not collide with other countries' events; events without a knowledge entry (holy days) open the popup with the search action only.
Built-in event popups use the short description “Calendar calculations by Khmer Calendar Engine.” in English and Khmer. This credits the calculations without treating the engine as the source of official holiday records. Public holidays, observances, calculated observances and holy days retain their category labels, and official holidays from the bundled government calendars show the citing subdecree or ministry reference as a secondary line in the detail popup; detailed event provenance is documented in the [reference event database guide](reference-event-database.md).

Settings shows the version on a separate line and groups clickable Android and PWA repository labels below it. The Sources dialog opens with an orange delivery note (new event years and corrections arrive through app updates), followed by the calculation-coverage summary, then two credited source paragraphs: official government holiday sources and the shared engine. The government source opens its list of calendar URLs in a dialog, while the engine name opens its repository directly. A collapsible “Open-source licenses” header has a leading chevron and trailing divider, all using the selected accent color; expanding it shows partitioned license sections with header attributes for both the app (including Khmer Calendar Manager catalog curation credit) and the engine (including upstream MIT notices), matching the PWA structure. It starts collapsed and announces its expanded or collapsed state to accessibility services. Calculation details belong in the engine documentation linked from [shared engine integration](shared-engine.md).

**Settings → Calendar → Show copy buttons** is off by default and saved on the device. When enabled, Date details can copy the displayed full date description and Event details can copy the displayed event title, including custom events. Both actions use the current language and Android's clipboard. Android 13+ shows the system clipboard confirmation; Android 12 shows a translated toast.

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

### Theme selection

**Settings → Appearance → Theme** offers **Light** and **Dark** chips styled like the language choices. Before a manual choice, the app follows the device theme and highlights the chip matching the current appearance. Tapping either chip, including the already highlighted one, saves a persistent override. Later system theme changes do not affect that choice. Existing saved Light or Dark preferences are preserved; the internal System default remains automatic until a chip is tapped.

### Themed launcher icons

Both adaptive launcher icons use `ic_launcher_monochrome.xml` for wallpaper-tinted icons. It wraps `drawable-nodpi/launcher_monochrome.png`, the supplied `khmer_calendar_app_transparent_mono_light_full.png` artwork copied without modification at 1254×1254, matching the full-color artwork's resolution. Its transparent lettering and temple cutouts preserve the app's Khmer calendar design. The wrapper uses the same 20% inset as the full-color foreground, keeping the artwork inside the adaptive-icon safe area.

### Color Palette & Accents
The **Background accent** switch follows **Accent color** under **Settings → Appearance**. It is enabled by default and persists across app restarts. When enabled, Calendar, Events and Settings blend the selected accent over the theme's base background. The bottom navigation bar, its system-navigation padding and the landscape navigation rail use the same opaque color. Changing the switch, accent or theme updates the background immediately; switching it off restores the base background.
- **Dark Mode**: Muted background shades have a subtle boost of approximately 10% in brightness and saturation: blue `#0A0F18`, lavender `#0D0D18`, rose `#110D14`, amber `#110F0F` and lime `#0A120D`. Lime keeps its clearer green shift. These are final background colors, without an additional darkening blend. The neutral background remains `#0C0E12` and card surfaces use `#1A1D24`.
- **Light Mode**: The accent overlays base background `#F3F4F8` at 10% opacity; card surfaces use `#FFFFFF`.
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

---

## 4. Home Screen Widgets & Adaptive Glance Layouts

### Widget Design & Responsive Layouts
Khmer Calendar provides three home screen widgets built with Jetpack Glance (`1.2.0`):

- **Month Widget (4×3 Target Size, Extendable to 4×4)**:
  - **Header Badges & Quick Action**: Left-aligned solar month name badge (`📅 មេសា` / `📅 Apr`), traditional year & BE year chip (`🐎 ឆ្នាំមមី · អដ្ឋស័ក · ព.ស. ២៥៧០`), mini timezone badge (shows full text on wide displays, collapses to emoji only `🇰🇭` / `🌐` on compact/phone displays to eliminate truncation ellipses), and right-aligned quick refresh action button.
  - **Full Calendar Table**: 7-column grid with traditional Khmer weekday colors (`អា`, `ច`, `អ`, `ពិ`, `ព្រ`, `សុ`, `ស`), Gregorian day numbers, holy-day lotus watermarks (`0.25f` opacity), and event markers (`●` holiday, `▲` holy day, `■` observance, `★` personal).
  - **Year Animal Background**: Mirrors the in-app calendar with a centered animal watermark across standard months, and dual-animal transition watermarks (old animal at top-start, new animal at bottom-end) during April (Khmer New Year).
  - **Footer Footnote Legend**: Centered footnote row (`● ថ្ងៃឈប់សម្រាក`, `▲ ថ្ងៃសីល`, `■ ពិធី និងទិវា`, `★ ផ្ទាល់ខ្លួន`). Automatically hides when available height is $< 195\,\text{dp}$ to preserve calendar grid legibility.
  - **Aspect Ratio Constraint**: Width is capped at a maximum of $1.25\times$ height (`minOf(height * 1.25f, 456.dp)`), preventing extreme horizontal stretching in tablet landscape mode.
  - **Height-Adaptive Layout**: Automatically detects compact landscape heights ($\le 250\,\text{dp}$) to tighten outer padding, dividers, header margins, and day number/marker offsets so numerals and markers never clip or overlap.
  - **Dynamic Zoom Gap**: Starting from 110% font zoom, the vertical gap between the day number and event markers dynamically expands by $+10\%$ per 10% zoom step.
  - **Resizability**: Horizontally resizable, and vertically extendable by +1 grid up to 4 rows (`android:resizeMode="horizontal|vertical"`).
- **Productivity Widget (4×2 Target Size)**:
  - **Left Card (Date Details)**: Prominent big day number, short weekday, short month, Khmer lunar month and day, Buddhist Era year, Western Zodiac sign, and holy day badge.
  - **Right Card (Events Overview)**: Dual 2-block layout for Today's and Tomorrow's event lists.
  - **Compact Width Adaptation**: When widget width is < 330dp, the right detail list (lunar month, zodiac, etc.) is hidden to cleanly display a centered weekday and big day number without text clipping.
  - **Width Freezing on Expansion**: When widget width reaches $\ge 330\,\text{dp}$ (step 2, standard 4-column phone/tablet width), the date details card freezes at its ideal width ($\approx 152\,\text{dp} \times \text{scale}$), routing all further horizontal resizing width directly to the event lists to display longer event titles without truncation.
- **Focus Widget (4×2 Target Size)**:
  - **Header Badges**: Lunar date, month, and BE year chip, timezone badge, and a quick refresh action button.
  - **Middle Content**: Scrollable `LazyColumn` for today's events with stable `itemId` keys for smooth list diffing and scroll position preservation on Android 12+.
  - **Footer Sub-Cards**: Sub-cards for Yesterday and Tomorrow with event counts and previews.
  - **Full Horizontal Expansion**: Width expands dynamically with user resizing handles across all launcher grid widths without arbitrary caps, providing full width for long event titles and yesterday/tomorrow sub-cards.

### App Settings & System Integration
- **Settings → Widgets**: Located directly after Notifications in the main app settings.
- **Master Enablement**: "Enable widgets" switch (`widgetsEnabled`, OFF by default). When toggled off, `PackageManager.setComponentEnabledSetting` disables all three widget receivers (`COMPONENT_ENABLED_STATE_DISABLED`), completely hiding them from the system Widget Browser with zero background resource usage.
- **Category & Privacy Controls**: 4 expandable toggles:
  1. *Personal events*
  2. *Public holidays*
  3. *Observances*
  4. *Hide personal event details* (shows event count only, suppressing titles and times)
- **Font Zoom Clamping (`WidgetPolicy.MAX_WIDGET_FONT_SCALE`)**: Widget text scaling is capped at $130\%$ (`1.30f`) across all three widgets even when the in-app font scale is set to $140\%$ or $150\%$, preserving home screen layout integrity.

### Preview Thumbnails
- Static 8-bit PNG preview thumbnails (`widget_focus.png`, `widget_productivity.png`, and `widget_month.png`) placed in `res/drawable/` (Light) and `res/drawable-night/` (Dark) allow Android's system Widget Browser to display high-resolution, theme-matching previews on Android 12+.

