# Copyright (c) 2026 RSG-KH | Apache-2.0 License
"""Build a review database from the website's publicly rendered month grids.

Run after browser capture and ExportCalendarReference.java:
    python tools/build-reference-events.py
Builds the app's compact UTF-8 event snapshot from SQLite after validation.
No network requests are made here.
"""
import csv
import hashlib
import json
import re
import sqlite3
from collections import Counter
from datetime import date, timedelta
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / 'artifacts/reference-events'
raw_path = OUT / 'site-observed-months.json'
raw = json.loads(raw_path.read_text(encoding='utf-8'))
months = raw['months']
expected_months = [f'{y}-{m:02}' for y in range(2000, 2031) for m in range(1, 13)]
assert [m['month'] for m in months] == expected_months, 'Missing or duplicated month'
days = [d for m in months for d in m['days']]
expected_days = []
cursor = date(2000, 1, 1)
while cursor < date(2031, 1, 1):
    expected_days.append(cursor.isoformat())
    cursor += timedelta(days=1)
assert [d['date'] for d in days] == expected_days, 'Missing or duplicated day'
assert all(isinstance(d['holyDay'], bool) and d['lunar'] for d in days)

events = []
for d in days:
    assert len(d['events']) == len(set(d['events'])), f'Duplicate event: {d["date"]}'
    for label in d['events']:
        assert isinstance(label, str) and label.strip()
        # Keep nested English parentheses and the original Khmer anniversary text.
        split_at = label.find('(')
        km, en = (label[:split_at].strip(), label[split_at + 1:-1].strip()) if split_at > 0 and label.endswith(')') else (label, None)
        events.append(dict(
            id=hashlib.sha256((d['date'] + '\n' + label).encode()).hexdigest()[:24],
            date=d['date'], title_km=km, title_en=en, raw_title=label,
        ))
assert len({e['id'] for e in events}) == len(events)

db_path = OUT / 'khmer-calendar-reference-2000-2030.sqlite'
temp_path = db_path.with_suffix('.sqlite.tmp')
temp_path.unlink(missing_ok=True)
db = sqlite3.connect(temp_path)
db.executescript('''
PRAGMA foreign_keys=ON;
PRAGMA user_version=1;
CREATE TABLE metadata(key TEXT PRIMARY KEY, value TEXT NOT NULL);
CREATE TABLE days(
    date TEXT PRIMARY KEY, lunar_label TEXT NOT NULL,
    holy_day INTEGER NOT NULL CHECK(holy_day IN (0,1))
);
CREATE TABLE events(
    id TEXT PRIMARY KEY, date TEXT NOT NULL REFERENCES days(date),
    title_km TEXT NOT NULL, title_en TEXT, raw_title TEXT NOT NULL,
    source_url TEXT NOT NULL,
    verification_status TEXT NOT NULL DEFAULT 'observed_on_website',
    is_public_holiday INTEGER CHECK(is_public_holiday IN (0,1)),
    UNIQUE(date, raw_title)
);
CREATE INDEX events_date ON events(date);
''')
metadata = {
    'source_url': raw['source'], 'captured_at': raw['capturedAt'],
    'capture_method': raw['method'], 'coverage_start': expected_days[0],
    'coverage_end': expected_days[-1], 'raw_sha256': hashlib.sha256(raw_path.read_bytes()).hexdigest(),
    'status': 'Website capture awaiting validation and compact app export',
    'public_holiday_status': 'Confirmed only for occurrences matched to government snapshots. Other entries are unknown; event labels alone do not establish official leave.',
    'reuse_license': 'No open-data license established from the inspected publisher pages.',
    'time_zone': 'Asia/Phnom_Penh',
}
db.executemany('INSERT INTO metadata VALUES (?,?)', metadata.items())
db.executemany('INSERT INTO days VALUES (?,?,?)', [(d['date'], d['lunar'], d['holyDay']) for d in days])
db.executemany('INSERT INTO events(id,date,title_km,title_en,raw_title,source_url) VALUES (?,?,?,?,?,?)',
               [(e['id'], e['date'], e['title_km'], e['title_en'], e['raw_title'], raw['source']) for e in events])
