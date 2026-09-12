# Copyright (c) 2026 RSG-KH | Apache-2.0 License
"""Read and convert a supplied event catalog, without executing its generator.

This is a development audit, not an app-data importer. Source holiday flags and
anniversary bases remain unverified claims. No personal reminders or feeds are
exported. The currently bundled event snapshot is never modified.
"""
import argparse
import ast
import csv
import hashlib
import io
import json
import re
import sqlite3
import zlib
from collections import Counter
from datetime import date
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
MONTHS = {
    'មិគសិរ': 0, 'បុស្ស': 1, 'មាឃ': 2, 'ផល្គុន': 3, 'ចេត្រ': 4,
    'ពិសាខ': 5, 'ជេស្ឋ': 6, 'អាសាឍ': 7, 'ស្រាពណ៍': 8, 'ភទ្របទ': 9,
    'អស្សុជ': 10, 'កត្តិក': 11, 'កក្ដិក': 11,
}
# Explicit name crosswalk for this supplied catalog; dates are compared below.
ALIASES = {
    'new_year_day': ["New Year's Day"],
    'khmer_new_year_1': ['Khmer New Year - Moha Sankranta at '],
    'khmer_new_year_2': ['Khmer New Year - Veareak Vanabat'],
    'khmer_new_year_3': ['Khmer New Year - Veareak Laeung Sak'],
    'pchum_ben_festival': ['Pchum Ben Festival'],
    'water_festival_day1': ['Water Festival'],
    'water_festival_day2': ['Water Festival'],
    'water_festival_day3': ['Water Festival'],
    'peace_day_win_win': ['Win-win Policy Day', 'Peace Day in Cambodia'],
    'chinese_new_year_day1': ['Chinese New Year'],
    'qingming_festival': ['Tomb-Sweeping Day (Qingming Festival)'],
    'ghost_festival': ['Ghost Festival'],
    'winter_solstice_festival': ['Winter Solstice Festival'],
    'arbor_day': ['Arbor Day'],
    'national_population_day': ['National Population Day and World Population Day'],
}
DIGITS = str.maketrans('០១២៣៤៥៦៧៨៩', '0123456789')


def read_only(path):
    return sqlite3.connect(path.resolve().as_uri() + '?mode=ro', uri=True)


def dump(path, value, compact=False):
    data = json.dumps(value, ensure_ascii=False, indent=None if compact else 2,
                      separators=(',', ':') if compact else None) + '\n'
    path.write_text(data, encoding='utf-8', newline='\n')
    return data.encode('utf-8')


