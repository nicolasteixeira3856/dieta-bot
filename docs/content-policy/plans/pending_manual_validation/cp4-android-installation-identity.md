# CP4 — Android installation identity

- Status: Pendente aprovação manual
- Approved: 2026-10-01 by the owner: "Aprovo o plano docs\content-policy\plans\cp4-android-installation-identity.md, analise e implemente o plano aprovado".
- Date: 2026-09-30 (prerequisites refreshed 2026-09-30 for the closed test)
- Owner: `content-policy`; executable owner: Android.
- Delivery boundary: `apps/android/`, plus related documentation/indexes.
- Prerequisites: [CP3](cp3-server-safety-identifier.md) implemented and automated acceptance recorded. No layout/Stitch prerequisite because this delivery adds no UI.

## Authorization and objective

Approve this named plan to send one private, pseudonymous installation signal to Dieta Bot's API. It is not account authentication or a device fingerprint. Follow the Android feature/architecture/Kotlin skills within this approved scope, one agent.

## Sources

[Identity specification](../../specifications/identity-and-audit.md), [ADR-025](../../adrs/ADR-025-safety-correlation-audit.md), [Android context](../../../android/README.md), [API contract](../../../api-contract.md).

## Implementation

1. Add an injected installation-ID provider in the data/core infrastructure boundary. Generate a secure UUID v4 once, persist atomically in app-private `noBackupFilesDir`, and avoid disk work on the UI thread. Concurrent first requests must share one value. A corrupt/missing file creates a fresh ID; log only a fixed diagnostic code.
2. Send `X-Client-Instance-Id` through the existing network stack for estimate, fit, chat and compaction. Restrict it to the configured API origin and prevent forwarding to another origin on redirects. Do not attach it to arbitrary image URLs or unrelated clients.
3. Preserve across updates/process death and daily wipe. Clear-data/uninstall resets it; dev/prod remain isolated. No setting, new user permission, Room schema migration, DataStore for day state, Firebase identity or advertising identifier.
4. Keep the existing `X-Invite` and request-ID behavior. Startup/ID-storage failure uses a bounded failure path; it never leaks a raw device identifier as fallback. Old servers may ignore the additive header.
5. Verify existing UI treatment of CP2/CP3 responses: refusal bubble has no action card/memory update; 400/503 do not display raw internal codes or retry indefinitely. This plan may adjust nonvisual response mapping in the existing data/domain flow if needed; any new layout/control requires a separate approved plan and Stitch gate.

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

## Results (2026-10-01)

Automation executed from `apps/android/`: `./gradlew.bat :app:testDevDebugUnitTest :app:assembleDevRelease` → 373 tests, 0 failures, 0 errors; `app-dev-release.apk` built. New: 17 tests (`InstallationIdTest`, `InstallationIdInterceptorTest`) and 2 Chat checks in `ChatViewModelTest`. No composable or response rendering changed, so `verifyRoborazziDevDebug`, emulator captures and a gold diff were not required and no visual completion is claimed.

| Check | Result |
| --- | --- |
| First use: one canonical lowercase UUID v4, written atomically (`.new` + fsync + `ATOMIC_MOVE`) | PASS |
| 64 concurrent first calls on 16 threads share one value, one generation | PASS |
| Provider recreated (restart/update) reads the same id | PASS |
| Missing file (clear-data) → new id; corrupt file (empty, garbage, uppercase, v1) → fresh canonical id | PASS |
| Write failure → random id kept for the process only, never a device identifier; fixed log codes only | PASS |
| Separate app-private dirs (dev/prod) never share an id | PASS (`noBackupFilesDir` is per applicationId) |
| Lives in `noBackupFilesDir`, not `filesDir`; daily wipe (Room rows only) keeps it; `allowBackup="false"` kept | PASS (Robolectric) |
| Header on `/v1/estimate`, `/v1/fit`, `/v1/chat` (compact uses `/v1/chat`) with `X-Invite` and `X-Request-Id` intact | PASS — real loopback HTTP server |
| `/health`, another port, `localhost` vs configured `127.0.0.1`, other scheme or base path → no header | PASS |
| Cross-origin 307 → no header on the other origin; same-origin redirect keeps it; a caller-set header to another origin is removed | PASS — real OkHttp redirect |
| Storage failure or non-canonical value → call proceeds without the header | PASS |
| Id absent from telemetry events, breadcrumbs, keys and non-fatals | PASS |
| Legacy server ignoring the header still answers 200 | PASS |
| CP2 refusal (200, fixed reply, `intent: question`, no estimate): plain bubble, no action card, no memory notice or memory write | PASS |
| HTTP 400 `invalid_client_instance_id` / 503: retry state, no raw code in UI state, no automatic retry | PASS — existing mapping, no code change needed |

Implementation notes:

- [`InstallationId.kt`](../../../../apps/android/app/src/main/java/com/nutri/android/core/network/InstallationId.kt): `FileInstallationId`, Hilt `@Singleton` binding. Lazy and synchronized; the first read/write happens on OkHttp's dispatcher thread, never the UI thread.
- [`InstallationIdInterceptor.kt`](../../../../apps/android/app/src/main/java/com/nutri/android/core/network/InstallationIdInterceptor.kt): an OkHttp network interceptor, so it runs per hop. Scope = configured `API_PUBLIC_URL` scheme, host and port, path under its `v1/`. It always strips an inbound header first.
- [`NetworkModule.kt`](../../../../apps/android/app/src/main/java/com/nutri/android/core/network/NetworkModule.kt) wires it after the existing invite and request-id interceptors; timeouts unchanged.
- No setting, permission, Room migration, DataStore, Firebase identity or advertising identifier. No server, infra, `version.properties` or signing change.

Manual pending (V15 emulator part, V11 CP4 part, V16 in CP5): on the distributed APK, restart/update keeps the id and a disposable fresh installation gets a different one, seen as the same/different `safety_identifier` in the dev log once CP5 provisions the secret. Not run here: the release APK is not debuggable, the identifier is only observable in the dev log after CP5, and no owner or tester installation was cleared. Distribution waits for the owner's test-build request (A16).
