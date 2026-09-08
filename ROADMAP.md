# AutoHub Android — Roadmap, Technical Architecture and Delivery Plan

> Status: Phase 0 — Technical Spike
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

The supported production path uses AndroidX Car App Library and Android media APIs. Phase 0 validates discovery, host connection, lifecycle and input callbacks first.

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

## 4. Current Technical Stack

| Area | Choice |
| --- | --- |
| Language | Kotlin |
| Build | Gradle Kotlin DSL |
| Android Gradle Plugin | 9.4.0 |
| JDK | 17 |
| compileSdk | 37 |
| targetSdk | 36 |
| minSdk | 28 |
| Phone UI | Jetpack Compose |
| Compose BOM | 2026.08.00 |
| Car integration | AndroidX Car App 1.7.0 |
| Playback | Media3 (planned Phase 1) |
| Browser | Android WebView (planned) |
| Local persistence | DataStore + Room (planned) |
| Dependency injection | Hilt (planned when module count justifies it) |
| CI | GitHub Actions |

## 5. Target Module Architecture

Phase 0 intentionally starts as one `:app` module. Splitting modules before the technical spike succeeds would add complexity without reducing risk.

After Phase 0, the intended shape is:

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

The intended high-level flow is:

```text
Content Provider
      │
      ▼
Shared Domain / Media Core
      │
      ├──────────────► Phone UI
      │
      └──────────────► Car Adapter
                           │
                           ▼
                    Android Auto Host
```

The car layer should not contain YouTube/TV/browser-specific business rules. It should request actions from the shared application layer and render the result using the capabilities available on the active car platform.

## 7. Phase 0 — Android Auto Technical Spike

### Objective

Validate the highest-risk assumption before product development: can our native APK be discovered by the Android Auto host, establish a Car App Library session, render a screen and receive an input callback reliably?

### Current implementation

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
- [ ] CI green on Phase 0 branch
- [ ] Desktop Head Unit (DHU) manual validation
- [ ] Physical Android Auto vehicle validation
- [ ] Record device/Android Auto version compatibility result

### Temporary category note

The spike declares the `POI` Car App Library category strictly to validate the standard templated-app host lifecycle with a simple `PaneTemplate`. It is **not** the intended product category and must be replaced once the production Android Auto experience is defined.

### Exit criteria

Phase 0 is complete only when all of the following are true:

1. Debug APK builds in CI.
2. Phone shell launches on a physical Android device.
3. AutoHub appears in Android Auto/DHU.
4. Opening AutoHub creates a car session without host errors.
5. Phase 0 screen renders.
6. Pressing `Test input` increments the counter on the car screen.
7. Reopening/refreshing the phone shell can observe the same in-process counter during the active process.
8. Disconnect/reconnect does not crash the app.

## 8. Phase 1 — Media Foundation

Goal: build the reusable playback layer before adding content providers.

Planned work:

- Media3 player integration
- foreground playback service
- Android `MediaSession`
- audio focus handling
- playback notification
- queue model
- play/pause/seek/next/previous
- persistent playback state
- unit tests for playback state machine

Exit criterion: a local/test audio source can be controlled from both phone and supported Android Auto media controls.

## 9. Phase 2 — Browser Foundation

Goal: create a robust phone-side browser module that is independent of the car adapter.

Planned work:

- WebView lifecycle wrapper
- navigation history
- cookies/session storage
- downloads policy
- desktop/mobile user agent modes
- full-screen media callbacks
- safe JavaScript bridge design
- browser state restoration
- allow/deny navigation hooks

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

The Phase 0 POI declaration must be removed before production release.

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
      ├── Android SDK
      ├── Gradle 9.6
      ├── unit tests
      └── debug APK assemble
```

Later additions:

- Android lint
- Detekt/Ktlint
- dependency vulnerability scanning
- release signing on protected tags
- build artifact retention
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

1. Get Phase 0 CI green.
2. Fix any AGP/Kotlin/Car App API compilation differences revealed by CI.
3. Clone the repository locally and open it in Android Studio.
4. Install/run the debug APK on the test phone.
5. Configure Android Auto developer mode + Desktop Head Unit for supported development testing.
6. Validate the Phase 0 screen and input counter.
7. Record results in this document.
8. Only after Phase 0 exit criteria pass, start Phase 1 Media Foundation.

## 21. Research References

Primary references used for the initial architecture:

- Android for Cars App Library documentation
- Android Auto templated app setup documentation
- AndroidX Car App release/API documentation
- Android Media3 documentation
- Android WebView documentation
- Android MediaProjection/VirtualDisplay documentation for architectural research
- Fermata open-source project as an external architectural research reference only

---

Last updated: 2026-09-08
