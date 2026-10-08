# HTTP contract — /health, /v1/estimate, /v1/fit, /v1/chat, /v1/close

Content controls (CP2, 2026-09-30, [ADR-024](content-policy/adrs/ADR-024-content-safety-boundaries.md)): see [content handling](content-policy/specifications/content-policy.md). Every model route moderates the current user text and photo before generation and the generated text after it (OpenAI moderation, fail closed), and the model must classify `scope`. Errors added: HTTP 400 `{"detail": "content_policy_blocked"}` on `/v1/estimate` and `/v1/fit`; HTTP 503 `{"detail": "content_policy_unavailable"}` on every model route when moderation fails or times out. One 60-second deadline covers moderation and generation.

Safety identifier (CP3, 2026-10-01, [ADR-025](content-policy/adrs/ADR-025-safety-correlation-audit.md), [identity/audit](content-policy/specifications/identity-and-audit.md#closed-test-profile)): see Installation header below.

Auth: header `X-Invite: $INVITE_CODE` (constant-time validation, HTTP 401 `{"detail": "unauthorized"}` if missing or mismatch).
Content-Type: application/json

Rate limits: 30 req/minute per IP + invite on `/v1/estimate`, `/v1/fit`, and `/v1/chat`. Returns HTTP 429 `{"detail": "rate_limit_exceeded"}` when limit is exceeded.
Body size limit: HTTP 413 `{"detail": "payload_too_large"}` when `Content-Length` > 24 MB (25,165,824 bytes) enforced at the ASGI layer. It covers the photo cap plus JSON; a photo over the cap is still `photo_too_large` (S8).
Field lengths: `text` in `/v1/estimate` and `/v1/fit` has a maximum length of 1,000 characters; `/v1/chat` limits are listed in its section. Returns HTTP 422 when exceeded.
Prompt injection defense: User inputs are encapsulated in strict markers (`### USER_MEAL_INPUT_START` / `### USER_MEAL_INPUT_END` for estimate/fit, and `### USER_MESSAGE_START` / `### USER_MESSAGE_END` for chat) and treated strictly as untrusted data. `###` inside client text is rewritten to `# # #`, so it cannot open or close a section.

Installation header: optional request header `X-Client-Instance-Id` on `/v1/estimate`, `/v1/fit` and `/v1/chat` (compact included), a canonical lowercase UUID v4 (36 characters, `xxxxxxxx-xxxx-4xxx-[89ab]xxx-xxxxxxxxxxxx`). Checked after `X-Invite`. Present but not in that shape: HTTP 400 `{"detail": "invalid_client_instance_id"}`, no model call; the value is never echoed or logged. Missing: accepted (older APKs), no identifier. The server derives `safety_identifier = "v1_" + HMAC-SHA256(SAFETY_ID_SECRET, SERVER_ENV + ":installation:" + uuid)` and sends it on every Responses call (never in the prompt, never on the moderation call); the raw UUID and the IP never reach the provider. Without `SAFETY_ID_SECRET` the server starts and sends no identifier. It is a correlation hint, not authentication, and never changes the JSON body.

Request id: optional request header `X-Request-Id` (`[A-Za-z0-9-]{1,64}`). The server reuses it, or generates a UUID when it is missing or invalid, and always returns it in the `X-Request-Id` response header, on every route and status. Optional request headers `X-App-Version` and `X-App-Env` are only recorded. None of them changes the JSON body.
Dev conversation log (ADR-015): when `CONVERSATION_LOG_PATH` is set, each call to the model writes one JSON line (input text, raw model output, error type and HTTP status, plus `reason` when a plain `ValueError` carries one of the server's fixed lowercase check messages, final response, `fallback`: `false` | `"error"` | `"text_only"`, `policy`, `safety_identifier` (derived, or null; never the raw UUID), latency). Never the photo, the invite, the API key or the provider's error text. A `policy_blocked` or severe turn keeps metadata only (CP2). Off by default.

The server does not compute the ceiling. The app sends the budget on /fit.

## GET /health
`{ "ok": true, "model": "gpt-6-luna", "safety_id": "on" }`

`safety_id`: `"on"` when `SAFETY_ID_SECRET` is set, `"off"` otherwise (CP3). Clients ignore unknown keys.

## POST /v1/estimate
IN
```json
{
  "local_time": "2026-09-23T12:35:00-03:00",
  "window": "lunch",
  "text": "4 colheres de arroz, 1 concha de feijão, 1 bife, salada sem azeite",
  "image_b64": null,
  "assumptions": [{"food":"leite","modifier":"semidesnatado"}]
}
```
OUT
```json
{
  "kcal": 610, "p": 42, "c": 68, "g": 16,
  "confidence": "medium",
  "question": "O bife era ~100 g ou ~150 g?",
  "items": [{"name":"arroz","g":80,"kcal":100}],
  "model": "gpt-6-luna"
}
```
`question` exists only if confidence != high.
`kcal` is the sum of `items[].kcal` (each rounded to a whole number) whenever at least one item carries a positive energy; `p`, `c` and `g` are scaled in the same ratio ([ADR-042](server/adrs/ADR-042-estimate-total-is-server-arithmetic.md)). The same rule applies to `estimate` in `POST /v1/chat`.
Out of scope, blocked or safety-support input (moderation or the model's `scope`): HTTP 400 `{"detail": "content_policy_blocked"}`, no zero-calorie placeholder.

## POST /v1/fit
IN
```json
{
  "mode": "want",
  "text": "quero hambúrguer caseiro",
  "available_items": [],
  "budget": {"kcal": 455, "p": 49},
  "image_b64": null
}
```
OUT: dish with portions, fits true/false, 1 question, 2 options in surprise mode.
Never offer a dish that blows the ceiling.
Out of scope, blocked or safety-support input: HTTP 400 `{"detail": "content_policy_blocked"}`.

## POST /v1/chat
IN
```json
{
  "local_time": "2026-09-25T21:10:00-03:00",
  "profile": {
    "ceiling_kcal": 2000,
    "p_target": 160,
    "c_target": 200,
    "g_target": 67,
    "eat_back": "zero",
    "slots": [{"id": "cafe", "name": "Cafe da manha", "time": "08:00"}]
  },
  "memory": "",
  "day": {
    "date": "2026-09-25",
    "eaten_kcal": 0,
    "eaten_p": 0,
    "eaten_c": 0,
    "eaten_g": 0,
    "workout_kcal": null,
    "slots": [{"id": "cafe", "status": "empty"}]
  },
  "digests": [],
  "messages": [],
  "text": "2 paes e 2 ovos",
  "image_b64": null,
  "compact": false
}
```
Constraints:
- `messages` max 12 items, roles: `user` | `assistant`.
- `digests` max 2 items.
- `text` and `messages[].text` max 2000 characters (code points; S9, ADR-022).
- `compact`: `false` = normal chat turn. `true` = summarise `messages` (S3), see below.

Optional fields (S11, ADR-023). Every one is optional; out of limits → HTTP 422:
```json
{
  "facts": [
    {"id": "P1", "kind": "permanent", "category": "preference", "key": "leite",
     "text": "Leite semidesnatado", "slot": null, "days_seen": 5, "last_seen": "2026-09-29"},
    {"id": "D2", "kind": "dynamic", "category": "routine", "key": "cafe",
     "text": "2 ovos mexidos, 1 pão francês c/ manteiga, 200 ml leite", "slot": "1", "days_seen": 3, "last_seen": "2026-09-30"}
  ],
  "recent": [
    {"date": "2026-09-29", "slot_id": "1", "slot_name": "Café", "text": "2 ovos mexidos, 1 pão francês",
     "kcal": 440, "p": 25, "c": 38, "g": 22}
  ],
  "day": {"remaining_kcal": 640}
}
```
- `facts`: ≤ 75 items. `id` matches `[PDT][0-9]{1,4}`. `kind` `permanent` | `dynamic` | `temp`. A `T` id requires `kind: temp` and vice versa, otherwise HTTP 422. `category` `preference` | `portion` | `routine`. `key` ≤ 40, `text` ≤ 160 characters. `slot`: a `profile.slots` id or `null`. `days_seen` ≥ 0. `last_seen`: ISO date or `null`.
- **v2 client** = `facts` present (even `[]`): the facts replace `memory` in the prompt. **Legacy client** = no `facts`: `memory` (text) goes to the prompt as before.
- `recent`: meals recorded in the last 7 days, ≤ 42 items, `text` ≤ 240, `date` ISO, `slot_id` `null` = "Outros". Any client may send it.
- `day.remaining_kcal`: integer or `null`, effective ceiling − eaten, computed by the app (may be negative). Any client may send it.

Optional fields (S13, ADR-026), out of limits → HTTP 422:
```json
{"clarify_rounds": 1, "force_estimate": false}
```
- `clarify_rounds`: integer 0–3, question rounds already shown for the pending meal. **v3 client** = `clarify_rounds` present: it renders question-only turns. Absent = the response is the same as before S13.
- `force_estimate`: boolean, default `false`. The user tapped **Forçar estimativa**. Ignored without `clarify_rounds`.

Optional field (S14, ADR-028):
```json
{"auto_record": true}
```
- `auto_record`: boolean, default `false`. **v4 client** = `clarify_rounds` present and `auto_record: true`: the OUT carries the record mark (below). Anything else = the response is the same as before S14.

Optional field (S16, ADR-029):
```json
{"temp_facts": true}
```
- `temp_facts`: boolean, default `false`. **v5 capability** = true with `facts` present; ignored for legacy clients without facts. Enables temporary proposals and the held `question_slot` below. V5 clients also send `clarify_rounds` and `auto_record`. Android support belongs to [A38](android/plans/completed/a38-fatos-temporarios-compactacao.md); the server must support T facts before that client ships.

Optional fields (S21, ADR-039), see [Plan-budget capability](#plan-budget-capability):
```json
{"plan_budget": true, "fit_kcal": 450}
```

Optional fields (S30, [ADR-044](produto/adrs/ADR-044-assistant-tone-and-closures.md), [ADR-046](produto/adrs/ADR-046-planned-meal-reservation.md)), any client:
```json
{
  "profile": {"tone": "duro"},
  "day": {"slots": [{"id": "jantar", "status": "planned", "text": "omelete de 2 ovos com salada",
                     "kcal": 550, "p": 30, "c": 6, "g": 40}]}
}
```
- `profile.tone`: `seco` | `duro`. Absent or `null` = `seco` (every client before S30). Any other value → HTTP 422. It picks the fixed instructions of the turn (one prefix per capability branch and tone); it never changes scope, refusals or the response shape. Compact ignores it.
- `day.slots[].status: planned`: the user reserved a plan for that meal (ADR-046). It requires `text` (1–2000 code points) and `kcal`, `p`, `c`, `g` (finite, ≥ 0); otherwise HTTP 422. Nothing of it was eaten: the client keeps it out of `eaten_*` and `remaining_kcal`. The server reserves the slot by its own kcal in the meal window (see [Plan-budget capability](#plan-budget-capability)), lists it in `WINDOWS` as `{slot}: planejado {kcal} kcal` and never closes it with a suggestion line. A planned slot is not occupied for the [meal-change capability](#meal-change-capability): a log into it is `operation: new`, `base_slot: null`.

OUT
```json
{
  "reply": "Otima escolha para o cafe da manha.",
  "estimate": {
    "kcal": 450,
    "p": 22,
    "c": 48,
    "g": 18,
    "confidence": "high",
    "question": null,
    "items": [
      {"name": "pao", "g": 100, "kcal": 270},
      {"name": "ovo", "g": 100, "kcal": 180}
    ],
    "suggested_slot": "cafe",
    "meal_text": "2 pães, 2 ovos"
  },
  "intent": "log",
  "memory_updates": [],
  "memory_used": [],
  "digest": null,
  "model": "gpt-6-luna"
}
```
- `intent` (S11): `log` (ate or is eating), `plan` (will eat, asks quantities or if it fits) or `question` (nothing to estimate).
- `reply` (S30, [ADR-045](produto/adrs/ADR-045-rich-replies-in-chat-bubbles.md)), every client: plain pt-BR text inside a fixed subset the client may render: `**bold**` spans; lines starting with `- ` (bullets, one level); lines starting with `1. ` (recipe steps); at most one table of two columns, a header row, a `| --- | --- |` separator and one to six rows. The server removes any other markup (headings, links, images, code, emphasis with `_` or single `*`, quotes, rules, HTML, emoji), keeping its text; nested or `*`/`+` bullets become `- `; a second table, a table of another width or beyond six rows becomes `- {item}: {grams}` lines; a sentence that is bold end to end and an unbalanced `**` lose the bold. Refusal copy, the fallback reply, the held-question reply's own prefix and the compact `digest` carry no markers. The client keeps `reply` raw for display and strips the markers wherever it stores or sends text.
- A released `log` whose `estimate.suggested_slot` is a `planned` DAY slot ends its prose (before the closing lines) with one line the server writes from the server total and the plan: `+{n} kcal sobre o plano.`, `−{n} kcal abaixo do plano.` or `Igual ao plano.`
- A `plan` reply never states whether the dish fits the day or by how much it goes over, for any client; the app shows the projected day. A recipe plan lists ingredients with grams and up to five numbered steps, and may include up to three foods marked `(opcional)` that are part of `estimate.items` and the totals (ADR-039).
- `estimate`: present for `log` and `plan`, `null` for `question`. A `plan` never carries a `question`.
- `estimate.meal_text`: the whole meal in pt-BR as corrected by the conversation, within the [meal-description bounds](#meal-change-capability). The app records this complete text; overflow returns a safe failure rather than truncated food.
- `memory_updates` (v2 client only, else `[]`): at most 5 proposals `{op, id, kind, category, key, text, slot}`, `op` `add` | `reinforce` | `replace` | `remove`. `add` has `id: null`; the others carry an id from `facts`. `slot` is set only for `routine`. The app decides and applies them; the server stores nothing.
- Temp proposals require v5 capability. Only `add` with null id, or `replace`/`remove` of a known T id, category `portion`/`preference`, slot null survive shaping. Temp reinforce/routine and operations on T ids with another kind are dropped. Without the flag, temp proposals are dropped. The model uses a matching reference on every portion, cites its T id and does not remove or reinforce it merely on use; the client owns lifetime/capacity.
- `memory_used` (v2 client only, else `[]`): ids from `facts` the reply relied on, unique, at most 10.
- Legacy client: `intent: plan` returns `estimate: null` (no record card on APK ≤ 0.0.3; grams and total stay in `reply`).
- The server asks the model for structured output (`json_schema` strict). The kind enum always includes `temp`; instructions and schema are identical with/without the capability for the same profile slots and fact ids. `suggested_slot` is an enum of the `profile.slots` ids plus `null`.
- `suggested_slot`: always a string id from `profile.slots` or `null`. The server normalises `1` / `{"id": 1}` to `"1"` and discards any other value to `null`.
- A message that completes or corrects a meal whose slot is `eaten` returns the estimate of the **whole meal** with that slot (ADR-017). A calorie total without food returns `estimate: null`.
- `question`: exists only if `confidence != high`. Missing → `"Alguma porção foi diferente do que considerei?"`. Clients without `clarify_rounds` only.

v3 client (`clarify_rounds` present, S13): the OUT gains a top-level `question` (string or `null`) and `estimate.question` is always `null`. A `log` turn with a material, answerable doubt is a question-only turn:
```json
{
  "reply": "Entendi: macarrão com frango ao molho branco.
Quanto de macarrão? E o molho era com creme de leite ou requeijão?",
  "intent": "log",
  "estimate": null,
  "question": "Quanto de macarrão? E o molho era com creme de leite ou requeijão?",
  "memory_updates": [],
  "memory_used": [],
  "digest": null,
  "model": "gpt-6-luna"
}
```
- `reply` on a question-only turn is the history text: `Entendi: {meal_text}.` + newline + the question (no `meal_text`: the question alone). The app stores it and sends it back in `messages[]`; it does not display it.
- The server releases the estimate (`estimate` present, `question: null`) when `force_estimate` is true, confidence is high or the model asked nothing, `clarify_rounds` is 3, or the question repeats one asked in an `assistant` turn of `messages`. A v3 client never gets the generic question above.
- Clarification distinguishes omitted from explicitly unavailable details per food/attribute. Do not ask again for an unavailable weight, brand or other detail, even approximately or in another unit. Use known counts, sizes, measures and photo context; a useful alternative may be asked if still answerable. Once none remains, estimate the whole meal with question null, honest medium/low confidence for assumed portions and a short assumption in reply. Later explicit measurements override earlier unavailability. The release gate and wire fields are unchanged; [Server Chat](server/specifications/v1-chat.md#functional-rules) owns the detailed rules.
- `plan`, `question` and `estimate: null` turns are unchanged, with `question: null`. Fallbacks carry `question: null`.

v4 client (`clarify_rounds` + `auto_record: true`, S14): the OUT also carries `record` and `skip_slot`, and `intent` may be `skip`. The server still writes nothing; the client records, checks its own guards (slot of today, no pending question) and may downgrade `auto` to `ask`.
```json
{
  "reply": "Ok, café de hoje fora.",
  "intent": "skip",
  "estimate": null,
  "memory_updates": [],
  "memory_used": [],
  "digest": null,
  "model": "gpt-6-luna",
  "question": null,
  "record": "auto",
  "skip_slot": "cafe"
}
```
- `record`: `auto` (record by itself), `ask` (show one **Registrar** button) or `none` (do not record). First matching rule wins:
  1. Refusal (scope or moderation, any fixed reply) → `none`.
  2. `intent` `question` or `plan`, a question-only turn, or a fallback → `none`.
  3. Food was eaten on another day → `none`; the `reply` says the Chat records only today's meals. A past day of asking, estimating, buying, cooking or planning is not the eating day. The before-05:00 dinner exception and food eaten `agora` follow the [Chat specification](server/specifications/v1-chat.md).
  4. `intent: skip` with a `profile.slots` id → `auto`, `skip_slot` = that id.
  5. `intent: skip` without a valid slot → `none`, shaped to `intent: question`.
  6. `log` with a released estimate and `suggested_slot: null` → `ask`.
  7. `log` with a released estimate and a clear intent to record (eating stated, "registra", a meal name followed by food, a photo with no text, `force_estimate`) → `auto`. A food photo sent with a question about it ("isso tem muita caloria?") is not clear → rule 8 (S15).
  8. Any other released `log` → `ask`.
- A request/complaint to record an identifiable meal is clear, reconstructed from history/digests, but does not change its eating day (`registra o almoço de ontem` remains other). An explicit eating-day correction overrides assistant assumptions. A day-only answer after a generic nutrition question does not create a log. Slot priority includes the earlier assistant marker `[refeição sugerida: {slot name}]` after user-named slots; it is a suggestion, not user testimony.
- `skip_slot`: a `profile.slots` id on rule 4, else `null`.
- `intent: skip` ("pulei o café", or a firm skip ahead: "hoje não vou jantar"): `estimate: null`, one short neutral `reply`. A pending meal ("ainda não almocei") or a hedged skip ("acho que não vou jantar") is never `skip`: `record: none` (S15). Clients before v4 never see it: the turn comes as `intent: question` with the same `reply`.
- The `reply` never says a meal was recorded or skipped; the app shows the receipt.
- Refusals and fallbacks carry `record: "none"`, `skip_slot: null`.
- Content refusal (CP2): the model's `scope` is not `in_scope`, or moderation flagged the input or the output. HTTP 200 in the normal shape: fixed pt-BR `reply` (e.g. `"Posso ajudar com refeições, porções e o orçamento alimentar do dia."`), `intent: "question"`, `estimate: null`, `memory_updates: []`, `memory_used: []`, `digest: null`, plus `question: null` for a v3 client. Copy: [refusal-copy.pt-BR.md](content-policy/specifications/refusal-copy.pt-BR.md). `scope` is never returned.
- Moderation error or timeout: HTTP 503 `{"detail": "content_policy_unavailable"}`.
- Model answered plain text (no JSON): the fixed out-of-scope `reply`, `estimate: null`. The model text is never returned. Model failure, timeout, empty or invalid output: HTTP 200 `{"reply": "nao deu pra estimar", "intent": "question", "estimate": null, "memory_updates": [], "memory_used": [], "digest": null}`.
- `model`: always `gpt-6-luna`.

v5 held turn (S16): when the clarify gate withholds an estimate, `question_slot` is the sanitized suggested profile slot id or null. It is absent for clients without effective temp capability, released turns, fallbacks and refusals. The field allows the client to retain the suggestion in history even when estimate is null. Dev chat log metadata adds `temp_facts` (T count, or null without capability) and `question_slot` (id or null), with no fact text in these fields.

Timeout 60s. Cap 16 MB JPEG. HTTP 413 `{"detail":"photo_too_large"}` when `image_b64` is longer than 22400000 characters. Photo is not persisted.

### Meal-change capability

Normal requests may opt into `meal_changes: true` with `clarify_rounds` present and `auto_record: true`. The capability accepts only JSON booleans. Other true-capability combinations return 422. Eaten DAY slot nutrients must be finite, nonnegative JSON numbers; strings, booleans and missing values return 422 before model generation. Validation errors for requests carrying meal-change fields expose field location, message and type, without echoing raw input; an overflowing numeric literal still returns serializable 422 JSON. Absent/false retains every older normal response shape. Compact ignores meal_changes and pending_addition. No model or effort change.

The opt-in request requires a unique profile slot list and exactly one DAY state per profile slot. Optional `pending_addition: {base_slot, addition}` describes the immediately continued **unrecorded** proposal; its shapes match the response below. It requires the capability, otherwise 422. A non-null base must identify an eaten DAY slot. Nested unknown fields, nonnumeric/nonfinite/negative nutrients, empty item lists or invalid item energy/portions return 422. The caller sends it only while the captured source still matches DAY and drops it for consumed, cancelled or stale proposals. It does not become part of DAY or authorize a mutation.

Effective responses always include `meal_change`, either null or:

```json
{
  "operation": "add",
  "base_slot": "meal-id",
  "addition": {
    "meal_text": "50 g de fruta",
    "kcal": 40, "p": 1, "c": 9, "g": 0,
    "items": [{"name": "fruta", "g": 50, "kcal": 40}]
  }
}
```

- `operation`: `new`, `add`, `revise`. New has null base and addition and cannot target an occupied slot. Revise has an eaten base equal to suggested_slot, null addition, and a complete replacement estimate. An add to an eaten target has that same base id; an empty, skipped or unknown target has null base. Targets must exist in both PROFILE and DAY.
- `addition` contains only new food, with 1–100 items in the existing shape `{name,g,kcal}`. Names are nonblank and at most 500 code points; descriptions are nonblank; grams are positive and finite; kcal/P/C/G are finite and nonnegative. Nutrients and each item's kcal round once to whole numbers, ties upward. Rounded item kcal must sum to rounded delta kcal. A positive caloric input cannot round to zero. Zero-energy, zero-macro food is allowed. Energy need not equal 4P + 4C + 9G; label values for alcohol remain valid.
- On occupied add, the server ignores model-supplied consolidated numbers: estimate kcal/P/C/G = exact supplied DAY values + rounded delta. For example, base `301.25 / 12.5 / 30.25 / 10.75` plus the delta above produces `341.25 / 13.5 / 39.25 / 10.75`. Base nutrients must be finite and nonnegative. `estimate.items` is empty because the historical base is an aggregate without item weights; addition.items supplies the new breakdown. New/revise estimates retain the whole-meal item breakdown; their total is recomputed from the rounded item kcal (ADR-042) instead of refused on a mismatch. Items without usable grams or energy (nonpositive or nonfinite grams, negative or nonfinite kcal) are dropped before validation; an estimate left with no usable item keeps its totals with `items: []`, and an estimate whose total equals a supplied `recent` row or eaten `day` slot is a copied record: its numbers stand and `items` is `[]`. Malformed items of a different kind (nonnumeric, missing name) still fail.
- Normal `estimate.meal_text` (including legacy, new and revise) and `addition.meal_text`: at most **500 Unicode code points**. For occupied additions only, composed estimate text is the exact DAY text, `; `, and delta text, at most **2000 code points**. Supplementary characters count once. Neither comma nor character truncation is allowed. Overflow fails with no action or memory proposal. Memory-fact, digest and user-message limits are unchanged.
- Addition reply uses validated numbers: `{food}: +{delta} kcal`, `Já registrado no {slot}: {prior} kcal`, `Total do {slot}: {total} kcal`. A revision uses `Atualizar {slot}?`, `Antes: {prior} kcal`, `Novo total: {total} kcal`. The model may describe a brief food assumption in meal_text. Neither response claims recording already happened.
- Held, refused, failed, other-day, plan and skip turns carry null meal_change. Invalid or missing metadata for a log candidate fails safely, without record or memory effects; it never falls back to the legacy update path. Unknown operation remains non-actionable even on force or the round cap. A known add with unknown target can be held for a target question, or released as the delta alone with suggested_slot null and record ask when forcing, at the cap, after a repeated question or when no target question is asked.
- Clients still validate the captured source against actual local state before any write. Server fields are not concurrency tokens. Confirmation remains required for occupied targets. Without the capable client, the existing whole-record destination controls remain; clearer legacy prose does not make rerouting a delta safe. The server never repairs historical local records.

### Skip-slots capability

Behavior: [ADR-047](produto/adrs/ADR-047-skips-alongside-other-actions.md). A normal request with `meal_changes: true` may also send `skip_slots: true` (JSON boolean only; without `meal_changes` it is a 422). Compact ignores it. Absent/false keeps every older response shape, including `skip_slot`.

With it, every response carries `skip_slots`: the profile slot ids the message says did not happen today or firmly will not, in profile order, without repeats, next to any `intent`. It never holds the suggested slot of the turn's log estimate (the log wins) and is `[]` on refusals, fallbacks, other-day turns and when nothing is skipped. Held, plan and question turns keep their skips. `record` and `skip_slot` follow the v4 rules unchanged; on `intent: skip`, `skip_slot` is the first listed slot.

```json
{
  "reply": "Pré-treino de hoje fora. Estimativa do café: 2 ovos mexidos e 1 pão francês, 300 kcal.",
  "intent": "log",
  "record": "auto",
  "skip_slot": null,
  "skip_slots": ["1"],
  "estimate": {"kcal": 300, "suggested_slot": "2", "...": "..."}
}
```

- The reply names each listed meal in one short neutral clause and never says it was saved. A skip of a meal with no profile slot is not listed; the reply says no meal with that name exists today and that its Home card can be held to skip it.
- The server writes nothing. The client applies the log first, then each listed skip, reading the slot state when it applies it (ADR-047).

### Plan-budget capability

Normal requests may opt into `plan_budget: true` with `clarify_rounds` present and `auto_record: true`. The capability accepts only JSON booleans; other combinations return 422. `fit_kcal`: optional integer 1–5000, sent with **Ajustar para caber**; it requires the capability, otherwise 422. Absent or false keeps every older response shape. Compact ignores both fields. No model or effort change.

Effective responses always include `plan_budget`, either null or:

```json
{
  "limit_kcal": 450,
  "over_kcal": 310,
  "reserved": [{"label": "fatia de bolo", "kcal": 250}],
  "choice": null
}
```

- Present for an in-scope `plan` of today with an estimate when the request carries `day.remaining_kcal`. Null for every other intent, a held or refused turn, a fallback, another meal day or a missing `remaining_kcal`.
- `reserved`: at most three other meals still to be eaten today that the user stated, each a label of 1–40 characters and an integer 1–3000 kcal (stated, or estimated by the model). They are not part of the plan's items, `meal_text` or any record.
- `limit_kcal`: `fit_kcal` when sent, otherwise the meal window: `day.remaining_kcal` minus the reservations of the other upcoming empty meals computed by the server ([ADR-043](produto/adrs/ADR-043-plan-objective-protein-and-meal-window.md); `reserved` lists them with the meal name), the kcal of every other `planned` slot (ADR-046, whatever the clock; the empty meals share what the plans leave) and the stated ones. A stated reservation never replaces a planned slot. May be zero or negative.
- `over_kcal`: `estimate.kcal` minus `limit_kcal`, rounded up, never below zero. The server computes it; the model never does.
- `choice`: `over_ok`, `fit` or null, what the user already said about the budget of this dish.
- Adjusting (`fit_kcal` sent, or `choice` `fit`) with `limit_kcal` ≥ 1 and the plan over it: the server asks the model once for the same dish within the target and returns that plan when it is valid. A plan still above the target returns with `over_kcal` > 0; there is no further attempt. With `limit_kcal` < 1 no adjustment is tried.
- A plan stays `record: none`. The server stores nothing. Dev chat log metadata adds `plan_budget` (this object or null) and `adjust_retry` (boolean).

### compact=true (S3)

Same IN. Only `messages` go to the model (no profile, day, digests, text or photo; `image_b64` is ignored). `messages` empty → HTTP 422 `{"detail":"compact_needs_messages"}`, model not called.

OUT
```json
{
  "reply": "",
  "estimate": null,
  "digest": "Cafe da manha: 2 paes e 2 ovos, 450 kcal e 22 g de proteina. Almoco pulado.",
  "model": "gpt-6-luna"
}
```
- `digest`: pt-BR prose, ≤ 400 tokens (capped at 1600 characters). Keep user-stated foods, quantities and nutrition numbers, user-named slots, skips and confirmed clarifications. Never state record status, an assistant-assumed day/slot or an unconfirmed estimate as eaten. A suggested-slot marker is not a user fact. Preserve explicit inability to supply a detail with its food/attribute scope; do not infer it from omission. An unavailable detail is closed, including after an assistant repetition. In a compound question only the unresolved, answerable part remains open. Later supplied measurements replace stale unavailability. With no open question, return facts only. Preserve an unanswered question and pending meal/photo description at the end as `Pergunta em aberto: {question} ({meal})`. No advice, judgement or new estimates/numbers.
- Model failure/timeout or empty digest: HTTP 200 with `"digest": null` (fail-soft). The client keeps its raw messages.
- The digest is moderated before it returns; a flag returns `"digest": null`. History is not re-moderated. Moderation error: HTTP 503 `content_policy_unavailable`.
- Stateless: the server returns the text; the client stores it (`day_digest`).

## POST /v1/close

Day and week closure text (S30, [ADR-044](produto/adrs/ADR-044-assistant-tone-and-closures.md)). Headers as `/v1/chat` (`X-Invite`, optional `X-Request-Id` and installation header). Same rate limit as `/v1/chat`. Body limit 16 KB (`Content-Length` above it → HTTP 413 `payload_too_large`).

IN (day)
```json
{
  "period": "day",
  "tone": "duro",
  "local_time": "2026-10-07T22:00:00-03:00",
  "profile": {"ceiling_kcal": 2000, "p_target": 150, "c_target": 220, "g_target": 65,
              "slots": [{"id": "jantar", "name": "Jantar", "time": "20:00"}]},
  "numbers": {"date": "2026-10-07", "kcal": 2230, "p": 98, "c": 250, "g": 80, "ceiling_kcal": 2000,
              "workout_kcal": null,
              "slots": [{"name": "Jantar", "status": "eaten", "kcal": 840}]}
}
```
IN (week): `"period": "week"` and `"numbers": {"days": [{"date", "kcal", "p", "c", "g", "ceiling_kcal", "workout_kcal", "recorded"}], "over_slot": {"name": "Jantar", "days": 4}}`.

- Every number is a JSON integer (`strict`: no floats, no strings): kcal 0–20000, grams 0–5000, `profile.ceiling_kcal` and each `ceiling_kcal` (the effective ceiling of that day) 1–20000, `workout_kcal` integer or `null`, `over_slot.days` 0–7. `tone`: `seco` | `duro`, default `seco`. `numbers.slots[].status`: `empty` | `eaten` | `skipped` | `planned`, with `kcal` integer or null; at most 12 slots. `week.days`: 1–7 items, each with `recorded` (JSON boolean). `over_slot`: optional. Meal names (`profile.slots[].name`, `numbers.slots[].name`, `over_slot.name`) are the only text, 1–40 characters. Unknown keys, a `numbers` shape that does not match `period` or any value outside these bounds → HTTP 422.

OUT
```json
{"text": "Passou 230 kcal do teto; o Almoço teve 980 kcal.\nAmanhã: ovos no café e salada no jantar.", "model": "gpt-6-luna"}
```
- `text`: pt-BR plain text (no markers), at most 3 lines and 400 characters. The model writes prose over the numbers; the server derives only the differences it serializes next to them (kcal over or left, macros missing or over; week total, means over the recorded days, days over the ceiling) and drops any sentence that carries a number not present in the serialized request (counts up to 10 excepted).
- A refusal of the output moderation, a generation failure, empty text after shaping or moderation unavailable return HTTP 200 with the fixed neutral line built from the numbers: `Dia fechado. {kcal} de {ceiling_kcal} kcal.` for a day; `Semana fechada. {total} kcal em {n} dias com registro.` (or `Semana fechada. Nenhum dia com registro.`) for a week. The closure itself never returns an HTTP error; 401, 413, 422 and 429 keep their meaning.
- Stateless: the server stores nothing. Dev log route `close` with `tone`, `period`, `fallback` (`false`, `error`, `policy` or `moderation`) and `close_dropped` (sentences dropped, or null); no photo fields.

## Provenance

- [S21](server/plans/completed/s21-plan-cooking-and-budget-choice.md) — cooking help and the plan budget check
- [S18](server/plans/completed/s18-meal-additions-and-revisions.md) — meal additions and revisions
- [S28](server/plans/completed/s28-copied-record-items.md) — new/revise items without usable grams dropped, copied records keep their numbers
- [S29](server/plans/completed/s29-skip-slots.md) — skip slots next to any intent
- [S24](server/plans/completed/s24-protein-first-plan.md) — the limit is the meal window; computed reservations in `reserved`; `(opcional)` protein items
- [S30](server/plans/completed/s30-tone-formatting-planned-slot.md) — `profile.tone`, `POST /v1/close`, the `reply` formatting subset and `status: planned`
