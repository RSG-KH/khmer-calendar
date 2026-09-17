# Android documentation

These guides describe the Android app and its integration with the dedicated [Khmer Calendar Engine](https://github.com/RSG-KH/khmer-calendar-engine).

| Guide | Scope |
| --- | --- |
| [Shared engine integration](shared-engine.md) | Pinned release, Android adapters, checksum verification and upgrades |
| [System architecture](architecture.md) | Event repositories, local storage, UI and reminders |
| [UI and responsive design](ui-and-responsive-design.md) | Phone and tablet layouts, themes, accessibility and system insets |
| [Development and testing](development-and-testing.md) | Build setup, app tests and resource generation |
| [Recurring event rules](recurring-event-rules.md) | App-owned definitions, engine mapping and snapshot precedence |
| [Bundled event data](reference-event-database.md) | Captured records, provenance, holiday classification and maintenance |
| [Custom repeat verification](custom-repeat-verification.md) | Behavior checklist and test record for repeating personal events |
| [Translation editor](../tools/translation/README.md) | Bilingual catalog and generated resources |

Calculation contracts and supporting research live with the engine: see the [v0.1.0 API](https://github.com/RSG-KH/khmer-calendar-engine/blob/v0.1.0/docs/api.md) and [reference evidence](https://github.com/RSG-KH/khmer-calendar-engine/blob/v0.1.0/docs/references.md). Android retains consumer regression tests and bundled attribution; it does not maintain a second calculation implementation or algorithm guide.

[Project README](../README.md)
