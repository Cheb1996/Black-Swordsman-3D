# Project workflow

The canonical project is https://github.com/Cheb1996/Black-Swordsman-3D.
The user's instruction on 2026-09-25 designates the current development
conversation as the primary project chat and restores repository-based
development. It supersedes the internal-only workflow from 2026-09-10.

Develop only in a checkout of this repository. Inspect git status, verify
origin, and fetch remote updates before editing. Preserve unrelated work.
Commit completed changes and synchronize through an authorized Git connection
or the connected GitHub API. Respect branch protections. Never force-push
shared history or use CI credentials to work around missing local Git access.
Do not mirror repository-backed files back to Library.

The completed v14 release has source commit
43bcc9513d1909102efb6946fbc9fde4ef3274f5. Its original v9-v14 history still
needs to be imported before new game development starts: main currently
contains the earlier v8 game. Preserve that completed work when reconciling
the histories. Do not mistake these workflow edits for publication of v14.
See PROJECT_WORKFLOW_RU.md for the handoff and import status.

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
