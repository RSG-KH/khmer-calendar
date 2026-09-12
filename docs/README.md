# Khmer Calendar Documentation

Welcome to the technical documentation for the Khmer Calendar Android project. This directory contains detailed guides covering the architecture, UI design system, development workflows, and reference research.

---

## Technical Guides

- **[System Architecture](architecture.md)**
  Detailed breakdown of the core calendar arithmetic engine, New Year calculation rules, offline event repository pipeline, SQLite storage, and exact alarm scheduling subsystem.

- **[UI & Responsive Design](ui-and-responsive-design.md)**
  Comprehensive documentation of the adaptive layout system across phone and tablet form factors in portrait and landscape orientations, custom zero-recomposition scrollbars, optical mark balancing, dynamic font scaling, and theme token styling.

- **[Development & Testing Guide](development-and-testing.md)**
  Setup instructions, build workflows, unit testing with Robolectric and on-device testing with Espresso, running the local translation editor, and updating bundled calendar event data.

---

## Reference & Research Documents

- **[Calendar Source Review](calendar-source-review.md)**
  Evaluation of source libraries (MomentKH, MetheaX, Khmer Hybrid Calendar, date-chinese), engine accuracy findings, and government holiday anchors.

- **[Recurring Event Rules](recurring-event-rules.md)**
  Specification of the 100 normalized recurrence rules for fixed-date, lunar, royal, heritage, and floating weekday observances covering 1800–2200.

- **[Reference Event Database](reference-event-database.md)**
  Capture, audit, and schema details for the 3,246 website event occurrences spanning 2000–2030.

- **[Supplied Event Database Audit](supplied-event-database-audit.md)**
  Audit and reconciliation analysis of the supplied legacy event database and recurrence discrepancies.

---

## Quick Links

- [Root Project README](../README.md)
- [Translation Tool Guide](../tools/translation/README.md)
