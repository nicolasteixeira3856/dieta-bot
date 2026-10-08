# Plan — S31 New meal on an eaten slot asks instead of failing (hotfix)

- Status: Concluído (08/10/2026)
- Date: 08/10/2026
- Owning context: `server`
- Executable boundary: `server/` only: `meal_changes.prepare_change`, the dev-log error record in `main.py`, tests. No prompt, schema or route change.
- Related documentation: [server Chat specification](../../specifications/v1-chat.md) rule 5e, [HTTP contract](../../../api-contract.md), [ADR-015](../../adrs/ADR-015-log-conversa-dev.md).
- Prerequisites: none.

Micro-plan written and approved in the same owner message (08/10/2026): "Escreve um microplano para atacar isso, já execute logo em sequência, faça merge para a master e dê deploy num hotfix. Não autorizo testes no servidor e nem testes no emulador."

## Objective

A tester logged "Lanche da tarde: 1 iogurte nuv e 2 bananas pequenas" twice and got `nao deu pra estimar` both times (dev log, requests `18c2dd72` and `0c0bda90`, 08/10/2026). The model returned a complete estimate with `suggested_slot` = the afternoon snack, already eaten (150 g of papaya), and `meal_change.operation: new`. `prepare_change` raised `ValueError("new meal cannot overwrite occupied target")`, which became the generic fallback `error`; the log kept only `ValueError`, so the cause took a manual replay to find.

## Scope

1. `prepare_change`: `new` with no base aimed at an eaten target drops the estimate and the change and sets the reply to `{slot} de hoje já tem registro: {text} ({kcal} kcal). Somo a esse registro ou substituo?` (text collapsed to 60 characters with `…`; text or kcal left out when unusable). The existing unresolved-operation path of `shape_chat_turn` then asks it as the top-level question (`clarify: asked`, record `none`, empty memory lists) or, with no round left or `force_estimate`, states its fixed sentence. The server never picks add or revise; the next turn carries the answer.
2. Dev log: `_error_record` adds `reason` for a plain `ValueError` whose single message matches `[a-z][a-z0-9 _]{0,79}` (the server's fixed check strings); library messages with quotes or colons, subclasses (`JSONDecodeError`, pydantic) and other types keep type and status only (CP2). The container warning line carries the same reason.

### Specification changes at Completion

- [v1-chat](../../specifications/v1-chat.md) rule 5e: the question for `new` on an eaten target; Provenance line.
- [HTTP contract](../../../api-contract.md): `reason` in the dev conversation log.

## Out of scope

- Prompt or schema changes so the model picks `add`/`revise` itself; item quality (the duplicated banana lines of the incident); client; production (blocked by the [production gate](../../../content-policy/production-gate.md)).

## Validation

1. `pytest server/tests -q` passes, including `server/tests/test_s31_new_on_occupied.py`.
2. No evaluation run (server code only, per the [server README](../../README.md#chat-evaluation) table).
3. Hotfix dev deploy with `tools/deploy-gcp.ps1`. The owner did not authorize tests on the server or the emulator: no smoke request.
4. `node tools/check-docs.mjs` passes.

## Results

- Unit tests: `server/.venv/Scripts/python -m pytest server/tests -q` → 523 passed, 496 subtests passed. New file `test_s31_new_on_occupied.py` (7): the incident shape asks with the slot, text and kcal; no round left states the fixed sentence with no question; long and missing base text never fail; empty, skipped and planned slots still record `new`; the error record keeps only fixed reasons. The existing `test_bad_metadata_fails_without_record_or_memory` (`new` on the eaten slot, forced) still holds: no estimate, change, record or memory.
- Evaluation: not run (server code only).
- Smoke on the dev server and emulator: not run, by owner decision. The first real turn of this shape after the deploy is the evidence; check its `clarify` and `reply` in the dev log.
