---
name: fastapi-security
description: Apply Dieta Bot FastAPI authentication, rate-limit, payload and LLM-input boundaries when an approved server plan changes routes or security handling.
---

# FastAPI boundaries

Read [AGENTS](../../../AGENTS.md), the approved server plan, [HTTP contract](../../../docs/api-contract.md), [Chat specification](../../../docs/server/specifications/v1-chat.md) and applicable accepted ADRs. A security skill does not authorize adding routes or changing the contract.

- Validate X-Invite with secrets.compare_digest. Do not expose secrets, keys or invite values in logs/errors.
- Preserve the contracted rate limits on model-consuming routes using the existing limiter. Review both rate-limit keys and proxy/network context; do not invent a separate authentication or limiting scheme.
- [ADR-022](../../../docs/produto/adrs/ADR-022-limite-texto-chat.md) sets Chat text and messages[].text to 2000 code points; estimate/fit retain 1000. The contract's older introductory all-route 1000 summary is inconsistent with its current Chat section. Do not reapply that historical Chat limit.
- [ADR-018](../../../docs/android/adrs/ADR-018-foto-2048.md) and the contract preserve a 16 MB photo guard with a 24 MB JSON-body boundary. Read current config/shaping rather than copying an illustrative generic threshold.
- A Content-Length check can reject an advertised oversized body early; an absent, malformed or smaller header does not prove the received body is bounded. Assess actual ASGI/proxy handling within the approved security scope, and report gaps without claiming a header-only check is complete protection.
- Keep untrusted meal/Chat inputs separate from system instructions and use the existing delimiters. Delimiters alone do not prove resistance to prompt injection.
- Preserve the server-only model configuration, fail-soft response shaping, stateless day handling and photo non-persistence. Dev conversation logging has its own restricted contract; it is not client telemetry.

Run the security/route tests required by the server plan and document the observed boundaries and unresolved issues. Do not test live model endpoints or deploy merely because this skill is active.
