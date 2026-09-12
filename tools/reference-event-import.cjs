// Copyright (c) 2026 RSG-KH | Apache-2.0 License
// Local-only receiver for public calendar observations collected through browser UI.
// Run: node tools/reference-event-import.cjs
const http = require('node:http');
const fs = require('node:fs');
const path = require('node:path');
const output = path.resolve(__dirname, '../artifacts/reference-events');
fs.mkdirSync(output, {recursive: true});
const target = path.join(output, 'site-observed-months.json');
const byMonth = new Map(fs.existsSync(target) ? JSON.parse(fs.readFileSync(target, 'utf8')).months.map(m => [m.month, m]) : []);
http.createServer(async (req, res) => {
  if (req.method === 'GET' && req.url === '/') {
    res.setHeader('Content-Type', 'text/html; charset=utf-8');
    return res.end('<!doctype html><html><meta charset="utf-8"><title>Local reference import</title><h1>Local reference import</h1><form method="post" action="/capture"><label>Public calendar observations<textarea name="payload" rows="10" cols="80"></textarea></label><button>Save observations</button></form></html>');
  }
  if (req.method !== 'POST' || req.url !== '/capture') { res.writeHead(404); return res.end(); }
  try {
    let body = '';
    for await (const chunk of req) { body += chunk; if (body.length > 8 * 1024 * 1024) throw Error('Import too large'); }
    const months = JSON.parse(new URLSearchParams(body).get('payload'));
    if (!Array.isArray(months)) throw Error('Expected month array');
    for (const m of months) {
      if (!/^20(?:0\d|1\d|2\d|30)-(?:0[1-9]|1[0-2])$/.test(m.month) || !Array.isArray(m.days) || m.days.length < 28 || m.days.length > 31) throw Error('Invalid month');
      if (m.days.some(d => !d.date.startsWith(m.month + '-') || !Array.isArray(d.events))) throw Error('Invalid day');
      byMonth.set(m.month, m);
    }
    const data = {source: 'https://khmer-lunar-calendar.com/', capturedAt: new Date().toISOString(), method: 'Public rendered calendar, date labels and event names only', months: [...byMonth.values()].sort((a,b) => a.month.localeCompare(b.month))};
    fs.writeFileSync(target + '.tmp', JSON.stringify(data, null, 2));
    fs.renameSync(target + '.tmp', target);
    res.setHeader('Content-Type', 'text/html; charset=utf-8');
    res.end('<!doctype html><html><meta charset="utf-8"><title>Saved observations</title><h1>Saved ' + byMonth.size + ' months</h1><a href="/">Import another batch</a></html>');
    console.log('Saved ' + byMonth.size + ' months');
  } catch (error) { res.writeHead(400, {'Content-Type':'text/plain'}); res.end(error.message); }
}).listen(8765, '127.0.0.1', () => console.log('Local reference import: http://127.0.0.1:8765'));
