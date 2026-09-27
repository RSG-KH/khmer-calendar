# Stability and performance review

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

## Verification

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

## Memory and performance limits

No retained activity was detected in the device scenario. Resource review also
confirmed scoped cursor/stream closure, application contexts for background
storage work, a 12-year event cache and a two-country location cache. These checks
do not prove the absence of every possible leak. Extended physical-device runs,
heap analysis of repeated dialog/location/widget use, and OEM background behavior
remain additional release checks. No frame-time, battery-life or startup-time
benchmark was performed; the performance changes remove specific redundant
work and allocation rather than claiming a measured overall speedup.
