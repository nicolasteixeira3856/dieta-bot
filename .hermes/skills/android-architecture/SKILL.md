---
name: android-architecture
description: Apply Fibrai's Android UI, domain and data boundaries when adding a screen, ViewModel, repository, use case or dependency injection.
---

# Android architecture

Read [AGENTS](../../../AGENTS.md), the [Android context](../../../docs/android/README.md) and the named approved plan before implementation. Use the existing feature-first packages; do not reorganize the app merely to match a generic example.

- Domain formulas are pure Kotlin. UI depends on screen/flow-scoped ViewModels and repositories, not DAOs or Retrofit.
- ViewModels expose immutable UiState through StateFlow; destinations collect with collectAsStateWithLifecycle. Child composables receive state and callbacks, not the whole ViewModel.
- Hilt provides dependencies. Keep the current Hilt kapt pipeline; Room entities/DAOs are Kotlin with KSP. Room's processor choice does not require removing kapt from unrelated processors.
- Read the [Room specification](../../../docs/android/specifications/room-v2.md); it states the current schema. Profile survives day rollover; daily state is keyed by America/Sao_Paulo. Historical ADR-010's Java/kapt implementation is superseded by the delivered [A0 refactor](../../../docs/android/plans/completed/a0-arch-refactor.md).
- Preserve existing typed Navigation Compose routes and native back behavior. Do not migrate to Navigation 3, introduce destinations or reset the entire backstack in response to state changes without an approved plan.
- Screen layouts follow their current Figma golds. Keep the Home FAB opening Chat, as specified in [ADR-012](../../../docs/produto/adrs/ADR-012-chat-home-perfil.md).

Use assembleDevRelease, testDevDebugUnitTest and verifyRoborazziDevDebug from apps/android. Aggregate build/test tasks can require unavailable prod configuration. Changed UI also requires fresh dark/light emulator captures and written gold comparisons; JVM results alone do not complete UI.

Official references: [architecture](https://developer.android.com/topic/architecture), [recommendations](https://developer.android.com/topic/architecture/recommendations), [Compose UDF](https://developer.android.com/develop/ui/compose/architecture).
