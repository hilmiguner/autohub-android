# AutoHub Android

AutoHub is a native Android / Android Auto project for a modular in-car media experience.

## Current status

**Phase 2 — Browser Foundation**

Phase 0 validated Android Auto host discovery, templated rendering, input callbacks and reconnect behavior. Phase 1 completed the reusable Media3 playback foundation used by the phone, Android system media controls and Android Auto media browsing. Phase 2 now builds a phone-only WebView browser foundation while keeping Android Auto on the validated media-only path.

Current Phase 2 scope includes:

- dedicated phone-only `BrowserActivity`
- native Android View browser chrome + platform WebView
- address entry with HTTPS normalization
- Back / Forward / Reload controls
- Android system Back integration with WebView history
- HTTP(S)-only navigation policy
- JavaScript + DOM storage baseline without a JavaScript bridge
- file/content access disabled
- mixed-content blocking and Safe Browsing
- first-party cookies enabled with third-party cookies disabled
- configuration-driven WebView current-page/history restoration (physical rotation retest pending)
- Phase 1 media regression coverage

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
- Android WebView for the phone browser

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
8. In the current build, Android Auto remains a media source; the browser is phone-only.

The Gradle Wrapper is committed to the repository so local development and CI use the same Gradle version. CI also verifies the wrapper JAR checksum before building and uploads the debug APK as the `autohub-debug` workflow artifact after a successful build.

## Media foundation

The Phase 1 player explicitly declares `USAGE_MEDIA` / music content and lets ExoPlayer manage audio focus. `handleAudioBecomingNoisy` is enabled so playback can pause safely when an output route such as a headset or Bluetooth device disappears. Queue/current-item/position state is persisted and restored paused after process recreation.

## Browser foundation

The browser uses a classic Android View hierarchy around WebView because the initial Compose + `AndroidView(WebView)` shell rendered as a blank white activity on the physical Samsung test phone. Browser navigation is restricted to HTTP(S). The browser saves WebView current-page/history state inside the Activity instance-state bundle and restores it after configuration-driven recreation such as orientation changes; this specific behavior is pending physical-device retest. Full process-death browser restoration remains a later Phase 2 slice.

## Branch policy

Development is performed on short-lived feature branches and merged through pull requests after CI and the relevant manual validation pass.