db.commit()
assert db.execute('PRAGMA integrity_check').fetchone()[0] == 'ok'
assert not db.execute('PRAGMA foreign_key_check').fetchall()
assert db.execute('SELECT count(*) FROM days').fetchone()[0] == len(days)
assert db.execute('SELECT count(*) FROM events').fetchone()[0] == len(events)
db.execute('VACUUM')
db.close()
temp_path.replace(db_path)

# Compare explicit lunar day/phase labels and every holy-day marker to the app.
# The website substitutes a month name on 1 Koeut and a BE label at New Year.
# Compare these separately to the app's month starts and Buddhist year transition.
with (OUT / 'app-lunar-reference.csv').open(encoding='utf-8') as handle:
    native = {r['date']: r for r in csv.DictReader(handle)}
assert set(native) == set(expected_days)
numerals = str.maketrans('០១២៣៤៥៦៧៨៩', '0123456789')
month_names = ['មិគសិរ', 'បុស្ស', 'មាឃ', 'ផល្គុន', 'ចេត្រ', 'ពិសាខ', 'ជេស្ឋ', 'អាសាឍ', 'ស្រាពណ៍', 'ភទ្របទ', 'អស្សុជ', 'កត្តិក', 'បឋមាសាឍ', 'ទុតិយាសាឍ']
lunar_mismatches, holy_mismatches, special_mismatches, labeled_days = [], [], [], 0
for d in days:
    n = native[d['date']]
    match = re.match(r'([0-9]+)\s*(កើត|រោច)', d['lunar'].translate(numerals))
    if match:
        labeled_days += 1
        if int(match[1]) != int(n['day']) or (match[2] == 'កើត') != (n['waxing'] == 'true'):
            lunar_mismatches.append({'date': d['date'], 'website': d['lunar'], 'app_day': int(n['day']), 'app_waxing': n['waxing'] == 'true'})
    else:
        if d['lunar'] in month_names:
            matches = int(n['month']) == month_names.index(d['lunar']) and n['day'] == '1' and n['waxing'] == 'true'
        elif d['lunar'].startswith('ព.ស.'):
            matches = d['lunar'].removeprefix('ព.ស.').translate(numerals) == n['buddhist_year'] and n['day'] == '1' and n['waxing'] == 'false' and n['month'] == '5'
        else:
            raise ValueError('Unknown lunar label: ' + d['lunar'])
        if not matches:
            special_mismatches.append({'date': d['date'], 'website': d['lunar'], 'app': n})
    if d['holyDay'] != (n['holy'] == 'true'):
        holy_mismatches.append({'date': d['date'], 'website': d['holyDay'], 'app': n['holy'] == 'true'})

# Compare only the app's already transcribed government annual snapshots.
# This is a comparison, not a new certification of website holiday status.
aliases = {
    'new-year': "New Year's Day", 'victory': 'Victory Over Genocide Day',
    'women': "International Women's Day", 'khmer-new-year': 'Khmer New Year -',
    'labour': 'International Labor Day', 'visak': 'Visak Bochea',
    'king-birthday': "King Sihamoni's Birthday", 'ploughing': 'Royal Ploughing Ceremony',
    'queen-birthday': "Queen Mother's Birthday", 'pchum': 'Pchum Ben Festival',
    'constitution': 'Constitution Day', 'king-father': "King Father's Commemoration Day",
    'coronation': "King's Coronation Day", 'water': 'Water Festival',
    'independence': 'Independence Day', 'peace': 'Peace Day in Cambodia',
}
snapshots = json.loads((ROOT / 'tools/reference-government-holidays.json').read_text(encoding='utf-8'))
anchor_results = []
for snapshot in snapshots:
    year, source = str(snapshot['year']), snapshot['source_url']
    for event_id, month, first, last in snapshot['dates']:
        expected = [f'{year}-{int(month):02}-{day:02}' for day in range(int(first), int(last) + 1)]
        found = [e['date'] for e in events if e['date'].startswith(year) and (e['title_en'] or '').startswith(aliases[event_id])]
        anchor_results.append({'year': int(year), 'event_id': event_id, 'expected': expected, 'observed': found, 'match': expected == found, 'comparison_source': source})
