# Privacy Policy for Khmer Calendar (ប្រតិទិនខ្មែរ)

**Effective Date:** September 15, 2026  
**Last Updated:** September 15, 2026  

This Privacy Policy explains how **Khmer Calendar (ប្រតិទិនខ្មែរ)** ("the Application"), developed by **RSG-KH** ("we", "us", or "our"), handles your information. 

We strongly believe in personal privacy and digital sovereignty. The Application is built offline-first, open-source, and does not collect, track, or share any personal user data.

---

## 1. Zero Network Access & Internet Permissions
The Application does **not** declare or request the `android.permission.INTERNET` permission in its Android Manifest. Any non-essential permissions merged automatically from third-party libraries (such as `ACCESS_NETWORK_STATE` and `FOREGROUND_SERVICE`) are explicitly stripped using manifest removal rules (`tools:node="remove"`).
* It is technically impossible for the Application to send data, logs, or telemetry from your device to any external server.
* The Application functions 100% offline at all times.

* **User-initiated external search:** The event details screen offers a "Search online" action on built-in events. Tapping it opens your web browser directly on an AI-mode search for the event title and its history — as a Custom Tab or an external browser — through Android's standard intent system, an action you explicitly initiate. The Application itself never opens a network connection, and nothing beyond that query is transmitted.

## 2. Information Handling and Storage
* **Personal Data:** We do not collect, store, or transmit your name, email address, phone number, location, IP address, or hardware identifiers.
* **Events, Notes, and Preferences:** Any custom calendar events, personal notes, reminders, or user preferences you save are stored exclusively on your local device within an isolated SQLite database protected by Android's application sandbox and device-level storage encryption. We have no access to your entries.
* **Alarms & Notifications:** The Application utilizes standard Android exact alarms (`SCHEDULE_EXACT_ALARM`) and notification permissions (`POST_NOTIFICATIONS`) solely to trigger local, on-device alerts for your holy days and custom reminders. No notification data is transmitted over a network.

## 3. Advertising and Third-Party Telemetry
* The Application contains **zero advertisements** (no AdMob, Unity, or third-party ad networks).
* The Application bundles **zero third-party tracking or analytics SDKs** (no Google Analytics, Firebase, Crashlytics, or Facebook SDK).

## 4. User Data Control and Deletion
You retain complete control over all data stored by the Application:
* You can view, edit, or delete personal events at any time directly within the application interface.
* Clearing the Application’s storage via your device’s **Android Settings > Apps > Khmer Calendar > Storage > Clear Data** permanently erases all databases and preferences.
* Uninstalling the Application permanently removes all locally stored data from your device immediately.

## 5. Children’s Privacy
Because the Application does not collect, retain, or share any personal information whatsoever, it does not knowingly collect personally identifiable information from children under the age of 13.

## 6. Open Source Transparency
The complete source code of the Application is publicly available under the Apache 2.0 License. Anyone may audit the codebase to verify our offline architecture and privacy commitments:  
[https://github.com/RSG-KH/khmer-calendar](https://github.com/RSG-KH/khmer-calendar)

## 7. Changes to This Privacy Policy
We may update our Privacy Policy periodically. Any updates will be posted to this page with a revised "Last Updated" date.

## 8. Contact & Inquiries
If you have questions, feedback, or inquiries regarding this Privacy Policy or the Application's privacy practices, please open an issue on our repository:  
* **GitHub Issues:** [https://github.com/RSG-KH/khmer-calendar/issues](https://github.com/RSG-KH/khmer-calendar/issues)
