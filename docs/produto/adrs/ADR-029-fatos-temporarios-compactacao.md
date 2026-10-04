# ADR-029 — Short-lived temp facts, a compaction that keeps the open tail, the meal day is the day it was eaten

- Status: Accepted (2026-10-03, owner approval of S16)
- Date: 2026-10-03
- Context: `produto`
- Supersedes: partially [ADR-023](ADR-023-chat-v2-memoria-v2.md) decision 4 (memory had two kinds, permanent and dynamic; a third kind, `temp`, is added; the permanent/dynamic rules survive unchanged). In the [Chat spec](../specifications/chat.md): rule 9 (the summary replaced the whole raw block). Refines [ADR-028](ADR-028-registro-autonomo.md) decision 1 ("the meal is of today") without changing its trigger. In [v1-chat](../../server/specifications/v1-chat.md): the `meal_day` sentence of rule 3g, rule 3d ("de sempre") and rule 7 (digest content). Extends `POST /v1/chat` of the [contract](../../api-contract.md) additively. No gold changes.

## Context

Owner session of 03/10/2026 (dev 0.0.9, log request ids `9335d4a4` to `8d63d830`).

- **02/10, 23:53.** The owner, planning the next day's lunch, sent a photo of a frozen lasagna label (Sadia bolonhesa, 600 g, 99 kcal and 7 g protein per 100 g). The AI answered with the numbers (`intent: question`, nothing recorded, no memory change).
- **03/10, 13:58.** "470 gramas da lasanha que pedi para você estimar ontem + 80 gramas de costela". The model set `meal_day: other` because of "ontem", although "ontem" described when the estimate was asked, not when the food was eaten. Every following turn was blocked by the record gate (`none_other_day`). The model also did not know the label numbers: yesterday's chat is not in the prompt (only today's messages are), the label turn recorded nothing (not in `recent`), and nothing was saved to memory. It estimated the lasagna at 700–850 kcal; the label gives about 465 kcal for 470 g.
- **13:59.** The cheesecake photo got a clarifying question (whole cake weight). Right after it, the client compacted the 12 raw messages into a digest. The summary replaced the whole raw block, so the open question disappeared from `HISTORY`, and the answer "100 gramas" reached the model with no context ("100 gramas de qual alimento?"). The digest also stated the wrong guess as a confirmed fact ("a conversa esclareceu que eram de ontem") and an unconfirmed estimate as eaten ("estimado em cerca de 150 g"). Later turns read that digest and kept `meal_day: other`.
- **14:15.** "Você não fez o registro… no meu almoço" still came back as "almoço de ontem". "É para registrar na refeição de hoje" was answered with "Em qual refeição?" although every previous estimate had suggested Almoço: the suggested slot lives only in the stored estimate and never reaches the next prompt (`HISTORY` carries text only). Only "Almoço" recorded the meal. The owner had to insist twice.
- Same morning: "Café o mesmo de sempre" with no breakfast routine fact in memory was answered "Não encontrei um café da manhã habitual" although `RECENT` held the same breakfast on 01/10 and 02/10.

## Decision

### 1. `temp` facts: short-lived reference data

A third memory kind, next to permanent and dynamic ([memoria-push](../specifications/memoria-push.md) rules 1–5).

