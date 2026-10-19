# livelooper control – technischer Vorschlag (Entwurf, 19.10.2026)

*English: [../en/PROPOSAL.md](../en/PROPOSAL.md)*

livelooper control ist eine Android-App, die Smartphone oder Tablet zu einer erweiterten Touch-Oberfläche mit niedriger Latenz für das livelooper-Plug-in bzw. die Standalone-App macht. Später kann daraus ein eigenständiger Looper werden. Nur Android (Querformat, Layouts für Phone + Tablet). Mockups: [`docs/mockups`](../mockups).

## 1. Stack – Kotlin + Jetpack Compose (Empfehlung)

| | Kotlin + Compose | Flutter |
|---|---|---|
| Latenz Touch → Netzwerk | direkt, keine Brücke | Dart ↔ Platform Channel für USB/NSD/Sockets |
| USB, NSD (mDNS), PackageInstaller, Keystore | native APIs | Plugins oder eigene Channels |
| Eigene Grafik (Wellenformen, XY, Meter) | Compose `Canvas`, 90/120 Hz | Impeller, ebenfalls gut |
| iOS später | nein (nicht nötig: nur Android) | ja |
| APK-Größe | ~6–8 MB | ~15–20 MB |

**Vorschlag:** Kotlin 2.x, Jetpack Compose (Material 3 nur als Basis, eigenes Glas-Theme), Coroutines, minSdk 26 (Android 8), targetSdk 35. Ohne iOS zählt Flutters Hauptvorteil nicht.

## 2. Verbindung

Ein Codepfad: Alles ist TCP/UDP zu einem kleinen Server in livelooper. Die USB-Varianten unterscheiden sich nur darin, wie die IP-Verbindung entsteht.

| Modus | Wie | Aufwand für Nutzer | Typische RTT |
|---|---|---|---|
| **USB-Netzwerk (Standard)** | USB-Tethering (NCM/RNDIS) – das Phone wird Netzwerkadapter des Rechners | Kabel + Schalter „USB-Tethering" | 0,5–2 ms |
| USB per adb reverse | `adb reverse tcp:PORT tcp:PORT` | Entwicklermodus, adb | 1–3 ms |
| WLAN | gleiches Netz, Auto-Discovery | keiner | 3–15 ms (Jitter) |
| AOA (Android Open Accessory) | rohe USB-Bulk-Transfers, libusb im Plug-in | Treiber unter Windows | ~1 ms |

**Vorschlag:** USB-Netzwerk + WLAN in v1, adb reverse als Option für Fortgeschrittene. AOA nur, wenn Messungen es verlangen (mehr Code im Plug-in, Treiberärger unter Windows, praktisch kein Gewinn gegenüber USB-Netzwerk).

