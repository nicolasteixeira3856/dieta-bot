# ADR-025 — Pseudonymous safety correlation

- Status: Proposed; acceptance requires approval of [CP3](../plans/cp3-server-safety-identifier.md).
- Date: 2026-09-30 (revised 2026-09-30 for the closed-test cut)
- Owner: `content-policy`
- Complements: [ADR-015](../../server/adrs/ADR-015-log-conversa-dev.md) (dev conversation log stays in the closed test) and [ADR-014](../../android/adrs/ADR-014-flavors-firebase-dev.md) (no identity or IP goes to Firebase).
- Successor needed before production: the minimal security journal and the retirement of ADR-015 raw capture belong to [CP9](../plans/out_of_scope/cp9-production-audit-and-containment.md), which must propose its own ADR.

## Context

The owner asked for safety identifiers and IP logging. The app has no accounts and one shared invite. Two testers use it through Firebase App Distribution. OpenAI recommends a stable, privacy-preserving `safety_identifier`.

## Decision

Closed-test profile of [identity and audit](../specifications/identity-and-audit.md):

- Private per-installation random UUID on Android, sent as `X-Client-Instance-Id` to the API origin only.
- Server-side HMAC pseudonym passed as `safety_identifier` on every Responses call and written to the dev log instead of the raw UUID.
- IP provenance from Caddy; Uvicorn forwarded-header trust narrowed to the compose network.
- Containment by invite rotation and the OpenAI project budget limit.

## Consequences

- Requests correlate per installation without direct identifiers reaching the provider.
- Installation identity is resettable and spoofable; IP is shared and changeable. Neither identifies a person.
- Introduces one secret to provision and preserve.
- Full audit, retention schedule, denylist and in-app quotas are not delivered; they block production through the [production gate](../production-gate.md).

## Alternatives

- Raw IP or email as `safety_identifier`: unnecessary disclosure.
- One identifier per shared invite: conflates testers.
- Full security journal now: disproportionate for a two-tester closed test; deferred to CP9.
- Verified accounts or device attestation: separate future decision.

Accepted ADRs remain immutable. Acceptance does not authorize implementation outside the named plan's folder.
