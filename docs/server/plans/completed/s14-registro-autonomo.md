# Plan — S14 Record mark (`record`) and skip by text

- Status: Concluído
- Approval: 02/10/2026 (owner: "Aprovo o plano docs/server/plans/s14-registro-autonomo.md. Implemente o plano aprovado.")
- Date: 01/10/2026
- Owning context: `server`
- Affected code: `server/` (`main.py`, `llm.py`, `shaping.py`, `config.py`, `conversation_log.py`, `evals/`, `tests/`)
- Prerequisites: none. Executes [ADR-028](../../../produto/adrs/ADR-028-registro-autonomo.md) decisions 1, 2, 4, 7 and 8. Opt-in by request field: no Android change needed to ship. Blocks [A34](../../../android/plans/pending_manual_validation/a34-registro-autonomo.md). Can run in parallel with [ST9](../../../stitch/plans/completed/st9-registro-autonomo.md).

## Authorization gate

This plan is documentation only. Implementation starts only after an explicit approval naming this file:

> Aprovo o plano `docs/server/plans/s14-registro-autonomo.md`. Implemente o plano aprovado.

If implementation reveals an uncovered decision, stop, update the artifacts and ask for a new approval.

## Goal

`POST /v1/chat` tells an opted-in client whether the turn should be recorded by itself (`auto`), offered with one Registrar button (`ask`) or not recorded (`none`), and recognises "pulei o café" as a skip of a slot of today. The server still writes nothing. Legacy and v3 clients see no change.

## Sources of truth

- [ADR-028](../../../produto/adrs/ADR-028-registro-autonomo.md), [ADR-026](../../../produto/adrs/ADR-026-perguntas-antes-da-estimativa.md), [ADR-017](../../../produto/adrs/ADR-017-registro-consolidado.md), [v1-chat](../../specifications/v1-chat.md), [api-contract](../../../api-contract.md).
- [Content policy](../../../content-policy/specifications/content-policy.md): a refused or blocked turn is always `record: none`.
- Skill `fastapi-security` (input limits, LLM input boundaries).

## Implementation scope

### 1. Request (`ChatIn`), additive

- `auto_record: bool = False`. Honoured only with `clarify_rounds` present (v3). `clarify_rounds` + `auto_record: true` = "v4 client". Anything else = today's behaviour, byte for byte.

### 2. Model output (`chat_turn` schema), all clients

- `intent` enum gains `skip` (the user says a meal of today did not happen: "pulei o café", "hoje não almocei"). Estimate null; the slot goes in the new field below.
- New required fields (strict schema, explicit null):
  - `record_intent`: `clear` | `unsure`. `clear` = the user states they ate (or skipped) the meal and expects it counted: past or present tense of eating, an explicit request ("registra", "anota", "marca"), a meal name followed by food ("Lanche da tarde: 200 g de…"), a plate photo with or without text, the answer to a question about that meal, "Pode estimar assim.". `unsure` = food with no sign of having been eaten ("pudim de leite com calda"), a doubt mixed with food, a hypothetical. Ignored outside `log`/`skip`.
  - `meal_day`: `today` | `other`. `other` = the message refers to a meal of another day ("ontem", "anteontem", a weekday in the past, "ontem à noite"). Late-night rule: a message before 05:00 local about "a janta" with no other date is `today` only when the profile has that slot today; otherwise `other`.
  - `skip_slot`: slot id of the profile (same enum as `suggested_slot`) or null.
- Legacy (no `auto_record`): `skip` is shaped to `question` (reply kept, `skip_slot` dropped); the new fields never leave the server.

### 3. Instructions (`_CHAT_INSTRUCTIONS`, English, all clients)

- Rule 3f ("o reply nunca diz que registrou") stays for every client: the app shows the receipt; the reply never claims a record or a skip.
- Add the `record_intent`, `meal_day` and `skip` definitions of § 2, with pt-BR examples taken from the owner's messages ("Na janta comi…", "Lanche da tarde: 200g de…", "Registra aí, comi tal e tal").
- Other day: estimate only if asked; the reply says in one short line that the Chat records only today's meals. Replaces "Outro dia ("ontem") → estima se pedido, e o reply avisa que grava hoje."
- Skip: reply one short neutral line, no advice.

