# ADR-028 — The Chat records a meal by itself, with a receipt that can undo it

- Status: Accepted (02/10/2026, with [A34](../../android/plans/completed/a34-registro-autonomo.md)); partially superseded by [ADR-032](ADR-032-acrescimos-e-correcoes-de-refeicoes.md) (decision 3: distinct addition/revision presentation and destination action)
- Date: 2026-10-01
- Context: `produto`
- Supersedes: partially [ADR-012](ADR-012-chat-home-perfil.md) (record only after the tap on Gravar) and [ADR-017](ADR-017-registro-consolidado.md) decision 3 only in its trigger and form (the replace confirmation was opened by a tap on Gravar, as a dialog; it now arrives inline with the answer; asking before replacing stays). In the [Chat spec](../specifications/chat.md): "Registro só depois do tap no chip", rules 4–7, the "tool invisível que grava meal_log" out-of-scope line and the criterion "Gravar sem tap não altera o círculo da Home". In [v1-chat](../../server/specifications/v1-chat.md): rule 3f, "nunca gravar sozinho" in rule 4, the "ontem → grava hoje" sentence of rule 4 and the criterion "Sem tap do user o server nao grava nada" (reworded: the server still writes nothing; it tells the client what to do). Edits golds `chatE`, `chatF`, `chatG`; adds golds `chatU`, `chatD` (29 → 31 per theme). Extends `POST /v1/chat` of the [contract](../../api-contract.md) additively.

## Context

Owner feedback after real use, as user and future maintainer (brainstorm of 01/10/2026). Messages such as "Na janta comi…", "Lanche da tarde: 200g de…" or "Registra aí, comi X e Y" state the intent to record, yet the meal only counts after a tap on **Gravar**. A product meant for the public store must act on a clear request by itself. The owner also expects the AI to be wrong sometimes (record what was not meant to be recorded, wrong slot, wrong numbers), so every automatic record must be visible and reversible.

Today's code, by design: the server never decides a record (`v1-chat` rule 3f: "o reply nunca diz que registrou"); the client records only from Gravar, Registrar assim, Substituir or the routine card; receipts (`logged`, `replaced`, `skipped`) are inert rows in `chat_message`.

## Decision

### 1. Trigger: a clear `log` of today

The client records by itself only when the turn is `intent = log`, the estimate is released (no open question, [ADR-026](ADR-026-perguntas-antes-da-estimativa.md)), the meal is of **today**, the suggested slot belongs to today's slots, and the AI judged the intent to record **clear**. The AI reads the Portuguese broadly (past tense, "comi", "jantei", "registra", a meal name followed by food, a plate photo), not a keyword list.

- A photo of food with no text is a clear `log`.
- `plan` never records by itself; **Registrar assim** stays ([ADR-023](ADR-023-chat-v2-memoria-v2.md)).
- `question` never records.
- While the AI asks questions (ADR-026) nothing is recorded; the record happens with the released estimate, including after **Forçar estimativa**.

### 2. Unsure intent: one Registrar button

When the AI is not sure the user wants a record (e.g. "pudim de leite com calda" with nothing else), the estimate comes with the mark `ask`. The app shows the estimate and a single **Registrar** button in the actions slot (gold `chatE`). The tap records locally, with no new request. Registrar disappears on the next send, and that estimate is marked `Não registrado`.

### 3. Slot already recorded: confirmation inside the Chat

ADR-017 stays: one record per slot, the AI re-estimates the whole meal. When the target slot already has a record, nothing is replaced without asking. The confirmation appears **inside the conversation**, right below the AI bubble (gold `chatU`): `Substituir {slot}?`, `{slot} tem {kcal antigo} kcal. Fica com {kcal novo} kcal.`, **Substituir** | **Outra refeição**. Outra refeição opens the Trocar sheet. The pending confirmation dies on the next send, at the day change, or when the slot changes by another path; the bubble is then marked `Não registrado`.

A skipped slot is not a record: a meal for it is recorded by itself, and that receipt's Desfazer brings the skip back.

### 4. Skip by text

"Pulei o café" (clear, today) marks the slot as skipped by itself, with a receipt. A firm skip announced ahead ("hoje não vou jantar") counts too; a hedged one ("acho que não vou jantar") or a pending meal ("ainda não almocei") does not (owner, 02/10/2026; [S15](../../server/plans/completed/s15-registro-casos-dificeis.md)). The Chat action bar (Gravar | Trocar | Pular) and its skip dialog leave the Chat. A skip for a slot that already has a record or a skip changes nothing; the turn is marked `Não registrado` (Excluir on the receipt removes a record). The Home skip (tap on an empty slot, dialog `chatP`) is unchanged.

### 5. Receipts carry the actions

Every record, replacement, skip, move or restore leaves a receipt. Only the **latest receipt of each slot** shows actions, as a vertical stack of full-width buttons below it (golds `chatG`, `chatF`, `chatD`). Actions are permanent while that receipt is the latest one of its slot(s), on any day.

| Receipt | Actions |
|---|---|
| First record (text, plan, routine) | Excluir · Trocar refeição · Editar |
| First record (photo) | Excluir · Trocar refeição |
| Replacement | Desfazer · Excluir · Trocar refeição · Editar (no Editar for a photo) |
| Move (Trocar refeição) | Desfazer · Excluir · Trocar refeição · Editar (no Editar for a photo) |
| Restore (after Desfazer) | Excluir · Trocar refeição · Editar (no Editar for a photo) |
| Skip | Desfazer |

