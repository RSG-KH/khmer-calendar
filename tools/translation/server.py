# Copyright (c) 2026 RSG-KH | Apache-2.0 License
"""Local English/Khmer editor and deterministic Android resource exporter (stdlib only)."""
import argparse
import base64
import hashlib
import json
import os
import re
import secrets
import threading
from datetime import datetime, timezone
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from urllib.parse import urlsplit
from xml.etree import ElementTree as ET

TOKEN = re.compile(r'\{([A-Za-z][A-Za-z0-9_]*)}')
SECTIONS = [dict(id=k, label=v) for k, v in [('events', 'Events'), ('interface', 'In-app text'),
            ('calendar', 'Calendar & dates'), ('notifications', 'Notifications'), ('about', 'About & sources')]]


def encoded(text):
    return base64.b64encode(text.encode('utf-8')).decode('ascii')


def render(template, values):
    return TOKEN.sub(lambda match: str(values[match[1]]), template)


def validate(catalog):
    if catalog.get('format') != 1:
        raise ValueError('Unsupported catalog format.')
    ids = set()
    event_ids = set()
    for entry in catalog['entries']:
        key = entry['id']
        if key in ids or not re.fullmatch(r'[a-z0-9_.]+', key):
            raise ValueError('Invalid or duplicate translation key: ' + key)
        ids.add(key)
        allowed = set().union(*map(set, entry['required'].values()))
        for lang in ('en', 'km'):
            value = entry[lang]
            if not isinstance(value, str) or not value.strip() or len(value) > 8000 or any(ord(c) < 32 and c not in '\n\t' for c in value):
                raise ValueError(f'{key}: {lang} must contain valid text (maximum 8,000 characters).')
            tokens = set(TOKEN.findall(value))
            missing = set(entry['required'][lang]) - tokens
            unknown = tokens - allowed
            if missing or unknown:
                raise ValueError(f'{key} ({lang}): ' + (f'keep {", ".join("{"+s+"}" for s in sorted(missing))}. ' if missing else '')
                                 + (f'Unknown placeholders: {", ".join(sorted(unknown))}.' if unknown else ''))
        for occurrence in entry.get('occurrences', []):
            if occurrence['id'] in event_ids:
                raise ValueError('Duplicate dated event translation.')
            event_ids.add(occurrence['id'])
            for lang in ('en', 'km'):
                render(entry[lang], occurrence['values'][lang])
    if 'app.name' not in ids:
        raise ValueError('Missing app name.')


def exports(project, catalog, revision):
    validate(catalog)
    words = []
    name = None
    for entry in catalog['entries']:
        if entry['id'] == 'app.name':
            name = entry
        words.append((entry['id'], encoded(entry['km']), encoded(entry['en'])))
    def tsv(rows):
        return ('# Generated from translations/catalog.json; edit with the translation tool.\n# catalog-sha256: ' + revision + '\n'
                + '\n'.join('\t'.join(row) for row in sorted(rows)) + '\n').encode('utf-8')
    resource = project / 'app/src/main/resources'
    outputs = {resource / 'translations.tsv': tsv(words)}
    for lang, folder in [('en', 'values'), ('km', 'values-km')]:
        root = ET.Element('resources')
        element = ET.SubElement(root, 'string', name='app_name', formatted='false')
        element.text = '"' + name[lang].replace('\\', '\\\\').replace('"', '\\"').replace('\n', '\\n') + '"'
        outputs[project / f'app/src/main/res/{folder}/strings.xml'] = ET.tostring(root, encoding='utf-8', xml_declaration=True) + b'\n'
    return outputs


def replace_files(outputs):
    previous = {path: path.read_bytes() if path.exists() else None for path in outputs}
    temporary = {}
    completed = []
    try:
        for path, data in outputs.items():
            path.parent.mkdir(parents=True, exist_ok=True)
            temp = path.with_name(path.name + '.' + secrets.token_hex(6) + '.tmp')
            temporary[path] = temp
            with temp.open('wb') as stream:
                stream.write(data)
                stream.flush()
                os.fsync(stream.fileno())
        for path, temp in temporary.items():
            os.replace(temp, path)
            completed.append(path)
    except OSError:
        for path in reversed(completed):
            if previous[path] is None:
                path.unlink(missing_ok=True)
            else:
                path.write_bytes(previous[path])
        raise
    finally:
        for temp in temporary.values():
            temp.unlink(missing_ok=True)


class Conflict(Exception):
    pass


