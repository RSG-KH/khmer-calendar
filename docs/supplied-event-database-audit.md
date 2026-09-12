# Supplied event database audit

Reviewed on 10 September 2026, after creating the initial Git checkpoint `0217917`.

The supplied `D:/DEV/android/event_database` can be read and converted. Its calendar table contains **43 recurrence definitions and zero dated event occurrences**. It does not provide an expanded 1900–2100 archive. The existing 2000–2030 app snapshot remains the runtime event source.

## Contents and provenance

`events.db` is an intact 118,784-byte SQLite database. Its SHA-256 is `b4bc6631f7f81b981a230bb5f3bf9d09f207c929e5ebb88d6b8cd02c23bdb309`.

| Table | Rows | Relevance |
| --- | ---: | --- |
| `calendar_events` | 43 | Event names, recurrence parameters and unverified category/anniversary claims |
| `table_news_menu_data` | 30 | News feed, not calendar event occurrences |
| `table_fuel_prices` | 11 | Fuel-price feed |
| `table_theme` | 4 | Theme presets |
| `table_reminder_data` | 3 | Explicitly described as sample reminders in the generator |
| `table_feng_shui` | 0 | Empty |
| `table_notification_data` | 0 | Empty |

SQLite also has its internal `sqlite_sequence` table. The 43 calendar rows exactly match both `events.json` and the literal `events_data` list in `generate_database.py`. The supplied generator embeds that list; it does not extract the calendar rules from an APK or implement calendar calculations. Its separate news/fuel import sections reference local tool-output files. The provided files do not establish how the original application obtained or evaluated these calendar rules.

The generator was parsed as Python syntax and its event-list literal was read without executing the script. The source database was opened read-only. News, reminders, themes, feed links and descriptive prose were not included in the converted calendar candidate.

## What can be calculated from the fields

| Calendar type | Rules | Assessment |
| --- | ---: | --- |
| Gregorian/solar | 23 | Month/day matching is straightforward; effective years and changing holiday status still need evidence. |
| Khmer lunar | 10 | Most fit the existing native lunar engine; Asadh rules lack intercalary-month selection and one rule disagrees with captured dates. |
| Chinese lunar | 5 | Requires a Chinese-calendar algorithm and month-length/leap-month policy. |
| Solar term | 2 | Requires term-date calculations; the month/day fields alone are insufficient. |
| Solar ingress/New Year | 3 | Requires annual New Year calculations and festival-day handling; the three April dates are not a dated archive. |

The project already has Khmer lunar and New Year calculations. That makes several definitions useful as review candidates, but it does not validate the source's historical coverage or supply the missing Chinese and solar-term calculations.

## Comparison with the current app snapshot

The audit compares explicit event-name aliases against the app's **3,246 captured occurrences for 2000–2030**, and evaluates solar/Khmer-lunar parameters against the existing native daily-calendar export. Chinese and astronomical rules are left unevaluated. This is a compatibility check against the captured website, not an independent historical or government-calendar certification.

- The supplied event families account for 1,452 captured occurrences. **1,794 occurrences belong to families absent from the supplied catalog**, including many international observances, heritage anniversaries and Ben days.
- Literal solar and Khmer-lunar matching produces 1,001 predictions; 941 match the corresponding captured event dates and 60 do not. Some unmatched predictions are years in which the website has no corresponding event, so absence alone is not proof of a wrong civil date.
- **Candle-making differs:** the supplied 15-waxing-Asadh rule gives **10 July 2025**; the captured event is **3 July 2025**. None of its 20 ordinary-Asadh predictions match that event's captured dates.
- **Leap-month handling is missing:** literal Asadh matching yields no beginning-of-Lent or candle-making date in 11 years, including 2026. The capture includes those events on 30 July and 22 July 2026 respectively. A second-Asadh policy is needed.
- **Festival duration is incomplete:** Pchum Ben has one source date versus three captured dates each year, leaving 62 dates unrepresented. Chinese New Year has only a first-day definition; the capture contains three days per year. The three New Year definitions also do not encode the five extra Vanabat dates found in the 31-year capture.
- **Anniversary bases disagree in 124 captured occurrences** across four event families. For example, the source computes the 116th Women's Day anniversary in 2026 while the captured label says 115th. National Culture Day is 27 versus 28, Children's Day is 76 versus 77, and the combined national/world environment label is 53 versus 30. These distinctions require review rather than silently replacing labels.
- The source has **21 public-holiday flags** and six holy-day flags, with no effective-year or official-source fields. A birth/foundation `base_year` is not a holiday's first applicable year. These flags cannot establish official leave throughout 1900–2100.

## Converted artifacts

Generated under `artifacts/supplied-event-audit/`:

| File | Purpose | Size |
| --- | --- | ---: |
| `calendar-rules.review.json` | Compact calendar-only definitions; source classifications explicitly marked as unverified claims | 18,719 bytes |
| `calendar-rules.review.tsv` | Flat calendar-only table for inspection | 7,972 bytes |
| `audit.json` | Source checksums, table counts, explicit date comparisons, missing dates and anniversary differences | Generated report |

The JSON compresses to **2,806 bytes with zlib DEFLATE**. Size is not the obstacle. The exported JSON is marked `review_only_not_app_data`, includes the database checksum, and contains no invented occurrences. The app's event resource is not changed by this tool.

Reproduce the audit and conversion with:

```powershell
python tools/audit-supplied-events.py D:/DEV/android/event_database
```

It uses `app/src/main/resources/calendar-events.tsv` and the existing `artifacts/reference-events/app-lunar-reference.csv`. The latter is exported by `tools/ExportCalendarReference.java` from the native app engine. The input directory is never modified.

## Bundling decision

Keep the existing dated snapshot. This catalog is useful for reviewing a future recurrence layer, but replacing the snapshot would discard coverage and introduce known discrepancies. Any future extension should use corrected rules with explicit effective-year limits and leap-month/duration policies, separately identify calculated occurrences, and apply public-holiday status only where a dated official source supports it. Annual decisions and exceptions still require dated updates. The supplied files alone do not resolve those requirements.

## Follow-up implementation

A subsequent [reviewed recurrence fallback](recurring-event-rules.md) selects 30 definitions as 28 rules, implements the missing durations and second-Asadh policy, and preserves all dated snapshot years. The audit findings above remain the original audit record; unsupported classifications and unresolved rules were not imported.
