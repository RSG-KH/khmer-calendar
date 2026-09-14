# Calendar source review — 10 September 2026

The supplied `D:/DEV/android/tmp/research.md` was a preliminary review. This follow-up inspected the sources and tested the native calendar against a pinned reference. The app remains Kotlin/Compose with offline event calculations and annual government records.

| Source | Finding and decision |
| --- | --- |
| [MomentKH](https://github.com/ThyrithSor/momentkh/tree/ff2bfd558385bd5403780b671fd2eca43b3e2228) | MIT. Useful Khmer lunar and traditional solar New Year algorithms. Native lunar results match all 73,414 dates from 1900–2100; native New Year starts match all 201 years. Its published date corrections within this range are retained. Precise arrival times are not displayed. |
| [MetheaX](https://github.com/MetheaX/khmer-chhankitek-calendar/tree/4d1df001de73fac715d6e4c54f06f69618ff49dd) | MIT Java port, already the basis of our Kotlin lunar engine. Shared ancestry with MomentKH means agreement is not independent proof. Updated the carry rule to inspect consecutive leap-month years, as current MomentKH does. |
| [Khmer Hybrid Calendar](https://github.com/hengloem/khmer-hybrid-calendar-package/tree/88cfdb6dbfa7978203a8d337685f50d254a5e7de) | Inspected the TypeScript integration, event rules, constants and dependency manifest. Do not adopt its holiday dataset unchanged; concrete issues are listed below. |
| [date-chinese](https://github.com/commenthol/date-chinese) | Calculates Chinese calendar dates, Chinese New Year and Qingming. It is an optional source for Chinese festivals, not a Khmer lunar engine. No Chinese-festival subsystem or npm runtime was added in this change. |
| Kizitonwose Calendar suggestion | A UI component would not improve the calendar arithmetic. The existing custom Compose month grid already meets the current scope, so it was retained. |

## Buddhist Era rollover convention

The Buddhist year changes on 1 Roach of Pisakh (the first waning day following Visak Bochea). This is the existing civil calendar calculation, separate from the animal-year and Sak transitions around Khmer New Year. Keep this implementation detail in developer documentation rather than the user-facing source description. The calendar arithmetic and pinned reference tests are unchanged.

## Confirmed hybrid-package issues

At commit `88cfdb6dbfa7978203a8d337685f50d254a5e7de`:

- `src/core/KhmerCalendar.ts:41` reads `result.khmer.lunarYear || 0`; current MomentKH returns `beYear`. The expected year is therefore lost by that integration.
- Lines 37–38 convert the numeric moon-phase enum to a string. `src/utils/constants.ts:16` uses names such as `Full Moon`, so these keys cannot match and the fallback icon is used.
- The event generator sets `isHoliday: true` for regular Buddhist observances. Its public API documents that flag as a public holiday. Those meanings must remain separate.
- Lines 357–364 fix Royal Ploughing to May 11 in every year. The government lists **May 15 in 2025** and **May 5 in 2026**. Our 4 Roach Pisakh calculation agrees with those anchors.

These are source-level findings, not a claim that the complete npm package or the [live demo](https://khc.loemheng.com/) was runtime-tested. The demo was not independently verified.

## Independent checks and remaining limits

Government inputs: [MEF 2025 calendar](https://mef.gov.kh/calendar-holiday-2025/), [Legal Reform Committee 2026 calendar](https://lrc.gov.kh/en/annual-holiday-calendar-2026/), and [National Radio's 2024 New Year announcement](https://rnk.gov.kh/index.php/interior-minister-calls-on-authorities-at-all-levels-to-be-well-prepared-to-ensure-safety-security-and-social-order-during-the-coming-traditional-khmer-new-year-celebration). These supply festival-date checks independent of the software ports, including the four-day New Year in 2024. March 8 coverage from 1975 follows the [UN observance history](https://www.un.org/womenwatch/feature/iwd/iwdarchives.html).

Government holiday lists are bundled for 2025 and 2026. All other years retain calculated traditions and applicable fixed anniversaries, visibly separated from confirmed days off. This does not certify historical celebration, cancellations, or every lunar date across two centuries. More independently verified almanac samples can be added without changing the UI or requiring a server.

The native engine, small event catalog and annual snapshots are sufficient for this app. A paid event API and decompilation of the proprietary reference app are unnecessary for the implemented scope.
