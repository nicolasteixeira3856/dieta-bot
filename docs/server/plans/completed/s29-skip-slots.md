# Plan — S29 Skip slots next to any intent

- Status: Concluído (07/10/2026)
- Date: 07/10/2026
- Owning context: `server`
- Executable boundary: `server/` only: the request capability `skip_slots`, the `chat_turn` schema (`skip_slot` → `skip_slots`), the record gate, the Chat instructions for skips and their reply clause, the dev log metadata, tests and evaluation cases. No new route.
- Related documentation: [ADR-047](../../../produto/adrs/ADR-047-skips-alongside-other-actions.md), [ADR-028](../../../produto/adrs/ADR-028-registro-autonomo.md), [server Chat specification](../../specifications/v1-chat.md), [HTTP contract](../../../api-contract.md).
- Prerequisites: none. Built on the current `master` (S23 + S28 on the dev server). S24 (draft, stopped) and S25–S27 touch the same instruction and gate files: whichever lands later rebases; no parallel server plan.

Approving this plan accepts ADR-047. Authorization and delivery follow [SDD](../../../sdd/README.md). Approval: `Aprovo o plano docs/server/plans/s29-skip-slots.md. Implemente o plano aprovado.`

## Objective

A message that states a skip next to a meal, a plan or a question returns every skipped slot in `skip_slots`, and the reply names each skip, so the client can apply it (A59) and nothing the user said is dropped (incident 2026-10-07, request `fbe15524`).

## Scope

### 1. Contract

- Request: `skip_slots: true` (strict bool, default false); requires `clarify_rounds`, `auto_record` and `meal_changes`, otherwise 422 like `meal_changes`.
- Response for that client: `skip_slots: [ids]` on every turn, profile order, no repeats, every id a slot of the request PROFILE; `[]` on refusals, fallbacks, `meal_day: other` and when nothing is skipped. `skip_slot` stays as today (rule 4) so the record mark of a skip-only turn is unchanged.
- Without the capability: wire unchanged (`skip_slot` from the first listed slot on a skip turn).

### 2. Model schema and gate

- `chat_turn`: internal `skip_slot` becomes `skip_slots`, an array of the per-request slot enum (all clients; the legacy shape is derived on the server).
- Record gate: a new step after rules 1–3 builds `skip_slots` for the opted-in client: drop unknown ids and repeats; drop the log estimate's `suggested_slot` when the turn is a released log (the log wins, ADR-047 decision 1); empty for refusals, fallbacks and other-day turns. Rules 4–5 read the first remaining slot for `intent: skip`. Record log values gain `skips={n}` (count only).
- Held turns (a question before the estimate) keep their `skip_slots`: the skip is a stated fact, independent of the held meal.

### 3. Instructions (both instruction variants)

- Rule 3a: a skip is a fact of the turn, not only an intent: list in `skip_slots` every profile slot the user says did not happen today or firmly will not, whatever the intent; a hedged or pending meal is never listed; a slot is never listed because an earlier slot is empty or a later meal was logged.
- Reply: one short neutral clause per listed slot (`{slot} de hoje fora.`), never saying it was saved (rule 3f); a skip naming no PROFILE slot gets one clause saying no meal with that name exists today and that the Home card can be held to skip.
- Global examples follow [ADR-033](../../../content-policy/adrs/ADR-033-global-chat-example-provenance.md): synthetic, no tester text.

### 4. Evaluation

