# Browser Provider Bridge Contract

## Purpose

Phase 2 defines the security and domain contract for future web-media provider integrations without exposing a JavaScript bridge to arbitrary browser pages.

The generic AutoHub browser remains a normal WebView with no provider bridge installed. Runtime provider messaging is deferred to Phase 4, where each supported provider must be registered explicitly.

## Trust model

Web content is untrusted input. A future provider bridge may be enabled only when all of the following are true:

1. A provider has an explicit `BrowserProviderDescriptor`.
2. The descriptor has a valid stable provider ID.
3. Every configured origin is HTTPS.
4. The current top-level page origin exactly matches an allowed origin after normalization.
5. The requested message/action is covered by the provider's declared capability set.
6. Incoming values pass the shared validation/sanitization policy before reaching media/domain state.

Origin matching is exact. A configured `https://media.example.com` does not implicitly trust `http://media.example.com`, `https://example.com`, or `https://cdn.media.example.com`.

## Current Phase 2 contract

The contract models these future capabilities:

- `MEDIA_METADATA`
- `PLAYBACK_STATE`
- `PLAYBACK_COMMANDS`

Metadata is bounded and sanitized before use. Artwork must resolve to an allowed HTTP(S) URL. Playback state rejects negative positions/durations and positions beyond a known finite duration. A null duration remains valid for live/unknown-duration media.

## Runtime transport requirements for Phase 4

When a provider transport is implemented:

- do not install a generic `addJavascriptInterface` object for normal browsing;
- prefer a message transport that supports explicit origin rules, such as an AndroidX WebKit web-message listener or a tightly scoped message-port adapter;
- create/enable the transport only for the active registered provider;
- remove/disable the transport when navigation leaves the trusted provider origin;
- validate every message again in native code;
- reject unknown message types, unknown fields that change semantics, malformed numeric values, oversized strings, and undeclared capabilities;
- never expose arbitrary native method invocation, filesystem access, cookies, auth tokens, headers, raw database access, or unrestricted URL loading through the bridge;
- never treat a provider message as proof of account entitlement or content rights;
- do not use the bridge to bypass DRM, paywalls, access controls, advertising controls, or vehicle/platform safety restrictions.

## Command boundary

`PLAYBACK_COMMANDS` is intentionally only a capability declaration in Phase 2. No generic page can issue native play/pause/seek commands today.

Phase 4 provider adapters must map a small fixed command vocabulary to shared media-domain operations. They must not accept JavaScript source, method names, reflection targets, native intents, shell commands, or arbitrary URLs as executable commands.

## Phase 2 closeout meaning

Phase 2 closes when the phone browser foundation, persistence, Mobile/Desktop UA modes, download policy, full-screen media lifecycle, and this provider bridge contract are present and automated CI is green.

Per `DEVELOPMENT_POLICY.md`, physical phone/DHU/AAOS acceptance testing is deferred until the MVP feature-freeze validation phase. The bridge itself has no runtime transport in Phase 2, so its current validation is pure policy/unit testing.
