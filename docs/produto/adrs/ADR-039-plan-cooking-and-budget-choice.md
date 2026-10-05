# ADR-039 — Cooking help and the over-budget choice in a plan

- Status: Proposed (2026-10-05; accepted with the owner's approval of [S21](../../server/plans/s21-plan-cooking-and-budget-choice.md))
- Date: 2026-10-05
- Context: `produto`
- Supersedes on acceptance: partially [ADR-023](ADR-023-chat-v2-memoria-v2.md), decision 3 (preparation "in a few lines"; the plan answer is final), and the sentence of [ADR-026](ADR-026-perguntas-antes-da-estimativa.md) decision 1 "`plan` keeps its rule (never asks)", only for the budget choice below. The rest of both remains: the app computes the projected day, **Registrar assim**, a plan never asks about the food and assumes instead. Adds gold `chatRB`. Extends `POST /v1/chat` of the [contract](../../api-contract.md) additively.

## Context

Owner feedback on 2026-10-05, after a recipe request in the closed test: the answer repeated the listed foods as the dish and suggested nothing that would improve it. The owner expects cooking help: a real dish, sensible additions, proportions and steps. When the dish does not fit what is left of the day, the owner wants to be asked whether going over is fine before the dish is shrunk. An answer of "fine" keeps the dish; "adjust" must fit.

Evaluation in the dev evaluator (synthetic cases per [ADR-033](../../content-policy/adrs/ADR-033-global-chat-example-provenance.md), `gpt-6-luna`, 3 repetitions):

- Current instructions: a recipe added no food to the listed ones in 8 of 9 runs at `none` and 9 of 9 at `low`. `medium` did not change the dish and took up to 32 s.
- A general cooking rule produced dishes with optional additions and numbered steps in all recipe cases, with no regression in the full suite.
- With a budget rule in the prompt, the model followed an answer the user had already given in every run ("go over" kept the dish; "adjust" rebuilt it within the budget). It did not reliably decide when to ask: it asked in 1 to 3 of 3 runs per over-budget case, and in 0 of 6 replays of the diagnostic turn. The same dish was estimated anywhere from 570 to 1080 kcal; when the estimate came out low, or a later meal was forgotten, the model declared that the dish fit. `low` effort passed 26/30 runs against 24/30 at `none`, roughly doubling p95 latency.

ADR-023 already moved the projected day out of the model ("the AI can miscount; the app cannot"). The over-budget decision is the same kind of arithmetic.

## Decision

### 1. Cooking help

A plan that asks for a recipe or for what to make names a real dish. The foods the user has are the base. The model may add up to 3 common, low-cost foods that improve flavor, volume, protein or satiety for few kcal; each addition is marked `(opcional)` in the reply and is part of the items and the estimate. The reply lists the ingredients with grams and up to 5 numbered steps with temperature and time. This replaces "preparation in a few lines" for recipes. Other plans keep their current shape.

### 2. Later meals stated in the conversation

When the current message or MEMORY says another meal is still to be eaten today, the model returns it as a reservation: a short label and its estimated kcal. A reservation is never part of the plan's items, `meal_text` or any record. It counts only for this plan's budget check. The Home budget, `reservedUpcoming` and records are not changed by it.

### 3. Deterministic over-budget check

The server computes, never the model:

- dish budget = `day.remaining_kcal` − sum of the reservations;
- over = the plan's kcal − dish budget, when positive.

No `remaining_kcal` (older clients, another day) means no check. The reply never states whether the dish fits or by how much; the app shows it from the structured result, as with the projected day.

### 4. The choice

The model also reports the user's budget choice from the conversation, if any: `over_ok` (going over is fine), `fit` (adjust it) or none. A choice stated in the original request counts.

- **Over, no choice:** below the plan the app shows the fixed line `Passa {over} kcal do que sobra.` (with `Reservei {kcal} kcal para {label}.` when there is a reservation) and two pills in the action position: **Pode passar** · **Ajustar para caber**. They replace **Registrar assim** while the choice is pending.
- **Pode passar:** local, no AI call. The pills leave, **Registrar assim** returns, the plan is unchanged and the projected day shows the overage in `bad` as today.
- **Ajustar para caber:** sends `Ajusta para caber em {budget} kcal.` with the target through the normal send/loading/failure/retry flow, as **Forçar estimativa** does. The server asks for the same dish rebuilt within the target, shrinking calorie-dense foods first and keeping the additions where possible. It verifies the result: an adjusted plan above the target is not returned as adjusted (one retry; if still above, the over state is returned and the choice shows again).
- **Nothing left to adjust to** (dish budget below 1 kcal): no choice is shown; the projected day shows the overage.
- **Typed answer:** the same rules apply when the user types the choice instead of tapping; the model's reported choice drives it. `over_ok` shows no question; `fit` is checked against the target like the pill.

### 5. Contract and screens

The fields are additive and gated by a client capability flag, like `meal_changes`. Clients without it get no budget fields. The reply stops stating the overage for every client, so the instructions stay one cached text; those clients still see it in the projected day. New gold `chatRB` (plan over budget with the choice) in both themes, drawn through the Figma review gate. `chatR` is unchanged.

## Motivation

- The owner asked for cooking help and for the choice; both are product behavior, not prompt tuning.
- The model extracts intent well (100% when the choice was stated) and does arithmetic poorly (it decided "fits" from its own low estimates). The decision keeps each part on the side that is good at it.
- The check reuses the inputs the app already sends (`remaining_kcal`), so the line matches the projected day.
- **Pode passar** costs no request; only **Ajustar** needs the model.

## Consequences

### Positive

- Recipe answers become usable dishes with steps.
- The question appears exactly when the numbers say the dish is over, not when the model notices.
- An adjusted plan cannot silently exceed its target.
- A later meal the user mentions is accounted for without touching the Home budget.

### Negative

- Longer fixed instructions (about 200 tokens), cached.
- A new gold per theme and a design plan before the Android plan.
- The reservation's kcal is the model's estimate; the app shows it so the user can correct it in the chat.
- Optional additions are inside the estimate; a user who skips them records a slightly higher number unless they say so.
- One more retry path on the server for the adjusted plan.

## Alternatives considered

### The model decides and asks in the reply

Tested. It asked in 1 to 3 of 3 runs per over-budget case and never on the diagnostic turn, because its estimate decides and that estimate drifts. Rejected.

### Always shrink the dish to fit

Overrides users for whom going over is fine. The owner wants to be asked. Rejected.

### Keep stating the overage only (current rule)

The statement depends on the same unreliable arithmetic and offers no adjustment path. Rejected.

### The check only in the app

The app could compute it, but the server needs the same target to enforce an adjusted plan and to handle a typed answer. One check on the server, from the inputs the app sends, serves both. Rejected.

### Higher reasoning effort

`low` gained 2 of 30 runs and doubled p95 latency; `medium` did not change the dishes. It does not remove the arithmetic from the model. Rejected for this decision; effort stays as configured.

### Use `reservedUpcoming` for a meal mentioned in the chat

Would change the Home budget from one chat sentence. Out of scope.

## Relations

- Specifications affected at Completion: [chat](../specifications/chat.md) rule 16, [v1-chat](../../server/specifications/v1-chat.md) rule 3c, [api-contract](../../api-contract.md), gold inventory in [qa](../../qa/README.md).
- ADRs related: [ADR-023](ADR-023-chat-v2-memoria-v2.md), [ADR-026](ADR-026-perguntas-antes-da-estimativa.md), [ADR-031](../../design/adrs/ADR-031-figma-source-of-truth.md), [ADR-033](../../content-policy/adrs/ADR-033-global-chat-example-provenance.md).
- Delivery: server plan S21 (instructions with the ADR-033 provenance record, fields, check, adjusted-plan verification, evaluation cases); design plan D12 (`chatRB`); Android plan A50 (pills, line, flag), after D12 is `Concluído` and S21 is deployed to dev.

After acceptance, the body of this ADR does not change. Only the `- Status:` line changes, to record a total or partial supersession by a new ADR.
