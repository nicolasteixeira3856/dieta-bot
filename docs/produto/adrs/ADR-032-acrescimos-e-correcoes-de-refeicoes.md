# ADR-032 — Explicit meal additions and revisions

- Status: Accepted (2026-10-04, explicit owner approval of S18); complemented by [ADR-042](../../server/adrs/ADR-042-estimate-total-is-server-arithmetic.md) (`new`/`revise` totals recomputed from the items instead of refused) and by [ADR-046](ADR-046-planned-meal-reservation.md) (a planned slot is not occupied: a log into it is `new` with `base_slot` null); partially superseded by [ADR-050](ADR-050-typed-actions-per-message.md) (a meal change is a field of a `log` action)
- Date: 2026-10-04
- Context: `produto`
- Supersedes on acceptance: partially [ADR-017](ADR-017-registro-consolidado.md), decision 2 (re-estimating an existing meal on every addition), and [ADR-028](ADR-028-registro-autonomo.md), decision 3 (one replacement presentation and destination action for every update). Their other rules remain.

## Context

The owner requested these plans after an investigation of meal continuation, ambiguous replacement wording and a consolidated estimate recorded in another slot. Investigation evidence belongs to [S18](../../server/plans/completed/s18-meal-additions-and-revisions.md#discovery-evidence). Requesting the plans authorizes documentation; it does not accept this ADR or authorize implementation.

The current design has one persisted record per meal. Its technical replacement operation is exposed to the user even when the intended action is to add one food. Re-estimating unchanged food also permits numbers to drift. Moving an unrecorded consolidated proposal to another slot can count the original food again.

## Decision proposed

### 1. Separate the intended operation from storage

Distinguish a new meal, an addition to a meal and a revision of a meal. Keep one consolidated record per slot, today's recording boundary, the existing uncertainty/clarification rules and confirmation before changing an occupied slot. The server stays stateless; Android owns persistence and verifies the actual state before writing.

- **Addition:** preserve the previous record's foods and kcal/P/C/G. Estimate only the added food. The result is the previous values plus the added values, calculated by code. A clarification of the same pending addition does not add that food twice.
- **Revision:** an explicit change to already described food, quantities or preparation may change the recorded totals. Re-estimate the affected meal when the previous aggregate lacks item-level nutrients. Do not describe that revision as an additive amount or pretend the prior total was preserved.
- **New meal:** follow the existing recording rules. Food similarity alone does not establish an addition. When a named occupied meal could mean either a new addition or replacement, resolve that intent before changing its record.

An already recorded addition, a second portion and a correction to a pending estimate are different events. Preserve this distinction in structured state and context; do not deduplicate solely by food names.

### 2. Resolve the meal from eating context

An explicit meal in the current message wins. Otherwise follow the same meal being continued, using eating time, user statements and the day's recorded meals. An interleaved correction about breakfast must not by itself make a dessert part of breakfast or a later empty snack.

A dessert food is not automatically lunch: it can itself be a snack. Use the relationship to the earlier meal, not a food-name whitelist. If an addition has two plausible targets, ask a short question through the existing clarification flow instead of choosing the next empty meal.

The existing clarification-round limit still applies. Forcing a portion estimate never authorizes inventing the operation or the destination. If the operation remains unresolved when no more clarification may be asked, return a non-actionable explanation; do not write a guessed update. If only the destination of a known addition is unresolved, offer the estimate of the addition with a manual destination choice.

### 3. Show the addition and the resulting total separately

Product-copy templates for an addition to an occupied meal:

```text
{added_food_and_quantity}: +{added_kcal} kcal
Já registrado no {slot}: {previous_kcal} kcal
Total do {slot}: {result_kcal} kcal

Adicionar ao {slot}?
[Adicionar] [Escolher outra refeição]
```

The macros displayed as the meal total must also be the resulting meal's macros. The added number is not labeled as the total. A clear label distinguishes an estimate from a completed local record; AI prose never claims persistence before the receipt.

Revision templates:

```text
Atualizar {slot}?
Antes: {previous_kcal} kcal
Novo total: {result_kcal} kcal
[Atualizar] [Cancelar]
```

These are states of the existing Chat, not new product screens. [D9](../../design/plans/completed/d9-chat-meal-updates.md) owns their proposed gold list and visual gate. Existing new-meal cards remain valid when there is no previous amount to explain.

### 4. Destination changes have a defined unit

Before recording an addition, **Escolher outra refeição** selects the destination of the added foods only:

- Empty destination: record only the addition. A skipped destination follows the existing reversible skip-removal behavior.
- Occupied destination: present that destination's previous amount plus the same addition and ask for confirmation.
- The initial source meal remains unchanged. Never copy its foods or totals into the new destination.

A pending revision stays attached to its source record; Cancelar expires the proposal without changing data. To move an already recorded whole meal, use its receipt's existing **Trocar refeição**, with source removal and target update in one transaction. This action continues to mean the whole consolidated meal, including any additions already confirmed.

### 5. Preserve reversibility and consistency

Store enough structured context to distinguish the added portion from a whole-meal proposal after screen recreation or process restart. Before confirming, compare the source/target state with the state used to build the displayed proposal. A changed day, wipe, changed meal or removed slot invalidates it; no silent overwrite or rebasing after confirmation.

Desfazer restores exactly the prior slot and memory state. After a rerouted addition, it affects its actual destination and never removes food from the original meal. An expired or already applied proposal cannot be recorded again.

Do not silently truncate existing foods when composing an addition's description. Missing or malformed operation metadata must never be interpreted by parsing the prose. Compatibility and failure behavior are specified in S18 and consumed by A47.

## Rationale and consequences

- The displayed amount explains both the user's new item and its effect on the day.
- Preserving the prior aggregate prevents unrelated foods and macros from drifting during an addition.
- Separate added nutrients enable safe destination selection without another model call.
- Revisions remain possible even though historical records do not contain item-level nutrition.
- Costs: an additive API capability, persisted proposal metadata, additional Chat golds and migration/transaction tests. No food database, account system or new provider is introduced.
- Nutrition remains an estimate. Deterministic addition validates accounting, not the nutritional accuracy of the new food. Numeric consistency must allow energy sources outside P/C/G, including alcohol; it must not fabricate macros to force an equality.

## Alternatives considered

1. **Change only the AI wording.** Useful, but does not preserve prior numbers or make destination changes safe.
2. **Always re-estimate the entire meal.** Retains the current architecture but allows unrelated values to change on a pure addition.
3. **Always add the returned total.** Double-counts the previous meal and cannot support corrections/removals.
4. **Automatically update every occupied slot.** Removes a safeguard the owner previously required; not proposed here.
5. **Classify desserts by food name or time alone.** Misclassifies genuine snacks and unusual schedules.

## Relations and adoption

- Delivery: [S18](../../server/plans/completed/s18-meal-additions-and-revisions.md), [D9](../../design/plans/completed/d9-chat-meal-updates.md), [A47](../../android/plans/completed/a47-chat-meal-updates.md).
- Specifications: [Chat](../specifications/chat.md), [server Chat](../../server/specifications/v1-chat.md), [HTTP contract](../../api-contract.md), [Room](../../android/specifications/room-v2.md).
- Related decisions: [ADR-026](ADR-026-perguntas-antes-da-estimativa.md), [ADR-029](ADR-029-fatos-temporarios-compactacao.md), [ADR-031](../../design/adrs/ADR-031-figma-source-of-truth.md).
- Implementation approval of S18 includes this linked product decision; no separate confirmation is required. Record that acceptance and update only the predecessor status lines with the partial supersession during S18. Rewrite each live specification at its owning delivery's Completion, documenting the capability boundary during rollout.

Once accepted, this ADR's body is immutable. A subsequent change requires a successor ADR; only its status line may change.
