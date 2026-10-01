# CP3 — Server safety identifier

- Status: Aguardando aprovação
- Date: 2026-09-30 (rescoped 2026-09-30 for the closed test)
- Owner: `content-policy`; executable owner: server.
- Delivery boundary: `server/`, plus related documentation/indexes.
- Prerequisites: [CP2](pending_manual_validation/cp2-server-content-controls.md) delivered. Approval accepts proposed [ADR-025](../adrs/ADR-025-safety-correlation-audit.md).

## History

Originally "Safety identifier and restricted audit", including a security journal, retention and legal hold, raw-log retirement, in-app quotas and a denylist. On 2026-09-30 the owner deferred those to [CP9](out_of_scope/cp9-production-audit-and-containment.md) because the app is in a closed test.

## Objective and authorization

Correlate requests per installation with OpenAI's `safety_identifier`, without sending raw identifiers. Approval names this plan. No APK change, no GCP change.

## Sources

[Identity and audit, closed-test profile](../specifications/identity-and-audit.md#closed-test-profile), [ADR-025](../adrs/ADR-025-safety-correlation-audit.md), [sources](../sources.md).

## Implementation

1. Accept optional `X-Client-Instance-Id`; validate canonical UUID v4; invalid → 400 `invalid_client_instance_id`, value never reflected or logged.
2. Derive `safety_identifier` with HMAC-SHA256 from `SAFETY_ID_SECRET` and `SERVER_ENV` (new config). Pass it on every Responses call, including compaction. Not in the prompt; not on the moderation call.
3. Missing header → no identifier. Missing secret → start, omit the identifier, `/health` reports `safety_id: off`.
4. Add the derived identifier (never the raw UUID) to the ADR-015 log record.
5. Update the [API contract](../../api-contract.md) with the optional header and the 400.

## Files

`server/main.py`, `llm.py`, `config.py`, `conversation_log.py`, a small identity module, tests.

## Validation

- Existing suite plus fake-transport tests: same ID → same identifier across all Responses calls; different IDs differ; environment separation; missing/invalid header; missing secret; serialized request contains no raw UUID or IP; moderation request has no `safety_identifier`.
- Sentinel search: raw UUID, invite and key never appear in log fixtures or serialized requests.

## Rollout and completion

Deploy code with `tools/deploy-gcp.ps1`. Until CP5 provisions the secret, `/health` shows `safety_id: off`; that is expected. Implementation and tests done → `Pendente aprovação manual`; end-to-end correlation is verified in CP5 with the CP4 APK. Apply [SDD](../../sdd/README.md) Git delivery.
