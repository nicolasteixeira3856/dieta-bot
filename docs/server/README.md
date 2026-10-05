# server

## Purpose and ownership

Dieta Bot's HTTP API estimates meals and portions that fit the supplied budget. It neither computes the ceiling nor stores the day. Code owner: `server/`; consumer: [Android](../android/README.md). Infrastructure: `server/docker-compose.yml`, `infra/`, `infra/gcp/` and the [deployment runbook](deploy-gcp.md).

## Scope and boundaries

- Routes, authentication, model configuration, timeout and payload/photo limits: [HTTP contract](../api-contract.md) and [Chat specification](specifications/v1-chat.md).
- The client owns ceiling/eat-back calculations, Room, push and UI. No server account, payment or day persistence.
- Host: [ADR-013](adrs/ADR-013-gcp-host.md); deployment: `tools/deploy-gcp.ps1`. Public distribution remains subject to the [production gate](../content-policy/production-gate.md).
- Optional dev conversation logging and request correlation: [ADR-015](adrs/ADR-015-log-conversa-dev.md), read through `tools/pull-conversations.ps1`.
- User-supplied content and safety correlation: [content-policy](../content-policy/README.md).
- Repository architecture and identifiers: [001](../decisions/001-monorepo.md), [007](../decisions/007-english-identifiers.md).

## Reading order

Follow [SDD](../sdd/README.md): [matrix](../README.md), this README, [HTTP contract](../api-contract.md), [Chat specification](specifications/v1-chat.md), cited ADR and named active plan, then `server/main.py`, `llm.py`, `shaping.py`, `config.py`.

## Chat evaluation

Run locally, never on the server. The key is loaded from the repository `.env` and never printed.

```bash
cd server
.venv/Scripts/python -m evals.run --effort none --repeat 3
.venv/Scripts/python -m evals.run --effort none --repeat 6 --only cafe-resposta-leite,cabe-acai
.venv/Scripts/python -m evals.run --tag temp
```

- Cases: `server/evals/cases/<id>.json`, with id, since (`v1`/`v2`/`v3`/`v4`/`v5`/`cp2`), tags, request (`ChatIn` body), expect, optional required/strict/image. Since is metadata only: v3 sends clarify_rounds, v4 also auto_record, v5 also temp_facts and structured facts.
- Normal cases call the route's `chat_reply`; compact requests call its `compact_reply`. Both include generation, shaping, moderation and failure handling, without HTTP. Default: two workers to avoid token-rate bursts with the longer prompt; `--workers` can select up to three.
- Expectations live in `server/evals/checks.py`. Missing versioned fields are n/a, except any expectation named in `required` fails on n/a. Record/skip_slot check the mark; top_question/top_question_not check the top-level question. Digest accepts present/absent; digest_has/digest_not check terms and require non-empty content. A failed or empty compact response cannot pass only a negative digest check.
- `meal_progress` accepts present/absent: a nonempty estimate object or nonblank top-level question counts as progress. `confidence` accepts one value or a list and fails without an estimate. Required positive checks prevent empty/fallback outputs from passing solely through negative question checks.
- A case passes when every applicable expectation passes in at least two of three repetitions. Strict cases require every repetition. Inspect the reported statuses; the CLI writes the report even when cases fail.
- Reports: terminal and `logs/evals/<date-time>-<effort>.json`, outside git, with pass rate, p95 and cost. New cases use manually rewritten situations or synthetic images; never commit raw tester logs/text.

## Index

### Specifications and decisions

- [v1-chat](specifications/v1-chat.md) and [HTTP contract](../api-contract.md).
- [ADR-013](adrs/ADR-013-gcp-host.md) — host.
- [ADR-015](adrs/ADR-015-log-conversa-dev.md) — dev conversation log.
- [ADR-033](../content-policy/adrs/ADR-033-global-chat-example-provenance.md) — global Chat example provenance.
- Each ADR's own status line is authoritative.

### Plans and validation

- Active plans: files directly under [plans/](plans/). Completed evaluation evidence belongs to each originating plan in [history](plans/completed/).
- [S18 — Meal additions and revisions](plans/s18-meal-additions-and-revisions.md).
- [S19 — Generalizable Chat instructions](plans/s19-generalizable-chat-instructions.md).
- Tests: `server/tests/test_api.py`, `test_photo_cap.py`, `test_security.py`, `test_chat.py`, `test_conversation_log.py`, `test_evals.py`, `test_clarify.py`, `test_record.py`, with remaining server tests under the same directory. Coverage includes temp validation/filtering and compatibility, held slots and logging, evaluator required/digest checks, shared compact moderation and prompt rules.
- Run `server/.venv/Scripts/python -m pytest server/tests` from the repository root; documentation changes also require `node tools/check-docs.mjs`.
