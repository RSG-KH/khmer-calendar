// Copyright (c) 2026 RSG-KH | Apache-2.0 License
// Development only. No JavaScript engine or downloaded package ships in the APK.
// These fixtures detect differences from an upstream implementation; agreement
// between related algorithms is not independent historical certification.
const fs = require('node:fs');
const path = require('node:path');
const crypto = require('node:crypto');
const vm = require('node:vm');
const commit = 'ff2bfd558385bd5403780b671fd2eca43b3e2228';
const sha = 'c29c9307e2b83410823fbce85ce54dd3bdea0425ff17ec919f7bba779fb9dcac';
async function main() {
  const source = process.argv[2] ? fs.readFileSync(process.argv[2], 'utf8') :
    await (await fetch(`https://raw.githubusercontent.com/ThyrithSor/momentkh/${commit}/momentkh.js`)).text();
  if (crypto.createHash('sha256').update(source).digest('hex') !== sha) throw new Error('Reference source checksum changed');
  const context = { module: { exports: {} } };
  vm.runInNewContext(source, context);
  const reference = context.module.exports;
  const lines = [`# MomentKH ${commit}; year|SHA256 of daily date,day,phase,month,BE lines|New Year start`];
  for (let year = 1900; year <= 2100; year++) {
    const hash = crypto.createHash('sha256');
    for (let date = new Date(Date.UTC(year, 0, 1)); date.getUTCFullYear() === year; date.setUTCDate(date.getUTCDate() + 1)) {
      const lunar = reference.fromGregorian(year, date.getUTCMonth() + 1, date.getUTCDate()).khmer;
      hash.update(`${date.toISOString().slice(0, 10)},${lunar.day},${lunar.moonPhase},${lunar.monthIndex},${lunar.beYear}\n`);
    }
    const ny = reference.getNewYear(year);
    lines.push(`${year}|${hash.digest('hex')}|${year}-${String(ny.month).padStart(2, '0')}-${String(ny.day).padStart(2, '0')}`);
  }
  const output = path.join(__dirname, '../app/src/test/resources/momentkh-reference.txt');
  fs.mkdirSync(path.dirname(output), { recursive: true });
  fs.writeFileSync(output, lines.join('\n') + '\n');
  console.log(`Wrote 201 yearly reference checksums and New Year dates to ${output}`);
}
main().catch(error => { console.error(error); process.exitCode = 1; });
