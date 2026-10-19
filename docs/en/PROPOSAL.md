# livelooper control – technical proposal (draft, 2026-10-19)

*Deutsch: [../de/VORSCHLAG.md](../de/VORSCHLAG.md)*

livelooper control is an Android app that turns a phone or tablet into an extended, low-latency touch surface for the livelooper plug-in / standalone app. Later it may grow into a standalone looper. Android only (landscape, phone + tablet layouts). Mockups: [`docs/mockups`](../mockups).

## 1. Stack – Kotlin + Jetpack Compose (recommended)

| | Kotlin + Compose | Flutter |
|---|---|---|
| Touch → network latency | direct, no bridge | Dart ↔ platform channel for USB/NSD/sockets |
| USB, NSD (mDNS), PackageInstaller, Keystore | first-class APIs | plugins or own channels |
| Custom drawing (waveforms, XY, meters) | Compose `Canvas`, 90/120 Hz | Impeller, also fine |
| iOS later | no (not needed: Android only) | yes |
| APK size | ~6–8 MB | ~15–20 MB |

**Decision proposal:** Kotlin 2.x, Jetpack Compose (Material 3 only as base, own glass theme), coroutines, minSdk 26 (Android 8), targetSdk 35. No iOS → Flutter's main advantage does not count.

## 2. Transport

One code path: everything is TCP/UDP to a small server inside livelooper. The "USB" options only differ in how the IP link is made.

| Mode | How | Setup for the user | Typical RTT |
|---|---|---|---|
| **USB network (default)** | USB tethering (NCM/RNDIS) – the phone becomes a network adapter of the computer | cable + toggle "USB tethering" | 0.5–2 ms |
| USB via adb reverse | `adb reverse tcp:PORT tcp:PORT` | developer mode, adb | 1–3 ms |
| WLAN | same LAN, auto discovery | none | 3–15 ms (jitter) |
| AOA (Android Open Accessory) | raw USB bulk transfer, libusb in the plug-in | drivers on Windows | ~1 ms |

**Proposal:** USB network + WLAN in v1, adb reverse as power-user option. AOA only if measurements show we need it (more code inside a plug-in, driver trouble on Windows, no gain over USB networking in practice).

**Discovery:** DNS-SD `_livelooper._tcp` (Android `NsdManager`; JUCE side via a tiny mDNS responder or OS service) + UDP broadcast beacon as fallback + manual IP. Each plug-in instance announces its own name ("livelooper – Studio-PC – Track 3 Bus") and port.

## 3. Latency targets

| Path | USB | WLAN |
|---|---|---|
| touch → parameter applied in the audio thread (p95, excl. audio buffer) | < 5 ms | < 15 ms |
| state change → visible on the app | < 1 frame (16 ms) | < 2 frames |
| playhead / meters update rate | 60 Hz | 30–60 Hz |

Measured continuously: protocol PING/PONG, shown in the header pill ("USB · 2.8 ms"), green/yellow/orange.
App side: Compose pointer input without recomposition per event, high refresh rate, `TCP_NODELAY`, sends coalesced per frame except triggers (REC/PLAY), which go out immediately.

## 4. Protocol (v1)

- **Control channel:** one TCP connection, `TCP_NODELAY`, length-prefixed binary frames: `u16 length | u8 type | payload` (little endian).
- **Fast channel (optional, UDP):** continuous values (XY, faders) and server telemetry (playhead, meters) with sequence numbers; loss is fine, latest wins.
- **Addressing = livelooper host parameter IDs** (u16, append-only → stable forever). Every touch control is a parameter, so everything stays MIDI-mappable and host-automatable; the app needs no hard-coded table.
- Messages: `HELLO/WELCOME`, `PARAM_TABLE` (id, names EN+DE, range, choices – sent once, cached by table hash), `PARAM_SET` (id, float normalized), `GESTURE_BEGIN/END` (host automation), `TRIGGER` (id, for momentary buttons), `STATE` (per-track state, loop length, position), `WAVE` (downsampled peak overview, on change only), `TICK` (bar/beat/playhead), `METERS`, `PING/PONG`, `ERROR`.
- **Versioning/handshake:** `HELLO {protoMajor, protoMinor, appVersion, deviceId, caps}` → `WELCOME {protoMajor, protoMinor, livelooperVersion, instanceName, paramTableHash, caps}`. Major mismatch → clean refusal with "please update app / livelooper"; minor → features negotiated via `caps` bits.
- **Spec + golden fixtures** live in this repo (`protocol/`); both the app tests and the livelooper tests decode the same fixture bytes.

