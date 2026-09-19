# Recurring event rules

Android owns 111 recurrence definitions, stored on each event in the bundled [event catalog](reference-event-database.md) (`khmer-calendar-data.json`). The [shared engine](shared-engine.md) evaluates their dates for every supported year; Android supplies effective years, localized titles, event classification and reviewed date overrides.

## Coverage and precedence

| Years | Event source |
| --- | --- |
| 1800–2200 | Applicable rules evaluated by the engine on demand; reviewed [overrides](reference-event-database.md#holiday-calendars-and-overrides) replace calculated dates for pinned years |
| 2016–2027 | Official government holiday calendars overlay matching occurrences as cited public holidays |

Engine-derived Buddhist holy days are computed on demand for every supported year. User-created events are stored separately and combined with built-in events by the UI and reminder planner.

Calculated occurrences use `DateBasis.CALCULATED`, `EventKind.OBSERVANCE` and the catalog event's ID; the event key also includes the date. Overridden occurrences use `DateBasis.CORRECTED` and carry the override's source. Neither has an official-source URL or arrival time. A recurrence describes a calendar pattern; it does not establish government leave for a year.

## Definition mapping

Each event's `rule` object maps directly to the engine's validated `RecurrenceRule` contract; `RecurringEvents.parseRule` fills engine-compatible defaults (month/day 1 for the festival stages, waxing, offset 0, duration 1, years 1800–2200, `exact` month policy, occurrence 1):

| Definition | Mapping |
| --- | --- |
| `solar` | Fixed Gregorian month/day with the supplied offset and duration |
| `khmer_lunar` | Lunar month, day and phase; `ordinary_or_second_asadh` selects the applicable Asadh month for Lent-related rules |
| `solar_nth_weekday` | ISO weekday in `day` with `occurrence` selecting the first–fifth match |
| `new_year_first`, `new_year_middle`, `new_year_last` | Engine festival stages |
| `chinese_festival` | Traditional Chinese festival evaluated dynamically by `ChineseLunisolarEngine` (1900–2100) |
| `fromYear`, `throughYear` | Inclusive years in which the rule applies |
| `anniversaryBase` | Android inserts `year - anniversaryBase` into the localized `{anniversary}` title |

The engine [API documentation](https://github.com/RSG-KH/khmer-calendar-engine/blob/v0.3.0/docs/api.md) defines month numbering, weekday numbering, offsets and festival behavior. Keep those calculation rules in the engine. Android currently requires every applicable rule to return at least one date within its requested year; review this constraint before adding rules that cross a year boundary.

The rules cover Khmer festivals, 9 traditional Chinese festivals, royal and national commemorations, heritage anniversaries, international observances and floating weekday events. Historical commemorations set `originalDate`, before which occurrences are suppressed. Fixed milestones (such as UNESCO inscription anniversaries) ship as recorded date lists in the catalog.

## Regression checks

`RecurringEventsTest.kt` compares rule results for 2000–2030 against the captured-occurrence fixture `app/src/test/resources/recurrence-reference.tsv`. The only allowed differences are the 2005–2019 King Sihamoni birthday years, where the source records the full three-day official block while the rule yields May 14; the repository layer reconciles those years through catalog overrides. This checks integration against the reviewed capture, not independent historical accuracy. The 2012 New Year correction is covered by the [engine migration checks](shared-engine.md#migration-behavior).

## Updating definitions

1. Edit the event's `rule` in `khmer-calendar-data.json`, keeping IDs stable and reviewing effective years and source evidence.
2. Update titles in `translations/catalog.json` when necessary and export translations as described in the [translation guide](../tools/translation/README.md).
3. Run `.\gradlew.bat testDebugUnitTest` and inspect failures before committing. Tests compare rule results with the fixture, engine parity, official calendars and repository classification.
