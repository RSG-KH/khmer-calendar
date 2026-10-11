# Stability and performance

This guide describes the current safeguards and verification limits. Runtime
behavior is maintained in [architecture](architecture.md) and
[UI and responsive design](ui-and-responsive-design.md); reproducible commands
are in [development and testing](development-and-testing.md). Superseded review
narratives and intermediate test counts remain in Git history.

## Runtime safeguards

| Area | Current behavior and regression coverage |
| --- | --- |
| Event data | A synchronized 12-year LRU bounds built-in event caching; fixtures check adapter compatibility, catalog recurrence and official holiday overlays. |
| Personal repeats | Arithmetic daily/weekly previews avoid allocating every date across 1800–2200; tests cover clipped schedules, leap centuries, DST and missing-date policies. |
| Locations | Loading runs off the UI thread and is cancelled with its composition; each dialog retains at most two country documents. Invalid preferences and event rows recover individually. |
| Reminders | A single next-alarm queue uses application-scoped work; appearance-only settings preserve alarms, and personal-only reminders skip built-in calendar generation. Storage migrations and both advance/event-time alerts have regression coverage. |
| Activity lifecycle | Today polling runs only while visible. New settings cancel superseded activity widget refreshes, and destruction cancels remaining work. Database helpers and cursors are closed. |
| Widget rendering | Personal events are read once per snapshot. Only Month builds a full lunar grid; public-holiday coloring reuses the background snapshot. Disabled widgets skip refresh work. |
| Layout | Frame checks cover stable calendar widths across month changes. Native-font checks cover Focus list heights; theme checks cover all five chooser previews. Lunar label sizing observes font/density changes. |
| Translations | Export checks compare generated files with the editable catalog and validate runtime keys. Both languages remain packaged in app bundles. |

`RefreshWidgetAction` remains as a compatibility entry point for callback intents
in widgets rendered by older app versions. Indexed country files, licenses,
framework callbacks, Compose previews and consumed test fixtures are required
assets even where they have no simple literal source reference.

## Maintenance verification — 11 October 2026

Reviewed the current project after `4935a5f`, updated the Gradle wrapper to 9.8.1
and removed obsolete documentation, capture inputs and translation archive data.
All 307 JVM/Robolectric tests across 34 suites and all seven translation-tool
tests passed, with no failures, errors or skips. Debug, optimized unsigned
release, bundle and device-test builds passed. Lint has zero errors and the same
three Planner layout warnings. Connected-device tests were not run in this pass.

The official wrapper JAR checksum, generated translation consistency, local
documentation links/anchors and all 252 indexed country catalogs were checked.
Retained translation entries are unchanged; only two unused repeat labels were
removed from the runtime language file. The editable catalog shrank from 899,144
to 157,339 bytes after removing 125 archived entries and those two labels.
Legacy translation-backup handling remains covered by the editor tests.

The build log is retained locally as `artifacts/maintenance-verification.log`;
JUnit and lint reports are under `app/build`. These are generated artifacts.

## Previous device verification — 10 October 2026

Before the current maintenance pass, the dependency-upgrade review recorded:

- 301 JVM/Robolectric tests across 33 suites and seven translation-tool tests.
- All 43 device tests on disposable Android 12/API 31 and Android 17/API 37
  emulators. Each full run included 40 activity recreations and collection of all
  41 destroyed instances after final closure.
- Debug, optimized unsigned release, bundle and device-test builds; a locally
  test-signed optimized release also installed and launched on API 31.
- Android lint with zero errors and three Planner layout warnings.

These are dated results, not a claim that the current checkout has been rerun on
devices. The original local evidence remains under the ignored
`artifacts/dependency-upgrade/` directory, including `verification.json`,
`device-api31-final.log`, `device-api37-final.log` and `screenshots-api31/`.

## Performance evidence and limits

The prior review measured two alternating before/after batches on an API 37
emulator, each with two discarded warmups and 15 process-cold starts. Optimized
release APKs used the same local signing key and Android's `speed` compilation
mode. Median first-display times were 658/626 ms before and 594/649 ms after the
library updates. Batch variation prevents a reliable speedup claim. These samples
retain warm filesystem caches and do not measure fully drawn content or frame
timing. The raw `startup-before-*.json` and `startup-after-*.json` files remain in
the same local evidence directory.

| Remaining check | Evidence needed |
| --- | --- |
| Physical-device performance | Startup and frame traces on representative phones under comparable thermal, compilation and workload conditions. |
| Extended heap retention | Heap comparisons after repeated dialog, country-search and widget use; activity collection alone does not cover other retained objects. |
| Battery and background delivery | Measurements through doze, reboot and vendor background restrictions on real devices. |
| Launcher coverage | Widget installation, refresh and resizing across multiple launchers. |
| Narrow Khmer lunar labels | At 360 dp with 150% text, some compact labels can still ellipsize; a typography/layout decision is needed. |
| Planner layout structure | Three intentional weighted-layout/parent lint warnings remain; a rewrite must preserve badge sizing and watermark resize bounds. |

Use the [startup measurement tool](development-and-testing.md#startup-measurements)
for repeatable sampling and the documented lifecycle checks for retention
regressions. Automated suites establish the behavior they exercise; they do not
establish overall battery life, a general speedup or the absence of all leaks.
