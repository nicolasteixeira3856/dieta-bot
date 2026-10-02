# Plan — S15 Pending meal is not a skip, a firm skip ahead is, photo with a question is `ask`, hard record cases

- Status: Aguardando aprovação
- Date: 02/10/2026
- Owning context: `server`
- Affected code: `server/` (`llm.py`, `evals/cases/`, `tests/`)
- Prerequisites: [S14](completed/s14-registro-autonomo.md) (`Concluído`). Refines [ADR-028](../../produto/adrs/ADR-028-registro-autonomo.md) decisions 2 and 4 within their text; no new ADR. Does not block [A34](../../android/plans/a34-registro-autonomo.md): the response shape does not change.

## Authorization gate

This plan is documentation only. Implementation starts only after an explicit approval naming this file:

> Aprovo o plano `docs/server/plans/s15-registro-casos-dificeis.md`. Implemente o plano aprovado.

If implementation reveals an uncovered decision, stop, update the artifacts and ask for a new approval.

## Goal

Close the two record errors found by the `none` vs `low` comparison of 02/10/2026, and keep its hard cases in the evaluator so the next prompt or model change is measured on them.

## Background (02/10/2026)

21 hard v4 cases (written in the scratchpad, not in the repo) plus the 68 existing ones, 3 repetitions, `none` vs `low`:

| | `none` | `low` |
|---|---|---|
| Whole suite (89) | 88/89 (98.9%) | 85/89 (95.5%) |
| Hard cases (21) | 20/21 | 18/21 |
| p50 / p95 | 3.0 s / 4.2 s | 3.0 s / 5.9 s |
| Cost (267 calls) | US$ 0.030 | US$ 0.041 |

Reports: `logs/evals/2026-10-02-150548-none.json`, `logs/evals/2026-10-02-151043-low.json` (outside git).

- `low` did not win any case `none` lost. `reasoning.effort` stays `none` (ADR-023 rule: ≥ 10 p.p. gain needed).
- Both efforts: "ainda não almocei" at 11:40 came back `intent: skip`, `skip_slot` almoço, `record: auto` in 2 of 3 runs. In the app that marks a pending lunch as skipped.
- `low` only: a food photo with "isso tem muita caloria?" came back `log` + `clear` → `auto` (0/3). The spec did not define it; owner decision below.

## Owner decisions (02/10/2026)

- A skip announced ahead counts as a skip when it is firm: "hoje não vou jantar", "vou pular o almoço hoje" → `intent: skip`, `skip_slot` = that slot → `record: auto` (the client skips the empty slot with a receipt that has Desfazer, A34). A hedged one ("acho que não vou jantar", "talvez eu pule a janta") is not a skip: `intent: question`, `skip_slot: null`, nothing marked. The skip gate (S14 row 4) does not read `record_intent`, so the line between firm and hedged lives in the instruction.
- A food photo sent with a question about it ("isso tem muita caloria?", "quanto tem isso?") is `intent: log`, estimate present, `record_intent: unsure` → `record: ask`. The user sees the estimate and one **Registrar** button. Only a photo with **no** text stays `clear` (`auto`).

## Implementation scope

### 1. Instructions (`_CHAT_INSTRUCTIONS`, English, all clients)

- **Pending meal:** "ainda não almocei", "não jantei ainda", "ainda vou almoçar" say the meal has not happened **yet**. That is not `skip`: `intent: question` (or `plan` when the user asks what to eat), `skip_slot: null`. `skip` = the meal did not happen ("pulei o café", "hoje não almocei", without "ainda") or the user firmly says it will not happen today ("hoje não vou jantar", "vou pular o almoço hoje").
- **Hedged skip:** "acho que não vou jantar", "talvez eu pule a janta", "não sei se vou almoçar" are not `skip`: `intent: question`, `skip_slot: null`.
- **Photo with a question:** a food photo with a question about it is `log`: estimate the plate, `record_intent: unsure`. A photo with no text is `clear` (unchanged).

No change to the schema, shaping, gates or response shape. Legacy, v2 and v3 responses unchanged except for the model's own text.

### 2. Evaluator (`server/evals/cases/`, new files only)

The 21 hard cases of the comparison, `since: v4`, tag `record-hard`, with `clarify_rounds: 3` except where the case is about the answer (`hard-resposta-curta`, `clarify_rounds: 1`), as in S14:

