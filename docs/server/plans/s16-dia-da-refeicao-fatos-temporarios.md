# Plan — S16 Meal day is the eating day, day/record/slot read apart, "de sempre" from RECENT, honest digest, temp facts

- Status: Aguardando aprovação
- Date: 03/10/2026
- Owning context: `server`
- Affected code: `server/` (`main.py`, `llm.py`, `shaping.py`, `config.py`, `conversation_log.py`, `evals/`, `tests/`)
- Prerequisites: [ADR-029](../../produto/adrs/ADR-029-fatos-temporarios-compactacao.md) accepted by the owner (it is `Proposed`). Executes ADR-029 decisions 1 (server part), 3, 4, 5 and 6. Does not depend on [A38](../../android/plans/a38-fatos-temporarios-compactacao.md); A38 depends on this plan being deployed to the dev server (rollout order below).

## Authorization gate

This plan is documentation only. Implementation starts only after an explicit approval naming this file:

> Aprovo o plano `docs/server/plans/s16-dia-da-refeicao-fatos-temporarios.md`. Implemente o plano aprovado.

If implementation reveals an uncovered decision, stop, update the artifacts and ask for a new approval.

## Goal

The owner's lunch of 03/10/2026 (ADR-029 § Context) records on the first message that carries it, with the label numbers from the day before; asking to record records at once in the slot already suggested; a real other-day meal still never records. Every failure of that session becomes an evaluator case.

## Background

Dev log of 03/10/2026, owner installation, APK 0.0.9-dev (v4 client):

| Request | Message | Model | Gate |
|---|---|---|---|
| `9335d4a4` | [foto] "470 gramas da lasanha que pedi para você estimar ontem + 80 gramas de costela" | `meal_day: other` | `asked` |
| `d718815d` | answer (bolonhesa, sem osso) | `other`, slot 3 | `none_other_day` |
| `e0208533` (compact) | — | digest: "a conversa esclareceu que eram de ontem … não foram registrados"; cheesecake "estimado em cerca de 150 g" stated as eaten | — |
| `1f1a7fcc` | "100 gramas" (answer to the cheesecake question, lost by the compaction) | `question` "100 gramas de qual alimento?" | `none_intent` |
| `863f4f22` | "Do cheesecake que você acabou de pedir" | `other` | `none_other_day` |
| `fa99ccd3` | "Você não fez o registro nem da lasanha e nem do cheesecake no meu almoço" | `other`, "almoço de ontem" | `none_other_day` |
| `303880fc` | "É para registrar na refeição de hoje" | `today`, `question` "Em qual refeição?" | `none_intent` |
| `8d63d830` | "Almoço" | `today`, slot 3, `clear` | `auto_log` |

Plus `49d854db`: "Café o mesmo de sempre", no breakfast routine fact, `RECENT` with the same breakfast on 01/10 and 02/10 → "Não encontrei um café da manhã habitual registrado".

The label numbers (99 kcal, 7 g P per 100 g) were given in the `question` turn of 02/10 23:54 and reached nothing the next day's prompt carries. The suggested slot (3) of every estimate never reached the next prompt: `HISTORY` is text only, and a question-only turn returns no slot at all.

## Implementation scope

### 1. Instructions (`_CHAT_INSTRUCTIONS`, English, all clients; the text is the same for every request)

- **Meal day (ADR-029 decision 4).** Rewrite the `meal_day` sentence: `other` only when the message says the food was **eaten** on another day (comi ontem, ontem à noite jantei, na quinta passada comi). A past-day word about something else (pedi para estimar ontem, comprei ontem, fiz ontem, planejei ontem) does not make the meal another day: `today`. "O mesmo de ontem" is a meal of today. The before-05:00 rule and "agora is always today" stay.
- **Day, record, slot apart (decision 5).**
  - Day: only an explicit statement of the eating day changes `meal_day` (é de hoje, comi hoje, foi ontem). A request to record or a complaint that it was not recorded leaves the day as the rest of the conversation sets it; "registra o almoço de ontem" is `other`.
  - Record: a request to record or a statement that it was not recorded (registra, você não registrou, era para registrar) is `intent: log`, `record_intent: clear`; re-estimate the meal it refers to from `HISTORY` and `DIGESTS` (every food and answer). A day statement alone ("é de hoje") after a question about food that was not a meal stays as it was (no log).
  - Slot: the slot named now; else named earlier by the user; else the assistant's earlier suggestion for that meal, shown in `HISTORY` as a line `[refeição sugerida: {slot name}]` at the end of an assistant turn. That line is the assistant's suggestion, never the user's words. When a slot is known by any of these, never ask which meal.
  - What the user says now overrides `DIGESTS` and earlier assistant replies.
