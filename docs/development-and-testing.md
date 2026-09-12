# Development & Testing Guide

This guide covers setting up the development environment, building APKs, running automated test suites, managing translations, and maintaining event datasets.

---

## 1. Prerequisites & Environment Setup

- **Java Development Kit (JDK)**: JDK 25 (auto-provisioned by Gradle toolchain resolution if absent).
- **Android SDK**:
  - `compileSdk`: **37**
  - `targetSdk`: **37**
  - `minSdk`: **31** (Android 12+)
- **Android Gradle Plugin (AGP)**: **9.4.0**
- **Gradle**: **9.6.0** (managed via `./gradlew`)
- **Kotlin**: **2.2.10** with Compose Compiler plugin enabled.

---

## 2. Build Commands

Run the following commands using the Gradle wrapper from the project root:

### Debug Build
```powershell
.\gradlew.bat assembleDebug
```
- Outputs debug APK: `app/build/outputs/apk/debug/app-debug.apk`
- Automatically signed with Android's standard debug keystore.

### Release Build
```powershell
.\gradlew.bat assembleRelease
```
- Outputs unsigned release APK: `app/build/outputs/apk/release/app-release-unsigned.apk`
- To distribute, sign using `apksigner` or configure your release keystore in `app/build.gradle.kts`.

### Code Quality & Linting
```powershell
.\gradlew.bat lintDebug
```

---

## 3. Testing Strategy

The repository maintains strict verification combining unit tests on the JVM via Robolectric with on-device UI instrumentation via Espresso.

### Running Unit Tests (Robolectric JVM)
```powershell
.\gradlew.bat testDebugUnitTest
```

#### What the Unit Tests Cover:
1. **Mathematical Accuracy (146,462 Dates across 1800–2200)**:
   - Evaluates every Gregorian date from January 1, 1800 to December 31, 2200.
   - Compares lunar month starts, day numbers, leap months (*Adhikamasa*), and leap days (*Adhikavara*) against pinned reference implementations and recurrence invariants.
   - Asserts all 201 pinned Khmer New Year start dates (1900–2100) and government festival anchors, including the four-day year in 2024.
2. **Buddhist Era (BE) Transition Rules**:
   - Asserts that BE increments strictly on **1 Roach Pisakh** each year, not at the Gregorian or Solar New Year.
3. **Responsive UI Configurations**:
   - Phone portrait (`w411dp-h891dp`): Bottom navigation bar, vertical scrolling.
   - Phone landscape (`w800dp-h400dp-land`): Navigation rail items expanding vertically across full screen (`weight(1f)`).
   - Tablet landscape (`sw800dp-w1280dp-h800dp-land`): Compact navigation rail, 2-column date card.
4. **Interactive Dialogs & Features**:
   - Dynamic font size scaling (80% to 120%) verifying text heights adjust proportionately.
   - Buddhist Holy Day toggle synchronization in Date Details popup and tablet landscape event list.
   - Precise reminder prompt behavior based on notification and exact alarm permissions.

### Running Instrumented UI Tests (Device / Emulator)
Ensure an active Android emulator or physical device is connected via ADB (`adb devices`):
```powershell
.\gradlew.bat connectedDebugAndroidTest
```

---

## 4. Translation & Localization Workflow

Khmer Calendar uses a centralized source of truth: [`translations/catalog.json`](../translations/catalog.json).

### Translation Editor Web Tool
A standalone web-based translation editor is available under `tools/translation/`:
1. Start the tool:
   ```cmd
   cd tools\translation
   Start.cmd
   ```
2. Open your browser to the local server URL displayed in the terminal.
3. Edit English–Khmer string pairs grouped by:
   - **Events** (event names, recurring festivals)
   - **In-app text** (buttons, headers, dialogs)
   - **Calendar & dates** (months, days, lunar attributes)
   - **Notifications** (reminders, alarm messages)
   - **About & sources** (credits, license text)
4. Saving automatically writes backup copies and regenerates:
   - `app/src/main/resources/translations.tsv`
   - `app/src/main/resources/event-translations.tsv`
5. Rebuild the app with `.\gradlew.bat assembleDebug` to test the new translations.

---

## 5. Event Data Management & Auditing

The app bundles 3,246 historical event occurrences (2000–2030) alongside 100 normalized recurrence rules (1800–2200).

### Generating Calendar Event Resources
To regenerate `app/src/main/resources/calendar-events.tsv`:
```powershell
python tools/build-reference-events.py
```
- Validates captured dates against official Cambodian holiday gazettes (MEF & LRC).
- Enriches matching entries with official holiday metadata.
- Exports a compact UTF-8 TSV resource with database checksums.

### Generating Recurring Event Rules
To regenerate `tools/recurring-event-rules.json`:
```powershell
python tools/build-recurring-events.py
```

### Auditing Discrepancies
To audit raw event databases against captured references:
```powershell
python tools/audit-supplied-events.py <path_to_event_database>
```