- **Excluir**: deletes the slot's record. No confirmation. The receipt is marked `Excluído`. No redo.
- **Desfazer**: reverses exactly what that receipt did. Replacement → the previous record comes back (text, kcal, P/C/G, source) and a new receipt `Restaurado em {slot} · {hora}` with `{kcal} kcal` takes the actions. Move → the record returns to its original slot (and a slot it replaced gets its previous record back). Skip → the skip is removed. The receipt is marked `Desfeito`. No redo.
- **Trocar refeição**: opens the Trocar sheet (gold `chatT`) with the current slot marked. An empty target moves the record; an occupied target asks through the confirmation of decision 3, below the receipt.
- **Editar**: deletes the record and puts the record's text (the consolidated `meal_text`) in the composer, focused, keyboard open. The receipt is marked `Removido para editar`. Sending it again is a normal turn. Not offered for a photo record (the photo is gone from the server).
- A receipt that lost its actions keeps its text; old messages keep no action bar. Estimates from before this ADR lose Gravar/Trocar/Pular.

### 6. Memory follows the record

The automatic record applies the routine proposed with that estimate, as Gravar did (chat rule 12). Excluir, Editar and Desfazer revert the memory change of that receipt. A fact changed again after the receipt (by a later turn) is left as it is: the revert only restores a fact that still matches what the receipt wrote.

### 7. Other days

A message about another day ("ontem jantei pizza", "na janta comi X" sent the next morning about last night) is not recorded. The reply says the Chat records only today's meals. Retroactive recording is a separate feature: [A35](../../android/plans/out_of_scope/a35-registro-retroativo.md) (`Fora de escopo`).

### 8. The server decides the mark, the client records

The server stays stateless and writes nothing (ADR-013). For a client that opts in, the response carries `record`: `auto` | `ask` | `none`, and a `skip` intent with its slot. The client applies the record, checks its own guards (slot of today, no pending question) and downgrades `auto` to `ask` when a guard fails. Older clients see no change.

### 9. Dev telemetry

Dev flavor only (ADR-014), enums and numbers only (ADR-015): automatic record, ask shown/tapped/expired, replace confirmation shown/confirmed/elsewhere/expired, skip by text, every receipt action with the receipt kind and the time since the receipt. The undo rate is the measure of wrong automatic records.

### 10. Gate

One Stitch gate, [ST9](../../stitch/plans/completed/st9-registro-autonomo.md): `chatE` (Registrar only), `chatF` and `chatG` (receipt with stacked actions, no action bar), new `chatU` (replace confirmation inside the Chat) and new `chatD` (undone replacement and restore receipt).

## Rationale

- A clear request recorded by itself removes a tap from every meal, which is the most frequent action of the app.
- Errors stay cheap: the receipt says what was done, and one tap reverses it. The undo rate in telemetry tells whether the AI records too eagerly.
- Replacing an existing record without asking was rejected in ADR-017; that holds. Only the trigger changes, and the question moves inline because it now arrives with the answer, not after a tap.
- Actions only on the latest receipt of a slot keep every action defined: the state it reverses is the current one.

## Consequences

### Positive

- One tap less per meal; the receipt doubles as the place to fix it.
- Delete, move and edit of a Chat record exist for the first time.
- The undo rate measures the classifier in real use.

### Negative

- A wrong automatic record touches the Home until undone. Accepted by the owner, with the receipt and the actions as mitigation.
- Room migration (receipt state and undo data) and a server schema change (`record`, `skip`).
- Up to four stacked buttons below a receipt take vertical space.
- Five gold changes per theme (three edited, two new): 29 → 31.
- The intent judgement is heuristic; the `ask` mark and the evaluator cases bound it.

## Alternatives considered

- **Keep Gravar, add a "record automatically" setting:** no settings toggle in this cut (`AGENTS.md`); rejected.
- **Record everything classified as `log`, with no `ask`:** no safety net for unclear messages; the owner chose an explicit fallback.
- **Ask by text ("digite registrar"):** a new request per answer (cost, latency, a new chance of error); the owner accepted a local Registrar button instead.
- **Undo as a temporary snackbar:** an expiry adds state for no gain; the owner chose permanent actions on the receipt.
- **Desfazer on a replacement deletes the whole meal:** loses the previous record; the owner chose to restore it.
- **Replace without asking:** rejected again (ADR-017).
- **Keep actions on every receipt:** an old receipt would reverse a state that no longer exists; rejected.

## Relations

- Specifications affected: [chat](../specifications/chat.md), [v1-chat](../../server/specifications/v1-chat.md), [api-contract](../../api-contract.md), [room-v2](../../android/specifications/room-v2.md).
- Related ADRs: [ADR-012](ADR-012-chat-home-perfil.md), [ADR-014](../../android/adrs/ADR-014-flavors-firebase-dev.md), [ADR-015](../../server/adrs/ADR-015-log-conversa-dev.md), [ADR-017](ADR-017-registro-consolidado.md), [ADR-023](ADR-023-chat-v2-memoria-v2.md), [ADR-026](ADR-026-perguntas-antes-da-estimativa.md).
- Plans: [S14](../../server/plans/completed/s14-registro-autonomo.md) ∥ [ST9](../../stitch/plans/completed/st9-registro-autonomo.md) → [A34](../../android/plans/completed/a34-registro-autonomo.md). Deferred: [A35](../../android/plans/out_of_scope/a35-registro-retroativo.md).

Once accepted, this ADR is not edited. A later change needs a new ADR that declares the supersession.
