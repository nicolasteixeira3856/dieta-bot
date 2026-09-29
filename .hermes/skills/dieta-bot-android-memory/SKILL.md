---
name: dieta-bot-android-memory
description: Record an explicit durable Dieta Bot product or architecture decision in its owning specification and ADR without changing accepted history.
---

# Dieta Bot decision records

Read [AGENTS](../../../AGENTS.md), the [context matrix](../../../docs/README.md) and [SDD](../../../docs/sdd/README.md) before choosing an owner.

- Update the live specification for an explicit behavior decision. Create an architectural ADR when a durable decision is not already covered.
- New records use docs/<context>/adrs/ADR-NNN-<slug>.md, with an unused ID and filename convention discovered from the matrix and existing files.
- Accepted historical ADRs 001–011 remain in docs/decisions/. An accepted ADR is immutable: create a successor and declare the extent of replacement.
- Record date, status, context, decision, motivation, consequences and alternatives; include version/path when relevant. Distinguish proposed from accepted decisions and link related specifications/plans.
- Update the owning README and affected indices. A decision record is not implementation authorization; code still needs approval identifying its plan.

Technical prose is English; product copy remains pt-BR. Do not store secrets, .env contents, unrequested opinions or a model's guess as an owner decision.
