# ADR-024 — Content safety boundaries

- Status: Proposed; acceptance requires approval of [CP2](../plans/cp2-server-content-controls.md).
- Date: 2026-09-30
- Owner: `content-policy`
- Supersedes on acceptance: the unrestricted off-topic interpretation of Chat `question` and unchecked text-only fallback in the live [Chat contract](../../server/specifications/v1-chat.md). Complements ADR-023; does not replace log/plan/question or meal behavior.
- Partially supersedes [ADR-015](../../server/adrs/ADR-015-log-conversa-dev.md) on acceptance: raw trace/error capture for the policy-controlled pipeline is suppressed in CP2. ADR-025 proposes the subsequent general replacement with minimal security audit. Historical files are not deleted by either code delivery.

## Context

The app answered an unrelated math question. Current delimiters and structured output do not enforce semantic scope. Text, photos, history and memory are untrusted inputs. The owner requests practical prevention while additional detection providers are outside the present budget.

## Proposed decision

Adopt the [content policy](../specifications/content-policy.md): server-side moderation using the existing OpenAI provider, bounded scope checks before/after generation, domain validation, and fixed refusal paths. Cover all model routes and compaction. No automatic legal finding, automatic reporting, tool execution or new app screen.

Keep `gpt-6-luna`, effort `none`. Treat specialized detection as [CP7](../plans/out_of_scope/cp7-specialist-detection.md), explicitly deferred. The moderation model is a safety classifier, not a replacement task LLM or a new vendor. Validate the entire user-visible response and all generated memory/digest text before use. Suppress raw traces of the policy pipeline.

## Consequences

- Lower scope drift and exposure; measurable refusal and false-positive behavior.
- Additional paid task-model checks, latency and potential false blocks. Shared deadlines and bounded calls are required.
- This does not provide full CSAM detection, legal compliance by itself, or perfect prompt-injection prevention.
- No layout change is authorized. Any later legal acceptance/report UI needs its own product plan and applicable Stitch gate.

## Alternatives

- Prompt-only restriction: insufficient as the sole control.
- General moderation alone: does not enforce product scope or specialized image detection.
- Immediate new vendor integration: deferred by owner constraint.
- Removing text/photo input: contradicts the present product; not selected.

Accepted ADRs remain immutable. Once accepted, record date and named approval; future changes require a successor.