### 4. Record gate (`shaping.record_gate`, pure function, unit-tested)

Runs for a v4 client after `clarify_gate` and after the content-policy shaping. In order:

| Order | Condition | `record` | Log `record` |
|---|---|---|---|
| 1 | `scope` ≠ `in_scope` or moderation flag (fixed reply) | `none` | `none_policy` |
| 2 | `intent` `question` or `plan`, or a question-only turn | `none` | `none_intent` |
| 3 | `meal_day = other` | `none` | `none_other_day` |
| 4 | `intent = skip` with `skip_slot` valid | `auto` | `auto_skip` |
| 5 | `intent = skip` without a valid slot | `none` (shaped to `question`) | `none_skip_slot` |
| 6 | `intent = log`, estimate released, `suggested_slot` null | `ask` | `ask_no_slot` |
| 7 | `intent = log`, estimate released, `record_intent = clear` | `auto` | `auto_log` |
| 8 | `intent = log`, estimate released, otherwise | `ask` | `ask_unsure` |

- `force_estimate: true` counts as `clear` (the user already asked for the estimate of a meal they described).
- A photo turn with no text and an in-scope food estimate counts as `clear`.
- Out: top-level `record` (`auto` | `ask` | `none`) and `skip_slot` (string or null; non-null only on row 4). v3/legacy responses do not carry either field.

### 5. Observability

- Dev conversation log ([ADR-015](../../adrs/ADR-015-log-conversa-dev.md)): `record` (log enum above, or `null` for a non-v4 client), `record_intent`, `meal_day` (enums). No user text in new fields.

### 6. Evaluator (`server/evals/`)

- New expectation keys in `checks.py`: `record` (`auto` | `ask` | `none`), `skip_slot` (id or `null`).
- New v4 cases (`clarify_rounds` and `auto_record` set), tag `record`:
  - `registro-na-janta-comi`: "Na janta comi arroz, feijão e 150 g de frango grelhado" → `record: auto`, slot jantar.
  - `registro-lanche-dois-pontos`: "Lanche da tarde: 200g de iogurte natural com granola" → `auto`, slot lanche.
  - `registro-pedido-explicito`: "Registra aí, comi 2 pães com manteiga e um café com leite" → `auto`.
  - `registro-almocei`: "almocei um PF de bife acebolado" → `auto` (or a question first, then `auto` on the answer).
  - `registro-comida-solta`: "pudim de leite com calda" → `ask`.
  - `registro-pergunta-nutricao`: "pudim tem muita caloria?" → `none`, intent `question`.
  - `registro-plano`: "vou jantar macarrão, cabe?" → `none`, intent `plan`.
  - `registro-ontem`: "ontem jantei pizza, 3 fatias" → `none`, reply mentions only today.
  - `registro-pulei`: "pulei o café hoje" → `auto`, intent `skip`, `skip_slot` café.
  - `registro-pulei-sem-slot`: "pulei a merenda" (no such slot) → `none`.
  - `registro-forcado`: `force_estimate: true` after a question → estimate present, `auto`.
  - `registro-acrescimo`: day with jantar recorded, "também comi um pudim" → `auto`, slot jantar, whole-meal estimate (ADR-017; the client asks before replacing).
  - `registro-fora-escopo`: an out-of-scope request → `none`.
- Existing cases keep passing unchanged (no `auto_record` → no `record` field).
- Run: 3 repetitions per case with the current effort (`none`); record pass rate and p95 latency here.

## Affected files and areas

- `server/main.py`, `server/llm.py`, `server/shaping.py`, `server/config.py`, `server/conversation_log.py`.
- `server/evals/checks.py`, `server/evals/cases/*.json` (new files only).
- `server/tests/test_chat.py`, new `server/tests/test_record.py`.
- Docs in the same delivery: [v1-chat](../../specifications/v1-chat.md) (rules 3a, 3f, 4, 5, new 5d, IN/OUT, log fields, acceptance criterion "Sem tap…" reworded to "O server nunca grava: só marca `record`"); [api-contract](../../../api-contract.md) `/v1/chat`; server README; this plan's result.

## Planned validation

