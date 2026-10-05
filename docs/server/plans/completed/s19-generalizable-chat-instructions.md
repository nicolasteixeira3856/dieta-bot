# Plan — S19 Generalizable Chat instructions

- Status: Concluído
- Date: 04/10/2026
- Owning context: `server`
- Executable boundary: `server/` only: model instructions, their assembly tests, existing evaluation cases/checks/runner and synthetic evaluation media when needed.
- Related documentation: server Chat and content-handling specifications, evaluation documentation, and plan/index lifecycle.
- Prerequisite: [S18](s18-meal-additions-and-revisions.md) delivered. Generalize its delivered behavior as well as the existing Chat rules; do not rewrite the same prompt concurrently. This plan does not require D9/A47 delivery and does not block their UI work.
- Integration note: the owner explicitly requested source integration into master before behavioral acceptance. Read [the continuation handoff](#owner-directed-master-integration-and-continuation) before resuming; integration is not Completion or deployment approval.
- Continuation: the remaining work is scoped in [Continuation scope — pt-BR cue lexicon](#continuation-scope--pt-br-cue-lexicon). It changes the approved scope and acceptance gate, so it needs its own approval before any code or paid evaluation.

Authorization and delivery follow [SDD](../../../sdd/README.md). Approval: `Aprovo o plano docs/server/plans/s19-generalizable-chat-instructions.md. Implemente o plano aprovado.`

## Objective

Make the fixed Chat instructions apply to different people's foods, habits, schedules and ways of speaking, while keeping each person's useful personalization in the supplied context. Replace incident-specific teaching with general rules and demonstrate that behavior transfers to situations absent from prompt examples.

Owner direction (2026-10-04): keep pt-BR responses and America/Sao_Paulo dates for this delivery. This is prompt generalization within the existing product, not an internationalization or public-distribution plan.

## Discovery and diagnosis

Read-only audit of [model instructions](../../../../server/llm.py), [context assembly](../../../../server/main.py), [evaluation cases](../../../../server/evals/cases/) and their governing specifications. The inspected deployed Chat instructions matched this worktree's source. Evaluation inventory below is the planning baseline at commit `3c82f86`; remeasure after S18 rather than treating these counts as delivery evidence.

| Finding | Evidence | Interpretation and action |
| --- | --- | --- |
| An incident's exact meal is a global intent example | `_CHAT_INSTRUCTIONS`, INTENT: the lasagna/rib combination with the owner's reported quantities. Its provenance is documented in [ADR-029](../../../produto/adrs/ADR-029-fatos-temporarios-compactacao.md). | Confirmed case-specific content in every user's instructions. Express the distinction between eating now and discussing/planning earlier without that meal or those quantities. |
| A named commercial product and its nutrition appear in global memory instructions | TEMP REFERENCES includes a branded lasagna, serving values and package size. | Illustrative data is unnecessary authority for other users' estimates. Teach preservation of the supplied product, serving basis and numbers without embedding a real product's values in the fixed prefix. Keep user-supplied brands and labels in context. |
| Examples repeatedly center on a narrow set of foods and habits | Meal-description example uses eggs, bread and semiskimmed milk; routine matching uses bread and milk; memory-key examples center on milk, yogurt and breakfast. | A concentration of examples, not proof of a global dietary default. Generalize the rule text and vary only the minimal examples still justified by evaluation. Verify other foods and non-breakfast routines. |
| Prompt examples and regressions overlap closely | Six case request payloads contain the same lasagna/rib quantity combination as the global instruction example. | Passing a familiar example is insufficient evidence of transfer. Preserve regression meaning and add structurally equivalent cases with different foods, amounts, phrasing and profiles. |
| Evaluation profiles are concentrated | Of 126 case files, 120 use the same ordered meal-name/time profile. There are four distinct such profiles. Five cases are compact requests, where profile data is not sent to the summarizer. | Inventory diversity is limited; it is not a measured model failure rate. Expand normal-turn coverage with valid alternative profiles and arbitrary slot ids, and evaluate digest behavior separately. |
| Individual context is intentionally personal | `PROFILE`, `MEMORY`, `DAY`, `RECENT`, `DIGESTS` and `HISTORY` are supplied for each request. | Preserve this personalization. A preference in one request must not become a global instruction, a default for an empty context, or a habit inferred from an example. |

Do not present these findings as a measured overfitting rate or as the established cause of the dinner incident. That incident and its accounting fix are owned by S18. This audit identifies preventable coupling and gaps in evidence.

Several detailed rules are accepted product choices, not evidence of owner-only personalization: habitual-meal lookup, the eating-day cutoff, clarification limits, assuming seasoning amounts after an answer, and the current plan-versus-log flow. Preserve their semantics. A request to generalize examples does not authorize silently changing these rules.

## Sources and boundaries

[Server Chat](../../specifications/v1-chat.md), [HTTP contract](../../../api-contract.md), [product Chat](../../../produto/specifications/chat.md), [ADR-023](../../../produto/adrs/ADR-023-chat-v2-memoria-v2.md), [ADR-026](../../../produto/adrs/ADR-026-perguntas-antes-da-estimativa.md), [ADR-028](../../../produto/adrs/ADR-028-registro-autonomo.md), ADR-029 and S18 define the behavior to retain. [Content policy](../../../content-policy/README.md) continues to own scope, injection, moderation, correlation and content handling; this plan does not redefine them.

[ADR-033](../../../content-policy/adrs/ADR-033-global-chat-example-provenance.md) owns the durable prompt-authoring restriction requested by the owner. Apply it throughout the refactor and its evaluation examples. Its acceptance does not approve this plan's implementation. If implementation needs a different meal/product rule, return to Planning for that decision instead of hiding it in a wording change.

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

Use the existing [evaluator](../../../../server/evals/run.py) and [checks](../../../../server/evals/checks.py). Extend required assertions only when existing checks cannot express a relevant invariant, and unit-test those assertions. Reuse S18's operation/arithmetic checks rather than duplicating them.

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

At Completion, update server Chat's rule about fixed instructions versus per-request context and its provenance, linking to the owning policy rather than duplicating it. Add the authored-instruction/provenance boundary to [content handling](../../../content-policy/specifications/content-policy.md) under ADR-033 and add this plan to its Provenance, without changing moderation or retention. Update the server README's evaluation guidance to route to the implemented transfer cases/checks, without copying their inventory or outcomes. Results in this plan own the rule map, example-origin audit, commands and measured comparison.

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

Source review covers the complete assembled legacy Chat, meal-change Chat and compact prefixes, not just the edited constants. Registry ids below are in `server/chat_instructions.py`. Owners are the [Chat specification](../../specifications/v1-chat.md), [HTTP contract](../../../api-contract.md) and the [content policy](../../../content-policy/specifications/content-policy.md). Rule blocks retain general semantics; separately registered illustrative material is audited below. Required JSON/marker syntax and contracted pt-BR copy are protocol rules, not invented user conversations.

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
| `memory_changes` | Chat 3e/5; ADR-023/029 | Five proposals, permanent versus dynamic, replace/forget/reinforce, routine slots, no one-off or health facts as habits, bounded keys/text, permanent capacity and consent before replacement. | Existing memory cases and unit tests; pairs 01–04. |
| `temp_references` | Chat 3e; ADR-029 | Capability-gated future references, partial labels preserved without invented values, serving basis, separate T identity, no T reinforce/promotion/deletion-on-use. | Existing temp cases; pairs 07–09. |
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

The first candidate deliberately used an empty illustrative-example inventory. The registry requires stable ids, owner, purpose and exact branch coverage for any reviewed synthetic examples, rejects unknown/real provenance and undeclared assembly fragments, and guards the final Chat/compact instruction argument before generation. These checks do **not** detect arbitrary incident-derived prose disguised as a rule; the source review above is separate required evidence. The ingredient/cup categories used by the accepted after-answer assumption rule remain because they are operative product behavior, not illustrative narratives.

Shared estimate/fit instructions and their original scope constant are byte-identical to the baseline. Chat has its own registered, example-free rendering of the same scope policy; schemas, payload/context serialization, deterministic meal accounting, moderation, limits and model configuration are unchanged. Unit assertions that formerly required literal example sentences now require the corresponding general rule. Existing model-evaluation cases and expectations are not removed or relaxed.

### Evaluation setup

`logs/run_s19.py` delegates to the existing evaluator's `run_effort`/`run_once`, with two workers, actual `gpt-6-luna`, effort `none`, the same pricing/cache/latency methodology, and separate saved baseline/candidate reports. It also retains per-repetition checks, latency and usage for auditing. The captured baseline module and prefixes are ignored local evidence; the original model module is loaded before the route/evaluator for baseline calls. Existing regression inventory: 155 cases, three repetitions each. New coverage: 14 matched pairs, six repetitions per case, with five history-bearing pairs (03, 07, 08, 10, 11), two compact pairs (12/13), and a synthetic-label image/text counterpart (14). Pair 11 compares raw history with a digest. Profiles use arbitrary reordered ids, three or six named meals, and overnight schedules. Eight additional combinations are sealed in `logs/s19-reserved-seal.json`; their outputs are reserved from prompt tuning until the candidate freeze.

Additional evaluator assertions validate exact supplied nutrients, item portions and energy, allowed memory ids, and product/serving/nutrient preservation in a single temporary fact. Existing S18 delta/base checks are reused. Every new expectation is required and every new case is strict; positive estimate, memory-reference or nonempty-digest checks prevent fallback/null from passing through negative substring checks. Variable estimated nutrition is not equated across different foods.

### Baseline evidence

`logs/run_s19.py baseline existing 3`: 155 cases, **151/155 case verdicts passed**, **456/465 repetitions**. Legacy: 121/124 cases and 364/372 repetitions; compact: 5/5 and 15/15; meal changes: 25/26 and 77/78. The strict flag remains effective, so a two-of-three result can fail a strict case. Report: `logs/s19-baseline-existing-3-2026-10-05T10-05-35-03-00.json` and `logs/s19-audit-baseline-existing.json`.

Actual model/effort: `gpt-6-luna` / `none`; reasoning tokens **0**. Input **2,414,247**, cached input **2,290,161**, output **75,462**; p50 **4390 ms**, p95 **5628 ms**; estimated **US$0.0730** at the evaluator's configured rates. Baseline fixed-prefix lengths: legacy **19,676**, meal changes **23,414**, compact **2,633** characters. Full prefix snapshots and SHA-256 values are in the ignored evaluation artifacts.

All baseline failures were inspected. `cafe-resposta-leite` (0/3) receives null model question with medium confidence; the unchanged pre-v3 shaping path inserts the contracted generic confirmation, which conflicts with that case's question-absent expectation. Do not inflate confidence or relax that test to make it pass. `memoria-cheia` (0/3) returns an acknowledgement rather than the permanent-capacity question. During organization by responsibility, the unchanged permanent-capacity/consent rule was moved out of TEMP REFERENCES into MEMORY CHANGES; this is not a new memory behavior. `mesmo-cafe-ontem-recent` (2/3) asked instead of copying once. `s18-interleaved-correction` (2/3, strict) had a contradictory non-null base with null suggested target once, rejected by the existing guard with no action. `unavailable-weight-size-known` (2/3, strict) had one moderation connection failure. These precede the candidate and are not evidence of S19 regressions.

The baseline matched-pair run used 28 cases × six repetitions. Before comparison, two defects in the **new** oracles were corrected: pair 08 incorrectly required a null plan estimate despite the v5 contract, and pair 12 matched a singular food noun against a valid plural. Pair 08 now requires an estimate with the supplied kcal/protein plus the exact supplied numeric set in its saved partial reference (no invented missing nutrients). Pair 12 uses the food stem and ingredient and more strictly rejects any question mark or pending-question explanation after closure. No incumbent case or expectation changed. Saved baseline responses were rescored without new API calls; original evidence is retained in `logs/s19-baseline-pairs-6-2026-10-05T10-11-51-03-00.json`, and the authoritative corrected score is `logs/s19-baseline-pairs-6-2026-10-05T10-15-33-03-00-rescored.json`.

Corrected baseline: **22/28 strict cases, 159/168 repetitions**. Legacy 59/60, meal changes 80/84, compact 20/24. By family: memory 45/48, profiles 10/12, composition 12/12, labels 36/36, temporal 12/12, updates 12/12, compact 20/24, photo 12/12. Actual input 835,404, cached input 751,463, output 26,201, reasoning 0; p50 4136 ms, p95 5995 ms; estimated US$0.0290. The nine remaining failures are retained: a one-meal exception modified/reinforced a conflicting permanent habit twice, one recent copy omitted its weekday, two overnight responses misapplied the other-day notice/day, and four compact responses reopened an unavailable detail or explained the absence of an open question.

### Candidate development evidence

The first paired candidate (`logs/s19-candidate-pairs-6-2026-10-05T10-21-16-03-00.json`) failed five strict cases: habitual-copy source/brand matching, an overnight notice, a missing plan estimate while saving a reference, and an unavailable question repeated in compact. This candidate is not accepted. Refinements clarify the existing rule relationships: select recent records by date before brand-independent comparison, identify the copied weekday, condition the other-day notice on the resolved day, keep plan estimates independent of temporary proposals, and apply the compact eligibility gate to every interrogative sentence. No food, product, profile id, timestamp or user-story example from a failing case was added to instructions. Reserved outputs remain unopened during this development.

The second paired run (`logs/s19-candidate-pairs-6-2026-10-05T10-27-56-03-00.json`) passed 163/168 repetitions but failed five strict cases. Inspection found a wrong sum of item calories, aggregate weight repeated for each unit, the wrong copied weekday, an unavailable compact question reopened, and rejection of a usable synthetic label as an unfamiliar product. The incumbent development run on the same prefix was stopped after the failures already established rejection; its partial responses remain in the ignored `s19-stream-candidate-existing-*.jsonl` artifact, not presented as a complete regression result. It exposed routine precedence/unequal-amount copying, open compact questions dropped, other-day copy omitted and legacy meal consolidation issues. Two independent API connection failures also occurred. There was a brief overlap with the next development run while identifying the Windows venv launcher/child process pair; both obsolete processes were then terminated. Neither run is used as final performance evidence.

Targeted development (`logs/s19-candidate-s19-01-first-use-0,s19-04-6-2026-10-05T10-36-00-03-00.json`) preserved all assertions, including the known pre-v3 generic question. It confirmed the numerical/label/closed-question fixes but retained failures in habitual weekday, open questions and other-day copy, plus two provider connection failures. Subsequent general-rule refinements make source selection mutually exclusive, keep known portions and item sums consistent, preserve readable unfamiliar-brand label evidence when values are usable, apply other-day copy independently of estimate/intent, and explicitly distinguish missing information from declared unavailability. Memory declarations are classified independently of meal intent, so an explicit enduring preference during a clarification remains permanent. All changes retain existing specification semantics; no new illustrative narrative was introduced.

The third paired run (`logs/s19-candidate-pairs-6-2026-10-05T10-41-21-03-00.json`) was also rejected: recent fallback and compact closure remained unreliable, alongside one overnight failure and two provider connection failures. The blanket instruction against inventing a remembered habit was clarified to preserve the explicitly permitted RECENT fallback; source review found that these requirements must be stated together. The following abstract synthetic illustrations were then authored from the specification's decision relations, without food names, brands, nutrients, dates, people or a meal transcript. They are symbolic truth-table rows, not anonymized retellings of the removed narratives or incident conversations. Their effect still requires evaluation; provenance does not establish accuracy.

| Registry id | Independent origin and owner | Purpose / assembly |
| --- | --- | --- |
| `attribute-status-table-v1` | Independently enumerated answered/unavailable/omitted attribute states; Chat 7 and registry rule `digest`. | Illustrate closure per attribute and the required pending suffix after repeated failures of prose-only rules; compact only. |
| `habitual-source-table-v1` | Independently enumerated routine-present, equal-food/amount, unequal-amount and single-day states; Chat 3d and registry rule `history`. | Distinguish source precedence, brand-insensitive equality and missing evidence; both Chat capability branches. |

The symbolic compact illustration did not resolve the failures and was removed. The retained inventory contains only `habitual-source-table-v1`: both new recent-copy variants passed all six targeted repetitions with it, while the unequal-amount incumbent still failed once and remains visible in the evidence. The compact rule returned to the first example-free candidate's wording, adding the general consistency requirement that an unavailable attribute cannot simultaneously be requested. No illustration is retained solely because its declared provenance passes.

### Frozen comparison candidate

The final comparison uses unchanged candidate prefixes from this point, without consulting reserved outputs: legacy 22,293 characters (`59aa9e7703b22064ab69cc7ec031b45def22ed681ccfff5c96ee3f60c9d37792`); meal changes 26,046 (`33a7927cbfb5c4b31459e65853c9b848015d7f398ee6dc567d002e0653bb04ba`); compact 2,874 (`9a32706372521a4bca8c43311df4796e4556d90175d396ce0199b49d7d668ed6`). This is a validation freeze, not an acceptance declaration. Its complete regression, transfer and reserved results determine whether delivery is permitted.

### Complete frozen comparison

Candidate source commit: `533f76a99ed6cd18df9f58a275977010a52f40a8`. The eight reserved input hashes match the pre-tuning seal. All results below use actual `gpt-6-luna` with effort `none`, two evaluator workers and unchanged prefix hashes. Development failures above remain part of the evidence; they are not counted as accepted runs.

| Set / version | Cases passing | Repetitions passing | Error-path repetitions | p95 ms | Input / cached / output tokens | Reasoning | Estimated USD |
| --- | ---: | ---: | ---: | ---: | --- | ---: | ---: |
| existing / baseline | 151/155 | 456/465 | 2 | 5628 | 2,414,247 / 2,290,161 / 75,462 | 0 | 0.0730 |
| existing / candidate | 146/155 | 439/465 | 0 | 4246 | 2,513,061 / 2,399,903 / 76,055 | 0 | 0.0733 |
| pairs / baseline | 22/28 | 159/168 | 0 | 5995 | 835,404 / 751,463 / 26,201 | 0 | 0.0290 |
| pairs / candidate | 26/28 | 165/168 | 0 | 3885 | 868,944 / 794,778 / 26,617 | 0 | 0.0287 |
| reserved / baseline | 8/8 | 48/48 | 0 | 3608 | 268,140 / 254,797 / 7,477 | 0 | 0.0076 |
| reserved / candidate | 8/8 | 48/48 | 0 | 3442 | 279,204 / 265,400 / 7,599 | 0 | 0.0078 |

Case verdicts retain each fixture's strict/majority rule. Every new case is strict. Error-path counts include provider failures and validation rejection: the baseline has one moderation connection failure and one invalid meal-change metadata rejection. Both remain failed repetitions. Costs use the evaluator's configured pricing. Latency follows its existing generation/moderation measurement; rate-limit retries reset the attempt timer, so p95 excludes their prior backoff and is not complete wall-clock latency. Rate limits during the frozen candidate runs recovered through the existing retry policy; no failed final repetition was omitted.

| Set / capability | Baseline cases; repetitions | Candidate cases; repetitions |
| --- | --- | --- |
| existing / legacy | 121/124; 364/372 | 117/124; 348/372 |
| existing / meal_changes | 25/26; 77/78 | 25/26; 77/78 |
| existing / compact | 5/5; 15/15 | 4/5; 14/15 |
| pairs / legacy | 9/10; 59/60 | 8/10; 57/60 |
| pairs / meal_changes | 11/14; 80/84 | 14/14; 84/84 |
| pairs / compact | 2/4; 20/24 | 4/4; 24/24 |
| reserved / legacy | 4/4; 24/24 | 4/4; 24/24 |
| reserved / meal_changes | 4/4; 24/24 | 4/4; 24/24 |

| Transfer family | Baseline cases; repetitions | Candidate cases; repetitions |
| --- | --- | --- |
| compact | 2/4; 20/24 | 4/4; 24/24 |
| composition | 2/2; 12/12 | 2/2; 12/12 |
| labels | 6/6; 36/36 | 6/6; 36/36 |
| memory | 6/8; 45/48 | 6/8; 45/48 |
| photo | 2/2; 12/12 | 2/2; 12/12 |
| profiles | 0/2; 10/12 | 2/2; 12/12 |
| reserved | 8/8; 48/48 | 8/8; 48/48 |
| temporal | 2/2; 12/12 | 2/2; 12/12 |
| updates | 2/2; 12/12 | 2/2; 12/12 |

Every frozen-candidate case with at least one failed repetition:

| Set / case | Baseline passing reps | Candidate passing reps | Failed checks |
| --- | ---: | ---: | --- |
| existing / `cafe-resposta-leite` | 0/3 | 0/3 | `question`, `memory_updates_has` |
| existing / `correcao-contra-digest` | 3/3 | 2/3 | `record` |
| existing / `correcao-slot-sugerido-pergunta` | 3/3 | 2/3 | `intent`, `record`, `suggested_slot`, `estimate` |
| existing / `dia-comi-ontem` | 3/3 | 2/3 | `reply_has` |
| existing / `digest-pergunta-aberta` | 3/3 | 2/3 | `digest_has` |
| existing / `hard-acho-que-nao-janto` | 3/3 | 2/3 | `record`, `skip_slot` |
| existing / `hard-foto-pergunta` | 3/3 | 2/3 | `intent`, `estimate`, `record` |
| existing / `igual-almoco-segunda` | 3/3 | 2/3 | `estimate`, `suggested_slot`, `kcal_range`, `meal_text_has`, `meal_text_not` |
| existing / `janta-o-que-como` | 3/3 | 2/3 | `intent`, `estimate`, `suggested_slot`, `kcal_range` |
| existing / `memoria-cheia` | 0/3 | 0/3 | `reply_has` |
| existing / `mesmo-cafe-ontem-recent` | 2/3 | 2/3 | `intent`, `estimate`, `suggested_slot`, `kcal_range`, `meal_text_has` |
| existing / `registro-acrescimo` | 3/3 | 2/3 | `suggested_slot`, `meal_text_has` |
| existing / `registro-noturno-ontem` | 3/3 | 1/3 | `reply_has` |
| existing / `registro-pulei-sem-slot` | 3/3 | 0/3 | `record`, `skip_slot` |
| existing / `s18-missing-volume` | 3/3 | 2/3 | `estimate`, `record`, `meal_change`, `top_question` |
| existing / `slot-cafe-20h` | 3/3 | 1/3 | `suggested_slot` |
| existing / `slot-cafe-repete-almoco` | 3/3 | 1/3 | `suggested_slot` |
| pairs / `s19-03-current-override-0` | 6/6 | 4/6 | `memory_used_only` |
| pairs / `s19-04-recent-brands-0` | 6/6 | 5/6 | `reply_has` |

Reports (ignored local evidence, with saved outputs, checks, usage, case hashes and prefix snapshots):

- baseline existing: `logs/s19-baseline-existing-3-2026-10-05T10-05-35-03-00.json`.
- baseline pairs: `logs/s19-baseline-pairs-6-2026-10-05T10-15-33-03-00-rescored.json`.
- baseline reserved: `logs/s19-baseline-reserved-6-2026-10-05T11-06-10-03-00.json`.
- candidate existing: `logs/s19-candidate-existing-3-2026-10-05T11-00-14-03-00.json`.
- candidate pairs: `logs/s19-candidate-pairs-6-2026-10-05T11-04-55-03-00.json`.
- candidate reserved: `logs/s19-candidate-reserved-6-2026-10-05T11-07-31-03-00.json`.

Prefix size increased versus baseline: legacy +2,617 characters, meal changes +2,632, compact +241. Removal of personal examples did not yield a smaller final prefix; explicit rule relationships added text. No size, cache or cost improvement is claimed as a substitute for behavior. This limited synthetic sample is not a population-wide accuracy estimate.

### Review of every incumbent with a new failed repetition

All incumbent expectations are unchanged. The frozen run has 439/465 passing repetitions, versus baseline 456/465. Not every two-of-three result fails a case: the original strict flag still determines its verdict. These are observed failures under the candidate, not estimates of their frequency in users. The sample cannot establish that every isolated failure was caused by the rewrite, but acceptance remains blocked by unresolved semantic failures.

| Incumbent | Inspected behavior and implication |
| --- | --- |
| `correcao-contra-digest` | Kept the digest's other-day classification despite the user's current correction. Estimate existed, but no record was offered. |
| `correcao-slot-sugerido-pergunta` | Asked for food already identifiable in history instead of reconstructing the requested meal and suggested target. |
| `dia-comi-ontem` | Correctly avoided recording an earlier-day meal, but omitted the required today-only explanation while asking about portion. |
| `digest-pergunta-aberta` | Preserved the food/photo fact but dropped the genuinely unanswered assistant question. This is a loss of continuation context. |
| `hard-acho-que-nao-janto` | Converted a hedged intention into `skip`; the unchanged skip path then returned an automatic skip. This is an actionable error, not a copy-only difference. |
| `hard-foto-pergunta` | Recognized food in the image but returned question intent and no estimate; the contract requires a photo estimate with unsure record intent. |
| `igual-almoco-segunda` | Failed to use an available specific-day RECENT meal and asked what was eaten. |
| `janta-o-que-como` | Returned a question about available foods instead of the contracted assumed plan and estimate. |
| `registro-acrescimo` | In the legacy path, the added food became a separate later-slot estimate and omitted the occupied meal's base. |
| `registro-noturno-ontem` | Correctly classified the meal as other-day and did not record; omitted the required explanation in two repetitions. |
| `registro-pulei-sem-slot` | Mapped an unmatched meal name to a profile snack and returned automatic skip in all three repetitions; the fixture requires null target/no action. |
| `s18-missing-volume` | Invented a serving for an omitted drink volume and returned no question. Arithmetic and base preservation remained valid, but the clarification gate released an actionable addition. Preserving deterministic arithmetic alone is insufficient. |
| `slot-cafe-20h`, `slot-cafe-repete-almoco` | The supplied profiles explicitly assign lunch to id `1` and coffee to id `3`. The current answer continues the user-named coffee, yet the candidate selected the occupied lunch with similar food in two repetitions each. Both fixtures were inspected; their expectations are valid. |

Baseline failures remain distinct: `cafe-resposta-leite` and `memoria-cheia` fail all three case repetitions before and after; the former additionally omitted a permanent preference proposal in one candidate repetition, beyond its known pre-v3 generic-question mismatch. `mesmo-cafe-ontem-recent` again failed once by not reconstructing the meal. Baseline's `s18-interleaved-correction` metadata contradiction and `unavailable-weight-size-known` moderation connection failure did not recur in this frozen full run. All 465 refusal expectations passed; no policy weakening is inferred from the instruction cleanup, and this is not a claim of universal safety.

The implementation changes only authored instructions/assembly and evaluation tooling. Schema, context serialization, shaping, accounting, clarification release and record gates remain unchanged. Thus these observed failures originate in model classification/content under the candidate prefix and are propagated by existing deterministic paths. They cannot be dismissed by pointing to passing registry/unit tests. Scope-bound prompt refinements above improved some cases while regressing others; this candidate does not demonstrate the plan's required behavior preservation.


The two failed transfer cases were inspected as well. `s19-03-current-override-0` preserved the explicitly supplied food/portion and made no memory update, but attributed the estimate to the contradicted habitual fact twice. `s19-04-recent-brands-0` copied the correct food, portions and nutrients but omitted the required copied weekday once. These are distinct from fabricated nutrition, but still fail the required assertions. All original reserved cases passed on both prefixes; they did not drive a prompt change.

### Additional rejected consistency experiment

After the complete comparison, a scoped instruction-only experiment made current/history source priority and final response consistency more explicit. Targeted six-repeat evidence: `logs/s19-candidate-correcao-contra-digest,co-6-2026-10-05T11-12-25-03-00.json`. It did not resolve unmatched skip names or continuation targets; the latter failed four/six and six/six repetitions in the two named-slot cases. The unchanged compact prefix also showed repeated open-question omissions. This experiment is rejected, its local diff is preserved in `logs/s19-rejected-b.patch`, and the source was restored exactly to the frozen comparison commit. All three assembled prefix hashes were rechecked after restoration. No results from different candidate prefixes are pooled into a success claim.

### Acceptance and remaining work

**Acceptance is not met.** Validation items 3 and 4 require every critical new repetition and no unresolved semantic regressions; item 4 states: “Unresolved semantic regressions block completion.” The candidate improves the paired sample but fails that gate and regresses the incumbent suite. Neither passing provenance checks nor successful reserved cases waive it. S19 remains `Em implementação`; it is not moved to completed or represented as deployed.

Local validation of the retained source: `python -m pytest server/tests -q` — **310 passed, 383 subtests passed**, 25.16 seconds. `node tools/check-docs.mjs` and `git diff --check` pass. The unit suite proves registry/assembly boundaries and deterministic contracts, not model behavior. No incumbent model-evaluation fixture or expectation was modified.

At the end of the frozen comparison, the code and measured evidence were retained in a draft PR without merging or deploying the candidate. The later owner-directed source integration is recorded below. No APK, live policy/specification rewrite, user-memory migration or changes in the other agent's checkout were performed. Completion documentation updates and HTTP dev smoke remain unperformed because behavioral acceptance was not met.

Source review and integration vehicle: [PR #109](https://github.com/nicolasteixeira3856/dieta-bot/pull/109), originating branch `codex/s19-generalizable-chat`. The PR owns its Git delivery state; merging it does not override this plan's failed model-evaluation gate. The retained model instructions match the frozen comparison hashes above.

Remaining work is concrete: preserve the current user's continuation target and eating day; avoid automatic skip for an unmatched/hedged name; retain useful open questions while closing unavailable attributes; require material missing portions before action; and attribute memory/copy sources correctly. Re-run the unchanged acceptance matrix after a successful correction. These observations do not establish that another instruction rewrite is impossible, but the attempted scope-bound revisions did not meet the required standard. A change of model/effort or a broader runtime decision mechanism is outside this approved refactor and requires an explicit scope/architecture decision under SDD.

### Owner-requested Codex Luna low diagnostic

The owner subsequently authorized exactly two Codex subagents using `gpt-6-luna`, effort `low`, to investigate the failures without additional app API calls. This bounded experiment is an explicit exception to the default one-agent rule; it does not change the pinned app model/effort or authorize ongoing delegation.

Both respondents started without the parent conversation, received opaque case ids, the exact frozen instruction branch, serialized input and per-request JSON schema, and accessed the supplied image. Expected answers, earlier responses and the other respondent's output were withheld. Each generated one initial response for all 19 scenarios that had at least one failing frozen-candidate repetition: 17 legacy Chat, one meal-change and one compact case.

Responses were replayed through the unchanged route orchestration, shaping, clarification/record gates and evaluator assertions, with a saved-response provider and a local clean moderation stub. Network connections were disabled during scoring. Replaying all 63 earlier API generations in this selected subset reproduced their shaped outputs, checks, statuses and errors exactly. All 38 new responses satisfied the offline schema validator, covering every keyword used by the supplied schemas; no route execution error occurred. Moderation and API structured-output enforcement were not evaluated by this diagnostic.

| Respondent / measure | Result |
| --- | ---: |
| Codex Luna low A | 17/19 responses passed existing assertions |
| Codex Luna low B | 15/19 responses passed existing assertions |
| Combined responses | 32/38 passed |
| Scenarios passing both respondents | 14/19 |
| Selected earlier API none repetitions, for reference only | 34/63 passed |

The last row is not a controlled effort comparison: cases were selected for earlier failures, repetition counts differ, and Codex has different instructions/tooling and a shared conversation for multiple cases. There is no same-environment none control, randomized assignment, API low latency/cost measurement or evidence that every passing assertion validates all nutrition/prose. Regression to the mean and contextual dependence remain possible. These results do not establish a causal benefit from low effort or an app-wide success rate.

| Remaining scenario | Observed low-effort failure | Interpretation |
| --- | --- | --- |
| `registro-pulei-sem-slot` | Both failed: A chose profile id 2, B id 4, for the unmatched meal name; both produced automatic skip. | More reasoning did not resolve target ambiguity; the unchanged record gate propagated each guessed target. |
| `registro-acrescimo` | B described adding 390 kcal to a 610 kcal dinner for 1,000 kcal, but its structured estimate contained only 390 kcal and the added dessert. A included the base. | The original prose/data inconsistency persists in legacy Chat. This case does not exercise S18's deterministic addition path. |
| `hard-foto-pergunta` | B described calories in prose but returned question intent and no structured estimate; A passed. | Contract/intent interpretation failure, not missing image access. |
| `cafe-resposta-leite` | B returned medium confidence and a null question, after which legacy shaping inserted a generic question. A passed with high confidence. | A's pass is not a fix for deterministic shaping; raising confidence merely to avoid a question is invalid. |
| `registro-noturno-ontem` | A correctly classified yesterday and produced no record, but omitted the required today-only explanation. | Explanatory-copy failure, distinct from a wrong write. |

Both respondents passed the existing assertions for the other 14 scenarios, including named-meal continuation, current-day correction against digest, open-question retention, hedged skip, memory capacity, required drink-volume clarification and the two transfer cases. This is exploratory evidence, not completion of the corresponding fixes.

**Conclusion:** low effort did not eliminate relevant failures and is not a demonstrated standalone correction. Some cases may benefit, but that hypothesis requires a controlled API comparison after the workflow is corrected. Deterministic legacy shaping requires its own code correction; arithmetic correctness and nutritional accuracy remain separate responsibilities.

### Owner-directed master integration and continuation

On 2026-10-05, after reviewing the failed API acceptance and Codex diagnostic, the owner explicitly requested merging all work from this worktree into master, preserving the other agent's ongoing app work, resolving integration conflicts locally, and moving this work's plan numbers to the end only if a collision exists. The owner allowed remaining work to be recorded here instead of creating another plan. This direction authorizes source integration of the known-incomplete candidate; it does not assert behavioral acceptance, complete this plan, authorize deployment, or approve the architecture proposals below for implementation.

Integration preparation merged origin/master `9ced9ca` into the isolated branch. The incoming app/brand/site documentation was retained without edits, and no conflict or plan-number collision occurred. S19 remains the continuation entry point; S20 and the other incoming plans retain their identities. No command switches branches, merges, resets, cleans, builds or writes files in the other agent's checkout. The Python environment is reused read-only to run server tests in this isolated checkout.

#### Proposed continuation, not yet an implementation plan

1. **Finish the existing end-to-end capability before attributing every legacy failure to Luna.** [D9](../../../design/plans/completed/d9-chat-meal-updates.md) and [A47](../../../android/plans/pending_manual_validation/a47-chat-meal-updates.md) own the designed/persisted meal-update flow under [ADR-032](../../../produto/adrs/ADR-032-acrescimos-e-correcoes-de-refeicoes.md). Verify the actual APK sends `meal_changes` and preserves `pending_addition`; test source/destination changes, base preservation, duplicate application, restart and undo. The current isolated Android source does not yet send those fields. Server arithmetic/copy alone does not complete client adoption. Do not mix Android implementation into S19.
2. **Plan explicit continuation state.** Explore retaining the active eating event, user-named target, pending operation, supplied/unavailable portion details and unresolved questions as structured context with origins. Separate an interleaved correction target from the meal being eaten. Android remains the owner of persisted state and the server stays stateless unless a separately accepted ADR changes that boundary. A digest must not be the sole store for action-critical context. This is a proposal requiring an ADR and distinct server/Android executable plans, not an implicit extension of S19.
3. **Plan action validation against that state.** The model proposes foods, operation, target and the source of its interpretation; code checks compatibility before authorizing a mutation. An ambiguous target or add/revise decision requires a useful clarification or the existing manual choice, rather than guessing. A forced nutritional estimate must not authorize an invented operation. Preserve direct progress for clear requests; do not obtain a higher safety score by blocking every interaction.
4. **Keep displayed and written operation values together.** S18 already composes addition/revision copy from validated state; finish its client use and evaluate other action-related prose that can contradict structured output. Correct the legacy generic-question insertion separately, preserving honest confidence and accepted clarification behavior. Supplied label/serving arithmetic can be deterministic; that does not certify model-estimated nutrition.
5. **Validate complete conversations and final storage.** Extend independent synthetic sequences with additions, revisions, interleaved meals, ambiguous names, date corrections, compacted history, changing destinations, restart and undo. Assert final foods, kcal/P/C/G, absence of unintended writes/duplicates and text/data consistency. Measure useful task completion and unnecessary questions alongside critical errors. Real incidents remain diagnostic/regression evidence and never become global examples, per [ADR-033](../../../content-policy/adrs/ADR-033-global-chat-example-provenance.md).
6. **Bound paid experimentation.** Start with deterministic unit/transaction tests and offline replay of saved generations. Then use a small, frozen API comparison for the remaining semantic decisions, followed by the unchanged full acceptance matrix for a release candidate. Codex subagents remain exploratory. A model/effort change requires its own scope decision and the governing API quality/latency criteria; none is made by this source merge.

Allocate any future ADR/plan ids from then-current master, appending after existing ids. The neighboring [S20](../s20-tali-prompt-identity.md) edits instruction identity; source integration of S19 is not the same as satisfying its behavioral Completion prerequisite. Its next implementation must discover the registry in `server/chat_instructions.py` rather than restore the removed inline instruction strings. Do not rewrite another agent's plan or accepted decision as part of this handoff.

#### Evidence preservation

Aggregate results, failure diagnosis and next work are versioned in this plan so continuation does not depend on the old worktree. Raw synthetic evaluation reports remain outside git under the existing evaluator policy. A verified archive also preserves them outside the worktree on the owner's machine:

- File: `C:/Users/Nicolas/.codex/diagnostics/dieta-bot/s19-handoff-20261005-114157.zip`.
- SHA-256: `bcfd1d292b6b488c2c08bd331359768e4acbd44d05aa4eb4207f7c39e8b1251b`.
- Contents: 38 evidence files plus a manifest, including the frozen baseline/candidate reports and instructions, rejected-experiment evidence, both blinded Codex outputs/inputs, scoring results and offline replay helpers. Every archived file hash and the ZIP integrity were verified. No credentials, environment files or original tester conversation logs are included.
- Frozen instruction source: commit `533f76a99ed6cd18df9f58a275977010a52f40a8`. The archive manifest distinguishes offline replay helpers from the paid API runner; inspect saved machine paths before rerunning any script.

No additional paid model evaluation is required merely to merge this unchanged, explicitly incomplete candidate. Integration checks below cover source/tests/document consistency; they do not replace the failed behavioral matrix or authorize a dev deployment.

#### Integration validation

After merging the incoming master documentation, `python -m pytest server/tests -q` passed **310 tests and 383 subtests** in 25.69 seconds. `node tools/check-docs.mjs` passed (49 live files, 52 files link-checked); `git diff --check` passed. The three assembled instruction hashes still match the frozen comparison, and shared estimate/fit/scope strings remain byte-identical to its baseline. The repository reports no GitHub checks for this PR; this is not a passing remote CI result. No Android build, device operation, live API evaluation, server deployment or app distribution is part of this integration.

## Continuation scope — pt-BR cue lexicon

- State: approved by the owner by name on 2026-10-05 and implemented; see [Continuation results](#continuation-results-2026-10-05) and [Closure](#closure-by-owner-decision-2026-10-05).
- Approval: `Aprovo a continuação "pt-BR cue lexicon" do plano docs/server/plans/s19-generalizable-chat-instructions.md. Implemente a continuação aprovada.`
- Executable boundary: unchanged, `server/` only.

### Owner decisions (2026-10-05)

1. pt-BR words and constructions may stay in the fixed Chat instructions for now, as language cues, provided they are authored from the rule and carry no food, quantity, brand, nutrient, person or date.
2. This is a known internationalization blocker, accepted for the current pt-BR-only product. It must be raised whenever releasing the app to another market or language is discussed, and it is to be fixed before such a release. This plan does not fix it.

### Diagnosis of the frozen candidate

Read-only review of the frozen candidate against the pre-S19 instructions (`1be6e4c^:server/llm.py`). No test or model call was run for this diagnosis; the mapping below is a source reading, not a measured ablation.

The first pass classified every concrete phrase of the old prompt as an example of unknown provenance and removed all of them together. Three different kinds of material were mixed:

| Class | What it was | Treatment so far | Correct treatment |
| --- | --- | --- | --- |
| Incident content | A reported meal with its quantities; a branded product label with its values. | Removed. | Stays removed (ADR-033 § 1). |
| pt-BR language cues | Function words and verb constructions that map Portuguese phrasing to an intent: pending, hedge, addition/correction, plan, skip, record-request and eating-day markers; common meal words; names that match no slot. | Removed as "unknown provenance". | Not a retelling of anyone's meal. Re-author from the rule and declare (this section). |
| Food-bearing illustrations | Short meal stories and a compact-digest sequence using a narrow set of foods. | Removed; one symbolic table added. | Add back only where a cue is measured insufficient, freshly authored and varied. |

The model receives English rules and pt-BR input at reasoning effort `none`. The incumbent regressions in [Complete frozen comparison](#complete-frozen-comparison) line up with removed cues:

| Regressed incumbent (baseline → candidate reps) | Removed cue class |
| --- | --- |
| `registro-pulei-sem-slot` (3/3 → 0/3) | Meal names that match no PROFILE slot. |
| `hard-acho-que-nao-janto` (3/3 → 2/3) | Hedge markers on a skip. |
| `slot-cafe-20h`, `slot-cafe-repete-almoco` (3/3 → 1/3) | Common meal words for a named slot; the continuation relation of an answer to the meal named before. |
| `janta-o-que-como` (3/3 → 2/3) | Plan markers. |
| `hard-foto-pergunta` (3/3 → 2/3) | Nutrition-question markers on a photo. |
| `igual-almoco-segunda` (3/3 → 2/3) | Particular-prior-day copy phrasing. |
| `dia-comi-ontem`, `registro-noturno-ontem` (3/3 → 2/3, 1/3) | Eating-day markers; the notice also applying alongside assumptions. |
| `digest-pergunta-aberta` (3/3 → 2/3) | The open-question format illustration in compact. |

Several of these are single failed repetitions out of three, which is within the baseline's own noise. The later abstract rule text grew each Chat prefix by about 2,600 characters without recovering them.

### Scope

1. **Base.** Continue from the frozen candidate (`533f76a`): keep the registry, the incident removal, the `context` rule and the transfer gains. Do not restore the pre-S19 strings.
2. **Cue lexicon.** Add a third declared inventory beside `RULES` and `EXAMPLES` in `server/chat_instructions.py`. Each cue has a stable id, the owning rule, its purpose, provenance `independent_synthetic` and locale `pt-BR`. A cue is a marker or minimal construction (at most four words), never a full user sentence, and contains no food, quantity, brand, nutrient, person or date. Meal words and eating verbs derived from slot names are allowed. Cues are authored from the rule text and ordinary pt-BR grammar, not copied from the old prompt, logs or fixtures; when the natural marker is a single common word, coinciding with the old prompt is not a violation (ADR-033 § 2: origin and function, not a vocabulary blacklist).
3. **Assembly.** Each rule that needs cues references them by id, so the assembled prefix stays identical across users within a capability branch. `validate_inventory` covers cues the same way as examples: undeclared, unowned, wrong-provenance, missing-locale or unassembled cues fail.
4. **Prompt/test separation.** A unit test fails when a cue equals a whole user message of any evaluation fixture (normalized for case, accents and punctuation). Where an incumbent fixture's message is only a marker plus a meal word, the cue list for that rule must not reproduce that exact combination.
5. **Food-bearing synthetic examples, one at a time.** Only after step 2 is measured. Add one only for a failure that cues did not fix, authored from the rule with foods, slots and amounts that appear in no fixture and not in the pre-S19 prompt; register it in `EXAMPLES` with origin and purpose. Remove it again if it is measured neutral. Expected candidates: the compact open-question format and the answer-continues-named-meal relation.
6. **Trim, optional.** Abstract rule text added during the first pass may be removed only when a targeted run shows no loss. Not required for acceptance.
7. **Traceability.** Extend the rule map in Results with the cue inventory (id, rule, purpose) and the source review of every cue and added example.

### Acceptance (replaces Validation items 3 and 4 for the continuation; items 1, 2, 5 and 6 stand)

The original "every repetition passes" gate is not reachable while the baseline itself fails 9 of 465 repetitions. Replacement:

1. Run the incumbent suite at three repetitions on the new candidate. Every case with fewer passing repetitions than the recorded baseline is rerun at ten repetitions on both the baseline and the candidate prefixes. The candidate passes when no such case has fewer passing repetitions than the baseline at ten.
2. No candidate repetition, in any case where the baseline shows none, produces an actionable error: an automatic skip from a hedged or unmatched meal, a record on the wrong slot or day, or an addition released without its required clarification.
3. Transfer pairs: no family and no capability below the baseline; reserved set 48/48. A reserved case that drives an edit becomes development evidence and is replaced before the claim, as in the original scope.
4. `cafe-resposta-leite` and `memoria-cheia` fail on the baseline in all repetitions and are excluded from the comparison. They are reported, not fixed here.
5. Same model, effort, worker count and cost/latency methodology as the recorded runs. Fixtures and expectations are not deleted or weakened.

Paid evaluation starts only after approval of this section.

### Deployment hold

Master carries the frozen candidate, which fails acceptance. Do not deploy `server/` from master to the dev VM until this continuation passes. S20 and S21 already list S19 as a prerequisite. If a server deploy becomes necessary first, restoring the pre-S19 rule text inside the registry is a separate owner decision.

### Out of scope for the continuation

- Internationalization of the instructions, cues or contracted pt-BR copy. Recorded as a blocker in the owner decisions above.
- The structured continuation state and action validation proposed in [the handoff](#proposed-continuation-not-yet-an-implementation-plan) (items 2 and 3). They need their own ADR and plans.
- Legacy shaping fixes for the two excluded baseline failures.
- Android, schema, context serialization, model or effort changes.

### Intended documentation changes at Completion

In addition to [§ 5](#5-intended-documentation-changes): server Chat states that the fixed instructions contain a declared pt-BR cue lexicon and contracted pt-BR copy, that both are language-specific, and that a release in another language or market requires replacing them first.

### Continuation results (2026-10-05)

Owner approved the continuation by name on 2026-10-05. Work ran in the isolated `s19-cue-lexicon-continuation` worktree on `feat/s19-cue-lexicon`, from master `cd3148a`. **Acceptance was not fully evaluated**: the provider began rate-limiting before the reserved set and the ten-repetition tiebreaks could run on the final prefix. The owner then closed the plan in this state; see [Closure](#closure-by-owner-decision-2026-10-05).

#### What changed in `server/`

- `chat_instructions.py`: a `CUES` inventory (24 cues, 115 markers, locale `pt-BR`, provenance `independent_synthetic`), rendered as one `pt-BR cues for the rule above` block after the rule it serves. `validate_inventory` rejects a cue with unknown provenance, a missing or other locale, an unowned rule, an empty purpose or meaning, a marker over four words or containing a digit, duplicate markers, or branch coverage that does not match the assembly. Compact has no cue block.
- `tests/test_instruction_provenance.py`: negative tests for each rejection above, and a test that no cue equals a whole user message of any evaluation fixture (normalized for case, accents and punctuation). That test rejected one authored marker during development, which was replaced.

Cue inventory by owning rule (purpose is recorded per cue in the registry):

| Rule | Cue ids |
| --- | --- |
| `intent` | `eating-report`, `plan-request`, `skip-firm`, `skip-pending`, `skip-hedge`, `occasion-words` |
| `record` | `record-request`, `accept-pending`, `nutrition-question`, `eaten-other-day`, `not-eating-day`, `eating-now`, `day-statement`, `record-complaint` |
| `estimate` | `meal-words` |
| `log` | `addition`, `correction`, `unavailable`, `approximate` |
| `history` | `habitual`, `particular-day` |
| `memory_changes` | `preference`, `forget`, `one-off` |

Source review of the cues: all were authored in this session from the rule text and ordinary pt-BR grammar. None carries a food, quantity, brand, nutrient, person or date. Single common words (meal names, eating verbs, `também`, `agora`) coincide with ordinary vocabulary, which ADR-033 § 2 allows. `occasion-words` deliberately uses occasion names that appear in no fixture. One marker of `not-eating-day` was first written close to the wording of the removed incident sentence; it was replaced by a generic verb before the final runs.

Examples added under scope step 5, each after cues alone were measured insufficient:

| Registry id | Origin and owner | Measured reason |
| --- | --- | --- |
| `answer-continues-meal-v1` | Symbolic, no food or meal name; rule `estimate`. Authored from slot priority 2. | Answer to a clarifying question went to another recorded slot with similar food: 2/6 with cues only, 9–10/10 after on most runs. |
| `open-question-table-v1` | Symbolic, no food; rule `digest`. Authored from Chat 7. | Open assistant question dropped from the digest: 2/6 before, 5–6/6 after, compact pairs 24/24 on the final run. |
| `habitual-comparison-v1` | Invented foods, amounts and two invented brand words that appear in no fixture and not in the pre-S19 prompt; rule `history`. Authored from Chat 3d branch B. | Brand-only difference refused, or a differing amount copied, in up to half of the repetitions with prose only. |

Rule text adjusted without changing documented semantics: `context` explains cue blocks; `record` states that the other-day notice depends only on the final `meal_day`, including the before-05:00 dinner rule; `log` puts that notice before the assumption line; `estimate` conditions slot priority 4 on an explicit correction or addition marker (already required by the LOG rule); `history` states the particular-day request as its own paragraph (one row is enough, no two-day comparison); `memory_use` repeats next to the citation that a logged brand, type or portion also needs `reinforce`.

#### Evidence on the final prefix

Final prefix: legacy 28,468 characters (`577cfea1…1fedd`), meal changes 32,229 (`bf2a7089…9e77`), compact 3,382 (`9c912f0b…2d9e`). Same runner, model `gpt-6-luna`, effort `none`, two workers, as the recorded baseline. Reasoning tokens 0.

| Set | Baseline (recorded) | Final candidate | p95 ms | Input / cached / output tokens | Estimated USD |
| --- | --- | --- | ---: | --- | ---: |
| existing, 155 × 3 | 151/155 cases, 456/465 reps | 152/155 cases, 458/465 reps | 4251 | 3,182,130 / 3,061,147 / 77,779 | 0.0816 |
| pairs, 28 × 6 | 22/28 cases, 159/168 reps | 26/28 cases, 166/168 reps | 3883 | 1,086,612 / 1,003,648 / 26,401 | 0.0315 |

By capability, existing: legacy 364/372 → 368/372; meal changes 77/78 → 76/78; compact 15/15 → 14/15. Pairs: legacy 59/60 → 60/60; meal changes 80/84 → 82/84; compact 20/24 → 24/24. Pairs by family: memory 45/48 → 48/48, profiles 10/12 → 11/12, compact 20/24 → 24/24, temporal 12/12 → 11/12, others unchanged at full marks.

Cases with fewer passing repetitions than baseline on the final prefix, each by one repetition: `digest-unavailable-now-known`, `pergunta-antes-jantar`, `s18-dinner-digest-pending-repeat`, `s18-missing-volume` (the model asked in `reply` with an incomplete `meal_change`, which the existing guard rejected with no action), `total-sem-comida`, and pair `s19-10-temporal-1`. Cases better than baseline: `memoria-cheia` 0/3 → 3/3, `cafe-resposta-leite` 0/3 → 1/3, `mesmo-cafe-ontem-recent`, `s18-interleaved-correction` and `unavailable-weight-size-known` 2/3 → 3/3. Of the 17 incumbents with a failed repetition in [Complete frozen comparison](#complete-frozen-comparison), 15 passed 3/3 on the final run; the exceptions are `cafe-resposta-leite` (1/3, a baseline failure) and `s18-missing-volume` (2/3).

#### Not evaluated, and why

- **Reserved set on the final prefix.** Three attempts were cut by `ModerationUnavailable: RateLimitError`, the last one stopped at the second case; none is a valid result. A prefix two small edits earlier (`56c68509…`) scored 48/48, which is development evidence only.
- **Ten-repetition tiebreaks on the final prefix** for the six cases above, and their baseline counterparts. Not run.

Ten-repetition evidence from near-final prefixes, for orientation only: the baseline scored 10/10 on `slot-cafe-20h`, `slot-cafe-repete-almoco`, `dia-pedi-estimar-ontem`, `hard-nao-jantei-ainda`, `hard-vou-pular-almoco`, `registro-pulei-sem-slot`, `hard-acho-que-nao-janto`, `registro-acrescimo`, `s18-missing-volume` and `memoria-reforco-iogurte`, 9/10 on `digest-unavailable-partial-answer` and 3/10 on `mesmo-cafe-ontem-recent`. Candidates scored between 6/10 and 10/10 on the two `slot-cafe` cases across four runs, 9–10/10 on `dia-pedi-estimar-ontem` and `hard-nao-jantei-ainda` after their last edit, 10/10 on `memoria-reforco-iogurte` after its edit and 3–7/10 on `mesmo-cafe-ontem-recent`. The pre-S19 prompt quoted the user sentence of the `slot-cafe`, `dia-pedi-estimar-ontem` and `hard-nao-jantei-ainda` fixtures; the candidate may not, by scope step 4. The two `slot-cafe` cases are the most likely to miss acceptance item 1 as written.

#### Remaining work

1. With provider quota available: reserved set (48 calls) and the tiebreaks (about 230 calls, estimated US$0.06–0.08) on the unchanged final prefix, one worker.
2. If acceptance item 1 fails only on fixtures whose sentence the pre-S19 prompt quoted, that is an owner decision on the gate, not a reason to quote the fixture.
3. On acceptance: Completion documentation of [§ 5](#5-intended-documentation-changes) and of this section, dev deploy and HTTP smoke, then the plan lifecycle.

Validation run for this state: `python -m pytest server/tests -q`, 327 passed and 383 subtests passed; assembled prefix hashes rechecked after the last formatting edit; `git diff --check` passes. `node tools/check-docs.mjs` reports no link or status finding; its gold-map check cannot load in this worktree because `pngjs` is not installed there. Evaluator-estimated spend for all continuation runs, including discarded ones: about US$0.55. Raw reports stay outside git in the worktree's `logs/`.

### Closure by owner decision (2026-10-05)

After the results above, the owner decided to stop here, merge the work and close the plan, and to open a new plan if the behavior proves unsatisfactory. This closes S19 with continuation acceptance items 1 to 3 only partly evidenced: the three-repetition regression suite and the transfer pairs are measured on the final prefix; the reserved set and the ten-repetition tiebreaks are not. The closure is an owner decision, not a claim that the gate was met.

- The deployment hold ends with this closure. No server deploy and no HTTP smoke test were performed in this delivery; the next dev deploy of `server/` from master ships these instructions.
- The live specifications were updated in the same delivery: server Chat rule 3 and its Provenance, the content-handling specification and the server README.
- Open observations for a future plan, none of them tracked as active work: the answer-continues-named-meal target (`slot-cafe-*`), the six one-repetition differences listed above, the two fixtures that fail on every prefix (`cafe-resposta-leite` shaping, `memoria-cheia` until this prefix), and the pt-BR dependence of the instructions before any other-language release.