assert len(anchor_results) == 32, 'Unexpected government snapshot parsing'

# Enrich matching website occurrences with reviewed holiday status, preserving
# the website date/name and never extrapolating government status to other years.
assert all(a['match'] for a in anchor_results), 'Review changed government anchors before publishing'
assert not lunar_mismatches and not holy_mismatches and not special_mismatches, 'Review calendar discrepancies before publishing'
official = {}
for a in anchor_results:
    for e in events:
        if e['date'] in a['expected'] and (e['title_en'] or '').startswith(aliases[a['event_id']]):
            official[e['id']] = a['comparison_source']
assert len(official) == 44
with sqlite3.connect(db_path) as published:
    published.execute('ALTER TABLE events ADD COLUMN official_source_url TEXT')
    published.executemany("UPDATE events SET is_public_holiday=1, verification_status='matched_government_snapshot', official_source_url=? WHERE id=?", [(url, event_id) for event_id, url in official.items()])
    published.execute("UPDATE metadata SET value='Bundled website snapshot; official status only where independently matched' WHERE key='status'")
    published.execute('PRAGMA user_version=2')
    packed_rows = published.execute('SELECT id,date,title_km,title_en,coalesce(official_source_url,\'\') FROM events ORDER BY date,id').fetchall()
    assert len(packed_rows) == len(events)
    assert published.execute('PRAGMA integrity_check').fetchone()[0] == 'ok'
    assert not published.execute('PRAGMA foreign_key_check').fetchall()
    published.commit()
    published.execute('VACUUM')

# A compact Java resource avoids a writable database copy and lets the same
# repository serve Android, notification receivers, JVM tests and Compose previews.
resource = ROOT / 'app/src/main/resources/calendar-events.tsv'
resource.parent.mkdir(parents=True, exist_ok=True)
assert all('\t' not in field and '\n' not in field and '\r' not in field for row in packed_rows for field in row)
header = ['# Khmer Calendar event snapshot v1', '# source=' + raw['source'], '# captured_at=' + raw['capturedAt'],
          '# coverage=2000-01-01/2030-12-31', '# database_sha256=' + hashlib.sha256(db_path.read_bytes()).hexdigest(),
          '# columns=id,date,title_km,title_en,official_source_url']
resource.write_text('\n'.join(header + ['\t'.join(row) for row in packed_rows]) + '\n', encoding='utf-8')

audit = {
    'months': len(months), 'days': len(days), 'events': len(events),
    'holy_days': sum(d['holyDay'] for d in days), 'database_bytes': db_path.stat().st_size,
    'labeled_lunar_days_compared': labeled_days,
    'lunar_day_phase_mismatches': lunar_mismatches,
    'month_start_and_be_mismatches': special_mismatches,
    'holy_day_mismatches': holy_mismatches,
    'government_snapshot_comparison': anchor_results,
    'events_per_year': dict(sorted(Counter(e['date'][:4] for e in events).items())),
}
(OUT / 'audit.json').write_text(json.dumps(audit, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
summary = {k: v for k, v in audit.items() if k not in ('lunar_day_phase_mismatches', 'month_start_and_be_mismatches', 'holy_day_mismatches', 'government_snapshot_comparison', 'events_per_year')}
summary.update(lunar_mismatches=len(lunar_mismatches), month_start_and_be_mismatches=len(special_mismatches), holy_mismatches=len(holy_mismatches), matched_government_categories=sum(a['match'] for a in anchor_results), government_categories=len(anchor_results))
print(json.dumps(summary, indent=2))
