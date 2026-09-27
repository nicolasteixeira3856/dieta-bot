# Plano — S2 POST /v1/chat

- Estado: Concluído
- Data: 26/09/2026
- Contexto proprietário: `server`
- Código afetado: `server/`
- Pré-requisitos: [s1-timeout-photo-cap.md](s1-timeout-photo-cap.md) (concluído)

## Gate de autorização

> Aprovo o plano `docs/server/plans/s2-v1-chat.md`. Implemente o plano aprovado.

## Objetivo

Nasce POST `/v1/chat`. Stateless. Devolve prosa + estimate estruturado + suggested_slot. estimate/fit permanecem.

## Fontes de verdade

- [v1-chat.md](../../specifications/v1-chat.md)
- [ADR-012](../../../produto/adrs/ADR-012-chat-home-perfil.md)
- [api-contract.md](../../../api-contract.md)

## Escopo de implementação

### 1. Modelos Pydantic

- `SlotIn`: `{id, name, time}`
- `ProfileIn`: `{ceiling_kcal, p_target, c_target, g_target, eat_back, slots}`
- `DaySlotIn`: `{id, status, text?, kcal?, p?, c?, g?}`
- `DayIn`: `{date, eaten_kcal, eaten_p, eaten_c, eaten_g, workout_kcal, slots}`
- `ChatMessageIn`: `{role, text: Field(..., max_length=1000)}`
- `ChatIn`: `{local_time, profile, memory, day, digests, messages, text: Field(..., max_length=1000), image_b64, compact=false}`
- Validações: `role` em `{"user", "assistant"}`, `status` em `{"empty", "eaten", "skipped"}`, `len(messages) <= 12`, `len(digests) <= 2`.

### 2. Rota

- POST `/v1/chat`, header `X-Invite` (validação em tempo constante).
- Rate limiting com `slowapi`: max 30 req/min por IP/invite (proteção contra Denial of Wallet).
- `compact=true` neste plano: 400 `{"detail":"compact_not_enabled"}` — S3 ligará o compactador real.
- Cap de foto igual a S1 (`PHOTO_MAX_B64_CHARS = 22_400_000`, 413 `photo_too_large` antes do LLM).
- `llm.chat_json(...)` executado com timeout e fail-soft em erro.
- `shape_chat(payload)`: defaults numéricos 0; `suggested_slot` validado contra `profile.slots` (descartado para `None` se inventado).

### 3. LLM & Segurança de Prompt

- `_CHAT_INSTRUCTIONS` conforme especificação regra 4.
- `user_text` estruturado com seções PROFILE / MEMORY / DAY / DIGESTS / HISTORY.
- Isolamento estrito de prompt injection: mensagem do usuário delimitada entre `### USER_MESSAGE_START` e `### USER_MESSAGE_END` com aviso instruindo a tratar o conteúdo estritamente como dado de refeição.
- Parse JSON keys: `reply`, `estimate`, `digest`, `model`.

### 4. Shaping

- `estimate` null se a mensagem não descrever refeição ingerida ou planejada.
- `question` presente apenas se `confidence != high`.
- `model` sempre `gpt-6-luna`.

### 5. Contrato

- `docs/api-contract.md` atualizado com especificação completa de POST `/v1/chat` (IN/OUT).
- Endpoints `/v1/estimate` e `/v1/fit` preservados intactos.

## Arquivos e áreas afetadas

- `server/main.py`
- `server/config.py`
- `server/llm.py`
- `server/shaping.py`
- `server/tests/test_chat.py` (novo arquivo de testes)
- `docs/api-contract.md`
- `docs/server/README.md`

## Validação planejada

1. `pytest server/tests -q`
2. Chat texto "2 paes e 2 ovos" + profile.slots cafe → 200, reply string, estimate.kcal int, suggested_slot e id do perfil
3. Chat "o que e TMB?" → estimate is null, reply não-vazio
4. Sem X-Invite → 401
5. compact=true → 400 neste plano
6. image_b64 gigante → 413

## Validação executada

1. `pytest tests -v` executado com sucesso: 37 passed, 16 subtests passed em 5.35s (100% sucesso).
2. `test_chat_food_returns_reply_estimate_and_suggested_slot`:
   - POST `/v1/chat` com "2 paes e 2 ovos" retorna 200 OK.
   - `reply`: string em pt-BR.
   - `estimate`: kcal=450, macros, confidence="high", question=None, items=[...], suggested_slot="cafe".
3. `test_chat_general_question_returns_null_estimate`:
   - Pergunta geral ("o que e TMB?") retorna 200 OK, `reply` explicativo não-vazio e `estimate: null`.
4. `test_chat_without_invite_returns_401`:
   - Sem header ou com invite incorreto retorna 401 `{"detail": "unauthorized"}` sem chamar o modelo.
5. `test_chat_compact_true_returns_400`:
   - `compact=true` retorna 400 `{"detail": "compact_not_enabled"}` sem chamar o modelo.
6. `test_chat_photo_over_cap_returns_413_and_skips_model`:
   - `image_b64` > 22.4M chars retorna 413 sem chamar o modelo; foto não é retida em disco nem em logs.
7. `test_chat_photo_under_cap_reaches_model_and_is_not_retained`:
   - Foto válida é enviada ao transporte do LLM e liberada da memória no bloco `finally`.
8. `test_chat_model_failure_returns_fail_soft_200`:
   - Erro de conexão/timeout retorna fail-soft 200 `{"reply": "nao deu pra estimar", "estimate": null}`.
9. `test_chat_hallucinated_slot_is_discarded_to_null`:
   - Slot retornado pelo modelo fora de `profile.slots` é descartado para `null`.
10. `test_chat_rate_limiting_triggers_at_31st_request`:
    - 30 requisições aceitas, 31ª bloqueada com 429 `rate_limit_exceeded`.
11. `test_chat_pydantic_validation_limits`:
    - Textos > 1000 chars, > 12 mensagens, > 2 digests, roles inválidos e status inválidos rejeitados com 422.
12. `test_chat_prompt_injection_is_strictly_delimited`:
    - Mensagem do usuário encapsulada em `### USER_MESSAGE_START` e `### USER_MESSAGE_END` com instrução restritiva de segurança.

## Fora de escopo

- Compact real (S3)
- Android (A5)
- Stream
- Apagar estimate/fit
- Guardar memória no server

## Critérios de aceite

- `/v1/chat` existe e passa todos os testes
- `/v1/estimate` e `/v1/fit` continuam funcionando perfeitamente
- Luna não é chamada quando 413/401/400

## Encerramento

Ciclo SDD em `docs/sdd/README.md`. Concluído.