- **What enters:** nutrition data the user gives for a specific food or product that is **not** being recorded in that turn: a label (photo or typed), a product with its kcal per portion, a dish the AI estimated for later. Example: `lasanha: Lasanha Sadia bolonhesa, 99 kcal e 7 g P por 100 g (rótulo, caixa 600 g)`. Habits and preferences never enter as temp (they stay dynamic or permanent). A generic question ("quantas calorias tem um pão?") never creates one.
- **Who decides:** the AI proposes (`memory_updates` `add` with `kind: temp`), the app applies, as in ADR-023. Applied at answer time, like preference and portion.
- **Id and size:** `T{n}`, never reused. At most **5** temp facts; a sixth pushes out the oldest. `category` `portion` or `preference`, never `routine`. `key` and `text` as the other kinds (≤ 40, ≤ 160). A temp key only matches other temp facts (it never merges with a dynamic or permanent fact of the same key).
- **Use:** the model uses the numbers whenever the user eats that food, as many times as it happens (the rest of the box the next day), and lists the id in `memory_used`.
- **Removal, whichever comes first:** **3 days** after it was created (America/Sao_Paulo dates; created 02/10 → gone on 05/10), the cap of 5, or an explicit request ("esquece a lasanha") → `remove`. Recording a meal never removes it; record, Trocar, Excluir, Editar and Desfazer do not touch temp facts.
- **No promotion.** A temp fact never becomes dynamic or permanent. A food eaten often becomes a routine through recorded meals, as today.
- **Visibility:** `Memória atualizada` shows when a temp fact is added or replaced (existing badge). No new origin chip: a temp fact used in an answer shows no chip. The dev memory editor ([ADR-019](ADR-019-ferramentas-dev.md)) lists temp facts read-only.
- **Compatibility:** the server proposes temp facts only to a client that declares it can store them (new request flag); the current Android app would treat an unknown kind as dynamic. A server without this ADR rejects `T` facts, so the client ships only after the server.

### 2. Compaction keeps the open tail

- When 12 raw messages accumulate since the last digest, the client summarises the **oldest contiguous block** of at most 12 raw messages, leaving the newest **4** raw. If an assistant question-only turn ([ADR-026](ADR-026-perguntas-antes-da-estimativa.md)) is still open, the kept tail starts at the user message that opened that meal, even if that keeps more than 4; with 12 or more messages in the open sequence, nothing is compacted.
- The digest records the id of the last message it summarised; only messages after it go in `HISTORY`. A message is summarised at most once and never skipped: after failed compactions (more than 12 raw), each compaction takes the next contiguous block and the marker advances only over what was summarised.
- The day and the latest wipe are fixed when the compaction starts; a summary that comes back after midnight or after a wipe is discarded.
- At most 2 digests and 12 raw in a prompt, as today.

### 3. Digest states facts only, never guesses

The summary keeps the foods and amounts the user stated, the slot when the user named it, skips, and the AI's open question with the meal it is about, marked as open. It never states a record status (recorded or not: the `DAY` snapshot owns that), never states a day or slot the AI only assumed, and never turns an estimate the user did not confirm into food eaten.

### 4. The meal day is the day the food was eaten

`meal_day: other` only when the **eating** happened on another day. A past-day word attached to anything else (asked, estimated, bought, cooked, planned yesterday: "a lasanha que pedi para você estimar ontem", "o bolo que comprei ontem") leaves the meal `today`. "O mesmo de ontem" is today's meal copying yesterday's (unchanged).

### 5. Three separate questions: which meal, which day, record or not

A user correction is read on each axis separately; none implies another.

- **Day:** only an explicit statement of the eating day changes it ("é de hoje", "comi hoje", "foi ontem"). Asking to record or saying it was not recorded does not change the day: "registra o almoço de ontem" stays `other` and is not recorded (decision 4, ADR-028).
- **Record intent:** asking to record or saying it was not recorded ("registra", "você não registrou", "era para registrar") is `record_intent: clear` for that meal, re-estimated from the conversation (foods and answers in `HISTORY` and `DIGESTS`). A statement of the day alone ("é de hoje") after a pure question about food does not make it a record.
- **Which meal (slot):** the slot named now; else the slot the user named earlier; else the slot the assistant suggested earlier for that meal. The assistant's earlier suggestion reaches the prompt as a marker on its turn, read as a suggestion and never as the user's words. When a slot is known by any of these, the assistant never asks which meal.
- What the user says now overrides `DIGESTS` and earlier assistant replies.

### 6. "De sempre" without a routine fact

"O de sempre", "o mesmo de sempre" for a slot:

1. A routine fact of that slot in `MEMORY` wins.
2. Else, take the two most recent `RECENT` records of that slot on two **different** days (on a day with more than one record in the slot, its latest). If they have the same foods in the same amounts (a different brand of the same food counts as the same; any different amount, an added or a missing food does not), use the most recent, its foods and numbers, and say in `reply` which day was copied.
3. Else (different, or fewer than two days), `estimate` is null and the reply asks what it was.

