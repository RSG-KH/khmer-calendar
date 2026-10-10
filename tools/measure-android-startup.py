#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Record process-cold Android startup samples without clearing installed app data."""

import argparse
import hashlib
import json
import math
import re
import statistics
import subprocess
import time
from datetime import datetime, timezone
from pathlib import Path


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--adb', default='adb', help='Path to adb')
    parser.add_argument('--serial', required=True, help='Explicit device/emulator serial')
    parser.add_argument('--output', type=Path, required=True, help='New JSON results file')
    parser.add_argument('--apk', type=Path, required=True, help='The APK already installed, for its checksum')
    parser.add_argument('--iterations', type=int, default=15)
    parser.add_argument('--warmups', type=int, default=2)
    args = parser.parse_args()
    if args.iterations < 5 or args.warmups < 1:
        parser.error('Use at least five measured starts and one warmup')
    if args.output.exists():
        parser.error('Output already exists; choose a new filename to preserve earlier measurements')
    if not args.apk.is_file():
        parser.error('APK does not exist')

    def adb(*command):
        result = subprocess.run([args.adb, '-s', args.serial, *command],
                                capture_output=True, text=True, encoding='utf-8',
                                errors='replace', timeout=60, check=True)
        return result.stdout.strip()

    package = 'com.rsgkh.calendar'
    component = package + '/.MainActivity'
    if adb('get-state') != 'device':
        raise RuntimeError('Selected Android device is unavailable')
    installed = adb('shell', 'pm', 'path', package)
    if not installed.startswith('package:'):
        raise RuntimeError('Install the APK on the selected device before measuring')
    apk_sha256 = hashlib.sha256(args.apk.read_bytes()).hexdigest()
    installed_base = next((line.removeprefix('package:') for line in installed.splitlines()
                           if line.endswith('/base.apk')), None)
    if not installed_base:
        raise RuntimeError('Could not locate installed base APK')
    installed_sha256 = adb('shell', 'sha256sum', installed_base).split()[0]
    if installed_sha256 != apk_sha256:
        raise RuntimeError('Installed APK differs from --apk; refusing a mislabeled measurement')
    metadata = {
        'recorded_at_utc': datetime.now(timezone.utc).isoformat(),
        'serial': args.serial,
        'fingerprint': adb('shell', 'getprop', 'ro.build.fingerprint'),
        'api': adb('shell', 'getprop', 'ro.build.version.sdk'),
        'emulator': adb('shell', 'getprop', 'ro.kernel.qemu') == '1',
        'display': adb('shell', 'wm', 'size'),
        'density': adb('shell', 'wm', 'density'),
        'apk': str(args.apk.resolve()),
        'apk_sha256': apk_sha256,
        'warmups': args.warmups,
        'method': 'am force-stop followed by am start -W; warm filesystem caches; app data preserved',
        'limitations': 'First display, not fully drawn. Emulator/host load affects results. '
                       'No frame-time, battery, or retained-object analysis.',
    }
    samples = []
    raw_starts = []
    # Initial launch warms package/filesystem caches and allows first-use work to settle.
    for index in range(args.warmups + args.iterations):
        adb('shell', 'am', 'force-stop', package)
        output = adb('shell', 'am', 'start', '-W', '-n', component)
        if not re.search(r'^Status: ok$', output, re.MULTILINE):
            raise RuntimeError('Launch failed: ' + output)
        fields = {key: int(value) for key, value in
                  re.findall(r'^(ThisTime|TotalTime|WaitTime): (\d+)$', output, re.MULTILINE)}
        if fields.get('TotalTime', 0) <= 0 or 'COLD' not in output:
            raise RuntimeError('Expected a process-cold timed launch: ' + output)
        time.sleep(2)
        if index >= args.warmups:
            samples.append(fields)
            raw_starts.append(output)
            print(f'Sample {len(samples)}/{args.iterations}: {fields["TotalTime"]} ms', flush=True)
    durations = sorted(sample['TotalTime'] for sample in samples)
    results = dict(metadata, samples=samples, raw_starts=raw_starts,
                   median_ms=statistics.median(durations),
                   p90_ms=durations[math.ceil(0.9 * len(durations)) - 1],
                   min_ms=min(durations), max_ms=max(durations),
                   final_meminfo=adb('shell', 'dumpsys', 'meminfo', package))
    args.output.parent.mkdir(parents=True, exist_ok=True)
    with args.output.open('x', encoding='utf-8') as stream:
        json.dump(results, stream, indent=2)
        stream.write('\n')
    print(f'Median {results["median_ms"]} ms; p90 {results["p90_ms"]} ms. Saved {args.output}')


if __name__ == '__main__':
    main()
