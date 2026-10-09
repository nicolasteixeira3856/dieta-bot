# Plan — A70 Decision line, fit and day projection per option in the plan bubble

- Status: Aguardando aprovação
- Date: 09/10/2026
- Owning context: `android`
- Affected code: `apps/android/` only: plan bubble with options (`ChatViewModel` option views and text split, `ChatScreen` option block, `ChatUiState`), `PlanBudget` on the chosen option, `DayBalance` projection per option, `tools/fake-chat-server.mjs` mode, `tools/capture-chat.sh` scene, tests, captures.
- Prerequisites: [S39](../../server/plans/pending_manual_validation/s39-plan-decision-line-and-option-budget.md) on the dev server; [A67](completed/a67-plan-options-and-discovery.md) delivered; [D26](../../design/plans/completed/d26-release-2-option-decision.md) `Concluído` (gold `chatO` redrawn).
- Related documentation: [ADR-056](../../produto/adrs/ADR-056-plan-decision-line-and-option-budget.md), [ADR-039](../../produto/adrs/ADR-039-plan-cooking-and-budget-choice.md), [ADR-051](../../produto/adrs/ADR-051-plan-option-identity-and-chat-discovery.md), [product Chat](../../produto/specifications/chat.md), [HTTP contract](../../api-contract.md).

Authorization and delivery follow [SDD](../../sdd/README.md). Approval: `Aprovo o plano docs/android/plans/a70-option-fit-and-projection.md. Implemente o plano aprovado.`

## Objective

The plan bubble shows the decision and, under each option, whether it fits the window of its meal and how the day closes with it; the budget note and the day panel follow the option the server chose.

## Scope

1. **Chosen option.** The plan row's `estimate` (the chosen option, S39) drives the day panel, the `Passa {over} kcal do que sobra.` note and the Pode passar / Ajustar choice (ADR-039); Registrar and Reservar per option keep A67's path (the tapped option becomes the estimate).
2. **Per-option lines (`chatO`, ADR-056 § 8).** Under each option's totals, in the caption style of the budget note: `Cabe na janela do {meal}` when `over_kcal` is 0, else `Passa {n} kcal da janela do {meal}` ({meal} = the plan's slot name); and `Dia: ~{kcal} de {ceiling} kcal · P {p} de {target}` computed by `DayBalance` as the A64 projection with that option (eaten + option against the effective ceiling and the protein target). Without `over_kcal` (older server) no fit line.
3. **Lead and trailing text.** The bubble shows the reply's lines before the first `Opção {n}` paragraph above the blocks and the lines after the last one (critique, skip clause, closing lines) below them, markers kept as in ADR-045; only the `Opção {n}` paragraphs are left to the blocks. A reply with no such paragraph shows whole, above.
4. **Tests.**
   - `ChatOptionsRenderTest` (JVM): replays the S39 fixture `app/src/test/resources/fixtures/s39-comparacao-hamburguer.json` (the request and the response of the synthetic comparison case, copied from the S39 run) through the real mapper (`ChatViewModel` → `ChatItem.Assistant`) and **prints the bubble as the app shows it** to the test output: lead text, each option block (title, items, totals, fit line, day line, action labels), trailing text, budget note. It asserts the first line is the decision line, the fit line of the chosen option says `Cabe`, the budget note is absent for a chosen option that fits, and the day line of each option equals `DayBalance.projection` with that option.
   - `PlanBudgetTest` / `DayBalanceTest`: the chosen option and the projection per option.
   - `GoldTest.chatO_dark` / `chatO_light` against the D26 gold.
5. **Captures.** `tools/fake-chat-server.mjs` mode `{"fixture": "<path>"}` answers the fixture's response; `tools/capture-chat.sh` scene `a70` seeds the fixture's day and captures `chatO` in both themes; diff list in Results.

### Specification changes at Completion

- [product Chat](../../produto/specifications/chat.md) rule 25 (lead/trailing text, fit and day lines, chosen option) and rule 6 (projection also per option); Provenance.

## Out of scope

- The server rules and arithmetic ([S39](../../server/plans/pending_manual_validation/s39-plan-decision-line-and-option-budget.md)); the gold ([D26](../../design/plans/completed/d26-release-2-option-decision.md)); a projection of the remaining meals (the closing lines cover them); any change to Registrar, Reservar or the receipts.

## Validation

1. `testDevDebugUnitTest` and `verifyRoborazziDevDebug` pass; the printed bubble of `ChatOptionsRenderTest` is pasted in Results as the owner asked (how the S39 answer appears in the app).
2. Captures vs the `chatO` gold only (Delivery pace rule); diff list in Results.
3. Device smoke on dev (dev build shipped from `develop`, at the end of the batch): one comparison with a skip in the same message; the decision line, the fit lines and the budget note agree.
4. `node tools/check-docs.mjs` passes.

## Results

<Filled at Completion: test counts, the printed bubble of the comparison fixture, the `chatO` diff list, captures.>
