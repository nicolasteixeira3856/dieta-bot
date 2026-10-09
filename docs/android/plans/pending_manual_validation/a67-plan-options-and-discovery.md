# Plan — A67 Plan options in the bubble; discovery on the first opening

- Status: Pendente aprovação manual (approved 08/10/2026 in the owner's batch message A64–A69; code, tests, the `chatO` gold comparison and emulator checks done; the device smoke on a dev build is the owner's manual acceptance)
- Date: 08/10/2026
- Owning context: `android`
- Affected code: `apps/android/` only: plan bubble with one action group per option (Registrar, Reservar), persistence of option ids in `chat_message`, the `discovery` flag on the first opening, application of declared facts (`declared`, `equipment`, `liked`), tests, captures.
- Prerequisites: [S37](../../../server/plans/pending_manual_validation/s37-plan-options-and-discovery.md) on the dev server; [A66](a66-typed-actions-batch.md) delivered; [D25](../../../design/plans/completed/d25-release-2-plan-options.md) `Concluído` (option control, gold `chatO`).
- Related documentation: [ADR-051](../../../produto/adrs/ADR-051-plan-option-identity-and-chat-discovery.md), ADR-046/048, [product Chat](../../../produto/specifications/chat.md), [memoria-push](../../../produto/specifications/memoria-push.md), [Room](../../specifications/room-v2.md).

Authorization and delivery follow [SDD](../../../sdd/README.md). Approval: `Aprovo o plano docs/android/plans/a67-plan-options-and-discovery.md. Implemente o plano aprovado.`

## Objective

Each option of a plan is a thing the user can record or reserve; the first opening asks the routines once and saves what the user declares.

## Scope

1. **Options.** The plan bubble renders `options[]` with the option name, items and totals, and the actions of ADR-048 per option; the chosen option is what Registrar or Reservar uses. Option ids persist with the message for later references.
2. **Discovery.** On the first Chat opening with an empty `FactMemory`, the request carries `discovery: true`; the answer renders as a normal bubble; the user's answer goes through the normal turn; the `memory_updates` with `declared: true` are applied by `MemoryRules` without counting a recorded day; `equipment` and `liked` categories stored and listed.
3. **Tests and captures.** Unit tests of option persistence and `MemoryRules` for declared routines; `capture-chat.sh` scenes `a67` (options, "fiz a 2") and the first-opening flow; new gold `chatO` compared.

### Specification changes at Completion

- [product Chat](../../../produto/specifications/chat.md), [memoria-push](../../../produto/specifications/memoria-push.md), [Room](../../specifications/room-v2.md).

## Out of scope

- O6 onboarding screen (conditional, ADR-053 § 4). Recipes: A68.

## Validation

1. `testDevDebugUnitTest` and `verifyRoborazziDevDebug` pass.
2. Captures vs the golds including `chatO`; diff list in Results. Only the screens this plan touches (Delivery pace rule): never the whole flow, never a full capture run; the JVM regression covers the rest.
3. Device smoke on dev (dev build shipped from `develop`): open request, "fiz a 2", fresh install discovery and a routine copied on the next day.
4. `node tools/check-docs.mjs` passes.

## Results

Implemented 08/10/2026 on `feat/a67-plan-options-discovery`, in the autonomous run of A64–A69 ([report](../../validation/batch-2026-10-08-a64-a69.md)).

### Delivered

- **Options (`chatO`).** A plan row whose own action carries two or more `options` (each id, name and estimate) shows the reply's first line, then one `Chat/PlanOption` block per option: `Opção {n}: {name}`, one item per line with its grams (`{g} g de {item}`; an item that starts with a count: `{item} ({g} g)`), `~{kcal} kcal · {p}P · {c}C · {g}G` in the macro colours and, while the plan is open, **Registrar** | **Reservar** side by side (Reservar only for a meal of today with nothing eaten); then the day panel with option 1. Nothing under the bubble (no Registrar assim, no Reservar para o {slot}). A tap writes the chosen option as the plan row's estimate (`ChatMessageDao.setEstimate`) and takes the plan's own record or reservation path, so Substituir, Trocar, the receipt and the reservation use the option's numbers. Telemetry `plan_option` (`record` | `reserve`).
- **Option ids persist** with the message without a new Room version: since A66 the first row of an answer stores the whole `actions` list and, from A67 on, every later row stores its own action (`ActionParts.rowActions`); the options are read from the row's own action. "fiz a 2" and "trava a 1" are server actions (S37) that the app applies as any log or reservation.
- **Discovery.** `discovery: true` while the memory is empty, on the first answer the app ever stores and the next one (`DISCOVERY_TURNS`, `ChatMessageDao.assistantCount`); the answer renders as a normal bubble.
- **Memory.** `ChatMemoryUpdate` and `MemoryUpdate` carry `kcal/p/c/g` and `declared`. Categories `equipment` (permanent, no slot) and `liked` (its slot and numbers) are stored and sent in `facts`. A declared routine applies at once (it never waits for a record), keeps the numbers the model estimated, has source `declared` and no recorded day, and lives 21 days from its creation (`MemoryRules.expire`). The stored memory JSON gains `declared` (default false; older files read as false).

### Validation

1. `testDevDebugUnitTest` + `verifyRoborazziDevDebug`: 702 tests, 0 failures. New: `ChatPlanOptionsTest` (5: option blocks with actions and no bar, lead line only, ids stored; Registrar on option 2 records 360 kcal and its text; Reservar on option 2 reserves 360 kcal; discovery on the first two turns with a declared routine with numbers and no day, equipment permanent, no discovery once facts exist, the routine sent with its numbers, alive on day 21 and gone on day 22; a liked dish with its slot and numbers) and `GoldTest.chatO_dark` / `chatO_light` (new, pass after setting the item rows 28 dp apart). `FactMemoryTest` expects the new `declared` key in the stored format.
2. Emulator (Medium_Phone 780 × 1688 @ 320, devDebug against the fake, `SCENES=a67`, both themes): every check passed — two option blocks with their actions and no Registrar assim, option 2 recorded (Jantar 360) and reserved (`planned_meal` 360), discovery true on the greeting and on the answer, the questions as a list, nothing logged, discovery false once the memory has facts, facts `air fryer` and `cafe` sent. `node tools/diff-gold.mjs` `chatO`: header 0.00 % / 0.15 %, thread tail 0.76 % / 1.24 % (pass). Flows left out (Delivery pace rule): the other Chat scenes, Home, Config, onboarding.
3. Device smoke on a dev build: **not run** (owner decision of 08/10/2026: no test build per plan, one build at the end); moved to the manual acceptance.
4. `node tools/check-docs.mjs` passes.

### Manual acceptance (after delivery)

- On a dev build: "o que eu janto?" shows two options with Registrar and Reservar each; "fiz a 2" records option 2 with its numbers; on a fresh install the first "oi" asks the routines and the answer is remembered; the next day the declared breakfast is copied with its numbers.

