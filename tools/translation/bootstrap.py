# Copyright (c) 2026 RSG-KH | Apache-2.0 License
"""One-time migration of the existing bilingual Kotlin literals into a catalog."""
import csv
import hashlib
import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
CATALOG = ROOT / 'translations/catalog.json'
entries = []
pairs = {}


def add(key, km, en, section='interface', context='', occurrences=None):
    entry = {'id': key, 'section': section, 'context': context, 'en': en, 'km': km,
             'original': {'en': en, 'km': km}, 'reviewed': False,
             'required': {lang: sorted(set(re.findall(r'\{(\w+)\}', value))) for lang, value in [('en', en), ('km', km)]}}
    if occurrences is not None:
        entry['occurrences'] = occurrences
    entries.append(entry)
    return key


def string_end(text, pos):
    assert text[pos] == '"'
    i = pos + 1
    while i < len(text):
        if text[i] == '\\':
            i += 2
        elif text.startswith('${', i):
            depth = 1
            i += 2
            while depth:
                if text[i] == '"':
                    i = string_end(text, i)
                else:
                    depth += (text[i] == '{') - (text[i] == '}')
                    i += 1
        elif text[i] == '"':
            return i + 1
        else:
            i += 1
    raise ValueError('Unterminated string')


def literal(raw):
    return json.loads(raw)  # Existing literals only use JSON-compatible escapes.


def extract(text, path):
    replacements = []
    for match in re.finditer(r'\btr\(k,\s*"', text):
        start = match.end() - 1
        end = string_end(text, start)
        english_start = end + len(re.match(r',\s*', text[end:])[0])
        english_end = string_end(text, english_start)
        assert text[english_end] == ')'
        km, en = literal(text[start:end]), literal(text[english_start:english_end])
        args = []
        if '$' in km or '$' in en:
            if '$zoneLabel' in km:
                km, en = km.replace('$zoneLabel', '{zone}'), en.replace('$zoneLabel', '{zone}')
                args = ['"zone" to zoneLabel']
            elif 'number(year, k)' in km:
                km, en = km.replace('${number(year, k)}', '{year}'), en.replace('$year', '{year}')
                args = ['"year" to number(year, k)']
            elif 'number(event.date.year, true)' in km:
                km, en = km.replace('${number(event.date.year, true)}', '{year}'), en.replace('${event.date.year}', '{year}')
                args = ['"year" to number(event.date.year, k)']
            else:
                raise ValueError((path, km, en))
        pair = (km, en)
        if pair not in pairs:
            slug = re.sub('[^a-z0-9]+', '_', en.lower()).strip('_')[:55]
            digest = hashlib.sha256((km + '\0' + en).encode()).hexdigest()[:6]
            section = 'notifications' if path.name == 'NotificationSettings.kt' else 'interface'
            if 'SourcesDialog' in text[:match.start()].split('@Composable')[-1] or 'CoverageNote' in text[:match.start()].split('@Composable')[-1]:
                section = 'about'
            pairs[pair] = add('ui.' + slug + '.' + digest, km, en, section, path.name)
        replacements.append((match.start(), english_end + 1, 'L.text("' + pairs[pair] + '", k' + (', ' + ', '.join(args) if args else '') + ')'))
    for start, end, replacement in reversed(replacements):
        text = text[:start] + replacement + text[end:]
    text = text.replace('internal fun tr(k: Boolean, km: String, en: String) = if (k) km else en\n', '')
    if replacements:
        text = text.replace('import androidx.compose', 'import com.rsgkh.calendar.i18n.L\n\nimport androidx.compose', 1)
    return text


def main():
    if CATALOG.exists():
        raise SystemExit('Catalog already exists; migration will not overwrite it.')
    for path in (ROOT / 'app/src/main/java/com/rsgkh/calendar/ui').glob('*.kt'):
        text = path.read_text(encoding='utf-8')
        path.write_text(extract(text, path), encoding='utf-8', newline='\n')
    with (ROOT / 'app/src/main/resources/calendar-events.tsv').open(encoding='utf-8') as stream:
        rows = csv.reader((line for line in stream if not line.startswith('#')), delimiter='\t')
        groups = {}
        for event_id, day, km, en, _ in rows:
            values = {'en': {}, 'km': {}}
            texts = {'en': en, 'km': km}
            for lang in texts:
                def anniversary(match):
                    digits = match[1]
                    values['km']['anniversary'] = digits
                    values['en']['anniversary'] = digits.translate(str.maketrans('០១២៣៤៥៦៧៨៩', '0123456789'))
                    return 'ខួបលើកទី{anniversary}'
                texts[lang] = re.sub(r'ខួបលើកទី\s*([០-៩0-9]+)', anniversary, texts[lang])
                def arrival(match):
                    values[lang]['time'] = match[0]
                    return '{time}'
                texts[lang] = re.sub(r'[០-៩0-9]{1,2}:[០-៩0-9]{2}\s*[AP]M', arrival, texts[lang])
            pair = (texts['km'], texts['en'])
            groups.setdefault(pair, []).append({'id': event_id, 'date': day, 'values': values})
        for (km, en), occurrences in groups.items():
            slug = re.sub('[^a-z0-9]+', '_', en.lower()).strip('_')[:55]
            key = 'event.' + slug + '.' + hashlib.sha256((km + '\0' + en).encode()).hexdigest()[:6]
            add(key, km, en, 'events', f'{len(occurrences)} dates · {occurrences[0]["date"]} to {occurrences[-1]["date"]}', occurrences)
    CATALOG.parent.mkdir(parents=True, exist_ok=True)
    CATALOG.write_text(json.dumps({'format': 1, 'languages': ['en', 'km'], 'entries': entries}, ensure_ascii=False, indent=2) + '\n', encoding='utf-8', newline='\n')
    print(f'{len(entries)} entries created')


if __name__ == '__main__':
    main()
