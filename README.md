# AutoHub Android

AutoHub is a native Android / Android Auto project for a modular in-car media experience.

## Current status

**Phase 1 — Media Foundation**

Phase 0 validated the Android Auto host lifecycle on a physical Android phone and Desktop Head Unit (DHU). Phase 1 now builds the reusable audio playback path that future content providers will share:

- Media3 ExoPlayer 1.11.0
- `MediaLibraryService` + `MediaLibrarySession`
- foreground media-playback service declaration
- Android Auto media capability declaration
- deterministic bundled/offline test tone
- phone-side `MediaController` test controls
- provider-independent queue domain model
- unit tests and CI debug build

The Phase 0 POI template path remains temporarily available for regression testing. Physical vehicle compatibility validation is deferred until a suitable test environment is available.

See [`ROADMAP.md`](ROADMAP.md) for the architecture, delivery phases, technical decisions and exit criteria.

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
- AndroidX Media3 ExoPlayer / Session 1.11.0

## Local development

1. Clone the repository and switch to the active feature branch when developing an open PR.
2. Verify the repository-controlled Gradle toolchain on Windows:

   ```powershell
   .\gradlew.bat --version
   ```

   The project expects Gradle 9.5.0.
3. Open the repository root in Android Studio Quail 3 (2026.1.3) or a compatible newer version.
4. Configure the project Gradle JDK as JDK 17.
5. Let Android Studio install/sync the required Android SDK components.
6. Run the `app` configuration on an Android phone/emulator.
7. On the Phase 1 phone screen, verify the media session connects and use `Play test tone`, `Pause`, and `Restart` to exercise the service-owned player.
8. For Android Auto media validation, use Google's supported Android Auto developer/DHU workflow. AutoHub now declares both media support and the temporary Phase 0 template capability.

The Gradle Wrapper is committed to the repository so local development and CI use the same Gradle version. CI also verifies the wrapper JAR checksum before building and uploads the debug APK workflow artifact after a successful build.

## Branch policy

Development is performed on short-lived feature branches and merged through pull requests after CI passes.
