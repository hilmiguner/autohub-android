# AutoHub Android — Roadmap, Technical Architecture and Delivery Plan

> Status: Phase 2 — Browser Foundation
>
> This document is the source of truth for the Android application's technical direction. It will be updated as architectural decisions are validated or changed.

## 1. Product Goal

AutoHub is planned as a native Android in-car media platform with a shared phone/car experience.

Long-term product areas:

- Android phone application
- Android Auto integration
- audio/media playback
- web browsing on platforms/modes where it is supported
- video playback on platforms/modes where it is supported
- TV/IPTV client support for user-provided or licensed sources
- favorites and history
- account, device and subscription management
- remote configuration and feature flags

The project must separate **content features** from **car transport/rendering** so that Android, Android Auto and Android Automotive OS can evolve independently.

## 2. Platform Strategy

### Android phone

The phone application is the full-featured client. It can host normal Android UI components such as Compose, WebView and Media3 playback.

### Android Auto

The supported production path uses AndroidX Car App Library and Android media APIs. Phase 0 validated discovery, host connection, lifecycle and input callbacks. Phase 1 validated the standard Android Auto media-browser path using Media3.

Android Auto-specific experimental work must remain isolated from product/business logic so compatibility changes do not force a rewrite of the entire application.

### Android Automotive OS

AAOS is a later target. Parked-app video/browser capabilities can be evaluated independently after the phone and Android Auto foundations are stable.

## 3. Architecture Principles

1. **Native Kotlin first** — Android-specific APIs are central to this product.
2. **Thin car adapter** — car code should translate host lifecycle/input into shared application operations rather than own business logic.
3. **Shared domain state** — phone and car entry points should consume the same state/services.
4. **Playback abstraction** — content sources should not depend directly on a concrete player implementation.
5. **Provider isolation** — browser, TV and other providers live behind independent interfaces.
6. **Compatibility isolation** — Android Auto/AAOS compatibility code must remain outside content/domain modules.
7. **Clean-room implementation** — Fermata is used as an architectural research reference only. AutoHub source code must be independently implemented. Fermata GPL source is not to be copied into proprietary AutoHub modules.
8. **No premature root/Xposed dependency** — the base product must not require privileged device modifications.
9. **Test the risky assumption first** — car host/render/input feasibility is validated before building subscription or content catalog systems.
10. **Reproducible toolchain** — Gradle is pinned through the repository wrapper so local development and CI use the same Gradle runtime.

## 4. Current Technical Stack

| Area | Choice |
| --- | --- |
| Recommended IDE | Android Studio Quail 3 / 2026.1.3 |
| Language | Kotlin (AGP 9 built-in Kotlin support) |
| Build | Gradle Kotlin DSL |
| Android Gradle Plugin | 9.3.2 |
| Gradle | 9.5.0 via checked-in Wrapper |
| JDK | 17 |
| compileSdk | 36 |
| targetSdk | 36 |
| minSdk | 28 |
| Phone UI | Jetpack Compose + classic Android Views where platform WebView integration benefits from them |
| Compose BOM | 2026.02.01 |
| AndroidX Core KTX | 1.17.0 |
| AndroidX Activity Compose | 1.12.4 |
| Car integration | AndroidX Car App 1.7.0 + app-projected 1.7.0 |
| Playback | Media3 1.11.0 |
| Browser | Android WebView |
| Local persistence | DataStore + Room (planned) |
| Dependency injection | Hilt (planned when module count justifies it) |
| CI | GitHub Actions |

The AGP 9.3.2 + Gradle 9.5.0 + JDK 17 baseline is selected to remain compatible with Android Studio Quail 3 while keeping the build on the AGP 9 toolchain.

## 5. Target Module Architecture

Phase 0 intentionally started as one `:app` module. The intended longer-term shape remains:

```text
AutoHub
│
├── app-mobile
│   ├── PhoneActivity
│   └── Compose navigation
│
├── car-integration
│   ├── CarAppService
│   ├── CarSession
│   ├── CarScreenRouter
│   ├── HostCompatibility
│   └── InputAdapter
│
├── core-model
│   ├── MediaItem
│   ├── PlaybackState
│   ├── ContentProvider
│   └── UserEntitlement
│
├── media-core
│   ├── PlaybackEngine
│   ├── PlaybackSession
│   ├── AudioFocus
│   └── MediaSessionAdapter
│
├── player-media3
│   └── Media3PlaybackEngine
│
├── feature-browser
│   ├── BrowserSession
│   ├── BrowserView
│   └── WebMediaBridge
│
├── feature-tv
│   ├── PlaylistParser
│   ├── M3U
│   ├── XMLTV
│   ├── EPG
│   └── ChannelRepository
│
├── data
│   ├── Room
│   ├── DataStore
│   └── repositories
│
└── backend-client
    ├── Auth
    ├── Subscription
    ├── DeviceBinding
    └── RemoteConfig
```