class Store:
    def __init__(self, project, backups):
        self.project = project.resolve()
        self.path = self.project / 'translations/catalog.json'
        self.backups = backups.resolve()
        self.lock = threading.RLock()
        self.token = secrets.token_urlsafe(32)

    def read(self):
        raw = self.path.read_bytes()
        return json.loads(raw), hashlib.sha256(raw).hexdigest()

    def public(self):
        with self.lock:
            catalog, revision = self.read()
            entries = []
            for original in catalog['entries']:
                entry = {k: v for k, v in original.items() if k != 'occurrences'}
                occurrences = original.get('occurrences', [])
                entry['occurrenceCount'] = len(occurrences)
                entry['examples'] = [occurrences[i] for i in sorted({0, len(occurrences)//2, len(occurrences)-1})] if occurrences else []
                entries.append(entry)
            return dict(revision=revision, token=self.token, entries=entries, sections=SECTIONS,
                        project=str(self.project), savedAt=catalog.get('savedAt'), format=1)

    def save(self, revision, changes):
        with self.lock:
            catalog, current = self.read()
            if revision != current:
                raise Conflict('The project changed in another window. Your edits are still here. Reload before saving again.')
            if not isinstance(changes, list) or len(changes) > len(catalog['entries']):
                raise ValueError('Invalid edit list.')
            entries = {e['id']: e for e in catalog['entries']}
            seen = set()
            for change in changes:
                if not isinstance(change, dict):
                    raise ValueError('Invalid translation edit.')
                key = change.get('id')
                if key not in entries or key in seen or set(change) != {'id', 'en', 'km', 'reviewed'} or not isinstance(change['reviewed'], bool):
                    raise ValueError('Invalid translation edit.')
                seen.add(key)
                entries[key].update({field: change[field] for field in ('en', 'km', 'reviewed')})
            catalog['savedAt'] = datetime.now(timezone.utc).isoformat(timespec='seconds')
            raw = (json.dumps(catalog, ensure_ascii=False, indent=2) + '\n').encode('utf-8')
            new_revision = hashlib.sha256(raw).hexdigest()
            outputs = exports(self.project, catalog, new_revision)
            outputs[self.path] = raw  # Source is committed last.
            self.backups.mkdir(parents=True, exist_ok=True)
            backup = self.backups / (datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S') + '-' + current[:12] + '.json')
            if not backup.exists():
                backup.write_bytes(self.path.read_bytes())
            replace_files(outputs)
            return self.public()

    def export(self):
        with self.lock:
            catalog, revision = self.read()
            replace_files(exports(self.project, catalog, revision))


def handler_for(store, static, port):
    class Handler(BaseHTTPRequestHandler):
        def log_message(self, fmt, *args):
            print('%s %s' % (self.log_date_time_string(), fmt % args), flush=True)

        def headers_safe(self):
            host = self.headers.get('Host', '')
            if host not in (f'127.0.0.1:{port}', f'localhost:{port}'):
                return False
            origin = self.headers.get('Origin')
            return origin is None or origin in (f'http://127.0.0.1:{port}', f'http://localhost:{port}')

        def send(self, code, data, mime='application/json; charset=utf-8', attachment=None):
            raw = json.dumps(data, ensure_ascii=False).encode('utf-8') if not isinstance(data, bytes) else data
            self.send_response(code)
            self.send_header('Content-Type', mime)
            self.send_header('Content-Length', str(len(raw)))
            self.send_header('Cache-Control', 'no-store')
            self.send_header('X-Content-Type-Options', 'nosniff')
            self.send_header('Content-Security-Policy', "default-src 'self'; script-src 'self'; style-src 'self'; img-src 'self' data:; frame-ancestors 'none'; base-uri 'none'; form-action 'self'")
            if attachment:
                self.send_header('Content-Disposition', 'attachment; filename="' + attachment + '"')
            self.end_headers()
            self.wfile.write(raw)

        def do_GET(self):
            if not self.headers_safe():
                return self.send(403, {'error': 'Local requests only.'})
            path = urlsplit(self.path).path
            if path == '/api/catalog':
                return self.send(200, store.public())
            if path == '/api/export':
                return self.send(200, store.path.read_bytes(), attachment='khmer-calendar-translations.json')
            files = {'/': ('index.html', 'text/html; charset=utf-8'), '/app.js': ('app.js', 'text/javascript; charset=utf-8'),
                     '/style.css': ('style.css', 'text/css; charset=utf-8')}
            if path not in files:
                return self.send(404, {'error': 'Not found.'})
            name, mime = files[path]
            return self.send(200, (static / name).read_bytes(), mime)

        def do_POST(self):
            if not self.headers_safe() or self.headers.get('X-Translation-Token') != store.token:
                return self.send(403, {'error': 'Reload this local editor before saving.'})
            if urlsplit(self.path).path != '/api/save':
                return self.send(404, {'error': 'Not found.'})
            try:
                length = int(self.headers.get('Content-Length', '0'))
                if not 0 < length <= 4_000_000 or not self.headers.get('Content-Type', '').startswith('application/json'):
                    raise ValueError('Invalid request.')
                body = json.loads(self.rfile.read(length))
                result = store.save(body['revision'], body['changes'])
                self.send(200, result)
            except Conflict as error:
                self.send(409, {'error': str(error)})
            except (ValueError, KeyError, TypeError, UnicodeError) as error:
                self.send(400, {'error': str(error)})
            except OSError:
                self.send(500, {'error': 'Could not write project files. Changes remain in the editor; check folder permissions and try again.'})
    return Handler


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--project', type=Path)
    parser.add_argument('--port', type=int, default=8766)
    parser.add_argument('--export', action='store_true')
    args = parser.parse_args()
    here = Path(__file__).resolve().parent
    config = json.loads((here / 'config.json').read_text(encoding='utf-8')) if (here / 'config.json').exists() else {}
    project = args.project or Path(config.get('project', str(here.parents[1])))
    store = Store(project, here / 'backups')
    if args.export:
        store.export()
        print('Android translation resources exported.', flush=True)
        return
    store.public()  # Fail clearly before starting if the catalog is unavailable.
    server = ThreadingHTTPServer(('127.0.0.1', args.port), handler_for(store, here / 'static', args.port))
    print(f'Translation editor: http://127.0.0.1:{args.port}', flush=True)
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        pass
    finally:
        server.server_close()


if __name__ == '__main__':
    main()
