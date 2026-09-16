# Bundled event data

The Android app packages **3,246 dated event occurrences for 2000–2030** in [`calendar-events.tsv`](../app/src/main/resources/calendar-events.tsv). These records were captured from the publicly rendered [Khmer Lunar Calendar website](https://khmer-lunar-calendar.com/) on 10 September 2026. They are app data, separate from the [shared calculation engine](shared-engine.md).

`EventRepository` uses the snapshot for all covered years and [calculated recurrences](recurring-event-rules.md) outside them, across 1800–2200. It adds engine-derived holy days in every supported year. The Android app does not yet consume event-manager exports; replacing this snapshot requires a separate data migration.

## Runtime format and classification

Each non-comment TSV row contains five tab-separated fields:

| Field | Meaning |
| --- | --- |
| `id` | Stable occurrence identifier derived from the captured date and original label |
| `date` | Gregorian date in `YYYY-MM-DD` format |
| `title_km` | Captured Khmer title |
| `title_en` | Captured English title |
| `official_source_url` | Year-specific holiday source, or an empty field |

Android assigns `DateBasis.WEBSITE` and prefixes IDs with `website:`. A nonempty official-source URL produces `EventKind.HOLIDAY`; other records are `OBSERVANCE`. Translated titles are applied through `event-translations.tsv`, generated from the [translation catalog](../translations/catalog.json). Multiple events and multi-day festival occurrences remain separate records.

The importer matched **44 holiday date/event pairs in 2025–2026** against the transcribed [2025 Ministry of Economy and Finance calendar](https://mef.gov.kh/calendar-holiday-2025/) and [2026 Legal Reform Committee calendar](https://lrc.gov.kh/en/annual-holiday-calendar-2026/). The checked inputs are in [`tools/reference-government-holidays.json`](../tools/reference-government-holidays.json). Holiday status is not inferred for other years.

## Provenance and limits

The capture covered 372 months and 11,323 consecutive dates. The original review database contained those dates, 3,246 event occurrences and 1,533 holy-day flags. Its comparison report checked the captured lunar labels and holy-day flags against the app calculation available at capture time. Those historical counts are not a fresh audit of the released engine.

The records establish what the website displayed when captured. They do not independently establish historical ceremony occurrence, cancellations, future holiday decisions or the correctness of recorded anniversary counts and New Year arrival times. Arrival text is preserved as source data and is not used to schedule notifications. No reference-app executable code or imagery was copied. The original review did not establish an open-data reuse license from the publisher's pages.

A separately supplied legacy database contained 43 recurrence definitions and no dated archive. It was used as a review input, not as a replacement snapshot. Its unverified holiday flags, missing leap-month policies and incomplete festival durations must not be imported as historical facts. Current fallback behavior is defined by the versioned recurrence manifest and its tests.

## Capture artifacts and maintenance

Normal builds use the committed TSV and need neither capture tools nor Python. The raw JSON, SQLite database and original audit report are not versioned; regenerating the snapshot requires the saved external capture.

| File under `artifacts/reference-events/` | Purpose |
| --- | --- |
| `site-observed-months.json` | Required input: captured month grids and original labels |
| `app-lunar-reference.csv` | Required input: daily results exported from the current Android adapter |
| `khmer-calendar-reference-2000-2030.sqlite` | Generated review database with daily observations, occurrences, source fields and capture metadata |
| `audit.json` | Generated coverage, calendar and government-snapshot comparisons |

For a deliberate snapshot rebuild:

1. Restore the saved `site-observed-months.json` to the artifact directory. New captures use the rendered month header and consecutive grid dates; `tools/reference-event-import.cjs` accepts reviewed batches locally.
2. Build the app classes. Run `tools/ExportCalendarReference.java` with the compiled debug Kotlin classes, the pinned engine JAR and Kotlin stdlib on the Java classpath, passing `artifacts/reference-events/app-lunar-reference.csv` as its output argument.
3. Run `python tools/build-reference-events.py`. It validates coverage and comparisons before publishing the SQLite database, audit and runtime TSV.
4. Review changes to dates, IDs, translations and holiday provenance. Update dependent translation and recurrence fixtures as needed, then run the [app tests](development-and-testing.md#testing).

`tools/audit-supplied-events.py "<event-database-directory>"` remains available for comparing a legacy candidate with the bundled snapshot and daily export. It produces review artifacts only. Calendar algorithm evidence is maintained in the engine project, as linked from the integration guide.
