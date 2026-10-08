# ADR-024 — Content safety boundaries

- Status: Accepted, 2026-09-30, with the owner's named approval of [CP2](../plans/completed/cp2-server-content-controls.md): "Aprovo o plano docs/content-policy/plans/cp2-server-content-controls.md e a proposta ADR-024 vinculada. Implemente, valide e faça o deploy no dev somente do escopo desse plano."; complemented by [CP10](../plans/completed/cp10-workout-in-scope-and-skip-boundary.md) (2026-10-08: a workout report is in scope; skipping one meal is not a safety signal), body unchanged
- Date: 2026-09-30 (revised 2026-09-30 for the closed-test cut)
- Owner: `content-policy`
- Supersedes: the off-topic "general question" interpretation of Chat `question` and the raw text-only fallback in the live [Chat contract](../../server/specifications/v1-chat.md). Complements ADR-023; log/plan/question and meal behavior unchanged.
- Partially supersedes [ADR-015](../../server/adrs/ADR-015-log-conversa-dev.md): moderation-flagged turns are logged as metadata only. Ordinary dev conversation logging stays during the closed test.

## Context

The app answered an unrelated math question. The live prompt allows "general question" under `question`; delimiters and structured output do not enforce semantic scope. Text, photos, history and memory are untrusted. The app is in a closed test with two testers and an invite. Specialist detection providers are outside the budget.

## Decision

Adopt the closed-test profile of the [content policy](../specifications/content-policy.md):

- Tighten the instructions to the meal-budget scope.
- Add a required `scope` field to the existing structured output; the server replaces any non-`in_scope` result with fixed copy and drops estimate, memory and digest.
- Free OpenAI moderation on the current text and photo before generation and on generated text after it. Fail closed.
- The text-only fallback returns fixed copy, never raw model text.
- A dedicated eval set for off-topic, injection and eating-disorder cases.

Keep `gpt-6-luna`, effort `none`, one generation call per turn. Separate scope-classifier calls are an escalation only if evals show leaks. Specialist detection stays in [CP7](../plans/out_of_scope/cp7-specialist-detection.md). Production additions are listed in the [production gate](../production-gate.md).

## Consequences

- Off-topic drift closes at the cause, with no extra paid call.
- Self-classification can be manipulated by injection; output moderation and evals bound it. Residual risk recorded.
- Some false refusals are possible; the eval baseline measures them.
- No full CSAM detection, legal compliance by itself, or perfect injection prevention.
- No layout change.

## Alternatives

- Prompt-only restriction without a server-enforced `scope` field: the model's free text still reaches the user; rejected.
- Separate LLM scope checks before and after generation: up to 3x cost and latency for a two-tester closed test; kept as escalation.
- General moderation alone: does not enforce product scope.
- New detection vendor now: deferred by owner (budget).
- Removing text/photo input: contradicts the product.

Accepted ADRs remain immutable. Once accepted, record date and named approval; future changes require a successor.
