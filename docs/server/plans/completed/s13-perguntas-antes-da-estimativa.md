# Plan — S13 Questions before the estimate, 3-round hard stop

- Status: Concluído
- Approval: 30/09/2026 (owner: "Aprovo o plano docs/server/plans/s13-perguntas-antes-da-estimativa.md. Implemente o plano aprovado.")
- Date: 30/09/2026
- Owning context: `server`
- Affected code: `server/` (`main.py`, `llm.py`, `shaping.py`, `config.py`, `conversation_log.py`, `evals/`, `tests/`)
- Prerequisites: none. Executes [ADR-026](../../../produto/adrs/ADR-026-perguntas-antes-da-estimativa.md) decisions 1–3, 5 and 6. Opt-in by request field: no Android change needed to ship. Blocks [A30](../../../android/plans/a30-perguntas-antes-da-estimativa.md).

## Authorization gate

This plan is documentation only. Implementation starts only after an explicit approval naming this file:

> Aprovo o plano `docs/server/plans/s13-perguntas-antes-da-estimativa.md`. Implemente o plano aprovado.

If implementation reveals an uncovered decision, stop, update the artifacts and ask for a new approval.

## Goal

`POST /v1/chat` can answer a `log` turn with a question only (no estimate) while the AI has material doubts, and releases the estimate in code after at most 3 question rounds, on a repeated question, or on request. Legacy clients see no change.

## Sources of truth

- [ADR-026](../../../produto/adrs/ADR-026-perguntas-antes-da-estimativa.md), [ADR-023](../../../produto/adrs/ADR-023-chat-v2-memoria-v2.md), [v1-chat](../../specifications/v1-chat.md), [api-contract](../../../api-contract.md).
- Skill `fastapi-security` (input limits, LLM input boundaries).

## Implementation scope

### 1. Request (`ChatIn`), additive

- `clarify_rounds: int | None`, `0 ≤ n ≤ 3` (422 outside). Present = client that renders question-only turns ("v3 client"). Absent = today's behaviour, byte for byte.
- `force_estimate: bool = False`. Ignored unless `clarify_rounds` is present.

### 2. Response (`ChatOut`), additive

- New top-level `question: str | None`. Non-null only on a question-only turn; then `estimate` is `null`, `intent` is `log`.
- On a question-only turn, `reply` is the history text for the app to store and send back: `Entendi: {meal_text}.` + newline + the question (the draft meal text of the discarded estimate; without it, just the question). The app does not display `reply` on these turns.
- Legacy clients keep `estimate.question`; v3 clients always get `estimate.question = null`.

### 3. Instructions (`_CHAT_INSTRUCTIONS`, English, all clients)

Replace "One question per meal: after the user answers it, confidence is high and question is null." with:

