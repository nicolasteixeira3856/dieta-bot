# Specification — POST /v1/chat

## Context and goal

Chat sends a profile, today's snapshot, up to 12 messages and an optional photo. The server returns prose, a structured estimate and a suggested slot. It is stateless and never records the day.

## Scope

- `POST /v1/chat`, authenticated JSON; normal turns and `compact=true` digests.
- Request, photo, timeout and error boundaries: [HTTP contract](../../api-contract.md).
- The client does not call estimate/fit from Chat ([product Chat](../../produto/specifications/chat.md), rule 13).
- Optional installation correlation: [identity/audit](../../content-policy/specifications/identity-and-audit.md#closed-test-profile).
- Content handling: [content policy](../../content-policy/specifications/content-policy.md) and rules 12–16 below.

## Out of scope

Photo, user, day and memory persistence; streaming; multipart; other models; server-side ceiling calculation; the [production audit gate](../../content-policy/production-gate.md).

## Functional rules

1. Authentication matches estimate: invalid or missing `X-Invite` returns 401.
2. Model: `gpt-6-luna`, `reasoning.effort=none`, `store=false`. Considering `low` requires at least 10 percentage points of evaluator gain and p95 at most 20 seconds (ADR-023).
3. Each turn has fixed instructions followed by `PROFILE`, `MEMORY`, `DAY`, `RECENT`, `DIGESTS`, `HISTORY` (up to 12 messages), current text and optional image. Instructions are fixed within each meal-change capability branch for prompt caching. Effective meal-change requests additionally serialize PENDING_ADDITION before the current user message; it is untrusted proposal context, never a DAY record. `DAY` carries recorded slot text and kcal/P/C/G, plus `remaining_kcal` when supplied.
   - Structured memory (`facts` present, even empty): `MEMORY: permanent {n}/30, dynamic {n}/40`. Permanent/dynamic lines: `{id} {category}[ slot={slot}] {key}: {text} (seen {n} days[, last {date}])`. The legacy `memory` text is ignored.
   - With `temp_facts: true` and structured memory (v5), append `, temp {n}/5` to that header. A temporary line is `{id} {category} {key}: {text} (temp since {last_seen})`; a missing date is `unknown`. The client supplies creation date in `last_seen`. Without the capability the permanent/dynamic header is unchanged.
   - Legacy clients without `facts` retain `MEMORY: {memory}`. Their `temp_facts` flag is ignored.
   - `RECENT`, when supplied by any client: `{date} {weekday} {slot_id} {slot_name}: "{text}" {kcal}kcal {p}P {c}C {g}G`; a null slot is `Outros`.
   - An assistant history message may end with `[refeição sugerida: {slot name}]`. It is the assistant's suggestion, never the user's words. Client creation of this marker belongs to A38.
3a. Intent is `log`, `plan`, `question` or `skip`. `log`: ate/is eating, answers a question about that meal, or names food alone without a verb or question. `plan`: will eat, asks quantities, a recipe or whether food fits. `question`: a greeting, app question, food nutrition question or memory statement without food to estimate. `skip`: a meal did not happen today (`pulei o café`, `hoje não almocei`, without `ainda`), or a firm decision it will not happen (`hoje não vou jantar`). A pending meal (`ainda não almocei`) or hedged skip (`acho que não vou jantar`) is `question`, or `plan` when asking what to eat, never `skip`. For skip, `skip_slot` is the matching profile slot, otherwise null. Estimate is present for log/plan and null for question/skip. When uncertain, past eating is log; future/conditional or quantities are plan. Off-topic requests are handled under rule 12. Clients before v4 receive skip as question with the same reply.
3b. `meal_text` is the entire meal, including corrections, in pt-BR, within the Unicode limits of the [HTTP contract](../../api-contract.md#meal-change-capability), foods and quantities only. Never the latest answer alone or commentary. Descriptions within their bound remain complete; overflow fails safely without a record or memory mutation, for every client.
3c. A plan's reply gives grams per item, preparation in at most three lines for a recipe, and totals as `kcal · P · C · G`. It fits `remaining_kcal` when possible, otherwise states the excess. No daily-total arithmetic in the reply. Plans assume rather than ask; the reply states assumptions, and estimate.question is null.
3d. `O mesmo de ontem` or `igual ao almoço de segunda` copies the matching RECENT day/slot, foods and numbers. No match means estimate null and a question about the meal. `O de sempre` / `o mesmo de sempre` first uses a routine fact of that slot. Otherwise compare the latest record of that slot on each of the two most recent distinct days (last record within a day). Foods and quantities must match; a different brand of the same food is allowed, changed amounts or added/missing foods are not. A bare habitual-meal report is log. On a match, return the estimate, copy the most recent foods and numbers and name the copied weekday in reply without asking whether the user ate it. Otherwise, including fewer than two days, ask what was eaten with estimate null.
3e. Use memory that resolves uncertainty, without asking again, and cite its id in `memory_used`. Explicit habits propose permanent add/replace; explicit forgetting proposes remove; brand/type/portion in a log reinforces or adds a dynamic fact; matching routines reinforce; a new habit can add a dynamic routine. Permanent/dynamic keys match only that family. Never save one-off meals, day totals, health conditions or one-off labels as habits. At permanent capacity, ask which least-seen fact to forget before adding.
   - Only with temp capacity in MEMORY, a specific product's supplied label/nutrition values or a dish estimated for later proposes `add`, kind `temp`, category `portion`, slot null. Keep the product, serving basis and given numbers even with incomplete nutrients; never invent missing values in the fact. The proposal is independent of the reply/estimate, including plans for another day. Generic calorie questions, habits/preferences and the meal being logged do not create temporaries.
   - A matching T reference supplies its numbers on every use, including later portions, and its id goes into `memory_used`. Use never reinforces, promotes or removes it. An explicit request to forget can remove it. Changed reference data replaces an existing T id; a temp key never replaces a permanent/dynamic fact. Storage, expiry and capacity belong to the client under [ADR-029](../../produto/adrs/ADR-029-fatos-temporarios-compactacao.md).
3f. Reply never claims a meal was recorded or skipped: the app writes it and shows a receipt ([ADR-028](../../produto/adrs/ADR-028-registro-autonomo.md)). Applies to every client.
3g. Internal record marks apply to every model request and drive v4 responses. `record_intent: clear` means stated eating/skipping, explicit registration, a meal name followed by food, a plate photo without text or with stated eating, an answer to a meal question, a concrete portion report referring to previously discussed/estimated food, or `Pode estimar assim`. Food alone, hypotheticals and food photos with a nutrition question are `unsure`; the latter still have log/estimate. Plan/question are unsure. `today` uses DAY.date at the supplied local_time for the whole dialogue, including undated eating statements in history and their answers. Past tense alone is not yesterday. `meal_day: other` means food was **eaten** on another day, not merely asked about, estimated, bought, cooked or planned then. `O mesmo de ontem` is today's meal. Before 05:00, an undated dinner is today only if that profile slot is scheduled before 05:00; otherwise it is last night's dinner, other. Other undated meals and food eaten `agora` are today. Skip replies are one short neutral line without advice.
4. Read eating day, intent to record and slot separately. A record request/complaint for an identifiable meal is log with clear intent; reconstruct every food and answer from HISTORY/DIGESTS. It does not change the eating day: `registra o almoço de ontem` remains other. An explicit day correction overrides an assistant assumption or digest. `É de hoje` alone after a generic nutrition question does not create a meal log. Without identifiable food, ask what it was. If the current report identifies food and quantity but the earlier estimate is unavailable, make a fresh estimate from that report.
   - Slot priority: current user-named meal; earlier user-named meal being continued; earlier assistant suggestion for that meal; recorded meal being corrected; local profile time or nearest empty slot. Never ask which meal when already named or suggested. Profile names/times are authoritative, including unusual schedules.
   - Distinguish known/usable, omitted and explicitly unavailable details per food and attribute, using current text, history, digests, applicable memory and photo context. Approximate values, counts, sizes and household measures are usable. Omission alone does not mean inability: ask material omitted portion/size and preparation details together unless context resolves them. Draft assumptions do not count as supplied facts. Never repeat a resolved question. A user saying they cannot know, remember or determine a detail closes it even in the first message: do not request it again with another unit, synonym or approximate qualifier. This applies to weight, brand, preparation and ingredients, without erasing other known values or closing unrelated doubts.
   - When weight is unavailable, use counts, supplied size/household measures and visual evidence. A photo supports estimation, not measured weight. A still-material size question or familiar comparison is allowed unless supplied or ruled out. Ask remaining useful doubts together, at most three short questions. After an answer, ask again only about food still lacking any usable portion and materially changing totals; assume sauce, oil, cream, cheese, seasoning and usual coffee/tea amounts. Never cycle back to weight after a size answer.
   - When no material answerable doubt remains, including when alternatives are unavailable, estimate the whole meal with question null. Keep medium/low confidence for assumed portions and state the assumption in one short reply line; never inflate confidence to release the estimate. A later explicit measurement/correction overrides earlier unavailability. Meal-specific inability to answer does not become a memory fact. Preserve every food, count and fractional portion.
   - Resolve additions from eating relationships rather than food category or the next empty slot. Explicit current targets win; an interleaved correction of another meal does not redirect the meal being eaten. Ask when two targets remain plausible. Profile times remain authoritative. Only an unambiguous unnamed addition may use the latest eaten DAY slot.
   - With `meal_changes`, resolve operation and target together. DAY is the sole recorded base. A named occupied target cannot inherit foods from another slot or a digest's old proposed destination. Keep add intent through target answers and repeated quantities. A correction of PENDING_ADDITION replaces the unrecorded delta; a second unrecorded portion joins that delta, while a portion already in DAY is part of the base. A repeated recorded description alone does not imply another portion. Explicit removal/replacement revises the whole meal.
   - Without the capability, totals still describe the whole meal and item list, including prior foods. Reply separates the addition, previous amount and result instead of calling the addition a replacement. Legacy accounting remains model-estimated; safe delta rerouting requires a capable client.
   - Food eaten on another day may be estimated if requested, but reply states that Chat records only today's meals, including the before-05:00 rule.
4a. A calorie total without food, even for an eaten slot, produces estimate null and a question about food. Nutrients must reflect the food; energy may include sources beyond P/C/G, including alcohol. Never fabricate macros to enforce 4P + 4C + 9G = kcal. Unable to estimate means null, not zeros.
5. Model output uses strict JSON schema `chat_turn`, all properties required with explicit nulls: reply; intent; estimate `{kcal,p,c,g,confidence,question,items,suggested_slot,meal_text}` or null; internal record_intent/meal_day/skip_slot; memory_updates; memory_used; digest (null for chat); and internal scope. The opted-in schema also requires meal_change (object or null), with profile-bound base ids; the legacy schema has no such field. The response adds the configured model. `memory_updates[].kind` always has `permanent`, `dynamic`, `temp`, even for older clients. Slots and fact ids supply the existing per-request enums; `temp_facts` creates no schema or instruction variant for the same ids. Compact uses the separate digest schema.
5a. Shaping deduces invalid intent from estimate (log if present, otherwise question); drops an estimate on question; nulls plan questions; fills empty legacy meal_text from items and rejects overflow without truncation. Memory updates discard invalid op/kind/category, unknown ids for non-add operations, add with an id, routine without valid slot, and empty key/text; truncate key/text to their input limits and retain at most five. `memory_used` retains known unique ids, at most ten.
   - Temp updates require the effective v5 capability: add with null id, or replace/remove of a known T id; category portion/preference; slot null. Reinforce, routine and any non-null slot are dropped. Operations on T ids with another kind are dropped. Without capability, all temp proposals are dropped.
5c. With `clarify_rounds` (v3), response gains top-level question and estimate.question is always null. For log with estimate, `clarify_gate` checks in order: force (`released_force`); high confidence or no question (`released_confident`); rounds at least three (`released_cap`); repeated assistant question (`released_repeat`); otherwise hold (`asked`). Release keeps the model confidence and estimate, with both questions null. Hold returns estimate null, top-level question and history reply `Entendi: {meal_text}.` + newline + question (question alone without meal_text). Memory lists pass unchanged. Repeat detection uses sentences ending in `?`, lowercase unaccented alphanumeric tokens excluding a fixed short pt-BR stop list, Jaccard at least 0.6 or set inclusion. Plan/question/null estimate bypass the gate. Fallback/refusal has question null. Without rounds there is no gate or top-level question.
   - A v5 held response additionally carries `question_slot`: the sanitized held estimate's profile slot id or null. It is absent for non-held responses and clients without effective temp capability. Older client response shapes stay unchanged. Android consumption belongs to A38.
5d. With rounds and `auto_record: true` (v4), the record gate adds record (`auto`, `ask`, `none`) and skip_slot. First matching rule wins:
   1. Refusal → none (`none_policy`).
   2. Question/plan, held turn, log without estimate or fallback → none (`none_intent`).
   3. Other eating day → none (`none_other_day`).
   4. Skip with profile slot → auto and that skip_slot (`auto_skip`).
   5. Skip without slot → none, intent question (`none_skip_slot`).
   6. Released log without suggested slot → ask (`ask_no_slot`).
   7. Released log with clear intent → auto (`auto_log`).
   8. Otherwise → ask (`ask_unsure`).
   Force and photo without text count as clear. Missing/invalid meal_day defaults today; record_intent defaults unsure. Skip_slot is null except on rule 4. The client applies its guards before recording. Without v4 capability, record/skip_slot are absent and skip becomes question with the same reply.
5e. Effective `meal_changes` requests validate operations before clarification and record gates. Invalid/missing metadata on a log candidate fails with null estimate/change, no record and empty memory lists; no fallback to legacy semantics. `new` requires no occupied target; `revise` requires an eaten base matching suggested_slot and validates the complete replacement; `add` validates only the delta then derives the candidate from the matching DAY aggregate. Exact schema, arithmetic, rounding and bounds belong to the [contract](../../api-contract.md#meal-change-capability).
   - Unknown operation has no estimate and no memory effects. If the model explicitly marks meal_change null but supplies provisional numbers with a question, discard the draft and retain the question; this state cannot become an actionable estimate. A missing field or invalid metadata object still fails safely. Ask through top-level question while rounds remain; on force/cap return a non-actionable explanation. Portion force cannot choose an operation. A known add with only an unknown target may ask with its held draft, even with high nutritional confidence; force/cap/repeat can release that delta with null suggested_slot and record ask.
   - Only released actionable log estimates expose meal_change. Held, plan, skip, other-day, failure and refusal responses carry null. Numeric add/revise copy is composed after validation from the same values as the estimate; base text is preserved verbatim. Output moderation covers the composed reply, description and all addition item names.
5b. Legacy clients without facts receive empty memory lists. Plans return estimate null to avoid a recording card in APKs through 0.0.3; quantities/totals stay in reply.
6. Suggested_slot is a profile id or null, selected under rule 4. Shaping normalizes string/integer/id-object to a string and rejects unknown ids. No profile slots means the schema permits only null.
6a. Legacy confidence below high without a question uses `Alguma porção foi diferente do que considerei?`. V3+ never receives that generic question: an empty model question releases the estimate.
7. Compact sends only delimited history, without image, and returns pt-BR prose up to 400 tokens, capped at 1600 characters. Keep user-stated foods, quantities and nutrition values, user-named slots, skips and confirmed clarifications. Never record status, assumed eating day/slot, or an unconfirmed estimate stated as eaten. A slot suggestion marker is not a user fact. Read user facts first: preserve explicit unavailability with its food/attribute scope, without inferring it from omission. An answered or unavailable detail contributes that fact, with no pending marker; a later assistant repetition cannot reopen it. For a compound question, keep only its unresolved, answerable part. A later supplied measurement replaces stale unavailability. With no eligible open question, return facts only, without a marker or explanation that no question remains. An unanswered assistant question ends the digest using the literal prefix in `Pergunta em aberto: {question} ({meal})`, preserving the pending food/photo description and only the unanswered interrogative sentence, without preceding assistant claims. No advice, judgement, new numbers or estimates. Empty history returns 422 before generation; generation failure returns digest null. Output moderation and failures share the same `compact_reply` path in HTTP and evals.
8. Photos arrive as JPEG; conversion and EXIF stripping belong to the client. No HEIC processing here.
9. Reject an oversized photo before calling the model; never log base64.
10. Plain text without JSON returns the fixed scope refusal (fallback `text_only`), never raw model prose. Generation failure/timeout, empty or invalid output returns `nao deu pra estimar`, estimate null (fallback `error`), without stack traces.
11. HTTP body and photo limits are enforced as specified in the [contract](../../api-contract.md); the body cap covers the photo plus JSON.
12. Scope and prompt-input handling follow [content policy](../../content-policy/specifications/content-policy.md). The model's scope is internal; client text cannot open prompt sections (`###` is neutralized).
13. A non-in-scope result discards all generated content and uses the fixed [refusal copy](../../content-policy/specifications/refusal-copy.pt-BR.md), null estimate/digest and empty memory lists; question is null for v3 and record none for v4.
14. Input/output moderation and metadata-only content handling follow [content policy](../../content-policy/specifications/content-policy.md). Compact history is not re-moderated; its output is.
15. Moderation unavailable/error/timeout fails closed as HTTP 503 `content_policy_unavailable`.
16. One 60-second deadline covers moderation and generation, without SDK retries or blocked-content retries. Deadline before generation is fail-soft; before output moderation it is 503.

## IN and failures

Canonical examples and base fields: [HTTP contract](../../api-contract.md#post-v1chat).

- Messages: up to 12, user/assistant roles only; digests: up to two. Text and message text: up to 2000 code points.
- Facts: up to 75 `{id,kind,category,key,text,slot,days_seen,last_seen}`. Id matches `[PDT][0-9]{1,4}`; T iff kind temp, otherwise 422. Kinds: permanent/dynamic/temp. Categories: preference/portion/routine. Key at most 40, text at most 160, slot profile id or null, days_seen nonnegative, last_seen ISO date or null. Present even empty means structured memory.
- Recent: up to 42, text at most 240, ISO date, nullable slot. Day.remaining_kcal is an integer or null, computed by the app and possibly negative.
- Clarify_rounds: 0–3 or absent. Force_estimate: false by default, ignored without rounds.
- Auto_record: false by default, effective only with rounds.
- Temp_facts: false by default, effective only with facts. The client sends rounds and auto_record for the full v5 behavior. Older APKs get neither temp updates nor question_slot.
- Slot status: empty/eaten/skipped. Invalid input returns 422; unauthorized 401; oversized body/photo 413. Content refusals return the usual 200 shape, moderation failure 503, generation failure a 200 fallback. No 504 for the model timeout.

## Boundaries and observability

The client builds profile/day/history, applies records and stores memory; the server never reads Room. Content logging restrictions and safety correlation belong to [content policy](../../content-policy/specifications/content-policy.md) and [identity/audit](../../content-policy/specifications/identity-and-audit.md#closed-test-profile). Never log photo base64 or secrets.

For dev chat logs, `clarify` identifies the release/hold reason and `clarify_rounds` is a number or null. `record` uses the reason labels in rule 5d or null outside v4; internal `record_intent` and `meal_day` are model enums or null. Logs also include `temp_facts`, the count of T references for effective v5 or null otherwise, and `question_slot`, the returned held slot or null. These fields contain no fact or user text. The conversation-log serializer writes these metadata.

## Related decisions

[ADR-012](../../produto/adrs/ADR-012-chat-home-perfil.md), [ADR-015](../adrs/ADR-015-log-conversa-dev.md), [ADR-017](../../produto/adrs/ADR-017-registro-consolidado.md), [ADR-022](../../produto/adrs/ADR-022-limite-texto-chat.md), [ADR-023](../../produto/adrs/ADR-023-chat-v2-memoria-v2.md), [ADR-024](../../content-policy/adrs/ADR-024-content-safety-boundaries.md), [ADR-026](../../produto/adrs/ADR-026-perguntas-antes-da-estimativa.md), [ADR-028](../../produto/adrs/ADR-028-registro-autonomo.md) and [ADR-029](../../produto/adrs/ADR-029-fatos-temporarios-compactacao.md).

## Functional acceptance

- A clear meal eaten today records with a known slot; true other-day meals do not. Past estimation/purchase alone does not change eating day.
- Record corrections reconstruct the meal and reuse the known/suggested slot. Day-only answers to generic questions do not record.
- Habit fallback requires two matching distinct days, or a routine fact.
- V5 labels produce reusable temp proposals; generic questions and old clients do not. Held v5 turns retain their slot; old response shapes remain compatible.
- Compaction retains stated facts and pending questions, excludes assumptions/record status, and fails closed on moderated output.
- Auth, payload/photo boundaries, fail-soft generation and fail-closed moderation remain covered by route tests.

## Provenance

- [S18](../plans/s18-meal-additions-and-revisions.md) — meal additions and revisions

- [S17](../plans/completed/s17-unavailable-meal-details.md) — unavailable meal details and useful clarification alternatives

- [S1](../plans/completed/s1-timeout-photo-cap.md) — timeout and photo cap
- [S2](../plans/completed/s2-v1-chat.md) — Chat route
- [S3](../plans/completed/s3-compact.md) — compact digest
- [S8](../plans/completed/s8-chat-json-slot-consolidado.md) — structured output and consolidated meals
- [S9](../plans/completed/s9-limite-texto-2000.md) — text limit
- [S10](../plans/completed/s10-avaliacao-chat.md) — evaluator
- [S11](../plans/completed/s11-chat-v2.md) — intent, structured memory and recent meals
- [S12](../plans/completed/s12-slot-nomeado.md) — named-slot priority
- [S13](../plans/completed/s13-perguntas-antes-da-estimativa.md) — questions and release gate
- [S14](../plans/completed/s14-registro-autonomo.md) — record mark and skips
- [S15](../plans/completed/s15-registro-casos-dificeis.md) — hard record cases
- [S16](../plans/completed/s16-dia-da-refeicao-fatos-temporarios.md) — eating day, corrections, habitual fallback, temp capability and honest digests
- [A38](../../android/plans/completed/a38-fatos-temporarios-compactacao.md) — client storage and history marker
- [CP2](../../content-policy/plans/completed/cp2-server-content-controls.md) — content controls
- [CP3](../../content-policy/plans/completed/cp3-server-safety-identifier.md) — safety identifier
- [CP9](../../content-policy/plans/out_of_scope/cp9-production-audit-and-containment.md) — production audit and containment
