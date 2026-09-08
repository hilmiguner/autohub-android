# AutoHub Android

AutoHub is a native Android / Android Auto project for a modular in-car media experience.

## Current status

**Phase 0 — Android Auto Technical Spike**

The first milestone intentionally focuses on the highest-risk technical path before product features are built:

- Android/Kotlin application skeleton
- Jetpack Compose phone shell
- AndroidX Car App `CarAppService`
- Android Auto projected runtime artifact
- car `Session` and `Screen` lifecycle
- interactive car input callback
- shared in-process spike state
- unit tests and CI debug build

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

## Local development

1. Clone the repository and switch to the active feature branch when developing an open PR.
2. Verify the repository-controlled Gradle toolchain on Windows:

   ```powershell
   .\gradlew.bat --version
   ```

   Phase 0 expects Gradle 9.5.0.
3. Open the repository root in Android Studio Quail 3 (2026.1.3) or a compatible newer version.
4. Configure the project Gradle JDK as JDK 17.
5. Let Android Studio install/sync the required Android SDK components.
6. Run the `app` configuration on an Android phone/emulator.
7. For Phase 0 Android Auto validation, use Google's supported Android Auto developer/DHU workflow and verify the `AutoHub · Phase 0` screen.
8. Press `Test input` on the car screen and verify that its counter increments.

The Gradle Wrapper is committed to the repository so local development and CI use the same Gradle version. CI also verifies the wrapper JAR checksum before building and uploads the Phase 0 debug APK as the `autohub-phase0-debug` workflow artifact after a successful build.

## Branch policy

Development is performed on short-lived feature branches and merged through pull requests after CI passes.