## Motivation

- The owner's real meal took four extra messages and was recorded with ~300–400 kcal too much. The record gate did what it was told; the failures were the inputs (a wrong `meal_day`, a lost question, a lost suggested slot, a digest that turned a guess into a fact).
- A temp fact carries exactly what crosses the day boundary, at ~25 tokens a line (≤ 5 lines), and leaves by itself. Sending yesterday's chat would grow every prompt and break the "today only" history rule ([chat](../specifications/chat.md) rule 8); saving label data as dynamic facts would keep one-off products in memory for 21 days and compete with habits for the 40 slots.
- A time-only lifetime keeps the numbers for a second portion of the same product (a 600 g box eaten in two days) and keeps memory out of the record and undo paths: nothing to revert, no receipt coupling.
- Keeping a short raw tail costs ~4 messages of prompt and removes the main way a compaction breaks an ongoing exchange.
- Separating day, record intent and slot keeps the correction rule from contradicting the other-day rule.

## Consequences

### Positive

- A label read yesterday is used today, and again tomorrow, with real numbers.
- A clarifying answer right after a compaction still has its question.
- "Ontem" in a sentence no longer blocks today's record; asking to record records at once, in the slot already suggested.
- "De sempre" works from the first repeated day, before the routine fact exists.

### Negative

- Memory format gains a kind and an id prefix (`T`); the request gains a flag; the server accepts a third `kind`; a question-only turn returns its held slot to the new client. All additive.
- Room v8 → v9: `day_digest` gains the id of the last message it covers.
- Up to 4 more raw messages per prompt after a compaction.
- A temp fact can stay 3 days after the food is gone; the cap of 5 and "esquece" bound it.
- The model can still misjudge `meal_day` or miss a temp add; the evaluator measures it and Registrar/Editar stay as the user's correction path.

## Alternatives considered

### Send yesterday's chat (or its digest) after midnight

Rejected: every turn of the morning pays for it, most of it is irrelevant, and the "today only" history rule would have exceptions.

### Save label data as dynamic facts

Rejected by the owner: one-off data would stay 21 days and crowd the habit memory.

### Remove the temp fact when the meal that used it is recorded

The owner's first idea. Rejected after review (03/10/2026, owner chose option A): a second portion of the same product would lose the numbers, and removal would have to be reverted by Desfazer, Excluir, Editar and Trocar (Trocar reverts receipt memory images and would bring the fact back while the meal stays recorded).

### Server-side guard on "ontem"

Rejected: a keyword rule cannot tell "comi ontem" from "pedi ontem"; the instruction plus evaluator cases is the control (as S15 did for "ainda").

### Compaction that keeps everything until the meal is recorded

Rejected: an unanswered question would grow the raw block without bound. The 12-message cap stays.

## Relations

- Specifications affected: [chat](../specifications/chat.md) (rules 8, 9), [memoria-push](../specifications/memoria-push.md) (memory rules 1–5, 7, 8), [v1-chat](../../server/specifications/v1-chat.md) (rules 3, 3d, 3g, 4, 5, 5a, 5c, 7), [api-contract](../../api-contract.md), [room-v2](../../android/specifications/room-v2.md).
- ADRs related: [ADR-023](ADR-023-chat-v2-memoria-v2.md), [ADR-026](ADR-026-perguntas-antes-da-estimativa.md), [ADR-028](ADR-028-registro-autonomo.md), [ADR-019](ADR-019-ferramentas-dev.md), [ADR-015](../../server/adrs/ADR-015-log-conversa-dev.md).
- Plans: [S16](../../server/plans/completed/s16-dia-da-refeicao-fatos-temporarios.md) (server: decision 1 server part, 3, 4, 5, 6), [A38](../../android/plans/completed/a38-fatos-temporarios-compactacao.md) (client: decision 1 client part, 2, 5 slot marker).

After acceptance, the body of this ADR is not edited. Only the `- Status:` line changes, to record a total or partial supersession by a new ADR.
