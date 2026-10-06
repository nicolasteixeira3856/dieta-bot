# Plan — S22 Open requests answered as plans, stable first-message estimates

- Status: Em implementação
- Date: 06/10/2026
- Owning context: `server`
- Executable boundary: `server/` only: the `plan`, `log`, `record` and `memory_use` rules and the `plan-request` cues of the Chat instructions registry (`server/chat_instructions.py`), one new rule with reference portions, tests, evaluation cases and the pilot runner (`server/evals/`). No route, schema, shaping or client change.
- Related documentation: [server Chat specification](../specifications/v1-chat.md), [ADR-033](../../content-policy/adrs/ADR-033-global-chat-example-provenance.md), [ADR-028](../../produto/adrs/ADR-028-registro-autonomo.md), [ADR-039](../../produto/adrs/ADR-039-plan-cooking-and-budget-choice.md), [ADR-040](../../produto/adrs/ADR-040-reference-portions-in-chat-instructions.md) (proposed by this plan), [server README](../README.md) § Model pilot.
- Prerequisites: none. No other plan edits the instructions registry at the time of writing.

Approving this plan accepts ADR-040. Authorization and delivery follow [SDD](../../sdd/README.md). Approval: `Aprovo o plano docs/server/plans/s22-open-requests-and-estimate-stability.md. Implemente o plano aprovado.`

## Objective

An open question about what to eat gets a concrete dish with numbers, never behavioural advice. A first message that already names every food with an amount is estimated and recorded, with the preparation assumptions declared, instead of flipping between a record and a question. Repeated estimates of the same staple foods stay inside a narrow band because the instructions carry reference portions.

The model stays `gpt-6-luna` with `reasoning.effort=none`: the pilot below showed that neither a stronger model nor more reasoning fixes these three problems, and the prompt does.

## Discovery evidence

Collected on 06/10/2026 with the model pilot (`server/evals/pilot/run.py`, [server README](../README.md) § Model pilot): 31 synthetic cases, 98 calls per configuration, no moderation call, same prompt and schema. Grok was run once for comparison and is not adopted.

| Measure | gpt-6-luna, none | gpt-6-luna, low | grok-4.7, low |
|---|---|---|---|
| 20 main-suite cases where Luna had failed or flaked (02–05/10) | 19/20 | — | 19/20 |
| 10 open creative questions (concrete dish with kcal in the reply) | 6/10 | 2/6 of those rerun | 8/10 |
| One fully quantified breakfast repeated 8 times: kcal band | 390–456 (spread 16%) | 401–494 (spread 23%) | 422–505 (spread 18%) |
| Same 8 repetitions: record mark | auto 4, ask 4 | auto 7, ask 1 | ask 8 |
| Latency p50 / p95 | 3.7 s / 6.1 s | 3.2 s / 7.4 s | 15.6 s / 31.1 s |
| Cost per 1000 turns (list price, cached input) | US$ 0.20 | US$ 0.32 | US$ 24.80 |

Failure modes observed, independent of model and effort:

- Open requests ("o que peço no iFood", "como me organizo no rodízio", "estou sem ideia") returned behavioural advice (eat slowly, drink water, check the calories on the app) with no dish and no number in the reply, or were classified as `question`. The `plan` rule already demands grams and totals in the reply; the gap is recognising the request as a plan with a dish.
- A breakfast with every food counted (2 eggs, 1 bread roll, 200 ml of milk) was recorded in half of the repetitions and questioned (milk type, cooking fat) in the other half. The `log` rule already assumes fat, sauce and milk type after an answer; it does not say so for the first message, so the model chooses at random.
- The kcal band of that same breakfast comes from different reference values per call (the bread roll at 135 or 150 kcal, two eggs at 150 or 180 kcal), not from different portions.
- Two secondary gaps: a plan built to differ from a stored routine did not cite the routine in `memory_used` (0/3 on both models); the fresh-estimate instruction for a meal whose earlier estimate is unavailable is buried mid-paragraph and the model skips it in 1 of 3 runs.

