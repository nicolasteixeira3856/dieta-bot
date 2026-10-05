# Plan — S18 Meal additions and revisions

- Status: Em implementação
- Date: 04/10/2026
- Owning context: `server`
- Executable boundary: `server/` only (`main.py`, `llm.py`, `shaping.py`, configuration, tests and evals).
- Related documentation: the owning server/product specifications and API contract, plus plan/index/ADR lifecycle changes.
- Prerequisites: no preceding executable plan. [ADR-032](../../produto/adrs/ADR-032-acrescimos-e-correcoes-de-refeicoes.md) is the product decision included in this plan's approval scope.

Authorization and delivery follow [SDD](../../sdd/README.md). Approval: `Aprovo o plano docs/server/plans/s18-meal-additions-and-revisions.md. Implemente o plano aprovado.` This approval also accepts the linked ADR-032; it does not authorize D9 or A47 implementation.

## Objective

Resolve the meal being continued, explain an addition separately from the consolidated total and make the accounting verifiable by the client. Preserve earlier nutrients on a pure addition. Supply a compatible contract for [A47](../../android/plans/a47-chat-meal-updates.md).

## Discovery evidence

Investigation date: 2026-10-04. Correlation references only; raw tester conversations and images are not repository fixtures.

- Request `f773e2ca-e397-4800-aaee-e59cf3e37ce6`: the model selected a later empty meal for an addition.
- Requests `5c4bca21-66d2-40a3-b5fa-f4d05ac2f023` and `d56db3c8-bdc4-4c1a-b4e2-4920332fa18f`: both selected the same intended slot and returned consolidated totals. Wording described replacement; repeated totals had different macros. The owner confirmed using the alternative-destination control, explaining the different local receipt.
- Follow-up request `056d2750-376e-4daa-b56b-c897bb581af2`: DAY confirmed dessert in both snack and dinner, with the drink included in dinner. The model selected dinner but composed its items from lunch plus the new dinner foods, omitting the actual dinner base. Its total was 850 kcal while its items summed to 1350. The raw description was 161 code points; the existing 160-character comma cut reduced it to 113, dropping all newly reported foods and leaving only lunch text. An offline call to the current `_meal_text` function reproduced the logged shaped description exactly. No fallback or transport error was involved.
- Requests `22757fdf-0438-47d9-bff6-0773c99c58c3` and `58aaeed3-005e-46fe-ba50-4eccf18b11e6` still received the prior dinner base in DAY. The second returned only the new dinner foods, losing the original addition intent and base. Request `191367a6-ffb4-4e69-ae54-4df940ef1311` then showed that replacement saved in DAY. The first wrong estimate is not observed as saved in these snapshots. Server logs do not identify the local confirmation tap; the installed app's recorder replaces the whole occupied-slot aggregate after confirmation.
- A compact call (`5388b174-971c-4046-8d1e-0f367f5eea31`) preceded the dinner request. Its digest described the earlier conversation's snack target, while DAY reflected the owner's subsequent dinner selection. This exposes two different kinds of context; it does not prove that compaction caused the model error. DAY was sufficient to identify the actual dinner base.
- Synthetic, text-only baseline: five situations, three repetitions each, current `gpt-6-luna` with `reasoning.effort=none`, zero reasoning tokens. Slot checks passed in all 15 outputs. An offline audit of food retention and addition totals passed 14/15: one output named the whole meal in `meal_text` but included only the addition in `items` and kcal. This is not an exact reproduction of the photo request and not a measured general failure rate.
- Local artifacts are ignored under `logs/` in the investigation worktree. Handling follows the [data map](../../content-policy/operations/closed-test-data-map.md). Rewrite situations manually and use synthetic images for committed regression cases.

## Sources

[Server Chat](../specifications/v1-chat.md), [HTTP contract](../../api-contract.md), [product Chat](../../produto/specifications/chat.md), ADR-032 and the existing shaping, clarification, record and moderation pipelines. Read the actual implementation again at start; do not use an earlier report as proof of deployed behavior.

## Scope

### 1. Resolve meal relationships

Implement the operation and target rules of ADR-032. Cover explicit meal names, dessert continuations, unrelated interleaved corrections, unusual profile times, digests and suggested-slot history markers. Do not infer an addition solely from a food's category. Preserve the difference between a second portion, a correction to a pending addition and a repeated description of food already in DAY.

Resolve operation, target and base together. For an explicit addition to a named occupied meal, that slot's current DAY record is the sole recorded base; food similarity, another meal's larger description and an earlier conversational destination in DIGESTS do not replace it. HISTORY/DIGESTS may explain the new food or the pending request, but do not assert that a proposed action was committed. A follow-up confirming the target or repeating the new quantities retains the unresolved addition intent unless the user explicitly changes the operation. It must not become a whole-meal revision merely because the follow-up omits the word "add". Keep explicit correction/replacement requests supported.

