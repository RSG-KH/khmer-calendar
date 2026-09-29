# PWA 0.12.1 → Android sync

Reviewed on 27 September 2026 against PWA `148c28c` (app 0.12.1.0), starting from Android `7f06e61` (0.11.1, code 26). The feature sync was committed separately from the version bump to **0.12.1, version code 27**. The calculation engine stays at 0.6.0 and the shared event catalog at 0.5.0; the two apps' event JSON is semantically identical.

## Change coverage

| PWA change | Android implementation |
| --- | --- |
| `071d220`: Rising location correction | Superseded by explicit saved place coordinates and its time zone; representative-city and central-meridian approximations removed. |
| `c29f0de`: offline administrative locations | Bundled country index, Cambodia and global ADM1/2/3 catalogs, provenance, attribution, and IANA zone search. All 255 source files are byte-identical to the PWA, as is the additional time-zone index. |
| `c29f0de`: Big 3 defaults and controls | Master switch, retained individual/emoji choices, default past/future time, default Rising place, Today-zone clock snapshot, header-only picker, display-only table cells and session-only overrides. |
| `c29f0de`: combined time/location workflow | Native country → ADM1 → combined ADM2/ADM3 search; saved catalog/custom chips; coordinate-pair paste, decimal/DMS/decimal-minute parsing; country/city/IANA zone search; clear, cancel, save and delete behavior. Settings opens the time wheel directly for the default time; the default location uses the location-only dialog. |
| `5e5c102`: calendar proportions | Month-card width capped at 1.25× its natural height; native row heights retained and columns centered. |
| `601da20`: event lists | Shared ordering across Calendar, Events and date details; timed personal events, untimed personal events, holidays, observances, holy days. Stable personal ties, one date column per day, inset separators, tighter weekday spacing and 2% Today tint with original event colors/artwork. |
| `601da20`: popup and copy fixes | Nested event details preserve the parent date popup and its overrides. Native clipboard failures no longer report success or dismiss source URLs; date/title text also supports native selection. |
| `601da20`: translations and credits | Sources and licenses title, supplied Khmer attribution, corrected អាជ្ញាប័ណ្ណ spelling, source URL popup and Buddhist Shaving Day label. The location source-URL link flows inside the attribution paragraph. Translation resources regenerated from the catalog. |
| `601da20`: stability/performance | Per-field preference recovery, per-row saved-event validation without deleting malformed neighbors, stable SQLite updates, a 12-year event LRU, a two-country location LRU, reused event-search snapshots and cancellable composition-scoped location loading. |

## Original Android adaptations

- Saved locations use compact bordered chips, capped at three rows with independent vertical scrolling. Both catalog and custom locations retain selection and removal actions, including with larger Khmer text. The Events floating search button uses a new right-handle icon; the action then labeled Search online retains the left-handle icon.
- With only Chinese Ganzhi enabled, the date-detail clock opens the time wheel directly, retaining the date popup underneath. Cancel preserves the previous time; OK changes only that popup's time.
- Material dialogs, native time wheels, keyboard, selection/copy feedback, navigation, scrolling tables and touch sizes remain Android-specific. Location suggestions use a lazy list capped at 178 dp; dialog width uses the actual window size. The explicit width avoids a reproduced intrinsic-measurement loop. The astrology time input shares the Add event outlined HH:mm field, direct editing and Pick button; both use the same parser and typography. The astrology time wheel uses a standalone 24-hour caption when no time zone is shown. Footer spacing follows the native date-detail layout; the mode action is on the left, with matching button typography and Cancel/Save kept together at the right (wrapping together on narrow screens). Administrative labels follow country and language. Cambodia offers communes only, global countries also offer ADM2 points; localized country names retain English hints. Prefix/alias ranking, unavailable-coordinate/zone hints, and empty-catalog guidance match the PWA.
- The existing Android personal-event editor still requires a time for notification scheduling. The shared list comparator also handles untimed entries and preserves equal-time saved order.
- Native widgets follow the new astrology master switch. Notification and widget preferences are preserved; changing only astrology defaults does not reschedule alarms.
- Browser service-worker, DOM-listener and Clipboard API fallbacks do not apply to Android. Compose owns popup disposal; location data is packaged for offline access. Android's asset merger expands source `.gz` files to `.json` paths; packaged file contents were checked against the original data.
- PWA source files and its maintainer-certified UI contract were not changed. The existing local time-wheel footer edit in `NotificationSettings.kt` was preserved.

## Subsequent Android changes — 28 September 2026

