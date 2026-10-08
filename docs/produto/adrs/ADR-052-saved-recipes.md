# ADR-052 — Saved recipes

- Status: Proposed (owner direction of 07/10/2026 in `benchmark/MELHORIAS_CHAT_07_10_2026.md` § 5; accepted with the approval of S38)
- Date: 2026-10-08
- Context: `produto`
- Supersedes: partially [ADR-012](ADR-012-chat-home-perfil.md) (the screen inventory gains the recipe list and detail, entered from Config). Complements [ADR-039](ADR-039-plan-cooking-and-budget-choice.md) (cooking help in a plan) and [ADR-017](ADR-017-registro-consolidado.md) (one record per meal).

## Context

A recipe the model builds in a plan is lost after the turn: the owner repeats "aquela pizza de pão sírio" and gets a different dish with different numbers. The benchmark of 08/10/2026 ([results](../../../benchmark/RESULTADOS_08_10_2026.md)) measured the recipe family at 0 % (current prompt: the capability does not exist) against 86–89 % (target: index plus full recipe on demand).

## Decision

1. **Recipe entity in the app.** Name, ingredients with grams and kcal, steps, totals (kcal, P, C, G), yield and portion, version, origin (plan turn id). Saving a recipe does not record a meal. Editing creates a new version; a record keeps the version it used.
2. **Save from the plan bubble.** A plan that is a recipe (cooking help with steps) offers "Salvar receita" under the message (ADR-048 actions in the thread). Config lists the saved recipes (most recent first, name and kcal · P/C/G) and opens a detail.
3. **What the model sees.** Every Chat turn sends the recipe index (`recipes[]`: id, name, totals, key foods; at most 30). When the app finds a recipe named in the message, it sends that recipe complete, only on that turn (`recipe_full`).
4. **Actions.** `recipe_recall {id}` answers "lembra a receita X?" with the full recipe from the data sent; a `log` by recipe copies the version's numbers without re-estimating; "a mesma, mas com Y" is a `log` with `meal_change` over the recipe; a plan "ajustada pra hoje" rebuilds from the full recipe under the protein and window rules with `recipe_id` set. A recipe that is not saved is answered as such, never invented.
5. **Memory stays memory.** Recipes are not facts; a "ficou boa" comment may create a `liked` fact (ADR-051) that names the recipe.

## Motivation

- Deterministic numbers for a repeated dish, like the routine of ADR-023 for a repeated meal.
- The index keeps the prompt small; the full recipe costs tokens only when named.

## Consequences

### Positive

- The dish the user liked comes back identical; the server does not re-estimate what the app knows.

### Negative

- Two product screens with golds (design plan), a Room migration, local search by name and ingredient, and the index on every turn (about 30 lines). Accepted.

## Alternatives considered

### Keep recipes as permanent memory facts

Rejected: a fact is one line; a recipe has items, steps and versions, and 30 permanent facts are the memory cap.

### Server-side recipe storage

Rejected: the server stores nothing per user (constitution).

## Relations

- Specifications affected: [chat](../specifications/chat.md), [memoria-push](../specifications/memoria-push.md) (Config entry), [v1-chat](../../server/specifications/v1-chat.md), [HTTP contract](../../api-contract.md), [Room](../../android/specifications/room-v2.md), [gold inventory](../../qa/README.md).
- Related ADRs: ADR-012, ADR-017, ADR-039, ADR-048, ADR-050, ADR-051.
- Consuming contexts: [design](../../design/README.md) (D23), [server](../../server/README.md) (S38), [android](../../android/README.md) (A68).

After acceptance the body of this ADR is not edited. Only the `- Status:` line changes.
