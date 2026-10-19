# RESUME – read this first after a reset

*Dev notes are English only (not user-facing).*

## State (2026-10-19)
- Phase: **proposal + mockups, waiting for user approval.** No app code yet.
- Proposal: `docs/en/PROPOSAL.md` / `docs/de/VORSCHLAG.md`.
- Mockups: `docs/mockups/*.png`, rendered by `python3 tools/mockups/render.py [m1 m2 m3 m4]` (~70 s for all; Pillow, fonts in `tools/mockups/fonts`).

## Next step after approval (M0)
1. Gradle scaffold (Kotlin 2.x, Compose, minSdk 26, targetSdk 35), theme (glass, Manrope, brand gradient), EN+DE strings.
2. CI `.github/workflows/ci.yml` per proposal §8 (changes → check ∥ assemble → "CI result"; release on tags).
3. Release keystore → repo secrets (`ANDROID_KEYSTORE_B64`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`, `ANDROID_KEY_PASSWORD`); backup to user.
4. Updater (GitHub Releases, sha256, PackageInstaller); tag v0.1.0.

## Rules
- Commit + push after every step. No local Gradle builds; CI is the compiler. Wait with `tools/ci-wait.sh <run>` in background, read only failing logs.
- livelooper rules apply: EN+DE everywhere user-facing, never name modelled hardware/brands, every function MIDI-mappable (= host param ID), param IDs append-only, local livelooper builds only via `flock /workspace/work/ohpe-build.lock` -j1.
- Re-create workspace: `tools/bootstrap.sh`.
