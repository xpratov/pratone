# Pratone

A minimalist, dark-mode, voice-controlled Android music player. Say **"Hey Pratone"** followed by
a short command ("next", "pause", "play", "previous") to control playback — including with the
screen off and the phone locked, subject to the Android platform limitations documented below.

---

## 1. Opening the project in Android Studio

1. Install **Android Studio (Ladybug or newer)** with Android SDK 35 and JDK 17.
2. `File → Open`, select the `Pratone/` project root (the folder containing `settings.gradle.kts`).
3. Let Gradle sync. If prompted to update the Gradle wrapper, accept — the wrapper's version
   metadata is included (`gradle/wrapper/gradle-wrapper.properties`, Gradle 8.7), but the wrapper
   `.jar` binary itself is not checked in here; running any `./gradlew` command, or opening the
   project in Android Studio and letting it sync, regenerates it automatically. Alternatively run
   `gradle wrapper` once if you have a system-wide Gradle install.

## 2. Building the APK

- **Debug build (fastest, for testing on your own phone):**
  ```
  ./gradlew assembleDebug
  ```
  Output: `app/build/outputs/apk/debug/app-debug.apk`

- **From Android Studio:** `Build → Build Bundle(s)/APK(s) → Build APK(s)`, or just click Run ▶
  with a device connected.

- **Release build** requires you to configure signing (not included, since it needs your own
  keystore):
  ```
  ./gradlew assembleRelease
  ```

## 3. Installing on a physical device

1. On your phone: **Settings → About phone → tap "Build number" 7 times** to enable Developer
   Options, then **Settings → Developer options → USB debugging → on**.
2. Connect via USB, accept the "Allow USB debugging?" prompt.
3. In Android Studio, select your device from the device dropdown and click Run ▶ — or manually:
   ```
   adb install app/build/outputs/apk/debug/app-debug.apk
   ```

## 4. Required permissions

| Permission | Why | When requested |
|---|---|---|
| `READ_MEDIA_AUDIO` (API 33+) / `READ_EXTERNAL_STORAGE` (below) | Read your on-device music library via MediaStore | On first launch, with an explanation screen first |
| `RECORD_AUDIO` | Wake-word and voice-command recognition | Only when you turn on Voice Control (Settings or the in-app prompt) — never at first launch |
| `POST_NOTIFICATIONS` (API 33+) | Show the playback and voice-listening notifications, which Android requires for any foreground service | On first launch |
| Foreground service permissions (`FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MEDIA_PLAYBACK`, `FOREGROUND_SERVICE_MICROPHONE`) | Let playback and voice listening keep running with the screen off | Declared in the manifest; no user prompt, but each requires the notification above to be visible |

No permission is requested before you've seen an explanation of why it's needed (see
`ui/permissions/PermissionScreens.kt`).

## 5. How voice control works

```
Microphone
    ↓
WakeWordDetector  (Porcupine if configured, else the SpeechRecognizer fallback)
    ↓
"Hey Pratone" detected
    ↓
CommandSpeechRecognizer (one short recognition session, a few seconds)
    ↓
VoiceCommandParser (fixed grammar → VoiceCommand enum)
    ↓
PlayerManager (drives the same Media3 MediaController the UI and lock screen use)
```

