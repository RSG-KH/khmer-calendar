# Stability and performance review

## 10 October 2026 review

Reviewed the four local commits `4433457`, `15f08e6`, `fd54d7a` and `7989eeb`
against `4935a5f`, plus the corrective working-tree changes. The initial working
tree was clean.

- **Focus widget sizing:** The initial height cutoff hid yesterday and tomorrow
  at the standard 4×2 size. It was corrected after launcher verification: compact
  header/card padding and 58 dp adjacent panels now keep all three lists visible
  while reserving a readable current-day row. Native-font layout checks require
  three lists in both languages across seven height/font combinations, including
  280 × 140 dp, app fonts through 200% and combined app/system font scaling.
  The earlier 3 dp measurement used Robolectric's legacy text renderer and should
  not be treated as a measured launcher result.
- **Lunar label sizing:** A UI regression reproduced stale full labels after
  changing from 80% to 150% font size without changing month. Sizing now observes
  density and the text measurer, reuses one month's lunar-date calculations, and
  skips text measurement when lunar labels are hidden. The regression verifies
  switching to compact labels and back, with no ellipsis at 411 dp width.
- **Widget rendering:** Public-holiday detection now uses a flag from the existing
  background snapshot load, preserving day coloring when holiday event details
  are hidden. Composition no longer queries the synchronized event repository or
  catches every `Throwable` during this check.
- **Translation consistency:** The generated Khmer Sunday subtitle differed from
  its source catalog and failed the export-consistency test. The catalog now
  preserves the corrected wording, and the generated resource checksum matches.
- **Code and tests:** Removed unused imports/locals and four tests of copied
  algorithms. Actual rendered UI checks now cover day and event-dialog colors,
  font-size changes and widget list heights. Swipe coverage also verifies the
  1800/2200 boundaries and that vertical scrolling does not change the year.

Full verification before the subsequent Focus widget compact-layout correction:

| Check | Result |
| --- | --- |
| JVM/Robolectric | 286 tests passed; zero failures, errors or skips |
| Android emulator | All 38 tests passed on Pixel 10 Pro XL, Android 17/API 37 |
| Activity retention | All nine destroyed activities were collected after eight recreation/background/foreground cycles and final closure |
| Translation tools | All 7 tests passed, including generated-output consistency |
| Builds | Debug and optimized unsigned release APKs built successfully |
| Android lint | Zero errors; 49 warnings, unchanged from the initial review run |
| Patch hygiene | `git diff --check` passed |

The final combined Gradle verification completed in 6 minutes 38 seconds.
Reports are under `app/build/reports`, `app/build/test-results/testDebugUnitTest`
and `app/build/outputs/androidTest-results/connected/debug`. Existing lint
warnings concern resources/previews, layout structure, the window-size API and
available dependency updates. Dependency versions were not changed.

After the 4×2 correction, the focused widget regression passed all fourteen
native-font layout cases. Debug installation, optimized release build and lint
passed again. The updated APK was installed on the running Pixel 10a/API 37
emulator; both existing standard and enlarged Focus widgets visibly retained
yesterday and tomorrow. The launcher capture is saved locally at
`artifacts/focus-widget-followup/launcher.png`.

The subsequent advance-reminder feature adds a per-event 0–180 minute setting,
including recurring events, without changing appointment times. Database version
4 preserves existing rows with a zero-minute default. Regression coverage checks
versions 1–3 migrating to version 4, storage validation, both editor languages,
Khmer digit input, midnight/year boundaries, DST gaps/folds, reminder ordering,
recurrence progression, and saving after the selected advance time has passed.
All 298 JVM/Robolectric tests across 33 suites and all 7 translation-tool tests
passed. The optimized release build and lint passed with zero errors and the same
49 warnings. Both actual notification receiver checks (at event time and 15
minutes early) passed on Pixel 10a/API 37, preserving the event time in the alert.
Both focused editor scenarios also passed on that device after moving their test
entry points into the concrete JVM/device test classes for reliable method
selection. A broader device UI run was stopped after the existing
`deletingCatalogLocationChipsPreservesCurrentInputs` scenario stalled; it is not
counted as a passing full-device run. The two focused JVM editor checks passed
again after the test-entry-point change, and the debug app was reinstalled.
The connected-test runner uninstalled the target app during cleanup, resetting
the emulator's app data. The build was reinstalled, but prior emulator settings
and widgets were not restored. Future connected runs should use a disposable
emulator or preserve the target app's data before running.

