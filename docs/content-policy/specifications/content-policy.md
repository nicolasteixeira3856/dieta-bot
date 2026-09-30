# Specification — Content handling

## Status and authority

Current state: described in the [context](../README.md); these controls are not deployed. The target below is proposed for approval through [CP2](../plans/cp2-server-content-controls.md) and [ADR-024](../adrs/ADR-024-content-safety-boundaries.md). Product formulas, screens and the selected model remain unchanged.

## Product policy

| Input | Proposed result |
| --- | --- |
| Meals, portions, food labels, recipes, budget arithmetic, food preferences | Process within existing log/plan/question behavior. |
| Greeting, app-help question, short clarification such as a quantity or milk type | Allow with relevant conversation context. Do not classify the latest fragment in isolation. |
| Unrelated math, code, politics or general-purpose assistant requests | Fixed scope refusal; no estimate, memory change or digest. |
| Mixed legitimate request plus instruction override/unrelated task | Do not follow the override. Process only if the allowed request is unambiguous and safely separable; otherwise fixed refusal. |
| Sexual content, harmful illegal instructions, threats or other prohibited content | Product-policy block according to category/context. A block is not a criminal finding. |
| Victim disclosure, help-seeking or eating-disorder/self-harm risk | Safe bounded response; no dangerous dietary optimization, no automatic punishment/report solely on the model score. Use the CP1 escalation procedure where applicable. |
| Photo with no relevant food/label information, or mixed image with prohibited material | No meal estimate. Food in one region does not exempt the rest of the image. |
| Known or suspected CSAM from an existing credible signal | Stop ordinary model processing; do not resubmit to moderation or another model to confirm. Follow CP1. |

English policy identifiers: `allowed`, `out_of_scope`, `policy_blocked`, `safety_support`, `unavailable`. They are internal categories, not new public Chat intent values.

## Pipeline

1. Authenticate and limit resources before external calls. Enforce actual bytes received, not just Content-Length; preserve existing size limits. Bound decoded image dimensions/pixels as well as bytes. Validate JPEG/base64 in a bounded decoder; client-side sanitization is not trusted proof. Never fetch an arbitrary user URL.
2. Respect a previously established incident stop before any content transmission. Otherwise moderate the text fields that will reach any model and the current ordinary image. Include free-text profile names, history, legacy memory, facts, recent meals and digests. A context-only unsafe item must not silently bypass moderation.
3. Use one bounded structured scope check with the same `gpt-6-luna`, `reasoning.effort=none`, and applicable context. Treat OCR/text in photos and all client data as untrusted. The classifier returns only an enum/reason code, never a user-visible answer or executable instruction.
4. Call the existing task model only on an allowed path. Keep rules in instructions and escaped structured data in the user input; embedded delimiter strings must not create a trusted section. No tools, shell, URLs, secrets or other users' context are introduced.
5. Validate schema and domain constraints, then check every generated text field for safety and scope before responding or logging content. Include reply, meal/item names, questions, proposed memory facts and digests. The text-only fallback has no exemption. A safety-valid statement can still be off-topic.
6. On a blocked/unavailable path return only a fixed safe envelope/error; no generated estimate, memory update or digest. Do not return raw provider errors or refused text. Do not automatically retry blocked content.

Compaction uses the same controls with a summarization-specific scope rule. Off-topic historical turns alone must not permanently poison later valid requests: CP2 must test context filtering/omission by field with the latest valid request preserved, while unsafe current content still blocks. Do not silently rewrite or delete the client's stored history. Repeated blocked context may remain unavailable when it cannot be safely separated; explain this residual limitation in validation.

## Category policy

Use provider boolean categories and supported modalities as the initial detection inputs; no arbitrary numeric threshold is advertised as a legal boundary. Explicit sexual/harmful-instruction categories route to a block; self-harm intent/help-seeking routes to safety support. Violence/hate/harassment signals require the bounded context decision to distinguish benign quotations or victim reports from assistance/endorsement. An unresolved classification routes to unavailable/block without punishment. Record a versioned category-to-action table in implementation; all rows need positive/negative mock tests.

The public `sexual/minors` category has no image coverage. Ordinary image moderation and a food-scope check reduce exposure; they do not constitute dedicated CSAM detection. A later signal of suspected CSAM stops all further content-bearing checks and ordinary diagnostic retention. A static local blocklist of words is not the primary detector.

## Response compatibility

- Chat refusals: HTTP 200 in the existing shape, `intent=question`, `estimate=null`, `memory_updates=[]`, `memory_used=[]`, `digest=null`; `reply` uses [copy](refusal-copy.pt-BR.md). Legacy clients cannot receive an actionable meal card.
- Compact refusal: existing shape with `digest=null`, no memory changes. The client already tolerates failed compaction; the following ordinary turn must still traverse policy.
- Estimate/fit policy blocks: HTTP 400 with `detail=content_policy_blocked`; malformed/oversize inputs retain existing 422/413 behavior. No fabricated zero-calorie dish.
- Mandatory safety service timeout/error: HTTP 503, `detail=content_policy_unavailable`, existing recoverable client error path. Never fall through to an unmoderated generation.
- Preserve the existing 60-second server ceiling and client timeout contract. Use a shared deadline across steps with no hidden SDK retry; CP2 tests exhaustion at each stage.

## Cost and evaluation contract

Maximum three Responses calls per ordinary turn: scope check, generation, output scope check. Initial output ceilings: 384, 2048 and 384 tokens respectively; compaction generation is capped at 512. Moderation calls may batch bounded text units; CP2 must record explicit batch/input caps within provider limits, with overflow rejected rather than truncated or skipped. No retry loops, automatic model escalation or new vendor. Limits are proposals accepted with CP2, and measured against existing legitimate scenarios before completion.

Scope/safety controls cover `/v1/estimate`, `/v1/fit`, `/v1/chat` and `compact=true`. Moderation failures and policy refusals must not enter ordinary raw conversation logging. CP2 suppresses raw trace/error bodies for the policy pipeline; CP3 replaces routine conversation capture with minimal audit events. Historical logs are handled under CP1/CP5, never deleted implicitly by a code change.

## Validation

Use the [matrix](../validation/README.md). Passing JSON/schema tests is not semantic proof. Use synthetic benign injections and mocked severe-content verdicts; never acquire, generate or upload illegal imagery for testing. Record misses and false positives separately. A legal/compliance claim cannot be inferred from a passing test suite.
