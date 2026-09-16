<div align="center">

<img src="docs/images/app-logo.png" alt="Khmer Calendar app icon" width="160" />

# Khmer Calendar

[![Android](https://img.shields.io/badge/Android-12%2B%20(API%2031%E2%80%9337)-3DDC84?logo=android&logoColor=white)](https://developer.android.com/)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.4.20-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Offline](https://img.shields.io/badge/Network-100%25%20Offline-success)](docs/architecture.md)
[![Privacy](https://img.shields.io/badge/Privacy-0%20Ads%20%7C%200%20Trackers-blue)](PRIVACY_POLICY.md)

A privacy-first, ad-free Android calendar built with Kotlin and Jetpack Compose. It uses the shared Khmer Calendar Engine for lunar dates (*Chhankitek*), Buddhist Era and traditional year transitions, and combines stored public holidays and cultural observances with customizable local notifications.

**[Live Web (PWA) Calendar](https://rsg-kh.github.io/khmer-calendar-pwa/)** — Open the web version in your browser on phones, tablets, and desktops.

</div>

<img src="docs/images/cover.jpg" alt="Khmer Calendar Cover" />

## Key Features

### 📅 Shared Khmer Calendar Engine (1800–2200)
- Uses the dedicated [Khmer Calendar Engine](https://github.com/RSG-KH/khmer-calendar-engine) for lunar dates, Buddhist Era, traditional year labels, holy days and New Year dates.
- Android handles localized presentation, event records, personal events and reminders. See [engine integration](docs/shared-engine.md) for the dependency and [engine reference evidence](https://github.com/RSG-KH/khmer-calendar-engine/blob/v0.1.0/docs/references.md) for calculation validation and limits.
- Western zodiac signs, elements and ruling bodies are supplied by the Android app.

### 🌸 Buddhist Holy Days (*Thngai Seil*)
- Accurately tracks the 8th and 15th waxing days, and 8th and 14th/15th waning days (including 29-day month boundary adjustments).
- Identifies Shaving Day (*Thngai Kaor*), the day immediately preceding each holy day.
- Semi-transparent lotus artwork (25% opacity, full cell scale) marks holy day cells: a closed lotus for day 8 of either lunar phase, and a blossom for the final holy day on day 14 or 15.
- Independent visibility toggles allow users to show or hide holy day markers in the calendar grid and event lists.

### 🏛️ Bundled Events & Recurrence Rules
- **Bundled Reference Database**: Contains **3,246 captured event occurrences** for 2000–2030. These preserve the reference website's records; they are not all independently verified.
- **Precomputed Event Dates**: Bundles engine-calculated observances for 1980–1999 and 2031–2050 and Buddhist holy days for 1980–2050, avoiding runtime event-date calculations within that range. Other supported years are calculated on demand.
- **Official Government Holidays**: The 2025–2026 public holiday markers use year-specific Ministry of Economy and Finance (MEF) and Legal Reform Committee (LRC) calendar snapshots.
- **Historical & Cultural Recurrences (1800–2200)**: 100 reviewed rules calculate traditional festivals (Water Festival, Pchum Ben, Royal Ploughing, Meak Bochea, Visak Bochea, Khmer New Year) and national/UN observances outside the primary reference window.

### ⏰ Custom Events & Precision Notifications
- **Local SQLite Persistence**: Create, edit, and delete personal events with titles, dates, times, and notes.
- **Time Zone Intelligence**:
  - **Local Time**: Follows device time zone changes during travel and daylight saving time.
  - **Cambodia Time (UTC+7)**: Option to fix calculations to Cambodia time regardless of location.
- **Local Alarms**: Powered by Android's `AlarmManager.setExactAndAllowWhileIdle`—delivers notifications reliably without background battery drain or remote push servers.
- **Flexible Repeat Intervals**: Configure daily push times with repeat reminders set to **Off**, 2, 4, 6, 8, or 12 hours.
- **Event-Type Controls**: Choose reminders for custom events, holidays, observances, and Buddhist holy days independently.

### 📱 Adaptive Multi-Form-Factor UI
- **Phone Landscape Experience**: Navigation rail tabs dynamically expand across the entire vertical height (`weight(1f)`), delivering ergonomic tap targets without empty dead space.
- **Tablet Landscape 2-Column Layout**: Left column features the month grid and a consolidated 2-column lunar and Gregorian date card, leaving the right column for full-month event browsing.
- **Responsive Month Picker**: Jump between months with a 6-column by 2-row layout in landscape mode.
- **Intelligent Scrollbars**: Clean, canvas-based vertical scrollbars appear strictly when content overflows dialog containers.
- **Optical Geometry Balancing**: Event markers (holiday circles, holy day triangles, observance squares) are normalized by minimum dimension for uniform visual balance.

### 🎨 Personalization & Accessibility
- **Longer Weekday Names**: Optional calendar headings show Sun–Sat in English and full weekday names in Khmer. Off by default under Settings → Calendar.
- **Weekday Colors**: Traditional weekday heading colors, with shades adapted for light and dark themes. On by default under Settings → Calendar; existing saved choices are preserved.
- **Curated Theme Accents**: Choose from **Blue** (Default), **Lavender**, **Rose**, **Amber**, and **Lime** (*បៃតងចាស់*).
- **Theme Modes**: Full support for System, Light (`#F3F4F8`), and OLED Dark (`#0C0E12`) modes.
- **Dynamic Font Scaling**: Choose 80%, 90%, 100%, 110%, or 120% on phones, with additional 130%, 140%, and 150% options on tablets.
- **Bilingual Experience**: Instant switching between Khmer and English with full localization.

---

## Project Structure

```
KhmerCalendar/
├── app/
│   ├── src/main/
│   │   ├── java/com/rsgkh/calendar/
│   │   │   ├── MainActivity.kt          # Entry point
│   │   │   ├── data/
│   │   │   │   ├── AppPreferences.kt    # Settings (theme, accent, font scale, timezone)
│   │   │   │   ├── CustomEventRepository.kt # Custom event CRUD (local persistence)
│   │   │   │   ├── EventRepository.kt   # Event models & bundled snapshot loading
│   │   │   │   └── RecurringEvents.kt   # 100-rule recurrence fallback (1800–2200)
│   │   │   ├── domain/
│   │   │   │   ├── KhmerCalendar.kt     # Shared-engine date adapter & lunar labels
│   │   │   │   ├── KhmerDateDetails.kt  # Engine results & Android formatting
│   │   │   │   ├── KhmerNewYear.kt      # Shared-engine festival date adapter
│   │   │   │   └── Zodiac.kt            # Western zodiac signs & elements
│   │   │   ├── i18n/
│   │   │   │   ├── CalendarWords.kt     # Khmer/English month, day & number words
│   │   │   │   └── L.kt                 # Offline localization dictionary access
│   │   │   ├── notifications/
│   │   │   │   ├── EventNotifications.kt # Exact alarm scheduling & grouping
│   │   │   │   └── ReminderPlanner.kt   # Push time & repeat interval planning
│   │   │   └── ui/
│   │   │       ├── CalendarApp.kt       # Main screens, responsive nav & month picker
│   │   │       ├── CustomEventEditor.kt # Custom event creator & time zone picker
│   │   │       ├── NotificationSettings.kt # Notification preference screens
│   │   │       ├── Previews.kt          # Compose previews
│   │   │       ├── Scrollbars.kt        # Zero-recomposition dynamic scrollbar
│   │   │       ├── SettingsControls.kt  # Reusable settings rows & dropdowns
│   │   │       └── Theme.kt             # Material 3 tokens, accents, & readableSp
│   │   ├── resources/
│   │   │   ├── calendar-events.tsv      # 3,246 bundled historical events (2000–2030)
│   │   │   ├── engine-event-dates.tsv   # Precomputed recurrence and holy-day dates through 2050
│   │   │   ├── recurrence-rules.tsv     # 100 reviewed recurrence rules (1800–2200)
│   │   │   ├── translations.tsv         # Offline localization dictionary
│   │   │   └── event-translations.tsv   # Translated event name templates
│   │   └── assets/
│   │       ├── NOTICE.txt               # Engine & upstream attribution notices
│   │       └── engine-LICENSE.txt       # Shared engine's Apache 2.0 license
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
- 📜 **[Recurring Event Rules](docs/recurring-event-rules.md)**: App recurrence definitions, engine mapping and snapshot precedence.
- 🗃️ **[Reference Event Database](docs/reference-event-database.md)**: Provenance and schema for the 3,246 captured 2000–2030 event database.

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

- **Zero Network Permissions**: The application does not request the Android `INTERNET` permission.
- **Zero Advertising or Telemetry**: No third-party SDKs, analytics, or tracking services are bundled.
- **Local Data Ownership**: User events and preferences are stored exclusively on-device in SQLite.

For complete details on our data practices and user controls, read our [Privacy Policy](PRIVACY_POLICY.md).

### Credits & Attribution
- Calculations use [Khmer Calendar Engine](https://github.com/RSG-KH/khmer-calendar-engine). The bundled [notices](app/src/main/assets/NOTICE.txt) retain its upstream attribution.
- Dated events and government holiday sources are documented in [Bundled event data](docs/reference-event-database.md).

### License
Released under the open-source [Apache 2.0 License](LICENSE). The engine's [license](app/src/main/assets/engine-LICENSE.txt) and [upstream notices](app/src/main/assets/NOTICE.txt) are bundled with the app.
