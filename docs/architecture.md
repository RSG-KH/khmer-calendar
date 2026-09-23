# System architecture

Khmer Calendar is an offline Kotlin/Jetpack Compose Android app. It uses a released calculation library and bundles its event data. The installed app has no Internet permission, telemetry or advertising SDKs.

```mermaid
flowchart TD
    Engine[Shared Khmer Calendar Engine] --> Adapters[Android date adapters]
    Catalog[Bundled event catalog JSON] --> Recurring[RecurringEvents]
    Engine --> Recurring
    Adapters --> Events[EventRepository]
    Recurring --> Events
    Events --> UI[Compose UI]
    Adapters --> UI
    Custom[CustomEventRepository: SQLite] --> UI
    Events --> Widgets[Home screen widgets]
    Custom --> Widgets
    Events --> Planner[ReminderPlanner]
    Custom --> Planner
    Preferences[AppPreferences] --> UI
    Preferences --> Planner
    Preferences --> Widgets
    Planner --> Alarms[AlarmManager and notification receivers]
    Widgets --> WidgetRefresh[WorkManager and midnight alarm]
```

## Calendar integration

The [shared engine integration guide](shared-engine.md) describes the pinned dependency, API boundary, Android adapters and upgrade checks. The engine owns lunar conversion, traditional year transitions, holy days, New Year dates, Ganzhi (sexagenary) year/month/day/hour pillars and recurrence evaluation for **1800–2200**. Android owns `LocalDate` conversion, localized labels and Western zodiac presentation.

Calculation algorithms and supporting evidence are maintained in the engine project. Android tests verify that the app continues to consume its results correctly when the dependency changes.

## Events and storage

`EventRepository` builds every supported year (1800–2200) on demand and caches each requested year's localized event list in memory. Each year is layered from the bundled event catalog (`khmer-calendar-data.json`) and the shared engine: recorded date lists (fixed heritage milestones), engine-evaluated recurrence rules (including dynamic Chinese festivals for 1900–2100) with reviewed per-year date overrides, official government holiday calendars for 2016–2027 that promote matching occurrences to cited public holidays, and Buddhist holy days computed day by day from the engine. The calendar grid and date details use the engine directly. User-created events come from a separate repository and are combined with built-in events by the UI and reminder planner.

| Event kind | Source |
| --- | --- |
| `HOLIDAY` | Date from an official government holiday calendar (2016–2027), carrying the citing subdecree or ministry source |
| `OBSERVANCE` | Recorded date lists, calculated recurrence results, or recurrence dates corrected by a reviewed override |
| `HOLY_DAY` | Shared engine holy-day result, computed per day for the requested year |
| `CUSTOM` | User-created event stored locally |

The [bundled data guide](reference-event-database.md) describes the event catalog: recurrence rules, dated records, official holiday calendars, date overrides and their sources. The [recurrence guide](recurring-event-rules.md) describes how catalog rules map to engine inputs. Engine upgrades do not replace these definitions or records.

`CustomEventRepository.kt` stores events in `custom-events.db`:

| Column | Value |
| --- | --- |
| `id` | UUID primary key |
| `title` | Event name, up to 120 characters |
| `date` | Gregorian date, `YYYY-MM-DD` |
| `time` | Scheduled time, `HH:mm` |
| `notes` | Optional description, up to 2,000 characters |
| `remind` | Reminder eligibility |
| `zone_id` | Saved IANA time zone |
| `offset_seconds` | Saved UTC offset, when available |
| `repeat_frequency` / `repeat_until` | Optional custom series rule and inclusive end date |
| `repeat_interval` | Number of days for a Days series; defaults to 3 |
| `include_thirty` / `include_february` | Independent fallback choices for missing month-end dates |
| `remind_after` | Save instant; earlier occurrences cannot send retrospective reminders |

Database version 3 adds nullable recurrence metadata to existing rows, preserving
their dates, saved offsets, notes and reminder eligibility. One row represents a
whole series. `EventRepeat` generates civil dates lazily from the original anchor;
February and 30-day fallbacks never shift later months. All repeat modes require
an end date within 1800–2200. Yearly February 29 series can skip non-leap years or
include February 28.