## 5. livelooper side (plug-in)

- New module `src/remote/`: `RemoteServer` on its own thread (JUCE sockets), **never touches the audio thread directly**: parameter changes go through the existing parameter objects (thread-safe atomics); telemetry is read from atomics / a lock-free FIFO filled by the audio thread without locks or allocation.
- Off by default; setting *Remote* (EN+DE) with on/off, port, pairing code, list of paired devices (revoke). No new host parameters needed.
- Unit tests: protocol encode/decode with the shared fixtures, handshake, auth, "server stall must not block processBlock" test.
- Branch proposal: `remote-wip` off `v1.7-wip`.

## 6. Security / pairing

- livelooper shows a 6-digit code (Settings → Remote). The app sends it once; the server returns a random 32-byte device key. App stores it in the Android Keystore; livelooper keeps a revocable device list.
- Every connect: challenge–response with HMAC-SHA256 over a server nonce. No auth → only `HELLO` is answered.
- Server listens only while Remote is on, only on private/link-local addresses. Transport encryption (e.g. Noise) is a later option; v1 carries no secrets besides the auth exchange.

## 7. In-app updater (from day one)

- Check GitHub Releases of `thegroovex/Livelooper-control` (`/releases/latest`; channel *Beta* includes pre-releases) on start, at most once per 24 h, plus a manual button.
- Download the APK asset + `.sha256`, verify the hash, install via `PackageInstaller` session (needs `REQUEST_INSTALL_PACKAGES` and one-time "install unknown apps" permission). Android itself enforces the same signing key.
- Not distributed via Play Store (Play forbids self-updating); optional F-Droid later.
- **Signing key:** generated once, stored as GitHub Actions secrets; you get an offline backup copy. Losing it means users must reinstall.

## 8. CI & builds (same pattern as OHPE / livelooper)

- GitHub Actions is the compiler: no local Gradle/Android SDK builds in the sandbox (2 cores, frequent resets).
- Jobs: `changes` (path filter: docs/mockup-only → skip) → `check` (lint + unit tests) ∥ `assemble` (debug APK) → one gate job **CI result** (single required check).
- `gradle/actions/setup-gradle` with dependency + build cache and configuration cache; JDK 17; concurrency `ci-${ref}` with cancel-in-progress; `[ci full]` / manual run forces everything.
- Debug APK as artifact (retention 3 days). **Tags `v*`** → signed release APK + `.sha256` + EN/DE notes uploaded to a GitHub Release (this is what the updater reads).
- Waiting: one background helper (`tools/ci-wait.sh <run>`), on failure only the failing job's log tail is read → fewer tokens, no polling in chat.

## 9. Reset-proof workflow

- Repo is the single source of truth; commit + push after every step.
- `docs/dev/RESUME.md`: current state, next step, open decisions – the first file a fresh session reads.
- `tools/bootstrap.sh` recreates the workspace (clone both repos, git identity, Python deps for mockups). Mockup renderer + fonts are in the repo (`tools/mockups`).

## 10. Milestones

1. **M0** – app scaffold, theme, CI, release signing, updater; release 0.1.0 to test the update path.
2. **M1** – protocol spec + fixtures, `RemoteServer` in livelooper, USB network + WLAN discovery, pairing, LIVE screen.
3. **M2** – MIX (faders, pan, sends, loop length, scenes) and FX/XY (multi-touch), tablet layout.
4. **M3** – polish: latency display, reconnection, EN/DE review, user test on your devices.
5. **Later** – standalone looper on Android (Oboe audio), AOA if needed.
