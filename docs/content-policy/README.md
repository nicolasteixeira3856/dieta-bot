# Content policy

## Purpose

Keep Dieta Bot within its meal-budgeting purpose, reduce abusive text/image processing, and make incident handling traceable without turning conversation logs into an unrestricted content archive.

## Type and ownership

- Type: cross-cutting policy, authorized by the owner on 2026-09-30.
- Directory name: `content-policy`, explicitly requested by the owner; exception to the default snake_case naming convention.
- Owns policy specifications, proposed architecture decisions, delivery plans and consolidated validation.
- Each implementation plan has exactly one executable ownership boundary: `server/`, `apps/android/`, or `infra/gcp/`. Documentation-only plans own this context. Related specifications/indexes may be updated for that delivery; this does not authorize changing another code boundary.
- [Server](../server/README.md) continues to own HTTP/LLM implementation, [Android](../android/README.md) owns the client, and [product](../produto/README.md) owns screens and meal behavior.

## Current state

Planning only. None of CP1–CP7 is implemented or approved for implementation by this documentation request.

The inspected server has invite authentication, per-route rate limits, size checks and structured Chat output. It has no application moderation pipeline, no OpenAI safety identifier and no dedicated security audit. Dev conversation logging includes input context and output. The GCP configuration trusts forwarded headers with `--forwarded-allow-ips=*`. This is a configuration to verify and narrow, not proof that public header spoofing currently succeeds through Caddy.

A read-only inspection confirmed an off-topic quadratic-equation response on 2026-09-30 at 14:53 America/Sao_Paulo. No user log, IP, identifier or attachment is copied into these documents. This is evidence of scope drift, not evidence of illegal activity.

## Scope and constraints

- Scope enforcement; OpenAI text/image moderation; checking responses and proposed memory/digest changes.
- Pseudonymous installation correlation with `safety_identifier`; trustworthy IP capture and minimal security events.
- Bounded costs, timeouts and logs; tests for bypasses and false positives.
- Reviewable legal/privacy documents and an incident procedure with explicit unresolved obligations.
- Existing OpenAI integration only. Additional specialist detection providers are deferred for financial/business reasons in [CP7](plans/out_of_scope/cp7-specialist-detection.md).
- Preserve photos from day one, the current model/effort, invite access, app screens, Room day state and Firebase boundaries. No new account system, paid moderation vendor, automatic criminal accusation or automatic authority report.

## Boundaries and residual risk

An installation UUID is client-controlled, replaceable and spoofable. An IP can represent a VPN, proxy, carrier NAT or shared connection. Neither proves a person's identity or authorship. `safety_identifier` supports provider correlation, not authentication or guaranteed enforcement. Moderation has false negatives and false positives; this cut cannot claim complete illegal-image or CSAM detection.

Legal review and user-facing publication/acceptance are not accomplished by implementing server code. CP1 prepares the material; CP6 records rollout readiness. Publishing legal documents, adding acceptance UI and opening public registration require their own approved scope and a Stitch gate if layout changes.

## Reading order

1. [SDD](../sdd/README.md), [AGENTS](../../AGENTS.md), and this context.
2. [Policy](specifications/content-policy.md), [identity and audit](specifications/identity-and-audit.md), and [sources](sources.md).
3. [ADR-024](adrs/ADR-024-content-safety-boundaries.md) and [ADR-025](adrs/ADR-025-safety-correlation-audit.md), both proposed.
4. [Plan order](plans/README.md) and [validation matrix](validation/README.md).
5. Current implementation in the owning folder before each approved delivery.

## Plan order

CP1 → CP2 → CP3 → CP4 → CP5 → CP6. CP7 remains `Fora de escopo` and is not runnable. See the [plan index](plans/README.md) for approval commands, prerequisites and completion gates.

## Decision status

Accepted owner constraints: create this context; plan economical prevention/correlation; defer additional detection platforms for now; add the SDD `Fora de escopo` state and `out_of_scope/` directory.

Proposed implementation choices, including retention defaults, transport headers and classification behavior, are reviewable in the specifications and plans. They are not described as already deployed. Accept the relevant proposed ADR when approving its named implementation plan; preserve that acceptance date and scope.
