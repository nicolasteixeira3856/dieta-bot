# Plan — S17 Respect unavailable meal details when clarifying an estimate

- Status: Aguardando aprovação
- Date: 2026-10-03
- Owning context: `server`
- Executable boundary: `server/`
- Affected code: `server/llm.py`, `server/evals/checks.py`, `server/evals/cases/`, `server/tests/test_clarify.py`, `server/tests/test_evals.py`.
- Prerequisites: the server behavior delivered by [S16](completed/s16-dia-da-refeicao-fatos-temporarios.md), [ADR-026](../../produto/adrs/ADR-026-perguntas-antes-da-estimativa.md) and [ADR-029](../../produto/adrs/ADR-029-fatos-temporarios-compactacao.md). No Android delivery or Stitch gate is required.

## Authorization and isolation

This is documentation-only planning. The owner requested a separate worktree because another model is working in the primary checkout. Planning was based on master `0a8f663`, in a dedicated worktree and branch. Do not switch, reset, stash, clean, commit or edit the primary checkout, or include its uncommitted Android work.

Implementation starts only after explicit approval naming this file:

> Aprovo o plano `docs/server/plans/s17-unavailable-meal-details.md`. Implemente o plano aprovado.

Before approved implementation, refresh the isolated checkout against the then-current master, resolve documentation paths there and confirm the scope still applies. Implementation, checks and their outputs stay in that checkout. The [SDD process](../../sdd/README.md) governs approval, delivery and lifecycle; Git delivery must not switch or overwrite another worktree's checked-out branch. A conflicting prerequisite or uncovered product decision returns to Planning.

## Goal

When the user says they do not know a meal detail, Chat does not ask for that same detail again, including a paraphrase or another unit. It may ask for a useful alternative the user has not ruled out. Once no answerable material question remains, it estimates the whole meal with explicit assumptions and honest uncertainty, without requiring the user to exhaust the question cap or press the force button.

For the reported dinner, weight was explicitly unavailable. A useful remaining clarification would concern size; asking for an approximate weight still asks for the unavailable information.

## Evidence and current behavior

Read-only inspection of dev request `e23865e9-61c3-4caa-8e60-6b12976fa7f5`, at 20:12:14 America/Sao_Paulo on 2026-10-03, confirmed that the complete message and photo reached the server. The message identified two pizza pastries and two sweet-pastry portions, and explicitly said their individual weights were unknown. The model nevertheless asked for approximate weight or size. The response preserved that question; `clarify_rounds` was zero and `clarify` was `asked`.

The relevant code explains why this is possible:

- `_CHAT_INSTRUCTIONS` asks for material missing details and forbids asking what the conversation already answers. It does not explicitly distinguish an omitted detail from a detail the user cannot supply.
- `shaping.clarify_gate` compares a proposed question only with earlier assistant questions. It does not interpret the current user's inability to answer. This first-round failure therefore is not a repeated-question match.
- The gate already releases a nonempty estimate when the raw model question is null/empty, including at low confidence. A new release rule or a forced high-confidence value is unnecessary.
- `_DIGEST_INSTRUCTIONS` keeps user facts and pending questions, but does not explicitly preserve unavailable attributes or close a question answered with an inability to provide the detail.
- Existing `pergunta-forcada` and `registro-forcado` fixtures include an unknown quantity in history, but the tested turn has `force_estimate: true`. They do not exercise ordinary handling of the initial inability to answer.

