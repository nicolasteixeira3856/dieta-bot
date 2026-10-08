# Plan — S33 Chat context in code and effort low

- Status: Pendente aprovação manual (approved 08/10/2026 in the owner's batch message; code, unit tests, smoke and dev deploy done; the one turn on the dev app is the owner's manual acceptance)
- Date: 08/10/2026
- Owning context: `server`
- Executable boundary: `server/` only: `config.py` (effort), `main.py` (`_chat_text`, `_memory_lines`, named-day resolver, `recent_days`), `chat_instructions.py` (rules named below), `llm.py` (timeout), `evals/` (cases, effort), tests. No client change; the new request fields are optional and ignored when absent.
- Related documentation: [ADR-054](../../adrs/ADR-054-chat-reasoning-effort-low.md) (accepted), [ADR-023](../../../produto/adrs/ADR-023-chat-v2-memoria-v2.md), [ADR-033](../../../content-policy/adrs/ADR-033-global-chat-example-provenance.md), [ADR-044](../../../produto/adrs/ADR-044-assistant-tone-and-closures.md), [v1-chat](../../specifications/v1-chat.md), [HTTP contract](../../../api-contract.md); brainstorm conclusion `benchmark/MELHORIAS_CHAT_07_10_2026.md` § 5.4 Plano 1; benchmark [results](../../../../benchmark/RESULTADOS_08_10_2026.md).
- Prerequisites: none. First plan of the order.

Authorization and delivery follow [SDD](../../../sdd/README.md). Approval: `Aprovo o plano docs/server/plans/s33-chat-context-effort-low.md. Implemente o plano aprovado.`

## Objective

The server resolves in code what the model gets wrong when asked to search: the record of a named day, the macros of a routine, the totals of the last seven days; the input is ordered for the prefix cache; the Chat generation runs at effort low (ADR-054). The target rules the benchmark measured become the server rules, with the findings of the run carried as rules.

## Scope

### 1. Effort low (ADR-054)

- `config.py`: `CHAT_EFFORT = "low"`; compaction and closures keep `none`. `llm.py`: generation timeout raised to cover the measured p95 (16 s) with margin (25 s). The dev log records the effort and the reasoning tokens.
- `evals/run.py`: default effort `low`. No suite rerun: the baseline for development is the benchmark of 08/10/2026 (`benchmark/RESULTADOS_08_10_2026.md`).

### 2. Named-day record in code (`COPY_SOURCE`)

- Deterministic resolver, the one of `benchmark/run.py` (`resolve_copy_source`) moved into `server/`: an equality word (`mesmo`, `igual`, `o de`, `repeti`) plus a day word (`ontem`, `anteontem`, a weekday, `semana passada`) plus a slot named or implied by the verb; one `RECENT` row → `COPY_SOURCE:` block with the row; several rows → `ambiguous` plus the rows; none → no block. One block per clause that names a day (two days in one message give two blocks).
- `history_copy` rule: branch C ("de ontem", "de segunda") reads "copy COPY_SOURCE exactly (kcal and macros), confidence high, no question; `ambiguous` asks which; no block asks what was eaten". `de sempre` keeps branches A/B of ADR-023.

### 3. Routine macros and seven-day totals

- `facts[]` accept optional `kcal`, `p`, `c`, `g`; `_memory_lines` prints them after the text (`· 485 kcal · P 25 · C 41 · G 24`). Branch A copies the routine numbers; the `memory_changes` rule proposes a routine only with macros.
- `recent_days[]` optional in the request (up to 7: date, weekday, kcal, P, C, G, effective ceiling, recorded flag, slot that went furthest over, slots without record). Serialized as `RECENT_DAYS:` after `RECENT`. The `duro` critique cites the week only over recorded days; a day with a gap is never "below the ceiling".
- Input order: PROFILE → MEMORY → RECENT → RECENT_DAYS → DAY (with `local_time`) → DIGESTS → HISTORY → PENDING_ADDITION → WINDOWS/BUDGET → message. The cached share of input tokens is measured before and after on the evaluator.

### 4. Rules carried from the benchmark findings (general rules, ADR-033 record in Results)

- `meal_changes`: a food already present in the record of that meal (routine text or RECENT row) that the user "forgot" to mention is a `revise` with the same total, never an `add` (the "esqueci o leite" case failed in all arms).
- `estimate`: a meal whose foods all carry grams or units is `confidence high`; `medium` is for an assumed amount.
- `plan`: a question about adding one item to a stated dish ("posso adicionar uma colher de maionese?") is answered with that item's kcal and its macro, in one line, not with the dish total.
- `log`: a canned drink without a stated volume is estimated at the most common can of that product in Brazil (350 ml for beer) and the volume assumed is said.
- `history_copy`/`ambiguous`: a clarifying question on a clear log (amount of butter, of salad) is not asked; an unstated condiment is assumed and stated (`low` asked more than `none`).

### 5. Smoke (at most 12 model calls)

- New cases, tag `s33`, synthetic situations independent of the benchmark texts (ADR-033), twelve at most: named-day copy (one row, ambiguous, none), two days in one message, routine with macros copied, week pattern under `duro` with a gap day, and one case per rule of 4. Run once: `--tag s33 --repeat 1`. No sentinel set, no suite.

