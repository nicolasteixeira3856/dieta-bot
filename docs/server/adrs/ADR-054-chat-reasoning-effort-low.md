# ADR-054 — Chat generation at `reasoning.effort=low`

- Status: Accepted (2026-10-08, explicit owner decision after the benchmark: "Esforço será no low, sempre"; the constitution line in `AGENTS.md` § LLM records it; the server rollout is S33)
- Date: 2026-10-08
- Owner: `server`
- Supersedes: the constitution rule "LLM only on the server: gpt-6-luna, reasoning.effort=none" for the Chat generation. Model, server-only key and everything else of that rule survive. Complements [ADR-042](ADR-042-estimate-total-is-server-arithmetic.md) (server arithmetic stays regardless of effort).

## Context

Every earlier measurement of `low` (S22) widened the kcal band and was rejected. The benchmark of 08/10/2026 ([results](../../../benchmark/RESULTADOS_08_10_2026.md)) measured the same target prompt at both efforts over 155 cases × 3 repetitions, with deterministic checks and a blind judge:

| Measure | new-none | new-low |
|---|---:|---:|
| det_strict | 75,3 % | 84,3 % |
| judge pass | 69,1 % | 79,4 % |
| judge correctness (457 common pairs) | 4,15 | 4,42 |
| "asks what it already knows" | 3,9 % | 1,1 % |
| "claims it recorded" | 7 answers | 1 answer |
| invalid JSON | 6 | 0 |
| p50 / p95 | 3,9 s / 10,2 s | 7,0 s / 16,4 s |
| cost per call | 1× | 1,31× |

The gain concentrates in memory (+26 pp), discovery (+56), habit (+17), photo (+17) and the protein boost (+29); `low` loses on logs (−6, extra clarifying questions). The agent's recommendation was to keep `none` for the latency; the owner decided `low`, accepting the latency.

## Decision

1. **The Chat generation runs at `reasoning.effort=low`.** `config.MODEL` stays `gpt-6-luna`; the effort is a server constant, never a client field.
2. **Compaction and closures stay at `none`** until measured: they are summaries, not decisions.
3. **Latency is a product fact.** The server timeout covers the measured p95 with margin; the app shows a waiting state after 4 s (copy and gold within the current Chat frames, or a design plan if a new state is needed).
4. **Evaluation at the same effort.** The server evaluator and the benchmark run at `low`; the previous full run at `none` is not a baseline for a prompt change after this ADR.

## Motivation and consequences

- Nine points of deterministic accuracy and ten of judge approval on the owner's own cases, with fewer invalid outputs and fewer false "registrei".
- Each turn costs about 240 reasoning tokens and three more seconds at the median; a slow turn can reach 16 s. The app's waiting state and the server timeout absorb it; the owner accepts the wait.
- The clarifying questions `low` adds on clear logs are a prompt matter (S33 rules), not a reason to go back.

## Alternatives considered

- Keep `none` and move the weak families to code: rejected by the owner; memory and discovery cannot be code.
- `low` only for some intents: rejected; the intent is known only after the call.
- `medium`: not measured; cost and latency grow without a measured need.

## Relationships and rollout

S33 delivers the constant, the timeout, the evaluator effort and the evidence; the Chat specification and the HTTP contract record the effort at its Completion. `AGENTS.md` § LLM states the decision from acceptance, with the rollout plan named.
