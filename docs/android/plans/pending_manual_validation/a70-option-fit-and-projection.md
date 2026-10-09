# Plan — A70 Decision line, fit and day projection per option in the plan bubble

- Status: Pendente aprovação manual (approved 09/10/2026: "Aprovo o plano docs/android/plans/a70-option-fit-and-projection.md. Implemente o plano aprovado."; code, JVM tests, the `chatO` gold comparison and the emulator scene done; the device smoke on a dev build is the owner's manual acceptance)
- Date: 09/10/2026
- Owning context: `android`
- Affected code: `apps/android/` only: plan bubble with options (`ChatViewModel` option views and text split, `ChatScreen` option block, `ChatUiState`), `PlanBudget` on the chosen option, `DayBalance` projection per option, `tools/fake-chat-server.mjs` mode, `tools/capture-chat.sh` scene, tests, captures.
- Prerequisites: [S39](../../../server/plans/pending_manual_validation/s39-plan-decision-line-and-option-budget.md) on the dev server; [A67](../completed/a67-plan-options-and-discovery.md) delivered; [D26](../../../design/plans/completed/d26-release-2-option-decision.md) `Concluído` (gold `chatO` redrawn).
- Related documentation: [ADR-056](../../../produto/adrs/ADR-056-plan-decision-line-and-option-budget.md), [ADR-039](../../../produto/adrs/ADR-039-plan-cooking-and-budget-choice.md), [ADR-051](../../../produto/adrs/ADR-051-plan-option-identity-and-chat-discovery.md), [product Chat](../../../produto/specifications/chat.md), [HTTP contract](../../../api-contract.md).

Authorization and delivery follow [SDD](../../../sdd/README.md). Approval: `Aprovo o plano docs/android/plans/a70-option-fit-and-projection.md. Implemente o plano aprovado.`

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

- [product Chat](../../../produto/specifications/chat.md) rule 25 (lead/trailing text, fit and day lines, chosen option) and rule 6 (projection also per option); Provenance.

## Out of scope

- The server rules and arithmetic ([S39](../../../server/plans/pending_manual_validation/s39-plan-decision-line-and-option-budget.md)); the gold ([D26](../../../design/plans/completed/d26-release-2-option-decision.md)); a projection of the remaining meals (the closing lines cover them); any change to Registrar, Reservar or the receipts.

## Validation

1. `testDevDebugUnitTest` and `verifyRoborazziDevDebug` pass; the printed bubble of `ChatOptionsRenderTest` is pasted in Results as the owner asked (how the S39 answer appears in the app).
2. Captures vs the `chatO` gold only (Delivery pace rule); diff list in Results.
3. Device smoke on dev (dev build shipped from `develop`, at the end of the batch): one comparison with a skip in the same message; the decision line, the fit lines and the budget note agree.
4. `node tools/check-docs.mjs` passes.

## Results

Delivered on 09/10/2026.

- **Chosen option (scope 1).** No mapping change was needed: the plan row already stores the action's `estimate` and `plan_budget`, which S39 sets to the chosen option, so the day panel and the `Passa {over} kcal` note follow it. One change: a plan with options used to hide every action under the bubble (A67); now the **Pode passar** | **Ajustar para caber** choice stays when the chosen option is over its window (`ChatViewModel` `shownActions`), as ADR-056 § 8 asks. Registrar and Reservar per option are unchanged.
- **Per-option lines (scope 2).** `ChatActions.options` reads `over_kcal` (null from an older server: no fit line); `DayBalance.optionFit` and `DayBalance.optionDay` write `Cabe na janela do {meal}` / `Passa {n} kcal da janela do {meal}` and `Dia: ~{kcal} de {ceiling} kcal · P {p} de {target}` (eaten + option against the effective ceiling, the same base as the plan's day panel, so a recorded plan is not counted twice). `OptionView.fit` / `day`; the `Chat/PlanOption` block draws them under the totals in Caption `text/muted`, 2 dp apart, with 1 dp more above and below than the block's 8 dp gap (measured against the D26 gold: without it each block was 2 dp shorter).
- **Lead and trailing text (scope 3).** `ReplyMarkup.splitOptions` mirrors S39's `evals/preview.py`: lead = the lines before the first `Opção {n}` title; the last option paragraph runs over its `- ` / `|` lines and its total (`… kcal ·`), and a closing line ends it; trailing = the rest. `ChatItem.Assistant.text` is the lead and `trailing` the rest, both drawn with the ADR-045 blocks when they carry markers, without the macro colours (the D26 gold draws them in `text/primary`). Past-day plans with options now show their blocks too (no actions, no day line). Copiar takes the lead and the trailing text.
- **Tests.** `testDevDebugUnitTest` 701 tests, 0 failures; `verifyRoborazziDevDebug` passes (no baseline changed). New: `ChatOptionsRenderTest` (3: the S39 fixture `app/src/test/resources/fixtures/s39-comparacao-hamburguer.json`, copied from `logs/evals/fixtures/`, replayed through `ChatViewModel` with the fixture's day seeded in Room; a chosen option over its window shows the note and the choice; without `over_kcal` no fit line), `DayBalanceTest` (3), `ReplyMarkupTest` (+3 split cases). `GoldTest.chatO_dark` / `chatO_light` at the D26 frame height (1348 dp): blurred 0.51 % / 0.55 % (were 9.66 % / 9.79 % before A70).
- **The S39 answer as the app shows it** (`ChatOptionsRenderTest` output; ceiling 2200, P 150, café and almoço eaten 1250 · P 85):

```text
Vai de Hambúrguer com batata rústica: ~680 kcal · P 39 g, cabe na janela do Jantar. Hambúrguer duplo caprichado passa ~130 kcal.

  │ Opção 1: Hambúrguer duplo caprichado
  │ • 240 g de patinho grelhado
  │ • 75 g de pão de hambúrguer
  │ • 30 g de queijo
  │ • 15 g de molho
  │ • 50 g de salada
  │ ~860 kcal · 68P · 59C · 39G
  │ Passa 130 kcal da janela do Jantar
  │ Dia: ~2.110 de 2.200 kcal · P 153 de 150
  │ [Registrar] [Reservar]

  │ Opção 2: Hambúrguer com batata rústica
  │ • 120 g de patinho grelhado
  │ • 75 g de pão de hambúrguer
  │ • 20 g de queijo
  │ • 15 g de molho
  │ • 50 g de salada
  │ • 100 g de batata rústica
  │ ~680 kcal · 39P · 62C · 29G
  │ Cabe na janela do Jantar
  │ Dia: ~1.930 de 2.200 kcal · P 124 de 150
  │ [Registrar] [Reservar]

1. Asse a batata a 220 °C por 25–30 min, virando na metade.
2. Grelhe o patinho em frigideira quente até o ponto desejado; derreta o queijo sobre ele.
3. Monte no pão com o molho e a salada.
Lanche de hoje fora.
O jantar passou do teto no fim de semana: sábado e domingo. Ainda faltam 65 g de proteína; ajuste a ceia para priorizar proteína.
Ceia: iogurte natural com whey ~270 kcal · P 26

[Dia: 1250 → 1930 de 2200 kcal · P 124/150]
```

  No budget note and no choice: the chosen option (o2) fits its 730 kcal window. Below the bubble the app shows the receipt `Pulado Lanche` of the same answer's skip.
- **Captures (scope 5).** `tools/fake-chat-server.mjs` mode `{"fixture": "<path>"}` (slot ids mapped by name); `tools/capture-chat.sh` scene `a70` (the `chatO` seed now carries the D26 content and is shared with `a67`). Emulator Medium_Phone 780 × 1688 @ 320, devDebug against the fake, both themes: every check passed (`chatO`: the decision line, `Passa 20` / `Cabe`, the day lines, the trailing text, no budget note; the fixture: the decision line, `Passa 130 kcal da janela do Jantar`, `Dia: ~2.110 …`, `Cabe na janela do Jantar`, `Dia: ~1.930 de 2.200 kcal · P 124 de 150`, no budget note, the `Pulado Lanche` receipt).
- **`chatO` diff list** (`node tools/diff-gold.mjs dark/chatO light/chatO`): header 0.00 % / 0.15 %; thread tail dark 0.32 % (pass), light 5.71 % (over 2 %). The light tail excess is the page outside the bubble: the gold frame is 1348 dp tall and the phone 844 dp, so the page gradient and the decorative circles sit elsewhere (scored on the bubble columns only, x 40–660, the same tail is 0.45 %). Inside the bubble: layout, tokens, type sizes, radii, the option block, Registrar | Reservar, the semantic macro colours of the totals, the caption fit and day lines and the day panel on the chosen option (`1.640 → 2.000`) match. Differences ignored by rule: system clock and the bubble time (the seed's host time, 17:58, vs 20:15), font raster. The full-frame compare stays report-only (gold conflict), as for A67. Flows left out (Delivery pace rule): the other Chat scenes, Home, Config, onboarding; the JVM `GoldTest` of every id passes.
- `node tools/check-docs.mjs` passes.

## Manual acceptance (after delivery)

- On a dev build from `develop` (the batch's build), one comparison with a skip in the same message: the decision line on top, `Cabe` / `Passa {n} kcal da janela do {meal}` and the `Dia:` line under each option, the rest of the reply below the options, and the budget note only when the chosen option passes its window.
