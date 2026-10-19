# RESUME – read this first after a reset

*Dev notes are English only (not user-facing).*

## State (2026-10-19)
- Proposal approved by user 2026-10-19 (+ Samsung DeX / external multitouch monitor support, §10).
- M0 code pushed (5fd13b3): Gradle 8.11.1 wrapper, AGP 8.7.3, Kotlin 2.0.21, Compose BOM 2024.12.01; LIVE demo screen, SETUP with updater, DeX manifest/orientation logic, EN+DE strings, VersionTest; CI `.github/workflows/ci.yml`; CHANGELOG.md (release notes source, EN+DE per version).
- Signing: `signing/release.p12` (PKCS12, alias livelooper-control, SHA-256 cert fingerprint A9:5A:62:E2:…:D2:8C). Password only in secret ANDROID_KEYSTORE_PASSWORD – the GitHub integration cannot write secrets, the USER adds it once in repo settings. Backup was handed to the user.

## Next
1. CI green on main → tag v0.1.0 (needs the secret) → user installs APK once manually, then updates come in-app.
2. M1: `protocol/` spec + golden fixtures, livelooper `src/remote/` RemoteServer on branch remote-wip, discovery, pairing, LIVE bound to real state.

## Rules
- Commit + push after every step. No local Gradle builds; CI is the compiler. Wait with `tools/ci-wait.sh <run>` in background, read only failing logs.
- livelooper rules apply: EN+DE everywhere user-facing, never name modelled hardware/brands, every function MIDI-mappable (= host param ID), param IDs append-only, local livelooper builds only via `flock /workspace/work/ohpe-build.lock` -j1.
- Re-create workspace: `tools/bootstrap.sh`.