## 6. Runtime Model

```text
Content Provider
      │
      ▼
Shared Domain / Media Core
      │
      ├──────────────► Phone UI / MediaController
      │
      └──────────────► MediaLibraryService / MediaSession
                           │
                           ├────────► Android system media controls
                           └────────► Android Auto media browser
```

The car layer should not contain YouTube/TV/browser-specific business rules. Content providers should resolve shared media IDs into playable items while the service/session owns playback and external control integration.

## 7. Phase 0 — Android Auto Technical Spike

### Objective

Validate the highest-risk assumption before product development: can our native APK be discovered by the Android Auto host, establish a Car App Library session, render a screen and receive an input callback reliably?

### Completed implementation

- [x] Repository initialized
- [x] Android/Kotlin project skeleton
- [x] Jetpack Compose phone shell
- [x] `CarAppService` entry point
- [x] `Session` lifecycle
- [x] Phase 0 car `Screen`
- [x] Interactive car test action
- [x] In-process spike state shared by phone/car entry points
- [x] Unit test for the spike state
- [x] GitHub Actions build/test workflow
- [x] Repository-controlled Gradle 9.5.0 Wrapper
- [x] Wrapper distribution checksum pinning
- [x] Wrapper-based CI green on final Phase 0 toolchain
- [x] Physical Android phone install and phone-shell launch validation
- [x] Desktop Head Unit (DHU) discovery/render/input validation
- [x] DHU disconnect/reconnect recovery validation
- [x] Compatibility result recorded
- [ ] Physical Android Auto vehicle validation — deferred until a suitable vehicle test environment is available; this does not block the Phase 0 technical-spike objective

### Manual validation record — 2026-09-08

| Item | Result |
| --- | --- |
| Phone | Samsung Galaxy A23 SM-A235F/DSN |
| Android | 14 |
| Android Auto | 17.5.663214-release |
| Test host | Android Auto Desktop Head Unit (DHU) |
| Connection | USB / ADB DHU tunnel |
| AutoHub visible in launcher | Pass |
| Car session opens without crash | Pass |
| Phase 0 screen renders | Pass |
| `Test input` increments car-side counter | Pass |
| Phone refresh observes shared counter | Pass |
| DHU disconnect/reconnect | Pass |
| Reconnect screen/input/shared-state flow | Pass |

The physical vehicle test is retained as a compatibility validation task and should be run with the same APK when a suitable Android Auto vehicle/head-unit environment becomes available.

### Temporary category note

The Phase 0 spike used the `POI` Car App Library category strictly to validate the standard templated-app host lifecycle with a simple `PaneTemplate`. Phase 1 no longer declares that templated service in the manifest; Android Auto discovery is media-only for the current build.

### Exit criteria

1. Debug APK builds in wrapper-based CI. **Pass**
2. Phone shell launches on a physical Android device. **Pass**
3. AutoHub appears in Android Auto/DHU. **Pass**
4. Opening AutoHub creates a car session without host errors. **Pass**
5. Phase 0 screen renders. **Pass**
6. Pressing `Test input` increments the counter on the car screen. **Pass**
7. Reopening/refreshing the phone shell can observe the same in-process counter during the active process. **Pass**
8. Disconnect/reconnect does not crash the app. **Pass**

**Phase 0 status: COMPLETE.**

## 8. Phase 1 — Media Foundation

Goal: build the reusable playback layer before adding product content providers.

### Completed implementation

- [x] Media3 1.11.0 dependency baseline
- [x] ExoPlayer owned by a service rather than the Activity
- [x] `MediaLibraryService` + `MediaLibrarySession`
- [x] foreground media-playback permissions/service declaration
- [x] legacy/platform media browser compatibility intent
- [x] Android Auto media-only capability declaration
- [x] deterministic 10-second generated offline test tone
- [x] phone-side `MediaController` connection
- [x] phone play / pause / resume controls
- [x] provider-independent immutable queue model
- [x] unit tests for queue navigation semantics
- [x] CI green across Phase 1 slices
- [x] physical-phone playback/manual lifecycle validation
- [x] Android system media notification validation
- [x] Android Auto DHU media discovery/browse/play/pause validation
- [x] DHU disconnect/reconnect recovery validation
- [x] explicit audio-focus routing validation after `USAGE_MEDIA` + ExoPlayer-managed focus fix
- [x] competing-media audio focus behavior validation
- [x] three-item Media3 queue integration
- [x] seek/next/previous integration on phone and DHU
- [x] phone/DHU queue and playback-state synchronization
- [x] persistent queue/current-item/playback-position state
- [x] safe paused restore after process recreation
- [x] resume from restored playback position

