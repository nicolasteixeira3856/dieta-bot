# Plan — S19 Generalizable Chat instructions

- Status: Aguardando aprovação
- Date: 04/10/2026
- Owning context: `server`
- Executable boundary: `server/` only: model instructions, their assembly tests, existing evaluation cases/checks/runner and synthetic evaluation media when needed.
- Related documentation: server Chat and content-handling specifications, evaluation documentation, and plan/index lifecycle.
- Prerequisite: [S18](completed/s18-meal-additions-and-revisions.md) delivered. Generalize its delivered behavior as well as the existing Chat rules; do not rewrite the same prompt concurrently. This plan does not require D9/A47 delivery and does not block their UI work.

Authorization and delivery follow [SDD](../../sdd/README.md). Approval: `Aprovo o plano docs/server/plans/s19-generalizable-chat-instructions.md. Implemente o plano aprovado.`

## Objective

Make the fixed Chat instructions apply to different people's foods, habits, schedules and ways of speaking, while keeping each person's useful personalization in the supplied context. Replace incident-specific teaching with general rules and demonstrate that behavior transfers to situations absent from prompt examples.

Owner direction (2026-10-04): keep pt-BR responses and America/Sao_Paulo dates for this delivery. This is prompt generalization within the existing product, not an internationalization or public-distribution plan.

## Discovery and diagnosis

Read-only audit of [model instructions](../../../server/llm.py), [context assembly](../../../server/main.py), [evaluation cases](../../../server/evals/cases/) and their governing specifications. The inspected deployed Chat instructions matched this worktree's source. Evaluation inventory below is the planning baseline at commit `3c82f86`; remeasure after S18 rather than treating these counts as delivery evidence.

| Finding | Evidence | Interpretation and action |
| --- | --- | --- |
| An incident's exact meal is a global intent example | `_CHAT_INSTRUCTIONS`, INTENT: the lasagna/rib combination with the owner's reported quantities. Its provenance is documented in [ADR-029](../../produto/adrs/ADR-029-fatos-temporarios-compactacao.md). | Confirmed case-specific content in every user's instructions. Express the distinction between eating now and discussing/planning earlier without that meal or those quantities. |
| A named commercial product and its nutrition appear in global memory instructions | TEMP REFERENCES includes a branded lasagna, serving values and package size. | Illustrative data is unnecessary authority for other users' estimates. Teach preservation of the supplied product, serving basis and numbers without embedding a real product's values in the fixed prefix. Keep user-supplied brands and labels in context. |
| Examples repeatedly center on a narrow set of foods and habits | Meal-description example uses eggs, bread and semiskimmed milk; routine matching uses bread and milk; memory-key examples center on milk, yogurt and breakfast. | A concentration of examples, not proof of a global dietary default. Generalize the rule text and vary only the minimal examples still justified by evaluation. Verify other foods and non-breakfast routines. |
| Prompt examples and regressions overlap closely | Six case request payloads contain the same lasagna/rib quantity combination as the global instruction example. | Passing a familiar example is insufficient evidence of transfer. Preserve regression meaning and add structurally equivalent cases with different foods, amounts, phrasing and profiles. |
| Evaluation profiles are concentrated | Of 126 case files, 120 use the same ordered meal-name/time profile. There are four distinct such profiles. Five cases are compact requests, where profile data is not sent to the summarizer. | Inventory diversity is limited; it is not a measured model failure rate. Expand normal-turn coverage with valid alternative profiles and arbitrary slot ids, and evaluate digest behavior separately. |
| Individual context is intentionally personal | `PROFILE`, `MEMORY`, `DAY`, `RECENT`, `DIGESTS` and `HISTORY` are supplied for each request. | Preserve this personalization. A preference in one request must not become a global instruction, a default for an empty context, or a habit inferred from an example. |

Do not present these findings as a measured overfitting rate or as the established cause of the dinner incident. That incident and its accounting fix are owned by S18. This audit identifies preventable coupling and gaps in evidence.

Several detailed rules are accepted product choices, not evidence of owner-only personalization: habitual-meal lookup, the eating-day cutoff, clarification limits, assuming seasoning amounts after an answer, and the current plan-versus-log flow. Preserve their semantics. A request to generalize examples does not authorize silently changing these rules.

## Sources and boundaries

[Server Chat](../specifications/v1-chat.md), [HTTP contract](../../api-contract.md), [product Chat](../../produto/specifications/chat.md), [ADR-023](../../produto/adrs/ADR-023-chat-v2-memoria-v2.md), [ADR-026](../../produto/adrs/ADR-026-perguntas-antes-da-estimativa.md), [ADR-028](../../produto/adrs/ADR-028-registro-autonomo.md), ADR-029 and S18 define the behavior to retain. [Content policy](../../content-policy/README.md) continues to own scope, injection, moderation, correlation and content handling; this plan does not redefine them.

