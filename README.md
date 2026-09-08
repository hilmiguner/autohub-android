# AutoHub Android

AutoHub is a native Android / Android Auto project for a modular in-car media experience.

## Current status

**Phase 0 — Android Auto Technical Spike**

The first milestone intentionally focuses on the highest-risk technical path before product features are built:

- Android/Kotlin application skeleton
- Jetpack Compose phone shell
- AndroidX Car App `CarAppService`
- car `Session` and `Screen` lifecycle
- interactive car input callback
- shared in-process spike state
- unit tests and CI debug build

See [`ROADMAP.md`](ROADMAP.md) for the architecture, delivery phases, technical decisions and exit criteria.

## Development stack

- Kotlin
- Gradle Kotlin DSL
- JDK 17
- Android Gradle Plugin 9.4.0
- compileSdk 36 / targetSdk 36 / minSdk 28
- Jetpack Compose (BOM 2026.06.00)
- AndroidX Car App 1.7.0

## Local development

1. Clone the repository.
2. Open the repository root in a current Android Studio version compatible with AGP 9.4.
3. Let Android Studio install/sync the required Android SDK components.
4. Run the `app` configuration on an Android phone/emulator.
5. For Phase 0 Android Auto validation, use Google's supported Android Auto developer/DHU workflow and verify the `AutoHub · Phase 0` screen.
6. Press `Test input` on the car screen and verify that its counter increments.

## Branch policy

Development is performed on short-lived feature branches and merged through pull requests after CI passes.