### DHU validation record — 2026-09-09

| Item | Result |
| --- | --- |
| AutoHub discovered as media source | Pass |
| `AutoHub Test Tone` visible | Pass |
| Test tone starts from DHU | Pass |
| Play / pause controls | Pass |
| Phone / DHU playback state sync | Pass |
| Disconnect / reconnect discovery | Pass |
| Disconnect / reconnect playback | Pass |
| Crash during reconnect | None |
| Fresh-session audible output without priming another app | Pass |
| Spotify ↔ AutoHub audio-focus handoff | Pass |
| Three-item queue Previous / Next | Pass |
| Seek / progress control | Pass |
| Restored queue/current item visible | Pass |
| Restored session controls after process recreation | Pass |

The first DHU build exposed an audio-routing issue where AutoHub could enter playing state without audible output until another media app activated the audio path. The service now configures explicit `USAGE_MEDIA`, `AUDIO_CONTENT_TYPE_MUSIC`, ExoPlayer-managed audio focus, and `handleAudioBecomingNoisy=true`. Fresh-DHU playback and competing-media handoff were both retested successfully on 2026-09-09.

Playback persistence stores ordered media IDs, current index, position and last playing-state metadata. Process recreation restores the queue/current item/position but intentionally does not autoplay; the next explicit Play/Resume command continues from the restored state.

### Architecture decision

Phase 1 uses `MediaLibraryService` instead of a plain `MediaSessionService`. AutoHub needs a browsable content tree for Android Auto, and `MediaLibraryService` extends the session model while exposing that library through the standard media-browser interfaces. Content-provider-specific code remains outside the service; the current `DemoMediaCatalog` exists only to validate the infrastructure.

ExoPlayer owns audio-focus behavior. The service configures media audio attributes and enables ExoPlayer's automatic focus handling rather than maintaining a second manual `AudioManager` focus implementation.

The Phase 1 persistence contract is isolated behind `PlaybackStateStore`. A small SharedPreferences-backed implementation provides synchronous service-start restoration for the current single-module phase; it can move to the planned data layer later without changing the playback-domain contract.

### Exit criterion

A local/test audio source can be browsed and controlled from both the phone and supported Android Auto media controls through the same service-owned Media3 session, with expected foreground/audio-focus/lifecycle and process-restoration behavior. **Pass.**

**Phase 1 status: COMPLETE.**

## 9. Phase 2 — Browser Foundation

Goal: create a robust phone-side browser module that is independent of the car adapter.

### Current implementation

- [x] dedicated phone-only `BrowserActivity`
- [x] native Android View hierarchy + platform WebView lifecycle wrapper
- [x] address bar and host-like input normalization to HTTPS
- [x] Back / Forward / Reload navigation controls
- [x] Android system Back integrated with WebView history
- [x] HTTP(S)-only allow/deny navigation policy
- [x] unit tests for URL normalization and blocked schemes
- [x] JavaScript + DOM storage baseline without a JavaScript bridge
- [x] file/content access disabled
- [x] mixed-content blocked and Safe Browsing enabled
- [x] first-party cookies allowed; third-party cookies disabled by default
- [x] browser remains excluded from Android Auto capabilities
- [ ] WebView current page/history restoration across configuration-driven Activity recreation — implementation added, physical rotation retest pending
- [ ] cookie/session persistence policy
- [ ] browser state restoration after full process death
- [ ] downloads policy
- [ ] desktop/mobile user-agent modes
- [ ] full-screen media callbacks
- [ ] safe JavaScript bridge design for later provider integrations

### Physical phone validation record — 2026-09-09

| Item | Result |
| --- | --- |
| Browser activity opens | Pass |
| Native browser chrome renders | Pass |
| Default `https://example.com` renders | Pass |
| HTTPS normalization | Pass |
| Back / Forward / Reload | Pass |
| Android Back uses WebView history first | Pass |
| `file://` blocked | Pass |
| `javascript:` blocked | Pass |
| Phase 1 media regression | Pass |
| Landscape rotation retains current page/history | Pending retest after state-restoration fix |

The first browser shell used Compose + `AndroidView(WebView)` and rendered as a blank white activity on the Samsung test phone. The browser chrome was moved to a classic Android View hierarchy so controls remain independent of WebView page rendering. A second physical-device test confirmed the chrome and default page render correctly.

A subsequent landscape-rotation test exposed that `BrowserActivity` was recreated and always loaded the default page. The activity now saves WebView state into the instance-state bundle and restores it after configuration-driven recreation instead of unconditionally loading `https://example.com`. This fix is pending physical rotation retest.

The browser should remain a normal Android feature and only be exposed to a car target when that target/mode officially supports the required UI.

