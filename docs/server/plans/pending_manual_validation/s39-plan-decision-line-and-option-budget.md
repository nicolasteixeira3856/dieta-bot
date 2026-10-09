# Plan — S39 Decision line of a plan with options; budget on the chosen option

- Status: Pendente aprovação manual (approved 09/10/2026: "Aprovo o plano docs/server/plans/s39-plan-decision-line-and-option-budget.md. Implemente o plano aprovado."; code, unit tests and docs done; the comparison on the dev app is the owner's manual acceptance)
- Date: 09/10/2026
- Owning context: `server`
- Executable boundary: `server/` only: `chat_instructions.py` (`plan` rule: DECISION LINE, COMPARISON, COOKING METHOD, ASSUMPTIONS, ONCE; `tone_duro` WEEK once a day; one pt-BR cue block `comparison`), `llm.py` / `shaping.py` (`options[].over_kcal`, chosen option), `main.py` / `meal_window.py` / `plan_budget.py` (same-message skip, chosen option, decision-line rewrite), `conversation_log.py` (`chosen`, `decision_line`), `evals/` (tag `s39`, `--show`, `--save-fixture`, `evals/preview.py`), tests.
- Related documentation: [ADR-056](../../../produto/adrs/ADR-056-plan-decision-line-and-option-budget.md) (accepted with this plan), [ADR-033](../../../content-policy/adrs/ADR-033-global-chat-example-provenance.md), [ADR-039](../../../produto/adrs/ADR-039-plan-cooking-and-budget-choice.md), [ADR-043](../../../produto/adrs/ADR-043-plan-objective-protein-and-meal-window.md), [ADR-047](../../../produto/adrs/ADR-047-skips-alongside-other-actions.md), [ADR-051](../../../produto/adrs/ADR-051-plan-option-identity-and-chat-discovery.md), [ADR-052](../../../produto/adrs/ADR-052-saved-recipes.md), [v1-chat](../../specifications/v1-chat.md), [HTTP contract](../../../api-contract.md).
- Prerequisites: [S37](s37-plan-options-and-discovery.md) and [S38](s38-saved-recipes.md) on the dev server.

Approving this plan accepts ADR-056. Authorization and delivery follow [SDD](../../../sdd/README.md). Approval: `Aprovo o plano docs/server/plans/s39-plan-decision-line-and-option-budget.md. Implemente o plano aprovado.`

## Objective

A plan with options opens with the decision and the numbers that support it, computed by the server with the meal the message skips out of the window; a comparison of dishes the user named keeps those dishes; a dish the user will cook gets its steps.

## Scope

1. **Prompt (`plan` and `tone_duro` rules).** General rules from ADR-056, no example from any conversation (ADR-033 record in Results):
   - DECISION LINE: a plan with options opens with one line in a fixed form the server can find, naming the chosen option and the reason: `Vai de {name}: ~{kcal} kcal · P {p} g, cabe na janela do {meal}. {other name} passa ~{n} kcal.` (or `… também cabe.` when both fit; `… passa ~{n} kcal da janela; {other} passa ~{m}.` when both pass). The options follow as ADR-051 presents them. The numbers are placeholders the model copies from BUDGET and its own estimates; the server rewrites them (scope 2).
   - COMPARISON: alternatives the user names with a choice question (cue block `comparison`: synthetic pt-BR markers of "A ou B, qual?") are a plan with options, each option the user's dish as named; no leaner/indulgent pair. The LATER REFERENCE rule applies unchanged.
   - COOKING METHOD: a plan that names an appliance or a preparation method for a dish the user will make follows COOKING: steps with temperature and time for the chosen option, `recipe` filled (S38); a plan that names none keeps no steps.
   - ASSUMPTIONS: components the message leaves open are completed from RECENT records of dishes of the same kind before a generic assumption; each assumption is said once, in one clause.
   - WEEK once a day (`tone_duro`): the weekly pattern is not repeated when an assistant turn of today in HISTORY already names it.
2. **Server arithmetic (ADR-056 § 2 and § 6).**
   - Same-message skip: the slots skipped by the answer's `skip` actions are treated as skipped by `_meal_window` and `_close_day` (reservation 0). Before generation, a meal of today named next to a skip cue in the current message (the existing skip lexicon) is left out of the BUDGET and WINDOWS lines.
   - Chosen option: found by the option name in the decision line (default `o1`); the plan action's `estimate`, `plan_budget`, protein boost gate (still none for an open request) and the closing lines use it. `options[].over_kcal` = max(0, option kcal − window) for each option.
   - Decision-line rewrite: the server rewrites the kcal, protein, fit verdict and over numbers of the line from the window of the chosen option's meal and the server totals of both options (as `close_reply` rewrites the closing lines); a line off-form is left as written and logged.
   - `_usual_foods`: the shortened RECENT text is cut at a word boundary.
3. **Contract.** `options[].over_kcal` (integer ≥ 0, only with the `plan_budget` capability); the action's `estimate` is the chosen option (option 1 when no decision line); conversation log gains `chosen` (option id or null) and `decision_line` (`rewritten`, `off_form` or null).
4. **Evaluation (at most 12 model calls).** Tag `s39`, synthetic profiles and dishes (ADR-033 § 2), run once (`--tag s39 --repeat 1 --show --save-fixture logs/evals/fixtures`):
   - `s39-comparacao-hamburguer` — the owner's request replayed on a fictional profile: ceiling 2200 kcal, P 150 g, five slots (Café 07:00, Almoço 12:30, Lanche 16:00, Jantar 19:30, Ceia 22:00), café and almoço eaten (1250 kcal · P 85), three RECENT days with a Ceia of ~220 kcal · P 25 and a Lanche of ~250 kcal, a RECENT wrap with 20 g of cheese and a yogurt-mustard sauce, tone `duro`, local time 16:40, message in the form "hoje pulo o lanche; à noite quero um hambúrguer caprichado: dois de patinho de 120 g na frigideira, ou um só com 100 g de batata rústica no forno, qual?". Expected: two options with the user's dishes; `actions` with the plan and the skip of Lanche; `plan_budget.limit_kcal` ≥ 700 (the Lanche reservation out) and `over_kcal` 0 on the chosen option; the first reply line in the decision form naming the chosen option; a Ceia closing line with ≥ 150 kcal; a recipe with steps naming a temperature; the week pattern at most once in the reply; `reply_not` "Assumi" twice.
   - `s39-comparacao-ambos-passam` — both dishes above the window on another fictional profile: decision line with both overs.
   - `s39-aberto-linha-decisao` — an open request (no dish named): leaner/indulgent pair still, decision line first.
   - `s39-metodo-de-preparo` — one named dish with an appliance: steps with temperature and time, `recipe` present, no options.
   - `s39-suposicao-recent` — a named dish with an open component and a RECENT record of the same kind of dish: the assumption names the RECENT component once.
   - `s39-semana-uma-vez` — `duro` tone with the week pattern already in a HISTORY turn of today: not repeated.
   New evaluator checks: `decision_line` (form and chosen option), `option_over` (per option `over_kcal`), `reply_count` (a term at most n times). Cases: 6 (+ at most 6 reruns).
   - `--show`: at the end of the run, for each case, the runner prints the final `reply` as the app receives it and, below it, the app preview built by `evals/preview.py` from the response: the bubble of [product Chat](../../../produto/specifications/chat.md) rule 25 as ADR-056 § 8 changes it (lead text; one block per option with `Opção {n}: {name}`, its items, `~{kcal} kcal · {p}P · {c}C · {g}G`, `Cabe na janela do {meal}` / `Passa {n} kcal da janela do {meal}`, `Dia: ~{kcal} de {ceiling} kcal · P {p} de {target}`; trailing text; `Passa {over} kcal do que sobra.` when the chosen option passes). The preview mirrors the app rules for the print only; the app's own rendering is A70's test.
   - `--save-fixture <dir>`: writes `<case id>.json` with the request and the response of each case (synthetic data only). A70 copies `s39-comparacao-hamburguer.json` into its test resources.
5. **Dev deploy** from `develop` and the three-turn HTTP smoke; one comparison on the dev app with the installed A67 build (the lead line shows the decision; the fit lines arrive with A70).

### Specification changes at Completion

- [v1-chat](../../specifications/v1-chat.md): rule 3n (decision line, chosen option, comparison, cooking method, assumptions), rule 5f (same-message skip, chosen option, decision-line rewrite, `over_kcal`), log fields; Provenance.
- [HTTP contract](../../../api-contract.md): `options[].over_kcal`, `estimate` = chosen option.
- [product Chat](../../../produto/specifications/chat.md) rule 25 is rewritten by A70.

## Out of scope

- The app rendering (fit lines, projection, lead/trailing split): [A70](../../../android/plans/a70-option-fit-and-projection.md); the `chatO` gold: [D26](../../../design/plans/d26-release-2-option-decision.md).
- Model arithmetic in the reply (ADR-039/042 stand), model or effort change (ADR-054), a projection of the remaining meals beyond the closing lines.

## Validation

1. `pytest server/tests -q` passes: decision-line parse and rewrite (on form, off form, both fit, both pass), chosen option by name and default, same-message skip out of the window and the closings, skip cue before generation, `over_kcal` per option, `_usual_foods` word boundary, the prompt rules and cue block assembled, the provenance inventory, the three evaluator checks, `preview.py` on a fixed response.
2. Smoke as in scope 4 (≤ 12 calls); numbers in Results, with the printed reply and the app preview of `s39-comparacao-hamburguer` pasted in Results as the owner asked.
3. Dev deploy from `develop` and the three-turn HTTP smoke (no model-call budget).
4. `node tools/check-docs.mjs` passes.

## Results

Delivered on 09/10/2026. ADR-056 accepted; ADR-039 and ADR-051 § 1 record the partial supersession on their status lines.

- **ADR-033 record.** The PLAN rule gains ASSUMPTIONS, COMPARISON, DECISION LINE and COOKING METHOD and `tone_duro` gains WEEK once a day: general rules written from ADR-056, with no food, amount, person or sentence of the owner's conversation; the appliance and preparation words of COOKING METHOD and the dish kinds of ASSUMPTIONS are generic English category words. The decision-line form and its pt-BR copy are contracted reply copy (rules). New cue block `comparison` (rule `plan`): `qual dos dois`, `qual deles`, `qual é melhor`, `qual compensa mais`, `ou então`, `isso ou aquilo`, independent synthetic language markers. By owner decision in this session (09/10/2026, asked before the smoke), the `skip-firm` block gains the marker `pulo`, so the server's skip lexicon also covers the present tense of the owner's phrasing before generation.
- **Server (scope 2).** `plan_budget.py`: `decision_option` finds the first line in the form `Vai de {name}: ~{kcal} kcal` and `named_option` matches the name (`Opção {n}`, the same name, one name inside the other, else the most shared words; a tie names none); `decision_text` writes the four verdicts (other passes, both fit, both pass, only the other fits: `…, passa ~{n} kcal da janela do {meal}. {other} cabe.`); `rewrite_decision` replaces the line and moves it to the top, and leaves an off-form reply as written; `option_over` is the rounding of `over_kcal`. `main.py`: the chosen option replaces the plan action's estimate before the budget check, the adjustment call and the closing lines (`_choose_option`, default `o1`); the window, `plan_budget` and the boost room are computed without the slots of the answer's `skip` actions (`_skipped`, `_without_meals`); each option gets `over_kcal` with the capability and a non-null `plan_budget`; the decision line is rewritten after the closing lines, also without the capability (`_decide`); `_chat_text` serves BUDGET and WINDOWS without a meal the message skips in words (`meal_window.cued_skips`: a firm marker of the cue lexicon followed within three words by an empty meal's name, in a clause without a pending or hedge marker); `_usual_foods` cuts at a word boundary. Log fields `chosen` and `decision_line` (route defaults in `main.py`; the conversation-log serializer needed no change). `llm.py` and `shaping.py` needed no change: `over_kcal` is added after shaping.
- **Contract and spec.** [HTTP contract](../../../api-contract.md) `options[].over_kcal`, estimate = chosen option; [v1-chat](../../specifications/v1-chat.md) rules 3c, 3i, 3n, 5f, log fields and Provenance.
- **Unit tests:** `pytest server/tests -q` → 626 passed, 560 subtests. New `test_s39_decision_line.py` (16): option naming, rewrite on form (moved to the top), the three verdicts, off form left as written, chosen option driving estimate, budget (730 with the Lanche skipped vs 480 without), `over_kcal` per option and the Ceia closing (260 kcal), default `o1` with `off_form`, no `over_kcal` without the capability, the served BUDGET/WINDOWS without the cued skip, `cued_skips` markers, `_usual_foods` boundary, the rules and the cue block assembled, `reply_count`, `option_over` arithmetic, `preview.py` on a shaped response and its lead/trailing split. `test_evals.py` covers the three new checks failing. Evaluator: checks `decision_line`, `option_over`, `reply_count`; `plan_budget.limit_kcal_min`; `closing_lines` accepts `{meal: minimum kcal}`.
- **Smoke (8 of 12 model calls, effort low, US$ 0.0109).** `--tag s39 --repeat 1 --show --save-fixture logs/evals/fixtures`, six synthetic cases: 5/6 as reported, 4/6 after review. `s39-semana-uma-vez` failed on a fixture error of mine (its HISTORY turn named sexta and sábado while its RECENT_DAYS over days are sábado and domingo); `s39-suposicao-recent` passed only because the Ceia closing line contains "iogurte" while the wrap ignored the RECENT components. Fixed the HISTORY turn and replaced the check with `meal_text_has: mostarda` on the plan; tightened ASSUMPTIONS (a RECENT dish of the same kind is the template: each component the message neither names nor excludes is in the dish with that record's amount). `--only s39-suposicao-recent,s39-semana-uma-vez --repeat 1` → 2/2: the wrap carries 20 g de queijo minas and the iogurte-mostarda sauce in one `Assumi` clause; the week pattern is not repeated. Final 6/6, p50 7.0 s. `s39-comparacao-hamburguer` (run before the ASSUMPTIONS change): plan + skip of Lanche, `limit_kcal` 730 (Lanche out), chosen `o2` 680 kcal with `over_kcal` 0, `o1` 860 kcal with 130, three steps with 220 °C, recipe present, Ceia closing 270 kcal, the weekend pattern once. `s39-comparacao-ambos-passam`: `Vai de Lasanha à bolonhesa: ~700 kcal · P 36 g, passa ~320 kcal da janela do Jantar; Pizza de calabresa passa ~520.` `s39-aberto-linha-decisao`: leaner/indulgent pair, decision line first, both fit. `s39-metodo-de-preparo`: air fryer at 200 °C, three steps, recipe, no options. Fixtures (synthetic) in `logs/evals/fixtures/` for A70.
- **Observations, not fixed here.** The option paragraphs of the reply keep the model's quoted totals (the hamburger's option 1 says 820 kcal; its server total is 860): ADR-042 rewrites only the plan's total in the reply, and A70 replaces those paragraphs by the blocks. A chosen option above the remaining kcal leaves the last closing line at `~0 kcal` (ADR-043 decision 6 unchanged).

The reply of `s39-comparacao-hamburguer` as the app receives it:

```text
Vai de Hambúrguer com batata rústica: ~680 kcal · P 39 g, cabe na janela do Jantar. Hambúrguer duplo caprichado passa ~130 kcal.
**Opção 1: Hambúrguer duplo caprichado**
- 240 g de patinho, 75 g de pão de hambúrguer, 30 g de queijo, 15 g de molho e 50 g de salada
Total: **820 kcal** · P **65 g**
**Opção 2: Hambúrguer com batata rústica**
- 120 g de patinho, 75 g de pão de hambúrguer, 20 g de queijo, 15 g de molho, 50 g de salada e 100 g de batata
Total: **680 kcal** · P **39 g**
1. Asse a batata a 220 °C por 25–30 min, virando na metade.
2. Grelhe o patinho em frigideira quente até o ponto desejado; derreta o queijo sobre ele.
3. Monte no pão com o molho e a salada.
Lanche de hoje fora.
O jantar passou do teto no fim de semana: sábado e domingo. Ainda faltam 65 g de proteína; ajuste a ceia para priorizar proteína.
Ceia: iogurte natural com whey ~270 kcal · P 26
```

Its app preview (`evals/preview.py`, ADR-056 § 8):

```text
Vai de Hambúrguer com batata rústica: ~680 kcal · P 39 g, cabe na janela do Jantar. Hambúrguer duplo caprichado passa ~130 kcal.

  │ Opção 1: Hambúrguer duplo caprichado
  │ 240 g de patinho grelhado
  │ 75 g de pão de hambúrguer
  │ 30 g de queijo
  │ 15 g de molho
  │ 50 g de salada
  │ ~860 kcal · 68P · 59C · 39G
  │ Passa 130 kcal da janela do Jantar
  │ Dia: ~2110 de 2200 kcal · P 153 de 150
  │ [Registrar] [Reservar]

  │ Opção 2: Hambúrguer com batata rústica
  │ 120 g de patinho grelhado
  │ 75 g de pão de hambúrguer
  │ 20 g de queijo
  │ 15 g de molho
  │ 50 g de salada
  │ 100 g de batata rústica
  │ ~680 kcal · 39P · 62C · 29G
  │ Cabe na janela do Jantar
  │ Dia: ~1930 de 2200 kcal · P 124 de 150
  │ [Registrar] [Reservar]

1. Asse a batata a 220 °C por 25–30 min, virando na metade.
2. Grelhe o patinho em frigideira quente até o ponto desejado; derreta o queijo sobre ele.
3. Monte no pão com o molho e a salada.
Lanche de hoje fora.
O jantar passou do teto no fim de semana: sábado e domingo. Ainda faltam 65 g de proteína; ajuste a ceia para priorizar proteína.
Ceia: iogurte natural com whey ~270 kcal · P 26
```

- **Dev deploy and three-turn HTTP smoke:** after the merge to `develop`, recorded in the follow-up commit.
- `node tools/check-docs.mjs` passes.

## Manual acceptance (after delivery)

- On the dev app with the installed A67 build, one comparison with a skip in the same message: the bubble's first line is the decision line with the server's numbers (the fit lines under each option arrive with A70).
