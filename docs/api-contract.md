# HTTP contract — /health, /v1/estimate, /v1/fit, /v1/chat

Planning notice (2026-09-30): [content handling](content-policy/specifications/content-policy.md) and [identity/audit](content-policy/specifications/identity-and-audit.md) propose controls and additive headers/errors through CP2/CP3. They are not implemented by this documentation update. The deployed contract below remains the baseline; prompt markers alone are not proof of semantic scope enforcement.

Auth: header `X-Invite: $INVITE_CODE` (constant-time validation, HTTP 401 `{"detail": "unauthorized"}` if missing or mismatch).
Content-Type: application/json

Rate limits: 30 req/minute per IP + invite on `/v1/estimate`, `/v1/fit`, and `/v1/chat`. Returns HTTP 429 `{"detail": "rate_limit_exceeded"}` when limit is exceeded.
Body size limit: HTTP 413 `{"detail": "payload_too_large"}` when `Content-Length` > 24 MB (25,165,824 bytes) enforced at the ASGI layer. It covers the photo cap plus JSON; a photo over the cap is still `photo_too_large` (S8).
Field lengths: `text` in `/v1/estimate` and `/v1/fit` has a maximum length of 1,000 characters; `/v1/chat` limits are listed in its section. Returns HTTP 422 when exceeded.
Prompt injection defense: User inputs are encapsulated in strict markers (`### USER_MEAL_INPUT_START` / `### USER_MEAL_INPUT_END` for estimate/fit, and `### USER_MESSAGE_START` / `### USER_MESSAGE_END` for chat) and treated strictly as meal data.

Request id: optional request header `X-Request-Id` (`[A-Za-z0-9-]{1,64}`). The server reuses it, or generates a UUID when it is missing or invalid, and always returns it in the `X-Request-Id` response header, on every route and status. Optional request headers `X-App-Version` and `X-App-Env` are only recorded. None of them changes the JSON body.
Dev conversation log (ADR-015): when `CONVERSATION_LOG_PATH` is set, each call to the model writes one JSON line (input text, raw model output, error, final response, `fallback`: `false` | `"error"` | `"text_only"`, latency). Never the photo, the invite or the API key. Off by default.

The server does not compute the ceiling. The app sends the budget on /fit.

## GET /health
`{ "ok": true, "model": "gpt-6-luna" }`

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
- `facts`: ≤ 70 items. `id` matches `[PD][0-9]{1,4}`. `kind` `permanent` | `dynamic`. `category` `preference` | `portion` | `routine`. `key` ≤ 40, `text` ≤ 160 characters. `slot`: a `profile.slots` id or `null`. `days_seen` ≥ 0. `last_seen`: ISO date or `null`.
- **v2 client** = `facts` present (even `[]`): the facts replace `memory` in the prompt. **Legacy client** = no `facts`: `memory` (text) goes to the prompt as before.
- `recent`: meals recorded in the last 7 days, ≤ 42 items, `text` ≤ 240, `date` ISO, `slot_id` `null` = "Outros". Any client may send it.
- `day.remaining_kcal`: integer or `null`, effective ceiling − eaten, computed by the app (may be negative). Any client may send it.

Optional fields (S13, ADR-026), out of limits → HTTP 422:
```json
{"clarify_rounds": 1, "force_estimate": false}
```
- `clarify_rounds`: integer 0–3, question rounds already shown for the pending meal. **v3 client** = `clarify_rounds` present: it renders question-only turns. Absent = the response is the same as before S13.
- `force_estimate`: boolean, default `false`. The user tapped **Forçar estimativa**. Ignored without `clarify_rounds`.

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
- `estimate`: present for `log` and `plan`, `null` for `question`. A `plan` never carries a `question`.
- `estimate.meal_text`: the whole meal in pt-BR as corrected by the conversation, ≤ 160 characters. The app records this text.
- `memory_updates` (v2 client only, else `[]`): at most 5 proposals `{op, id, kind, category, key, text, slot}`, `op` `add` | `reinforce` | `replace` | `remove`. `add` has `id: null`; the others carry an id from `facts`. `slot` is set only for `routine`. The app decides and applies them; the server stores nothing.
- `memory_used` (v2 client only, else `[]`): ids from `facts` the reply relied on, unique, at most 10.
- Legacy client: `intent: plan` returns `estimate: null` (no record card on APK ≤ 0.0.3; grams and total stay in `reply`).
- The server asks the model for structured output (`json_schema` strict). `suggested_slot` is an enum of the `profile.slots` ids plus `null`.
- `suggested_slot`: always a string id from `profile.slots` or `null`. The server normalises `1` / `{"id": 1}` to `"1"` and discards any other value to `null`.
- A message that completes or corrects a meal whose slot is `eaten` returns the estimate of the **whole meal** with that slot (ADR-017). A calorie total without food returns `estimate: null`.
- `question`: exists only if `confidence != high`. Missing → `"Alguma porção foi diferente do que considerei?"`. Clients without `clarify_rounds` only.

v3 client (`clarify_rounds` present, S13): the OUT gains a top-level `question` (string or `null`) and `estimate.question` is always `null`. A `log` turn with a material doubt is a question-only turn:
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
- `plan`, `question` and `estimate: null` turns are unchanged, with `question: null`. Fallbacks carry `question: null`.
- Model answered plain text (no JSON): that text is the `reply`, `estimate: null`. Model failure, timeout, empty or invalid output: HTTP 200 `{"reply": "nao deu pra estimar", "intent": "question", "estimate": null, "memory_updates": [], "memory_used": [], "digest": null}`.
- `model`: always `gpt-6-luna`.

Timeout 60s. Cap 16 MB JPEG. HTTP 413 `{"detail":"photo_too_large"}` when `image_b64` is longer than 22400000 characters. Photo is not persisted.

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
- `digest`: pt-BR prose, facts only (foods, kcal/P as stated, slot, skips), no advice. ≤ 400 tokens (capped at 1600 characters).
- Model failure/timeout or empty digest: HTTP 200 with `"digest": null` (fail-soft). The client keeps its raw messages.
- Stateless: the server returns the text; the client stores it (`day_digest`).