The reported text/photo remains outside git. New cases must be manually rewritten or synthetic under the existing [evaluation workflow](../README.md#chat-evaluation).

## Decision fit

This refines which clarifying questions are useful under ADR-026 decisions 1–3, and which user facts survive compaction under ADR-029 decision 3. It preserves the maximum rounds, all-doubts-together rule, force-button timing, recording gates and fact-only summaries. It introduces no architecture, persistence or protocol change, so this plan does not propose a successor ADR or edit an accepted ADR body.

The distinction below is reasoning guidance in the fixed prompt, not a new stored state machine or API field.

## Scope

### 1. Clarification instructions

Revise the existing question instructions in `_CHAT_INSTRUCTIONS` together, rather than appending a contradictory exception:

1. **Known or usable:** use a stated value, range, size, count or household measure. An approximate value is still useful. For example, a user who did not weigh the food but supplies an approximate weight has supplied that approximation.
2. **Not supplied:** ask only when the missing detail materially changes the estimate and is not already answered by the current message, history, digest, applicable memory or photo context.
3. **Explicitly unavailable:** the user says they do not know, remember, have or cannot determine that detail. Do not ask for the same information again, even in the first round, with a synonym, another weight unit, or an approximate qualifier. Inability to supply a detail is a valid answer about its availability.

Apply this per food, meal and attribute. Unknown pastry weight does not erase a stated count, filling, dessert weight or known drink volume. It does not ban asking about an unrelated answerable detail. The same principle applies to an explicitly unknown brand, preparation or ingredient; it is not a blanket ban on questions containing a weight word.

For an unavailable weight, use counts, a supplied size/measure and available visual evidence. If size is still materially uncertain, a short question about small/medium/large or a familiar comparison is allowed. Do not replace a weight question with another request for a precise measurement the user has already ruled out. A photo supports an estimate, not a claim that weight was measured.

Ask the remaining useful questions together, within ADR-026's existing limit. Do not ask again for a supplied alternative or cycle back to weight after the user gives a size. If the user also cannot provide the alternative, assume a plausible portion using the available information and return the whole-meal estimate with raw `estimate.question: null`. Preserve medium/low confidence when the portion is still assumed; do not raise it solely to release the gate. State the material assumption briefly in `reply`.

Use current text, `HISTORY` and `DIGESTS`. A later explicit measurement or correction overrides an earlier unavailable value for that food. Do not save a meal-specific inability to answer as a permanent, dynamic or temporary memory fact.

Example user-facing clarification, only if size still needs asking:

> Os pastéis eram pequenos, médios ou grandes? E os sonhos?

The example is not mandatory copy or a required extra question when the context already supports an estimate. Preserve all reported foods and counts, including fractional portions, when estimating after clarification.

### 2. Digest instructions

Extend `_DIGEST_INSTRUCTIONS` to preserve the user's explicit inability to supply an attribute, with its food/meal scope, alongside the known counts, fillings, sizes and answers. Do not turn a missing statement into an assertion that the user does not know.

- A question answered with an inability to provide that detail is closed for that detail. Do not list it as still pending.
- An assistant's later repetition does not reopen a detail already declared unavailable by the user.
- For a compound question, retain only an actually unresolved, answerable part as the open question; do not invent a new question. A weight answered as unknown can be closed while a separately asked filling question stays open.
- A later supplied measurement replaces the earlier unavailability. Preserve the new value without continuing to describe that attribute as unknown.
- Keep the existing food facts, pending photo description, limits and prohibitions on assumed day/slot, unconfirmed estimates and record status. Keep content-policy handling unchanged.

The result stays ordinary digest prose in the existing field. No new client parser, `DayDigestEntity` field or compaction boundary is introduced. The fix works with an old client's whole-block compaction as well as a client that retains a raw tail.

### 3. Gates and compatibility

Keep `main.py`, HTTP fields, schemas, `shaping.clarify_gate`, `record_gate`, round counting and similarity thresholds unchanged. Use the existing null-question release path. A clear meal of today still follows the existing record rules after release; an unresolved useful question still holds the estimate. Plans, scope refusals and generation failures retain their current behavior.

Instructions remain fixed across requests, without per-message instruction variants. Model and reasoning effort do not change. Test current v4 requests and a v5 capability variant; no new capability flag or APK is required. Keep legacy response shaping covered by the existing tests.

### 4. Evaluator and cases

Use the existing `required`, `strict`, `top_question_not`, `question_not`, `estimate`, `meal_text_has`, `record`, `digest_has` and `digest_not` checks. Add two small evaluator-only expectations to `evals/checks.py`:

- `meal_progress: present | absent`: present when the shaped output contains a nonempty estimate object or a nonblank top-level question. Absent when both are missing/empty. A fallback, refusal or empty log cannot pass a required positive-progress expectation just because it avoided a forbidden term. This is a structural check, not a semantic judge of the question.
- `confidence`: a string or list of accepted confidence values, checked on the shaped estimate. No estimate fails this check. This covers honest uncertainty when all usable portion measurements are unavailable.

No live model judge, new test framework or second generation call. Extend `test_evals.py`, including its exhaustive `KNOWN` expectation test, for both checks and required behavior. Keep the real compact path and moderation in the evaluator unchanged.

New fixtures use tag `s17` plus `unavailable-detail` or `digest`, `refusal: none`, v4 request fields for chat, and `force_estimate: false`. Ordinary chat cases use `clarify_rounds: 0` or `1`, so the cap cannot hide a failure. Compact cases set `compact: true` and supply `messages`. Mark applicable positive and negative expectations `required`.

| Case id | Situation | Required outcome |
|---|---|---|
| `unavailable-weight-first` **strict** | First dinner message: two filled pastries plus a whole and half sweet pastry; weight explicitly unknown. Include a synthetic meal image. | Log; `meal_progress: present`; no weight request. A useful size question or a released estimate is allowed. |
| `unavailable-weight-answer` **strict** | An assistant asked the weight; the user answers that they do not know it. One round, no force. | Useful progress without asking weight again. Preserve the original meal. |
| `unavailable-weight-size-known` **strict** | Weight unknown, counts/fillings and sizes already supplied for every food. | Estimate present, no question, known dinner slot, auto record for today's stated meal; all foods and fractional portions retained. |
| `unavailable-weight-and-size` **strict** | Identified meal and counts; the user cannot provide either weight or size and says no other measurements are available. | Estimate present with assumed portions, question absent, confidence medium/low; a brief assumption in reply. |
| `unavailable-weight-food-scope` | Only one food has unavailable weight; another has an explicit gram value and a drink has an explicit volume. | Keep the supplied values and food-specific scope; do not ask again for unavailable or already supplied values. |
| `unavailable-weight-now-known` | Earlier weight unknown; current user explicitly supplies a newly measured weight for that same food. | Use the new measurement, return the estimate and do not keep treating that attribute as unavailable. |
| `unavailable-brand-known-portion` | Exact food type and portion known; the user explicitly does not know the brand. | Estimate present without a brand question; no meal-specific unavailability memory update. |
| `unavailable-detail-after-digest` **strict** | Digest preserves unavailable pastry weight, all meal foods and supplied sizes; current message asks to estimate that dinner. Raw history empty. | Estimate present, no renewed weight/size question; whole meal and slot preserved. |
| `digest-unavailable-weight` **strict** | Assistant asks weight; user says it is unknown, gives count and size. | Nonempty digest retains food, unavailable weight, count and size; no pending weight question. |
| `digest-unavailable-partial-answer` **strict** | Assistant asks weight and filling; user says weight is unknown, but does not answer filling. | Digest keeps weight unavailability and the still-open filling question. Weight is not an open question. |
| `digest-unavailable-now-known` | User first lacks the weight, then supplies a measured value. | Digest preserves the later measurement without a stale unknown-weight assertion or pending weight question. |

For weight-forbidden cases, check the actual question fields with normalized terms covering weight/grams/kilograms and common requests to weigh food. Do not globally ban those words from `reply`: stating an assumed weight is legitimate. Inspect every question and interrogative sentence in replies manually for paraphrases and scope; substring checks alone cannot prove semantic compliance. Digest checks require nonempty content and food/attribute terms, with manual verification that the attribute is explicitly unavailable rather than simply omitted. For released estimates, inspect food counts, fractions and user-supplied measurements; do not accept a correct question policy that loses part of the meal.

Reuse existing question-before-estimate, all-doubts-together, repeat, force and cap fixtures as controls. They must still pass; this change must not silence every clarification. Do not weaken existing expectations or change known-failure thresholds to accommodate a prompt regression.

### 5. Unit and route coverage

- Extend the existing instruction assertions in `tests/test_clarify.py` for unavailable details, scoped alternatives, current corrections and confidence, including v4/v5 instruction equality where relevant.
- Reuse `test_row2_empty_question_releases_without_fallback_question`, which already covers low confidence. Extend its assertions only if needed to verify preserved confidence; do not duplicate it.
- Cover a mocked v4/v5 low-confidence, null-question estimate through the route: response estimate retained, no question, unchanged record behavior, and no held `question_slot` on release. Keep existing tests for useful held questions, force, cap, repetition and legacy output.
- Cover both new evaluator checks with passing and failing outputs, including blank question, empty estimate, fallback/refusal, wrong confidence and absent estimate. Digest moderation and failure tests remain in the full suite.
- Prompt-string assertions are wiring checks; the real-model cases and semantic review are the behavior evidence.

## Intended specification changes

Write these only at Completion; this planning change does not rewrite live behavior:

- [Server Chat](../specifications/v1-chat.md): rule 4 distinguishes omitted from explicitly unavailable details, scopes alternatives and later corrections, and permits honest uncertain estimates without another question. Rule 7 preserves unavailability and closes only the answered/unavailable part of a question. Rule 5c remains the same release algorithm. Add S17 to Provenance.
- [Product Chat](../../produto/specifications/chat.md): refine rule 14 to describe useful alternatives and estimating when the user cannot provide more detail, linking to server rules and preserving ADR-026's limits and UI. Add S17 to Provenance. Reconcile with concurrent Android documentation in the isolated checkout, without overwriting that delivery.
- [HTTP contract](../../api-contract.md): clarify the existing question/release and digest prose behavior. No new or renamed wire fields or changed validation bounds.
- [Server README](../README.md): document the two evaluator expectations. Keep status and implementation evidence in this plan.

## Out of scope

- Android code, Room, raw-tail selection, memory storage, UI, golds, SDKs, new model/effort, extra model calls or dependencies.
- New structured unknown-attribute fields, persistent state, keyword/regex suppression in the runtime gate, or stripping every weight question regardless of food context.
- Changes to scope/moderation, content logging, retention, record intent, eating day, numeric nutrition formulas or the existing hard stop.
- Rewriting the historical dev conversation, correcting the owner's meal record, copying tester content into fixtures, or promising exact weight from a photo.
- Retuning unrelated known evaluator failures. A demonstrated new regression must be resolved within scope or returned for a scope decision.

## Risks and controls

- Prompt compliance remains probabilistic. This plan does not add a deterministic semantic guard; strict cases, full-suite comparisons and live checks measure the improvement, while the existing hard stop remains in place.
- Overcorrecting could suppress useful questions or treat one food's unknown weight as a property of the whole meal. Food-specific cases and the existing clarification controls cover that risk.
- A digest could incorrectly close every part of a compound question. The partial-answer case requires retaining the answerable unresolved part, and the live round trip checks the returned digest's effect.
- Negative term checks can miss paraphrases or pass an empty response. Required positive progress, confidence checks and semantic review are acceptance requirements, not optional polish.

## Planned validation and acceptance

1. Use a Python environment and test outputs belonging to the isolated checkout; do not modify the other checkout's environment or `.env`. Never print or commit credentials. Before prompt edits, measure the same existing cases on current master and retain the baseline commit/report for comparison.
2. From the isolated repository root, `server/.venv/Scripts/python.exe -m pytest server/tests` must pass.
3. From its `server/`, run `.venv/Scripts/python.exe -m evals.run --effort none --repeat 3 --tag s17 --workers 2`, then the full suite with the same effort/repeat/workers and no tag. Every new strict case requires 3/3; every other new case at least 2/3. Review the questions, assumptions, portion preservation and digests using the semantic checklist above, and record findings alongside machine results.
4. Existing case results must not regress against the measured master baseline. Compare apparent drops with six repetitions on both revisions, as in S16; record repeat counts and errors, not just the final case label. Do not run baseline/candidate suites simultaneously. Record pass rate, p95 and reported cost in Results.
5. After the local criteria pass, deploy only the approved server change with `tools/deploy-gcp.ps1` using the [dev runbook](../deploy-gcp.md). No shared dev deployment during another task's live server validation; coordinate the deployment window without switching its checkout or sending unsolicited messages to another chat.
6. Send synthetic dev requests with `X-Request-Id: s17-*`: initial unavailable weight, a size answer, all measurements unavailable, and a compact-then-chat sequence using the digest actually returned. Confirm the first response does not request weight, the size answer releases the whole estimate without circling back, an assumed estimate can be released at medium/low confidence, and compaction does not reopen an unavailable detail. Inspect only those request IDs using `tools/pull-conversations.ps1`; verify no fallback masked a failure. These calls can be performed by the implementing agent; no APK build or UI comparison is required.
7. Run `node tools/check-docs.mjs` and `git diff --check`. Record executed commands, real numbers, semantic/manual evidence and any pending item in Results before applying the SDD lifecycle and scoped Git delivery.

Acceptance requires useful progress as well as the absence of the bad question. A null estimate with no useful question, lost food, a silently ignored supplied measurement, or unjustified high confidence is not a successful fix.

## Results

To be filled at Completion with implementation, automated evaluation, semantic review and dev evidence. No implementation or runtime validation is authorized by creating this plan.
