# Plan — S27 Planned slot in DAY

- Status: Cancelado (07/10/2026, dono: "ao invés de ter vários planos, concentre tudo num plano só, 1 para server e 1 para app"); scope carried unchanged into [S30](../s30-tone-formatting-planned-slot.md)
- Date: 06/10/2026
- Owning context: `server`
- Executable boundary: `server/` only: `ChatDaySlot.status` accepts `planned` with its numbers, DAY serialization, the meal window reservation, the `log` and `plan` rules (comparison clause), the meal-change target rules (a planned slot is not occupied), tests and evaluation cases. No new route.
- Related documentation: [ADR-046](../../../produto/adrs/ADR-046-planned-meal-reservation.md) (accepted on 2026-10-06 with the approval of D18 and this plan), [ADR-043](../../../produto/adrs/ADR-043-plan-objective-protein-and-meal-window.md), [ADR-032](../../../produto/adrs/ADR-032-acrescimos-e-correcoes-de-refeicoes.md), [server Chat specification](../../specifications/v1-chat.md), [HTTP contract](../../../api-contract.md).
- Prerequisites: [S24](../completed/s24-protein-first-plan.md) delivered (the window module this plan extends). Independent of S25 and S26.

Approving this plan accepts ADR-046. Authorization and delivery follow [SDD](../../../sdd/README.md). Approval: `Aprovo o plano docs/server/plans/s27-planned-slot.md. Implemente o plano aprovado.`

## Objective

A slot the user reserved with a plan reaches the model as `planned` with its numbers, reserves exactly those kcal in the meal window, and a log into it is compared with the plan.

## Scope

### 1. Contract

- `day.slots[].status` gains `planned`; with it, `text`, `kcal`, `p`, `c`, `g` carry the plan (same bounds as `eaten`). A legacy client never sends it. Any other combination is a 422.
- DAY serialization: `{id}:planned ({kcal}kcal, {p}P {c}C {g}G, text="...")`. `eaten_*` and `remaining_*` exclude planned slots (nothing eaten).

### 2. Meal window

- `meal_window.py`: a `planned` slot is reserved by its own kcal, replacing the expected value; when it is the plan's target slot it is not reserved (the new plan replaces it). `WINDOWS` lists it as `{slot}: planejado {kcal} kcal`.

### 3. Rules

- `log` (both branches): a log whose target slot is `planned` is a normal record (`new` operation; a planned slot is not occupied for ADR-032: `base_slot` null, no addition); the reply includes one clause with the difference to the plan in kcal (`{+n} kcal sobre o plano` or `{−n} kcal abaixo do plano`), numbers from DAY, never recomputed; under `duro` (S25, when delivered) one adjustment for the open slots.
- `plan`: a plan for a slot that is `planned` is allowed and says it replaces the reservation only if the user reserves again; the model never reserves.
- `product` rule: the model never says it reserved or locked a meal.

### 4. Evaluation

- Cases, tag `s27`: a dinner log into a planned dinner (expect `new`, `base_slot` null, the difference clause with the right sign, `record auto`); a plan for a planned slot (expect `plan`, no claim of reserving); a plan for another slot with a planned dinner (expect `plan_budget.reserved` carrying the planned kcal and `limit_kcal` reduced by it); a legacy-shape request unchanged.
- Regression (unattended budget, [autonomous run](../../../sdd/autonomous-run.md)): `--tag s27 --repeat 3`, `--tag s24 --repeat 1`, then the sentinel set `--tag s22 --tag s23 --tag s24 --tag recipe --repeat 1`. No full-suite run. Ceiling for this plan: **US$ 0.10**.

### 5. Dev deploy

After validation: `tools/deploy-gcp.ps1`, code only; smoke with one planned slot.

### Specification changes at Completion

- [v1-chat](../../specifications/v1-chat.md): rule 3 (planned slot in DAY and WINDOWS), rules 4 and 5 (comparison clause; planned is not occupied), observability. Provenance line.
- [HTTP contract](../../../api-contract.md): `status: planned` and its fields.
- ADR-046 status to Accepted (if not already by D18 or A58); ADR-032 status line records the complement.

## Out of scope

- Client (A58), design (D18), tone and closures (S25), formatting (S26), production (blocked by the [production gate](../../../content-policy/production-gate.md)).

## Validation

1. `server/.venv/Scripts/python -m pytest server/tests -q` passes, including the `planned` validation, serialization, window and meal-change tests.
2. Evaluation as in scope 4 within its US$ 0.10 ceiling; the `s27` cases 3/3 each.
3. Dev deploy and smoke; request ids recorded.
4. `node tools/check-docs.mjs` passes.

## Results

Planning only.
