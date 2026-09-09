#!/usr/bin/env python3
from pathlib import Path
import hashlib,json,sys
root=Path(__file__).resolve().parents[1]
kit=Path(sys.argv[1])
lock=json.loads((root/'toolchain.lock.json').read_text())
for name,digest in lock['files'].items():
 f=kit/name
 # License files are stored separately in the bundled kit, not required in external kits.
 if name.startswith('licenses/'):continue
 if not f.exists() or hashlib.sha256(f.read_bytes()).hexdigest()!=digest:raise SystemExit('Toolchain mismatch: '+name)
print('Pinned build dependencies verified')
