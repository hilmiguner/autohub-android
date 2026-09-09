# AutoHub Android

AutoHub is a native Android / Android Auto project for a modular in-car media experience.

## Current status

**Phase 1 — Media Foundation**

Phase 0 validated Android Auto host discovery, templated rendering, input callbacks and reconnect behavior. Phase 1 now builds the reusable Media3 playback foundation used by the phone, Android system media controls and Android Auto media browsing.

Current Phase 1 scope includes:

- Media3 ExoPlayer + `MediaLibraryService`
- service-owned `MediaLibrarySession`
- Android Auto media-source discovery
- deterministic bundled test media
- phone `MediaController` controls
- foreground playback notification/system controls
- shared phone/DHU playback state
- explicit media audio attributes and ExoPlayer-managed audio focus
- provider-independent queue model
- unit tests and CI debug build

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
7. For Android Auto validation, use Google's supported Android Auto developer/DHU workflow.
8. In the Phase 1 build, open AutoHub as an Android Auto media source and validate `AutoHub Test Tone` browsing/playback.

The Gradle Wrapper is committed to the repository so local development and CI use the same Gradle version. CI also verifies the wrapper JAR checksum before building and uploads the debug APK as the `autohub-debug` workflow artifact after a successful build.

## Audio focus

The Phase 1 player explicitly declares `USAGE_MEDIA` / music content and lets ExoPlayer manage audio focus. `handleAudioBecomingNoisy` is also enabled so playback can pause safely when an output route such as a headset or Bluetooth device disappears.

## Branch policy

Development is performed on short-lived feature branches and merged through pull requests after CI and the relevant manual validation pass.
