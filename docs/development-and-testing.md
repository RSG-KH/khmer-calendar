# Development and testing

## Setup

- JDK 25, auto-provisioned by Gradle toolchain resolution if absent.
- Android SDK Platform 37; the app targets API 37 and supports API 31+.
- Gradle 9.6.0 through the wrapper, Android Gradle Plugin 9.4.0 and Kotlin 2.4.20.

AGP provides built-in Kotlin support. The root `build.gradle.kts` selects the Kotlin Gradle plugin version from `gradle/libs.versions.toml`, which also sets the Compose compiler version. Update that shared version to keep both compilers aligned. See Android's [built-in Kotlin configuration](https://developer.android.com/build/releases/agp-9-0-0-release-notes#runtime-dependency-on-kotlin-gradle-plugin).

Gradle downloads and verifies the pinned engine release. A sibling engine checkout is not required. See [shared engine integration](shared-engine.md) for checksums, offline builds and dependency upgrades.

Runtime and test library versions, including Robolectric, are pinned in `gradle/libs.versions.toml`. Core 1.19 includes the Kotlin extensions in the main `androidx.core:core` artifact, so a separate `core-ktx` dependency is unnecessary.

## Builds

Run from the project root in PowerShell:

```powershell
.\gradlew.bat assembleDebug
.\gradlew.bat assembleRelease bundleRelease
.\gradlew.bat lintDebug
```

The debug APK is `app/build/outputs/apk/debug/app-debug.apk` and uses the Android debug keystore. The unsigned release APK is `app/build/outputs/apk/release/app-release-unsigned.apk`; the release bundle is `app/build/outputs/bundle/release/app-release.aab`. Release distribution requires your signing configuration.

## Testing

```powershell
.\gradlew.bat testDebugUnitTest
.\gradlew.bat connectedDebugAndroidTest
python -m unittest discover -s tools/translation -p "test_*.py"
```

Connected tests require a running emulator or device listed by `adb devices`.

| Checks | Android responsibility |
| --- | --- |
| Calendar adapters | Supported range, date conversion, localized year labels and festival results |
| Pinned reference fixtures | Compatibility through the app adapters, including the reviewed 2012 correction |
| Event repository and recurrence | Catalog coverage across 1800–2200, engine parity for calculated events and holy days, recorded date lists, official holiday calendars with citations, rule translation, captured-date comparisons and classification |
| Reminder planner and delivery | Category controls, appearance changes preserving alarms, permissions, saved event instants, clock/time-zone broadcasts without an activity, daylight saving and repeats |
| Today refresh | Visible-only polling, immediate refresh on return, midnight, clock jumps and device time-zone changes |
| Compose UI | Phone/tablet layouts, both languages, dialogs, font scaling and settings |
| System window behavior | Light/dark edge-to-edge configuration on API 31, 34 and 35 |
| Translation tools | Catalog validation, export, backups and conflicting saves |

The app retains its pinned MomentKH fixture and generator to catch consumer regressions. Agreement with that fixture is a compatibility check; it is not independent historical validation. Calendar algorithms and their evidence are maintained and tested in the engine project, linked from the [integration guide](shared-engine.md).

Shared UI scenarios in `app/src/sharedTest` run under Robolectric and on a device. Device screenshots capture the full display so open dialogs are included. Inspect changed screens when updating layout or source-dialog content.

The bilingual About/Sources interaction check runs in `CalendarUiTest` on Android, covering real inline-link rendering, the engine link's destination and bundled licenses. Compose tests use the v2 test rules; alarm assertions use Robolectric's current accessors.

## Translations

Edit [`translations/catalog.json`](../translations/catalog.json) through the local [translation editor](../tools/translation/README.md), or make an intentional catalog edit and export:

```powershell
python tools/translation/server.py --export
```

The catalog generates `translations.tsv`, `event-translations.tsv` and launcher strings. Rebuild and reinstall to see changes. Do not hand-edit generated resources. License and notice texts remain verbatim assets outside the translation catalog.

## Event resources

The app owns its event catalog separately from the engine. Normal builds use the committed `khmer-calendar-data.json` and do not run any generator.

To change event definitions, official holiday years or date overrides, edit the catalog directly following the [bundled data guide](reference-event-database.md#maintenance), then run `.\gradlew.bat testDebugUnitTest`. The catalog embeds event titles and sources; the [translation catalog](#translations) continues to supply all other app strings.

The retired TSV pipeline's scripts (`tools/build-recurring-events.py`, `tools/ExportEngineEventDates.java`) remain under `tools/` for history; they no longer produce runtime resources and expect files removed with the old snapshot. The capture-assist tools (`tools/reference-event-import.cjs`, `tools/audit-supplied-events.py`, `tools/generate-calendar-reference.cjs`) still support reviewing new website captures.
