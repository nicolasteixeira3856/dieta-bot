# Specification — Safety correlation and audit

## Status

Proposed, not deployed. Implements the owner's requested direction through [CP3](../plans/cp3-server-safety-audit.md), [CP4](../plans/cp4-android-installation-identity.md) and [CP5](../plans/cp5-gcp-trusted-ingress.md). Architecture: [ADR-025](../adrs/ADR-025-safety-correlation-audit.md). Operational decisions: [CP1](../plans/cp1-policy-and-incident-procedure.md).

## Installation signal

- Android creates a cryptographically random UUID v4 once, atomically, in private no-backup storage. No advertising ID, Android ID, IMEI, CPF, email, Firebase ID or fingerprint.
- Proposed header: `X-Client-Instance-Id`. Stable across process restarts, app updates and the existing wipe-today operation; reinstall/clear-data creates another ID. App flavors have separate storage. Do not repurpose the daily wipe into an identity reset or create a new UI.
- Only send to the configured Dieta Bot API origin. Prevent header propagation on cross-origin redirects or unrelated HTTP clients.
- The server validates canonical UUID shape and length before use. Missing header is supported for existing APKs. Invalid header returns 400 `invalid_client_instance_id`; never reflect its value.
- The value is untrusted and spoofable. No authorization or permanent ban relies on it. It is not a verified user account.

## OpenAI parameter

Derive `safety_identifier = "v1_" + HMAC-SHA256(server_secret, server_environment + ":installation:" + uuid).hexdigest()` on the server. The fixed prefix/version and ASCII hex value are short and bounded. Use a dedicated secret, never the invite or API key; provision securely and preserve across deploys. Environment is server configuration, not a client header.

Pass the derived value separately on every Responses call, including scope, output checks and compaction. Do not put it in the prompt or send the raw IP/UUID to OpenAI. Only pass it to other endpoints if their actual documented schema supports it; no invented moderation parameter. Test the installed SDK's serialized request.

For a missing legacy ID, derive a per-request pseudonym using a server-generated audit event UUID, with a distinct `legacy-request` namespace. Do not use IP as a fallback person identifier or one shared value for every tester. This intentionally loses cross-request attribution for old clients; report legacy coverage. If the secret is missing with safety correlation enabled, stop startup/requests safely; never fall back to a raw identifier.

The provider's request ID, when available, is recorded separately from the app's request ID. Rotation of the HMAC key has a versioned operational procedure and correlation consequences; never rotate on restart. No promise of OpenAI notifications, automatic blocking or immunity is made.

## IP provenance

- Capture only the canonical observed client address after a verified trusted proxy boundary. Never accept an IP in the JSON body or a naked forwarding header as evidence.
- `ip_source` is one of `trusted_edge`, `direct_peer`, `unverified`. An unverified peer is never labelled as the user's address.
- CP5 establishes the actual Internet → Caddy → API path, strips/rebuilds relevant headers and restricts Uvicorn trust to that proxy. No wildcard trust and no publicly published API port.
- Preserve full IPv4/IPv6 only in the restricted security journal when enabled under the CP1 schedule. Do not put IPs into prompts, Firebase, general stdout or user responses.
- Precise timezone-aware server timestamps, event IDs and trustworthy address provenance improve correlation. NAT, VPNs and missing source-port/provider records limit attribution; never infer a person from an IP alone.

## Security journal

Structured, bounded append-only application events in a private file, separate from conversation logs. Append-only here describes writer behavior, not tamper-proof storage; host administrators can modify files. No new cloud service or database is required.

Allowlisted fields: `schema_version`, `policy_version`, server-generated `event_id`, `ts_utc`, `request_id` (client correlation only), route enum, HTTP status, outcome enum, `identity_source`, derived `safety_identifier`, restricted `client_ip`, `ip_source`, `has_photo`, actual received/validated byte counts, moderation model/category booleans and applicable modalities, checked stage, provider request IDs, bounded latency/token counts, retention class and sanitized error code. Missing facts are null, never inferred.

Never store raw UUID, invite, key, arbitrary headers, query strings, full URL, request/response body, base64, image, prompt, transcript, profile, memory or provider error text. Denormalized model text is not a reason code. Do not hash content as an unofficial illegal-content database.

Record ingress rejection (401/413/422/429), policy block, provider failure, allowed completion and audit failure where technically observable; pre-parse rejects have no content classification. Use a server-generated event ID to prevent client `X-Request-Id` collisions from masquerading as unique evidence. Log every outbound provider call's linkage without logging content.

## Retention and failure behavior

- CP1 proposes and records an explicit schedule by data class, operating regime, purpose, legal basis, access and deletion trigger. No arbitrary duration becomes a claimed statutory rule.
- Implementation requires explicit configured durations; an absent/invalid schedule prevents enabling persistent audit. Tests use synthetic short periods. No unbounded retention default.
- Age-based cleanup must run even at low traffic and after downtime; also bound file/disk growth. Capacity exhaustion must alert and stop accepting new model work when required records cannot be written; do not silently overwrite records under a legal hold/minimum retention duty.
- When required security auditing fails, return a fixed 503 before external generation. A terminal write failure after a call produces a sanitized operational alert and fails the response closed; never claim the missing event was recorded. `/health` can report degraded status without personal data.
- Routine raw conversation logging becomes disabled by default, including when the old path is configured. An implementation must not accidentally leave ADR-015 body capture active through an environment setting. Do not enable diagnostic content capture in this cut.
- Existing log copies, rotated files, Docker/Caddy logs, workstation downloads and backups must be inventoried. Cleanup only follows the approved CP1 procedure and any incident hold; no blanket deletion during planning/deploy.
- Security records are not a substitute for any legally required content preservation. This cut does not implement a media evidence vault. Credible incidents invoke the CP1 procedure; unresolved capability/legal gaps block public-readiness claims in CP6.

## Operational controls

Retain IP-based rate limiting, add a separate installation-derived limit, and bound aggregate model concurrency so changing UUIDs does not reset all limits. No automatic permanent banning based on model flags. An owner-operated, expiring pseudonym denylist can return 403 `access_restricted`; entries require a reason code and expiration, with no content in the list. Reinstall, spoofing and NAT limitations are explicit. These are containment controls, not identity verification.

Proposed initial defaults for this single-VM dev cut: preserve the existing 30 requests/minute per-route IP limit; add 30/minute per installation across model routes and a global 60/minute across them; allow at most two concurrent policy pipelines with no unbounded waiting queue. Invalid invites must not create unlimited rate-limit bucket keys. These are abuse/resource limits, not a currency spending guarantee; CP1 records an owner-selected daily provider-call ceiling, CP3 enforces it across process restarts, and CP5 activates it. Keep `/health` outside model quotas. No increase in limits is implied by a newer APK or an absent installation header.