Ask unresolved operation/target questions through the existing Chat flow. Respect the clarification cap; force may settle missing portions but cannot authorize an unknown mutation. A known addition with no target may be estimated alone with `record: ask` and no suggested slot. Unresolved operation returns no actionable estimate. Document and test the gate precedence instead of letting `released_cap` or `released_force` bypass it.

### 2. Add an opt-in contract

Proposed wire contract, owned here until incorporated into the API contract at Completion:

- Request capability `meal_changes: true`, effective with `clarify_rounds` and `auto_record: true`. Absent/false preserves the older response shape; unsupported capability combinations return validation errors, not silently different semantics. Compact requests ignore this capability.
- Optional `pending_addition: {base_slot, addition}` carries the most recent unrecorded addition immediately being continued, in the same shapes defined below. It is absent for legacy/compact requests. The client sends it only while its captured source still matches DAY, taking this context before expiring the old action on send. Treat it as untrusted context, not a command or an already eaten DAY entry. A clarification replaces this proposed addition, not the base plus the proposal plus another copy. An explicit second portion remains a separate addition. Never send it for an already consumed, cancelled or stale proposal.
- For effective callers, responses add `meal_change`, either null or `{operation, base_slot, addition}`. `operation` is `new`, `add` or `revise`; `base_slot` is a profile slot id or null; `addition` is null except for `add`, where it contains `{meal_text, kcal, p, c, g, items}` describing only the newly eaten food. Item shape follows the existing item contract.
- `base_slot` identifies the occupied meal used for an initial update and must match `estimate.suggested_slot`. A new meal has a null base. An addition without an occupied target also has a null base; its estimate is the addition alone. A revision requires an identifiable eaten base slot.
- `estimate` remains the candidate whole record for its suggested target. For an addition to an occupied base, derive its kcal/P/C/G from DAY plus the validated addition; never trust model-supplied consolidated arithmetic. Its `items` is empty for this new capability: the prior meal is an authoritative aggregate, not an invented item/weight. The separate addition supplies its own item breakdown. Validation is operation-aware: addition items sum to the addition, and base plus addition equals the result. Other operations retain the existing full-meal item semantics.
- Pure addition descriptions retain the exact previous DAY text followed by the added-food description. The addition's description uses the normal meal-description bound defined below. For this capability only, the composed `estimate.meal_text` is allowed up to 2000 code points. If complete composition cannot fit, return no actionable update instead of cutting previous food. No database cleanup or historical text rewrite.
- Emit no actionable `meal_change` on held, refused, failed, plan or skip turns. For an effective caller, a released log estimate requires valid operation metadata; malformed/contradictory metadata results in the existing safe failure shape with no record or memory mutation, not a legacy fallback. Memory proposals must not survive a rejected action.
- The client captures the actual source state used for its request and validates it locally; a server echo is not a concurrency token. Do not add personal identifiers or a server-side meal store.

Introduce the capability without changing the pinned model/effort. New-schema instructions must distinguish incremental and whole-meal values explicitly. Keep the legacy structured-output path available while old clients remain installed.

Author changed instructions under [ADR-033](../../content-policy/adrs/ADR-033-global-chat-example-provenance.md). The discovery incidents above motivate general rules and independent synthetic regressions; they are not examples to embed in the global prompt. S19 owns the full existing-example cleanup and provenance-check implementation.

### 3. Shape, validate and explain

Owner decision (2026-10-04): raise the normal meal-description limit from 160 to 500 Unicode code points. Apply it to `estimate.meal_text` for legacy callers and ordinary new/revised meals, and to `meal_change.addition.meal_text`. The composed-addition exception above remains separate. Update the prompt, configuration, shaping and contract together; do not change memory-fact, digest or user-message limits as a side effect. Remove silent comma/character truncation from meal-description shaping: preserve the complete description within its applicable bound; otherwise return the existing safe failure shape with no actionable record or memory mutation. This includes legacy callers. Increasing the bound alone does not fix lost-food or arithmetic errors.

Validate finite, nonnegative nutrient values, positive energy for a nonempty caloric addition, profile/day slot membership and operation/base compatibility. Preserve the prior aggregate exactly. Define one rounding policy shared in the wire examples: round the addition once, then derive the consolidated values from the supplied base; do not round two independent model totals.

The server composes the numeric addition explanation from validated values, using ADR-032's copy, so reply numbers cannot contradict the structured fields. Model prose supplies only a brief food assumption when needed. No claim that recording already happened. Existing moderation must cover every emitted string, including composed descriptions; no new logging/content-policy subsystem.

