# Specification — Safety correlation and audit

## Status

Server side live on dev since 2026-10-01 ([CP3](../plans/completed/cp3-server-safety-identifier.md), `safety_id: on` since CP5 provisioned the secret on 2026-10-01); Android header in dev 0.0.6, distributed 2026-10-01 ([CP4](../plans/completed/cp4-android-installation-identity.md)); GCP activation done 2026-10-01 ([CP5](../plans/completed/cp5-gcp-dev-ingress.md), `Concluído`). Architecture: [ADR-025](../adrs/ADR-025-safety-correlation-audit.md) (accepted). Closed-test profile delivered by [CP3](../plans/completed/cp3-server-safety-identifier.md), [CP4](../plans/completed/cp4-android-installation-identity.md) and [CP5](../plans/completed/cp5-gcp-dev-ingress.md). Production profile deferred to [CP9](../plans/out_of_scope/cp9-production-audit-and-containment.md) and enforced by the [production gate](../production-gate.md).

## Closed-test profile

### Installation signal

- Android creates a cryptographically random UUID v4 once, atomically, in private no-backup storage. No advertising ID, Android ID, IMEI, CPF, email, Firebase ID or fingerprint.
- Header `X-Client-Instance-Id`. Stable across restarts, updates and the daily wipe; reinstall or clear-data creates another. Flavors have separate storage. No new UI.
- Sent only to the configured Dieta Bot API origin; never propagated on cross-origin redirects or other HTTP clients.
- The server validates canonical UUID shape and length. Missing header is accepted (old APKs). Invalid header returns 400 `invalid_client_instance_id`; the value is never reflected.
- The value is untrusted and spoofable. It authorizes nothing and is not an account.

### OpenAI `safety_identifier`

`safety_identifier = "v1_" + HMAC-SHA256(SAFETY_ID_SECRET, SERVER_ENV + ":installation:" + uuid).hexdigest()`, derived on the server.

- Dedicated secret, never the invite or API key; preserved across deploys; never rotated on restart.
- Passed on every Responses call, including compaction. Never in the prompt. Never the raw UUID or IP. Not sent to endpoints whose documented schema lacks it (moderation).
- Missing header: no `safety_identifier` on that request. No IP fallback, no shared value for all testers.
- Missing secret: the server starts, omits the identifier and `/health` reports `safety_id: off`. Never fall back to a raw value. (The production profile makes the secret mandatory.)
- Test the installed SDK's serialized request with a fake transport.

### Dev log correlation

The ADR-015 conversation log record gains the derived `safety_identifier` (never the raw UUID) so the owner can tell installations apart. No other new field.

### IP provenance

- Caddy v2 without `trusted_proxies` replaces client-sent `X-Forwarded-For` with the address it observes. The API port is not published. CP5 verifies this with a spoofing test and restricts Uvicorn `--forwarded-allow-ips` to the compose network instead of `*`.
- The rate-limit key keeps using that address. No IP is added to prompts, Firebase or user responses. IPs stay out of the conversation log body.
- An IP can be a VPN, carrier NAT or shared connection. Never infer a person from it.

### Containment

- Keep the per-route IP+invite rate limits.
- Primary abuse control in the closed test: the invite. The APK carries it, so a leaked APK leaks access. CP5 documents an invite rotation procedure (new value in `.env`, new dev build through A16, old value rejected).
- Spend ceiling: the OpenAI project budget limit set by the owner in the OpenAI dashboard. No in-app quota counter in this profile.

## Production profile (deferred)

Required before production by [CP9](../plans/out_of_scope/cp9-production-audit-and-containment.md). Preserved from the original 2026-09-30 proposal.

### Security journal

Structured, bounded, append-only application events in a private file, separate from conversation logs. Append-only describes writer behavior, not tamper-proof storage.

Allowlisted fields: `schema_version`, `policy_version`, server-generated `event_id`, `ts_utc`, `request_id` (client correlation only), route enum, HTTP status, outcome enum, `identity_source`, derived `safety_identifier`, restricted `client_ip`, `ip_source` (`trusted_edge | direct_peer | unverified`), `has_photo`, received/validated byte counts, moderation model/category booleans and modalities, checked stage, provider request IDs, bounded latency/token counts, retention class, sanitized error code. Missing facts are null.

Never stored: raw UUID, invite, key, arbitrary headers, query strings, full URL, request/response body, base64, image, prompt, transcript, profile, memory or provider error text. No content hashing as an unofficial illegal-content database.

Record ingress rejections (401/413/422/429), policy blocks, provider failures, allowed completions and audit failures. Server event IDs prevent client `X-Request-Id` collisions from masquerading as unique evidence.

### Retention and failure

- Explicit schedule per data class, purpose, legal basis, access and deletion trigger (from [CP8](../plans/out_of_scope/cp8-public-legal-pack.md)). No unbounded default; an absent schedule prevents enabling the journal.
- Cleanup runs at low traffic and after downtime; disk growth is bounded; records under legal hold are never overwritten.
- Required audit failing: fixed 503 before external generation; a terminal write failure after a call fails the response closed.
- Routine raw conversation capture (ADR-015) off in production, with no environment switch left that re-enables it.
- Historical logs, rotated files, Docker/Caddy logs, workstation downloads and backups are inventoried and disposed of only under the approved procedure and any hold.

### Containment

- Missing `SAFETY_ID_SECRET` with correlation enabled stops startup.
- Missing installation header: per-request pseudonym from a server event UUID in a distinct `legacy-request` namespace; legacy coverage reported.
- Independent per-installation limit plus global model concurrency cap, so rotating UUIDs does not reset limits. Initial proposal: 30/min per IP per route (existing), 30/min per installation across model routes, 60/min global, at most two concurrent pipelines, no unbounded queue.
- In-app daily provider-call ceiling persisted across restarts, America/Sao_Paulo midnight, fail closed on corrupt state.
- Owner-operated expiring pseudonym denylist (403 `access_restricted`), reason code and expiration, no content, no public admin endpoint. No automatic permanent ban from a classifier score.

Security records do not replace legally required content preservation. This profile does not implement a media evidence vault.
