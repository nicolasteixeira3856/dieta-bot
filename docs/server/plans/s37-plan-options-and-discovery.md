# Plan — S37 Plan options with ids; routine discovery turn

- Status: Aguardando aprovação
- Date: 08/10/2026
- Owning context: `server`
- Executable boundary: `server/` only: `chat_instructions.py` (`plan_options`, `discovery`, `memory_changes` for `equipment` and `liked`), `llm.py` schema (`options[]`, categories), compaction (`digest` keeps option ids), `main.py`, `evals/`, tests.
- Related documentation: [ADR-051](../../produto/adrs/ADR-051-plan-option-identity-and-chat-discovery.md) (accepted with this plan), ADR-023/029/043/046, [v1-chat](../specifications/v1-chat.md), [HTTP contract](../../api-contract.md); rule text `benchmark/prompts/new_instructions.py` (`plan_options`, `discovery`, `memory_changes_macros`); benchmark [results](../../../benchmark/RESULTADOS_08_10_2026.md) families `plan`, `discovery`, `memory`.
- Prerequisites: [S36](s36-typed-actions.md) delivered.

Approving this plan accepts ADR-051. Authorization and delivery follow [SDD](../../sdd/README.md). Approval: `Aprovo o plano docs/server/plans/s37-plan-options-and-discovery.md. Implemente o plano aprovado.`

## Objective

Each option of a plan has an id the user can refer to, before and after compaction; the first opening with an empty memory asks the routines in one message and saves them with macros.

## Scope

1. **Options.** `plan.options[] = {id, name, estimate}` (two for an open request; `estimate` of the action is option 1); reply format `Opção 1: {name}` / `Opção 2: {name}`. Rule: a later reference by number or name is answered about that option only (question quoting grams, `log` copying the option, reserve of ADR-046). The compaction keeps id, name, kcal and protein of the options of the last plan in the digest.
2. **Discovery.** Request flag `discovery: true` (app: first opening, empty memory). The answer is one `question` action whose reply lists up to four skippable questions (breakfast, lunch, dinner, fixed preferences or equipment); the FORMAT rule allows bullets for this message. The next turn's answers produce `memory_updates`: permanent preferences, routines with server-estimated macros and `declared: true` (no recorded day), `equipment` facts. A skip ("não quero responder") proposes nothing.
3. **Memory rules.** Categories `equipment` and `liked` in the schema and the `memory_changes` rule; routine proposals always carry macros; the explicit `replace` of a restated preference and the `reinforce` of a routine named with its brand are stated as rules (the benchmark saw them missing at `none`). The sentence "Minha memória fixa está cheia" stays the rule for 30/30.
4. **Smoke (at most 12 model calls).** Tag `s37`, synthetic, ten cases: open request with two options, "quanto de X na 1?", "fiz a 2", option in the digest only, discovery answered, discovery skipped, equipment then plan, liked after a plan, replace, reinforce. Run once: `--tag s37 --repeat 1`.
5. **Dev deploy** and one open dinner request plus "fiz a 2" on the dev app (the option control arrives with A67; the log shows ids).

### Specification changes at Completion

- [v1-chat](../specifications/v1-chat.md), [HTTP contract](../../api-contract.md): `options[]`, `discovery`, categories, `declared`. [memoria-push](../../produto/specifications/memoria-push.md): declared routines and the two categories (text of ADR-051), rewritten with A67.

## Out of scope

- The option control, the discovery flow in the app and the fact application: [A67](../../android/plans/a67-plan-options-and-discovery.md). O6 (conditional, ADR-053 § 4).

## Validation

1. `pytest server/tests -q` passes (digest with option ids, schema).
2. Smoke as in 4 (≤ 12 calls); numbers in Results.
3. Dev deploy from `develop` and the three-turn HTTP smoke (no model-call budget).
4. `node tools/check-docs.mjs` passes.

## Results

<Filled at Completion.>
