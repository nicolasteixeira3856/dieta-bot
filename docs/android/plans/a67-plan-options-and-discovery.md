# Plan — A67 Plan options in the bubble; discovery on the first opening

- Status: Aguardando aprovação
- Date: 08/10/2026
- Owning context: `android`
- Affected code: `apps/android/` only: plan bubble with one action group per option (Registrar, Reservar), persistence of option ids in `chat_message`, the `discovery` flag on the first opening, application of declared facts (`declared`, `equipment`, `liked`), tests, captures.
- Prerequisites: [S37](../../server/plans/s37-plan-options-and-discovery.md) on the dev server; [A66](a66-typed-actions-batch.md) delivered; a design plan for the option control (new gold `chatO`) `Concluído` — opened as D25 when A67 is scheduled, after [D23](../../design/plans/d23-release-2-recipes.md) and [D24](../../design/plans/d24-release-2-visible-memory.md).
- Related documentation: [ADR-051](../../produto/adrs/ADR-051-plan-option-identity-and-chat-discovery.md), ADR-046/048, [product Chat](../../produto/specifications/chat.md), [memoria-push](../../produto/specifications/memoria-push.md), [Room](../specifications/room-v2.md).

Authorization and delivery follow [SDD](../../sdd/README.md). Approval: `Aprovo o plano docs/android/plans/a67-plan-options-and-discovery.md. Implemente o plano aprovado.`

## Objective

Each option of a plan is a thing the user can record or reserve; the first opening asks the routines once and saves what the user declares.

## Scope

1. **Options.** The plan bubble renders `options[]` with the option name, items and totals, and the actions of ADR-048 per option; the chosen option is what Registrar or Reservar uses. Option ids persist with the message for later references.
2. **Discovery.** On the first Chat opening with an empty `FactMemory`, the request carries `discovery: true`; the answer renders as a normal bubble; the user's answer goes through the normal turn; the `memory_updates` with `declared: true` are applied by `MemoryRules` without counting a recorded day; `equipment` and `liked` categories stored and listed.
3. **Tests and captures.** Unit tests of option persistence and `MemoryRules` for declared routines; `capture-chat.sh` scenes `a67` (options, "fiz a 2") and the first-opening flow; new gold `chatO` compared.

### Specification changes at Completion

- [product Chat](../../produto/specifications/chat.md), [memoria-push](../../produto/specifications/memoria-push.md), [Room](../specifications/room-v2.md).

## Out of scope

- O6 onboarding screen (conditional, ADR-053 § 4). Recipes: A68.

## Validation

1. `testDevDebugUnitTest` and `verifyRoborazziDevDebug` pass.
2. Captures vs the golds including `chatO`; diff list in Results. Only the screens this plan touches (Delivery pace rule): never the whole flow, never a full capture run; the JVM regression covers the rest.
3. Device smoke on dev (dev build shipped from `develop`): open request, "fiz a 2", fresh install discovery and a routine copied on the next day.
4. `node tools/check-docs.mjs` passes.

## Results

<Filled at Completion.>
