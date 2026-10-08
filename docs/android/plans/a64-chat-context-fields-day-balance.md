# Plan — A64 Chat context fields, day balance in the receipt, "Anotado" line, waiting state

- Status: Aguardando aprovação
- Date: 08/10/2026
- Owning context: `android`
- Affected code: `apps/android/` only: `PromptBuilder` (facts macros, `recent_days`), the receipt composable (balance line), `FactMemory` application ("Anotado: …"), the Chat waiting state, unit tests, captures.
- Prerequisites: [S33](../../server/plans/pending_manual_validation/s33-chat-context-effort-low.md) on the dev server (the fields are optional: the app may ship first). Figma gate: none if the balance line and the waiting state fit the current golds (`chatF`, `chatQ`); otherwise a design plan before this one.
- Related documentation: [ADR-054](../../server/adrs/ADR-054-chat-reasoning-effort-low.md) (waiting state), [ADR-053](../../produto/adrs/ADR-053-visible-memory-screen.md) § 2 ("Anotado"), [ADR-023](../../produto/adrs/ADR-023-chat-v2-memoria-v2.md), [product Chat](../../produto/specifications/chat.md), [HTTP contract](../../api-contract.md); brainstorm conclusion `benchmark/MELHORIAS_CHAT_07_10_2026.md` § 5.4 Plano 2.

Authorization and delivery follow [SDD](../../sdd/README.md). Approval: `Aprovo o plano docs/android/plans/a64-chat-context-fields-day-balance.md. Implemente o plano aprovado.`

## Objective

The app sends what the server now reads in code (routine macros, seven-day totals) and shows the user what the model is not asked to compute: the day balance after a record and the fact it saved.

## Scope

1. **Request fields.** `facts[].kcal/p/c/g` from `FactMemory` for routines; `recent_days[]` for the last seven days from Room (kcal, P, C, G, effective ceiling of that day with its own workout credit, recorded flag, slot that went furthest over its expected size, slots without record), reusing `Closures` and correcting its ceiling per day.
2. **Balance line in the receipt.** After a record the receipt shows one line computed from Room: `{eaten} de {effective ceiling} kcal · faltam {p} g de proteína`. A pending estimate (ask mode) shows the projection with the label `projeção`. Within the current gold of the receipt (`chatF`); if the line does not fit, stop and open a design plan.
3. **"Anotado" line.** When a permanent fact from an explicit statement is applied, the thread shows `Anotado: {fact text}` read back from `FactMemory` (ADR-053 § 2).
4. **Waiting state.** After 4 s without answer the composer area shows the waiting copy (`Tali está pensando…`) until the answer or the timeout; the client timeout covers the server's 25 s (ADR-054 § 3). Within `chatQ`/`chatE` if no new state is needed.
5. **Tests and captures.** Unit tests of `PromptBuilder` (fields present, absent for a fact without macros) and of the balance; `capture-chat.sh` scenes for the receipt and the waiting state; JVM `GoldTest` unchanged ids.

### Specification changes at Completion

- [product Chat](../../produto/specifications/chat.md): balance line, Anotado line, waiting state. [HTTP contract](../../api-contract.md): fields sent by the client.

## Out of scope

- Actions, workout, options, recipes, memory screen: A65–A69. Server rules: S33.

## Validation

1. `testDevDebugUnitTest` and `verifyRoborazziDevDebug` pass.
2. Captures of the touched flows vs the golds (partial validation rule), diff list in Results. Only the screens this plan touches (Delivery pace rule): never the whole flow, never a full capture run; the JVM regression covers the rest.
3. Three-turn smoke on the dev app (dev build shipped from `develop`) against the S33 server: a routine with macros is copied; "mesmo almoço de ontem" copies the record.
4. `node tools/check-docs.mjs` passes.

## Results

<Filled at Completion.>
