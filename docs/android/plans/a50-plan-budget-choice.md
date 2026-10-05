# Plan — A50 Plan over budget: the choice in the Chat

- Status: Aguardando aprovação
- Date: 05/10/2026
- Owning context: `android`
- Executable boundary: `apps/android/` (Chat UI/ViewModel, network DTOs, Room entity/migration, telemetry, tests) plus the Chat QA tooling `tools/fake-chat-server.mjs` and `tools/capture-chat.sh`.
- Related documentation: [product Chat](../../produto/specifications/chat.md) rule 16, [Room](../specifications/room-v2.md), Android validation and the plan indexes. Fresh captures go to the existing QA folders.
- Prerequisites:
  - acceptance of [ADR-039](../../produto/adrs/ADR-039-plan-cooking-and-budget-choice.md); [S21](../../server/plans/s21-plan-cooking-and-budget-choice.md) delivered and deployed to the dev server;
  - [D12](../../design/plans/d12-plan-budget-choice.md) `Concluído` with `chatRB` exported;
  - [A47](pending_manual_validation/a47-chat-meal-updates.md) delivered (same Chat files and Room column family);
  - [A48](completed/a48-fibrai-app-id-firebase.md) and [A49](completed/a49-fibrai-tali-visible-rename.md) delivered or cancelled (package rename and visible copy; no parallel Android plan).

Authorization and delivery follow [SDD](../../sdd/README.md). Approval: `Aprovo o plano docs/android/plans/a50-plan-budget-choice.md. Implemente o plano aprovado.`

## Objective

When a plan's dish is over what is left of the day, the Chat asks once whether going over is fine or the dish should be adjusted, using the server's numbers, never the reply text.

## Sources

ADR-039 owns the behavior and copy. S21 owns the capability, then the [HTTP contract](../../api-contract.md). `chatRB` (D12) and `chatR` own the layout. Read the delivered A47 code at start and rebase on the then-current master.

## Scope

### 1. Capability and data

- Send `plan_budget: true` on every normal Chat turn (with `clarify_rounds` and `auto_record`, as today). Decode the response `plan_budget` `{limit_kcal, over_kcal, reserved, choice}` with nullable defaults; an older server or a malformed object means no choice UI.
- Store it on the assistant row: a nullable JSON column on `chat_message` with the next Room version discovered at start, plus the local choice (`over_ok`) once **Pode passar** is tapped. Non-destructive migration, exported schema, migration test. Existing rows stay null.

### 2. When the choice shows

Only on the most recent plan bubble of today, when `over_kcal` > 0, `limit_kcal` ≥ 1 and the stored choice is not `over_ok`. Then, below the bubble (`chatRB`):

- `Passa {over_kcal} kcal do que sobra.`; for each reservation, `Reservei {kcal} kcal para {label}.`
- Pills **Pode passar** · **Ajustar para caber** in the action position, in place of **Registrar assim**.

Otherwise the plan is `chatR` as today, including the projected day in `bad` when over. With `limit_kcal` < 1 there is nothing to adjust to: no line, no pills, `chatR`. A new send, a day change or a wipe ends the choice, like **Forçar estimativa**.

### 3. Actions

- **Pode passar:** no request. Store `over_ok` on the row; the pills leave and **Registrar assim** returns. Survives recreation.
- **Ajustar para caber:** sends `Ajusta para caber em {limit_kcal} kcal.` with `fit_kcal = limit_kcal` through the normal send/loading/failure/retry flow; retry keeps `fit_kcal`. The answer is a normal plan: `chatR` when it fits, the choice again when the server returns it still over.
- A typed answer needs nothing special: the server's `choice` drives the next response.
- **Registrar assim** keeps its rule-16 behavior once shown; recording an over-budget plan after **Pode passar** is allowed.

### 4. Telemetry

One event with enums and numbers only: the choice (`over_ok` | `fit`) and `over_kcal`. No text.

### 5. QA tooling

`tools/fake-chat-server.mjs` gains a `{"plan_budget": ...}` switch returning fits, over with a reservation, over still after fit, and `limit_kcal` ≤ 0. `tools/capture-chat.sh` gains `SCENES=a50`: onboarding, an over-budget plan (`chatRB`), **Pode passar**, **Ajustar** to an adjusted plan, and recreation with a pending choice.

### Intended specification changes

At Completion: product Chat rule 16 gains the choice (when it shows, copy, actions, expiry) and `chatRB` in the state list; Room gains the column and version; Provenance links. The gold inventory stays owned by [qa](../../qa/README.md).

## Out of scope

Server, prompt or contract changes; Figma or gold edits; the Home budget and `reservedUpcoming`; any change to how a plan is recorded; production distribution.

## Validation

1. DTO and ViewModel tests: response with and without `plan_budget`; malformed fields; every show/hide condition above; **Pode passar** persisted and surviving recreation; **Ajustar** sends the text and `fit_kcal` once, retry keeps it; still-over response shows the choice again; expiry on send, day change and wipe.
2. Room migration test from the then-current version with chat rows, receipts and A47 proposals intact.
3. Emulator, dev flavor, against the fake server: fresh `chatRB` captures in `docs/qa/android/current/{dark,light}/` compared with the D12 gold under the QA rules of AGENTS; `chatR` regression after **Pode passar** and after an adjusted plan. Partial validation: only the flows this plan touches.
4. One real turn through the dev server (S21 deployed): an over-budget recipe shows the choice; **Ajustar** returns a plan at or under the limit or the choice again. Request ids recorded, no user content committed.
5. `testDevDebugUnitTest`, `verifyRoborazziDevDebug` and `assembleDevRelease` pass; `node tools/check-docs.mjs` passes.

## Results

Planning only.
