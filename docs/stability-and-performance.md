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

Settings controls dynamically constrain control width (`LocalSettingsControlMaxWidth`) and truncate Rising location button text to 16 characters for Khmer and 12 characters for English/other with trailing `...`, keeping full text in accessibility descriptions and dropdowns on a single line. In-app calculation attribution was consolidated to “Calculations by Khmer Calendar Engine v0.6.0.” / “ការគណនាធ្វើឡើងដោយ Khmer Calendar Engine កំណែ 0.6.0 ។” at `10.readableSp` (`16.readableSp` line height). Monthly event lists format headers with `ខែ` prefix and event count in parentheses. All 246 local tests and translation tooling tests passed.

## Memory and performance limits

No retained activity was detected in the device scenario. Resource review also
confirmed scoped cursor/stream closure, application contexts for background
storage work, a 12-year event cache and a two-country location cache. These checks
do not prove the absence of every possible leak. Extended physical-device runs,
heap analysis of repeated dialog/location/widget use, and OEM background behavior
remain additional release checks. No frame-time, battery-life or startup-time
benchmark was performed; the performance changes remove specific redundant
work and allocation rather than claiming a measured overall speedup.