Dev conversation log of 30/09 (diagnostic only, [ADR-015](../adrs/ADR-015-log-conversa-dev.md)): the same breakfast text returned 390–600 kcal across 25 repetitions. Nothing from that log enters the instructions.

## Scope

### 1. Open requests are plans with a dish (`plan` rule and `plan-request` cues)

- The `plan` rule gains: an open request about what to eat, order or make, how to organise a meal out, or a message saying the user has no idea what to eat, is a plan. The reply names one concrete dish or order (or two alternatives at most) with grams per item and the `kcal · P · C · G` total. The reply never gives behavioural advice (how fast to eat, drinking water, stopping when satisfied, "listening to hunger") and never defers to an external source (the delivery app, the restaurant, a label the user does not have). When the user names a venue or occasion, the dish is what that venue typically serves, sized to fit `remaining_kcal` when the user left the dish to the model, per the existing BUDGET paragraph.
- `plan-request` cues gain pt-BR markers for these forms, authored from the rule and not from any logged turn: `o que peço`, `o que você comeria`, `sem ideia do que comer`, `como me organizo`, `me surpreende`.
- Spec change at Completion: rule 3a (open requests are `plan`) and rule 3c (concrete dish, no behavioural advice, no deferral to an external source).

### 2. First-message assumptions for a fully quantified meal (`log` rule)

- The `log` rule gains, next to its after-an-answer sentence: when every food of the current message has a usable amount (count, measure, size or household measure), cooking fat or oil, milk type, sugar or sweetener, and usual coffee or tea amounts are assumed with their common value, stated in one short reply line, and never asked; the estimate keeps `question` null and `record_intent` follows the RECORD rule (clear for a stated meal). A fact in MEMORY still wins over the common value. A food with no usable amount keeps the existing portion question.
- Spec change at Completion: rule 4 questions paragraph (second bullet) extends the assumed-details sentence to a fully quantified first message.
- This narrows the question rule of [ADR-026](../../produto/adrs/ADR-026-perguntas-antes-da-estimativa.md) for one situation and aligns it with [ADR-028](../../produto/adrs/ADR-028-registro-autonomo.md) (record when in doubt). ADR-040 records that reading; ADR-026 and ADR-028 keep their bodies.

### 3. Reference portions (new `reference` rule, ADR-040)

- A new registry rule, assembled in both Chat branches after `estimate`, lists about twenty staple foods of the Brazilian diet with one reference portion each and its kcal, P, C and G, taken from the Tabela Brasileira de Composição de Alimentos (TACO, 4th edition, NEPA/UNICAMP) and rounded. Candidates: pão francês, ovo de galinha (cozido, frito), leite integral and desnatado, arroz branco cozido, feijão carioca cozido, frango grelhado (peito), carne bovina (patinho, alcatra), macarrão cozido, batata cozida, banana, mamão, maçã, laranja, iogurte natural, queijo minas, manteiga, óleo de soja, açúcar, café. The final list is fixed in the delivery with its source rows recorded in Results.
- The rule says how to use it: when an item of the current message is one of these foods, its kcal and macros come from the table scaled to the stated amount; the model may deviate only for a stated preparation or brand and says so. Foods outside the table are estimated as today.
- Table values are generic, not any person's data, so they stay outside the per-request context boundary of [ADR-033 § 3](../../content-policy/adrs/ADR-033-global-chat-example-provenance.md); their provenance (edition, food rows) is the ADR-033 § 4 record of this delivery. No example is added.
- Prompt growth target: at most 1,200 characters per branch, cached after the first call.
- Spec change at Completion: rule 3 (the assembled instructions include a reference-portions rule sourced from TACO), rule 4a (nutrients for listed foods follow the reference portions).

### 4. Memory cited when it steers a plan away from a habit (`memory_use` rule)

- One sentence: a fact used to avoid, contrast or vary from a habit (a plan "different from the usual") is also cited in `memory_used`.
- Spec change at Completion: rule 3e.

### 5. Fresh estimate when the earlier one is unavailable (`record` rule, form only)

