# Bundled event data

The Android app packages its event catalog in [`khmer-calendar-data.json`](../app/src/main/resources/khmer-calendar-data.json) — one versioned JSON resource (`schemaVersion: 2`) holding recurrence rules, recorded date lists, official holiday calendars, reviewed date overrides and their sources. It is app data, separate from the [shared calculation engine](shared-engine.md). Version 0.4.0 replaced the former three-resource pipeline (the 3,246-row captured snapshot, the precomputed engine date cache and the runtime rules TSV) with this single catalog.

`dataVersion` inside the file tracks catalog data revisions independently of the app version. Titles for catalog events are stored in the catalog itself, including `{anniversary}` placeholders resolved per year; only the holy-day label comes from the shared app translations.

## Catalog structure

| Top-level field | Contents |
| --- | --- |
| `schemaVersion` | Catalog schema major version; the current bundle is `2` |
| `dataVersion` | Data revision of this bundle |
| `sources` | Provenance records referenced by `sourceIds` elsewhere |
| `events` | 124 event definitions: rules and recorded dates |
| `holidayCalendars` | Official public-holiday calendars, one per year (2020–2027) |
| `overrides` | Reviewed per-year date replacements for specific events |
| `eventCalendars` | Reserved for future per-year calendar records; currently empty |

### Sources

Each source records `id`, `kind` (`government` or `calendar`), `title`, `publisher` and optional `url`, `reference`, `publishedOn` and `notes`. The bundle currently carries one calendar source — the [Khmer Lunar Calendar website](https://khmer-lunar-calendar.com/) capture of 10 September 2026 that reviewed the event definitions — and ten government sources: the annual holiday subdecrees for 2020–2027, the [2025 Ministry of Economy and Finance calendar](https://mef.gov.kh/calendar-holiday-2025/) and the [2026 Legal Reform Committee calendar](https://lrc.gov.kh/en/annual-holiday-calendar-2026/). Event details cite the first government source carrying a reference or URL.

### Events

Each event has `id`, `kind` (`observance`, `traditional` or `historical`), bilingual `names` (plus optional `description`), `sourceIds`, and one of two date carriers:

- **`rule`** (100 events): engine `RecurrenceRule` fields — 72 `solar`, 23 `khmer_lunar`, 2 `solar_nth_weekday` and the 3 Khmer New Year stages. Optional `anniversaryBase` inserts `year − anniversaryBase` into the `{anniversary}` title placeholder. `historical` events may set `originalDate`, before which occurrences are suppressed.
- **`dates`** (24 events): explicit ISO date lists — the nine Chinese festivals (each 31 captured years) and fifteen fixed heritage milestones such as the UNESCO inscription anniversaries. These are emitted as `DateBasis.RECORDED` without calculation.

### Holiday calendars and overrides

Each `holidayCalendars` year carries `coverage` (`complete` for all bundled years) and `holidays` with `id`, bilingual `names`, explicit `dates`, `status` (`cancelled` entries are skipped), `sourceIds` and an optional `eventId` linking a catalog event — used to resolve `{anniversary}` counts. Years 2020–2027 are bundled; 173 official days in total, each carrying a subdecree or ministry citation.

`overrides` pin a specific `eventId`/`year` to explicit `dates`, with a mandatory `sourceId` and `reason`. They preserve reviewed differences between captured records and the calculation — currently the 2005–2019 three-day King Sihamoni birthday holiday blocks, where the rule yields only May 14. Overridden occurrences use `DateBasis.CORRECTED`.

## Runtime loading and precedence

`RecurringEvents` parses the catalog lazily on first use. For each requested year, `EventRepository.buildYear` layers:

1. Recorded `dates` entries falling in the year (`RECORDED`).
2. Rule events within `fromYear`..`throughYear`, evaluated by the engine with any year override passed as an `EventDateOverride` (`CALCULATED`, or `CORRECTED` when the override supplies the dates).
3. The year's official holiday calendar, which promotes matching calculated occurrences — or adds new events — to `EventKind.HOLIDAY` with `DateBasis.OFFICIAL`, merging citations and source references; cancelled entries are skipped.
4. Buddhist holy days, computed day by day from the engine (`KHMER_LUNAR`).

Every supported year 1800–2200 is built this way on demand and cached in memory per year. Outside 2020–2027 no event is marked as an official holiday; a calculated festival date alone never establishes government leave.

Normal builds package the committed catalog; there is no on-device database, precaching job or network request for built-in events.

## Provenance and limits

The catalog's event definitions were compiled by reviewing the publicly rendered Khmer Lunar Calendar website, captured on 10 September 2026 across 372 months and 11,323 consecutive dates (3,246 event occurrences). The capture is credited in Settings → Calendar sources & licenses; the raw capture artifacts are not versioned. The review established what the website displayed, not independent historical validation: ceremony occurrence, cancellations and future holiday decisions require year-specific government records, which is exactly what the official holiday calendars and overrides carry, with their limits stated above.

Chinese festival dates are recorded lists, not calculated rules; they exist only for their captured years. Anniversaries and other counts inherit the catalog's `anniversaryBase` values and are only as accurate as the reviewed definitions.

## Maintenance

Edit `khmer-calendar-data.json` directly, keeping event IDs stable and recording sources for any new or changed dates:

- Adding an official holiday year: append a `holidayCalendars` entry with its subdecree source, then extend `EventRepositoryTest`'s expectations.
- Correcting a calculated date: add an `overrides` entry with `sourceId` and `reason`; tests will then expect the corrected dates.
- Changing a rule: update the event's `rule`, then re-run the tests below.

`RecurringEventsTest` compares every rule against the committed captured-occurrence fixture [`recurrence-reference.tsv`](../app/src/test/resources/recurrence-reference.tsv) for 2000–2030, allowing exactly the documented King Sihamoni birthday differences (which the repository layer resolves through overrides). `EventRepositoryTest` re-checks catalog coverage, engine parity for calculated events and holy days across sampled years, the recorded Chinese festival and milestone dates, and every official calendar's day counts, URLs and citations. Run `.\gradlew.bat testDebugUnitTest` after any catalog change.

The capture-assist tools (`tools/reference-event-import.cjs`, `tools/audit-supplied-events.py`, `tools/generate-calendar-reference.cjs`) still support reviewing new website captures. The scripts of the retired TSV pipeline were removed with the resources they generated; git history preserves them.
