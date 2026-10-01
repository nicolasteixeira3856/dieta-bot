# CP3 — Server safety identifier

- Status: Concluído (manual approval by the owner 2026-10-01: "Os planos estão todos aprovados, pode completar tudo.")
- Approved: 2026-10-01 by the owner: "Aprovo o plano docs\content-policy\plans\cp3-server-safety-identifier.md. Analise e implemente o plano aprovado."
- Date: 2026-09-30 (rescoped 2026-09-30 for the closed test)
- Owner: `content-policy`; executable owner: server.
- Delivery boundary: `server/`, plus related documentation/indexes (and the `.env.example` template).
- Prerequisites: [CP2](cp2-server-content-controls.md) delivered. Approval accepts proposed [ADR-025](../../adrs/ADR-025-safety-correlation-audit.md).

## History

Originally "Safety identifier and restricted audit", including a security journal, retention and legal hold, raw-log retirement, in-app quotas and a denylist. On 2026-09-30 the owner deferred those to [CP9](../out_of_scope/cp9-production-audit-and-containment.md) because the app is in a closed test.

## Objective and authorization

Correlate requests per installation with OpenAI's `safety_identifier`, without sending raw identifiers. Approval names this plan. No APK change, no GCP change.

## Sources

[Identity and audit, closed-test profile](../../specifications/identity-and-audit.md#closed-test-profile), [ADR-025](../../adrs/ADR-025-safety-correlation-audit.md), [sources](../../sources.md).

## Implementation

1. Accept optional `X-Client-Instance-Id`; validate canonical UUID v4; invalid → 400 `invalid_client_instance_id`, value never reflected or logged.
2. Derive `safety_identifier` with HMAC-SHA256 from `SAFETY_ID_SECRET` and `SERVER_ENV` (new config). Pass it on every Responses call, including compaction. Not in the prompt; not on the moderation call.
3. Missing header → no identifier. Missing secret → start, omit the identifier, `/health` reports `safety_id: off`.
4. Add the derived identifier (never the raw UUID) to the ADR-015 log record.
5. Update the [API contract](../../../api-contract.md) with the optional header and the 400.

## Files

`server/main.py`, `llm.py`, `config.py`, `conversation_log.py`, a small identity module, tests.

## Validation

- Existing suite plus fake-transport tests: same ID → same identifier across all Responses calls; different IDs differ; environment separation; missing/invalid header; missing secret; serialized request contains no raw UUID or IP; moderation request has no `safety_identifier`.
- Sentinel search: raw UUID, invite and key never appear in log fixtures or serialized requests.

## Rollout and completion

Deploy code with `tools/deploy-gcp.ps1`. Until CP5 provisions the secret, `/health` shows `safety_id: off`; that is expected. Implementation and tests done → `Pendente aprovação manual`; end-to-end correlation is verified in CP5 with the CP4 APK. Apply [SDD](../../../sdd/README.md) Git delivery.

## Results (2026-10-01)

Automation executed (`server/.venv`, `python -m pytest -q`): 180 passed, 135 subtests (163 before CP3, plus 17 new in `tests/test_safety_identifier.py`). The `/health` test now expects `safety_id`.

| Check | Result |
| --- | --- |
| HMAC formula `v1_` + HMAC-SHA256(secret, `SERVER_ENV:installation:uuid`) | PASS — unit test against an independent `hmac` computation |
| Same ID → same identifier on chat, compact, estimate and fit | PASS — fake transport, serialized SDK body (openai 3.19.1) |
| Different IDs differ; `SERVER_ENV` and secret separate identifiers | PASS — unit and route tests (`SERVER_ENV=prod`) |
| Missing header → no `safety_identifier`, log `null`, 200 | PASS |
| Invalid header (empty, uppercase, braces, no hyphens, v1, wrong variant, URN, extra char, non-ASCII digit) → 400 `invalid_client_instance_id` | PASS — no moderation or model call, no log line, value not in body or headers; still 400 with the secret off; `X-Invite` checked first (401) |
| Missing secret → server starts, no identifier, `/health` `safety_id: off` | PASS |
| Moderation request has no `safety_identifier` | PASS |
| Identifier is a request field only, not in instructions or input | PASS |
| Sentinels (raw UUIDs, invite, API key, secret, client IP) absent from serialized requests and the conversation log | PASS |
| Log record carries the derived identifier, never the raw UUID | PASS |

Implementation notes:

- [`server/identity.py`](../../../../server/identity.py) holds the only copy of the raw value; routes drop it after derivation. Canonical shape is lowercase (`java.util.UUID.toString()`); uppercase is rejected, not normalized, so one installation cannot map to two pseudonyms.
- `SERVER_ENV` defaults to `dev` when unset (the closed test is dev only). `SAFETY_ID_SECRET` is also redacted from the conversation log.
- Old APKs send no header and keep working. Android ignores the new `/health` key (`ignoreUnknownKeys`).

Rollout: deployed to dev with `tools/deploy-gcp.ps1` (code only, `.env` untouched) on 2026-10-01. Smoke: `/health` → 200 `{"ok": true, "model": "gpt-6-luna", "safety_id": "off"}` (expected until CP5 provisions the secret); `POST /v1/estimate` with a valid invite and `X-Client-Instance-Id: not-a-uuid` → 400 `{"detail":"invalid_client_instance_id"}`, and an uppercase UUID → 400. No model call was made in the smoke.

Manual pending: end-to-end correlation (same installation → same identifier in the dev log, different installations differ) with the CP4 APK and the CP5 secret, as V11/V16 in the [validation matrix](../../validation/README.md). Not run here: live model calls with an identifier (secret not provisioned; CP5 scope).

## Manual approval (2026-10-01)

End-to-end correlation verified in CP5 with the secret active and the dev 0.0.6 APK on an emulator: one installation kept the same `safety_identifier` across an app restart and an `api` container restart; a reinstall produced a different one; identifiers present on every chat turn in the dev log. Evidence: [CP5 results](cp5-gcp-dev-ingress.md#results-2026-10-01), V11/V12/V16. The owner then approved the plan.
