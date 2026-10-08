# ADR-043 — A plan targets the protein gap inside the meal window

- Status: Accepted (2026-10-06, with the owner's approval of S24; delivery stopped in implementation, see the plan); decision 4 partially superseded by [ADR-055](ADR-055-protein-boost-hybrid.md) (the model proposes the boost foods, the server validates)
- Date: 2026-10-06
- Context: `produto`
- Supersedes: partially [ADR-039](ADR-039-plan-cooking-and-budget-choice.md) (the budget of a plan: the limit is the meal window, not the whole remaining day, and the server computes the default reservations); complements [ADR-041](ADR-041-reference-portions-in-chat-instructions.md) (open requests answered as plans) and [ADR-042](../../server/adrs/ADR-042-estimate-total-is-server-arithmetic.md) (server arithmetic).

## Context

On 2026-10-06 the owner planned a dinner with the dev app. The server had `p_target=165`, `eaten_p=85` and `remaining_kcal=824`: 80 g of protein missing and 824 kcal left for dinner and supper. The model answered a 280 kcal dinner with 13 g of protein, then kept the same dish through three more turns. Under the current instructions that answer is correct: the only objective the `plan` rule gives the model is to fit `remaining_kcal`, and when the user names the foods the dish is sized "as they would make it" and never changed. `p_target` reaches the model in `PROFILE` on every turn and no rule uses it.

The constitution defines `windowBudget = effectiveCeiling − eaten − reservedUpcoming`, but nothing computes `reservedUpcoming`: ADR-039 takes reservations only from meals the user mentions in the conversation. In practice a dinner plan sees the whole day's remainder and forgets supper.

The owner's reference experience (a general chat bot with a hand-written profile) had one explicit rule: protein is the priority. Fibrai collects the protein target in O4 and never acts on it.

## Decision

1. **Remaining macros in DAY.** The server computes and serializes `remaining_p`, `remaining_c`, `remaining_g` (target − eaten, may be negative) next to `remaining_kcal`. The app keeps sending targets and eaten values; the arithmetic is the server's.
2. **Objective of a plan for today.** Inside the meal window (3), the dish covers as much of the remaining protein as the window allows, then fat and carbohydrate. The model names in one clause of the reply what the dish does for the protein of the day; the app's projected-day panel (chat rule 16) stays the source of the numbers.
3. **Meal window.** `window_kcal = remaining_kcal − reserved_upcoming`. `reserved_upcoming` is the sum, over the other slots of today still empty (not eaten, not skipped), of each slot's expected kcal: the mean of that slot's `RECENT` records when it has records on at least two of the last seven days, otherwise the day's ceiling divided by the number of slots of the day. A reservation the user states in the conversation (ADR-039 `reserved`) replaces the computed value of the same slot. The model receives one `BUDGET` line with the window; the ADR-039 budget check uses the window as `limit_kcal` and keeps **Pode passar** / **Ajustar para caber**.
4. **A named dish keeps its foods.** When the user names the foods, the dish is still sized as stated. When that dish covers less than 30% of the remaining protein and the window leaves room, the model adds up to two common, low-cost protein foods marked `(opcional)`, as the cooking rule of ADR-039 already allows for recipes. Without room, it adds nothing and says so in numbers.
5. **Open request.** "Não sei o que comer" and equivalents receive two concrete options, one leaner and one more indulgent, both with grams, totals and inside the window, instead of one.
6. **Closing the day after a log.** When a log or a plan of today leaves other slots empty, the reply ends with one closing line per remaining slot: a food (the slot's routine from MEMORY or RECENT when one exists, otherwise a common choice) with its kcal and protein, the slots summing to the day's remainder with the protein gap covered as far as the kcal allow. The windows per slot are server arithmetic (3), serialized for the model as one `WINDOWS` line; the model fills the foods. Nothing in that line is recorded or reserved; it is a suggestion the next turn may change.
7. **No scale.** When the user says the meal cannot be weighed, a plan states each food in household measures (units, spoons, slices, palm-size) with the approximate grams beside them.
8. **Nothing else moves.** Logs, questions, skips, memory, moderation and the record gate are untouched. A plan still never records by itself.

## Motivation

- The defect is the objective function, not the model: the same turn with the same numbers has an obvious better answer (pão sarraceno 50 g, 3 ovos, maionese 5 g, 100 g de frango desfiado opcional ≈ 560 kcal and 50 g of protein, leaving 260 kcal for supper).
- O4 already asks every user for a protein target. Using it needs no new screen and applies to every user without configuration.
- The meal window is the constitution's formula finally computed. A dinner plan that ignores supper is the second cause of the "1074 kcal lunch" effect the testers see.

## Consequences

### Positive

- A plan answers the question the user actually has: what to eat now so the day closes near its targets.
- Deterministic: the window, the reservations and the macro remainders are server arithmetic, like ADR-039 and ADR-042.
- No client change for the objective itself; the client already shows the projected day.

### Negative

- Two prompt rules grow; the full evaluation suite must be rerun once (about 250 model calls per the server README policy).
- A slot without history is reserved at an equal share of the ceiling, which can under- or over-reserve on the first days. Accepted: the user can say the reservation and ADR-039 takes it.
- A named dish may come back with `(opcional)` additions the user did not ask for. Accepted: they are marked, counted and easy to drop.

## Alternatives considered

### Ask the user whether protein matters (onboarding field)

Rejected. O4 already collects the target; a second question adds a screen (ADR-012) for a preference the numbers already express.

### Let the model compute the window from `RECENT`

Rejected. ADR-039 and ADR-042 show the model extracts well and adds poorly; the window is arithmetic.

### Per-meal kcal caps typed by the user in O3

Deferred. Possible later as an override; the computed default covers the common case without a new screen.

## Relations

- Specifications affected: [chat](../specifications/chat.md) rule 16, [v1-chat](../../server/specifications/v1-chat.md) rules 3 and 5, [HTTP contract](../../api-contract.md) `/v1/chat`.
- Related ADRs: ADR-039, ADR-041, ADR-042.
- Consuming contexts: [server](../../server/README.md) ([S24](../../server/plans/completed/s24-protein-first-plan.md)); [android](../../android/README.md) (none for this ADR).

After acceptance the body of this ADR is not edited. Only the `- Status:` line changes.
