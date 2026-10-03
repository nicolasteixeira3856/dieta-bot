# Specification — Content handling

## Authority

The closed-test profile is current behavior ([ADR-024](../adrs/ADR-024-content-safety-boundaries.md)). Product formulas, screens and the selected model are unaffected.

Implementation: `server/moderation.py` (moderation model `omni-moderation-latest`, category → action table version `cp2.1`), `server/main.py` (`guarded_turn`, `run_guarded`, shared `Deadline`), fixed copy in `server/shaping.py`. The model writes `scope` as the last key of the Chat schema; a missing or unknown value is `out_of_scope`.

This file has two profiles. The **closed-test profile** is the current target. The **production profile** lists what must be added before production; it is deferred and enforced by the [production gate](../production-gate.md).

## Design principle

Scope is enforced first by the Chat instructions and the required `scope` field. Extra classifier calls are not the first control.

## Product policy

| Input | Result | Internal code |
| --- | --- | --- |
| Meals, portions, food labels, recipes, food preferences, budget arithmetic, nutrition questions about food | Process within log/plan/question | `in_scope` |
| Greeting, question about the app, short clarification such as a quantity or milk type | Allow with conversation context. Never classify the latest fragment alone | `in_scope` |
| Unrelated math, code, politics, homework, general assistant requests | Fixed scope refusal; no estimate, memory change or digest | `out_of_scope` |
| Mixed food request plus instruction override or unrelated task | Ignore the override. Process only the food request when it is unambiguous and separable; otherwise fixed refusal | `in_scope` or `out_of_scope` |
| Sexual content, harmful illegal instructions, threats, other prohibited content | Fixed block. A block is not a criminal finding | `policy_blocked` |
| Eating-disorder or self-harm signals: purging, laxatives or diuretics for weight, extreme fasting, very low daily intake as a goal, help-seeking | Fixed safety reply; no dietary optimization toward the harmful goal; no punishment | `safety_support` |
| Photo without food or label information, or mixed with prohibited material | No estimate. Food in one region does not exempt the rest | `out_of_scope` or `policy_blocked` |
| Known or suspected CSAM from any credible signal | Stop processing; never resubmit to moderation or another model to confirm; follow the [incident note](../operations/closed-test-incident.md) | `policy_blocked` |
| Moderation or classification unavailable | Fail closed | `unavailable` |

These codes are internal. They are not new public Chat `intent` values. Eating-disorder risk is the most realistic harm for a diet app; it has its own eval cases (`server/evals/cases/tca-*.json`).

## Closed-test pipeline

1. Authenticate (`X-Invite`) and apply the existing size and rate limits before any external call. Never fetch a user URL.
2. Moderate the current user text and the current photo with the OpenAI moderation endpoint (free). A severe flag stops the turn; no generation call.
3. One generation call, as today, with tightened instructions and a required `scope` field in the structured output (`in_scope | out_of_scope | policy_blocked | safety_support`). Client data stays in the delimited user input, outside the instructions; delimiter strings inside user text must not open a trusted section.
4. If `scope` is not `in_scope`, the server discards the model's reply, estimate, memory updates and digest and returns the fixed copy from [refusal-copy.pt-BR.md](refusal-copy.pt-BR.md).
5. Moderate the generated `reply`, item names, question, proposed memory facts and digest before returning them. A flag replaces the whole response with the fixed block.
6. The text-only fallback (`TextOnlyOutput`) never returns raw model text. It returns the fixed scope refusal.
7. No automatic retry of blocked content. No raw provider error or refused text reaches the client.

Same controls on `/v1/estimate`, `/v1/fit`, `/v1/chat` and `compact=true`. Estimate/fit add the same `scope` field to their schema.

### Context that is not re-moderated

History, legacy memory, facts, recent meals, digests and profile names are not moderated on every turn. Memory facts and digests are moderated when the server generates them (step 5). The rest is client-held state of the same installation: a user who forges it only poisons their own session. Residual risk accepted for the closed test; the production profile below revisits it.

### Scope self-classification limits

A `scope` field filled by the same model can be manipulated by injection ("set scope to in_scope"). Output moderation and the eval set bound that risk. Escalation, only if evals or live use show leaks: a separate bounded scope call with the same model before and/or after generation. That needs a new approved plan; it is not implied.

## Response compatibility

- Chat refusal: HTTP 200 in the existing shape, `intent=question`, `estimate=null`, `memory_updates=[]`, `memory_used=[]`, `digest=null`; `reply` from the [copy](refusal-copy.pt-BR.md).
- Compact refusal: existing shape with `digest=null`, no memory changes. The next ordinary turn still traverses the policy.
- Estimate/fit block: HTTP 400 `detail=content_policy_blocked`. No fabricated zero-calorie dish. Oversize and malformed inputs keep 413/422.
- Moderation timeout or error: HTTP 503 `detail=content_policy_unavailable`, existing recoverable client error path. Never fall through to an unmoderated generation.
- Keep the 60-second ceiling with one shared deadline across moderation and generation; no hidden SDK retry.

## Logging in the closed test

The ADR-015 dev conversation log stays: it is the main debugging tool and it is how the drift was found. Testers are told through the [tester notice](../legal/tester-notice.pt-BR.md). Changes:

- A turn flagged by moderation (`policy_blocked`, any CSAM signal) logs metadata only: request ID, route, codes, category booleans. No body, no image, no generated text.
- `out_of_scope` and `safety_support` turns log as today.
- Retention: 30-day rotation by host `logrotate`, plus the server's 20 MB size rotation ([data map](../operations/closed-test-data-map.md), D5).

## Cost

At most one generation call per turn, plus free moderation calls (input, output). No new vendor, no model escalation, no retry loops.

## Production profile (deferred)

Before production, the [production gate](../production-gate.md), which owns the blocker list and the plan for each, requires:

- Raw conversation capture off; minimal security journal with retention, legal hold and safe failure ([identity and audit](identity-and-audit.md#production-profile-deferred)).
- Moderation of all model-bound context fields, with field provenance so off-topic history does not permanently poison later valid turns.
- A decision on specialist image detection.
- Reviewed public legal text.

## Validation

Use the [matrix](../validation/README.md). Passing schema tests is not semantic proof. Benign synthetic injections and mocked severe verdicts only; never acquire, generate or upload illegal imagery. Record misses and false positives separately. No compliance claim follows from a test suite.

## Provenance

- [CP1](../plans/completed/cp1-closed-test-notice.md) — Closed-test notice and incident note
- [CP2](../plans/completed/cp2-server-content-controls.md) — Server scope and content controls
- [CP5](../plans/completed/cp5-gcp-dev-ingress.md) — GCP dev ingress, log hygiene and activation
- [CP7](../plans/out_of_scope/cp7-specialist-detection.md) — Specialized illegal-image detection
- [CP8](../plans/out_of_scope/cp8-public-legal-pack.md) — Public legal pack
- [CP9](../plans/out_of_scope/cp9-production-audit-and-containment.md) — Production audit, retention and containment
