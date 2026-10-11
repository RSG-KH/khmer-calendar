# Android documentation

These guides describe the Android app and its integration with the dedicated [Khmer Calendar Engine](https://github.com/RSG-KH/khmer-calendar-engine).

| Guide | Scope |
| --- | --- |
| [Shared engine integration](shared-engine.md) | Pinned release, Android adapters, checksum verification and upgrades |
| [System architecture](architecture.md) | Event repositories, local storage, UI and reminders |
| [UI and responsive design](ui-and-responsive-design.md) | Phone and tablet layouts, themes, accessibility and system insets |
| [Development and testing](development-and-testing.md) | Build setup, app tests and resource generation |
| [Stability and performance](stability-and-performance.md) | Runtime safeguards, verification coverage and measurement limits |
| [Recurring event rules](recurring-event-rules.md) | App-owned definitions, engine mapping and reviewed overrides |
| [Bundled event data](reference-event-database.md) | Event catalog schema, official holiday calendars, provenance and maintenance |
| [Translation editor](../tools/translation/README.md) | Bilingual catalog and generated resources |

Calculation contracts and supporting research live with the engine: see the [v0.6.0 API](https://github.com/RSG-KH/khmer-calendar-engine/blob/v0.6.0/docs/api.md) and [reference evidence](https://github.com/RSG-KH/khmer-calendar-engine/blob/v0.6.0/docs/references.md). Android retains consumer regression tests and bundled attribution; it does not maintain a second calculation implementation or algorithm guide.

These guides describe the current app. Superseded PWA sync notes and one-time
verification reports remain available in Git history. Personal-event repeat
behavior is maintained in [architecture](architecture.md#events-and-storage),
with regression coverage in [development and testing](development-and-testing.md#testing).

[Project README](../README.md)
