# Shared calendar engine

Android uses the JVM package from [Khmer Calendar Engine v0.1.0](https://github.com/RSG-KH/khmer-calendar-engine/releases/tag/v0.1.0). It supplies lunar conversion, Buddhist Era, animal year, Sak, holy days, New Year dates and recurrence evaluation for 1800–2200. Android keeps date conversion to `java.time.LocalDate`, labels, event records, UI and reminders.

## Ownership and API

The engine's [API contract](https://github.com/RSG-KH/khmer-calendar-engine/blob/v0.1.0/docs/api.md) defines supported inputs and calculation behavior. Its [reference evidence](https://github.com/RSG-KH/khmer-calendar-engine/blob/v0.1.0/docs/references.md) records algorithm provenance, reviewed corrections and validation limits. Calculation fixes belong in that project, followed by an Android dependency update.

| Android component | Responsibility |
| --- | --- |
| `domain/KhmerCalendar.kt` | Reuses one engine instance, converts civil dates and supplies localized lunar labels |
| `domain/KhmerNewYear.kt` | Converts the engine's New Year dates to `LocalDate` |
| `domain/KhmerDateDetails.kt` | Formats lunar and traditional year results for the UI |
| `data/RecurringEvents.kt` | Translates app recurrence definitions into engine rules and attaches event titles |
| `data/EventRepository.kt` | Selects dated records or calculated recurrences and adds engine-derived holy days |

The engine operates on civil dates without a time zone. Android chooses the date for **Today follows** and resolves reminder instants. Event definitions, effective years, translations and official holiday records are app data; the engine release does not supply or update them. The Android app does not yet consume manager exports.

## Build dependency

Gradle downloads `khmer-calendar-engine-jvm-0.1.0.jar` directly from the release. The version is pinned in `gradle/libs.versions.toml`; `settings.gradle.kts` limits the release repository to that module. No engine checkout or manual download is needed.

Before building, `:app:verifyCalendarEngine` checks the JAR against the release's SHA-256:

```text
4072d280bd65a1433bb02aa7ff8a54050fb9d251f899113966b8d3aa7c73819a
```

The first build needs network access. Gradle caches the dependency for later builds, including `--offline` builds once all dependencies are cached. The installed app remains offline and has no Internet permission.

The release is resolved as an artifact without Maven metadata. Version 0.1.0's only runtime dependency is Kotlin stdlib, which the app already supplies through AGP's built-in Kotlin support. Both the app and engine produce Java 11 bytecode; the Android build uses JDK 25 and Kotlin 2.4.20.

## Updating the engine

1. Review the new release and its dependency metadata. Add any new runtime dependencies explicitly, or change the repository integration to consume its Maven metadata.
2. Update `calendarEngine` in `gradle/libs.versions.toml` and the expected SHA-256 in `app/build.gradle.kts`, using the release's `SHA256SUMS`.
3. Refresh the engine license and notices in `app/src/main/assets` from the released JAR's `META-INF/LICENSE` and `META-INF/NOTICE`.
4. Run the [app checks](development-and-testing.md#testing) and debug/release builds. Review changed date results against the engine's cited evidence before updating fixtures.

New engine releases do not silently change an Android build or an installed app.

## Migration behavior

Version 0.1.0 corrects the former embedded calculation's 2012 New Year dates to **13–15 April**, with the animal-year change on April 13 and Sak change on April 15. Supporting publications are recorded in the engine's reference evidence above. The original MomentKH fixture is retained so the rejected April 14 start remains visible in the Android regression test.

The 3,246 captured event records for 2000–2030 still take precedence over calculated recurrences. They will need a separate migration to reviewed manager exports; replacing the calculation dependency does not verify or correct their titles, including recorded arrival times. The engine currently returns New Year dates and duration, not an arrival instant.

Supported date coverage and passing regression tests are not a claim of independent historical validation for every date. Official public holidays require year-specific government records; a calculated festival date alone does not establish a day off.

## Attribution

The app bundles the released engine's Apache 2.0 [license](../app/src/main/assets/engine-LICENSE.txt) and upstream MIT [notices](../app/src/main/assets/NOTICE.txt). Settings → Calendar sources & licenses links to the engine and displays these texts offline. Upstream author and library references remain in those notices and in test fixtures where they identify the comparison source.