- **"De sempre" (decision 6).** Next to the "o mesmo de ontem" sentence: "o de sempre", "o mesmo de sempre" → a routine fact of that slot in `MEMORY` if any; else the two most recent `RECENT` records of that slot on two different days (the latest of a day when it has more than one): same foods in the same amounts (another brand of the same food counts as the same; a different amount, an added or a missing food does not) → use the most recent, its foods and numbers, and say in `reply` which day was copied (e.g. "Copiei o café de sexta"); else `estimate` null and ask what it was.
- **Temp facts (decision 1).** In MEMORY CHANGES: when the user gives nutrition data for a specific food or product that is not being logged in this turn (a label, typed or in a photo; a product with kcal per portion; a dish estimated for later), propose `add` with `kind: temp`, `category: portion`, a short `key` (lasanha) and `text` with the product and the numbers as given (Lasanha Sadia bolonhesa, 99 kcal e 7 g P por 100 g, caixa 600 g). A generic question about a food (quantas calorias tem um pão?) is not temp. Never a habit or preference as temp, never `reinforce` or `routine` for temp. In MEMORY USE: a temp fact (`T` id) that matches the food being logged gives its numbers, every time that food is logged; use them and list the id in `memory_used`.

### 2. Digest instructions (`_DIGEST_INSTRUCTIONS`, decision 3)

Keep: foods and amounts the user stated, the slot only when the user named it, skips. An open question of the assistant ends the digest as `Pergunta em aberto: {question} ({meal})`. Never: whether something was recorded, a day or slot the assistant only assumed, an estimate the user did not confirm stated as eaten. A `[refeição sugerida: …]` line is not a user statement.

### 3. Contract (additive)

- `ChatIn.temp_facts: bool = False`. True = **v5 client** (stores temp facts, sends the slot marker). Ignored for a legacy client (no `facts`).
- `FactIn.id` pattern `^[PDT][0-9]{1,4}$`; `kind` adds `temp`. A `T` id requires `kind: temp` and vice versa; otherwise 422. `FACTS_MAX` 70 → 75 (`MEMORY_TEMP_MAX = 5` in `config.py`).
- `MEMORY` header: with `temp_facts: true`, `permanent {n}/30, dynamic {n}/40, temp {n}/5`; without it, byte-identical to today. Temp line: `{id} {category} {key}: {text} (temp since {last_seen})` (the client sends the creation date as `last_seen`).
- JSON schema (`chat_turn`): `memory_updates[].kind` enum adds `temp` for every request. The flag adds no schema variant: the same profile slots and fact ids give the same schema with or without `temp_facts` (the enums of slot and fact ids already vary per request, as today).
- `shaping._memory_updates`: `kind: temp` kept only when the request has `temp_facts: true`; then only `op` `add` (id null), `replace` or `remove` (id a known `T` id), `category` `portion` or `preference`, `slot` null. Otherwise dropped. A `reinforce`/`replace`/`remove` of a `T` id with another `kind` is dropped.
- **Held slot of a question-only turn (v5 only).** When the clarify gate (rule 5c) holds the estimate, the v5 response carries `question_slot`: the held estimate's `suggested_slot` (a profile id) or null. Absent for v3/v4 clients (their responses stay byte-identical).
- Dev log (ADR-015): new fields `temp_facts` (count of `T` facts in the request, or null without the flag) and `question_slot` (id or null). No fact text.

### 4. Evaluator (`server/evals/`)

- `since: v5` accepted (the enum is asserted in `server/tests/test_evals.py`). `since` is metadata only: every v5 case carries `temp_facts: true` (and `clarify_rounds`, `auto_record: true`) in its request.
- New case field `required`: a list of expectation keys; an `n/a` on a required key fails the repetition.
- Compact mode: `request.compact: true` runs the digest path; expectations `digest` (`present` | `absent`), `digest_has` / `digest_not` (terms, as `reply_has`).
- Cases marked **strict** below need 3/3 (`strict: true`).

New cases, tag `meal-day`, `correction`, `de-sempre`, `temp` or `digest`, `refusal: none`, texts written by hand from the log situations (no raw tester text in git):

