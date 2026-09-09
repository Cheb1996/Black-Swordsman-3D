# Project workflow

The canonical project is https://github.com/Cheb1996/Black-Swordsman-3D.
The user requested that all future development happen in a checkout of this
repository. Start from its current Git history, inspect `git status` and fetch
remote updates before editing. Preserve unrelated user changes. Do not continue
development in old scratch exports or Library ZIPs, and do not mirror repository
files back to Library. Commit the completed work; follow the user's instructions
for pushing branches and publishing builds. Never force-push shared history.

## Layout and validation

- `app/src/main`: Java source, Android resources, generated assets and manifest.
- `tests`, `core-sources.txt`, `test.sh`: Android-independent simulation tests.
- `toolchain`, `toolchain.lock.json`: pinned Linux x86_64 build dependencies and notices.
- `build.sh`: offline debug or release APK build; Java 17 and Python 3 are required.
- `releases/v8.0`: the originally delivered signed APK and its verified checksum.
- `verification`: recorded v8 validation and genuine v6/v7 save migration fixtures.

Use `./test.sh` for core gameplay changes, or the affected named smoke tests for
bounded changes. Use `./build.sh debug` to validate Android compilation and asset
baking without a private key. Verify a new APK with `tools/verify_apk.py`; debug
builds require `--debug`. Keep Java heap limits in the scripts.

Do not claim that host tests or mesh previews prove Android/GLES runtime behavior,
FPS, audio, sensors or thermal performance. Record actual device testing separately.
Preserve save migration, package name and release signing compatibility. Changes
to generated meshes/materials must originate in their generators and be rebaked.

## Signing and releases

This repository is public. Never commit signing keys, passwords, `.signing.local`,
credentials, old source ZIPs containing keys, or generated build directories.
The existing release key remains local under ignored `keystore/`; configuration
is loaded from ignored `.signing.local` or environment variables. Release builds
must retain the certificate recorded in `releases/v8.0/package.json`. A debug APK
uses a separate development key and cannot update a release installation.

Do not overwrite historical APKs or validation reports. Put new release APKs in
a new version directory, record SHA-256 and verification, and link to GitHub from
the README so downloads do not depend on temporary workspace paths.
