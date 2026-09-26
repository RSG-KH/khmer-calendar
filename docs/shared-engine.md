# Shared calendar engine

Android uses the JVM package from [Khmer Calendar Engine v0.6.0](https://github.com/RSG-KH/khmer-calendar-engine/releases/tag/v0.6.0). It supplies lunar conversion, Buddhist Era, animal year, Sak, holy days, New Year dates and traditional arrival estimate, Ganzhi (sexagenary) pillars, Chinese festival recurrence calculation and recurrence evaluation for 1800–2200. Android keeps date conversion to `java.time.LocalDate`, labels, event records, UI and reminders.

## Ownership and API

The engine's [API contract](https://github.com/RSG-KH/khmer-calendar-engine/blob/v0.6.0/docs/api.md) defines supported inputs and calculation behavior. Its [reference evidence](https://github.com/RSG-KH/khmer-calendar-engine/blob/v0.6.0/docs/references.md) records algorithm provenance, reviewed corrections and validation limits. Calculation fixes belong in that project, followed by an Android dependency update.

| Android component | Responsibility |
| --- | --- |
| `domain/KhmerCalendar.kt` | Reuses one engine instance, converts civil dates and supplies localized lunar labels |
| `domain/KhmerNewYear.kt` | Converts the engine's New Year dates to `LocalDate` and exposes traditional arrival estimate |
| `domain/KhmerDateDetails.kt` | Formats lunar and traditional year labels, localizes Ganzhi animal names and calculates the Ganzhi day pillar only when needed |
| `ui/CalendarApp.kt` | Presents the optional Ganzhi and Western Big 3 tables in date details, with a shared time picker and per-setting calculation guards |
| `data/RecurringEvents.kt` | Parses the bundled event catalog, translates rule definitions into engine rules, and formats arrival time |
| `data/EventRepository.kt` | Layers catalog rules, recorded dates, official holiday calendars and date overrides over engine results for each requested year |

The engine's lunar calendar operates on civil dates without a time zone. Android chooses the date for **Today follows**, supplies a selected clock hour to Ganzhi and time and offset inputs to Western astrology, and resolves reminder instants. Event definitions, effective years, translations, official holiday calendars and date overrides are app data in the bundled catalog; the engine release does not supply or update them.

## Build dependency

Gradle downloads `khmer-calendar-engine-jvm-0.6.0.jar` directly from the GitHub release. The version is pinned in `gradle/libs.versions.toml`; `settings.gradle.kts` limits the release repository to that module. No engine checkout or manual download is needed.

Before building, `:app:verifyCalendarEngine` checks the downloaded JAR against the release's SHA-256:

```text
ca791529a2f9d4175c342440ac4b15448c4af19f4a3df3742b6782c71c2baba6
```

The first build needs network access. Gradle caches the dependency for later builds, including `--offline` builds once all dependencies are cached. The installed app remains offline and has no Internet permission.

The release is resolved as an artifact without Maven metadata. Version 0.6.0's only runtime dependency is Kotlin stdlib, which the app already supplies through AGP's built-in Kotlin support. Both the app and engine produce Java 11 bytecode; the Android build uses JDK 25 and Kotlin 2.4.20.

## Updating the engine

1. Review the new release and its dependency metadata. Add any new runtime dependencies explicitly, or change the repository integration to consume its Maven metadata.
2. Update `calendarEngine` in `gradle/libs.versions.toml` and the expected SHA-256 in `app/build.gradle.kts`, using the release's `SHA256SUMS`.
3. Refresh the engine license and notices in `app/src/main/assets` from the released JAR's `META-INF/LICENSE` and `META-INF/NOTICE`.
4. Run the [app checks](development-and-testing.md#testing). `EventRepositoryTest` and `RecurringEventsTest` re-evaluate every catalog rule and holy day against the new engine and fail on changed results; review any diff against the engine's cited evidence. Then re-check the reviewed date overrides in `khmer-calendar-data.json`: they pin specific occurrences to recorded dates regardless of formula changes.
5. Run the debug/release builds. Review changed date results against the engine's cited evidence before updating fixtures.

New engine releases do not silently change an Android build or an installed app.

## Migration behavior

Version 0.1.0 corrects the former embedded calculation's 2012 New Year dates to **13–15 April**, with the animal-year change on April 13 and Sak change on April 15. Supporting publications are recorded in the engine's reference evidence above. The original MomentKH fixture is retained so the rejected April 14 start remains visible in the Android regression test.

The former bundled snapshot of 3,246 captured occurrences (2000–2030) was replaced in version 0.4.0 by the Schema v2 event catalog: recurrence rules, recorded date lists, official holiday calendars for 2016–2027 and reviewed date overrides, each with source records. Captured dates that differed from the calculation — such as the 2005–2019 three-day King Sihamoni birthday blocks — are preserved as overrides with their provenance.

Version 0.2.0 introduces the zero-dependency `ChineseLunisolarEngine` in `commonMain` and integrates dynamic `type: "chinese_festival"` recurrence rules directly into the calculation pipeline (1900–2100). Event catalog v0.3.0 adopts these rules for all 9 traditional Chinese festivals, preserving 100% legacy parity via 3 explicit overrides (Qingming 2009 & 2029, Zongzi 2013).

Version 0.3.0 exposes the traditional Moha Sangkran `arrivalEstimate` on `NewYearCelebration`. Event catalog v0.4.0 (Schema v3) pairs this with 19 verified broadcast and proclamation records in `newYearArrivals` (1997, 2009, 2010–2026 unbroken), displayed via authentic Khmer 12-hour period descriptors and official/estimated tags directly in the unified Moha Sangkran title without separate detail rows.

Version 0.4.0 adds the standalone `ChineseZodiacCalculator` with Ganzhi (干支, sexagenary) day and hour pillars over the proleptic Gregorian range 1..9999. Android first presented the day pillar as a row in date details. The current **Show Chinese Ganzhi (干支)** setting controls the table described below; existing lunar, holy-day and recurrence results are unchanged.

Version 0.5.0 adds Lichun-based year pillars, solar-term month pillars, Four Pillars (BaZi) and clash branches to `ChineseZodiacCalculator`. These solar-calendar calculations cover 1900–2100 under the engine's UTC+8 civil-date convention. Android displays the Year, Month and Day animal and clash branches in date details, plus Hour when a time is selected. Today starts with the current time in the configured **Today follows** zone when its popup opens; other dates start without a time. For dates outside 1900–2100, Year and Month are marked unavailable; Day remains available.

Version 0.5.1 synchronizes Four Pillars (BaZi) day and hour rollover at 23:00 (late Rat hour), isolates the Chinese festival registry, and hardens runtime validation.

Version 0.6.0 introduces the standalone `WesternZodiacCalculator` calculating Western astrology ("Big 3" + Angles: Sun, Moon, Ascendant, Midheaven) coordinates over 1800–2200 using Jean Meeus algorithms and Espenak & Meeus piecewise Delta-T with singularity detection. Android displays the Big 3 (Sun, Moon, Rising sign) table in date details. Today uses the time captured when its popup opens; other dates use noon for Sun and Moon until the user picks a time, at which point Rising also becomes available. The popup does not update its time while open; the user can change or clear it. Each table's calculator runs only while its setting is enabled. If both tables are off, the popup neither captures time nor shows a time picker.

Supported date coverage and passing regression tests are not a claim of independent historical validation for every date. Official public holidays require year-specific government records; a calculated festival date alone does not establish a day off.

## Attribution

The app bundles the released engine's Apache 2.0 [license](../app/src/main/assets/engine-LICENSE.txt) and upstream MIT [notices](../app/src/main/assets/engine-NOTICE.txt), alongside app and catalog attribution in [NOTICE.txt](../app/src/main/assets/NOTICE.txt). Settings → Calendar sources & licenses links to the engine and displays these partitioned texts offline. Upstream author and library references remain in those notices and in test fixtures where they identify the comparison source.
