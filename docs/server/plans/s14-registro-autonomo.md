# Plan — S14 Record mark (`record`) and skip by text

- Status: Aguardando aprovação
- Date: 01/10/2026
- Owning context: `server`
- Affected code: `server/` (`main.py`, `llm.py`, `shaping.py`, `config.py`, `conversation_log.py`, `evals/`, `tests/`)
- Prerequisites: none. Executes [ADR-028](../../produto/adrs/ADR-028-registro-autonomo.md) decisions 1, 2, 4, 7 and 8. Opt-in by request field: no Android change needed to ship. Blocks [A34](../../android/plans/a34-registro-autonomo.md). Can run in parallel with [ST9](../../stitch/plans/completed/st9-registro-autonomo.md).

## Authorization gate

This plan is documentation only. Implementation starts only after an explicit approval naming this file:

> Aprovo o plano `docs/server/plans/s14-registro-autonomo.md`. Implemente o plano aprovado.

If implementation reveals an uncovered decision, stop, update the artifacts and ask for a new approval.

## Goal

`POST /v1/chat` tells an opted-in client whether the turn should be recorded by itself (`auto`), offered with one Registrar button (`ask`) or not recorded (`none`), and recognises "pulei o café" as a skip of a slot of today. The server still writes nothing. Legacy and v3 clients see no change.

## Sources of truth

- [ADR-028](../../produto/adrs/ADR-028-registro-autonomo.md), [ADR-026](../../produto/adrs/ADR-026-perguntas-antes-da-estimativa.md), [ADR-017](../../produto/adrs/ADR-017-registro-consolidado.md), [v1-chat](../specifications/v1-chat.md), [api-contract](../../api-contract.md).
- [Content policy](../../content-policy/specifications/content-policy.md): a refused or blocked turn is always `record: none`.
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

- Dev conversation log ([ADR-015](../adrs/ADR-015-log-conversa-dev.md)): `record` (log enum above, or `null` for a non-v4 client), `record_intent`, `meal_day` (enums). No user text in new fields.

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
- Docs in the same delivery: [v1-chat](../specifications/v1-chat.md) (rules 3a, 3f, 4, 5, new 5d, IN/OUT, log fields, acceptance criterion "Sem tap…" reworded to "O server nunca grava: só marca `record`"); [api-contract](../../api-contract.md) `/v1/chat`; server README; this plan's result.

## Planned validation

1. `pytest server/tests` green, with unit tests for each gate row, `force_estimate` and photo-only as `clear`, skip shaping for legacy, the strict schema accepting the new fields, policy refusal → `none`, legacy and v3 snapshots byte for byte (checked against `master` before S14).
2. `python -m server.evals.run --effort none --repeat 3`: every new case ≥ 2/3; existing 35 cases no regression vs [S13](completed/s13-perguntas-antes-da-estimativa.md).
3. Deploy to the dev server (`tools/deploy-gcp.ps1`), then one real v4 request by `curl` with `X-Request-Id: s14-*` and the log line checked for `record`.

## Out of scope

- Android (A34), Stitch (ST9).
- Any server-side write or session state.
- Retroactive recording ([A35](../../android/plans/out_of_scope/a35-registro-retroativo.md)).
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

For an owner-authorized future deferral, use `Fora de escopo` and `plans/out_of_scope/` under [SDD](../../sdd/README.md#fora-de-escopo).
