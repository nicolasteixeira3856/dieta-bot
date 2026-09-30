# ADR-025 — Pseudonymous safety correlation and minimal audit

- Status: Proposed; acceptance requires approval of [CP3](../plans/cp3-server-safety-audit.md).
- Date: 2026-09-30
- Owner: `content-policy`
- Partially supersedes on acceptance: [ADR-015](../../server/adrs/ADR-015-log-conversa-dev.md), routine raw conversation capture and size-only retention. Preserve that accepted ADR as history.
- Complements: [ADR-014](../../android/adrs/ADR-014-flavors-firebase-dev.md); no identity/IP is added to Firebase.

## Context

The owner proposed safety identifiers and IP logging. The app has no verified accounts and shares an invite. Current dev logs retain user/model text but do not form a minimal, trustworthy incident audit.

## Proposed decision

Follow [identity and audit](../specifications/identity-and-audit.md): private per-installation random UUID, server HMAC pseudonym, OpenAI `safety_identifier` on each Responses call, server event IDs and a dedicated restricted security journal. Capture a client IP only with known provenance. Keep existing clients working with explicitly non-stable legacy correlation.

Disable routine raw conversation capture; enforce an explicit retention schedule, access controls and bounded resource use. CP1 establishes an incident procedure; statutory access records and legally preserved evidence are separate from routine diagnostics. Activation of trusted raw-IP logging is part of CP5, after its operational prerequisites.

## Consequences

- Correlates requests while avoiding direct identifiers in model input and provider safety fields.
- Installation identity is resettable/spoofable; IP is shared/changeable. This does not identify a natural person or replace an account system.
- Loses convenient raw-conversation debugging. Use synthetic reproductions and aggregate outcomes instead.
- Introduces secret provisioning, retention/hold decisions and failure handling; rollout requires verified proxy behavior.
- App daily wipe preserves installation identity. Reinstall/clear-data resets it. No cross-app tracking or anti-uninstall persistence.

## Alternatives

- Raw IP/email as provider safety identifier: unnecessary disclosure and weak identity semantics.
- One identifier per shared invite: conflates all testers.
- Verified accounts or device attestation: separate future business/architecture decision, not silently introduced.
- Keep full chat logs indefinitely: disproportionate retention and additional exposure.

Accepted ADRs remain immutable. Acceptance of this proposal does not authorize implementation outside the named plan's folder.
