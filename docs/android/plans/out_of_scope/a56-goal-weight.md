# A56 — Goal weight and date in the profile

- Status: Fora de escopo
- Date: 2026-10-06
- Owner: `android` (product decision in `produto`; a paired server plan and a design plan are expected)
- Prospective delivery boundary: documentation first (product ADR and spec changes), then one `design` plan (O1 or a new field in Config), one `apps/android/` plan and one `server/` plan, never in the same `/goal`
- Authority: owner decision of 06/10/2026, after reviewing the Grok Bot transcripts: "o app deve ter uma meta de peso".
- Reason: business order. [ADR-043](../../../produto/adrs/ADR-043-plan-objective-protein-and-meal-window.md) and [ADR-044](../../../produto/adrs/ADR-044-assistant-tone-and-closures.md) come first; the goal weight is what lets the `duro` tone and the week closure frame an overshoot as deficit lost against a dated goal ("o fim de semana come o déficit de 4–5 dias bons"), which they cannot do with the ceiling alone.

This plan is not approved, implemented or cancelled. Do not run `/goal` while it remains out of scope.

## Future objective and scope

The profile gains an optional goal: target weight (kg) and date, next to the current weight of O1. The server receives both in `PROFILE`; the `duro` tone and the week closure may cite the weekly overshoot against the deficit the goal implies; the day closure may show the trend. Open questions for its ADR: where the field lives (O1, or a Config row only); whether the current weight is updated over time and how (Config, Chat); what the app shows of the goal (a Home line, a weekly card line); the formula that turns an overshoot into "days of deficit" (the constitution forbids TDEE as a product, so the deficit is the ceiling gap, not a maintenance estimate); limits of the `duro` copy about weight (ADR-044 forbids remarks about the body; a goal the user set is the exception to define precisely); content-policy review of a weight goal under [ADR-024](../../../content-policy/adrs/ADR-024-content-safety-boundaries.md) (a very low target or an unrealistic date is a safety signal).

## Residual risk and dependencies

Until then the tones and the closures speak in kcal, protein and days over, never in weight or deficit. Depends on [S24](../../../server/plans/completed/s24-protein-first-plan.md), [S25](../../../server/plans/cancelled/s25-tone-and-closures.md) and [A55](../cancelled/a55-tone-choice-and-closures.md) delivered, and on a content-policy decision for the weight-related copy.

## Reconsideration conditions

An explicit owner decision to reactivate once ADR-043 and ADR-044 are delivered on dev, with the content-policy question answered in the ADR that reactivation writes.

## Re-entry

Preserve the ID and dated history. Refresh scope, costs, dependencies and validation. Move to `plans/` as `Aguardando aprovação`, update indexes/links and obtain explicit approval naming the active plan before implementation. Follow [SDD](../../../sdd/README.md).

## Future acceptance

To be defined with its ADR: a goal set in the profile reaches the server; a week closure under `duro` cites the overshoot against the goal without any remark about the body; a goal under the safety limits is refused with the ADR-024 handling.
