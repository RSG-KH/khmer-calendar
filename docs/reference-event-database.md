# Bundled event data

The Android app packages its event catalog in [`khmer-calendar-data.json`](../app/src/main/resources/khmer-calendar-data.json) — one versioned JSON resource (`schemaVersion: 3`) holding recurrence rules, recorded date lists, official holiday calendars, reviewed date overrides, verified New Year arrival records, and their sources. It is app data, separate from the [shared calculation engine](shared-engine.md). Version 0.4.0 replaced the former three-resource pipeline (the 3,246-row captured snapshot, the precomputed engine date cache and the runtime rules TSV) with this single catalog.

`dataVersion` inside the file tracks catalog data revisions independently of the app version. Titles for catalog events are stored in the catalog itself, including `{anniversary}` placeholders resolved per year; only the holy-day label comes from the shared app translations.

## Catalog structure

| Top-level field | Contents |
| --- | --- |
| `schemaVersion` | Catalog schema major version; the current bundle is `3` |
| `dataVersion` | Data revision of this bundle (`0.4.4`) |
| `sources` | Provenance records referenced by `sourceIds` elsewhere |
| `events` | 139 event definitions: rules and recorded dates |
| `holidayCalendars` | Official public-holiday calendars, one per year (2016–2027) |
| `overrides` | Reviewed per-year date replacements for specific events |
| `newYearArrivals` | 19 verified traditional Moha Sangkran arrival records (1997, 2009, 2010–2026 unbroken) |
| `eventCalendars` | Reserved for future per-year calendar records; currently empty |

### Sources

