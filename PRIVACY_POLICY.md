# Privacy Policy for Khmer Calendar (ប្រតិទិនខ្មែរ)

**Effective Date:** September 15, 2026  
**Last Updated:** September 24, 2026

This Privacy Policy explains how **Khmer Calendar (ប្រតិទិនខ្មែរ)** ("the Application"), developed by **RSG-KH** ("we", "us", or "our"), handles your information. 

We strongly believe in personal privacy and digital sovereignty. The Application is built offline-first and open-source. We do not receive your personal events or preferences, and the Application contains no analytics or advertising SDKs.

---

## 1. App Network Access & External Actions
The Application does **not** declare or request the `android.permission.INTERNET` permission in its Android Manifest. Any non-essential permissions merged automatically from third-party libraries (such as `ACCESS_NETWORK_STATE` and `FOREGROUND_SERVICE`) are explicitly stripped using manifest removal rules (`tools:node="remove"`).
* The Application itself does not connect to a server or send analytics or telemetry. Its calendar, event, widget, and reminder features work offline.
* If Android backup is enabled on your device, the operating system may back up the Application's saved data through your configured backup service. See Section 2.

* **User-initiated external search:** The event details screen offers a "Search online" action on built-in events. Tapping it opens your web browser on a search for the event title and its history, as a Custom Tab or an external browser. The browser may connect to the internet and handle the query under its own privacy terms; the Application itself does not open a network connection.

## 2. Information Handling and Storage
* **Personal Data:** We do not collect, store, or transmit your name, email address, phone number, location, IP address, or hardware identifiers.
* **Events, Notes, and Preferences:** Custom events and notes are stored in an app-private SQLite database; settings are stored in app-private Android SharedPreferences. We have no access to your entries or settings.
* **Android backup and transfer:** The Application allows Android backup. Depending on your device and backup settings, Android may copy the database and preferences to your configured cloud backup or transfer them to a new device. Reminder delivery state is excluded from backup. You can manage backup in your device settings. See [Android's Auto Backup documentation](https://developer.android.com/identity/data/autobackup).
* **Alarms & Notifications:** The Application utilizes standard Android exact alarms (`SCHEDULE_EXACT_ALARM`) and notification permissions (`POST_NOTIFICATIONS`) solely to trigger local, on-device alerts for your holy days and custom reminders. No notification data is transmitted over a network.

## 3. Advertising and Third-Party Telemetry
* The Application contains **zero advertisements** (no AdMob, Unity, or third-party ad networks).
* The Application bundles **zero third-party tracking or analytics SDKs** (no Google Analytics, Firebase, Crashlytics, or Facebook SDK).

## 4. User Data Control and Deletion
You retain complete control over all data stored by the Application:
* You can view, edit, or delete personal events at any time directly within the application interface.
* Clearing the Application’s storage via your device’s **Android Settings > Apps > Khmer Calendar > Storage > Clear Data** erases its local databases and preferences.
* Uninstalling the Application removes its local data from that device. If Android made a backup, a separate backup copy may remain under your device's backup settings.

## 5. Children’s Privacy
We do not receive personal information through the Application, including information about children under the age of 13.

## 6. Open Source Transparency
The complete source code of the Application is publicly available under the Apache 2.0 License. Anyone may audit the codebase to verify our offline architecture and privacy commitments:  
[https://github.com/RSG-KH/khmer-calendar](https://github.com/RSG-KH/khmer-calendar)

## 7. Changes to This Privacy Policy
We may update our Privacy Policy periodically. Any updates will be posted to this page with a revised "Last Updated" date.

## 8. Contact & Inquiries
If you have questions, feedback, or inquiries regarding this Privacy Policy or the Application's privacy practices, please open an issue on our repository:  
* **GitHub Issues:** [https://github.com/RSG-KH/khmer-calendar/issues](https://github.com/RSG-KH/khmer-calendar/issues)