[ADR-033](../../content-policy/adrs/ADR-033-global-chat-example-provenance.md) owns the durable prompt-authoring restriction requested by the owner. Apply it throughout the refactor and its evaluation examples. Its acceptance does not approve this plan's implementation. If implementation needs a different meal/product rule, return to Planning for that decision instead of hiding it in a wording change.

## Scope

### 1. Map the delivered rules before rewriting

Start from up-to-date master after S18. Record a compact traceability table in Results: instruction responsibility, governing specification/ADR, retained general rule, any retained example with its origin and purpose, and regression coverage. Audit every assembled Chat example under ADR-033, including shared text and digest instructions; unknown provenance does not count as synthetic. Review shared estimate/fit instructions for accidental changes without expanding their behavior.

Separate general product rules, JSON contract requirements, illustrative examples and user-specific facts. Every operative rule must have an owner; every removed phrase must be classified as redundant explanation or replaceable example, not quietly deleted behavior. Preserve the current precedence for user facts, recorded DAY state, pending proposals, history, digests and applicable memory.

Capture a baseline with the existing evaluator before editing the prompt. Record the revision and effective prompt used, including S18's capability branches. Do not use the pre-S18 incident log as that baseline.

### 2. Rewrite fixed instructions around relationships

Replace the exact reported meals, commercial label values and repeated incident narratives with food-independent rules. For example, describe that a temporal reference to an earlier discussion does not change the eating day; do not teach that rule by repeatedly naming a particular meal.

Organize the fixed instructions by responsibility: product behavior, intent/eating day, meal target and operation, portion uncertainty, estimation/response, applicable memory and continuation. Existing policy text keeps its content and authority. Named constants or pure assembly helpers inside `server/` are sufficient if organization helps; do not add a prompt service, framework or dynamic prompt generation.

Preserve every functional distinction, including supplied versus assumed portions, unavailable versus omitted detail, a pending addition versus a second portion, and a proposed action versus committed DAY state. Keep S18's arithmetic, description bounds and failure behavior. Model prose must not replace deterministic guards.

Use a small synthetic example only where it resolves a demonstrated ambiguity or teaches required syntax and satisfies ADR-033; record its origin and purpose in the traceability table. Independently author it from the general rule instead of anonymizing, translating or lightly changing a real case. Do not replace a long list of personal examples with an equally long catalog of regional dishes. Examples are illustrations, never defaults for foods, nutrients, brands, preferences or schedules.

Within each existing capability branch, instructions remain identical across users and requests. All personal values stay in the supplied context; the existing slot/fact enums remain request-specific in the schema. Do not select a different prefix based on an inferred demographic, dietary identity or the installation making the request.

Keep the illustrative examples in a small explicit inventory alongside the server prompt, with stable ids, synthetic provenance, owning rule and purpose. Assemble their text through that inventory so tests can verify every declared example is accounted for and an undeclared inserted example fails validation. Cover all global Chat/compact assembly paths. Review prose changes too: examples disguised as ordinary instructions cannot be reliably detected by a registry or string blacklist alone. Do not load logs, evaluation fixtures or historical ADR narratives into the instruction prefix. Include the provenance check in the existing server test suite; no new tooling boundary or dependency is needed.

### 3. Preserve context-driven personalization

Verify that profile meal names, times and ids determine targets. Do not assume the common test profile's slot ids or number of meals. Preserve the documented time fallback when a meal has not been identified, and the accepted eating-day rules.

Applicable memory should resolve the user's uncertainty; an explicit current statement can override a previous assumption. Empty memory is valid. A new user must not inherit the milk type, breakfast, product label or routine from a prompt example or another synthetic request. An ordinary estimate may still use a clearly stated portion assumption under the existing clarification rules; that is different from claiming a remembered personal habit.

Keep supplied label data and its serving basis attached to its specific food. A temporary reference for one product does not establish the values of a different product. Preserve the accepted rule for copying habitual meals across a brand-only difference; this plan does not redefine routine matching or memory retention.

Keep context fields, selection limits and client serialization as delivered. Any discovered need to change Android context construction is reported as a separate scope decision; it is not implemented here. No deletion or rewriting of users' memory or recorded meals.

### 4. Add evaluations that test transfer

Use the existing [evaluator](../../../server/evals/run.py) and [checks](../../../server/evals/checks.py). Extend required assertions only when existing checks cannot express a relevant invariant, and unit-test those assertions. Reuse S18's operation/arithmetic checks rather than duplicating them.

Create at least twelve matched pairs of synthetic cases across the families below. A pair changes incidental details while retaining the semantic situation; expectations distinguish fields that must remain equivalent from nutrition values that may legitimately differ. Include both legacy and S18 callers where applicable. At least four pairs exercise continuation using supplied history, and at least two exercise compact output/continuation.

