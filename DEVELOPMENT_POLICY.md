# AutoHub Android — Development Policy

This document defines the active delivery and validation workflow for AutoHub Android.

## 1. MVP-First Delivery Strategy

AutoHub now follows an **MVP-first, validation-last** development strategy.

The development order is:

1. Define the MVP scope and keep `ROADMAP.md` as the product/technical source of truth.
2. Implement the remaining MVP features continuously in short-lived feature branches.
3. Keep automated CI/build checks running on each pull request so `main` remains buildable and obvious regressions are caught early.
4. Do **not** stop after every feature slice for physical-phone, DHU, AAOS-emulator, or vehicle acceptance testing unless a development blocker requires it.
5. Continue development until the agreed MVP feature scope is complete.
6. Enter **MVP feature freeze**.
7. Create the final validation matrix only after the MVP implementation is complete.
8. Execute the validation matrix as a dedicated test phase.
9. Fix defects discovered during validation, rerun the affected test groups, and repeat until the MVP release criteria are satisfied.

Automated CI is an engineering guardrail, not the final acceptance test. Passing CI does not by itself mean the MVP is release-ready.

## 2. Final MVP Validation Phase

The exact test list is intentionally decided after MVP feature completion so it reflects the product that actually exists.

The final validation plan is expected to cover, where applicable:

- Android phone functional flows
- process-death and persistence behavior
- browser navigation/session/user-agent/download/full-screen behavior
- Media3 playback, queue, seek, audio focus, lifecycle and persistence
- Android Auto DHU discovery, browse, playback, reconnect and phone/DHU synchronization
- Android Automotive OS emulator compatibility for features officially supported on AAOS
- TV/IPTV parsing/playback for lawful user-provided or licensed sources
- regression testing across previously completed phases
- performance, crash and recovery checks
- install/upgrade behavior
- release configuration and packaging
- physical vehicle compatibility when a suitable vehicle/head-unit environment is available

The test matrix may be expanded, reduced or reordered at MVP freeze based on the implemented feature set and known risks.

## 3. Pull Request Policy During MVP Development

During the MVP implementation phase:

- each feature should still be developed on a short-lived branch;
- pull requests may be merged after code review and automated CI are acceptable;
- manual device/DHU acceptance evidence is **not required per feature slice**;
- `main` must remain buildable;
- architectural boundaries and data migrations must remain reversible where practical;
- known manual-validation debt must be carried forward into the final MVP validation matrix instead of being silently discarded.

## 4. Unsupported / Experimental Components

The following product ideas may remain documented for research or product planning, but they are **not part of the supported production path** unless their platform, safety, legal and distribution constraints are resolved independently.

### Experimental vehicle-display behavior

- unofficial Android Auto projection/mirroring approaches outside the supported production APIs;
- browser or video presentation on Android Auto surfaces where the platform does not officially expose those app categories;
- behavior intended to remove or bypass parked/driving safety restrictions so restricted UI remains available while the vehicle is moving;
- privileged/root/Xposed-style hooks or other host modifications used to alter Android Auto/AAOS safety behavior.

These items must remain isolated from the supported product architecture and must never become a hidden dependency of the normal Android phone, Media3 Android Auto, or supported AAOS paths.

### Content-access boundaries

The following are also outside the supported implementation scope:

- DRM/paywall/access-control circumvention;
- unauthorized pay-TV/IPTV decryption or restreaming;
- provider-specific mechanisms intended to bypass advertising or access controls;
- distribution of content without the required rights or user authorization.

AutoHub may support lawful user-provided playlists, licensed streams, normal WebView browsing, standard Media3 playback and provider integrations that comply with the relevant platform/content rules.

## 5. Architecture Rule for Experimental Work

Any experimental component must be separated behind explicit interfaces/modules so the supported product can build and operate without it.

The intended boundary is:

```text
Supported AutoHub Core
├── Android phone UI
├── Browser core
├── Media3 playback
├── lawful TV/IPTV client
├── Android Auto supported media integration
└── AAOS supported integrations

Experimental / Unsupported Boundary
└── isolated research components only
```

The supported core remains the release baseline. Experimental research must not weaken CI, persistence, media-session behavior, content-security boundaries or supported car integrations.

## 6. Current Development Rule

Until the MVP feature scope is declared complete:

> **Implement first, validate comprehensively at the end.**

Do not interrupt the normal feature-development sequence with repetitive manual acceptance-test rounds. Preserve CI and code-level automated tests throughout development, then perform the complete physical/emulator/DHU validation campaign after MVP feature freeze.
