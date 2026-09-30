# CP2 — Server scope and content controls

- Status: Aguardando aprovação
- Date: 2026-09-30
- Owner: `content-policy`; executable owner: server.
- Delivery boundary: `server/`; affected documentation/indexes only outside it.
- Prerequisites: CP1 operational procedure prepared and owner-reviewed; approval accepts proposed ADR-024. CP1 external public-release review need not be complete to implement/test these controls.

## Objective and authorization

Stop off-topic answers and prevent unchecked content from reaching responses, memory and summaries. Approval must name this plan. No Android/infra change, deployment, model upgrade or new vendor is part of this delivery.

## Sources

[Content policy](../specifications/content-policy.md), [copy](../specifications/refusal-copy.pt-BR.md), [ADR-024](../adrs/ADR-024-content-safety-boundaries.md), [HTTP contract](../../api-contract.md), [Chat spec](../../server/specifications/v1-chat.md), [sources](../sources.md).

## Implementation

1. Add a cohesive policy service and provider adapter in `server/`; keep route orchestration, pure category/action decisions and provider calls separately testable. Reuse the existing OpenAI account and SDK. Verify actual moderation/request schemas first; no invented parameters.
2. Enforce bounded actual request-body bytes and image decoding (not just declared Content-Length); reject malformed base64/JPEG, excessive dimensions/pixels and decompression bombs before model calls. Use an established maintained decoder if needed; no broad dependency migration. Preserve 16 MB photo/24 MB body limits and 2000-character Chat behavior.
3. Implement the policy pipeline and category table across estimate, fit, chat and compact. Existing credible incident signals stop processing before transmission. Ordinary moderation receives all model-bound text fields and the current ordinary photo; keep field provenance for filtering unsafe historical context. No user URL fetches.
4. Add bounded structured scope checks before generation and on proposed output. Same task model/effort; no instruction parsing by regex as the sole defense. Escape client context structurally and keep it outside trusted instructions. Permit legitimate short answers and food arithmetic. Reject off-topic and dangerous dietary assistance.
5. Validate all output fields, including memory text, item names and summaries. Severe suspicion stops subsequent content-bearing checks. Invalid/unavailable classification fails closed. A valid JSON object cannot bypass semantic checks; raw text fallback must be checked or replaced by the fixed refusal.
6. Implement existing-shape Chat/compact refusal and documented estimate/fit errors. No meal estimate, memory update or digest survives a block. Provider refusals/errors must not become generic generated advice or be retried as another prompt.
7. Suppress raw content traces and exception text for this pipeline, including success and classifier stages. Replace logging tests that currently expect raw bodies with explicit privacy assertions; preserve existing files for CP1 disposition. CP3 will add durable minimal audit, so CP2 validation must state the temporary observability gap.
8. Apply the spec's shared 60-second deadline, call/token ceilings and no-retry policy. Batch moderation within actual provider limits with explicit count/size caps. Keep the 60-second API contract; safety-service errors are 503 without generation. Record per-stage usage without content.

## Files

Existing anchors: `server/main.py`, `llm.py`, `shaping.py`, `config.py`, `conversation_log.py`, `requirements.txt`, `tests/`, `evals/`. New focused modules/tests remain inside `server/`. Update the owning live API/Chat specs only to reflect implemented behavior and keep pending/deployed status distinct.

## Validation

- From `server/`: `.venv/Scripts/python -m unittest discover -s tests` (use the actual existing environment; do not print secrets).
- Mock transports prove: zero generation on input block; no unchecked output on fallback; no memory/digest on block; no content in logs/error paths; shared deadline exhaustion; input-byte limits without Content-Length; invalid JPEG/large decoded image; every route/compact.
- Add evaluation cases for off-topic math, food arithmetic, benign greeting, contextual quantity answer, mixed request, fake system markers, image-text injection, poisoned facts/digests and recovery after old refused turns.
- Adapt the evaluator to exercise the complete policy orchestration. The current direct `LlmClient` plus shaping path is only a model baseline and cannot certify server moderation. Keep baseline and end-to-end policy results separately labelled; assert the actual endpoint contract with fake HTTP/provider transport too.
- Severe/illegal-content categories use synthetic neutral fixtures and mocked verdicts. Never use real illegal images or ask a model to generate them.
- With existing eval tooling, run the existing legitimate cases and the new benign adversarial set three times. Require zero safety/scope leaks in the finite new set and no regression against the recorded legitimate baseline; this is a release criterion, not a reliability percentage. Do not use a 2-of-3 pass to excuse a safety leak. Record latency/tokens/provider cost assumptions.

## Acceptance, rollout and rollback

- All paths satisfy [validation matrix](../validation/README.md); failures block completion.
- Manual owner check: math is refused; a portion calculation and a normal meal still work; a blocked turn does not create cards or memory changes. Keep pending until observed in the CP5 dev rollout; CP5 may consume CP2 once automated acceptance and review are complete.
- Rollback closes affected model routes with a fixed unavailable response; it must not silently disable moderation or re-enable raw logging.
- No deploy in CP2. Record implementation as `Pendente aprovação manual` until integrated evidence, then apply SDD Git delivery and lifecycle.
