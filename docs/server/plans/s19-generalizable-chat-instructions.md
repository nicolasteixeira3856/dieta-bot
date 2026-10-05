# Plan — S19 Generalizable Chat instructions

- Status: Em implementação
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

Owner approved the named plan on 2026-10-05. Implementation uses the isolated `debug-meal-addition` worktree on `codex/s19-generalizable-chat`, based on master `c1e8e76911ae985318b91a8c0c54196fdaabaefa` after S18. Baseline/candidate evidence and the source/provenance review are recorded below as they complete. No Android changes or additional agents.

### Rule traceability and source review

Source review covers the complete assembled legacy Chat, meal-change Chat and compact prefixes, not just the edited constants. Registry ids below are in `server/chat_instructions.py`. Owners are the [Chat specification](../specifications/v1-chat.md), [HTTP contract](../../api-contract.md) and the [content policy](../../content-policy/specifications/content-policy.md). All rows retain rules without illustrative examples. Required JSON/marker syntax and contracted pt-BR copy are protocol rules, not invented user conversations.

| Registry responsibility | Owner | Retained general rule | Coverage |
| --- | --- | --- | --- |
| `product`, `product_meal_changes` | Content policy; Chat 3f/5 | Food-budget scope, contextual short answers, mixed-request separation, prohibited-content/safety categories, no instruction override, stateless/no persistence claims, pt-BR and response fields. | Existing CP2 scope/injection/safety cases and route tests; provenance tests. |
| `context` | Chat 3/3e/4; ADR-033 | Personal foods, profile and memory stay in request context; explicit facts override assumptions; labels apply only to their product and serving basis; DAY alone proves a committed meal. | S19 pairs 01–03, 07, 09, 11 and request-isolation tests. |
| `intent`, `intent_meal_changes` | Chat 3a; ADR-023/028/032 | Eating/portion continuation versus planning, food alone, nutrition questions, firm skip versus pending/hedged meal, matching profile slots; reaffirmation is not new consumption in the opted-in branch. | Existing record/skip/S18 cases; pairs 01, 05, 08, 10. |
| `record`, `record_meal_changes` | Chat 3g/4; ADR-028/029/032 | Clear/unsure record intent, photo question distinction, DAY/local-time eating date, overnight dinner cutoff, discussion date versus eating date, record complaint versus day correction, identifiable pending meal reconstruction. | Existing record-hard/S16 cases; pairs 05, 07, 10, 11. |
| `estimate`, `estimate_meal_changes` | Chat 3b/4/4a/5; HTTP meal-change contract | Draft plus material question, target priority, profile-owned ids/times, complete descriptions, all foods/items, energy including non-macro sources, null rather than fabricated zeros; delta-only draft on add. | Existing slot/clarify/S18 cases; pairs 01, 05–07, 09, 11, 14. |
| `log`, `log_meal_changes` | Chat 4; ADR-026; S17 | Known/omitted/unavailable per food/attribute, useful alternatives, all doubts together, no repeats, honest confidence, complete continuation, accepted seasoning/cup assumptions after an answer, no bare-calorie logs, today's-only copy. Legacy branch retains its whole-meal update and relationship rules. | Existing clarification/unavailability/consolidation cases; pairs 03, 06, 11–13. |
| `meal_changes` | Chat 4/5e; ADR-032; HTTP contract | Resolve new/add/revise and target together, preserve authoritative base, pending-delta correction versus second portion, no guessed operation, target questions, numeric/item invariants and server-composed copy. | Entire S18 set unchanged; pair 11 and opted-in variants of other pairs. |
| `plan` | Chat 3c; ADR-023 | Portion quantities, concise recipe, remaining budget/excess, assumptions without questions, no recomputation of daily totals. | Existing plan cases and pair 08. |
| `history` | Chat 3d; ADR-023/029 | Exact recent day/slot copy; routine first, otherwise two distinct days; ignore brand-only changes, reject differing foods/amounts or a single day; preserve copied nutrition and identify weekday. | Existing habitual/recent cases; pairs 02/04. |
| `memory_use` | Chat 3e; ADR-023/029 | Applicable fact resolves uncertainty, only supplied ids; repeated use of the same temporary reference uses its values. | Existing memory/temp cases; pairs 02, 03, 07, 09. |
| `memory_changes` | Chat 3e/5; ADR-023/029 | Five proposals, permanent versus dynamic, replace/forget/reinforce, routine slots, no one-off or health facts as habits, bounded keys/text. | Existing memory cases and unit tests; pairs 01–04. |
| `temp_references` | Chat 3e; ADR-029 | Capability-gated future references, partial labels preserved without invented values, serving basis, separate T identity, no T reinforce/promotion/deletion-on-use, capacity behavior. | Existing temp cases; pairs 07–09. |
| `digest` | Chat 7; ADR-029; S17 | Food-only user facts, no inferred record/day/slot, scoped unavailability, later measurement precedence, only unanswered answerable questions, literal open-question marker, no new nutrition/advice. | Existing compact cases; pairs 12/13. |

