# Plan — S41 `POST /v1/profile`: one call builds the profile from the onboarding answers

- Status: Aguardando aprovação
- Date: 09/10/2026
- Owning context: `server`
- Affected code: `server/` only: new route `POST /v1/profile`, its instruction block, the output schema and validation, `profile.goal` in the Chat and close prompts, the content-policy pipeline applied to the route, tests, eval cases. Documentation at Completion: [HTTP contract](../../api-contract.md), [v1-chat](../specifications/v1-chat.md), [content policy](../../content-policy/specifications/content-policy.md) (the route under the Chat scope and moderation; [ADR-024](../../content-policy/adrs/ADR-024-content-safety-boundaries.md) status line records the complement).
- Prerequisites: [ADR-057](../../produto/adrs/ADR-057-conversational-onboarding.md) accepted by this approval; [S40](completed/s40-eval-personas.md) delivered (the cases run as personas).
- Related documentation: [perfil-onboarding](../../produto/specifications/perfil-onboarding.md), [memoria-push](../../produto/specifications/memoria-push.md) (fact schema and rules), [ADR-054](../adrs/ADR-054-chat-reasoning-effort-low.md).

Authorization and delivery follow [SDD](../../sdd/README.md). Approval: `Aprovo o plano docs/server/plans/s41-onboarding-profile.md. Implemente o plano aprovado.`

## Objective

The app sends the structured and free-text answers of the guided onboarding once, on confirmation, and receives a typed profile plus up to 30 memory facts in the existing schema, so that day 0 starts with Tali knowing the person ([ADR-057](../../produto/adrs/ADR-057-conversational-onboarding.md) decision 5).

## Scope

1. **Route.** `POST /v1/profile`, header `X-Invite` and `X-Client-Instance-Id` like the Chat, `reasoning.effort=low`, timeout 25 s. IN: `local_time`; `body` (`sex`, `age`, `height_cm`, `weight_kg`); `ceiling_kcal`, `p_target`, `c_target`, `g_target` (the text the user edited, already parsed by the app); `eat_back` (`zero` | `partial` with `pct` | `full`); `slots` (id, name, time); `tone`; `notifications` (`enabled`, `closure_time`); `goal` (`weight_kg`, `date`, both nullable); `answers`: `restrictions`, `measuring`, `foods`, `dislikes`, `equipment`, each a string ≤ 2000 characters (code points), nullable when skipped. OUT: `profile` (the numeric fields echoed, with `ceiling_kcal` and the targets validated ≥ limits), `goal` (echoed, or `null` with `goal_refused: true` when the safety rule refuses it), `facts` (≤ 30: `kind` `permanent` | `dynamic`, `category` `preference` | `portion` | `routine` | `equipment`, `key` ≤ 40, `text` ≤ 160, `slot` for a routine, `kcal`/`p`/`c`/`g` on every routine, `declared: true`), `summary` (pt-BR, ≤ 600 characters, the profile in the chosen tone, numbers first, no judgment about the body), `request_id`.
2. **Instruction block.** One block for the route, written under [ADR-033](../../content-policy/adrs/ADR-033-global-chat-example-provenance.md): the model receives the answers delimited as data and returns the facts and the summary; the priority order (restrictions, routines with the slot the food belongs to by time, equipment, measuring style as a `portion` fact, dislikes and likes as `preference`) and the cap are instructions and are enforced in code: the server truncates to 30 by that order, drops facts out of schema, drops a routine without the four numbers, and dedupes by `key`.
3. **Content policy.** The five free-text fields go through the same pipeline as the Chat text (scope, injection, moderation, correlation, metadata-only logging); a blocked field returns HTTP 400 `content_policy_blocked` with the field name, the app shows the error screen. A goal below the safety limits (the thresholds are written in the content-policy specification at Completion) is refused with `goal_refused: true` and the profile is built without it; a restriction is health data used only to avoid or prefer foods, the instructions say so.
4. **`profile.goal` in the Chat and close prompts.** The `PROFILE` line gains `meta {kg} kg até {data}` when present; the `duro` tone and the week closure may cite the overshoot as days of deficit lost (deficit = ceiling gap), never a remark about the body. The dev chat log metadata records `goal: true | false`, never the numbers.
5. **Dev log.** The route logs metadata only (request id, counts of facts by category, `goal_refused`, latency); under ADR-015 the dev capture keeps the answers like Chat text, production keeps nothing.
6. **Tests.** Unit: schema limits (422), truncation to 30 in priority order, routine without numbers dropped, blocked field → 400, goal refusal. Eval cases: one per persona of S40, each from that persona's answers.

## Out of scope

- The app (A71). The Figma golds (D27).
- Storing anything on the server. A second call to refine the profile.
- Memory rules on the device (unchanged; the cap 50 is a client change in A71).

## Validation

1. `python -m pytest server/tests -q` passes.
2. Smoke after the dev deploy: the seven persona cases once, plus one blocked field and one refused goal (synthetic, no model call for the blocked one). Cap: **9 model calls**.
3. Three-turn HTTP smoke of the Chat after the deploy (free), with one persona that has a goal, to see `PROFILE` carry it.
4. `node tools/check-docs.mjs` passes.

## Results

<Filled at Completion.>
