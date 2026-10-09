---
name: dieta-bot-android-memory
description: Record an explicit durable Fibrai product or architecture decision in its plan and ADR without changing accepted history.
---

# Fibrai decision records

Read [AGENTS](../../../AGENTS.md), the [context matrix](../../../docs/README.md) and [SDD](../../../docs/sdd/README.md) before choosing an owner.

- At Planning, record an explicit behavior decision in the active plan as the intended specification change. Create an architectural ADR when a durable decision is not already covered. The live specification is rewritten in place only at Completion of the plan that delivers the behavior, with the plan added to its Provenance section.
- New records use docs/<context>/adrs/ADR-NNN-<slug>.md, with an unused ID and filename convention discovered from the matrix and existing files.
- Accepted historical ADRs 001–011 remain in docs/decisions/. An accepted ADR body is immutable: create a successor, declare the extent of replacement and the surviving scope, and update only the predecessor's status line.
- Record date, status, context, decision, motivation, consequences and alternatives; include version/path when relevant. Distinguish proposed from accepted decisions and link related specifications/plans.
- Update the owning README and affected indices by linking, without copying status, version or date (single writer). A decision record is not implementation authorization; code still needs approval identifying its plan.

Technical prose is English; product copy remains pt-BR. Do not store secrets, .env contents, unrequested opinions or a model's guess as an owner decision.
