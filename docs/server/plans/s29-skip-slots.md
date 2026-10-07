# Plan — S29 Skip slots next to any intent

- Status: Aguardando aprovação
- Date: 07/10/2026
- Owning context: `server`
- Executable boundary: `server/` only: the request capability `skip_slots`, the `chat_turn` schema (`skip_slot` → `skip_slots`), the record gate, the Chat instructions for skips and their reply clause, the dev log metadata, tests and evaluation cases. No new route.
- Related documentation: [ADR-047](../../produto/adrs/ADR-047-skips-alongside-other-actions.md), [ADR-028](../../produto/adrs/ADR-028-registro-autonomo.md), [server Chat specification](../specifications/v1-chat.md), [HTTP contract](../../api-contract.md).
- Prerequisites: none. Built on the current `master` (S23 + S28 on the dev server). S24 (draft, stopped) and S25–S27 touch the same instruction and gate files: whichever lands later rebases; no parallel server plan.

Approving this plan accepts ADR-047. Authorization and delivery follow [SDD](../../sdd/README.md). Approval: `Aprovo o plano docs/server/plans/s29-skip-slots.md. Implemente o plano aprovado.`

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
- Global examples follow [ADR-033](../../content-policy/adrs/ADR-033-global-chat-example-provenance.md): synthetic, no tester text.

### 4. Evaluation

- Cases, tag `s29` (synthetic): a skip followed by a breakfast log (expect `log`, `record auto`, `skip_slots` = [the pre-workout], the reply clause); two skips in one message (expect `skip`, both slots); a skip plus `o que janto?` (expect `plan`, the skip listed); a hedged skip next to a log (expect `[]`); a skip of the same slot the log targets (expect `[]`); a skip naming no profile slot next to a log (expect `[]`, the clause about the missing meal); a legacy-shape request unchanged.
- Run per the [server README](../README.md#chat-evaluation) table: the `s29` cases and the cases that assert `skip_slot` at `--repeat 3`, then the whole suite at `--repeat 1` against the last recorded full run.

### 5. Dev deploy

After validation: `tools/deploy-gcp.ps1`, code only; smoke with the incident's shape (synthetic text) and one legacy request; request ids recorded.

### Specification changes at Completion

- [v1-chat](../specifications/v1-chat.md): rules 3a (skips as a list next to any intent), 3f (the clause per skip), 5 (schema), 5d (the `skip_slots` step), observability (`skips` count). Provenance line.
- [HTTP contract](../../api-contract.md): `skip_slots` in the request and the response, with the example of a log plus a skip.
- ADR-047 status to Accepted (if not already by D19); ADR-028 and ADR-020 status lines record the partial supersession.

## Out of scope

- Client (A59), design (D19), deleting records on the server (the server writes nothing, ADR-013), skips of another day, production (blocked by the [production gate](../../content-policy/production-gate.md)).

## Validation

1. `server/.venv/Scripts/python -m pytest server/tests -q` passes, including the capability validation, the gate step (unknown, repeated, log-target and other-day slots), the legacy derivation and the log metadata.
2. Evaluation as in scope 4; record noise as noise, per the server README.
3. Dev deploy and smoke; request ids recorded.
4. `node tools/check-docs.mjs` passes.

## Results

Planning only.