| Family | Variations | Required evidence |
| --- | --- | --- |
| First use and memory | Empty context; relevant routine; unrelated memory; explicit current override; different request with different habits | Useful question/estimate under the contract; only supplied fact ids; no unsupported habit or example food. |
| Profiles and slots | Three versus six meals; custom names; reordered/nonsequential ids; different daytime times; valid overnight schedule | Correct user-named slot and eating day; no dependence on familiar ids or the common test schedule. |
| Meal composition | Different food traditions, plant-based and mixed meals, drinks and desserts; grams, counts and household measures already supported | Preserve specified foods and portions; no imported meat, dairy, supplement or regional default. Unknown material details follow the existing question rules. |
| Labels and temporary references | Fictional products and brands; different serving bases; partial nutrient data; same product later versus another product | Use only the applicable supplied values and ids; preserve the serving basis; do not borrow the fixed prompt's example numbers. |
| Language and temporal references | Brief/colloquial pt-BR, paraphrases and ordinary typos; yesterday modifies buying/planning/discussion versus eating | Equivalent intent/day/operation for equivalent meaning; progress on identifiable food without requiring a familiar sentence. |
| Updates and continuation | Addition/correction to different meals; an unrelated interleaved turn; pending clarification; raw history versus digest | S18 target/base/operation and arithmetic preserved; no omitted base or duplicate proposed addition. |

Run at least one synthetic-photo case and its textual counterpart to check that removing familiar dish examples does not break photo-based context. Existing image mechanics and nutrition uncertainty remain unchanged.

Prepare eight additional synthetic cases whose food/profile/phrase combinations are absent from all retained prompt examples. Reserve them from prompt tuning until the candidate is frozen. If a reserved case drives a further prompt edit, classify it as development evidence and add fresh reserved coverage before claiming a final transfer result. Do not claim a population-wide accuracy rate from this small set.

A substring match alone is insufficient. Require a useful estimate or eligible clarification as appropriate, correct intent/target/operation, relevant memory, preserved foods and valid arithmetic. Negative checks must not pass through an empty response or fallback. Compare semantic invariants, not exact wording or identical model-estimated nutrition for different foods. Deterministic supplied-label and ledger arithmetic can require exact expected values.

### 5. Intended documentation changes

At Completion, update server Chat's rule about fixed instructions versus per-request context and its provenance, linking to the owning policy rather than duplicating it. Add the authored-instruction/provenance boundary to [content handling](../../content-policy/specifications/content-policy.md) under ADR-033 and add this plan to its Provenance, without changing moderation or retention. Update the server README's evaluation guidance to route to the implemented transfer cases/checks, without copying their inventory or outcomes. Results in this plan own the rule map, example-origin audit, commands and measured comparison.

The live API contract and product rules retain S18's delivered semantics. Do not rewrite accepted ADR bodies or modify the current specifications during this planning request.

## Out of scope

Android/Room/UI/Figma changes; language or timezone changes; internationalization; account or cross-device memory systems; new product screens; changed diet advice or nutrient targets; nutrition lookup/databases; model/effort changes; prompt fine-tuning; new dependencies or additional agents; changing scope/moderation/logging policies; public distribution or infrastructure changes. S18 retains ownership of meal-update accounting and description limits.

## Validation and acceptance

1. The rule map accounts for every operative rule and the example-origin audit satisfies ADR-033 for the owner and every other user. No global example is a real-case reproduction, including anonymized/paraphrased variants. Fixed instructions contain no owner meal transcript or commercial label values. Their byte content does not vary by the supplied user's foods, habits or profile within a capability branch. Scope/refusal controls are preserved, not removed to reduce prompt length.
2. Unit tests verify instruction assembly, the declared example inventory, context separation and any added evaluator assertions, including negative cases for undeclared examples, real/unknown provenance, context inserted into instructions, and false passes on null/fallback. Record the source review needed for provenance alongside automated results; do not claim automated semantic-origin detection. Run the existing server test suite using the available Python environment without modifying another checkout.
3. Run the existing regression suite before and after the refactor with at least three repetitions per case, and the new matched-pair/reserved sets at least six times per case on both the baseline and candidate. Use the pinned model and effort. Preserve existing strict checks; require every critical new repetition to pass for intent/day, slot/base/operation, food preservation, supplied-label arithmetic, absence of unsupported memory and no action on failure.
4. Report results by case family and capability, including useful-response failures, not only one aggregate score. Investigate every newly failing incumbent case; do not delete a case or weaken its expectation to make the rewrite pass. Unresolved semantic regressions block completion. Stochastic baseline failures must be recorded separately from demonstrated regressions.
5. Report fixed-prompt characters and actual usage tokens, reasoning tokens, p95, cache usage and estimated cost using the same evaluator methodology for both candidates. Seek less redundant text; no length reduction compensates for a lost rule. No subagent is a substitute for actual API evaluation with Luna `none`.
6. `node tools/check-docs.mjs` and `git diff --check` pass. Complete SDD git delivery; stop on conflict or red CI. Dev verification uses the existing deploy tooling without editing infrastructure: smoke-test contrasting synthetic profiles through the HTTP route and record request ids. No APK distribution is required.

## Results

Planning only. Source inspection and the evaluation-file inventory were performed; no new prompt, evaluator implementation, model benchmark, deployment or application change was made. Populate the rule map and baseline/candidate evidence during implementation. This plan does not claim generalization has already improved.