| Case | Situation | Expect |
|---|---|---|
| `dia-pedi-estimar-ontem` **strict** | 13:58, "470 g da lasanha que pedi pra você estimar ontem e 80 g de costela sem osso", `clarify_rounds: 3` | `log`, `record: auto`, slot almoço; required `record` |
| `dia-comprei-ontem` | 15:30, "comi um pedaço de 100 g do bolo de cenoura que comprei ontem" | `log`, `record: auto` |
| `dia-comi-ontem` **strict** | "ontem no almoço comi uma feijoada" | `record: none`, reply has hoje |
| `registra-almoco-de-ontem` **strict** | "registra meu almoço de ontem: arroz, feijão e bife" | `record: none`, reply has hoje |
| `correcao-nao-registrou` **strict** | HISTORY: lasanha + costela estimate, assistant turn ends `[refeição sugerida: Almoço]`, reply "Como é de ontem, o Chat registra apenas refeições de hoje"; "você não registrou, comi hoje no almoço" | `log`, `record: auto`, slot almoço, meal_text has lasanha |
| `correcao-slot-sugerido` **strict** | HISTORY as above, reply text never names almoço, marker present; "é para registrar na refeição de hoje" | `log`, `record: auto`, slot almoço, `top_question` absent |
| `correcao-slot-sugerido-pergunta` | HISTORY: question-only assistant turn ("Qual o peso do cheesecake?") with marker `[refeição sugerida: Almoço]`, answer, assistant estimate without slot name; "registra" | `log`, `record: auto`, slot almoço |
| `correcao-contra-digest` | DIGESTS say "eram de ontem"; "não, comi hoje no almoço, pode registrar" | `log`, `record: auto`, slot almoço |
| `correcao-dois-pratos` | HISTORY: lasanha estimate and cheesecake estimate, both marked almoço; "registra a lasanha e o cheesecake no almoço de hoje" | `log`, `record: auto`, slot almoço, meal_text has lasanha, cheesecake |
| `e-de-hoje-sem-refeicao` | HISTORY: "quantas calorias tem um pão de queijo?" + answer; "é de hoje" | `record: none` |
| `de-sempre-recent-igual` | no routine fact; RECENT café equal on the 2 last days; 10:45 "café o mesmo de sempre" | `log`, estimate present, slot café, reply has the weekday copied |
| `de-sempre-quantidade-diferente` | RECENT café same foods, 250 ml vs 300 ml leite | estimate absent, `record: none` |
| `de-sempre-um-dia` | RECENT has café on one day only | estimate absent, `record: none` |
| `de-sempre-rotina-vence` | routine fact café (tapioca) + RECENT café equal (ovos) on 2 days | estimate present, meal_text has tapioca, `memory_used_has` the routine id |
| `temp-rotulo-texto` **strict** | v5, 23:50, "amanhã vou almoçar essa lasanha, o rótulo diz 99 kcal e 7 g de proteína por 100 g, a caixa tem 600 g" | `memory_updates_has` `{op: add, kind: temp}`, `record: none`; required `memory_updates_has` |
| `temp-rotulo-foto` | v5, label image (synthetic: label text rendered on a plain background, made for the case), "amanhã almoço essa" | `memory_updates_has` `{op: add, kind: temp}` |
| `temp-pergunta-generica` | v5, "quantas calorias tem um pão francês?" | `memory_updates_not` `{kind: temp}` |
| `temp-usa-rotulo` **strict** | v5, fact `T1 portion lasanha: Lasanha Sadia bolonhesa, 99 kcal e 7 g P por 100 g`; 13:00 "comi 470 g da lasanha" | `log`, `record: auto`, `kcal_range` [420, 520], `memory_used_has` [T1]; required `memory_used_has` |
| `temp-sem-flag` **strict** | `temp-rotulo-texto` without `temp_facts` (v4) | `memory_updates_not` `{kind: temp}` |
| `digest-pergunta-aberta` **strict** | compact: cheesecake photo turn + assistant question about the whole weight | `digest: present`, `digest_has` [cheesecake, Pergunta em aberto], `digest_not` [comeu cerca de 150]; required `digest` |
| `digest-sem-dia-suposto` **strict** | compact: lasanha turn + assistant "Como é de ontem…" + user answer, no user statement of the day | `digest: present`, `digest_has` [lasanha, 470], `digest_not` [de ontem, registrad]; required `digest` |

### 5. Tests (`server/tests/`)

- `test_chat.py`: `FactIn` accepts `T1` + `temp`, rejects `T1` + `dynamic` and `P1` + `temp`; `MEMORY` header with and without `temp_facts`; 75 facts accepted, 76 rejected; the `chat_turn` schema is equal with and without `temp_facts` for the same slots and fact ids.
- `test_chat.py` (shaping): temp `add` kept with the flag, dropped without; temp `reinforce` dropped; temp `routine` dropped; `remove` of a `T` id kept.
- `test_clarify.py`: `question_slot` present for v5 on a held estimate (id or null), absent for v3/v4 (byte-identical response).
- `test_record.py`: instruction text assertions for the meal-day, day/record/slot, slot marker, "de sempre" and temp rules (as S15).
- `test_evals.py`: `since` enum has `v5`; `required` turns `n/a` into a fail; compact mode; `digest`, `digest_has`, `digest_not`.
- `test_conversation_log.py`: `temp_facts` and `question_slot` fields.