Turn it on in **Settings → Voice → Voice control**. You'll see an explanation screen the first
time (microphone use requires a persistent notification while active — this is an Android
requirement, not something the app can hide). Supported commands: **play, pause, next, previous,
stop, shuffle, repeat**, plus a few natural variants ("please play", "skip this song", "stop the
music", etc.) — see `voice/VoiceCommandParser.kt` for the full grammar and
`voice/VoiceCommandParserTest.kt` for the test cases that pin it down.

## 6. Configuring the wake-word engine

The app ships with **two interchangeable engines** behind one `WakeWordDetector` interface
(`voice/WakeWordDetector.kt`):

### Fallback (works immediately, zero setup)
Built on Android's `SpeechRecognizer`, restarted in a loop. This is what runs if you do nothing.
It is **not a real wake-word engine** — it's a practical fallback, and it comes with real costs:
higher latency (each restart has ~0.3–1.5s overhead), higher battery use, and reliability that
varies by phone manufacturer. See the doc comment in
`voice/SpeechRecognizerWakeWordDetector.kt` for the full, unvarnished explanation.

### Porcupine (recommended — a real on-device wake-word engine)
1. Create a free account at **https://console.picovoice.ai** and copy your personal AccessKey.
2. In the app, go to **Settings → Voice → Porcupine access key** and paste it in.
3. On the Picovoice Console, train a custom wake word for the phrase **"Hey Pratone"** targeting
   **Android** — this produces a `.ppn` file (takes a couple of minutes, no ML expertise needed).
4. Copy that file into `app/src/main/assets/hey_pratone.ppn` and rebuild.
5. Once both the key and the file are present, the app automatically prefers Porcupine over the
   fallback (see `PorcupineWakeWordDetector.isAvailable()`); Settings shows which engine is active.

**Why this two-file dance can't be shipped pre-configured:** Picovoice's AccessKeys are
per-developer and tied to their terms of use (free for personal/development use; check
https://picovoice.ai/pricing/ before distributing a build commercially), and "Hey Pratone" isn't
one of Porcupine's built-in keywords, so a custom model has to be trained by whoever owns the key.
Shipping a shared hardcoded key would violate Picovoice's terms and wasn't something this project
was going to fake.

**APK size / offline / compatibility:** Porcupine's Android SDK + native libraries add roughly
3–5MB to the APK; the trained keyword model itself is under 50KB. It runs fully offline once the
key has done its one-time online validation. Supports API 21+ (well below this app's minSdk 26).
SDK license: Apache-2.0; AccessKey/model usage is governed by Picovoice's own commercial terms.

## 7. Features that depend on Android version / device capabilities

| Feature | Depends on |
|---|---|
| Background wake-word listening while locked/screen-off | A running foreground microphone service is mandatory (Android 9+); Android 14+ additionally requires the `FOREGROUND_SERVICE_MICROPHONE` type declared in the manifest (already done). Some OEM battery-optimization layers throttle background services more aggressively than stock Android regardless of correct API usage. |
| Acoustic echo cancellation (mitigates the mic hearing music from the speaker) | `AcousticEchoCanceler.isAvailable()` — not every device's audio HAL implements it. When unavailable, the app falls back to briefly ducking playback volume while actively listening (see `PlayerManager.duckVolume()`), which is a mitigation, not a fix. |
| Offline speech recognition (fallback engine) | Requires an offline language pack to be installed on the device for `EXTRA_PREFER_OFFLINE` to actually avoid the network; Android does not guarantee one is present. |
| Album artwork | Read from MediaStore's per-album artwork; not every file has embedded art. |
| `READ_MEDIA_AUDIO` vs `READ_EXTERNAL_STORAGE` | API 33+ vs below — handled automatically in `MediaPermissions.kt`. |

## 8. Known limitations

- The `SpeechRecognizer`-based wake-word fallback is meaningfully less reliable and more
  battery-hungry than the Porcupine path — this is disclosed, not hidden, in-app (Settings shows
  which engine is active) and in code comments.
- Custom wake phrases with Porcupine require manually training and dropping in a `.ppn` file;
  there's no in-app training flow (Picovoice doesn't expose one that could run on-device).
- Volume ducking during voice listening is a fallback mitigation for the speaker/microphone
  feedback problem, not a substitute for hardware AEC, and won't fully prevent false activations
  on devices without AEC support.
- No cloud backup/sync of playlists or favorites by design (see `backup_rules.xml` — the local
  Room database is explicitly excluded from Android auto-backup, since it stores device-local
  MediaStore ids that wouldn't resolve to anything meaningful after a restore to a different phone).
- No lyrics, streaming, or online metadata — this is an offline local player by design.

## 9. Architecture

```
app/
├── data/
│   ├── local/        MediaStoreScanner, Room (playlists/favorites/recent), permission helpers
│   └── repository/    MusicRepository, PlaylistRepository
├── domain/model/       Song, Album, Artist, RepeatMode
├── playback/           MusicPlaybackService (Media3 MediaSessionService), PlayerManager
├── voice/               VoiceCommand, VoiceCommandParser, VoiceState, WakeWordDetector +
│                        two implementations, CommandSpeechRecognizer, VoiceController,
│                        VoiceListeningService, AudioEffectsHelper
├── ui/
│   ├── home, library, nowplaying, settings, permissions, components, theme, navigation
│   └── AppViewModel    shared state holder tying repository + player + voice together
└── MainActivity.kt
```

The music player and the voice system are independent: `PlayerManager` never knows a command came
from voice vs. a tap vs. the lock screen, and `WakeWordDetector` is a small interface specifically
so the engine can be swapped or upgraded without touching anything downstream.

## 10. Testing

`app/src/test/java/com/pratone/app/voice/VoiceCommandParserTest.kt` covers the parser's grammar:
play/pause/next/previous variants, punctuation, whitespace, case-insensitivity, empty input, and
unknown commands. Run with:
```
./gradlew test
```

Playback lifecycle, screen-off playback, lock-screen controls, and permission-denial behavior are
best verified on a physical device per the checklist in the project brief, since they depend on
real Android service lifecycle and OEM behavior that a unit test can't exercise meaningfully.
#   p r a t o n e  
 