def matches(title, labels):
    return any(title.startswith(label) if label.endswith(' at ') else title == label for label in labels)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('source', type=Path)
    parser.add_argument('--output', type=Path, default=ROOT / 'artifacts/supplied-event-audit')
    parser.add_argument('--lunar', type=Path, default=ROOT / 'artifacts/reference-events/app-lunar-reference.csv')
    args = parser.parse_args()
    source, out = args.source.resolve(), args.output.resolve()
    if out == source or source in out.parents:
        raise ValueError('Audit outputs must be outside the source directory')
    out.mkdir(parents=True, exist_ok=True)
    hashes = {p.name: hashlib.sha256(p.read_bytes()).hexdigest() for p in source.iterdir() if p.is_file()}
    with read_only(source / 'events.db') as db:
        db.row_factory = sqlite3.Row
        integrity = [row[0] for row in db.execute('PRAGMA integrity_check')]
        if integrity != ['ok']:
            raise ValueError(integrity)
        counts = {name: db.execute('SELECT COUNT(*) FROM "' + name.replace('"', '""') + '"').fetchone()[0]
                  for name, in db.execute("SELECT name FROM sqlite_master WHERE type='table'")}
        rules = [dict(row) for row in db.execute('SELECT * FROM calendar_events ORDER BY id')]
    if len({row['key'] for row in rules}) != len(rules):
        raise ValueError('Duplicate event keys')
    json_data = json.loads((source / 'events.json').read_text(encoding='utf-8'))
    tree = ast.parse((source / 'generate_database.py').read_text(encoding='utf-8-sig'))
    assignment = next(node for node in tree.body if isinstance(node, ast.Assign)
                      and any(isinstance(target, ast.Name) and target.id == 'events_data' for target in node.targets))
    generator_rules = ast.literal_eval(assignment.value)
    if rules != json_data['events'] or rules != generator_rules or json_data['total_events'] != len(rules):
        raise ValueError('SQLite, JSON, or literal generator catalog differ')

    with (ROOT / 'app/src/main/resources/calendar-events.tsv').open(encoding='utf-8') as stream:
        observed = [dict(zip(('id', 'date', 'km', 'en', 'official_url'), row))
                    for row in csv.reader((line for line in stream if not line.startswith('#')), delimiter='\t')]
    years = sorted({int(row['date'][:4]) for row in observed})
    with args.lunar.open(encoding='utf-8') as stream:
        lunar = list(csv.DictReader(stream))
    if {row['date'] for row in lunar} != {date(y, m, d).isoformat() for y in years for m in range(1, 13)
                                        for d in range(1, 32) if valid_date(y, m, d)}:
        raise ValueError('Native lunar export does not cover the observed date range')
    issues, comparisons, anniversary_differences = [], [], []
    represented = set()
    literal_predictions, observed_by_group = {}, {}
    normalized = []
    for row in rules:
        key, kind = row['key'], row['calendar_type']
        labels = ALIASES.get(key, [row['title_en']])
        found = [entry for entry in observed if matches(entry['en'], labels)]
        if not found:
            raise ValueError('Missing explicit name mapping for ' + key)
        represented.update(entry['id'] for entry in found)
        group = '|'.join(labels)
        observed_by_group.setdefault(group, set()).update(entry['date'] for entry in found)
        predicted = set()
        if kind == 'solar':
            predicted = {date(y, row['solar_month'], row['solar_day']).isoformat() for y in years}
        elif kind == 'khmer_lunar':
            predicted = {entry['date'] for entry in lunar if int(entry['month']) == MONTHS[row['lunar_month']]
                         and int(entry['day']) == row['lunar_day']
                         and (entry['waxing'] == 'true') == (row['lunar_moon_phase'] == 'កើត')}
            if row['lunar_month'] == 'អាសាឍ':
                issues.append({'key': key, 'issue': 'No intercalary-Asadh selection rule; literal ordinary-month matching has gaps.'})
        else:
            issues.append({'key': key, 'issue': 'Needs a calendar/astronomical algorithm; this database supplies no dated results or executable rule.'})
        literal_predictions.setdefault(group, set()).update(predicted)
        found_dates = {entry['date'] for entry in found}
        if kind in ('solar', 'khmer_lunar'):
            comparisons.append({
                'key': key, 'predicted': len(predicted), 'matched': len(predicted & found_dates),
                'unmatched_predictions': sorted(predicted - found_dates),
                'years_without_prediction': [y for y in years if not any(d.startswith(str(y)) for d in predicted)],
                'examples': [{'year': y, 'predicted': sorted(d for d in predicted if d.startswith(str(y))),
                              'observed': sorted(d for d in found_dates if d.startswith(str(y)))} for y in (2025, 2026)],
            })
        if row['show_anniversary']:
            for entry in found:
                text = re.sub(r'[\s\u200b]+', '', entry['km']).translate(DIGITS)
                count = re.search(r'ខួបលើកទី(\d+)', text)
                expected = int(entry['date'][:4]) - row['base_year']
                if count and int(count[1]) != expected:
                    anniversary_differences.append({'key': key, 'date': entry['date'], 'source_base': row['base_year'],
                                                   'computed_count': expected, 'website_count': int(count[1])})
        normalized.append({
            'id': key, 'title_km': row['title_km'], 'title_en': row['title_en'],
            'rule': {k: row[k] for k in ('calendar_type', 'solar_month', 'solar_day', 'lunar_month', 'lunar_moon_phase', 'lunar_day') if row[k] is not None},
            'source_claims': {k: row[k] for k in ('category', 'is_holiday', 'is_siel_day', 'base_year', 'show_anniversary')},
            'verification': 'unverified_supplied_rule',
        })
    normalized_data = {'format': 1, 'status': 'review_only_not_app_data', 'source_sha256': hashes['events.db'],
                       'dated_occurrences': 0, 'rules': normalized}
    packed = dump(out / 'calendar-rules.review.json', normalized_data, compact=True)
    buffer = io.StringIO(newline='')
    columns = ('key', 'title_km', 'title_en', 'calendar_type', 'solar_month', 'solar_day', 'lunar_month',
               'lunar_moon_phase', 'lunar_day', 'is_holiday', 'is_siel_day', 'base_year', 'show_anniversary')
    writer = csv.DictWriter(buffer, fieldnames=columns, delimiter='\t', lineterminator='\n', extrasaction='ignore')
    writer.writeheader()
    writer.writerows(rules)
    (out / 'calendar-rules.review.tsv').write_text(buffer.getvalue(), encoding='utf-8', newline='\n')

    dates_not_reproduced = {group: sorted(dates - literal_predictions.get(group, set()))
                           for group, dates in observed_by_group.items() if dates - literal_predictions.get(group, set())}
    report = {
        'source_sha256': hashes, 'integrity': integrity, 'table_counts': counts,
        'database_json_generator_agree': True, 'generator_catalog_is_literal_list': True,
        'calendar_types': dict(Counter(row['calendar_type'] for row in rules)),
        'source_public_holiday_flags': sum(row['is_holiday'] for row in rules),
        'source_siel_flags': sum(row['is_siel_day'] for row in rules),
        'dated_calendar_occurrences': 0, 'explicit_effective_year_fields': False,
        'comparison_range': [min(years), max(years)], 'observed_event_occurrences': len(observed),
        'observed_occurrences_in_supplied_event_families': len(represented),
        'observed_occurrences_without_a_supplied_event_family': len(observed) - len(represented),
        'comparison_method': 'Literal solar and Khmer-lunar matching against native CSV; explicit name crosswalk to the bundled website capture. No dates inferred for Chinese/astronomical rules; no legal or historical verification.',
        'comparisons': comparisons, 'missing_rule_details': issues,
        'anniversary_differences': anniversary_differences,
        'observed_dates_not_reproduced_by_literal_rules': dates_not_reproduced,
        'conversion_sizes': {'normalized_json_bytes': len(packed), 'normalized_json_deflate_bytes': len(zlib.compress(packed)),
                             'normalized_tsv_bytes': len(buffer.getvalue().encode('utf-8'))},
        'disposition': 'Not suitable as a full replacement or verified 1900–2100 extension. Keep the existing dated snapshot; use this export only as a rule-review candidate.',
    }
    dump(out / 'audit.json', report)
    unmatched = sum(len(row['unmatched_predictions']) for row in comparisons)
    total = sum(row['predicted'] for row in comparisons)
    summary = {
        'rules': len(rules), 'calendar_types': report['calendar_types'], 'dated_occurrences': 0,
        'literal_date_comparisons': total, 'unmatched_literal_predictions': unmatched,
        'anniversary_differences': len(anniversary_differences),
        'missing_event_family_occurrences': report['observed_occurrences_without_a_supplied_event_family'],
        'conversion_sizes': report['conversion_sizes'], 'output': str(out),
    }
    print(json.dumps(summary, ensure_ascii=False, indent=2))


def valid_date(year, month, day):
    try:
        date(year, month, day)
        return True
    except ValueError:
        return False


if __name__ == '__main__':
    main()
