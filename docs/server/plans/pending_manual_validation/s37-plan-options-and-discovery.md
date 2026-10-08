# Plan — S37 Plan options with ids; routine discovery turn

- Status: Pendente aprovação manual (approved 08/10/2026 in the owner's batch message; code, unit tests, smoke and dev deploy done; the open dinner request and "fiz a 2" on the dev app are the owner's manual acceptance)
- Date: 08/10/2026
- Owning context: `server`
- Executable boundary: `server/` only: `chat_instructions.py` (`plan_options`, `discovery`, `memory_changes` for `equipment` and `liked`), `llm.py` schema (`options[]`, categories), compaction (`digest` keeps option ids), `main.py`, `evals/`, tests.
- Related documentation: [ADR-051](../../../produto/adrs/ADR-051-plan-option-identity-and-chat-discovery.md) (accepted with this plan), ADR-023/029/043/046, [v1-chat](../../specifications/v1-chat.md), [HTTP contract](../../../api-contract.md); rule text `benchmark/prompts/new_instructions.py` (`plan_options`, `discovery`, `memory_changes_macros`); benchmark [results](../../../../benchmark/RESULTADOS_08_10_2026.md) families `plan`, `discovery`, `memory`.
- Prerequisites: [S36](../pending_manual_validation/s36-typed-actions.md) delivered.

Approving this plan accepts ADR-051. Authorization and delivery follow [SDD](../../../sdd/README.md). Approval: `Aprovo o plano docs/server/plans/s37-plan-options-and-discovery.md. Implemente o plano aprovado.`

## Objective

Each option of a plan has an id the user can refer to, before and after compaction; the first opening with an empty memory asks the routines in one message and saves them with macros.

## Scope

1. **Options.** `plan.options[] = {id, name, estimate}` (two for an open request; `estimate` of the action is option 1); reply format `Opção 1: {name}` / `Opção 2: {name}`. Rule: a later reference by number or name is answered about that option only (question quoting grams, `log` copying the option, reserve of ADR-046). The compaction keeps id, name, kcal and protein of the options of the last plan in the digest.
2. **Discovery.** Request flag `discovery: true` (app: first opening, empty memory). The answer is one `question` action whose reply lists up to four skippable questions (breakfast, lunch, dinner, fixed preferences or equipment); the FORMAT rule allows bullets for this message. The next turn's answers produce `memory_updates`: permanent preferences, routines with server-estimated macros and `declared: true` (no recorded day), `equipment` facts. A skip ("não quero responder") proposes nothing.
3. **Memory rules.** Categories `equipment` and `liked` in the schema and the `memory_changes` rule; routine proposals always carry macros; the explicit `replace` of a restated preference and the `reinforce` of a routine named with its brand are stated as rules (the benchmark saw them missing at `none`). The sentence "Minha memória fixa está cheia" stays the rule for 30/30.
4. **Smoke (at most 12 model calls).** Tag `s37`, synthetic, ten cases: open request with two options, "quanto de X na 1?", "fiz a 2", option in the digest only, discovery answered, discovery skipped, equipment then plan, liked after a plan, replace, reinforce. Run once: `--tag s37 --repeat 1`.
5. **Dev deploy** and one open dinner request plus "fiz a 2" on the dev app (the option control arrives with A67; the log shows ids).

### Specification changes at Completion

- [v1-chat](../../specifications/v1-chat.md), [HTTP contract](../../../api-contract.md): `options[]`, `discovery`, categories, `declared`. [memoria-push](../../../produto/specifications/memoria-push.md): declared routines and the two categories (text of ADR-051), rewritten with A67.

## Out of scope

- The option control, the discovery flow in the app and the fact application: [A67](../../../android/plans/a67-plan-options-and-discovery.md). O6 (conditional, ADR-053 § 4).

## Validation

1. `pytest server/tests -q` passes (digest with option ids, schema).
2. Smoke as in 4 (≤ 12 calls); numbers in Results.
3. Dev deploy from `develop` and the three-turn HTTP smoke (no model-call budget).
4. `node tools/check-docs.mjs` passes.

## Results

Delivered on 08/10/2026 in the autonomous batch S33 → S38 (report: `docs/server/validation/batch-2026-10-08.md`). ADR-051 accepted.

- **Options (scope 1).** The action schema types `options` as null or a list of `{id: o1|o2, name, estimate}`; `shaping.shape_options` keeps unique `o1`/`o2`, a name of at most 60 characters and an estimate with energy (each option's total is the server total, ADR-042, applied in generation). Only a plan action returns `options` (inside `actions[]`, with the capability). The PLAN rule: an open request is two options with ids, presented as `**Opção 1: {name}**` / `**Opção 2: {name}**`, the action's estimate is option 1; LATER REFERENCE answers about the named option only (a question quotes its grams, eating it copies it, planning it again plans it); a new pt-BR cue block (`option-reference`) under PLAN. An open request (options present) gets no protein boost: the S34 note that the server completion boosted option 1 of an open request is closed here. The digest rule keeps, for the last turn with numbered options, `Opção {n}: {name}, {kcal} kcal, P {protein} g`.
- **Discovery (scope 2).** Request flag `discovery` (JSON boolean, compact ignores it) adds `DISCOVERY: first_open` before the message; the DISCOVERY rule (both branches) asks up to four skippable questions as `- ` lines (FORMAT exception) and turns the answers into declared routines with numbers (`declared: true`), permanent preferences and `equipment` facts, nothing logged; a skip proposes nothing.
- **Memory rules (scope 3).** Categories `equipment` and `liked` in `FactIn`, the schema and shaping (a liked dish keeps its slot and numbers; temp facts keep portion/preference only); `declared` in the schema, kept only on a routine. MEMORY CHANGES states the categories, `declared`, replace for a restated preference, reinforce for a branded routine log, and (after the smoke) that approval of an eaten dish always proposes `liked`, with a pt-BR cue block (`liked`). "Minha memória fixa está cheia" stays the 30/30 rule. ADR-033 record: general rules from ADR-051; the two cue blocks are independent synthetic language markers.
- **Unit tests:** `pytest server/tests -q` → 600 passed, 546 subtests. New `test_s37_options_discovery.py` (10): schema of options, shaping of options, an open request returns options and gets no boost, the `options` check, the digest rule, the plan rule and cue, the discovery line and rule, the new categories in request and schema, shaping of `declared`, `equipment` and `liked`. New evaluator check `options` (also a per-action key). The provenance test's unassembled cue now uses a rule without cues.
- **Smoke (11 of 12 model calls, effort low, US$ 0.0094).** `--tag s37 --repeat 1` (10 synthetic cases): 9/10, p50 6.0 s. Open request → plan with `o1`/`o2`; "quanto de queijo vai na 1?" → question quoting the 60 g; "fiz a 2" and "comi a opção 2" with the options only in the digest → log of 610 kcal (option 2); discovery answered → three declared routines with numbers, a milk preference and an `equipment` fact; discovery skipped → no proposal; equipment then a recipe → the reply uses the air fryer; restated milk → replace of P1; branded routine → reinforce of D1. `s37-gostei` failed (the approval became a plan without the `liked` fact); after the explicit sentence and cue, `--only s37-gostei --repeat 1` → pass.
- **Dev deploy and HTTP smoke:** in the batch report.
- `node tools/check-docs.mjs` passes.

## Manual acceptance (after delivery)

- On the dev app, an open dinner request and then "fiz a 2": the second option is recorded with its numbers (the option control arrives with A67; the installed app reads the options from the reply).
