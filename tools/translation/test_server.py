# Copyright (c) 2026 RSG-KH | Apache-2.0 License
import base64
import copy
import json
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch

from server import Conflict, Store, exports, render, replace_files, validate

PROJECT = Path(__file__).resolve().parents[2]


def decoded_rows(raw):
    return {parts[0]: tuple(base64.b64decode(value).decode('utf-8') for value in parts[1:])
            for line in raw.decode('utf-8').splitlines() if line and not line.startswith('#')
            for parts in [line.split('\t')]}


class TranslationStoreTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.project = Path(self.temp.name) / 'project'
        self.catalog = json.loads((PROJECT / 'translations/catalog.json').read_text(encoding='utf-8'))
        self.store = Store(self.project, Path(self.temp.name) / 'backups')
        self.store.path.parent.mkdir(parents=True)
        self.store.path.write_text(json.dumps(self.catalog, ensure_ascii=False), encoding='utf-8')

    def edit(self, entry, **changes):
        return dict(id=entry['id'], **({k: entry[k] for k in ('en', 'km', 'reviewed')} | changes))

    def test_original_templates_losslessly_cover_every_source_occurrence(self):
        source_dates = {}
        for line in (PROJECT / 'app/src/main/resources/calendar-events.tsv').read_text(encoding='utf-8').splitlines():
            if not line or line.startswith('#'):
                continue
            key, date, _, _, _ = line.split('\t')
            source_dates[key] = date
        exported_events = decoded_rows((PROJECT / 'app/src/main/resources/event-translations.tsv').read_bytes())
        reconstructed = {}
        for entry in self.catalog['entries']:
            for event in entry.get('occurrences', []):
                reconstructed[event['id']] = (event['date'], *[render(entry['original'][lang], event['values'][lang]) for lang in ('km', 'en')])
        self.assertEqual(3246, len(reconstructed))
        self.assertEqual(len(source_dates), len(reconstructed))
        for key, (date, km, en) in reconstructed.items():
            self.assertEqual(source_dates[key], date)
            self.assertEqual(exported_events[key], (km, en))

    def test_saved_khmer_and_multiline_text_reach_android_and_survive_reopen(self):
        entry = next(e for e in self.catalog['entries'] if e['id'] == 'language.khmer')
        original = self.store.path.read_bytes()
        old = self.store.public()
        change = self.edit(entry, en='Khmer "language"\nSecond line\t✓', km='ភាសាខ្មែរ\nអរគុណ', reviewed=True)
        result = self.store.save(old['revision'], [change])
        new = Store(self.project, self.store.backups).public()
        self.assertEqual(result['revision'], new['revision'])
        edited = next(e for e in new['entries'] if e['id'] == entry['id'])
        self.assertEqual(change['km'], edited['km'])
        self.assertTrue(edited['reviewed'])
        rows = decoded_rows((self.project / 'app/src/main/resources/translations.tsv').read_bytes())
        self.assertEqual((change['km'], change['en']), rows[entry['id']])
        self.assertEqual([original], [p.read_bytes() for p in self.store.backups.glob('*.json')])
        self.assertNotEqual(old['revision'], new['revision'])

    def test_event_correction_reaches_all_linked_years_with_original_values(self):
        entry = next(e for e in self.catalog['entries'] if 'anniversary' in e['required']['km'])
        result = self.store.save(self.store.public()['revision'], [self.edit(entry, km='ខួប {anniversary} — កែសម្រួល', en='Edited anniversary {anniversary}')])
        rows = decoded_rows((self.project / 'app/src/main/resources/event-translations.tsv').read_bytes())
        self.assertEqual(3246, len(rows))
        templates = decoded_rows((self.project / 'app/src/main/resources/translations.tsv').read_bytes())
        self.assertEqual(('ខួប {anniversary} — កែសម្រួល', 'Edited anniversary {anniversary}'), templates[entry['id']])
        for event in entry['occurrences']:
            self.assertEqual('ខួប ' + event['values']['km']['anniversary'] + ' — កែសម្រួល', rows[event['id']][0])
            self.assertEqual('Edited anniversary ' + event['values']['en']['anniversary'], rows[event['id']][1])
        saved = json.loads(self.store.path.read_text(encoding='utf-8'))
        updated = next(e for e in saved['entries'] if e['id'] == entry['id'])
        self.assertEqual(entry['occurrences'], updated['occurrences'])
        self.assertEqual(entry['original'], updated['original'])
        self.assertEqual(len(entry['occurrences']), next(e for e in result['entries'] if e['id'] == entry['id'])['occurrenceCount'])

    def test_invalid_placeholder_or_blank_text_leaves_files_and_backups_untouched(self):
        entry = next(e for e in self.catalog['entries'] if 'anniversary' in e['required']['km'])
        self.store.export()
        before = {p: p.read_bytes() for p in self.project.rglob('*') if p.is_file()}
        for km in ['Missing required token', '{anniversary} {misspelled}', '   ']:
            with self.assertRaises(ValueError):
                self.store.save(self.store.public()['revision'], [self.edit(entry, km=km)])
            self.assertEqual(before, {p: p.read_bytes() for p in self.project.rglob('*') if p.is_file()})
            self.assertFalse(self.store.backups.exists())

    def test_stale_revision_cannot_overwrite_a_newer_correction(self):
        entry = self.catalog['entries'][0]
        revision = self.store.public()['revision']
        self.store.save(revision, [self.edit(entry, en=entry['en'] + ' corrected')])
        saved = self.store.path.read_bytes()
        with self.assertRaises(Conflict):
            self.store.save(revision, [self.edit(entry, en='Stale browser')])
        self.assertEqual(saved, self.store.path.read_bytes())

    def test_failed_multi_file_write_rolls_back_completed_files(self):
        one, two = self.project / 'one', self.project / 'two'
        one.write_bytes(b'original one')
        two.write_bytes(b'original two')
        import os
        original_replace = os.replace
        def fail_second(src, dest):
            if dest == two:
                raise OSError('Simulated locked file')
            return original_replace(src, dest)
        with patch('server.os.replace', side_effect=fail_second), self.assertRaises(OSError):
            replace_files({one: b'changed one', two: b'changed two'})
        self.assertEqual(b'original one', one.read_bytes())
        self.assertEqual(b'original two', two.read_bytes())
        self.assertFalse(list(self.project.glob('*.tmp')))

    def test_committed_android_outputs_match_catalog(self):
        catalog, revision = Store(PROJECT, PROJECT / 'artifacts/unused').read()
        for path, expected in exports(PROJECT, catalog, revision).items():
            self.assertEqual(expected, path.read_bytes(), str(path))

    def test_current_catalog_and_static_android_keys_are_valid(self):
        validate(self.catalog)
        import re
        keys = {e['id'] for e in self.catalog['entries']}
        for path in (PROJECT / 'app/src/main/java').rglob('*.kt'):
            for key in re.findall(r'L\.text\("([a-z0-9_.]+)"', path.read_text(encoding='utf-8')):
                self.assertIn(key, keys, str(path))


if __name__ == '__main__':
    unittest.main()