Example-origin audit and removal classification:

| Removed material | Origin assessment | Treatment |
| --- | --- | --- |
| Exact previously discussed meal/quantity combination | Real incident, confirmed by ADR-029 and the discovery audit. | Removed from both Chat branches. Retained only the general distinction between discussion date and eating date and between a portion report and a plan. No anonymized retelling. |
| Named commercial product, label numbers and package size | No eligible independent-synthetic provenance. | Removed. Product/serving/value preservation remains a rule; synthetic eval labels stay request-local. |
| Habitual-meal brand narrative and quantity counterexample | Unknown provenance; therefore ineligible. | Removed both stories, retaining all comparison conditions and exact-copy behavior. |
| Concrete record/skip/photo/day/slot sentences, dessert example, approximate-weight example, memory-key/preference examples and food arithmetic illustration | Unknown provenance; therefore ineligible. | Replaced by semantic categories, preserving each operative distinction in the table above. No new example dishes or user phrases. |
| Compact's illustrated food/question/answer sequence | Unknown provenance; therefore ineligible. | Removed the sequence; preserved pending-description, answered/unavailable attributes, later correction and open-question syntax rules. |
| Numerical low-intake illustration in Chat scope | Illustration, not a policy threshold; no eligible provenance. | Kept the full very-low-intake/safety-support rule without the illustrative number. The scope/refusal behavior is unchanged. |

The retained illustrative-example inventory is deliberately empty. No demonstrated ambiguity required a fictional story in the first candidate. The registry supports future reviewed synthetic examples with stable ids, owner, purpose and exact branch coverage, rejects unknown/real provenance and undeclared assembly fragments, and guards the final Chat/compact instruction argument before generation. These checks do **not** detect arbitrary incident-derived prose disguised as a rule; the source review above is separate required evidence. The ingredient/cup categories used by the accepted after-answer assumption rule remain because they are operative product behavior, not illustrative narratives.

Shared estimate/fit instructions and their original scope constant are byte-identical to the baseline. Chat has its own registered, example-free rendering of the same scope policy; schemas, payload/context serialization, deterministic meal accounting, moderation, limits and model configuration are unchanged. Unit assertions that formerly required literal example sentences now require the corresponding general rule. Existing model-evaluation cases and expectations are not removed or relaxed.

### Evaluation setup

`logs/run_s19.py` delegates to the existing evaluator's `run_effort`/`run_once`, with two workers, actual `gpt-6-luna`, effort `none`, the same pricing/cache/latency methodology, and separate saved baseline/candidate reports. It also retains per-repetition checks, latency and usage for auditing. The captured baseline module and prefixes are ignored local evidence; the original model module is loaded before the route/evaluator for baseline calls. Existing regression inventory: 155 cases, three repetitions each. New coverage: 14 matched pairs, six repetitions per case, with five history-bearing pairs (03, 07, 08, 10, 11), two compact pairs (12/13), and a synthetic-label image/text counterpart (14). Pair 11 compares raw history with a digest. Profiles use arbitrary reordered ids, three or six named meals, and overnight schedules. Eight additional combinations are sealed in `logs/s19-reserved-seal.json`; their outputs are reserved from prompt tuning until the candidate freeze.

Additional evaluator assertions validate exact supplied nutrients, item portions and energy, allowed memory ids, and product/serving/nutrient preservation in a single temporary fact. Existing S18 delta/base checks are reused. Every new expectation is required and every new case is strict; positive estimate, memory-reference or nonempty-digest checks prevent fallback/null from passing through negative substring checks. Variable estimated nutrition is not equated across different foods.
