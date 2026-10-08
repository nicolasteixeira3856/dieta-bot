# server

## Purpose and ownership

Fibrai's HTTP API estimates meals and portions that fit the supplied budget. It neither computes the ceiling nor stores the day. Code owner: `server/`; consumer: [Android](../android/README.md). Infrastructure: `server/docker-compose.yml`, `infra/`, `infra/gcp/` and the [deployment runbook](deploy-gcp.md).

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
.venv/Scripts/python -m evals.run --tag <plan> --repeat 1                    # the cases a change touches (effort low, ADR-054)
.venv/Scripts/python -m evals.run --repeat 1                                 # the whole suite, once (before a production deploy)
.venv/Scripts/python -m evals.run --tag cp2 --moderation all                 # refusal cases, real moderation
```

How much to run (owner decision, 2026-10-08, [Delivery pace](../../AGENTS.md#delivery-pace-owner-decision-08102026): paid model calls are not part of development):

| Change | Validation | Model calls |
| --- | --- | --- |
| Server code only (route, shaping, log, infra) | unit tests + the three-turn HTTP smoke after the dev deploy | 0 |
| Prompt or schema change, attended or unattended ([autonomous run](../sdd/autonomous-run.md)) | the plan's own cases once (`--tag <plan> --repeat 1`), inside the plan's cap; no sentinel set, no suite, no repeat to settle a flaky case | at most 12 per plan |
| New or changed evaluator check | unit tests for the check; at most the cases that use it, once | inside the same 12 |
| Production deploy (future, `master`) | the whole suite once and the app end to end, before the deploy | once per production deploy |

- There is no running baseline during development: a case that fails is read by its output, fixed as a rule or a fixture, and rerun once. The whole suite runs only before a production deploy.
- A case that passes and fails across runs of the same prompt is noise, not a regression: record it and move on; fix the rule or the fixture when there is time. No ten-repetition tiebreaks and no run on both prompts to settle one case.
- Moderation: by default only CP2 cases (`since: cp2` or tag `cp2`) call the provider's moderation endpoint; every other case gets a local clean verdict (`--moderation cp2`). The endpoint's daily request cap is per project; the evaluator and the dev server use keys from different projects ([runbook](deploy-gcp.md)). `--moderation all` restores the old behavior for a deliberate CP2 review; `--moderation none` is for offline replay only.
- The closed test has two testers. Validation exists to catch a broken rule before they see it, not to prove a rate.

- Cases: `server/evals/cases/<id>.json`, with id, since (`v1`/`v2`/`v3`/`v4`/`v5`/`meal_changes`/`cp2`), tags, request (`ChatIn` body), expect, optional required/strict/image. Since is metadata only: v3 sends clarify_rounds, v4 also auto_record, v5 also temp_facts and structured facts; meal_changes cases exercise the opted-in contract and its legacy comparison branch.
- Normal cases call the route's `chat_reply`; compact requests call its `compact_reply`. Both include generation, shaping, moderation and failure handling, without HTTP. Default: two workers to avoid token-rate bursts with the longer prompt; `--workers` can select up to three.
- Expectations live in `server/evals/checks.py`. Missing versioned fields are n/a, except any expectation named in `required` fails on n/a. Record/skip_slot check the mark; top_question/top_question_not check the top-level question. Digest accepts present/absent; digest_has/digest_not check terms and require non-empty content. A failed or empty compact response cannot pass only a negative digest check.
- `reply_has`/`reply_not` check terms in the reply; `reply_max_chars` requires a nonblank reply on one line within that many characters.
- `meal_progress` accepts present/absent: a nonempty estimate object or nonblank top-level question counts as progress. `confidence` accepts one value or a list and fails without an estimate. Required positive checks prevent empty/fallback outputs from passing solely through negative question checks.
- `meal_change` checks operation/base, delta item presence and exclusions, optional exact portions/nutrients, retained base text, all four consolidated values and numeric reply labels. Null requires no actionable change. S18's independent synthetic cases and fictional label are regression data only, never assembled into global instructions. Run them with `--tag s18 --repeat 6`; each case is strict.
- S22 cases carry the tag `s22`: open questions (`creative`, each with a `note` describing a good answer), one meal text repeated (`consistency`, with `repeat` and the case-level `kcal_spread` expectation: the kcal band across repetitions as a percentage of the median, at most the given value; a case may set `repeat` to run more repetitions than the CLI asks) and the first-message rules (`consistency-rule`). Run them with `--tag s22`. S23 cases (`s23`) check the server total: `estimate_values`/`item_portions` on reference foods, a budget decided by the server total, a question without estimate. The `s28` case (one RECENT day, "o mesmo de ontem", meal-changes branch) is non-strict and documents an open prompt gap: it fails while the model answers that it has no record; a pass means the server returned the copied totals.
- Transfer cases carry the tags `s19`, `s19-pairs` (matched pairs that vary foods, profiles and phrasing around one situation, with `family` and `pair` fields) or `s19-reserved` (kept out of prompt tuning). Run them with `--tag`; a prompt change is compared against the previous prefix on these and on the untagged regression cases.
- A case passes when every applicable expectation passes in at least two of three repetitions (at `--repeat 1`, in that one). Strict cases require every repetition. Inspect the reported statuses; the CLI writes the report even when cases fail.
- Reports: terminal and `logs/evals/<date-time>-<effort>.json`, outside git, with pass rate, p95 and cost. New cases use manually rewritten situations or synthetic images; never commit raw tester logs/text.

### Model pilot

`server/evals/pilot/run.py` runs the same `/v1/chat` turn (same prompt, same schema, no moderation call) against `gpt-6-luna` and against Grok through xAI's OpenAI-compatible Responses API. It compares, side by side: the main-suite cases where Luna failed or flaked (`FAIL_SET`) and the main-suite cases tagged `s22` (open questions for manual review, the repeated meal judged by `kcal_spread`, the first-message rules). `--luna-effort` runs the same cases at another effort for an A/B.

```bash
cd server
.venv/Scripts/python -m evals.pilot.run --provider luna                 # baseline
.venv/Scripts/python -m evals.pilot.run --provider grok                 # needs XAI_API_KEY in the repo-root .env
.venv/Scripts/python -m evals.pilot.run --report-only                   # markdown from the newest run of each provider
```

- Keys: `OPENAI_API_KEY` and `XAI_API_KEY` from the repo-root `.env` (a worktree reads the nearest `.env` up the tree, or `--env <file>`). Never printed, never deployed: the pilot is local only and changes nothing in `config.MODEL`.
- Grok 4.7 cannot disable reasoning; the pilot uses `effort=low` (`--grok-effort`, `--grok-model` to try another model). Its list price is 20x Luna's input and 12x the output; the report projects the cost per 1000 turns.
- Reports: `logs/evals/pilot/<stamp>-<provider>-<effort>.json` and `<stamp>-report.md`, outside git. Switching the model is a product decision (AGENTS § LLM) and needs its own ADR and plan; the Grok comparison of 2026-10-06 is recorded in S22.

## Index

### Specifications and decisions

- [v1-chat](specifications/v1-chat.md) and [HTTP contract](../api-contract.md).
- [ADR-013](adrs/ADR-013-gcp-host.md) — host.
- [ADR-015](adrs/ADR-015-log-conversa-dev.md) — dev conversation log.
- [ADR-042](adrs/ADR-042-estimate-total-is-server-arithmetic.md) — estimate total computed by the server from the items.
- [ADR-054](adrs/ADR-054-chat-reasoning-effort-low.md) — Chat generation at `reasoning.effort=low`.
- [ADR-033](../content-policy/adrs/ADR-033-global-chat-example-provenance.md) — global Chat example provenance.
- Each ADR's own status line is authoritative.

### Plans and validation

- Pending manual validation: [S33](plans/pending_manual_validation/s33-chat-context-effort-low.md) (context in code, effort low; one turn on the dev app); [S34](plans/pending_manual_validation/s34-protein-boost-hybrid.md) (protein boost hybrid; one dinner on the dev app); [S36](plans/pending_manual_validation/s36-typed-actions.md) (typed actions; one two-meal message on the dev app); [S37](plans/pending_manual_validation/s37-plan-options-and-discovery.md) (options and discovery; an open dinner request and "fiz a 2" on the dev app). Active plans, in execution order: [S38](plans/s38-saved-recipes.md) (saved recipes). Benchmark that motivated them: `benchmark/RESULTADOS_08_10_2026.md`. S30 (tone, closures, reply formatting and planned slot) and S35 (workout) are in [history](plans/completed/). Cancelled by owner decision (07/10/2026, one plan per context): S25, S26, S27 in [`plans/cancelled/`](plans/cancelled/). Completed evaluation evidence belongs to each originating plan in [history](plans/completed/).
- Tests: `server/tests/test_api.py`, `test_photo_cap.py`, `test_security.py`, `test_chat.py`, `test_conversation_log.py`, `test_evals.py`, `test_clarify.py`, `test_record.py`, with remaining server tests under the same directory. Coverage includes temp validation/filtering and compatibility, held slots and logging, evaluator required/digest checks, shared compact moderation and prompt rules.
- Run `server/.venv/Scripts/python -m pytest server/tests` from the repository root; documentation changes also require `node tools/check-docs.mjs`.
- Validation covering more than one plan: [validation/](validation/).
