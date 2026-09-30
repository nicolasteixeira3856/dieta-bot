# Content-policy plan order

All active plans are `Aguardando aprovação`. Creating them does not approve implementation. Use one agent and one plan/code boundary per `/goal`, following [SDD](../../sdd/README.md).

| Order | Plan | Delivery boundary | Dependency / result |
| --- | --- | --- | --- |
| 1 | [CP1 — Policy and incident procedure](cp1-policy-and-incident-procedure.md) | `docs/content-policy/` | Reviewable notices, retention schedule and incident procedure; owner review before live activation. |
| 2 | [CP2 — Server content controls](cp2-server-content-controls.md) | `server/` | CP1 operational review; scope/moderation/output/memory controls and safe failures. No deployment. |
| 3 | [CP3 — Safety correlation and audit](cp3-server-safety-audit.md) | `server/` | CP2 automated acceptance; HMAC identifiers, minimal events, retention and containment. No deployment. |
| 4 | [CP4 — Android installation identity](cp4-android-installation-identity.md) | `apps/android/` | CP3 contract; private UUID and API-only transport. No new screen. |
| 5 | [CP5 — GCP ingress and activation](cp5-gcp-trusted-ingress.md) | `infra/gcp/` | CP2–CP4 automated acceptance, CP1 activation decisions/notice; trusted proxy, secret provisioning, dev deploy. |
| 6 | [CP6 — Validation and readiness](cp6-validation-and-readiness.md) | `docs/content-policy/` | CP5 deployed evidence and CP4 APK; integrated checks and explicit readiness decisions. |
| — | [CP7 — Specialist detection](out_of_scope/cp7-specialist-detection.md) | Future `server/` | `Fora de escopo`: financial/business deferral. Do not run. |

CP2–CP4 can be consumed by their dependents after implementation and automated acceptance while they await the integrated manual evidence from CP5/CP6. This is an explicit prerequisite distinction, not permission to mark them completed early. CP1 outstanding external public-launch review does not automatically block local control implementation; unresolved facts required for a specific live data collection do block that activation.

## Approval and goal invocation

The owner explicitly approves the named plan before execution. A single `/goal` message that both approves and names the plan satisfies this gate. Example wording appears below; it is a command suggestion, not recorded approval.

```text
/goal Aprovo o plano docs/content-policy/plans/cp1-policy-and-incident-procedure.md. Execute a entrega documental e as validações previstas, mantendo as pendências manuais explícitas.

/goal Aprovo o plano docs/content-policy/plans/cp2-server-content-controls.md e a proposta ADR-024 vinculada. Implemente e valide somente o escopo desse plano.

/goal Aprovo o plano docs/content-policy/plans/cp3-server-safety-audit.md e a proposta ADR-025 vinculada. Implemente e valide somente o escopo desse plano.

/goal Aprovo o plano docs/content-policy/plans/cp4-android-installation-identity.md. Implemente e valide somente o escopo desse plano.

/goal Aprovo o plano docs/content-policy/plans/cp5-gcp-trusted-ingress.md. Confira os pré-requisitos, implemente e valide a ativação controlada no dev.

/goal Aprovo o plano docs/content-policy/plans/cp6-validation-and-readiness.md. Execute a validação integrada e registre separadamente a prontidão técnica e as pendências de publicação e revisão jurídica.
```

Run one command at a time. After CP4, request a test build when ready, through A16; do not bypass automatic versioning. Do not invoke CP7 until explicit reactivation and refreshed approval. Plan paths change on lifecycle transitions; use the current indexed path for follow-up work.
