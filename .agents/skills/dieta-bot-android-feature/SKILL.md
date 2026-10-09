---
name: dieta-bot-android-feature
description: Implement a named approved Fibrai Android plan for a screen, domain rule, API client or local persistence, within apps/android.
---

# Fibrai Android implementation

Read [AGENTS](../../../AGENTS.md), [SDD](../../../docs/sdd/README.md), the [Android context](../../../docs/android/README.md) and the named approved plan. Planning alone does not authorize code. Check any prerequisite design plan is completed before layout work.

Keep server implementation outside this client delivery. Use domain rules, Room/repository boundaries, screen-scoped ViewModel + UiState, and Compose as needed by the feature; do not introduce layers or tests without a relevant behavior to validate.

- Structured profile/day/meal state uses Room. DataStore is only a legacy import path; do not restore day-state JSON persistence.
- Photo processing follows [ADR-018](../../../docs/android/adrs/ADR-018-foto-2048.md): apply rotation, longest side at most 2048 px without upscaling, JPEG q85, EXIF stripped, 16 MB guard.
- Read [Chat behavior](../../../docs/produto/specifications/chat.md) and the [HTTP contract](../../../docs/api-contract.md). Client credentials are API_PUBLIC_URL + INVITE_CODE / X-Invite, never an OpenAI key.
- Follow [NetworkModule](../../../apps/android/app/src/main/java/app/fibrai/android/core/network/NetworkModule.kt): connect 20 s, read/write 60 s, overall call 65 s. Network failures use the Chat retry state; they are not low-confidence portion questions.
- Home/Chat day-one chips depend on their matching specifications and golds; do not ban every chip.
- Preserve flavor boundaries and dev-only telemetry. Events carry enums and numbers, never user text.

From apps/android, run assembleDevRelease, testDevDebugUnitTest and verifyRoborazziDevDebug as required by the plan. Changed UI additionally needs fresh dark/light emulator captures, measured bounds and a written comparison with matching Figma golds; use the visual skill.

Record actual results, pending manual validation and plan lifecycle before the required scoped git delivery. Do not declare completion from compilation alone or include unrelated changes.
