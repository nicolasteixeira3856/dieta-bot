# ADR-051 — Plan options with identity; routine discovery on the first Chat opening

- Status: Accepted (2026-10-08, owner approval of S37 in the batch message; owner direction of 07/10/2026 in `benchmark/MELHORIAS_CHAT_07_10_2026.md` § 5)
- Date: 2026-10-08
- Context: `produto`
- Supersedes: none. Complements [ADR-043](ADR-043-plan-objective-protein-and-meal-window.md) decision 5 (two options for an open request), [ADR-023](ADR-023-chat-v2-memoria-v2.md) and [ADR-029](ADR-029-fatos-temporarios-compactacao.md) (memory and digests).

## Context

An open request returns two options in prose; "fiz a 2", "quanto de iogurte na 1?" and "travar a 1" have nothing to point at, and after compaction the options are gone. On the first opening the memory is empty and the model asks the user's habits one meal at a time, or never. The benchmark of 08/10/2026 ([results](../../../benchmark/RESULTADOS_08_10_2026.md)) measured the plan family at 52 % (current) against 77–85 % (target), and the discovery family at 22 % against 67 % at effort low.

## Decision

1. **Every option of a plan has an id and its own estimate.** A plan with options carries `options[] = {id, name, estimate}` (`o1`, `o2`); `estimate` describes option 1. The reply presents them as `Opção 1: {name}` and `Opção 2: {name}` with items, totals and protein. A later message that names an option by number or name is answered about that option only: a question quotes the grams already given; "fiz a 2" is a log copying option 2 unchanged; "travar a 1" reserves option 1 (ADR-046).
2. **Digests preserve the options.** The compaction keeps the id, name, kcal and protein of each option of the last plan, so the references in 1 survive compaction.
3. **Discovery on the first opening.** When the Chat opens with an empty memory for the first time, Tali asks up to four skippable questions in one message: the usual breakfast, lunch, dinner, and fixed preferences or equipment. The answers become facts by the current rules: explicit preferences are permanent; a routine is saved with macros estimated by the server and marked as declared, without counting a recorded day. The user may skip; the day proceeds as today. The format rule is relaxed for this one message (a bulleted list of questions).
4. **Two memory categories.** `equipment` (declared: air fryer, pressure cooker, scale) and `liked` (a record that names a plan of the day the user approved) exist in the contract and in the memory rules; plans prefer declared equipment and liked dishes.

## Motivation

- Continuity measured: with ids in HISTORY or in the digest, "fiz a 2" and "quanto de iogurte?" pass in the target prompt.
- Discovery in the Chat avoids a new onboarding screen (O6) while the measurement says the Chat covers day 0; O6 stays conditional ([ADR-053](ADR-053-visible-memory-screen.md) § decision 4).

## Consequences

### Positive

- Options become things the user can act on; the first day has routines without a form.

### Negative

- The option control in the bubble needs a gold (design plan); `options` and the two categories enter the contract and `MemoryRules`. The benchmark shows discovery at effort none failing to save routines with macros (11 %); the delivery depends on [ADR-054](../../server/adrs/ADR-054-chat-reasoning-effort-low.md).

## Alternatives considered

### Onboarding screen O6 for routines

Deferred, conditional: a sixth screen changes ADR-012 and the production gate copy; the Chat covers the same questions.

### Options without ids, matched by name

Rejected: the benchmark shows the model copying the wrong option when names are close.

## Relations

- Specifications affected: [chat](../specifications/chat.md), [memoria-push](../specifications/memoria-push.md), [v1-chat](../../server/specifications/v1-chat.md), [HTTP contract](../../api-contract.md).
- Related ADRs: ADR-023, ADR-029, ADR-043, ADR-046, ADR-050, ADR-053.
- Consuming contexts: [server](../../server/README.md) (S37), [android](../../android/README.md) (A67), [design](../../design/README.md) (option control gold, inside A67's prerequisites).

After acceptance the body of this ADR is not edited. Only the `- Status:` line changes.