1. `pytest server/tests` green, with unit tests for each gate row, `force_estimate` and photo-only as `clear`, skip shaping for legacy, the strict schema accepting the new fields, policy refusal → `none`, legacy and v3 snapshots byte for byte (checked against `master` before S14).
2. `python -m server.evals.run --effort none --repeat 3`: every new case ≥ 2/3; existing 35 cases no regression vs [S13](s13-perguntas-antes-da-estimativa.md).
3. Deploy to the dev server (`tools/deploy-gcp.ps1`), then one real v4 request by `curl` with `X-Request-Id: s14-*` and the log line checked for `record`.

## Out of scope

- Android (A34), Stitch (ST9).
- Any server-side write or session state.
- Retroactive recording ([A35](../../../android/plans/out_of_scope/a35-registro-retroativo.md)).
- Changing `reasoning.effort`.

## Risks and controls

- **The model marks `clear` too eagerly:** `ask` is the default when unsure; evaluator cases on both sides; the client's undo telemetry (A34) measures it in use.
- **Late-night "janta" lands on the wrong day:** explicit rule in § 2 plus a case; the client never records a slot outside today's slots.
- **Schema change breaks old clients:** new fields are internal; legacy/v3 snapshot tests.
- **Skip of a slot that has a record:** the client decides (A34 leaves the record untouched and marks the turn `Não registrado`; Excluir on the receipt removes it); the server only marks.

## Acceptance criteria

- A v4 clear `log` of today with a slot returns `record: auto`.
- A v4 unclear `log` returns `record: ask`; a `plan`, `question`, question-only, other-day or refused turn returns `record: none`.
- "pulei o café" returns `intent: skip`, `skip_slot` = café, `record: auto`.
- A request without `auto_record` returns the same shape as before S14.

## Closure

After implementation, record real results and apply the lifecycle in `docs/sdd/README.md`.

Only an explicit owner statement cancelling this plan allows `Cancelado` and `plans/cancelled/`.

