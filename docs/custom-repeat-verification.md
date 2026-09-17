# Custom recurring events verification

Verified on 2026-09-17. This change adds PWA-compatible custom repeats to Android.
The feature was tested on the 0.2.1 codebase; it ships in 0.3.0 (build 12), versioned
in a separate commit.

## Behavior

- None, Days (default 3), Weekly, Monthly and Yearly use Android Material chips.
- Every repeat requires an inclusive end date. Invalid dates and intervals block Save.
- Monthly day 29/30/31 and yearly February 29 keep the original anchor. Missing-date
  switches appear only when their affected dates fall within the chosen range.
- The preview shows scheduled dates, count, last occurrence and skipped months/years.
- Start and end dates use Android's existing calendar picker; time keeps the existing
  time selection dialog. Date labels omit the fixed format. Notes occupy two lines
  before the repeat divider; the header displays the zone and date-specific offset.
- One database row stores a whole series. Editing/deleting from any occurrence
  applies to that series. Database upgrades preserve existing single events.
- Reminders use each occurrence's saved zone, honor category preferences and advance
  through the existing single-alarm queue. New past occurrences do not generate
  retrospective reminders; future occurrences of a past anchor remain eligible.

## Results

| Check | Result |
| --- | --- |
| `gradlew.bat testDebugUnitTest` | 138 passed; no failures or skips |
| `gradlew.bat lintDebug` | No issues found |
| `gradlew.bat assembleDebug` | Debug APK built successfully |
| `gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.rsgkh.calendar.CalendarUiTest` | All 19 UI tests passed on Pixel 10 Pro XL, Android 17 emulator |
| Translation editor Python suite | 8 passed |
| Repeat translation parity | All 23 keys match PWA English and Khmer text exactly |

Added regression checks cover 1,628 independently calculated full/clipped schedules,
including 1900/2000/2100 leap-century rules, huge day intervals and the supported
1800–2200 range. Further checks cover DST gaps/folds, date-line crossings, month-query
partitioning, SQLite v1/v2 upgrades, persistence, invalid rule rejection, series
conversion/deletion, alarm delivery and cancellation, and future reminders from
past anchors.

UI checks verify preview counts, all required-end modes, default/invalid day
intervals, native end-date picking, later-occurrence editing, changing display zones,
conversion to/from a single event, and series deletion. Rendered screenshots were
reviewed for English and Khmer, including a 320 dp phone at 120% text and an 800 dp
tablet at 150% text. Repeat chips use one horizontally scrollable row; switch labels,
preview dates and series actions wrap without horizontal clipping.

The connected device was an emulator; physical-device keyboard and OEM-specific
rendering were not checked. The build emits existing Java native-access/deprecation
warnings from test tooling, with no lint findings.

The review APK is `app/build/outputs/apk/debug/app-debug.apk`. Generated screenshots
are under `app/build/reports/screenshots/` and are not committed.
