# System Architecture

Khmer Calendar is built entirely using native Kotlin and modern Android Jetpack libraries with Jetpack Compose for declarative UI rendering. The application is strictly offline: it contains no webviews, no background telemetry services, no network permissions, and zero third-party tracking or advertising SDKs.

```
┌─────────────────────────────────────────────────────────────┐
│                    Jetpack Compose UI                       │
│  CalendarApp · MonthPicker · CustomEventEditor · Popups     │
└───────────────┬─────────────────────────────┬───────────────┘
                │                             │
┌───────────────▼───────────────┐ ┌───────────▼───────────────┐
│     Khmer Calendar Engine     │ │      Event Repository     │
│ KhmerCalendar · KhmerNewYear  │ │ Bundled TSV · Recurrences │
│       KhmerDateDetails        │ │  Custom SQLite Database   │
└───────────────┬───────────────┘ └───────────┬───────────────┘
                │                             │
┌───────────────▼─────────────────────────────▼───────────────┐
│              Alarms & Notification Subsystem                │
│       AlarmManager · AlarmReceiver · BootReceiver           │
└─────────────────────────────────────────────────────────────┘
```

---

## 1. Khmer Calendar Arithmetic Engine

The calendar engine accurately computes Khmer lunar and Buddhist Era information for any Gregorian date between **January 1, 1800 and December 31, 2200** (146,462 consecutive calendar days).

### Core Components
- **`KhmerCalendarEngine`**: Interface defining month calculation, day mapping, and lunar properties. Allows alternative engines to be tested or plugged in.
- **`KhmerCalendar.kt`**: Main arithmetic implementation adapted from [MetheaX/khmer-chhankitek-calendar](https://github.com/MetheaX/khmer-chhankitek-calendar) and aligned with [MomentKH](https://github.com/ThyrithSor/momentkh).
- **`KhmerNewYear.kt`**: Computes astronomical New Year transitions (Moha Songkran, Lerng Sak, and animal year progression).
- **`KhmerDateDetails.kt`**: Aggregates date attributes into a high-level representation (Khmer day/month names, waxing/waning moon count, Buddhist year, Sak, Animal Year, and Western Zodiac sign).

### Lunar Arithmetic Principles
1. **Pre-computed Month Indexing & Binary Search**:
   - Rather than calculating every day incrementally from an epoch, month boundaries and leap properties are indexed once during initialization.
   - Any Gregorian date is resolved in $O(\log N)$ time using binary search against month start epochs.
2. **Leap Months (`Adhikamasa`)**:
   - In lunar leap years, an extra month (Second Asadha, or *Chhantrea Adhikamasa*) is inserted.
   - The engine validates consecutive leap-month intervals to accurately mirror Cambodian civil almanacs.
3. **Leap Days (`Chhantrea Adhikavara`)**:
   - In great leap years (*Adhikavara*), an extra day is added to Jestha, making it a 30-day month instead of 29 days.
4. **Buddhist Era (BE) Transition**:
   - Unlike naive algorithms that simply add 543 or 544 years indiscriminately, the Buddhist Era officially increments on **1 Roach Pisakh** (the day after Visak Bochea / Buddha Purnima).
5. **Traditional Year Transitions**:
   - **Animal Year (Zodiac)**: Changes on the arrival moment of **Moha Songkran** (mid-April).
   - **Sak**: Increments at **Lerng Sak** (the final day of the New Year festival).
   - **Buddhist Year**: Increments on **1 Roach of Pisakh** in May.

---

## 2. Event System & Data Pipeline

The event architecture merges four distinct tiers of information into a unified, searchable, and filterable event stream.

```
Bundled Dated Events (2000–2030, incl. 2025–2026 official holidays) ──┐
Calculated Recurrences (1800–2200, 100 rules) ────────────────────────┼──► EventRepository ──► UI & Alarms
User Custom Events (SQLite Database) ─────────────────────────────────┘
```

### Event Kinds (`EventKind`)
- `HOLIDAY`: Official public holidays recognized by royal decree or government gazette (MEF & LRC validated).
- `HOLY_DAY`: Buddhist holy days (*Thngai Seil* / 8th & 15th waxing, 8th & 14th/15th waning).
- `OBSERVANCE`: Cultural anniversaries, historical memorials, and UN international observances.
- `CUSTOM`: User-created appointments, reminders, and personal events.

### Data Sources & Storage
- **`calendar-events.tsv`**: Compact, pre-compiled UTF-8 resource bundling 3,246 verified historical events for 2000–2030 from the reference database.
- **`recurring-event-rules.json` / `recurrence-rules.tsv`**: 100 normalized recurrence rules computing festival, royal, heritage, national, and floating weekday dates outside 2000–2030 across 1800–2200.
- **`CustomEventRepository.kt`**: SQLite database (`custom-events.db`) storing custom user events with columns:
  - `id`: Unique UUID string primary key.
  - `title`: Event name (max 120 characters).
  - `date`: Gregorian date (`YYYY-MM-DD`).
  - `time`: Scheduled time (`HH:mm`, minutes precision).
  - `notes`: Optional detailed description (max 2000 characters).
  - `remind`: Boolean reminder-eligibility flag.
  - `zone_id`: Persisted IANA time zone identifier (default `Asia/Phnom_Penh`).
  - `offset_seconds`: Captured fixed UTC offset for the event's original zone (nullable).

---

## 3. Alarm & Notification Subsystem

The application provides local, reliable event notifications without relying on Google Play Services, Firebase Cloud Messaging (FCM), or external background workers.

### Architecture
- **`EventReminderReceiver`** (in `notifications/EventNotifications.kt`): Broadcast receiver triggered by Android's `AlarmManager.setExactAndAllowWhileIdle` for precision alarms.
- **`ReminderRestoreReceiver`** (in `notifications/EventNotifications.kt`): Re-calculates and re-registers the next upcoming alarm upon device restart (`ACTION_BOOT_COMPLETED`), package replacement, clock adjustment (`ACTION_TIME_SET`, `ACTION_TIMEZONE_CHANGED`), or exact-alarm permission changes.
- **Single Next-Alarm Queue**:
  - Rather than filling Android's alarm table with hundreds of future alarms, the app calculates the immediate next event moment and schedules a single alarm.
  - When that alarm fires, notifications are posted, and the queue automatically schedules the subsequent alarm.

### Time-Zone Intelligence
- **"Today Follows" Setting**:
  - `Local Time`: Follows the device's current time zone and adjusts automatically during travel or daylight saving time.
  - `Cambodia (UTC+7)`: Fixed to Phnom Penh time regardless of device location.
- **Built-in Events**: Reminders trigger at the configured daily push time in Cambodia Time (UTC+7).
- **Custom Events**: Reminders trigger at the exact instant intended in the time zone where the event was created, converting cleanly if the user switches display time zones.
- **Repeats**: Configurable periodic repeats (**Off**, 2, 4, 6, 8, or 12 hours) terminate cleanly at midnight in the event's local day.

---

## 4. UI Layer Architecture

The UI is built exclusively using Jetpack Compose with Material 3 design tokens:

- **State Hoisting**: Screens and components are stateless composables driven by immutable state models (`AppSettings`, `KhmerDateDetails`, `CalendarEvent`).
- **Adaptive Scaffolding**: Detects screen dimensions, smallest width (`sw600dp`), and orientation (`ORIENTATION_LANDSCAPE`) to dynamically switch between compact phone layouts, phone landscape rails, and tablet 2-column widescreen experiences.
- **Dynamic Text Scaling**: Custom `readableSp` extension scales typography dynamically based on user-selected font scaling preferences without disrupting fixed grid geometry.
