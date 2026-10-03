---
name: room-ksp-coroutines
description: Implement Dieta Bot Room persistence with Kotlin entities, KSP, coroutines and Flow during an approved schema, DAO or repository change.
---

# Room persistence

Read [AGENTS](../../../AGENTS.md), the [Android context](../../../docs/android/README.md), [Room specification](../../../docs/android/specifications/room-v2.md), which states the current schema. Confirm it against the current database class and exported schemas before choosing a version or table shape.

- Room 2.6.x entities, DAOs and converters are Kotlin; Room's compiler uses KSP. Hilt currently uses kapt separately; do not remove that processor as part of a Room change.
- Profile is durable across calendar days. Workout/day/meal state is date-keyed in America/Sao_Paulo; daily workout values do not belong in an illustrative profile entity.
- DAOs expose observable Flow for queries and suspend writes where appropriate. UI goes through repositories, not DAOs.
- Use transactions for coupled writes and the current meal-replacement/wipe rules; do not restore the historical append-only behavior when accepted successors require consolidation.
- Schema changes need explicit migrations and exported schemas. Preserve existing data; do not use destructive fallback to hide migration failures.
- Keep domain models/formulas independent of Android, Room and Compose. Converters are Kotlin and must preserve their actual serialization/date contracts.

DataStore remains a legacy import path, not structured day persistence. Test meaningful migration, rollover and transaction behavior within the plan using dev checks. This skill does not authorize a new schema version or migration on its own.