## Affected files and areas

- `server/main.py`, `server/llm.py`, `server/shaping.py`, `server/config.py`, `server/conversation_log.py`, `server/evals/run.py`, `server/evals/checks.py`, `server/evals/cases/` (21 new files, one synthetic label image), `server/tests/`.

## Intended spec changes (written at Completion, not now)

- [v1-chat](../specifications/v1-chat.md): rule 3 (MEMORY header with temp; the slot marker in `HISTORY`), 3d ("de sempre"), 3g (`meal_day` = eating day), 4 (day/record/slot apart), 5 (schema `kind` enum), 5a (temp shaping), 5c (`question_slot` for v5), 7 (digest content), § IN (`facts` with `T`/`temp`, `temp_facts`), dev log fields; Provenance S16.
- [api-contract](../../api-contract.md): `temp_facts` (v5 client), `facts` (`T` ids, `temp`, ≤ 75), `memory_updates` temp ops, `question_slot`, compact digest content.
- [memoria-push](../../produto/specifications/memoria-push.md): written by A38 (the client owns the memory rules).
- Server README: evaluator fields (`since: v5`, `required`, compact mode) and test list.

## Rollout

S16 first, A38 after. A server without S16 rejects `T` facts (422): the v5 APK must not reach testers before S16 is on the dev server. APKs up to 0.0.9 never send `temp_facts`, so they never receive a temp update or `question_slot`.

## Planned validation

1. `pytest server/tests` green.
2. `python -m evals.run --effort none --repeat 3`: every new case ≥ 2/3, the **strict** ones 3/3; all existing cases no regression against `master` (compare drops with 6 repetitions, as S15; `memoria-cheia` and `ceia-completa-suco` are known unstable). Record pass rate, p95 and cost here.
3. Deploy to the dev server (`tools/deploy-gcp.ps1`). Real requests with `X-Request-Id: s16-*`: the `dia-pedi-estimar-ontem` body → log `meal_day: today`, `record: auto_log`; the `temp-rotulo-texto` body → a `kind: temp` add in the response; a v4 body with a held estimate → no `question_slot`; the same body as v5 → `question_slot` present.
4. Docs: `node tools/check-docs.mjs` passes.

## Out of scope

- Android (A38): storing, sending and expiring temp facts; the slot marker in `HISTORY`; compaction tail.
- `reasoning.effort` (stays `none`).
- Sending yesterday's chat or digest to the model (rejected in ADR-029).
- A keyword guard on "ontem" (rejected in ADR-029).
- Messages mixing two days ("ontem jantei pizza e hoje comi pão"): still no rule (S15 out of scope).
- Fixing the owner's recorded lunch of 03/10 (the owner edits it in the app).

## Risks and controls

- **`today` where the user meant yesterday:** strict controls `dia-comi-ontem` and `registra-almoco-de-ontem`; a wrong auto record has Excluir (ADR-028).
- **A record request turns a non-meal into a record:** `e-de-hoje-sem-refeicao`; a record needs food in the conversation.
- **The slot marker read as the user's words** (a wrong suggestion repeated as a fact): the instruction and the digest rule treat it as a suggestion; the user naming a slot always wins.
- **Temp facts added for chatter:** `temp-pergunta-generica`; the client caps at 5 and expires in 3 days.
- **Digest drops useful context:** only record status and assumptions are banned; `digest-*` cases require content terms and a non-empty digest.
- **Prompt drift:** full suite against `master`.

## Acceptance criteria

- "…que pedi para você estimar ontem…" returns `meal_day: today` and records.
- "Registra meu almoço de ontem" never records.
- "Você não registrou, comi hoje no almoço" and "é para registrar na refeição de hoje" record in the suggested slot without asking which meal.
- "Café o mesmo de sempre" with two equal `RECENT` breakfasts returns an estimate; with different amounts it asks.
- A label message with `temp_facts: true` returns a temp add; a generic food question and a v4 request never do.
- A digest is non-empty, keeps the stated foods and the open question, and never says whether something was recorded.

## Results

<Filled at Completion: commands and real numbers, manual evidence, pending items.>

## Closure

After implementation, record real results and apply the lifecycle in `docs/sdd/README.md`.

Only an explicit owner statement cancelling this plan allows `Cancelado` and `plans/cancelled/`.

For an owner-authorized future deferral, use `Fora de escopo` and `plans/out_of_scope/` under [SDD](../../sdd/README.md#fora-de-escopo).