Each source records `id`, `kind` (`government`, `calendar`, `other` or `historical`), `title`, `publisher` and optional `url`, `reference`, `publishedOn` and `notes`. The bundle carries 46 sources: 25 `government` — the annual holiday subdecrees for 2016–2027, the [2025 Ministry of Economy and Finance calendar](https://mef.gov.kh/calendar-holiday-2025/) and the [2026 Legal Reform Committee calendar](https://lrc.gov.kh/en/annual-holiday-calendar-2026/), plus TVK Moha Sangkran broadcasts and AKP reports for the New Year arrival records — 10 `calendar` sources (the [Khmer Lunar Calendar website](https://khmer-lunar-calendar.com/) capture of 10 September 2026 that reviewed the event definitions, and pagoda calendars from Wat Ratanarangsey and Wat Kiryvongsa Bopharam), 10 `other` news and travel articles backing arrival records, and one `historical` research dossier set establishing event origin years. Event details cite the first government source carrying a reference or URL.

### Events

Each event has `id`, `kind` (`observance`, `traditional` or `historical`), bilingual `names` (plus optional `description`), `sourceIds`, and one of two date carriers:

- **`rule`** (113 events): engine `RecurrenceRule` fields — 73 `solar`, 25 `khmer_lunar`, 3 `solar_nth_weekday`, the 3 Khmer New Year stages, and 9 traditional Chinese festivals (`chinese_festival` with `monthPolicy: "cn-reference-utc8"` across 1900–2100). Optional `anniversaryBase` inserts `year − anniversaryBase` into the `{anniversary}` title placeholder (English renders an ordinal suffix, e.g. `· 47th`; Khmer renders Khmer numerals, e.g. `ខួបលើកទី៤៧`). `historical` events may set `originalDate`, before which occurrences are suppressed.
- **`dates`** (26 events): explicit ISO date lists for fixed heritage milestones such as the UNESCO inscription anniversaries. These are emitted as `DateBasis.RECORDED` without calculation.

### New Year arrival records

`newYearArrivals` carries 19 verified Moha Sangkran arrival times (1997, 2009, and 2010–2026 unbroken) from TVK national broadcasts, pagoda calendar proclamations and contemporaneous press coverage. Each record pins `year`, `localDate`, `localTime` (with optional `second`), `minuteOfDay`, an evidence `status`/`grade` (all bundled records are `evidenced`, grade `A`), `sourceIds`, and zone attribution fields (`precision`, `zoneStated`, `interpretedZone`, `interpretedOffset`, `zoneBasis`) recording how the printed time was interpreted.

At runtime the repository prefers a bundled record — tagged official, `ម៉ោងផ្លូវការ` — and otherwise falls back to the engine's traditional `arrivalEstimate`, tagged estimated, `ម៉ោងប៉ាន់ស្មាន`. The resolved phrase uses Khmer 12-hour period descriptors (`ព្រឹក`, `រសៀល`, `ល្ងាច`, `យប់`, `រំលងអធ្រាត្រ`) and is appended to the unified Moha Sangkran title; the record's sources are merged into the event.

### Holiday calendars and overrides

Each `holidayCalendars` year carries `coverage` (`complete` for all bundled years) and `holidays` with `id`, bilingual `names`, explicit `dates`, `status` (`cancelled` entries are skipped), `sourceIds` and an optional `eventId` linking a catalog event — used to resolve `{anniversary}` counts. Years 2016–2027 are bundled; 283 official days in total (110 days for 2016–2019 and 173 days for 2020–2027), each carrying a subdecree or ministry citation.

`overrides` pin a specific `eventId`/`year` to explicit `dates`, with a mandatory `sourceId` and `reason`. They preserve reviewed differences between captured records and the calculation — the 2005–2019 three-day King Sihamoni birthday holiday blocks (where the rule yields only May 14) and 3 Chinese festival parity overrides (Qingming 2009 & 2029, Zongzi 2013). Overridden occurrences use `DateBasis.CORRECTED`.

### Event knowledge companion

The companion `event-knowledge.json` resource bundles the manager's curated knowledge dataset — one bilingual name and summary per catalog event, keyed by event id, with a `provenance` block crediting its research models. `RecurringEvents.knowledgeById` loads it lazily, the Learn more popup renders it, and a unit test enforces one-to-one coverage with the catalog.

## Runtime loading and precedence

`RecurringEvents` parses the catalog lazily on first use. For each requested year, `EventRepository.buildYear` layers:

1. Recorded `dates` entries falling in the year (`RECORDED`).
2. Rule events within `fromYear`..`throughYear`, evaluated by the engine with any year override passed as an `EventDateOverride` (`CALCULATED`, or `CORRECTED` when the override supplies the dates).
3. The year's official holiday calendar, which promotes matching calculated occurrences — or adds new events — to `EventKind.HOLIDAY` with `DateBasis.OFFICIAL`, merging citations and source references; cancelled entries are skipped.
4. Buddhist holy days, computed day by day from the engine (`KHMER_LUNAR`).

For the first New Year stage (`khmer_new_year_1`), both the calculated occurrence and any official holiday promotion append the resolved Moha Sangkran arrival time to the title and merge the arrival record's sources, as described above.

Every supported year 1800–2200 is built this way on demand and cached in memory per year. Outside 2016–2027 no event is marked as an official holiday; a calculated festival date alone never establishes government leave.

Normal builds package the committed catalog; there is no on-device database, precaching job or network request for built-in events.

## Provenance and limits

The catalog's event definitions were compiled by reviewing the publicly rendered Khmer Lunar Calendar website, captured on 10 September 2026 across 372 months and 11,323 consecutive dates (3,246 event occurrences). Full provenance, validation datasets and curation tools are maintained in the [Khmer Calendar Manager](https://github.com/RSG-KH/khmer-calendar-manager) repository. The review established what the website displayed, not independent historical validation: ceremony occurrence, cancellations and future holiday decisions require year-specific government records, which is exactly what the official holiday calendars and overrides carry, with their limits stated above.

Chinese festivals are evaluated dynamically via `ChineseLunisolarEngine` for years 1900–2100 (`DateBasis.CALCULATED`), with 3 explicit overrides for historical parity. Anniversaries and other counts inherit the catalog's `anniversaryBase` values and are only as accurate as the reviewed definitions.

## Maintenance

Edit `khmer-calendar-data.json` directly, keeping event IDs stable and recording sources for any new or changed dates:

- Adding an official holiday year: append a `holidayCalendars` entry with its subdecree source, then extend `EventRepositoryTest`'s expectations.
- Correcting a calculated date: add an `overrides` entry with `sourceId` and `reason`; tests will then expect the corrected dates.
- Changing a rule: update the event's `rule`, then re-run the tests below.

`RecurringEventsTest` compares every rule against the committed captured-occurrence fixture [`recurrence-reference.tsv`](../app/src/test/resources/recurrence-reference.tsv) for 2000–2030, allowing exactly the documented King Sihamoni birthday differences (which the repository layer resolves through overrides). `EventRepositoryTest` re-checks catalog coverage, engine parity for calculated events and holy days across sampled years, the recorded Chinese festival and milestone dates, and every official calendar's day counts, URLs and citations. Run `.\gradlew.bat testDebugUnitTest` after any catalog change.

The capture-assist tools (`tools/reference-event-import.cjs`, `tools/generate-calendar-reference.cjs`) still support reviewing new website captures. The scripts of the retired TSV pipeline — including the snapshot audit and the one-time catalog bootstrap — were removed with the resources they consumed; git history preserves them.