- question lists every open doubt of the meal in one message, at most 3 short questions, each specific (portion, size, preparation, ingredient);
- after an answer, ask again only about what is still unknown and changes the estimate materially; otherwise confidence is high and question is null;
- never repeat a question already asked in HISTORY;
- if confidence is not high, reply states in one short line what was assumed;
- the answer to a question is a `log` of the same meal (today's rule, kept).

The JSON schema does not change: the model keeps returning `estimate` with `confidence` and `question`.

### 4. Release gate (`shaping.py`, pure function, unit-tested)

For a v3 client, `intent = log` and an estimate present, decide `ask` or `release`:

| Order | Condition | Result | Log `clarify` |
|---|---|---|---|
| 1 | `force_estimate` | release | `released_force` |
| 2 | confidence high, or question empty | release | `released_confident` |
| 3 | `clarify_rounds ≥ 3` | release | `released_cap` |
| 4 | question repeats an assistant turn of `messages` | release | `released_repeat` |
| 5 | otherwise | ask | `asked` |

- Release: `estimate` returned with `question: null`, confidence as the model gave it; top-level `question: null`.
- Ask: `estimate: null`, `question` = the model's question, `reply` as in §2. `memory_updates` and `memory_used` pass unchanged (a preference answered in the conversation still applies now; routines wait for a record as today).
- v3 clients never receive `CHAT_FALLBACK_QUESTION` (a generic question with no estimate makes no sense); the fallback stays for legacy clients.
- Repetition: normalise both texts (casefold, strip accents, keep `[a-z0-9]`, drop pt-BR stop words from a short fixed list); repeat when token Jaccard ≥ 0.6 or one token set contains the other. Constants in `config.py` (`CLARIFY_MAX_ROUNDS = 3`, `CLARIFY_REPEAT_JACCARD = 0.6`).
- `plan`, `question` and estimate-null turns are untouched.

### 5. Observability

- Dev conversation log ([ADR-015](../../adrs/ADR-015-log-conversa-dev.md)): add `clarify` (enum above, or `none`) and `clarify_rounds` (number). No user text in new fields.

### 6. Evaluator (`server/evals/`)

- New expectation keys in `checks.py`: `top_question` (`present`/`absent`), `top_question_not` (terms).
- New cases (v3 requests, `clarify_rounds` set):
  - `pergunta-antes-jantar`: "jantei macarrão com frango ao molho branco" → `estimate: absent`, `top_question: present`.
  - `pergunta-todas-de-uma-vez`: meal with two unknowns → one question message mentioning both (`top_question` terms).
  - `pergunta-resposta-libera`: history with question + answer, `clarify_rounds: 1` → estimate present, `kcal_range`.
  - `pergunta-sem-repetir`: history where the leite question was answered → `top_question_not` leite.
  - `pergunta-limite-3`: `clarify_rounds: 3` → estimate present, `top_question: absent` (gate, deterministic).
  - `pergunta-forcada`: `force_estimate: true`, text "Pode estimar assim." → estimate present.
- Existing cases keep passing unchanged (no `clarify_rounds` → legacy path).
- Run: 3 repetitions per case with the current effort (`none`), record pass rate and p95 latency here.

## Affected files and areas

- `server/main.py`, `server/llm.py`, `server/shaping.py`, `server/config.py`, `server/conversation_log.py`.
- `server/evals/checks.py`, `server/evals/cases/*.json` (new files only).
- `server/tests/test_chat.py`, new `server/tests/test_clarify.py`.
- Docs in the same delivery: [v1-chat](../../specifications/v1-chat.md) rules 4, 5, 6a and IN/OUT; [api-contract](../../../api-contract.md) `/v1/chat`; server README; this plan's result.

## Planned validation

1. `pytest server/tests` green, with unit tests for the gate table (each row), the repetition function (accents, stop words, paraphrase below threshold), `clarify_rounds` 422 bounds, legacy path unchanged (snapshot of an existing request/response).
2. `python -m server.evals.run` with 3 repetitions: all new cases pass ≥ 2/3; existing cases no regression vs [S12](s12-slot-nomeado.md) (29/29).
3. Deploy to the dev server (`tools/deploy-gcp.ps1`), then one real v3 request by `curl` with `X-Request-Id: s13-*` and the log line checked for `clarify`.

## Out of scope

- Android (A30), Stitch (ST7).
- `plan` asking questions.
- A second LLM call to force an estimate.
- Changing `reasoning.effort`.

## Risks and controls

- **Model keeps asking trivial questions:** the 3-round cap, the force button (A30) and the "materially changes the estimate" rule; evaluator cases measure it.
- **Repetition false positive releases too early:** the released estimate is still the model's full re-estimate with assumptions stated; threshold tunable in `config.py`.
- **Legacy regression:** every change behind `clarify_rounds` presence; snapshot test.

## Acceptance criteria

- A v3 `log` turn with a doubt returns `estimate: null` and a non-empty top-level `question`.
- With `clarify_rounds: 3`, or `force_estimate: true`, or a repeated question, the response has an estimate and no question, whatever the model returned.
- A request without `clarify_rounds` returns the same shape as before S13.

## Closure

After implementation, record real results and apply the lifecycle in `docs/sdd/README.md`.

Only an explicit owner statement cancelling this plan allows `Cancelado` and `plans/cancelled/`.

For an owner-authorized future deferral, use `Fora de escopo` and `plans/out_of_scope/` under [SDD](../../../sdd/README.md#fora-de-escopo).

## Results (30/09/2026)

### Implemented

- `ChatIn.clarify_rounds` (0–3, 422 outside) and `force_estimate`; `config.CLARIFY_MAX_ROUNDS = 3`, `CLARIFY_REPEAT_JACCARD = 0.6`.
- `shaping.clarify_gate` (table § 4, in order) and `shaping.repeats_question`; `main.shape_chat_turn` = `shape_chat` + gate for a v3 client, shared by the route and the evaluator. v3 fallbacks (error, text only) carry `question: null`.
- `_CHAT_INSTRUCTIONS`: "One question per meal" replaced by § 3. The `LOG:` sentence that asked "one specific question about the biggest uncertainty" now asks for every open doubt (at most 3 short questions), so the two rules do not contradict.
- Dev conversation log: `clarify` and `clarify_rounds` on every `chat` line (`none` / `null` for a legacy client).
- Evaluator: `top_question` (`present` | `absent` | list of terms = present and naming every term, used by `pergunta-todas-de-uma-vez`), `top_question_not`; 6 `since: v3` cases with tag `clarify`.

### Implementation details within the plan

- Repetition compares the new question with the **question sentences** (ending in `?`) of each assistant turn, not with the whole turn. A whole estimate reply ("…200 ml de leite…") would otherwise contain a short new question ("Qual o leite?") and release it as a false repeat.
- First full run: `pergunta-resposta-libera` 0/3. After the answer, the model asked a sub-detail ("quanto creme de leite?"). The § 3 line "ask again only about what is still unknown and changes the estimate materially" was made concrete: "only about a food the answers left with no portion at all … After an answer, amounts of sauce, oil, cream, cheese or seasoning are assumed, never asked." Then 3/3.

### Validation

1. `pytest server/tests`: 120 passed (85 subtests). `test_clarify.py`: each gate row, ask reply format, plan/question/null untouched, repetition (accents, stop words, subset, paraphrase below threshold, question sentences only, empty), `clarify_rounds` −1/4 → 422 and 0/3 → 200, legacy snapshot byte for byte (checked against `master` before S13), legacy fallback question kept, v3 fallback `question: null`, log fields.
2. `python -m evals.run --effort none --repeat 3` (final prompt, report `logs/evals/2026-09-30-210107-none.json`): **35/35** (100%). New cases 6/6, each 3/3: `pergunta-antes-jantar`, `pergunta-todas-de-uma-vez`, `pergunta-resposta-libera`, `pergunta-sem-repetir`, `pergunta-limite-3`, `pergunta-forcada`. Existing 29/29, no regression vs S12 (`ceia-completa-suco` and `ontem-grava-hoje` 2/3). Latency p50 2212 ms, p95 2847 ms. US$ 0.0138 for 105 calls.
3. `tools/deploy-gcp.ps1` (code only): `/health` 200. Real v3 request (`pergunta-antes-jantar` body, `clarify_rounds: 0`, `X-Request-Id: s13-check-1`): `intent: log`, `estimate: null`, `question` "A porção de macarrão e frango foi aproximadamente essa?", `reply` `Entendi: …` + the question. Log line: `"clarify": "asked", "clarify_rounds": 0`.

### Pending

- None for S13. The Android side (question-only turns, round count, Forçar estimativa) is [A30](../../../android/plans/a30-perguntas-antes-da-estimativa.md), which accepts ADR-026 on completion.
