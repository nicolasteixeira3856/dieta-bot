# Plan — S32 Eaten slot: the model picks add or revise, and the answer never fails (hotfix)

- Status: Concluído (08/10/2026)
- Date: 08/10/2026
- Owning context: `server`
- Executable boundary: `server/` only: `meal_changes.prepare_change`, the MEAL CHANGES rule of `chat_instructions.py`, tests and evaluation cases. No schema, route or client change.
- Related documentation: [server Chat specification](../../specifications/v1-chat.md) rules 4 and 5e, [HTTP contract](../../../api-contract.md#meal-change-capability), [ADR-042](../../adrs/ADR-042-estimate-total-is-server-arithmetic.md), [S31](s31-new-on-occupied-slot.md).
- Prerequisites: S31 (on master and on the dev server).

Written and executed in the same owner message (08/10/2026): "escreve o plano para o modelo escolher add ou revise [...] o plano agora deve ser completo, devidamente atacado e o problema resolvido como hotfix." Mid-run the owner added: "você não tem permissão para rodar nenhum teste que custe dinheiro na API [...] prossiga com testes gratuitos e faça o deploy da correção."

## Objective

After S31 the tester got the add-or-replace question, answered "Pode somar" and got `nao deu pra estimar` again (request `4e47517c`): the model returned `operation: add`, `base_slot` = the snack, `addition: null` and a zero estimate asking the portions it had assumed one turn earlier (the S31 question dropped the draft, and HISTORY carries only the reply). `nutrition(None)` raised `ValidationError`. Close every path of this flow that ends in the error fallback, and make the model pick the operation itself.

## Scope

1. The S31 question carries the draft: `{slot} de hoje já tem registro: {text} ({kcal} kcal). Somo {draft meal_text}, ~{kcal} kcal, a esse registro ou substituo o registro por isso?`, so the answer turn has the foods and portions in HISTORY.
2. `add` with `addition: null`: the draft estimate (for add it describes only the new food) becomes the addition when valid; else the model's `estimate.question` is asked; else the turn fails as before. A present but invalid addition is never replaced.
3. `add`/`revise` with a null `base_slot` on an eaten target: base = target (rule 5e allows only that value).
4. A model operation with a null estimate: the unresolved question, `meal_change` null (was `missing meal change`).
5. The addition total is recomputed from its items (ADR-042, as new/revise), instead of `inconsistent item energy` (found by the eval: addition 145 kcal, items 87 + 56).
6. Prompt, MEAL CHANGES rule (both branches): read the DAY status of `suggested_slot` before choosing, `new` is never valid on an eaten slot; an answer to the add-or-replace question, even one word, resolves it on the meal the question named, with the foods and portions it named, without asking them again.
7. Evaluation cases, tag `s32`, synthetic: an ambiguous log to an eaten snack (pass: the question or a resolved add, never the fallback), the "Pode somar" answer (add, portions 170/110 g), the "Substitui" answer (revise).

### Specification changes at Completion

- [v1-chat](../../specifications/v1-chat.md) rule 4 (the two prompt behaviors) and rule 5e (question with the draft, null base, null estimate, addition total, null addition); Provenance line.
- [HTTP contract](../../../api-contract.md#meal-change-capability): the question, the null-base reading, the addition total and the null-addition fallback.

## Out of scope

- Client; item quality of the first incident (two banana lines); production (blocked by the [production gate](../../../content-policy/production-gate.md)).

## Validation

1. `pytest server/tests -q`.
2. Evaluation: `--tag s32 --repeat 3`, then the whole suite at `--repeat 1` (prompt change, server README table).
3. Hotfix dev deploy with `tools/deploy-gcp.ps1`; no smoke on the server or the emulator (owner decision in S31).
4. `node tools/check-docs.mjs`.

## Results

- Unit tests: `pytest server/tests -q` → 532 passed, 496 subtests. New `test_s32_occupied_answers.py` (10): question with the draft and its fallbacks; null addition → draft; null addition with zero draft → the model question (incident shape); zero draft without question and a present invalid addition still fail; null base on an eaten target for add and revise; null base on an empty target stays an unknown-target add; operation with no estimate; addition total recomputed. `test_meal_changes.py`: the null-base and `kcal=55` variants left the bad-metadata list (now resolved on purpose); S31 tests updated to the draft wording.
- Evaluation, run before the owner withdrew paid tests (cost not measured, small):
  - first prompt, `--tag s32 --tag s18 --repeat 3`: 27/32. `s32-answer-add` 3/3; `s32-answer-replace` 1/3 (the model did not find the meal on "Substitui"); `s32-occupied-ambiguous` 1/3, one run hit `inconsistent item energy` (fixed by scope 5) and the case expected only the question (relaxed to "question or add, never the fallback"). `s18-unknown-operation-question` 2/3 (one `JSONDecodeError`, noise), `s18-missing-volume` (already flaky in the 07/10 runs), `s18-pending-second-portion` 0/1 portion (passed in the last full run).
  - after the stronger answer sentence and scope 5, `--tag s32 --repeat 3`: 3/3 on all three cases.
  - the whole suite at `--repeat 1` was started and stopped by the owner ("não tem permissão para rodar nenhum teste que custe dinheiro na API"): **pending**, together with the rerun of the three `s18` cases. Run them only with the owner's authorization.
- Dev deploy: see the PR; no smoke by owner decision.