Each entire Big 3 or Ganzhi table now opens its own detail popup, including taps
on its heading and rows. Horizontal swipes still scroll the columns. The child
popups reuse the selected time/place results and emoji choices, with a Sun-sign
or Year-animal watermark. Big 3 adds three sign/element/ruler rows at
`12.readableSp` with `20.readableSp` line height. Learn more and both astrology
popups use **Ask AI** / **សួរ AI**, retaining the search and external-browser icons.

Current behavior is maintained in [UI and responsive design](ui-and-responsive-design.md#astrology--zodiac-visibility).
The [stability review](stability-and-performance.md#astrology-popup-follow-up)
records the later 242-test local suite, 29-test emulator run and final font-size checks.

Calendar sizing was subsequently centralized around a fixed five-row reference.
Four-, five- and six-row months now retain identical widths in both orientations;
only their visible heights change. The tablet monthly list can expand to 2.0×
the calendar width, with centered columns and at least 12 dp side margins.
Settings rows also gained separators matching the event list. See the current
[layout rules](ui-and-responsive-design.md#grouped-event-lists-and-calendar-width),
[settings separators](ui-and-responsive-design.md#settings-row-separators), and
[246-test local / two-scenario tablet verification](stability-and-performance.md#equal-widths-across-four--five--and-six-row-months).

Settings controls were also updated with single-line labels (`softWrap = false`, `maxLines = 1`) and dynamic width allocation (`LocalSettingsControlMaxWidth`), truncating Rising location labels to 16 characters for Khmer and 12 characters for English/other with trailing `...` while preserving full accessibility descriptions. In-app calculation attributions across event and astrology popups were standardized to “Calculations by Khmer Calendar Engine v0.6.0.” / “គណនាដោយ Khmer Calendar Engine កំណែ 0.6.0 ។” at `10.readableSp`. Monthly event list headers gained the month prefix `ខែ` and event count, and Sources dialog URLs are now presented and copied as numbered lists. Event list subtitles display “Observance · Holiday” / “ពិធី និងទិវា · ថ្ងៃឈប់សម្រាក” in the tertiary holiday color, the Events tab Observances filter includes promoted holidays, and personal event lists display the big day number in the personal event accent color (matching its subtitle).

## Original verification

This section records the September 27 sync checks; counts and artifact names
refer to that run. Current build commands and lifecycle checks are maintained in
[Development and testing](development-and-testing.md).

- `:app:testDebugUnitTest`: **230 tests pass**, including native rendering, recurrence, notifications, widgets, storage recovery, coordinate parsing, data/cache validation, nested dialogs, clipboard failure paths and Settings persistence. The full suite, debug build and lint were rerun with version 0.11.1 (26) before committing.
- Translation tooling: **7 tests pass**.
- `:app:lintDebug`: no errors; 51 warnings remain in existing widget/resources/dependency configuration. No warnings in the new location picker/catalog or updated custom-event storage.
- `:app:assembleDebug`, `:app:assembleRelease`, `:app:bundleRelease` and `:app:assembleDebugAndroidTest` succeed. Release artifacts need the maintainer's distribution signing configuration.
- Rendered layouts reviewed for grouped dark-theme events, native catalog/custom location dialogs, Belgium suggestions, Khmer on a 320 dp phone at 120%, and a short landscape window at 150%.
- Pixel 10 Pro XL emulator: **21 UI scenarios verified**. Twenty passed in the full device run; the bilingual Sources test passed on targeted rerun after scoping its Khmer title selector to the dialog. Coverage includes the actual keyboard, country search, native clipboard/source URLs, both languages, nested date/event dialogs, recurrence editing and notification controls. The debug app was updated in place to 0.12.1 (27); all five existing preference/database files remained byte-identical. Reports: `device-ui-tests.txt` and `device-sources-retest.txt`. A subsequent two-scenario picker run passed for device keyboard search and Khmer labels/action alignment in both catalog and custom modes (`device-location-corrections.txt`), bringing unique device coverage to 22 scenarios. Four targeted scenarios also passed after the clock-caption and inline-source-link corrections, covering both languages, URL copy/return behavior and notification time selection (`device-inline-source-and-clock.txt`).

The shared Add event / astrology time input passed six targeted unit/render checks (typed-time validation, saved wheel changes, nested dialogs, narrow/landscape layouts and event-editor behavior) and three device scenarios in English/Khmer (`device-shared-time-input.txt`). Subsequent targeted runs verified the direct Ganzhi time wheel, compact chip scrolling/selection/removal in both languages with 150% Khmer text, and both search actions. The emulator builds used temporary version 0.12.1 (27); the source version was restored before committing at the maintainer's request.

Build reports and screenshots are under `app/build/reports`. Cache bounds and passing lifecycle regressions reduce known risks; they do not prove the absence of every memory leak. Physical-device longevity, OEM keyboards and extended background/foreground testing remain outside this automated run.
