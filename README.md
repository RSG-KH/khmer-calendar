<div align="center">

<img src="docs/images/app-logo.png" alt="Khmer Calendar app icon" width="160" />

# Khmer Calendar

[![Android](https://img.shields.io/badge/Android-12%2B%20(API%2031%E2%80%9337)-3DDC84?logo=android&logoColor=white)](https://developer.android.com/)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.4.20-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Offline](https://img.shields.io/badge/Network-100%25%20Offline-success)](docs/architecture.md)
[![Privacy](https://img.shields.io/badge/Privacy-0%20Ads%20%7C%200%20Trackers-blue)](PRIVACY_POLICY.md)

A privacy-first, ad-free Android calendar built with Kotlin and Jetpack Compose. Browse Khmer lunar dates, Buddhist holy days, holidays and observances, and manage personal events with local reminders.

**[Live Web (PWA) Calendar](https://rsg-kh.github.io/khmer-calendar-pwa/)** — Open the web version in your browser on phones, tablets, and desktops.

</div>

<img src="docs/images/cover.jpg" alt="Khmer Calendar Cover" />

## Key Features

### 📅 Shared Khmer Calendar Engine (1800–2200)

- Gregorian and Khmer lunar dates, Buddhist Era, animal year, Sak, New Year dates, traditional Moha Sangkran arrival estimates, traditional Chinese festivals, and Ganzhi (干支) year/month/day/hour pillars are provided by [Khmer Calendar Engine](https://github.com/RSG-KH/khmer-calendar-engine).
- Calendar algorithms, source references and validation are documented in the [engine project](https://github.com/RSG-KH/khmer-calendar-engine#verification-and-accuracy). Android supplies the interface, translations, personal events and reminders.
- Date details also show Western zodiac signs, elements and ruling bodies, plus a Chinese Ganzhi (干支) table with the Year, Month and Day sign and clash animals — and the Hour pillar for Today — displayed as emoji or animal names.

### 🌸 Buddhist Holy Days (*Thngai Seil*)

- Shows Buddhist holy days and Shaving Day (*Thngai Kaor*) using the engine's results.
- Lotus artwork marks holy days, with separate visibility settings for the calendar and event lists.

### 🏛️ Holidays & Cultural Observances

- **Unified Moha Sangkran Arrival Time**: Displays verified official arrival times (TVK broadcasts and government decrees, 1997, 2009, 2010–2026 unbroken) and traditional astronomical estimates (2027+) directly in event titles with authentic Khmer 12-hour period descriptors (`ព្រឹក`, `រសៀល`, `ល្ងាច`, `យប់`, `រំលងអធ្រាត្រ`).
- **Official Holiday Calendars (2016–2027)**: Bundled government calendars mark public holidays with their citing subdecree or ministry source, shown in event details.
- **Anniversary Counts**: Commemorations display their anniversary count in both languages, rendered per year from the catalog — ខួបលើកទី៤៧ in Khmer and · 47th with English ordinal suffixes (1st, 2nd, 3rd … 11th–13th, 21st) — alongside official holiday titles from the citing subdecree.
- **Learn More Knowledge**: Every built-in event carries a curated bilingual knowledge summary (bundled from the Khmer Calendar Manager dataset), shown in a Learn more popup that stacks both languages — the app language first.
- **On-Device Event Calculation (1800–2200)**: Observances, traditional Cambodian festivals, Chinese festivals and Buddhist holy days are calculated on the device from bundled rules for every supported year, with reviewed date corrections where captured records differ.
- **Cambodian Holidays & Festivals**: Browse public holidays and traditional festivals, including Khmer New Year, Pchum Ben, Water Festival and Royal Ploughing.
- **National & International Observances**: Includes commemorations and UN observances alongside Buddhist holy days.

### ⏰ Personal Events & Precision Notifications
- **Local SQLite Persistence**: Create, edit, and delete personal events with titles, dates, times, and notes.
- **Repeating Events**: Repeat every X days, weekly, monthly or yearly, with a required end date and a preview. Choose whether to skip missing month-end or leap-day dates or include the available date. Edit or delete the whole series together.
- **Time Zone Intelligence**:
  - **Display Time Zone**: Choose device local time or Cambodia time (UTC+7) for Today, event display and the daily reminder clock.
  - **Repeating Event Time Zone**: Each series keeps its saved time zone and wall-clock time across daylight saving changes; occurrences are shown in the selected display zone.
- **Local Alarms**: Android's `AlarmManager.setExactAndAllowWhileIdle` schedules reminders on-device, with notification and exact-alarm access enabled. No remote push server is used.
- **Repeat Reminders**: Configure the daily reminder time and optional additional reminders every 2, 4, 6, 8 or 12 hours. These are separate from an event's repeat schedule.
- **Event-Type Controls**: Choose reminders for personal events, holidays, observances, and Buddhist holy days independently.

### 📱 Home Screen Widgets
- **Month Widget (4×3)**: Full-month calendar grid with Gregorian and Khmer lunar dates, Buddhist holy days, traditional weekday heading colors, today cell highlight, event markers row (● holiday, ▲ holy, ■ observance, ★ personal), legend footnote, and interactive date-cell tapping that launches the app directly into that day's date details.
- **Productivity Widget (4×2)**: Date details card featuring a prominent day number, short weekday, short Gregorian month, Khmer lunar month and day, Buddhist Era year, Western Zodiac sign, and Today/Tomorrow event lists. Automatically adapts when resized horizontally by hiding detail rows on compact widths.
- **Focus Widget (4×2)**: Daily events overview featuring full lunar date and BE year badges, a scrollable list of today's events, and preview sub-cards for yesterday and tomorrow.
- **Planner Widget (4×2)**: Resizable 29-day agenda centered on today, with a date-range header, timezone and refresh badges, a scrollable day list, and color-coded event chips. Timed events come first; tapping a chip opens its event, while tapping the rest of a row opens date details.
- **App Settings Integration**: Master **Enable widgets** toggle switch under Settings → Widgets. Toggling off uses `PackageManager.setComponentEnabledSetting` to disable widget receivers, hiding them from the system widget picker with zero background battery or memory overhead, and cancels queued, periodic and midnight refreshes.
- **Shared Appearance**: Widgets follow the app's language, theme, accent color, selected time zone and widget font size; changing any setting refreshes installed widgets immediately while the app is open.
- **Global Event Filtering**: Expandable switches in App Settings control Personal events, Public holidays, Observances, and Hide personal event details across all widgets. Buddhist holy days follow app-wide Calendar and Event settings.
- **Dynamic Light & Dark Previews**: Embedded 8-bit PNG preview thumbnails (`res/drawable/` and `res/drawable-night/`) automatically reflect the system light/dark theme in the Android widget picker.

### 📱 Adaptive Multi-Form-Factor UI
- **Phone Landscape Experience**: Navigation rail tabs dynamically expand across the entire vertical height (`weight(1f)`), delivering ergonomic tap targets without empty dead space.
- **Tablet Landscape 2-Column Layout**: Left column features the month grid and a consolidated 2-column lunar and Gregorian date card, leaving the right column for full-month event browsing.
- **Responsive Month Picker**: Jump between months with a 6-column by 2-row layout in landscape mode.
- **Intelligent Scrollbars**: Clean, canvas-based vertical scrollbars appear strictly when content overflows dialog containers.
- **Optical Geometry Balancing**: Event markers (holiday circles, holy day triangles, observance squares) are normalized by minimum dimension for uniform visual balance.

### 🎨 Personalization & Accessibility
- **Longer Weekday Names**: Optional calendar headings show Sun–Sat in English and full weekday names in Khmer. Off by default under Settings → Calendar.
- **Weekday Colors**: Traditional weekday heading colors, with shades adapted for light and dark themes. On by default under Settings → Calendar; existing saved choices are preserved.
- **Western Zodiac Signs**: The selected-date card, date details and event details show the Western zodiac sign, element and ruling body. On by default under Settings → Astrology & Zodiac.
- **Chinese Ganzhi (干支) Table**: Date details show the Year, Month and Day sign and clash animals, plus the Hour pillar for Today in the selected time zone, with an emoji/name toggle. On by default under Settings → Astrology & Zodiac.
- **Curated Theme Accents**: Choose from **Blue** (Default), **Lavender**, **Rose**, **Amber**, and **Lime** (*បៃតងចាស់*).
- **Theme Modes**: Initially follows the system theme; choosing Light or Dark saves that preference. Background accent is on by default, tinting pages and navigation with the chosen accent; turning it off restores neutral backgrounds.
- **Dynamic Font Scaling**: Choose 80%–150% on all devices. Home screen widgets have their own separate 80%–200% font size setting.
- **Bilingual Experience**: Instant switching between Khmer and English with full localization.

---

## Project Structure

```
KhmerCalendar/
├── app/
│   ├── src/main/
│   │   ├── java/com/rsgkh/calendar/
│   │   │   ├── MainActivity.kt          # Entry point
│   │   │   ├── TodayRefresh.kt          # Visible-only today-date refresh polling
│   │   │   ├── data/
│   │   │   │   ├── AppPreferences.kt    # Settings (theme, accent, font scale, timezone, widgets)
│   │   │   │   ├── CustomEventRepository.kt # Personal event CRUD (local persistence)
│   │   │   │   ├── EventRepository.kt   # Event models & bundled snapshot loading
│   │   │   │   └── RecurringEvents.kt   # Built-in observance recurrence rules
│   │   │   ├── domain/
│   │   │   │   ├── EventRepeat.kt       # Personal event repeat schedules & fallbacks
│   │   │   │   ├── KhmerCalendar.kt     # Shared-engine date adapter & lunar labels
│   │   │   │   ├── KhmerDateDetails.kt  # Engine results & Android formatting
│   │   │   │   ├── KhmerNewYear.kt      # Shared-engine festival date adapter
│   │   │   │   └── Zodiac.kt            # Western zodiac signs & elements
│   │   │   ├── i18n/
│   │   │   │   ├── CalendarWords.kt     # Khmer/English month, day & number words
│   │   │   │   └── L.kt                 # Offline localization dictionary access
│   │   │   ├── notifications/
│   │   │   │   ├── EventNotifications.kt # Exact alarm scheduling & grouping
│   │   │   │   ├── ReminderPlanner.kt   # Push time & repeat interval planning
│   │   │   │   └── ReminderWork.kt      # Background executor for reminder work
│   │   │   ├── widgets/
│   │   │   │   ├── CalendarHomeWidget.kt # Glance base class & Focus/Productivity/Month widgets
│   │   │   │   ├── PlannerWidgetRenderer.kt # Native 29-day agenda widget
│   │   │   │   ├── WidgetContent.kt     # Size-adaptive Glance composables
│   │   │   │   ├── WidgetDataSource.kt  # IO snapshot loading & filtering
│   │   │   │   ├── WidgetNavigation.kt  # Widget tap intents into app date details
│   │   │   │   ├── WidgetPalette.kt     # Day/night widget color providers
│   │   │   │   ├── WidgetPolicy.kt      # Pure date & event-filtering policies
│   │   │   │   ├── WidgetRefreshWorker.kt # WorkManager periodic refresh worker
│   │   │   │   ├── WidgetUpdater.kt     # WorkManager & AlarmManager scheduler
│   │   │   │   ├── WidgetReceivers.kt   # Receiver components & system restores
│   │   │   │   └── WidgetStrings.kt     # Localized string formatters & Khmer time
│   │   │   └── ui/
│   │   │       ├── CalendarApp.kt       # Main screens, responsive nav & widget settings
│   │   │       ├── CopyTextButton.kt    # Copy icon with copied confirmation feedback
│   │   │       ├── CustomEventEditor.kt # Personal event editor with native pickers
│   │   │       ├── EventRepeatEditor.kt # Repeat choices, end date, switches & preview
│   │   │       ├── NotificationSettings.kt # Notification preference screens
│   │   │       ├── Previews.kt          # Compose previews
│   │   │       ├── RequiredFieldLabel.kt # Accent asterisk label for required fields
│   │   │       ├── Scrollbars.kt        # Zero-recomposition dynamic scrollbar
│   │   │       ├── SelectionChip.kt     # Shared filter-chip for filters & repeat choices
│   │   │       ├── SettingsControls.kt  # Reusable settings rows & dropdowns
│   │   │       ├── Theme.kt             # Material 3 tokens, accents, & readableSp
│   │   │       └── WeekdayColors.kt     # Traditional weekday colors for light & dark
│   │   ├── resources/
│   │   │   ├── khmer-calendar-data.json # Event catalog: rules, dated records, official calendars, overrides & sources
│   │   │   └── translations.tsv         # Offline localization dictionary
│   │   └── assets/
│   │       ├── NOTICE.txt               # App & data catalog attribution notices
│   │       ├── app-LICENSE.txt          # App's Apache 2.0 license
│   │       ├── engine-LICENSE.txt       # Shared engine's Apache 2.0 license
│   │       └── engine-NOTICE.txt        # Engine's upstream attribution notices
│   ├── src/test/
│   │   └── java/com/rsgkh/calendar/     # Robolectric & JUnit unit test suites
│   ├── src/sharedTest/
│   │   └── java/com/rsgkh/calendar/     # Shared Compose UI scenario tests (unit + device)
│   └── src/androidTest/
│       └── java/com/rsgkh/calendar/     # Instrumentation tests (device/emulator)
├── docs/                                # In-depth technical architecture & guides
├── tools/                               # Event audit scripts & translation editor
└── translations/
    └── catalog.json                     # Primary translation catalog
```

---

## Building the Project

### Prerequisites
- **JDK 25** (auto-provisioned by Gradle toolchain resolution if absent)
- **Android SDK Platform 37** (minSdk 31)
- **Gradle 9.6.0** (bundled via `gradlew`)

Gradle downloads the pinned shared engine release and verifies its checksum during the build. See [Shared Calendar Engine](docs/shared-engine.md) for updates and offline builds. The installed app needs no network access.

### Build Commands
```powershell
# Build Debug APK
.\gradlew.bat assembleDebug

# Build Unsigned Release APK
.\gradlew.bat assembleRelease

# Run Unit & Robolectric UI Tests
.\gradlew.bat testDebugUnitTest

# Run Connected Instrumentation Tests (on Device/Emulator)
.\gradlew.bat connectedDebugAndroidTest

# Run Android Lint
.\gradlew.bat lintDebug
```

Output APKs:
- **Debug**: `app/build/outputs/apk/debug/app-debug.apk`
- **Release**: `app/build/outputs/apk/release/app-release-unsigned.apk`

---

## Technical Documentation

Android architecture, integration and developer guides are maintained in the [`docs/`](docs/README.md) directory:

- 📦 **[Shared Calendar Engine](docs/shared-engine.md)**: Release dependency, checksum verification, upgrades and migration behavior.
- 📖 **[System Architecture](docs/architecture.md)**: Engine adapters, event repository pipeline and exact alarm subsystem.
- 🎨 **[UI & Responsive Design](docs/ui-and-responsive-design.md)**: Phone vs. tablet layouts, landscape navigation rail distribution, dynamic scrollbar modifier, and font scaling architecture.
- 🛠️ **[Development & Testing Guide](docs/development-and-testing.md)**: Environment configuration, test suite details, translation tool setup, and dataset generation pipelines.
- 📜 **[Recurring Event Rules](docs/recurring-event-rules.md)**: App recurrence definitions, engine mapping and reviewed date overrides.
- ✅ **[Custom Repeat Verification](docs/custom-repeat-verification.md)**: Behavior checklist and test record for repeating personal events.
- 🗃️ **[Bundled Event Data](docs/reference-event-database.md)**: Event catalog schema, official holiday calendars, provenance and maintenance.

---

## Translation & Localization

All in-app text, calendar vocabulary, and event templates are managed through [`translations/catalog.json`](translations/catalog.json).

A local web editor is included for seamless translation maintenance:
```cmd
cd tools\translation
Start.cmd
```
For detailed workflow instructions, consult the [Translation Tool Guide](tools/translation/README.md).

---

## Privacy & Open Source Philosophy

- **Zero Network Permissions**: The application does not request the Android `INTERNET` permission. Library permissions merged from dependencies (`ACCESS_NETWORK_STATE`, `FOREGROUND_SERVICE`) are explicitly stripped in the manifest. The event details "Search online" action is strictly user-initiated: it opens the browser directly in Google AI mode (Custom Tab) for the event title and its history via Android's intent system — the app itself holds no network permission and sends nothing.
- **Zero Advertising or Telemetry**: No third-party SDKs, analytics, or tracking services are bundled.
- **Local Data Ownership**: Personal events and repeat schedules are stored on-device in SQLite; settings use Android SharedPreferences.

For complete details on our data practices and user controls, read our [Privacy Policy](PRIVACY_POLICY.md).

### Credits & Attribution
- Calculations use [Khmer Calendar Engine](https://github.com/RSG-KH/khmer-calendar-engine). The bundled [engine notices](app/src/main/assets/engine-NOTICE.txt) retain its upstream attribution.
- Dated events and government holiday sources are documented in [Bundled event data](docs/reference-event-database.md).

### License
Released under the open-source [Apache 2.0 License](LICENSE). The app [license](app/src/main/assets/app-LICENSE.txt), the engine's [license](app/src/main/assets/engine-LICENSE.txt) and [upstream notices](app/src/main/assets/engine-NOTICE.txt), and the app & catalog [notices](app/src/main/assets/NOTICE.txt) are bundled with the app.
