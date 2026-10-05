# ADR-033 — Global Chat example provenance

- Status: Accepted (2026-10-04, explicit owner direction in this conversation to prohibit owner/user incidents from becoming global Chat examples)
- Date: 2026-10-04
- Owner: `content-policy`
- Supersedes: none. Complements [ADR-024](ADR-024-content-safety-boundaries.md) and the fixed-instructions/per-request-context boundary in [server Chat](../../server/specifications/v1-chat.md).
- Delivery: [S19](../../server/plans/s19-generalizable-chat-instructions.md) removes existing violations and adds verification. This decision governs new prompt changes immediately; acceptance does not mean the deployed prompt has already been cleaned up or authorize implementation of S19.

## Context

Debugging individual conversations has introduced concrete incident examples into instructions shared by all Chat users. Fixing one person's case can therefore make that case part of every future request, reinforce a narrow test distribution and allow illustrative details to influence unrelated estimates.

An incident is evidence of a failure mode. Its foods, amounts, brands, habits, wording and sequence are not a reusable product instruction. The owner explicitly requires this distinction for their own cases and for every other user's cases.

## Decision

### 1. Real cases never become global Chat examples

Do not promote any actual owner's, tester's or other user's interaction into an example in global Chat instructions. This applies to text, photo/label descriptions, meals, quantities, brands, nutrient values, preferences, routines and conversation sequences from logs, screenshots, support reports or direct reports to an agent.

The prohibition covers literal copying, translation, anonymization, paraphrasing and lightly modified reproductions. Replacing a name, brand or quantity does not make a retelling of the incident eligible. Removing personal identifiers alone does not satisfy this decision.

The boundary includes fixed system/developer instructions, shared prompt prefixes, illustrative user/assistant turns, shared compact/digest instructions and examples injected by templates or helpers before an ordinary Chat request. Moving an example into another file, role or assembly step does not bypass it.

### 2. Derive a general rule, then test it independently

For an observed problem, identify the violated relationship or invariant and its owning specification. Change the general rule or deterministic behavior within an approved implementation plan. Do not append the incident as a teaching example or create a special case keyed to its particular foods or phrasing.

Minimal fictional examples remain possible when a rule or required syntax demonstrably needs one. They must be authored from the general rule, independently of any real person's incident, and their synthetic origin and purpose must be documented. Do not relabel an anonymized retelling as synthetic. A common food or ordinary phrase is not permanently prohibited just because a user once mentioned it; origin and function, not a vocabulary blacklist, determine compliance.

Regression coverage may exercise the same abstract failure mode using independent synthetic situations and varied profiles. Follow the existing [data map](../operations/closed-test-data-map.md) and [content policy](../specifications/content-policy.md) for evidence handling; this decision grants no new permission to retain or publish conversations. Test fixtures must never be loaded as global instruction examples.

### 3. Keep personal context personal

The current user's own profile, memory, recorded day, recent meals, conversation, digests, message and photo remain legitimate per-request context under the existing contract. Their presence in that user's request is not a global example. Keep that data outside shared instructions; do not turn an individual habit into a default for other users or for an empty context.

No change to product language, timezone, model, meal rules, memory behavior, moderation, correlation or retention follows from this decision.

### 4. Verify every future prompt change

A delivery touching global Chat instructions must record the general rule being addressed, its source specification and the provenance/purpose of any illustrative examples. An added or retained example with unknown or real-user origin is ineligible; omit it or derive the rule without it.

Review the final assembled prompt, not only the edited constant. Automated checks must cover the declared example inventory and context separation, including deliberate failures when an undeclared snippet is assembled. Regression evaluations must include independent food/profile/phrasing variants that do not reproduce retained prompt examples.

Automation cannot prove that an arbitrary sentence was invented independently of every user conversation. Source review and the provenance record remain required alongside tests; do not claim an exact-string scan provides that guarantee. A failing provenance check blocks delivery of the prompt change. This is part of the normal repository delivery, not a separate owner approval ritual.

## Motivation and consequences

- An incident can improve behavior for everyone through a general rule without carrying that person's case into everyone else's context.
- The allowed per-user personalization remains useful; this decision does not remove memory or habitual-meal features.
- Existing prompt examples need an origin audit and cleanup. New examples require a small provenance record and transfer tests, adding review work.
- Historical accepted ADRs and diagnostic evidence are not rewritten to erase provenance. They are not runtime instructions and cannot be imported into a prompt as examples.
- Compliance reduces one source of coupling; it is not a guarantee of model accuracy or universal generalization.

## Alternatives considered

- Copy the incident verbatim to prevent recurrence: rejected because it promotes an individual case to a global instruction.
- Change identifying details or food quantities: rejected because the case remains an incident-derived example.
- Remove all per-user memory and history: rejected because contextual personalization is a separate, intended behavior.
- Ban every fictional example: unnecessary; an independently authored minimal syntax/ambiguity example may still be useful when its purpose and provenance are verified.
- Rely only on a list of forbidden foods/strings: rejected because paraphrases bypass it and ordinary food vocabulary is legitimate.

## Relationships and rollout

[Content policy](../README.md) owns this rule. [Server](../../server/README.md) implements the cleanup and automated checks through S19. The repository constitution routes agents here before changing global Chat instructions. S19 incorporates the authored-instruction boundary into the live content-handling and server specifications at its Completion, with measured validation; the deployed prompt is not declared compliant during planning.

Accepted ADR bodies remain immutable. A future change to this decision requires a successor ADR; only the status line records supersession.
