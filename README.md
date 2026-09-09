# AutoHub Android

AutoHub is a native Android / Android Auto project for a modular in-car media experience.

## Current status

**Phase 2 — Browser Foundation**

Phase 0 validated Android Auto host discovery/lifecycle behavior. Phase 1 completed the reusable Media3 playback foundation, including Android Auto browsing/playback, audio focus, queue controls and process-restored playback state. Phase 2 now builds the phone-side browser module independently from the car adapter.

Current validated/completed media foundation includes:

- Media3 ExoPlayer + `MediaLibraryService`
- service-owned `MediaLibrarySession`
- Android Auto media-source discovery
- deterministic 10-second generated test media
- phone `MediaController` controls
- foreground playback notification/system controls
- shared phone/DHU queue and playback state
- explicit media audio attributes and ExoPlayer-managed audio focus
- Previous / Next / Seek queue controls
- persistent queue/current-item/position restore

Current Phase 2 browser slice includes:

- dedicated phone-only `BrowserActivity`
- Compose + Android WebView shell
- address bar with HTTPS normalization
- Back / Forward / Reload navigation
- Android Back integration with WebView history
- HTTP(S)-only navigation policy with unit tests
- Safe Browsing and mixed-content blocking
- file/content access disabled
- JavaScript + DOM storage enabled without a JavaScript bridge
- first-party cookies enabled and third-party cookies disabled by default

The browser is not exposed as an Android Auto capability. Android Auto remains on the validated media-only path.

See [`ROADMAP.md`](ROADMAP.md) for the architecture, delivery phases, technical decisions and validation status.

## Development stack

- Android Studio Quail 3 / 2026.1.3 recommended for local development
- Kotlin with AGP 9 built-in Kotlin support
- Gradle Kotlin DSL
- JDK 17
- Android Gradle Plugin 9.3.2
- Gradle 9.5.0 through the checked-in Gradle Wrapper
- compileSdk 36 / targetSdk 36 / minSdk 28
- Jetpack Compose (BOM 2026.02.01)
- AndroidX Core KTX 1.17.0
- AndroidX Activity Compose 1.12.4
- AndroidX Car App 1.7.0
- AndroidX Car App Projected 1.7.0
- AndroidX Media3 1.11.0
- Android WebView

## Local development

1. Clone the repository and switch to the active feature branch when developing an open PR.
2. Verify the repository-controlled Gradle toolchain on Windows:

   ```powershell
   .\gradlew.bat --version
   ```

   The current baseline expects Gradle 9.5.0.
3. Open the repository root in Android Studio Quail 3 (2026.1.3) or a compatible newer version.
4. Configure the project Gradle JDK as JDK 17.
5. Let Android Studio install/sync the required Android SDK components.
6. Run the `app` configuration on an Android phone/emulator.
7. Open **Open phone browser** from the AutoHub phone shell to validate the current Phase 2 browser slice.
8. For Android Auto regression validation, use Google's supported Android Auto developer/DHU workflow and confirm the existing media source still behaves normally.

The Gradle Wrapper is committed to the repository so local development and CI use the same Gradle version. CI also verifies the wrapper JAR checksum before building and uploads the debug APK as the `autohub-debug` workflow artifact after a successful build.

## Audio focus

The media player explicitly declares `USAGE_MEDIA` / music content and lets ExoPlayer manage audio focus. `handleAudioBecomingNoisy` is also enabled so playback can pause safely when an output route such as a headset or Bluetooth device disappears.

## Browser boundary

The Phase 2 browser is a normal phone Android feature. It does not add a browser/template capability to Android Auto. Provider-specific bridges and any future car exposure must be evaluated independently against supported platform capabilities before integration.

## Branch policy

Development is performed on short-lived feature branches and merged through pull requests after CI and the relevant manual validation pass.
