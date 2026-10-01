# Content-policy plan order

Rescoped by the owner on 2026-09-30: the app is in a closed test. Active plans cover only that phase. Production work is deferred to `out_of_scope/` and enforced by the [production gate](../production-gate.md).

CP2 is `Pendente aprovação manual` (implemented and deployed to dev on 2026-09-30, owner APK check pending). The other active plans are `Aguardando aprovação`. Creating them does not approve implementation. One agent and one plan/code boundary per `/goal`, following [SDD](../../sdd/README.md).

## Active (closed test)

| Order | Plan | Delivery boundary | Dependency / result |
| --- | --- | --- | --- |
| 1 | [CP2 — Server content controls](pending_manual_validation/cp2-server-content-controls.md) | `server/` | None. Fixes the off-topic drift: tightened prompt, `scope` field, fixed refusals, free moderation, evals. Deployed to dev 2026-09-30; `Pendente aprovação manual`. |
| 2 | [CP1 — Closed-test notice and incident note](cp1-closed-test-notice.md) | `docs/content-policy/` | None; can run any time before CP5. Tester notice, data map, one-page incident note. |
| 3 | [CP3 — Server safety identifier](cp3-server-safety-identifier.md) | `server/` | CP2. Optional installation header, HMAC `safety_identifier`. |
| 4 | [CP4 — Android installation identity](cp4-android-installation-identity.md) | `apps/android/` | CP3. Private UUID and API-only header. No new screen. |
| 5 | [CP5 — GCP dev ingress and log hygiene](cp5-gcp-dev-ingress.md) | `infra/gcp/` | CP2, CP3 deployed; CP4 accepted; CP1 notice delivered. Narrow proxy trust, log rotation, secret, budget limit, invite rotation runbook. |

## Out of scope

| Plan | Reason | Production blocker |
| --- | --- | --- |
| [CP6 — Production readiness](out_of_scope/cp6-production-readiness.md) | App in closed test | Yes (PG5) |
| [CP7 — Specialist detection](out_of_scope/cp7-specialist-detection.md) | Budget | Decision required (PG4) |
| [CP8 — Public legal pack](out_of_scope/cp8-public-legal-pack.md) | App in closed test | Yes (PG2) |
| [CP9 — Production audit and containment](out_of_scope/cp9-production-audit-and-containment.md) | App in closed test | Yes (PG3) |

Do not run `/goal` on an out-of-scope plan. Reactivation follows [SDD § Fora de escopo](../../sdd/README.md#fora-de-escopo).

## Approval and goal invocation

The owner explicitly approves the named plan before execution. One `/goal` message that both approves and names the plan satisfies this gate. Suggested wording, not recorded approval:

```text
/goal Aprovo o plano docs/content-policy/plans/cp2-server-content-controls.md e a proposta ADR-024 vinculada. Implemente, valide e faça o deploy no dev somente do escopo desse plano.

/goal Aprovo o plano docs/content-policy/plans/cp1-closed-test-notice.md. Execute a entrega documental e mantenha a entrega do aviso aos testers como pendência manual.

/goal Aprovo o plano docs/content-policy/plans/cp3-server-safety-identifier.md e a proposta ADR-025 vinculada. Implemente e valide somente o escopo desse plano.

/goal Aprovo o plano docs/content-policy/plans/cp4-android-installation-identity.md. Implemente e valide somente o escopo desse plano.

/goal Aprovo o plano docs/content-policy/plans/cp5-gcp-dev-ingress.md. Confira os pré-requisitos, implemente e valide a ativação no dev.
```

Run one command at a time. After CP4, request a test build through A16 when ready; do not bypass automatic versioning. Plan paths change on lifecycle transitions; use the current indexed path for follow-up work.