- Cases, tag `s29` (synthetic): a skip followed by a breakfast log (expect `log`, `record auto`, `skip_slots` = [the pre-workout], the reply clause); two skips in one message (expect `skip`, both slots); a skip plus `o que janto?` (expect `plan`, the skip listed); a hedged skip next to a log (expect `[]`); a skip of the same slot the log targets (expect `[]`); a skip naming no profile slot next to a log (expect `[]`, the clause about the missing meal); a legacy-shape request unchanged.
- Run per the [server README](../../README.md#chat-evaluation) table: the `s29` cases and the cases that assert `skip_slot` at `--repeat 3`, then the whole suite at `--repeat 1` against the last recorded full run.

### 5. Dev deploy

After validation: `tools/deploy-gcp.ps1`, code only; smoke with the incident's shape (synthetic text) and one legacy request; request ids recorded.

### Specification changes at Completion

- [v1-chat](../../specifications/v1-chat.md): rules 3a (skips as a list next to any intent), 3f (the clause per skip), 5 (schema), 5d (the `skip_slots` step), observability (`skips` count). Provenance line.
- [HTTP contract](../../../api-contract.md): `skip_slots` in the request and the response, with the example of a log plus a skip.
- ADR-047 status to Accepted (if not already by D19); ADR-028 and ADR-020 status lines record the partial supersession.

## Out of scope

- Client (A59), design (D19), deleting records on the server (the server writes nothing, ADR-013), skips of another day, production (blocked by the [production gate](../../../content-policy/production-gate.md)).

## Validation

1. `server/.venv/Scripts/python -m pytest server/tests -q` passes, including the capability validation, the gate step (unknown, repeated, log-target and other-day slots), the legacy derivation and the log metadata.
2. Evaluation as in scope 4; record noise as noise, per the server README.
3. Dev deploy and smoke; request ids recorded.
4. `node tools/check-docs.mjs` passes.

## Results

Approved by the owner on 07/10/2026 in the chat ("aprovo o plano docs/server/plans/s29-skip-slots.md. Implemente o plano aprovado."), together with the copy of ADR-047 decision 3. Branch `feat/s29-skip-slots` from master `c967205`.

### Delivered (`server/` only)

- `llm.py`: the `chat_turn` schema replaces `skip_slot` with `skip_slots`, an array of the per-request slot ids (`{"type": "null"}` items for a profile without slots), for every client.
- `shaping.py`: `skipped_slots()` (profile ids only, profile order, no repeats, minus the suggested slot of a log estimate) and `skip_list()` (empty on a refusal or an other-day turn); the record gate reads the first listed slot for `intent: skip`, so `skip_slot` and the legacy shapes are unchanged.
- `main.py`: `ChatIn.skip_slots` (strict bool, requires `meal_changes`, dropped by compact); the opted-in response carries `skip_slots` on every path (`[]` on fallbacks), including held turns and the unresolved-operation question; the dev log gains `skips` (a count).
- `chat_instructions.py`: the INTENT rules (both branches) keep their master text with the `skip_slot` sentences replaced by a pointer; a new `skips` rule (SKIPS, ADR-047) closes both Chat branches; the refusal and key lists name `skip_slots`; cues: the hedge and occasion-word meanings name `skip_slots`, and `merenda` joins the occasion words.
- Evaluator: `skip_slots` check (set of ids, NA without the field); capability tag `skip_slots`; six synthetic cases tagged `s29` (skip + breakfast, two skips, skip + plan, hedge + log, skip of the logged slot, skip of a meal with no slot).
- Tests: `server/tests/test_skip_slots.py` (15: helper order/dedupe/unknown ids, log-slot drop, plan slot kept, malformed lists; incident shape; skip-only turn; plan and question; refusal and other day; held turn; capability off; strict bool and meal_changes requirement; compact; HTTP with the log `skips` count; fallback `[]`; 422), schema and fixture updates in the existing tests, evaluator check test.

### Evaluation (effort none; OpenAI cost about US$ 0.20)

| Run | Scope | Result |
|---|---|---|
| first prompt, `--repeat 3` | the six `s29` cases and the 11 cases asserting `skip_slot` | 16/17; `s29-pulo-sem-slot` 1/3 (`merenda` mapped to Lanche) |
| `merenda` cue + "never the nearest slot" sentence | `s29` + two skip cases | 8/8, but the full suite at `--repeat 1` (212/230) and the rerun of its failures put `slot-cafe-20h` at 0/4 and `cafe-resposta-leite` at 0/4 |
| without the "nearest" sentence | four sensitive cases | `slot-cafe-20h` 2/3, `s29-pulo-sem-slot` 2/3, `cafe-resposta-leite` 0/3, `ceia-completa-suco` 0/3 |
| shipped prompt (INTENT kept as master, SKIPS as its own rule) | `s29` + 6 sensitive/skip cases, `--repeat 3` | 10/12: every `s29` case 3/3, skip cases 3/3, `slot-cafe-20h` 2/3, `cafe-resposta-leite` 1/3, `ceia-completa-suco` 1/3 |
| shipped prompt, full suite `--repeat 1` | 230 cases | 214/230 (S23: 215/223; S28 variant: 213/224); all six `s29` cases pass; p50 3.1 s, p95 5.3 s, 8,358 input tokens per call (6,971 cached) |
| failures of that run, `--repeat 3` | 16 cases | 0/3 only `s28-cafe-de-ontem-um-dia` (known, documented in S28) and `s19-04-recent-brands-1` (the copied-record rule of S28 empties the items the case checks: server code, not the prompt); the rest pass 1–3 of 3 (noise per the server README) |

Recorded as weaker than the master sample of the same day: `cafe-resposta-leite` (legacy branch, answer about milk; the model sometimes asks about butter) and `ceia-completa-suco` (legacy branch; the model writes the earlier meal as a 0 g item and the ADR-042 sum keeps only the juice). S28 already showed both fixtures move with any instruction edit. Stopped after two prompt attempts, per the lean policy. Prefix sizes: legacy 34,234, meal changes 37,995 characters.

### Validation

1. `server/.venv/Scripts/python -m pytest server/tests -q`: 424 passed, 433 subtests passed.
2. Evaluation as above.
3. Dev deploy from this branch with `tools/deploy-gcp.ps1`, code only; `GET /health` 200. Smoke: `s29-smoke-1791380470-0` (the incident shape with synthetic text, `skip_slots: true`) returned `log`, `record auto`, `skip_slots ["1"]`, reply `Pré-treino de hoje fora. …`; `s29-smoke-1791380470-1` (legacy v4 skip request) returned `skip`, `record auto`, `skip_slot "1"` and no `skip_slots`.
4. `node tools/check-docs.mjs`: passed.

Docs: [v1-chat](../../specifications/v1-chat.md) rules 3a, 3f, 5, 5d and observability, Provenance; [HTTP contract](../../../api-contract.md#skip-slots-capability); ADR-047 Accepted; ADR-028 and ADR-020 status lines. The current APK does not send the capability: nothing changes for testers until [A59](../../../android/plans/completed/a59-skips-with-other-actions.md).