| Case | Message (context) | Expect |
|---|---|---|
| `hard-mudou-ideia` | "ia comer pizza mas acabei comendo um sanduíche natural de frango" (20:10) | `log`, `auto`, slot jantar, meal_text has sanduíche, not pizza |
| `hard-plano-vira-passado` | "acho que vou comer pizza... na real já comi, 2 fatias de calabresa" | `log`, `auto`, meal_text has pizza |
| `hard-hipotetico` | "se eu comer um açaí de 500 ml com granola, quanto fica?" | `plan`, `none` |
| `hard-receita` | "como faço uma panqueca de banana com aveia?" | `plan`, `none` |
| `hard-registra-sem-comida` | "registra aí" (no food, no history) | estimate absent, `none` |
| `hard-foto-pergunta` | food photo + "isso tem muita caloria?" | `log`, estimate present, **`ask`** |
| `hard-pergunta-continuacao` | "e brigadeiro?" after a nutrition Q&A | `question`, `none` |
| `hard-hoje-de-manha` | "hoje de manhã comi 2 ovos mexidos e uma tapioca com queijo" (15:30) | `log`, `auto`, slot café |
| `hard-dia-semana-passado` | "na quinta passada comi uma feijoada completa" (today is Thursday) | `none`, reply has hoje |
| `hard-ontem-a-noite` | "ontem à noite comi um hambúrguer com batata frita" | `none`, reply has hoje |
| `hard-madrugada-pao` | "comi um pão com manteiga agora" (00:40) | `log`, `auto` |
| `hard-madrugada-janta-foi` | "a janta foi um prato de feijoada" (02:00, Jantar 18:00) | `none` |
| `hard-pulei-sobremesa` | "pulei a sobremesa hoje" (no such slot) | `none`, skip_slot null |
| `hard-ainda-nao-almocei` | "ainda não almocei" (11:40) | `none`, skip_slot null, intent not skip |
| `hard-hoje-nao-jantei` | "hoje não jantei" (23:00) | `skip`, `auto`, skip_slot jantar |
| `hard-acrescimo-dois-slots` | "também comi um pudim" (almoço and jantar recorded, 21:30) | `log`, `auto`, slot jantar, meal_text has frango and pudim |
| `hard-correcao-bife` | "na verdade o bife era frango grelhado" (almoço recorded) | `log`, `auto`, slot almoço, meal_text has frango, not bife |
| `hard-ceia-igual-jantar` | "na ceia comi o mesmo arroz com frango, metade do prato" (jantar recorded) | `log`, `auto`, slot ceia |
| `hard-plano-depois-comi` | "comi exatamente isso, pode registrar" after a plan reply | `log`, `auto`, slot jantar, meal_text has macarr |
| `hard-resposta-curta` | "2" answering "Quantos pães?" | `log`, `auto`, slot café, meal_text has 2 |
| `hard-tirou-o-pao` | "na verdade não comi o pão, só o ovo" after an unrecorded café estimate | `log`, `auto`, slot café, meal_text without pão |

New, for this plan's rules:

| Case | Message | Expect |
|---|---|---|
| `hard-nao-jantei-ainda` | "não jantei ainda, o que como?" (19:00) | `plan`, `none`, skip_slot null |
| `hard-nao-vou-jantar` | "hoje não vou jantar" (19:00) | `skip`, `auto`, skip_slot jantar |
| `hard-vou-pular-almoco` | "vou pular o almoço hoje" (10:30) | `skip`, `auto`, skip_slot almoço |
| `hard-acho-que-nao-janto` | "acho que não vou jantar hoje" (19:00) | `none`, skip_slot null |
| `hard-foto-quanto-tem` | food photo + "quanto tem isso?" | `log`, estimate present, `ask` |

Every case adds `refusal: none`. Expectations follow v1-chat rules 3a, 3g, 4 and 5d with this plan's two rules. For the pending-meal cases `record: none` and `skip_slot: null` are enough: a wrong `skip` with the slot fails both.

### 3. Tests (`server/tests/`)

- `test_record.py`: the instructions carry the pending-meal and photo-question rules (text assertions, as S14).

## Affected files and areas

- `server/llm.py`, `server/evals/cases/hard-*.json` (26 new files), `server/tests/test_record.py`.
- Docs in the same delivery: [v1-chat](../specifications/v1-chat.md) rules 3a and 3g (remove the S15 proposal note); [api-contract](../../api-contract.md) `intent: skip` line; server README; this plan's result.

## Planned validation

1. `pytest server/tests` green.
2. `python -m evals.run --effort none --repeat 3`: every `record-hard` case ≥ 2/3; `hard-ainda-nao-almocei`, `hard-nao-jantei-ainda` and `hard-acho-que-nao-janto` 3/3 (a wrong skip is the costly error); the other 68 cases no regression against S14 (`memoria-cheia` and `ceia-completa-suco` are already unstable; compare with a 6-repetition run if they fail). Record pass rate and p95 here.
3. Deploy to the dev server (`tools/deploy-gcp.ps1`), one real v4 request "ainda não almocei" with `X-Request-Id: s15-*`: `record: none`, log `record` ≠ `auto_skip`.

## Out of scope

- `reasoning.effort` (stays `none`; the comparison above is the evidence).
- Schema, gates, response shape, Android (A34).
- Messages with two days or two meals ("ontem jantei pizza e hoje no café comi pão"): no rule yet; a separate decision.
- A server-side guard on the word "ainda": the model rule plus cases are the control; a keyword guard would misfire on "ainda comi um pão".

## Risks and controls

- **A firm skip ahead turns out wrong** (the user eats after all): the receipt's Desfazer removes the skip, and a meal sent for a skipped slot is recorded by itself (A34). The hedged-skip case guards the opposite error.
- **The rule makes real skips less frequent** ("hoje não almocei" read as pending): cases `hard-hoje-nao-jantei` and `registro-pulei` must keep passing.
- **Photo + question now asks instead of recording:** intended (owner decision); one tap on Registrar.
- **Prompt drift on other cases:** the full suite runs; failures are compared with `master`.

## Acceptance criteria

- "ainda não almocei" returns `record: none`, never `intent: skip`.
- "hoje não vou jantar" returns `intent: skip`, `skip_slot` jantar, `record: auto`; "acho que não vou jantar" returns `record: none`.
- A food photo with a question returns `record: ask` with the estimate.
- A food photo with no text still returns `record: auto`.
- All `record-hard` cases ≥ 2/3, the two pending-meal cases 3/3.

## Closure

After implementation, record real results and apply the lifecycle in `docs/sdd/README.md`.

Only an explicit owner statement cancelling this plan allows `Cancelado` and `plans/cancelled/`.

For an owner-authorized future deferral, use `Fora de escopo` and `plans/out_of_scope/` under [SDD](../../sdd/README.md#fora-de-escopo).
