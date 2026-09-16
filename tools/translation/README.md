# Khmer Calendar translation editor

Double-click **Start.cmd**, then use <http://127.0.0.1:8766>.
Python 3.10+ is the only requirement. Everything runs locally, with no account or external service.

## Review and save

1. Choose Events, In-app text, Calendar & dates, Notifications, or About & sources.
2. Search in English or Khmer. Edit either column and optionally mark the pair Reviewed.
3. Click **Save changes** or press **Ctrl+S**. Saving writes into the Android project and creates a backup of the previous catalog in this tool's `backups` folder.

Use the review filter to find unreviewed, edited, or unsaved entries. Unsaved edits survive tab changes and browser reloads on the same browser. **Undo edits** restores the last saved pair; **Restore original wording** restores the original imported pair. Export JSON downloads the complete saved catalog for keeping a copy.

Keep placeholders such as `{anniversary}`, `{time}`, and `{zone}`. Their position can change, but deleting or renaming a required placeholder blocks saving. Event templates show example dates with the real stored values. One template correction applies to all its linked dates and any calculated future occurrences that use the same rule; dates, anniversary numbers, arrival times, and official holiday status remain unchanged. Some traditional names use the same wording in both columns until you choose an English rendering.

## How the app uses this

The source of truth is **`translations/catalog.json` inside the Android project**. The tool's `config.json` identifies that project; this folder does not maintain a second editable catalog.

Each save regenerates:

- `app/src/main/resources/translations.tsv` — app labels, calendar names, date formats, notifications, and event templates used by calculated observances.
- `app/src/main/resources/event-translations.tsv` — translated event names expanded to the 3,246 dated occurrences.
- `app/src/main/res/values/strings.xml` and `values-km/strings.xml` — Android launcher names.

**Rebuild and reinstall the APK to see saved corrections on a phone.** Saving does not change an already installed app. Kotlin reads these generated resources offline; builds do not need Python or this server. Generated text is encoded as UTF-8/base64 so Khmer, quotes, tabs and newlines are preserved. Do not edit the generated TSVs directly. The original `calendar-events.tsv` remains unchanged as source data.

The catalog covers app-owned wording. Android/Material system controls and the verbatim open-source license are supplied by their respective libraries; personal custom-event text belongs to the user.

To recover a saved version, stop editing, copy a backup over the project's `translations/catalog.json`, run `python server.py --export`, then reload the editor. Saving from a stale browser version is rejected rather than overwriting newer project changes.

## Development

The versioned tool lives at `tools/translation` inside the Android repository. For a separate installation, set `project` in its `config.json` to the Android repository's location. To update that installation, copy `server.py`, `static`, the launchers and this README into its directory; preserve its config, logs and backups.

Run `python -m unittest discover -s tools/translation -p "test_*.py"` from the repository to check saving, export, backups, placeholder validation and conflicts. `python tools/translation/server.py --export` regenerates resources after an intentional direct catalog edit. The one-time `bootstrap.py` script documents the initial extraction; it refuses to overwrite an existing catalog.

The server binds only to loopback, checks request origins and a session token, and has no endpoint for changing event dates or arbitrary files.