For an owner-authorized future deferral, use `Fora de escopo` and `plans/out_of_scope/` under [SDD](../../../sdd/README.md#fora-de-escopo).

## Results (02/10/2026)

### Implemented

- `ChatIn.auto_record` (default `false`); `ChatIn.records` = `clarify_rounds` present and `auto_record` true (v4 client).
- `chat_turn` schema (all clients): `intent` enum gains `skip`; new required `record_intent` (`clear` | `unsure`), `meal_day` (`today` | `other`), `skip_slot` (profile slot ids + null), before `scope`. Enums in `config.RECORD_INTENTS` / `config.MEAL_DAYS`.
- `shape_chat(..., skip=False)`: before v4, `skip` becomes `question` with the reply kept; a `skip` never carries an estimate.
- `shaping.record_gate`: table § 4 in order, plus `force_estimate` and photo-only as `clear`. `main.shape_chat_turn` = `shape_chat` → `clarify_gate` (v3) → `record_gate` (v4), shared by the route and the evaluator; returns the record log value. Refusals (scope, moderation, text only) carry `record: none` (`none_policy`), fallbacks `none` (`none_intent`).
- `_CHAT_INSTRUCTIONS`: rule 3f kept and extended ("recorded, registered, noted, saved or skipped"); `skip`, `RECORD` (`record_intent`, `meal_day`, late-night rule) with the owner's pt-BR examples; the other-day sentence now says the Chat records only today's meals; refusal sets `skip_slot` null.
- Dev conversation log: `record` (log enum, `null` for a non-v4 client), `record_intent`, `meal_day` (enums from the model, any client) on every `chat` line. Set in `main` with the S13 fields; `conversation_log.py` needed no change.
- Evaluator: `record` (one value or a list of accepted values) and `skip_slot` in `checks.py` (`n/a` on an output without them); `intent` accepts `skip`; 16 `since: v4` cases, tag `record`.

### Implementation details within the plan

- **Late-night rule wording.** "today only when the profile has that slot today" was written for the model as: before 05:00, a janta with no date is `today` only when PROFILE has that slot at a time before 05:00 (night shift, e.g. Jantar 03:00); otherwise last night's janta, `other`. This matches ADR-028 decision 7 ("na janta comi X" sent the next morning is another day) and keeps `slot-noturno-jantei` (Jantar 03:00 at 03:10) as today. Two cases cover it: `registro-noturno-ontem`, `registro-noturno-turno`.
- **Food named alone.** First run: `registro-comida-solta` ("pudim de leite com calda") came back as `intent: question`, estimate null, so `none` instead of `ask`. One INTENT line added: "A food named alone, with no verb and no question, is log: estimate it." Then 3/3 `ask`.
- **Released estimate in single-turn cases.** First run 8/16: seven of the eight failures were the ADR-026 gate asking a question first (row 2, `record: none`, correct by ADR-028 decision 1); the eighth was `registro-comida-solta` above. The single-turn log cases that measure the mark, not the questions, now send `clarify_rounds: 3`, so the gate releases (`released_cap`) and only the mark is judged: `registro-na-janta-comi`, `registro-lanche-dois-pontos`, `registro-pedido-explicito`, `registro-comida-solta`, `registro-ontem`, `registro-noturno-ontem`, `registro-noturno-turno`, `registro-acrescimo`. `registro-almocei` keeps `clarify_rounds: 0` and accepts `auto` or `none` (a question first). The extra case `registro-almocei-resposta` checks `auto` on the answer.
- Extra cases beyond the § 6 list: `registro-almocei-resposta`, `registro-noturno-ontem`, `registro-noturno-turno` (risk "late-night janta").

### Validation

1. `pytest server/tests`: **211 passed** (175 subtests). New `test_record.py` (30 tests): each gate row 1–8, row 6 before `clear`/force/photo, `force_estimate` and photo-only as `clear`, missing `meal_day` = today, skip shaped to `question` before v4 (legacy and v2), skip without estimate, strict schema with the new fields (and `skip_slot` `[None]` with no slots), route: v4 auto log/skip/force/photo, scope refusal and output-moderation flag → `none_policy`, fallback → `none_intent`, log fields enum-only, v3 / `auto_record: false` / `auto_record` without `clarify_rounds` carry no record fields. Snapshot tests: v3 log and legacy/v2/v3 skip **byte for byte**; the same literals were run against `master` code (dab0435) in a temporary worktree: 5/5 identical. A wider dump (6 client shapes × 7 model payloads, new fields included) was also identical between `master` and the branch. `test_chat.py` / `test_evals.py` updated for the new schema keys, instruction text and expectation keys.
2. `python -m evals.run --effort none --repeat 3`:
   - Tag `record` (report `logs/evals/2026-10-02-144048-none.json`): **16/16**, each 3/3 except `registro-pulei-sem-slot` 2/3. p50 3239 ms, **p95 3988 ms**.
   - Full suite (report `logs/evals/2026-10-02-144421-none.json`): **66/68** (97.1%). p50 3100 ms, **p95 4081 ms**, US$ 0.0262 for 204 calls. The 52 existing cases: 50/52. Failed `ceia-completa-suco` 1/3 (already 2/3 in S13) and `memoria-cheia` 0/3. Re-run of both with 6 repetitions: `master` 5/6 and 3/6, branch 5/6 and 4/6. No regression against `master`: the model drifted on `memoria-cheia` since S13 (it was passing then).
3. `tools/deploy-gcp.ps1` (code only): `/health` 200. Real v4 requests (bodies of `registro-pulei` and `registro-na-janta-comi`): `X-Request-Id: s14-registro-pulei` → `intent: skip`, `record: auto`, `skip_slot: "1"`; log `record: auto_skip`, `record_intent: clear`, `meal_day: today`. `X-Request-Id: s14-registro-na-janta-comi` → `record: auto`, `suggested_slot: "5"`; log `record: auto_log`, `clarify: released_cap`.

### Findings for the owner (not changed, no new decision taken)

- **Other day with a doubt.** For a v3/v4 client, a `log` about another day with a material doubt ("ontem jantei pizza, 3 fatias", `clarify_rounds: 0`) is a question-only turn first: the ADR-026 gate runs before the record gate, so the reply is `Entendi: …` + the question, and the "only today" line arrives only on the released turn (`record: none` either way; nothing is recorded). Skipping the questions for `meal_day: other` would be a new decision (ADR-026 / ADR-028).
- `memoria-cheia` is flaky on `master` too (3/6): a candidate for a separate prompt fix.

### Pending

- None for S14. The client side is [A34](../../../android/plans/pending_manual_validation/a34-registro-autonomo.md), which accepts ADR-028 on completion.
