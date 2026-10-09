---
name: kotlin-clean
description: Keep Fibrai Kotlin responsibilities, naming, coroutine ownership and API error boundaries clear during an approved implementation or refactor.
---

# Kotlin boundaries

Use [AGENTS](../../../AGENTS.md), [Android architecture](../android-architecture/SKILL.md) and the approved scope.

- English source identifiers/logs; pt-BR product copy. Give each class/file a cohesive responsibility.
- UI → screen-scoped ViewModel → domain/repository → service/DAO. A ViewModel does not call Retrofit directly; formulas remain pure domain Kotlin.
- Expose immutable UiState. Preserve coroutine ownership with viewModelScope and the current StateFlow subscription policy where appropriate; no GlobalScope.
- Model errors explicitly at API/repository boundaries with the existing result/sealed types. Do not swallow exceptions or force-unwrap network responses.
- [NetworkModule](../../../apps/android/app/src/main/java/app/fibrai/android/core/network/NetworkModule.kt) currently sets connect 20 s, read/write 60 s and overall call 65 s. Do not replace those distinct limits with a blanket 20 s timeout.
- [Chat specification](../../../docs/produto/specifications/chat.md) distinguishes failure/retry from estimate confidence. A network error must not manufacture a portion question or record a meal.
- Never log INVITE_CODE, OpenAI credentials or URLs containing secret query values. Keep .env out of commits.

Preserve the current stack and dependency versions. A style cleanup does not authorize product-copy changes, storage migrations or new screens.
