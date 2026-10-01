# Content policy

## Purpose

Single home for every safety concern about user-supplied content reaching the AI: anything a user sends (text, photo, history, memory, profile fields, headers) that could be malicious or harmful. Covers scope drift, prompt injection, memory/digest poisoning, prohibited and illegal content, eating-disorder and self-harm risk, abuse of model cost, correlation of abusive requests, logging of that content, and incident handling.

Keep Dieta Bot within its meal-budgeting purpose without turning conversation logs into an unrestricted content archive.

## Type and ownership

- Type: cross-cutting policy, authorized by the owner on 2026-09-30. The owner requires this folder to stay and to concentrate this topic; do not split it into other contexts or remove it.
- Directory name: `content-policy`, explicitly requested by the owner; exception to the default snake_case naming convention.
- Owns policy specifications, proposed architecture decisions, delivery plans, the production gate and consolidated validation.
- Each implementation plan has exactly one executable boundary: `server/`, `apps/android/` or `infra/gcp/`. Documentation-only plans own this context. Owning specs/indexes may be updated with a delivery; that does not authorize another code boundary.
- [Server](../server/README.md) owns HTTP/LLM implementation, [Android](../android/README.md) owns the client, [product](../produto/README.md) owns screens and meal behavior.

## Current state

Phase: **closed test** (dev flavor, invite, two testers via Firebase App Distribution).

[CP2](plans/pending_manual_validation/cp2-server-content-controls.md) is deployed to the dev server (2026-09-30, owner APK check pending): tightened scope instructions, a required `scope` field with fixed refusal copy, free OpenAI moderation of input and output that fails closed, and an eval set for off-topic, injection and eating-disorder cases. [CP3](plans/pending_manual_validation/cp3-server-safety-identifier.md) is deployed to dev (2026-10-01): optional `X-Client-Instance-Id`, HMAC `safety_identifier` on every Responses call and in the dev log, active on dev since CP5 provisioned the secret (2026-10-01). [CP4](plans/pending_manual_validation/cp4-android-installation-identity.md) is implemented and distributed as dev 0.0.6 (2026-10-01): the APK keeps a random UUID v4 in `noBackupFilesDir` and sends `X-Client-Instance-Id` only to the configured API origin's `v1/` routes, never across a redirect. No dedicated audit (CP9). Dev conversation logging (ADR-015) includes input and output, except metadata-only lines for blocked turns. [CP5](plans/pending_manual_validation/cp5-gcp-dev-ingress.md) is deployed to dev (2026-10-01): Uvicorn trusts forwarded headers only from the fixed compose subnet `172.30.53.0/28`, Caddy without `trusted_proxies` overwrites client `X-Forwarded-For`, Docker logs are bounded (10 MB × 3) and the conversation log rotates at 30 days; the OpenAI project budget limit is the owner's pending manual step.

[CP1](plans/completed/cp1-closed-test-notice.md) documents were delivered on 2026-10-01: [tester notice](legal/tester-notice.pt-BR.md), [data map](operations/closed-test-data-map.md) and [incident note](operations/closed-test-incident.md). The owner delivered the notice to both testers on 2026-10-01; CP1 is `Concluído`.

A read-only inspection confirmed an off-topic quadratic-equation response on 2026-09-30 at 14:53 America/Sao_Paulo. No user log, IP, identifier or attachment is copied here. Evidence of scope drift, not of illegal activity.

## Scope

Closed test (active plans):

- Prompt scope fix, server-enforced `scope` field, fixed refusals, safe text-only fallback.
- Free OpenAI text/image moderation on input and generated output; fail closed.
- Eval sets for off-topic, injection and eating-disorder cases.
- Installation pseudonym and HMAC `safety_identifier`.
- Narrowed proxy trust, bounded log retention, invite rotation, OpenAI project budget limit.
- Tester notice and one-page incident note.

Production (deferred, blocking): public legal pack, security journal with retention and hold, context-wide moderation, in-app quotas and denylist, specialist detection decision, integrated readiness. See the [production gate](production-gate.md).

Constraints: existing OpenAI integration only; photos from day one; current model/effort; invite access; app screens; Room day state; Firebase boundaries. No account system, paid moderation vendor, automatic criminal accusation or automatic authority report.

## Production gate

[production-gate.md](production-gate.md) lists what blocks production. Any agent that sees a production trigger must list the open blockers and refuse production work until they are closed there. [AGENTS.md](../../AGENTS.md) repeats the rule.

## Boundaries and residual risk

An installation UUID is client-controlled, replaceable and spoofable. An IP can be a VPN, proxy, carrier NAT or shared connection. Neither proves identity or authorship. `safety_identifier` supports provider correlation, not authentication. A model-filled `scope` field can be manipulated by injection. Moderation has false negatives and positives; this cut cannot claim illegal-image or CSAM detection.

Legal review and publication are not achieved by code. In the closed test, testers receive a notice (CP1); public text belongs to CP8.

## Reading order

1. [SDD](../sdd/README.md), [AGENTS](../../AGENTS.md), and this context.
2. [Production gate](production-gate.md).
3. [Policy](specifications/content-policy.md), [identity and audit](specifications/identity-and-audit.md), [refusal copy](specifications/refusal-copy.pt-BR.md), [sources](sources.md).
4. Closed-test operations: [data map](operations/closed-test-data-map.md), [incident note](operations/closed-test-incident.md), [tester notice](legal/tester-notice.pt-BR.md).
5. [ADR-024](adrs/ADR-024-content-safety-boundaries.md) (accepted 2026-09-30) and [ADR-025](adrs/ADR-025-safety-correlation-audit.md) (accepted 2026-10-01).
6. [Plan order](plans/README.md) and [validation matrix](validation/README.md).
7. Current implementation in the owning folder before each approved delivery.

## Plan order

Closed test: CP2 → CP1 (any time before CP5) → CP3 → CP4 → CP5. Out of scope: CP6, CP7, CP8, CP9. See the [plan index](plans/README.md).

## Decision status

Accepted owner constraints (2026-09-30): create and keep this context as the single home for user-content × AI safety; plan economical prevention and correlation; defer additional detection platforms (budget); add the SDD `Fora de escopo` state; keep only closed-test controls active; defer production plans with "app in closed test" as the reason; enforce them through the production gate.

ADR-024 was accepted with the named approval of CP2 on 2026-09-30. Other proposed implementation choices are reviewable in the specifications and plans and are not deployed. Accept the relevant proposed ADR when approving its named plan.