The follow-up simplifies the reminder input to one full-width field and adds an
event-time alert after each positive advance reminder. Both phases have localized
messages; optional hourly repeats remain anchored to the event time. All 48
focused scheduling, notification and editor tests passed, including two alerts
per recurring occurrence, DST, midnight, duplicate delivery and delayed-message
wording. All 7 translation-tool tests, the debug/release builds and lint passed.
The English, Khmer and narrow-phone renders were inspected. This update backed
up the emulator's app data and installed in place; saved settings matched the
backup afterward. No connected-test cleanup ran for this follow-up.

The Khmer label follow-up uses **រំលឹកជាមុន (០–១៨០ នាទី)** with Latin digits
in the input. Event and repeat-end date pickers now follow the app language;
date/time pickers retain Latin numeric entry. Five focused editor/render tests,
two direct emulator picker tests and seven translation-tool tests passed. Debug
build and lint passed (0 errors, 49 existing warnings); Khmer reminder, date and
time picker screenshots were inspected. App data was backed up before installing
in place, and saved settings matched afterward. Only the test package was removed.

The final compatibility check reran 42 tests across reminder storage, repeat
storage, notification integration, advance timing, planning and time zones, with
zero failures, errors or skips. These include migrations from all three earlier
database formats, preservation of existing event data and the zero-minute default,
and duplicate/obsolete-alarm handling. No production code changes were needed for
this check. Forward upgrades are covered; downgrading to an older APK after the
database reaches version 4 is unsupported.

This review does not establish an overall speedup or prove that every possible
memory leak is absent. It removes specific redundant calculations and rendering
work. Extended physical-device heap analysis, frame-time/startup/battery
benchmarks and launcher-specific widget checks remain outside this run. Very
narrow calendar cells (observed at 360 dp with 150% text) can still ellipsize some
compact Khmer labels; this review preserves the existing typography and overflow
behavior.

## 10 October 2026 project-wide cleanup

The follow-up inspected tracked source, resources, bundled data, translation
exports, documentation references, tests and build configuration. A clean Git
status was not treated as evidence that every file or declaration was used.

- Removed 36 unused Android resource names: 28 strings in both languages, five
  colors in both themes and three unused drawable files.
- Removed 14 obsolete non-event translations. Preserved 125 historical event
  templates in the translation editor's archive while excluding them from the
  generated app language file. Current event titles remain in the event catalog.
- Removed unused imports, the old time-subtitle and dot helpers, unused lunar
  month lists and an obsolete Ganzhi text formatter. Existing tests now exercise
  the current title/time-zone and Ganzhi formatting paths. Moved the New Year
  reference adapter into test sources and kept the cache-reset test hook internal.
- Kept `RefreshWidgetAction` as a documented compatibility entry point for
  callback intents in widgets rendered by older versions. Framework callbacks,
  Compose previews, licenses, reference fixtures and indexed country data remain.
- Moved the ten light/dark widget preview PNGs into density-independent resource
  folders and changed the chooser to use its actual window height. Removed the
  leftover backup-rule template comments and updated documentation paths.

All 252 indexed countries resolve to their bundled catalogs, including Cambodia's
separate catalog. Local Markdown links resolve. No additional unreferenced active
translation keys or ordinary unused imports were found by the reference scans;
Compose delegated-property imports are required even without a literal call.

Lint is down from 49 warnings to 7, with no errors or unused-resource warnings.
Three remaining Planner XML warnings concern deliberate weighted watermark and
badge layouts. The badge sizing regression verifies short and long dates at
wide and narrow widget widths; a structural rewrite is outside this cleanup.
Four other warnings flag newer dependencies, verified against official releases:

| Dependency | Pinned | Available stable release |
| --- | --- | --- |
| AndroidX Core | 1.19.0 | [1.19.1](https://developer.android.com/jetpack/androidx/releases/core#1.19.1) |
| AndroidX Browser | 1.8.0 | [1.10.0](https://developer.android.com/jetpack/androidx/releases/browser#1.10.0) |
| WorkManager | 2.11.2 | [2.12.0](https://developer.android.com/jetpack/androidx/releases/work#2.12.0) |
| Kotlin / Compose compiler | 2.4.20 | [2.4.21](https://kotlinlang.org/docs/releases.html) |

Dependency versions are unchanged; their updates need their own integration
checks. This audit does not prove that every possible dead path, memory leak or
runtime issue has been eliminated.

Final verification passed 301 JVM/Robolectric tests across 33 suites, all seven
translation-tool tests, the debug and optimized release builds, JavaScript syntax
checking and patch whitespace checks. There were no test failures, errors or
skips. Widget preview checks covered all five pages in both app themes against
both device themes; the moved PNG files are byte-for-byte unchanged. The generated
language file decreased from 81,898 to 43,459 bytes. This cleanup did not run
connected-device tests or modify emulator data.

## 10 October 2026 dependency upgrade and verification follow-up

Completed the four dependency updates identified by the cleanup:

| Dependency | Previous | Updated |
| --- | --- | --- |
| AndroidX Core | 1.19.0 | [1.19.1](https://developer.android.com/jetpack/androidx/releases/core#1.19.1) |
| AndroidX Browser | 1.8.0 | [1.10.0](https://developer.android.com/jetpack/androidx/releases/browser#1.10.0) |
| WorkManager | 2.11.2 | [2.12.0](https://developer.android.com/jetpack/androidx/releases/work#2.12.0) |
| Kotlin / Compose compiler | 2.4.20 | [2.4.21](https://kotlinlang.org/docs/releases.html) |

The shared Kotlin version keeps AGP's Kotlin compiler and the Compose compiler
aligned; release dependency resolution also confirms Kotlin stdlib 2.4.21.
WorkManager's minimum API 24 remains below the app's minimum API 31. No app API
level, database format or distribution signing configuration changed.

The complete device runs exposed assumptions in the shared UI tests. The
location-deletion check now waits for both independently loaded country and
division fields before comparing the draft. Calendar checks accept the existing
abbreviated month headers on narrow phones and scope month selection to the
picker. The localized date-picker check selects its prerequisite Monthly option
through the semantic action and asserts selection, avoiding an unreliable
injected tap after keyboard/inset changes. Picker-language, numeric-entry,
selected-date and draft-preservation assertions remain in place.

| Final check | Result |
| --- | --- |
| JVM/Robolectric | 301 tests across 33 suites; zero failures, errors or skips |
| Android 12 / API 31 | All 43 device tests passed on a disposable Pixel 4 emulator, 1080 × 2280 at 440 dpi |
| Android 17 / API 37 | All 43 device tests passed on a disposable read-only Pixel 10 Pro XL emulator, 1344 × 2992 at 480 dpi |
| Activity retention | Both full device runs used 40 recreation/background/foreground cycles and collected all 41 activities after closure |
| Translation tools | All seven tests passed |
| Builds | Debug APK, optimized unsigned release APK, release bundle and device-test APK built successfully |
| Release compatibility | Locally test-signed optimized release installed and launched on API 31 |
| Android lint | Zero errors, three existing Planner layout warnings; all four dependency warnings resolved |

The formerly stalled catalog-location scenario passed in both complete device
runs. The narrow calendar and Khmer date-picker device captures were inspected.
Direct instrumentation targeted only the disposable emulators, preserving the
existing Pixel 10a installation and its settings, events and widgets.
`LifecycleDeviceTest` now accepts `lifecycleIterations` (1–100, default eight)
for repeatable longer retention runs. Commands and the startup measurement tool
are documented in [Development and testing](development-and-testing.md).

Evidence is retained locally under `artifacts/dependency-upgrade/`, including
`gradle-check.log`, `jvm-lint-final.log`, `device-api31-final.log`,
`device-api37-final.log`, `verification.json` and `screenshots-api31/`.
Earlier failed device logs are retained separately from those final passing runs.

### Emulator startup samples

The new `tools/measure-android-startup.py` recorded two alternating before/after
comparisons on the API 37 emulator. Each batch discarded two warmups and retained
15 process-cold launches (30 measured launches per build). Both APKs were optimized
release builds signed with the same local test key and compiled with Android's
`speed` mode. The emulator used two virtual CPU cores and 3072 MB RAM; window,
transition and animator scales were zero. Builds and device tests had finished,
and the disposable API 31 emulator was shut down before sampling.

| Build / batch | Median first-display time | p90 |
| --- | --- | --- |
| Previous dependencies / 1 | 658 ms | 715 ms |
| Updated dependencies / 1 | 594 ms | 649 ms |
| Previous dependencies / 2 | 626 ms | 816 ms |
| Updated dependencies / 2 | 649 ms | 682 ms |

Only the disposable emulator's app data was cleared before each baseline batch;
each following update was installed in place. The tool verifies the installed
APK checksum, records raw launch output and saves a final memory snapshot in
`startup-before-1.json`, `startup-after-1.json`, `startup-before-2.json` and
`startup-after-2.json` under the evidence directory. These measure first display
with warm filesystem caches, not fully drawn content or frame timing. The
batch-to-batch variation, including a 1633 ms baseline outlier, prevents a reliable
speedup claim. Physical-device measurements are still required.

### Remaining release checks

| Gap | Evidence still needed |
| --- | --- |
| Physical-device performance | Startup and frame-time traces on representative phones, with consistent thermal, compilation and workload conditions |
| Extended heap retention | Heap comparisons after repeated dialog, country-search and widget use; activity collection alone does not cover other retained objects |
| Battery and OEM background behavior | Measured idle/active battery use plus reminder and widget delivery through doze, reboot and vendor background restrictions on real devices |
| Launcher coverage | Installation, refresh and resizing across multiple launchers; the existing Focus check covers only the recorded emulator launcher |
| Narrow Khmer lunar labels | The existing 360 dp / 150% text ellipsis case still needs a typography/layout decision; these dependency updates preserve rendering behavior |
| Planner layout structure | Three intentional weighted-layout/parent warnings remain; any rewrite must preserve badge sizing and the watermark's resize bounds |

Only emulators were connected for this follow-up. These remaining checks are
not counted as completed by the passing automated suites.

## Earlier verification records

Reviewed on 28 September 2026 for Android 0.12.1 (27), engine 0.6.0.
This is a dated verification record; [Development and testing](development-and-testing.md)
contains the maintained commands.

## Changes

- Daily and weekly repeat previews use an arithmetic list with constant retained
  storage and constant-time count/index access. A daily series covering 1800–2200
  still reports all 146,462 occurrences, but no longer allocates that many dates
  when the editor only displays the first few and the last date. Monthly/yearly
  preview behavior is unchanged.
- Reminder planning skips built-in calendar generation when only personal events
  are eligible, or all built-in categories are disabled/hidden.
- Saving unchanged settings does no work. A newer settings change cancels the
  preceding activity widget refresh; lifecycle destruction also cancels it.
  The coroutine uses the application context, and WorkManager retains its fallback.
- Month widgets read personal events once per snapshot, expand one date window
  and group by date for both daily cards and month markers. The window includes
  adjacent days across month/year boundaries.
- App bundles retain both language resources, so offline English/Khmer switching
  is independent of the device's installed language splits.
- The bilingual About/Sources device test scrolls each repository link into view
  before checking it, supporting smaller phones as well as the larger emulator.

## Original stability verification

| Check | Result |
| --- | --- |
| JVM/Robolectric suite | 234 tests passed; no failures, errors or skips |
| Android device coverage | All 27 tests passed in one complete confirmation run on Pixel 10a, Android 17/API 37; no failures, errors or skips |
| Activity retention | All nine destroyed `MainActivity` instances were collected after eight recreation/background/foreground cycles and final closure |
| Translation tools | 7 tests passed |
| Debug APK, optimized release APK and release bundle | Built successfully; distribution signing remains external |
| Android lint | No errors; 50 warnings (down from 51 after fixing language splitting) |
| Documentation | Local Markdown links checked; event counts, dependency/build settings, lifecycle, alarm fallback, location storage and backup descriptions compared with code |

The first device run passed 26/27 tests. The About/Sources test failed because
scrolling to the version label did not bring both repository links onto the
smaller screen. After fixing the test's scrolling, that complete bilingual
scenario passed on rerun. The retention, notification and translation device
checks passed in the full run. A subsequent complete run on the user's running
Pixel 10a emulator passed all 27 tests together in 2 minutes 38 seconds, including
the corrected bilingual Sources scenario and collection of all nine destroyed
activities. Its report is saved locally as
`artifacts/stability-audit/device-confirmation.xml`.

Regression coverage includes full-range and huge-interval previews, cache
eviction while preserving recent years, custom-only reminder planning without
built-in loads, and personal widget events spanning New Year. Existing tests
also cover location-cache bounds, preference/row recovery, visible-only polling,
recurrence, daylight-saving transitions, widget filtering and Compose dialogs.

Lint's remaining warnings concern unused resources, preview drawable locations,
widget layout structure, a window-size API, version-catalog placement and
available dependency updates. Dependency versions were preserved for this review.

Reports are generated under `app/build/reports` and `app/build/test-results`.
The local review logs and initial device results are retained under the ignored
`artifacts/stability-audit/` directory.

## Astrology popup follow-up

Later on 28 September, the Big 3/Ganzhi detail popups and Ask AI actions passed
the expanded **242-test JVM/Robolectric suite**, **29 Android tests** on the same
Pixel 10a emulator, and **7 translation-tool tests**. The Android run repeated
the activity-retention check successfully. Debug and test APK builds and lint
also passed; lint retained the same 50 warnings and no errors.

Coverage includes both languages, emoji/name preferences, shared table values,
preserved parent time/place state, calculated Sun/Year watermarks, unavailable
signs, browser query contents, and narrow/landscape layouts at 150% text.
A further gesture check verified horizontal scrolling without opening a popup
and opening it by tapping the table header.

After reducing the three Big 3 sign-detail rows to `12.readableSp` with
`20.readableSp` line height, all four focused popup/render checks and the debug
build passed. The updated build was installed on the emulator; the complete
device suite above preceded this typography-only adjustment. Logs are retained
under `artifacts/stability-audit/` as `astrology-final-check.log`,
`astrology-device-check.log`, `astrology-gestures-check.log` and
`astrology-smaller-text-check.log`.

## Calendar sizing follow-up

The month-navigation regression reproduced a visible width change from 742 dp
to 432.5 dp on consecutive frames. Resetting the remembered natural height on
each month change drew an uncapped layout before `onSizeChanged` supplied its
replacement width.

`CalendarContentLayout` now resolves the dimensions before placing visible
content, using shared weekday/legend layout and grid row metrics. The sizing
slot excludes date calculations and artwork; both layout slots use fixed keys.
April's decorative images no longer contribute to the measured card height.
This first fix preserved the calendar's 1.25× natural-height cap, tablet
event-list 2.0× width cap, centering and outer margins. It removed changes
between frames but still allowed different months to have different widths.

Verification on 28 September: **246 JVM/Robolectric tests passed**, including
four frame-by-frame regressions across phone/tablet, portrait/landscape,
four/five/six-week months, April, a personal-event legend, and Khmer at 150%
with longer weekday names. **Both targeted Android frame checks passed** on
the running Pixel Tablet emulator (Android 17/API 37). Debug and test APKs
built successfully, and the updated app was installed and reopened.
Logs are `calendar-resize-reproduction.log`, `calendar-resize-full-check.log`
and `calendar-resize-device-check.log` under `artifacts/stability-audit/`.

### Equal widths across four-, five- and six-row months

A subsequent regression reproduced the remaining month-to-month width change
in tablet portrait and landscape. The reference now always uses five grid rows
and the standard legend, independent of the displayed month and its personal
events. The 1.25× cap applies to that reference height. Visible months keep
their actual row counts and natural heights, while calendar and event-list
left/right edges remain fixed. Font/window/settings changes still adapt them.

The strengthened tests compare widths and horizontal positions across months,
as well as consecutive frames, and verify that four-row February remains
shorter and six-row May taller than five-row March. All **246 local tests** and
**both targeted tablet emulator tests** passed again; debug/test builds passed.
The updated app was installed. Logs are `calendar-stable-width-reproduction.log`,
`calendar-stable-width-full-check.log` and `calendar-stable-width-device-check.log`.

### Settings layout and attribution follow-up

Settings controls dynamically constrain control width (`LocalSettingsControlMaxWidth`) and truncate Rising location button text to 16 characters for Khmer and 12 characters for English/other with trailing `...`, keeping full text in accessibility descriptions and dropdowns on a single line. In-app calculation attribution was consolidated to “Calculations by Khmer Calendar Engine v0.6.0.” / “គណនាដោយ Khmer Calendar Engine កំណែ 0.6.0 ។” at `10.readableSp` (`16.readableSp` line height). Monthly event lists format headers with `ខែ` prefix and event count in parentheses. Event list subtitles display “Observance · Holiday” / “ពិធី និងទិវា · ថ្ងៃឈប់សម្រាក” for official holidays, the Events tab Observances filter includes promoted holidays, and personal event day groups highlight the big day number with the personal event accent color. All local tests and translation tooling tests passed.

## Memory and performance limits

No retained activity was detected in the device scenario. Resource review also
confirmed scoped cursor/stream closure, application contexts for background
storage work, a 12-year event cache and a two-country location cache. These checks
do not prove the absence of every possible leak. Extended physical-device runs,
heap analysis of repeated dialog/location/widget use, and OEM background behavior
remain additional release checks. The September review performed no frame-time,
battery-life or startup-time benchmark. The October dependency follow-up adds
emulator startup samples; physical-device performance and battery measurements
remain open. Removing specific redundant work and allocation does not establish
a measured overall speedup.
