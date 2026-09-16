# Recurring event rules

Android owns 100 recurrence definitions in [`tools/recurring-event-rules.json`](../tools/recurring-event-rules.json). The [shared engine](shared-engine.md) evaluates their dates; Android supplies effective years, localized titles and event classification.

## Coverage and precedence

| Years | Event source |
| --- | --- |
| 2000–2030 | The 3,246 [captured dated records](reference-event-database.md); recurrences do not supplement or replace them |
| 1980–1999 and 2031–2050 | Applicable recurrence dates precomputed with the engine and bundled in `engine-event-dates.tsv` |
| 1800–1979 and 2051–2200 | Applicable recurrence rules evaluated by the engine on demand |

Engine-derived Buddhist holy days are available throughout 1800–2200, with precomputed dates bundled for 1980–2050. User-created events are stored separately and combined with built-in events by the UI and reminder planner.

Calculated occurrences use `DateBasis.CALCULATED`, `EventKind.OBSERVANCE` and an ID of `calculated:<rule-id>`. The event key also includes the date. They have no official-source URL or arrival time. A recurrence describes a calendar pattern; it does not establish government leave for a year.

## Definition mapping

`tools/build-recurring-events.py` compiles the JSON manifest to `app/src/main/resources/recurrence-rules.tsv`. `RecurringEvents.Rule` translates the TSV fields to the engine's validated `RecurrenceRule` contract:

| Definition | Android mapping |
| --- | --- |
| `solar` | Fixed Gregorian month/day with the supplied offset and duration |
| `khmer_lunar` | Lunar month, day and phase; `ordinary_or_second_asadh` selects the applicable Asadh month for Lent-related rules |
| `solar_nth_weekday` | TSV `day` becomes the weekday; TSV `offset` becomes `occurrence`, with engine offset set to zero |
| `new_year_first`, `new_year_middle`, `new_year_last` | Engine festival stages; unused TSV month/day values are replaced with valid defaults |
| `fromYear`, `throughYear` | Inclusive years in which the rule applies |
| `anniversaryBase` | Android inserts `year - anniversaryBase` into the localized `{anniversary}` title |

The engine [API documentation](https://github.com/RSG-KH/khmer-calendar-engine/blob/v0.1.0/docs/api.md) defines month numbering, weekday numbering, offsets and festival behavior. Keep those calculation rules in the engine. Android currently requires every applicable rule to return at least one date within its requested year; review this constraint before adding rules that cross a year boundary.

The manifest covers Khmer festivals, royal and national commemorations, heritage anniversaries, international observances and floating weekday events. Chinese festivals remain available only where captured in the dated snapshot; the current fallback does not calculate them.

## Regression checks

`RecurringEventsTest.kt` compares rule results against captured occurrences for 2000–2030, even though runtime recurrence fallback is disabled in those years. This checks integration and compatibility with the snapshot, not independent historical accuracy.

The expected differences are the extra May 13 and 15 dates in the captured 2005–2019 King Sihamoni birthday holiday blocks. The recurrence represents the May 14 birthday. The 2012 New Year correction is covered by the [engine migration checks](shared-engine.md#migration-behavior) and is no longer an allowed recurrence mismatch.

## Updating definitions

1. Edit `tools/recurring-event-rules.json`, keeping IDs stable and reviewing effective years and source evidence.
2. Update titles in `translations/catalog.json` when necessary and export translations as described in the [translation guide](../tools/translation/README.md).
3. Run `python tools/build-recurring-events.py`. It generates the runtime TSV and `app/src/test/resources/recurrence-reference.tsv` from existing captured occurrences; it does not calculate replacement reference dates.
4. Regenerate the [bundled engine dates](reference-event-database.md#precomputed-engine-dates) with the updated app classes.
5. Run `.\gradlew.bat testDebugUnitTest` and inspect the generated diffs before committing. Tests compare every bundled engine date with the current engine and rules.
