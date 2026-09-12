# Website event database capture

Captured on 10 September 2026 from the publicly rendered [Khmer Lunar Calendar](https://khmer-lunar-calendar.com/).

The website's date search explicitly accepts **2000–2030**. A 1900 date was rejected with that range. Every month from January 2000 through December 2030 was then opened and captured. The [Android app listing](https://play.google.com/store/apps/details?id=com.rotha.calendar2015) advertises 1900–2100; that broader dataset was not obtained from the website or APK.

This database now supplies the app's **bundled events for 2000–2030**. A compact UTF-8 resource generated directly from SQLite is packaged in the APK and read by the shared event repository. The old hard-coded event catalog and festival recurrence rules are removed. No reference-app executable code, imagery, articles or descriptions were copied into this project.

## Files

All outputs are in `artifacts/reference-events/`:

| File | Purpose |
| --- | --- |
| `khmer-calendar-reference-2000-2030.sqlite` | SQLite database, 2,662,400 bytes (2.54 MiB), including checked holiday provenance. |
| `site-observed-months.json` | Original month headers, dated cells, lunar labels, holy-day markers and bilingual event labels. |
| `app-lunar-reference.csv` | Native app calculation for comparison; exported from the compiled app classes. |
| `audit.json` | Counts, per-year totals, calendar discrepancies and holiday comparison results. |

The database contains **11,323 dates, 3,246 event occurrences and 1,533 holy days**. All event entries have both Khmer and English labels. Multiple events on the same date remain separate. Repeated days of a festival remain separate dated entries.

## Validation

- All 372 expected months and 11,323 consecutive dates are present, without duplicate dates or duplicate event labels on a date.
- Each rendered grid was sorted by its visible row/column position and checked against consecutive Gregorian dates. Only cells inside the actual displayed month were retained.
- All 10,908 explicit lunar day/phase labels match the native engine. The remaining 384 month-start labels and 31 Buddhist-year transition labels also match.
- All 11,323 holy-day flags match the native engine, including short lunar months.
- All 32 event categories across the app's existing 2025 and 2026 government snapshots match the website dates: 44 holiday date/event pairs. The snapshots cite the [2025 Ministry of Economy and Finance calendar](https://mef.gov.kh/calendar-holiday-2025/) and [2026 Legal Reform Committee calendar](https://lrc.gov.kh/en/annual-holiday-calendar-2026/). This compares the already transcribed snapshots; it is not a new independent review of every historical holiday.
- SQLite integrity and foreign-key checks pass, and database row counts match the raw capture.

The website's date-search jump displayed a month after the requested month during inspection. Capture therefore used the **actual month header and grid**, validating each next-month transition, instead of trusting the search input.

## What the records establish

These records establish that the website displayed an event label on a date when captured. They do not independently establish historical ceremony occurrence, cancellations, postponed leave or future government decisions. Matching lunar engines may share algorithms; agreement alone does not certify every historical date.

The rendered text did not provide a reliable public-holiday classification. For 44 matching occurrences in 2025–2026, the importer sets `is_public_holiday=1`, `verification_status=matched_government_snapshot` and `official_source_url` from the reviewed government inputs. All other entries retain **NULL** holiday status and `observed_on_website`; no adjacent-year status is inferred. Turning every entry into a public holiday would incorrectly include birthdays, cultural anniversaries and international observances. Original anniversary counts and New Year arrival times remain in `raw_title`; no notification times are inferred from title text.

No open-data reuse license was established from the inspected publisher pages. [Rotha Apps](https://rotha-apps.com/) identifies the publisher; its [privacy page](https://rotha-apps.com/privacy) describes privacy practices, without providing an open-data license.

The app uses these dated records throughout 2000–2030. The missing-event-data note appears only outside that range; lunar dates, holy days and custom events remain supported across 1900–2100. Public-holiday verification is explained in source details separately from event coverage. Any extension must retain its own provenance. Updates can add new years or corrections without requiring a paid event API.

## Schema and reproducibility

`days(date, lunar_label, holy_day)` stores the daily reference. `events` stores an occurrence ID, date, Khmer/English names, raw label, source URL and verification fields. `metadata` records coverage, capture time, method and the raw file's SHA-256 hash. Dates use ISO `YYYY-MM-DD`, making range queries and indexes straightforward.

Example:

```sql
SELECT date, title_km, title_en
FROM events
WHERE date BETWEEN '2026-01-01' AND '2026-12-31'
ORDER BY date, id;
```

Capture used ordinary browser month navigation and read only the rendered DOM. Direct HTTP file requests returned Cloudflare errors, so no protection was bypassed. Public observations were pasted into a temporary form served by `tools/reference-event-import.cjs` on `127.0.0.1:8765`; that receiver was stopped after capture. It merges validated batches by displayed month.

`tools/ExportCalendarReference.java` runs with the app's compiled debug Kotlin classes and Kotlin standard library on the Java classpath, writing `artifacts/reference-events/app-lunar-reference.csv`. Run `python tools/build-reference-events.py` afterward to rebuild SQLite, the audit and `app/src/main/resources/calendar-events.tsv` from the saved raw capture. The builder validates full range coverage, preserves original labels and stops publication on calendar or government-anchor discrepancies. The APK resource contains 3,246 occurrence rows and a SHA-256 reference to the database. The same repository is used by Android screens, notification receivers, JVM tests and Compose previews.
