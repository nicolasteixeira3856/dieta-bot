# CP4 — Android installation identity

- Status: Aguardando aprovação
- Date: 2026-09-30
- Owner: `content-policy`; executable owner: Android.
- Delivery boundary: `apps/android/`, plus related documentation/indexes.
- Prerequisites: CP3 contract implemented and automated acceptance recorded. No layout/Stitch prerequisite because this delivery adds no UI.

## Authorization and objective

Approve this named plan to send one private, pseudonymous installation signal to Dieta Bot's API. It is not account authentication or a device fingerprint. Follow the Android feature/architecture/Kotlin skills within this approved scope, one agent.

## Sources

[Identity specification](../specifications/identity-and-audit.md), [ADR-025](../adrs/ADR-025-safety-correlation-audit.md), [Android context](../../android/README.md), [API contract](../../api-contract.md).

## Implementation

1. Add an injected installation-ID provider in the data/core infrastructure boundary. Generate a secure UUID v4 once, persist atomically in app-private `noBackupFilesDir`, and avoid disk work on the UI thread. Concurrent first requests must share one value. A corrupt/missing file creates a fresh ID; log only a fixed diagnostic code.
2. Send `X-Client-Instance-Id` through the existing network stack for estimate, fit, chat and compaction. Restrict it to the configured API origin and prevent forwarding to another origin on redirects. Do not attach it to arbitrary image URLs or unrelated clients.
3. Preserve across updates/process death and daily wipe. Clear-data/uninstall resets it; dev/prod remain isolated. No setting, new user permission, Room schema migration, DataStore for day state, Firebase identity or advertising identifier.
4. Keep the existing `X-Invite` and request-ID behavior. Startup/ID-storage failure uses a bounded failure path; it never leaks a raw device identifier as fallback. Old servers may ignore the additive header.
5. Verify existing UI treatment of CP2/CP3 responses: refusal bubble has no action card/memory update; 400/403/503 do not display raw internal codes or retry indefinitely. This plan may adjust nonvisual response mapping in the existing data/domain flow if needed; any new layout/control requires a separate approved plan and Stitch gate.

## Files

`app/src/main/java/com/nutri/android/core/network/NetworkModule.kt`, a focused installation provider/interceptor, existing Hilt bindings and corresponding tests under `app/src/test/`. No server, infra, version.properties or signing changes.

## Validation

- Existing Android test libraries: first-use concurrency, persistence after provider recreation, missing/corrupt storage, separate flavor storage, daily-wipe preservation, no backup, header scope, cross-origin redirect, legacy server compatibility and no telemetry leakage.
- From `apps/android/`: `./gradlew.bat :app:testDevDebugUnitTest :app:assembleDevRelease`; run `verifyRoborazziDevDebug` if response rendering/composable behavior changes. No new test framework.
- Emulator: update/restart retains the ID; clear-data/reinstall of a disposable test installation changes it. Do not erase an owner's/tester's real data to test reset. Capture only synthetic identifiers in private evidence, not committed logs.
- If rendered behavior changes, use fresh dark/light captures against existing matching golds and a written diff; do not alter golds to fit code. No UI change means no invented visual completion claim.

## Acceptance and completion

Same installation correlates across a restart without sending IP/UUID to Firebase/OpenAI. A second installation has a different signal. Client remains usable with the additive contract.

Automated implementation may move to `Pendente aprovação manual`; CP5 verifies the distributed APK path. A build is not automatically a distribution request: when the owner requests the test release, use `tools/distribute-dev.ps1 -Notes <scratchpad.md>` per A16, never manual version editing. Apply SDD Git delivery. No UI for legal acceptance is delivered here.
