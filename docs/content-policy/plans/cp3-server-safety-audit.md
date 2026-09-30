# CP3 — Server safety identifier and restricted audit

- Status: Aguardando aprovação
- Date: 2026-09-30
- Owner: `content-policy`; executable owner: server.
- Delivery boundary: `server/`, plus related documentation/indexes.
- Prerequisites: CP2 implemented with automated checks passing; CP1 schedule/procedure reviewed for the intended dev activation. Approval accepts proposed ADR-025.

## Objective and authorization

Correlate requests/provider calls and policy decisions with a pseudonymous installation signal and trustworthy network metadata. Approval names this plan. It does not authorize an APK change or GCP activation.

## Sources

[Identity/audit](../specifications/identity-and-audit.md), [ADR-025](../adrs/ADR-025-safety-correlation-audit.md), [CP2](cp2-server-content-controls.md), [sources](../sources.md).

## Implementation

1. Validate optional `X-Client-Instance-Id`, derive a versioned HMAC using a dedicated server secret and server environment, and propagate the safety identifier to every Responses call introduced/existing in CP2. No raw IP/UUID in provider parameters or prompts. Maintain explicit non-stable legacy fallback. Validate SDK serialization using fake transport.
2. Add a unique server-generated audit event ID; retain `X-Request-Id` as untrusted client correlation for compatibility. Capture provider request IDs separately. Duplicate client IDs never overwrite or merge events.
3. Add a bounded structured security journal with exactly the specification's allowlist. Record stage/outcome, modality/category coverage and provenance. Handle pre-body failures without reading/logging content; audit failure records cannot recursively call the broken writer.
4. Capture IP through a provenance adapter. Default to unverified/direct-peer semantics until CP5 certifies the proxy path. Do not parse arbitrary forwarded headers inside the app or label a proxy's address as the end user's. Canonicalize IPv4/IPv6; store full address only in the explicitly enabled restricted journal.
5. Enforce configured age retention, scheduled cleanup independent of request volume, bounded disk use, access permissions and required-audit failure behavior. Implement narrowly scoped cleanup within the journal directory, no symlink traversal and no deletion of held evidence or unrelated logs. Test restart/downtime and expiration. Historical ADR-015 logs remain for CP1/CP5 disposition.
6. Disable routine raw conversation logging even with the previous `CONVERSATION_LOG_PATH` configured. Keep minimal counters/error codes; no diagnostic opt-in for content in this cut. Ensure stdout, validation errors and provider exceptions cannot leak bodies or secrets.
7. Preserve IP rate limits and add an independent installation bucket; enforce aggregate bounded concurrency and upstream usage caps with explicit defaults documented in config. Rotating/missing UUIDs must not reset IP/global limits. Capacity exhaustion is a recoverable failure, not a moderation bypass.
   Use the specification's proposed defaults and the CP1 daily provider-call ceiling. Reserve/increment the daily counter atomically before each outbound call; persist only aggregate counters across restarts in the restricted server state directory. No per-user/profile/day-state database is introduced. Corrupt/unavailable quota state fails closed; test concurrency, midnight in America/Sao_Paulo and restart without budget reset.
8. Add an owner-operated expiring pseudonym denylist from restricted local configuration, with no public admin endpoint. It returns 403 without model calls. No automatic permanent ban from a classifier score; document false-positive removal and spoofing limitations.

## Files and contract

`server/main.py`, `llm.py`, `config.py`, `conversation_log.py`, policy modules from CP2, new audit/identity modules, tests. Update [API contract](../../api-contract.md) with optional header, invalid-header 400, restricted-access 403 and unavailable audit 503. Preserve legacy responses and existing authentication. No Room, account registry or Firebase additions.

## Validation

- Existing server suite plus fake-transport tests: stable ID across requests and all Responses stages; environment/key-version separation; different installs; missing/invalid header; compact; no raw identifiers in provider payload; no unsupported moderation parameter.
- Temporary-directory/fake-clock tests: retention at boundary, cleanup without new requests, restart, full disk/write failure, hold exclusion, permission error, no path traversal/symlink deletion. Test terminal logging failure after provider completion.
- Malicious `X-Forwarded-For`, IPv6, malformed headers, repeated request IDs, rotating UUIDs, no UUID, quota exhaustion and denylist expiry. CP5 remains responsible for actual proxy evidence.
- Search generated fixture logs and serialized requests for sentinel body/key/invite/UUID values; none may appear outside the explicitly allowed test boundary. Do not test with production personal data.

## Acceptance and completion

Implementer records executed commands/results and configured operational dependencies. No claim of identifying a person, tamper-proof evidence or automatic OpenAI enforcement. Manual end-to-end correlation stays pending until CP4/CP5. The plan moves to `Pendente aprovação manual` after implementation/checks and normal SDD Git delivery; CP5 can consume that state once automated acceptance is documented.

Rollback disables model work if mandatory audit is broken; it never restores raw logs or wildcard IP trust. No live secret creation, legacy-log deletion or deployment in this plan.