## 10. Phase 3 — TV / Stream Client

Goal: add a source-agnostic TV client for user-provided or licensed streams.

Planned work:

- M3U/M3U8 parser
- channel groups
- channel metadata
- XMLTV parser
- EPG timeline
- favorites
- recent channels
- stream health/error states
- Media3 playback adapter
- optional catch-up abstraction when provided by the source

No provider-specific DRM circumvention belongs in the TV core.

## 11. Phase 4 — Web Media Provider Integration

Goal: build provider adapters without coupling them to the car layer.

Planned work:

- WebView-based provider abstraction
- playback metadata bridge
- play/pause/seek synchronization where permitted
- media-session metadata mapping
- provider-specific lifecycle isolation

Provider integrations must be reviewed independently for platform terms and content rights before commercial release.

## 12. Phase 5 — Account, Licensing and Subscription Backend

Android client scope:

- sign-in
- access/refresh token storage
- device registration
- entitlement cache
- subscription status
- remote configuration
- feature flags
- minimum supported app version
- logout/device revoke

Backend is expected to live in a separate repository (`autohub-backend`) once this phase begins.

## 13. Phase 6 — Production Android Auto Experience

After the content and media foundations exist, define the final supported Android Auto product category and UX.

Expected production-safe features include:

- audio browsing
- favorites
- recent content
- playback controls
- voice/search integrations where supported
- account status/error resolution screens

The Phase 0 POI declaration is no longer active in Phase 1 and must not return in the production media build unless a supported product category requires it.

## 14. Phase 7 — Android Automotive OS

Planned work:

- dedicated AAOS module/app target
- parked-app capability detection
- video UI for supported parked modes
- browser UI for supported parked modes
- driving-state transitions
- AAOS-specific testing matrix

## 15. Phase 8 — Compatibility Research Track

This track is deliberately isolated from the main product architecture.

Research topics may include:

- Android Auto host/version behavior
- rendering surface lifecycle
- input coordinate mapping
- device/manufacturer compatibility
- install/distribution behavior
- reconnect/session recovery
- deferred physical vehicle/head-unit validation from Phase 0

Research code must not contaminate shared media/content modules. Any technique that depends on unsupported host behavior should remain an experiment until its reliability, policy implications and maintenance cost are understood.

## 16. Phase 9 — Commercial Hardening

- backend production deployment
- subscription billing integration
- device limits
- entitlement revocation
- crash reporting
- analytics with privacy controls
- APK signing/release pipeline
- update mechanism/distribution strategy
- support diagnostics bundle
- privacy policy and terms
- security review
- license compliance review

## 17. Testing Strategy

### Unit tests

Required for:

- parsers
- domain state
- playback state machine
- entitlement decisions
- coordinate/state transformations

### Android instrumentation tests

Required for:

- persistence
- WebView lifecycle-critical paths
- activity/service integration where practical

### Car tests

Three levels:

1. host-independent unit tests
2. Desktop Head Unit manual tests
3. physical vehicle tests

Compatibility reports should record:

- phone model
- Android version
- Android Auto version
- wired/wireless connection
- vehicle/head unit model
- observed result

## 18. CI/CD Plan

Current CI:

```text
push / pull request
      │
      ├── JDK 17
      ├── Android SDK 36
      ├── verify Gradle wrapper JAR checksum
      ├── Gradle Wrapper 9.5.0
      ├── unit tests
      ├── debug APK assemble
      └── upload debug APK artifact
```

The build must use `./gradlew` / `gradlew.bat`; CI must not select a different Gradle version independently from the repository.

Later additions:

- Android lint
- Detekt/Ktlint
- dependency vulnerability scanning
- release signing on protected tags
- automated versioning/release notes

## 19. Git Workflow

Use short-lived feature branches:

```text
main
 └── feat/phase-X-description
       └── draft PR
            ├── CI
            ├── review
            └── squash merge
```

Rules:

- `main` should remain buildable.
- Development work should happen in PR branches.
- One roadmap slice per PR where practical.
- Architecture changes must update this document.

## 20. Immediate Next Steps

1. Retest browser current-page/history restoration across orientation changes.
2. Define cookie/session persistence policy.
3. Add full browser process-restoration behavior.
4. Add desktop/mobile user-agent modes and downloads policy.
5. Run deferred physical vehicle compatibility validation when an environment becomes available.

## 21. Research References

Primary references used for the initial architecture:

- Android for Cars App Library documentation
- Android Auto media app documentation
- AndroidX Car App release/API documentation
- Android Media3 documentation
- Android WebView documentation
- Android MediaProjection/VirtualDisplay documentation for architectural research
- Fermata open-source project as an external architectural research reference only

---

Last updated: 2026-09-09