### 6. Dev deploy

`tools/deploy-gcp.ps1`, then the three-turn smoke of the server README, plus one "mesmo almoço de ontem" turn on the dev app.

### Specification changes at Completion

- [v1-chat](../../specifications/v1-chat.md): effort, input order, `COPY_SOURCE`, `RECENT_DAYS`, routine macros, the rules of 4; provenance line.
- [HTTP contract](../../../api-contract.md): `facts[].kcal/p/c/g`, `recent_days[]`.
- `AGENTS.md` § LLM already states `low` (ADR-054); the rollout note is removed at Completion.

## Out of scope

- The app sending the new fields: [A64](../../../android/plans/a64-chat-context-fields-day-balance.md). Actions, workout, options, recipes: later plans. The protein boost: [S34](s34-protein-boost-hybrid.md).

## Validation

1. `server/.venv/Scripts/python -m pytest server/tests -q` passes, including the resolver tests (moved from the benchmark's dry-run checks into unit tests).
2. Smoke as in 5 (≤ 12 calls), numbers in Results; cached share read from those calls.
3. Dev deploy from `develop` and the three-turn HTTP smoke (no model-call budget).
4. `node tools/check-docs.mjs` passes.

## Results

Delivered on 08/10/2026 in the autonomous batch S33 → S38 (report: `docs/server/validation/batch-2026-10-08.md`).

- **Effort (scope 1).** `config.CHAT_EFFORT = "low"`; `LlmClient` applies it to the `chat` call only, estimate, fit, compaction and closures keep `REASONING_EFFORT = "none"`. The dev log (`trace`) gains `effort` and `usage` (`input`, `cached`, `output`, `reasoning`). Timeout: no change was needed; the one 60-second request deadline (rule 16) already exceeds the 25 s the plan names. `evals/run.py` defaults to `--effort low`.
- **Named-day record (scope 2).** `server/copy_source.py` (the benchmark's `resolve_copy_source` moved into the server, plus a first-word match for slot names such as "Café da manhã"); `_chat_text` serializes `COPY_SOURCE:` / `COPY_SOURCE: ambiguous` before DAY. The HISTORY rule's PARTICULAR DAY branch reads the block (copy exactly, no question; ambiguous asks which; no block asks what was eaten).
- **Routine macros, seven-day totals, input order (scope 3).** `facts[].kcal/p/c/g` optional, printed after the text when all four exist; `memory_updates[]` schema gains `kcal/p/c/g` (number or null) and the MEMORY CHANGES rule proposes a routine only with them; shaping keeps them rounded on a routine and drops them elsewhere. `recent_days[]` (≤ 7, slot ids validated against the profile) serialized as `RECENT_DAYS`. Input order PROFILE → MEMORY → RECENT → RECENT_DAYS → COPY_SOURCE → DAY → DIGESTS → HISTORY → PENDING_ADDITION → WINDOWS/BUDGET → message. TONE duro gains the WEEK sentence.
- **Benchmark rules (scope 4), ADR-033 record:** general rules written from the benchmark findings, no example and no fixture text: MEAL CHANGES (a forgotten food already in the record is never an add: revise with the same totals, or the reaffirmation answer), ESTIMATE in both branches (CONFIDENCE: all foods with amounts is high), PLAN (ONE ITEM: the added food alone), LOG in both branches (canned drink at the common Brazilian can, side or condiment assumed on a clear log). Found during the smoke: the server protein boost appended foods to a one-item answer; `protein_boost.MIN_DISH_KCAL = 150` skips a one-food plan under 150 kcal.
- **Unit tests:** `pytest server/tests -q` → 552 passed, 507 subtests. New `test_s33_context.py` (17: resolver one row, weekday + verb, ambiguous, none, two days, first word, habitual and undated never resolve, last week; COPY_SOURCE placement, routine macros line, RECENT_DAYS lines and order, slot validation, seven-day cap; schema and shaping of routine numbers; chat `low` / digest `none` and usage in the trace); new `meal_change_op` evaluator check with its unit test; the boost test for one small food; the order and default-effort tests updated.
- **Smoke (12 of 12 model calls, effort low, US$ 0.0075).** `--tag s33 --repeat 1` (11 synthetic cases): 8/11, p50 3.3 s, p95 5.3 s, 11 250 input tokens per call of which 7 736 cached (69 %). Failures read by their output: `s33-um-item-no-prato` (the model answered the oil alone, 119 kcal; the server boost inflated it: fixed in code, replayed offline → pass), `s33-esqueci-item-registrado` (the model answered "already in the record, nothing to change" with no estimate: correct, no double count; the case now accepts revise or no change through `meal_change_op`, re-judged offline → pass), `s33-duro-semana-lacuna` (no week pattern: the WEEK sentence was reworded; `--only s33-duro-semana-lacuna --repeat 1` → pass, the reply names "sexta e terça"). Cached share before the reorder: not measured (no budget left inside the 12 calls); the benchmark measured 93 % on its repeated prefixes.
- **Dev deploy and three-turn HTTP smoke:** in the batch report.
- `node tools/check-docs.mjs` passes.

## Manual acceptance (after delivery)

- One "mesmo almoço de ontem" turn on the dev app against the deployed server: the reply copies yesterday's lunch with its numbers and asks nothing.
