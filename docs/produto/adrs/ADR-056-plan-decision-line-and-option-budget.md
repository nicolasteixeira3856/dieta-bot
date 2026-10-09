# ADR-056 — Decision line and per-option budget in a plan with alternatives

- Status: Accepted (2026-10-09, owner approval of [S39](../../server/plans/pending_manual_validation/s39-plan-decision-line-and-option-budget.md))
- Date: 2026-10-09
- Context: `produto`
- Supersedes: partially [ADR-039](ADR-039-plan-cooking-and-budget-choice.md) ("the reply never states whether the dish fits or by how much"): for a plan with options the decision line states it, with numbers the server writes; the over/fit choice, the `Passa {over} kcal` note and the adjustment call survive. Partially [ADR-051](ADR-051-plan-option-identity-and-chat-discovery.md) § 1 (the reply opens with `Opção 1: {name}`): the reply opens with the decision line; ids, `options[]`, the later reference and the digest rule survive.

## Context

On 09/10/2026 the owner compared, on the dev app, one answer of Tali with the same request answered by Grok: a dinner with two alternatives the user named, a skip of the afternoon meal in the same message, a preparation appliance named. The dev conversation log ([ADR-015](../../server/adrs/ADR-015-log-conversa-dev.md)) showed six general gaps, none tied to the foods of that case:

1. The app shows only the first line of a plan with options ([A67](../../android/plans/completed/a67-plan-options-and-discovery.md), ADR-051): that line was `Opção 1: …`, so the recommendation the model wrote in its third paragraph never reached the screen.
2. The meal window served to the model and the `plan_budget` counted the meal the same message skipped ([ADR-043](ADR-043-plan-objective-protein-and-meal-window.md) reservation, [ADR-047](ADR-047-skips-alongside-other-actions.md) skip): the limit was wrong by that meal's reservation and the `Passa {over} kcal` note contradicted the recommendation.
3. The budget and the closing lines ([ADR-043](ADR-043-plan-objective-protein-and-meal-window.md) decision 6) were computed on option 1 because the action's estimate is option 1 by contract, not on the option the reply recommended; a closing line with a few kcal for the last meal followed.
4. The appliance named in the message did not make it a cooking plan: no steps, no `recipe` ([ADR-052](ADR-052-saved-recipes.md)).
5. The components the message left open were completed with generic assumptions although RECENT had the user's own components for the same kind of dish; the assumption was stated twice.
6. The weekly pattern of the `duro` critique ([ADR-044](ADR-044-assistant-tone-and-closures.md)) was repeated in every answer of the day.

Grok's answer is the UX reference: the decision in the first sentence, the day with that dish, the other alternative quantified, then the recipe. Its arithmetic is not the reference: the model's numbers stay out of the product ([ADR-039](ADR-039-plan-cooking-and-budget-choice.md), [ADR-042](../../server/adrs/ADR-042-estimate-total-is-server-arithmetic.md)). Per [ADR-033](../../content-policy/adrs/ADR-033-global-chat-example-provenance.md) the case yields general rules; it is not an example.

## Decision