For corrections, validate the whole revised estimate and display the before/after amounts. Do not subtract a guessed nutrient amount from an aggregate when an item breakdown is unavailable. Pure additions may not silently re-estimate the base to make totals fit. Energy validation must not fabricate P/C/G to force `4P + 4C + 9G = kcal` for foods with other energy sources; include an alcohol fixture with explicit supplied values.

Legacy callers gain clearer wording, the increased meal-description bound with safe overflow handling, and target-resolution regression coverage. Keep their response shape and record flow. They do not gain A47's safe rerouting. Test old/new capability branches separately and state this rollout limit in the live contract.

### 4. Intended specification changes

At Completion, update [server Chat](../specifications/v1-chat.md) operation/target/gate rules and [HTTP contract](../../api-contract.md) with the opt-in schema, limits, failures and examples. Update only the server-owned behavior in [product Chat](../../produto/specifications/chat.md), with a rollout qualification for clients without A47; leave unimplemented Android controls out of current behavior. Add this plan under each affected specification's Provenance. ADR adoption follows its own acceptance rule.

## Out of scope

Android, Room, Figma/golds, nutrition databases or web lookup inside the app, model/effort changes, retroactive records, automatic occupied-slot updates, new infrastructure, changes to the production gate or content retention. A deployed server cannot fix the records already on a tester's phone.

## Validation

1. Unit/route tests: exact addition arithmetic for kcal/P/C/G; preserved base text; full/unknown/empty/skipped targets; corrections/removals; invalid metadata, overlong composition, refusals and no memory effects on rejected actions; release-gate order, force and round cap. Include contradictory model totals versus item sums, a mismatched base slot, and the historical 161-code-point description regression. Test normal descriptions of 499/500/501 code points and composed additions at their own boundary, including commas, accents and supplementary Unicode characters. Valid descriptions remain complete; overflow never yields a truncated actionable estimate, in either capability branch.
2. Contract compatibility: existing callers with each supported capability combination, effective new callers, compact, old slot ids, and malformed numbers/schema. Existing suite: `server/.venv/Scripts/python -m pytest server/tests` (use the available environment without changing another checkout).
3. Synthetic eval matrix: dessert after lunch; interleaved breakfast correction; explicit snack dessert; ambiguous two-target addition; named drink addition with synthetic photo; missing/unavailable volume; correction of a pending addition; second portion versus already recorded food; mixed removal/addition; digest continuation; profile times; supplied alcohol nutrition. Check operation, target, retention of the prior aggregate, addition items, total arithmetic and reply labels. Do not accept only food names in `meal_text` as proof.
   Include a multi-turn dinner sequence with a drink already in DAY, a similar pasta/meat lunch elsewhere in DAY/DIGESTS, an explicit dinner addition, a target clarification and repeated quantities. Assert the same dinner base throughout, no imported lunch food in the addition, no duplicate proposed addition, and base plus delta on every released update. Repeat with raw history and with a digest whose old proposed destination differs from the current DAY state. Use a separate explicit revision case to ensure preservation rules do not block an intended replacement.
4. Run the new cases at least six times each with Luna `none`, and the relevant existing slot/consolidation/record/clarification regressions at least three times. Require every critical new repetition to pass. Record per-check counts, actual model/effort, reasoning tokens, p95 and cost; inspect failures even if the CLI exits zero. No extra agents.
5. `node tools/check-docs.mjs` passes. Finish the SDD git delivery; stop on conflict or red CI.
6. Dev verification after delivery: use the existing deployment runbook/tool, without changing `infra/`; smoke-test an old and an opt-in client with synthetic input and request ids. The server plan does not claim Android/UI validation. Keep release notes clear that old APKs still have the old destination flow.

## Results

Owner approved this named plan on 2026-10-04, including ADR-032. Implementation runs in the isolated `debug-meal-addition` worktree on `codex/s18-meal-additions`; no Android or design implementation is included.

Implemented: opt-in operation metadata and pending proposal context, validation before release gates, server-derived addition totals/copy, full-meal revisions, complete description bounds, output moderation coverage and legacy response compatibility.

Automated validation so far: `C:/Users/Nicolas/Desktop/projetos/Pessoal/dieta-bot/server/.venv/Scripts/python.exe -m pytest server/tests -q --tb=short` from the isolated root: **281 tests and 346 subtests passed** (26.61 seconds). `node tools/check-docs.mjs`: passed. The existing Python environment and root dotenv are used read-only; no shared-checkout files were edited.

Prompt-change provenance: the new authored instructions contain general operation/target/delta rules from ADR-032 and this plan, plus the food-independent rounding/energy constraints in the HTTP contract. No new illustrative user case is embedded in global instructions. The synthetic accounting, meal/profile variants and fictional drink-label image live only in eval fixtures. Existing example cleanup/inventory remains owned by S19; this delivery does not declare the entire existing prompt ADR-033 compliant.

Live evaluation and deployment results are being collected. The diagnostic baseline above is not implementation acceptance.
