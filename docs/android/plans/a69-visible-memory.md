# Plan — A69 "O que a Tali sabe": memory screen in Config

- Status: Aguardando aprovação
- Date: 08/10/2026
- Owning context: `android`
- Affected code: `apps/android/` only: Config entry and screen (`memL`), fact list with origin, delete and correct, tombstones for deleted facts (Room v15), removal of the dev tool A23, tests, captures.
- Prerequisites: [D24](../../design/plans/completed/d24-release-2-visible-memory.md) `Concluído`; [A68](pending_manual_validation/a68-saved-recipes.md) delivered (Room version order).
- Related documentation: [ADR-053](../../produto/adrs/ADR-053-visible-memory-screen.md), ADR-019/023/029, [memoria-push](../../produto/specifications/memoria-push.md), [Room](../specifications/room-v2.md), [gold inventory](../../qa/README.md).

Authorization and delivery follow [SDD](../../sdd/README.md). Approval: `Aprovo o plano docs/android/plans/a69-visible-memory.md. Implemente o plano aprovado.`

## Objective

The user sees every fact Tali keeps, with its origin, and can delete or correct it; a deleted fact never comes back from a digest.

## Scope

1. **Screen.** Config → O que a Tali sabe (`memL`): facts grouped by kind (fixas, rotinas, temporárias), each with category, slot, text, macros, origin (`declarado`, `registrado N dias`, `até {date}`) and date; actions delete (confirmation) and correct (text field, same category and slot).
2. **Tombstones.** A deleted fact's key is kept with a deletion date; `MemoryRules` rejects a `memory_updates` add with the same key until the next compaction; digests never recreate it.
3. **Dev tool.** `Memória da IA (dev)` (A23) removed from the dev flavor; `hide_dev_tools` keeps its meaning.
4. **Tests and captures.** Unit tests of tombstones and correction; migration test; capture of `memL` vs the gold; `capture-config.sh` updated.

### Specification changes at Completion

- [memoria-push](../../produto/specifications/memoria-push.md) (screen, tombstones), [Room](../specifications/room-v2.md) (v15), [gold inventory](../../qa/README.md), [ADR-019](../../produto/adrs/ADR-019-ferramentas-dev.md) status line.

## Out of scope

- O6 onboarding (conditional). Goal weight ([A56](out_of_scope/a56-goal-weight.md), deferred).

## Validation

1. `testDevDebugUnitTest` and `verifyRoborazziDevDebug` pass.
2. Capture of `memL` vs the gold; diff list in Results. Only the screens this plan touches (Delivery pace rule): never the whole flow, never a full capture run; the JVM regression covers the rest.
3. Device smoke on dev (dev build shipped from `develop`): delete a routine, send "café de sempre", the routine is not recreated; correct a preference and see it in the next prompt.
4. `node tools/check-docs.mjs` passes.

## Results

<Filled at Completion.>