Calendar and Events expand only the years being viewed, projecting each occurrence
from its saved zone into the selected display zone. Occurrence IDs include the
source date, with a separate series ID for editing and deletion. Both operations
apply to the entire series. Editing a series keeps its original date, wall time and
zone; switching a single event to a series anchors it in the editor's displayed zone.

## Reminders and notifications

The application provides local, reliable event notifications without relying on Google Play Services, Firebase Cloud Messaging (FCM), or external background workers.

### Architecture
- **`EventReminderReceiver`** (in `notifications/EventNotifications.kt`): Broadcast receiver triggered by Android's `AlarmManager.setExactAndAllowWhileIdle` for precision alarms.
- **`ReminderRestoreReceiver`** (in `notifications/EventNotifications.kt`): Re-calculates and re-registers the next upcoming alarm upon device restart (`ACTION_BOOT_COMPLETED`), package replacement, clock adjustment (`ACTION_TIME_SET`, `ACTION_TIMEZONE_CHANGED`), or exact-alarm permission changes.
- **Single Next-Alarm Queue**:
  - Rather than filling Android's alarm table with hundreds of future alarms, the app calculates the immediate next event moment and schedules a single alarm.
  - When that alarm fires, notifications are posted, and the queue automatically schedules the subsequent alarm.

### Event-type controls

Notification settings provide separate **Push custom**, **Push holidays**, **Push observances**, and **Push Buddhist holy days** switches before **Push time**. Custom, holiday and observance choices default to on; the master notification switch and holy-day reminders default to off. Turning off **Buddhist holy days in events** hides **Push Buddhist holy days** and suppresses its reminders while preserving the saved on/off choice. Showing holy days again restores that choice. Holy-day reminders require both switches to be on. The planner applies these choices to initial reminders and repeats, and alarm delivery rechecks current settings before posting. Disabling every eligible category leaves no alarm scheduled.

Settings changes reschedule alarms only when reminder enablement, event categories, holy-day event visibility, push time, repeat interval or **Today follows** changes. Appearance and calendar display settings leave the existing alarm in place. Language changes update the channel labels; delivery reads the current language. Custom-event edits, permission changes, app resume and system restore broadcasts still refresh the next alarm.

### Time-Zone Intelligence

- **"Today Follows" Setting**:
  - `Local Time`: Follows the device's current time zone and adjusts automatically during travel or daylight saving time.
  - `Cambodia (UTC+7)`: Fixed to Phnom Penh time regardless of device location.
- **Built-in Events**: Reminders trigger at the configured daily push time in the zone selected by **Today follows**: the device's local zone or Cambodia (UTC+7). Changing this setting or the device time zone reschedules the next alarm.
- **Custom Events**: Reminders trigger at the exact instant intended in the time zone where the event was created, converting cleanly if the user switches display time zones.
- **Custom series**: Each date resolves in the saved zone, keeping its wall time across DST. Future gaps move forward by the gap; future folds choose the first offset. The original occurrence retains its saved offset. The planner lazily searches from the current day for the next eligible occurrence, including distant leap years, while retaining the single-alarm queue. Past anchors can have future reminders, but occurrences at or before the save instant are suppressed.
- **Repeats**: Configurable periodic repeats (**Off**, 2, 4, 6, 8, or 12 hours) use elapsed hours and stop at midnight in the selected zone for built-in events, or the saved event zone for custom events. Daylight-saving gaps move a nonexistent push time forward by the gap; repeated clock times use the first occurrence for the initial reminder.

The UI reads today's date immediately when the activity becomes visible and every 30 seconds while its lifecycle is at least `STARTED`, including visible multi-window use. Polling stops when the activity is hidden and restarts with an immediate read when it returns. This keeps the Today marker current across midnight or device clock changes without hidden UI polling. Reminder delivery is independent: `AlarmManager` invokes its receiver, and the manifest-registered restore receiver recalculates the next alarm after system date/time or time-zone changes, reboot, app replacement and exact-alarm access changes.

---

## Home Screen Widgets

Khmer Calendar provides five home screen app widgets. Month, Productivity, Focus, and Glance use Jetpack Glance (`1.2.0`); Planner uses Android RemoteViews for a list that opens near today:

