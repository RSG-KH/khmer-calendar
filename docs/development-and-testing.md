# Development and testing

## Setup

- JDK 25, auto-provisioned by Gradle toolchain resolution if absent.
- Android SDK Platform 37; the app targets API 37 and supports API 31+.
- Gradle 9.8.1 through the wrapper, Android Gradle Plugin 9.4.1 and Kotlin 2.4.21.

AGP provides built-in Kotlin support. The root `build.gradle.kts` selects the Kotlin Gradle plugin version from `gradle/libs.versions.toml`, which also sets the Compose compiler version. Update that shared version to keep both compilers aligned. See Android's [built-in Kotlin configuration](https://developer.android.com/build/releases/agp-9-0-0-release-notes#runtime-dependency-on-kotlin-gradle-plugin).

Gradle downloads and verifies the pinned engine release. A sibling engine checkout is not required. See [shared engine integration](shared-engine.md) for checksums, offline builds and dependency upgrades.

Runtime and test library versions, including Robolectric, are pinned in `gradle/libs.versions.toml`. Core 1.19 includes the Kotlin extensions in the main `androidx.core:core` artifact, so a separate `core-ktx` dependency is unnecessary.

### Dependency review — 11 October 2026

Checked all pinned dependencies against [Google Maven](https://dl.google.com/dl/android/maven2/index.html),
[Maven Central](https://repo.maven.apache.org/maven2/), the
[Foojay plugin releases](https://plugins.gradle.org/plugin/org.gradle.toolchains.foojay-resolver-convention)
and the [engine releases](https://github.com/RSG-KH/khmer-calendar-engine/releases/latest).
The versions in the catalog and Foojay 1.0.0 remain the latest stable releases.
Compose libraries continue to follow the stable BOM; preview releases are not selected.

The wrapper was updated from 9.6.0 to [Gradle 9.8.1](https://docs.gradle.org/9.8.1/release-notes.html),
including its generated scripts, JAR and official distribution SHA-256. Gradle's
[compatibility matrix](https://docs.gradle.org/9.8.1/userguide/compatibility.html)
covers AGP 9.4 and JDK 25. The app's SDK levels and version are unchanged.

## Builds

Run from the project root in PowerShell:

```powershell
.\gradlew.bat assembleDebug
.\gradlew.bat assembleRelease bundleRelease
.\gradlew.bat lintDebug
```

The debug APK is `app/build/outputs/apk/debug/app-debug.apk` and uses the Android debug keystore. The unsigned release APK is `app/build/outputs/apk/release/app-release-unsigned.apk`; the release bundle is `app/build/outputs/bundle/release/app-release.aab`. Release distribution requires your signing configuration.

Language splitting is disabled for the app bundle. English and Khmer resources
must both be installed because the app and widgets choose their language
independently of the device locale, including while offline.

## Testing

```powershell
.\gradlew.bat testDebugUnitTest
.\gradlew.bat connectedDebugAndroidTest
python -m unittest discover -s tools/translation -p "test_*.py"
```

Connected tests require a running emulator or device listed by `adb devices`.
Use a disposable emulator for `connectedDebugAndroidTest`: its cleanup can
uninstall the target app and erase its data and launcher widgets. When testing an
existing installation, first back up its app data, install APKs with
`adb install -r`, and invoke only the required tests through `adb shell am instrument`.
Remove only `com.rsgkh.calendar.test` afterward and verify that saved settings and
events were preserved.

| Checks | Android responsibility |
| --- | --- |
| Calendar adapters | Supported range, date conversion, localized year labels and festival results |
| Pinned reference fixtures | Compatibility through the app adapters, including the reviewed 2012 correction |
| Event repository and recurrence | Catalog coverage across 1800–2200, engine parity for calculated events and holy days, recorded date lists, official holiday calendars with citations, rule translation, captured-date comparisons and classification |
| Reminder planner and delivery | Category controls, appearance changes preserving alarms, permissions, 0–180 minute advance and event-time alerts, duplicate delivery, saved event instants, clock/time-zone broadcasts without an activity, daylight saving and repeats |
| Custom-event storage | Database versions 1–3 upgrading to version 4 with zero advance minutes; preserved event data, recurrence, saved offsets and reminder eligibility; invalid reminder values rejected |
| Today refresh | Visible-only polling, immediate refresh on return, midnight, clock jumps and device time-zone changes |
| Memory and lifecycle | Twelve-year event cache eviction, two-country location cache bounds, full-range arithmetic repeat previews, and on-device collection of destroyed activities after recreation and background/foreground cycles |
| Date details | Today-zone clock snapshot, saved default time/place, header-only time/location picker, clickable astrology tables with detail popups and Ask AI queries, nested popup state, grouped event ordering, and master/individual astrology visibility |
| Compose UI | Phone/tablet layouts, both languages, dialogs, font scaling, settings, first-layout event-title sizing, localized filter counts and Today auto-scroll |
| Home screen widgets | Date rollover and event-filter policies, tap navigation into date or event details, hidden personal-event details, Glance labels and badges, Planner week boundaries and scroll targets, snapshots following saved widget choices, appearance-following palettes, English Western zodiac names in both app languages, and in-app preview themes when device and app appearances differ |
| System window behavior | Light/dark edge-to-edge configuration on API 31, 34 and 35 |
| Translation tools | Catalog validation, export, backups and conflicting saves |

The app retains its pinned MomentKH fixture and `tools/generate-calendar-reference.cjs`
to catch consumer regressions. The generator checks the upstream source checksum
before recreating `app/src/test/resources/momentkh-reference.txt`; it does not
capture website event data. Run it with `node tools/generate-calendar-reference.cjs`
or supply a previously downloaded source file as its argument. Agreement with
that fixture is a compatibility check; it is not independent historical validation.
Calendar algorithms and their evidence are maintained and tested in the engine
project, linked from the [integration guide](shared-engine.md).

Shared UI scenarios in `app/src/sharedTest` run under Robolectric and on a device. Device screenshots capture the full display so open dialogs are included. Inspect changed screens when updating layout or source-dialog content.

`CustomEventReminderStorageTest`, `CustomEventRepeatStorageTest` and
`NotificationIntegrationTest` cover the database upgrade paths.
`ReminderLeadTimeTest` checks zero versus positive lead times, both alert phases,
recurring occurrences, midnight/year boundaries, daylight-saving changes and
saving after the advance time has passed. Editor scenarios verify the reminder
range, persistence, Khmer label/Latin input digits, and localized date/time pickers
including native date text entry.

`CalendarUiScenarios.eventPopupTitleKeepsItsFirstMeasuredHeight` checks short/wrapped titles with copy buttons on/off in English and Khmer at 80%, 100% and 150% font sizes. Filter-count coverage exercises year, search, visibility, language and personal-event changes. Today auto-scroll coverage checks the current and another month in both languages, with a separate landscape case in `CalendarRenderTest`.

`WidgetPreviewTest` renders all five chooser pages with both light and dark app themes on light and dark device configurations. Pixel-brightness assertions catch loading the wrong image or applying a second dark-mode conversion. Inspect the actual Glance and Productivity widgets on a launcher when changing their spacing, corner radius or column alignment.

The astrology layout regression checks the actual dialog/action bounds and the watermark's **unclipped** bounds for both Big 3 and Ganzhi, English and Khmer, and 80%, 100% and 150% font sizes. `CalendarRenderTest.astrologyPopupFooterInsets*` covers phone/tablet portrait and landscape plus a short landscape window; `CalendarUiTest.astrologyPopupFooterInsetsOnDevice` runs the same assertions on the emulator's current orientation. The expected bottom inset is 12 dp, independent of the decorative watermark. The whole image viewport must fit inside the popup with a 12 dp inset from the rounded edges. Also inspect the screenshots to confirm that the complete artwork is visible, rather than merely checking footer spacing. Run the device case in both orientations when changing this layout.

The bilingual About/Sources interaction check runs in `CalendarUiTest` on Android, covering real inline-link rendering, the engine link's destination and bundled licenses. Compose tests use the v2 test rules; alarm assertions use Robolectric's current accessors.

For a focused activity-retention check:

```powershell
.\gradlew.bat connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.rsgkh.calendar.LifecycleDeviceTest"
```

`LifecycleDeviceTest` changes settings, backgrounds/resumes and recreates the
activity eight times, then closes the last activity and checks that all nine weak
references clear after garbage collection. It restores the original settings.
This detects retained activities in that scenario; it is not proof that every
dialog, widget host or long-running device session is leak-free. For release
qualification, also inspect heap dumps after extended navigation, location
searches and widget resizing on representative devices. See the
[stability guide](stability-and-performance.md) for the scope of prior device
verification and remaining measurement limits.

For a longer retention check on a disposable emulator with the debug and test
APKs already installed, use an explicit serial and direct instrumentation:

```powershell
adb -s emulator-5580 shell am instrument -w -r -e timeout_msec 180000 -e lifecycleIterations 40 -e class com.rsgkh.calendar.LifecycleDeviceTest com.rsgkh.calendar.test/androidx.test.runner.AndroidJUnitRunner
```

The default remains eight recreations; `lifecycleIterations` accepts 1–100.
Forty recreations must release all 41 activity instances after final closure.
Run the complete device suite with the same command without `-e class ...` or
`-e lifecycleIterations ...`. Direct instrumentation leaves both packages
installed. Inspect the final `OK (N tests)` result: adb's exit status alone does
not establish that the tests passed.

## Startup measurements

[`tools/measure-android-startup.py`](../tools/measure-android-startup.py) records
15 process-cold starts after two discarded warmups, raw `am start -W` output,
median/p90 time to first display, device/build details, the APK checksum and a
final memory snapshot. It requires an explicit device serial and verifies the
installed APK against the supplied file. It stops the app between launches but
does not clear its data, install packages or change device settings.

Use an optimized release APK signed with a local test key on a disposable
emulator, or a dedicated physical test device. Keep device configuration, app
settings/data, compilation mode and background load the same between runs:

```powershell
adb -s emulator-5580 install -r artifacts/dependency-upgrade/after-release.apk
adb -s emulator-5580 shell cmd package compile -m speed -f com.rsgkh.calendar
python tools/measure-android-startup.py --serial emulator-5580 --apk artifacts/dependency-upgrade/after-release.apk --output artifacts/dependency-upgrade/startup-after.json
```

Pass `--adb` with the SDK executable path if it is not on PATH. The APK in this
example is a locally test-signed release artifact, not a distribution package.
Repeat in alternating build order before attributing small differences to a
change. Filesystem caches remain warm, and first display is not fully drawn.
Emulator timings are diagnostic samples, not physical-device release benchmarks;
the memory snapshot does not identify retained objects. Frame timing, battery
use and heap retention require separate measurements.

## Translations

Edit [`translations/catalog.json`](../translations/catalog.json) through the local [translation editor](../tools/translation/README.md), or make an intentional catalog edit and export:

```powershell
python tools/translation/server.py --export
```

The catalog generates `translations.tsv` and launcher strings. Rebuild and reinstall to see changes. Do not hand-edit generated resources. License and notice texts remain verbatim assets outside the translation catalog.

The source catalog contains active app wording. Retired event templates and their
captured occurrences were removed; Git history retains them. Current catalog-event
titles come from `khmer-calendar-data.json`; the Buddhist holy-day label remains
an active translation. Older translation backups with archive entries can still
be opened, but those entries remain excluded from Android exports. The translation
tests verify that literal Kotlin translation keys are present in the generated
output, not merely in the source.

## Event resources

The app owns its event catalog separately from the engine. Normal builds use the committed `khmer-calendar-data.json` and do not run any generator.

To change event definitions, official holiday years or date overrides, edit the catalog directly following the [bundled data guide](reference-event-database.md#maintenance), then run `.\gradlew.bat testDebugUnitTest`. The catalog embeds event titles and sources; the [translation catalog](#translations) continues to supply all other app strings. Website capture and curation belong to the [Khmer Calendar Manager](https://github.com/RSG-KH/khmer-calendar-manager); Android keeps the committed recurrence fixture for consumer regression tests.
