# Content-policy plans

Rescoped by the owner on 2026-09-30: the app is in a closed test. Active plans cover only that phase. Production work is deferred to `out_of_scope/` and enforced by the [production gate](../production-gate.md), which owns the blocker list. One agent and one plan/code boundary per `/goal`, following [SDD](../../sdd/README.md).

## Active

Files at the root of this folder. Plan state: each plan's own State line and its folder.

- [CP10 — Workout reports in scope; the boundary of "skipping a meal"](cp10-workout-in-scope-and-skip-boundary.md) (prerequisite of S35).

## Out of scope

- [CP6 — Production readiness](out_of_scope/cp6-production-readiness.md)
- [CP7 — Specialist detection](out_of_scope/cp7-specialist-detection.md)
- [CP8 — Public legal pack](out_of_scope/cp8-public-legal-pack.md)
- [CP9 — Production audit and containment](out_of_scope/cp9-production-audit-and-containment.md)

Each plan records its reason. Do not run `/goal` on an out-of-scope plan. Reactivation follows [SDD § Fora de escopo](../../sdd/README.md#fora-de-escopo).

## History

[`completed/`](completed/) (closed-test controls CP1–CP5).

## Approval

The owner explicitly approves the named plan before execution: `Aprovo o plano docs/content-policy/plans/<plan>.md. Implemente o plano aprovado.` Run one plan at a time.