- **Month Widget (`MonthWidget`)**: 4×3 full-month calendar grid with Gregorian and Khmer lunar dates, Buddhist holy days, traditional weekday colors, event markers, footnote legend, and interactive date-cell tapping. Features height-adaptive compact padding, a $1.25\times$ max width-to-height aspect ratio constraint, and dynamic zoom gap scaling.
- **Productivity Widget (`ProductivityWidget`)**: 4×2 date details card showing the big day number, short weekday, short month, Khmer lunar date, Buddhist Era year, optional Western zodiac sign, and today's/tomorrow's event lists. The sign follows its app setting and retains its English name in Khmer mode. Detail rows hide on narrow widths (< 330dp).
- **Focus Widget (`FocusWidget`)**: 4×2 daily events overview with full lunar date and BE year badges, scrollable today's events list, and preview sub-cards for yesterday and tomorrow.
- **Planner Widget (`PlannerWidgetReceiver`)**: 4×2, resizable in both directions, with 29 dates centered on today. Its native list scrolls through past and future dates, follows the first-day-of-week setting for stronger week dividers, displays timed events before untimed events, and opens either an event or date details based on the tapped area.
- **Glance Widget (`GlanceWidget`)**: 4×1, horizontally resizable to 2×1. A date tile and three detail rows show today's Gregorian, lunar, traditional year and timezone information. At 2×1 the third row shows traditional animal year and Sak; at wider sizes it shows holy-day and optional zodiac/Ganzhi badges. The app name and version fill that row when it is otherwise empty. Its refresh button uses the shared pressed state.

All widgets follow the dedicated **Font size** setting in the Widgets section of app settings (`widgetFontScale`, 80%–200%), decoupled from the in-app font size. At large zoom levels the adaptive layouts drop optional detail rows and collapse non-essential text to safeguard against home screen clipping.

`WidgetDataSource` builds the full-month lunar grid only for the Month widget; other widgets load the dates they need.

### Lifecycle & Background Refresh
- **`WidgetUpdater`**: Manages WorkManager (`WidgetRefreshWorker`) periodic hourly updates, immediate background updates, and `AlarmManager`'s inexact midnight triggers (`RTC_WAKEUP`). Every refresh path re-reads the **Enable widgets** setting and skips work while it is off; turning it off cancels the queued, periodic and midnight refreshes. A refresh failure on one widget does not stop the remaining widgets.
- **`WidgetReceivers`**: Manifest-registered broadcast receivers (`FocusWidgetReceiver`, `ProductivityWidgetReceiver`, `MonthWidgetReceiver`, `PlannerWidgetReceiver`, `GlanceWidgetReceiver`, `WidgetRefreshReceiver`) react to system triggers (`BOOT_COMPLETED`, `TIME_CHANGED`, `TIMEZONE_CHANGED`, `DATE_CHANGED`, `LOCALE_CHANGED`, `MY_PACKAGE_REPLACED`). The midnight receiver refreshes widgets inline within its broadcast window — with the queued WorkManager job kept as a fallback for OEMs that interrupt background receivers — and reschedules the next selected-zone midnight.
- **In-app refresh**: Changing any setting while the app is open refreshes installed widgets immediately, so language or appearance changes are visible as soon as the user returns home; the queued worker remains as a fallback.

### Master Enable/Disable Control
- **`PackageManager` State Management**: When **Enable widgets** is toggled off in App Settings, `WidgetUpdater.setWidgetsEnabled(context, false)` sets `COMPONENT_ENABLED_STATE_DISABLED` on all five widget receivers and cancels their queued, periodic and midnight refreshes.
- **Global Event Filtering**: Expandable switches in App Settings control Personal events, Public holidays, Observances, and Hide personal event details across all widgets.

---

## UI layer

The in-app UI is built using Jetpack Compose with Material 3 design tokens:

- **State Hoisting**: Screens and components are stateless composables driven by immutable state models (`AppSettings`, `KhmerDateDetails`, `CalendarEvent`).
- **Adaptive Scaffolding**: Detects screen dimensions, smallest width (`sw600dp`), and orientation (`ORIENTATION_LANDSCAPE`) to dynamically switch between compact phone layouts, phone landscape rails, and tablet 2-column widescreen experiences.
- **Dynamic Text Scaling**: Custom `readableSp` extension scales typography dynamically based on user-selected font scaling preferences without disrupting fixed grid geometry.