- The sentence "when the current report gives food and quantity but the earlier estimate is unavailable, make a fresh estimate from that food and quantity; missing yesterday's estimate does not make today's estimate null" moves to the first sentence of the CORRECTIONS paragraph. No rule change; spec rule 4 already states it.

### 6. Evaluation

- The eleven pilot cases (`server/evals/pilot/cases/`) move into the main suite with tag `s22` and `since: v4`; the pilot runner keeps selecting them by tag. The ten open questions keep their current expectations plus `reply_not` for behavioural-advice terms (`devagar`, `água`, `satisfeito`, `confira`) and `reply_has: ["kcal"]`.
- New check `kcal_spread` for a case with `repeat`: the band across repetitions, as a percentage of the median, must stay at or below the expected value. Case-level, computed in `summarize`; unit-tested.
- Four new synthetic cases, varied foods and profiles per ADR-033: an open request with a venue (`churrascaria`), one with a budget stated in the message, one fully quantified lunch (record expected `auto` or `clear`), one quantified meal whose milk type is in MEMORY (the fact must be used and cited, never asked). Independent of the pilot cases and of any logged turn.
- The pilot runner (`evals.pilot.run`) keeps `--luna-effort` for A/B runs and drops nothing; the `grok` provider stays available but is not part of any validation.

### 7. Dev deploy

After validation: `tools/deploy-gcp.ps1` to the dev VM ([ADR-013](../adrs/ADR-013-gcp-host.md)), code only. Dev only.

### Specification changes at Completion

- [v1-chat](../specifications/v1-chat.md): rule 2 unchanged (model and effort); rule 3 (reference-portions rule in the assembled instructions); 3a and 3c (open requests, concrete dish, no behavioural advice or external deferral); 3e (memory cited on contrast); rule 4 questions paragraph (first-message assumptions for a fully quantified meal); 4a (nutrients of listed foods follow the reference portions). Provenance line for S22.
- [server README](../README.md) § Chat evaluation: the `s22` tag and the `kcal_spread` check; § Model pilot: the pilot cases now live in the main suite.
- On approval: ADR-040 status to Accepted; the status lines of ADR-026 (first-message assumptions for a fully quantified meal) and ADR-033 (generic reference data allowed in fixed instructions with recorded provenance) record the partial complement. Bodies unchanged.

## Out of scope

- Model or provider change. `gpt-6-luna`, `reasoning.effort=none`; the Grok pilot is closed by owner decision on 06/10/2026 and the runner stays only as an A/B tool.
- Any schema, shaping, route or client change; the receipt and the record gate are unchanged.
- Per-user calibration of reference values, brand databases or label lookup.
- Reducing variance of foods outside the reference table beyond what the new rules achieve.
- Production: blocked by the [production gate](../../content-policy/production-gate.md).

## Validation

1. `server/.venv/Scripts/python -m pytest server/tests -q` passes, including the inventory, cue and provenance tests of the registry with the new rule and cues declared, and the `kcal_spread` check tests.
2. Pilot runner, `--provider luna --repeat 3`, before (master prompt) and after, same day: the ten open questions pass in at least 9 of 10 cases (concrete dish, `kcal` in the reply, no behavioural-advice term); the repeated breakfast has `record` stable in 8 of 8 and a kcal spread at or below 10%; the fully quantified lunch records in 3 of 3; the memory-contrast case cites the routine in 3 of 3.
3. Full evaluator suite, `--effort none --repeat 1`, compared with the last recorded full run per the server README policy: no case that passed before this plan fails, flakes recorded by name.
4. Prompt review per ADR-033 § 4: the assembled legacy and meal-change prefixes reviewed in full; the ADR-033 record in Results names the rules addressed, the TACO rows used and that no example was added. Prefix sizes and hashes recorded.
5. Dev smoke after deploy through the dev URL: one open request returns a plan with a dish and `kcal` in the reply; one fully quantified breakfast returns `record: auto` with an assumption line; one legacy-shape request is unchanged.
6. `node tools/check-docs.mjs` passes.

## Results

Filled at Completion.
