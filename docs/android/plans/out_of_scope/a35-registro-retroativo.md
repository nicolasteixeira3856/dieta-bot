# A35 — Retroactive record (a meal of another day)

- Status: Fora de escopo; reactivated on 2026-10-09 by owner decision inside [ADR-058](../../../produto/adrs/ADR-058-extras-and-history.md), [S42](../../../server/plans/s42-extras-and-other-day.md) and [A72](../a72-extras-and-history.md) (this file stays as the dated deferral history)
- Date: 2026-10-01
- Owner: `android` (product decision in `produto`; a paired server plan is expected)
- Prospective delivery boundary: documentation first (product spec + ADR), then one `apps/android/` plan and one `server/` plan, never in the same `/goal`
- Authority: owner decision of 01/10/2026 in the autonomous-record brainstorm: "Isso irá abrir registro retroativo, que é outra feature."
- Reason: business scope. [ADR-028](../../../produto/adrs/ADR-028-registro-autonomo.md) records only today's meals; recording another day changes the day model, the Home and the prompt, and is a feature of its own.

This plan is not approved, implemented or cancelled. Do not run `/goal` while it remains out of scope.

## Future objective and scope

Let the user record (or fix) a meal of a past day from the Chat, e.g. "ontem jantei pizza" or "na janta comi X" sent after midnight about last night. Open questions for its ADR: how far back; how a past day is shown and edited (Home has only today); whether `day` totals, eat-back credit and push reminders of the past day change; how the server marks the target date (S14 already returns `meal_day: other`); receipts and undo for a past day.

## Residual risk and dependencies

Until then, a message about another day is never recorded: the reply says the Chat records only today's meals (ADR-028 decision 7). A user who forgets to record a meal before midnight cannot add it. Depends on [S14](../../../server/plans/completed/s14-registro-autonomo.md) (`meal_day`) and [A34](../completed/a34-registro-autonomo.md).

## Reconsideration conditions

An explicit owner decision to reactivate, ideally backed by the dev conversation log (`record: none_other_day`, S14) or tester feedback showing how often other-day meals are sent.

## Re-entry

Preserve the ID and dated history. Refresh scope, costs, dependencies and validation. Move to `plans/` as `Aguardando aprovação`, update indexes/links and obtain explicit approval naming the active plan before implementation. Follow [SDD](../../../sdd/README.md).

## Future acceptance

To be defined with its ADR: a past-day meal recorded from the Chat lands in that day, the past day's totals reflect it, and today's budget is unchanged.