**Discovery:** DNS-SD `_livelooper._tcp` (Android `NsdManager`; livelooper-seitig kleiner mDNS-Responder oder OS-Dienst) + UDP-Broadcast-Beacon als Fallback + manuelle IP. Jede Plug-in-Instanz meldet eigenen Namen („livelooper – Studio-PC – Track 3 Bus") und Port.

## 3. Latenzziele

| Pfad | USB | WLAN |
|---|---|---|
| Touch → Parameter im Audio-Thread angewendet (p95, ohne Audiopuffer) | < 5 ms | < 15 ms |
| Zustandsänderung → sichtbar in der App | < 1 Frame (16 ms) | < 2 Frames |
| Update-Rate Playhead / Meter | 60 Hz | 30–60 Hz |

Laufend gemessen: PING/PONG im Protokoll, angezeigt in der Kopfzeile („USB · 2.8 ms"), grün/gelb/orange.
App-seitig: Compose-Pointer-Input ohne Recomposition pro Event, hohe Bildwiederholrate, `TCP_NODELAY`; kontinuierliche Werte werden pro Frame zusammengefasst, Trigger (REC/PLAY) gehen sofort raus.

## 4. Protokoll (v1)

- **Steuerkanal:** eine TCP-Verbindung, `TCP_NODELAY`, binäre Frames mit Längenpräfix: `u16 Länge | u8 Typ | Nutzdaten` (Little Endian).
- **Schneller Kanal (optional, UDP):** kontinuierliche Werte (XY, Fader) und Telemetrie (Playhead, Meter) mit Sequenznummern; Verluste egal, der neueste Wert gewinnt.
- **Adressierung = livelooper-Host-Parameter-IDs** (u16, nur anhängen → für immer stabil). Jedes Touch-Element ist ein Parameter – damit bleibt alles MIDI-mappbar und automatisierbar; die App braucht keine fest eingebaute Tabelle.
- Nachrichten: `HELLO/WELCOME`, `PARAM_TABLE` (ID, Namen EN+DE, Bereich, Auswahlwerte – einmal gesendet, per Tabellen-Hash gecacht), `PARAM_SET` (ID, normierter Float), `GESTURE_BEGIN/END` (Host-Automation), `TRIGGER` (ID, für Taster), `STATE` (Zustand pro Track, Loop-Länge, Position), `WAVE` (verkleinerte Peak-Übersicht, nur bei Änderung), `TICK` (Takt/Schlag/Playhead), `METERS`, `PING/PONG`, `ERROR`.
- **Versionierung/Handshake:** `HELLO {protoMajor, protoMinor, appVersion, deviceId, caps}` → `WELCOME {protoMajor, protoMinor, livelooperVersion, instanceName, paramTableHash, caps}`. Abweichende Major-Version → saubere Ablehnung mit „bitte App / livelooper aktualisieren"; Minor → Funktionen über `caps`-Bits ausgehandelt.
- **Spezifikation + Golden-Fixtures** liegen in diesem Repo (`protocol/`); App-Tests und livelooper-Tests dekodieren dieselben Bytes.

## 5. livelooper-Seite (Plug-in)

- Neues Modul `src/remote/`: `RemoteServer` in eigenem Thread (JUCE-Sockets), **greift nie direkt in den Audio-Thread**: Parameteränderungen laufen über die vorhandenen Parameter-Objekte (thread-sichere Atomics); Telemetrie kommt aus Atomics bzw. einem lock-freien FIFO, das der Audio-Thread ohne Locks und ohne Allokation füllt.
- Standardmäßig aus; Einstellung *Remote* (EN+DE) mit An/Aus, Port, Kopplungscode, Liste gekoppelter Geräte (widerrufbar). Keine neuen Host-Parameter nötig.
- Unit-Tests: Protokoll-Kodierung mit den gemeinsamen Fixtures, Handshake, Auth, Test „hängender Server blockiert processBlock nicht".
- Branch-Vorschlag: `remote-wip` auf Basis von `v1.7-wip`.

## 6. Sicherheit / Kopplung

- livelooper zeigt einen 6-stelligen Code (Einstellungen → Remote). Die App sendet ihn einmal; der Server gibt einen zufälligen 32-Byte-Geräteschlüssel zurück. Die App speichert ihn im Android Keystore, livelooper führt eine widerrufbare Geräteliste.
- Bei jeder Verbindung: Challenge-Response mit HMAC-SHA256 über eine Server-Nonce. Ohne Auth wird nur `HELLO` beantwortet.
- Der Server lauscht nur bei aktiviertem Remote und nur auf privaten/Link-Local-Adressen. Transportverschlüsselung (z. B. Noise) später optional; v1 überträgt außer dem Auth-Austausch keine Geheimnisse.

## 7. In-App-Updater (von Anfang an)

- Prüft die GitHub Releases von `thegroovex/Livelooper-control` (`/releases/latest`; Kanal *Beta* inkl. Pre-Releases) beim Start, höchstens alle 24 h, plus Button.
- Lädt APK + `.sha256`, prüft den Hash, installiert per `PackageInstaller`-Session (braucht `REQUEST_INSTALL_PACKAGES` und einmalig „Unbekannte Apps installieren"). Android erzwingt selbst denselben Signaturschlüssel.
- Kein Play Store (Play verbietet Selbst-Updates); F-Droid später optional.
- **Signaturschlüssel:** PKCS12-Keystore (AES-256) liegt in `signing/`, sein Zufallspasswort nur im GitHub-Secret `ANDROID_KEYSTORE_PASSWORD`; du bekommst eine Offline-Sicherung. Geht er verloren, müssen Nutzer neu installieren.

## 8. CI & Builds (Muster wie OHPE / livelooper)

- GitHub Actions ist der Compiler: keine lokalen Gradle-/Android-SDK-Builds in der Sandbox (2 Kerne, häufige Resets).
- Jobs: `changes` (Pfadfilter: nur Doku/Mockups → überspringen) → `check` (Lint + Unit-Tests) ∥ `assemble` (Debug-APK) → ein Gate-Job **CI result** (einziger Pflicht-Check).
- `gradle/actions/setup-gradle` mit Dependency- + Build-Cache und Configuration Cache; JDK 17; Concurrency `ci-${ref}` mit Abbruch alter Läufe; `[ci full]` / manueller Lauf erzwingt alles.
- Debug-APK als Artefakt (3 Tage). **Tags `v*`** → signierte Release-APK + `.sha256` + Notes EN/DE als GitHub Release (das liest der Updater).
- Warten: ein Hintergrund-Helfer (`tools/ci-wait.sh <run>`), bei Fehlern nur das Log-Ende des fehlgeschlagenen Jobs → weniger Tokens, kein Polling im Chat.

## 9. Reset-sicherer Ablauf

- Das Repo ist die einzige Wahrheit; nach jedem Schritt Commit + Push.
- `docs/dev/RESUME.md`: aktueller Stand, nächster Schritt, offene Entscheidungen – die erste Datei, die eine neue Sitzung liest.
- `tools/bootstrap.sh` stellt den Workspace wieder her (beide Repos klonen, Git-Identität, Python-Pakete für Mockups). Mockup-Renderer + Schriften liegen im Repo (`tools/mockups`).

## 10. Samsung DeX & externe Touch-Monitore (Wunsch 19.10.2026, S25 Ultra)

- Frei skalierbares Fenster (`resizeableActivity`, keine Orientierungssperre außerhalb des Phone-Displays), DeX-Keep-Alive-Metadaten, kein Neustart beim Wechsel zwischen Bildschirmen (`configChanges` inkl. density).
- Layout nach Fenstergröße: Phone-Querformat vs. großes Layout (≥ 1000 × 600 dp) für Tablets, DeX-Fenster und externe Touch-Monitore.
- Alle Bedienelemente reagieren beim Berühren und sind multitouchfähig (jeder Finger gehört seinem Element) – ein Multitouch-Monitor unter DeX funktioniert wie ein Tablet.
- M2: Zwei-Bildschirm-Modus – Phone als Controller (FX/XY oder Pads), externer Monitor als volle LIVE/MIX-Ansicht (`Presentation`-API / zweite Activity auf dem Zweitbildschirm); Maus-Hover + Tastenkürzel unter DeX.

## 11. Meilensteine

1. **M0** – App-Gerüst, Theme, CI, Release-Signatur, Updater; Release 0.1.0 zum Testen des Update-Wegs.
2. **M1** – Protokoll-Spezifikation + Fixtures, `RemoteServer` in livelooper, USB-Netzwerk + WLAN-Discovery, Kopplung, LIVE-Ansicht.
3. **M2** – MIX (Fader, Pan, Sends, Loop-Länge, Szenen) und FX/XY (Multitouch), Tablet-Layout.
4. **M3** – Feinschliff: Latenzanzeige, Wiederverbinden, EN/DE-Review, Test auf deinen Geräten.
5. **Später** – eigenständiger Looper unter Android (Oboe-Audio), AOA falls nötig.