1. **Decision line.** A plan with options opens with one line that names the chosen option and gives one numeric reason: its kcal and protein, whether it fits the window of its meal, and by how many kcal the other option passes that window (or that it fits too). The model writes the line in a fixed form and chooses; the server rewrites the numbers of the line from its own arithmetic, as it does for the closing lines (ADR-043 decision 6). The options follow the line, as ADR-051 presents them.
2. **Chosen option.** The option the decision line names is the chosen option (by name; option 1 when the line is missing or names none). The action's `estimate`, its `plan_budget`, the protein numbers of the critique and the closing lines are those of the chosen option. Each option also carries its own excess over the window (`over_kcal`, 0 when it fits), the server's number.
3. **Comparison.** Two or more alternatives the user names with a choice question are a plan with options: each option is the user's dish as named (its foods and amounts), without the leaner/indulgent pair of ADR-051 open requests; what the message leaves open follows decision 5.
4. **Cooking method.** A plan that names an appliance or a preparation method for a dish the user will make is a cooking plan (ADR-039 COOKING): up to five steps with temperature and time for the chosen option, `recipe` filled (ADR-052). A plan that names none keeps no steps.
5. **Assumptions from the user's records.** Components the message leaves open (bread, cheese, sauce, side, dressing) are completed from RECENT records of dishes of the same kind before any generic assumption, and the reply says each assumption once, in one clause.
6. **Same-message skip.** A meal the same message skips reserves nothing: the window, the `plan_budget`, the decision line and the closing lines are computed with it skipped. When the message names a meal of today next to a skip cue, the BUDGET and WINDOWS lines served to the model already leave that meal out.
7. **Once.** The weekly pattern of the `duro` critique is named once a day: not when an assistant turn of today in HISTORY already names it. An assumption is said once per reply.
8. **App.** The bubble of a plan with options shows the reply's lead text (the decision line) above the option blocks and the rest of the reply (critique, skip clause, closing lines) below them; only the `Opção {n}` paragraphs are left to the blocks. Under each option's totals: `Cabe na janela do {meal}` or `Passa {n} kcal da janela do {meal}` from the server's `over_kcal`, and the day with that option, `Dia: ~{kcal} de {ceiling} kcal · P {p} de {target}`, computed by the app as the projection of [ADR-053](ADR-053-visible-memory-screen.md)/A64 (eaten + option against the effective ceiling and the protein target). The budget choice of ADR-039 is shown for the chosen option.

## Motivation

- The only line the app shows of a plan with options must be the decision. A line that repeats option 1 is noise.
- Arithmetic stays on the server (ADR-039 evidence: the model decided "fits" from its own low estimates). The model decides between dishes, which needs judgment about protein, taste and what the user asked; the server writes the numbers that justify it.
- A comparison is answered on the user's own dishes. Inventing bread and cheese for both alternatives moves the estimate away from what will be eaten.
- A skip stated in the same message is part of the day's state for that answer, not only for the next one.
- Grok's structure (decision, quantified alternative, recipe) is reproducible without moving arithmetic to the model.

## Consequences

### Positive

- The recommendation reaches the screen with the numbers that support it, and the budget note agrees with it.
- A dish to be cooked gets its steps and the save button of ADR-052.
- Fewer repeated sentences in the `duro` tone.

### Negative

- One more fixed form the server parses (the decision line), with the same fragility as the closing lines: a line the model writes off-form keeps the model's numbers until the server finds it; the plan tests cover the form.
- A served BUDGET line of a message whose skip cue the server does not recognise still counts that meal; the numbers after generation are right either way.
- The `chatO` gold changes (D26).

## Alternatives considered

### Let the model compute the fit and the day

Rejected: ADR-039's evidence (the model decides "fits" from its own low estimates) and ADR-042 (totals are server arithmetic).

### Show the whole reply in the bubble above the blocks

Rejected: the `Opção {n}` paragraphs would duplicate the blocks; the lead/trailing split keeps the rest of the reply.

### Build the decision line on the server without the model

Rejected: the choice between two dishes needs the model's judgment about protein, taste and what the user asked; only the numbers are the server's.

## Relations

- Affected specifications: [v1-chat](../../server/specifications/v1-chat.md), [HTTP contract](../../api-contract.md), [product Chat](../specifications/chat.md).
- Related ADRs: [ADR-033](../../content-policy/adrs/ADR-033-global-chat-example-provenance.md), [ADR-039](ADR-039-plan-cooking-and-budget-choice.md), [ADR-043](ADR-043-plan-objective-protein-and-meal-window.md), [ADR-044](ADR-044-assistant-tone-and-closures.md), [ADR-047](ADR-047-skips-alongside-other-actions.md), [ADR-051](ADR-051-plan-option-identity-and-chat-discovery.md), [ADR-052](ADR-052-saved-recipes.md).
- Consuming contexts: [server](../../server/README.md) ([S39](../../server/plans/pending_manual_validation/s39-plan-decision-line-and-option-budget.md)), [android](../../android/README.md) ([A70](../../android/plans/pending_manual_validation/a70-option-fit-and-projection.md)), [design](../../design/README.md) ([D26](../../design/plans/completed/d26-release-2-option-decision.md)).

Depois de aceito, o corpo deste ADR não se edita. Só a linha `- Status:` muda, para registrar substituição total ou parcial por um ADR novo.
