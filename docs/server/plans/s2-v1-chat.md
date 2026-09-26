# Plano — S2 POST /v1/chat

- Estado: Aguardando aprovacao
- Data: 25/09/2026
- Contexto proprietario: `server`
- Codigo afetado: `server/`
- Pre-requisitos: [s1-timeout-photo-cap.md](s1-timeout-photo-cap.md)

## Gate de autorizacao

> Aprovo o plano `docs/server/plans/s2-v1-chat.md`. Implemente o plano aprovado.

## Objetivo

Nasce POST `/v1/chat`. Stateless. Devolve prosa + estimate estruturado + suggested_slot. estimate/fit permanecem.

## Fontes de verdade

- [v1-chat.md](../specifications/v1-chat.md)
- [ADR-012](../../produto/adrs/ADR-012-chat-home-perfil.md)

## Escopo de implementacao

### 1. modelos Pydantic

- SlotIn `{id, name, time}`
- ProfileIn `{ceiling_kcal, p_target, c_target, g_target, eat_back, slots}`
- DaySlotIn `{id, status, text?, kcal?, p?, c?, g?}`
- DayIn `{date, eaten_kcal, eaten_p, eaten_c, eaten_g, workout_kcal, slots}`
- ChatMessageIn `{role, text}`
- ChatIn `{local_time, profile, memory, day, digests, messages, text, image_b64, compact=false}`
- Validar role em `{user,assistant}`, status em `{empty,eaten,skipped}`, `len(messages)<=12`, `len(digests)<=2`

### 2. rota

- POST `/v1/chat`, header `X-Invite`
- `compact=true` neste plano: 400 `{"detail":"compact_not_enabled"}` — S3 liga
- cap foto igual S1
- `llm.chat_json(...)`
- `shape_chat(payload)`: defaults numericos 0; `suggested_slot` so se id esta em `profile.slots`

### 3. llm

- `_CHAT_INSTRUCTIONS` conforme spec regra 4
- input_text com secoes PROFILE / MEMORY / DAY / DIGESTS / HISTORY / USER
- Imagem igual estimate (jpeg base64)
- Parse JSON keys `reply`, `estimate`, `digest`, `model`

### 4. shaping

- `estimate` null se a fala nao for comida
- `question` so se confidence != high
- `model` sempre `gpt-6-luna`

### 5. contrato

- `docs/api-contract.md` ganha secao POST `/v1/chat` (IN/OUT da spec)
- estimate/fit nao saem

## Arquivos e areas afetadas

- `server/main.py`
- `server/llm.py`
- `server/shaping.py`
- `server/tests/test_chat.py`
- `docs/api-contract.md`
- `docs/server/README.md`

## Validacao planejada

1. `pytest server/tests -q`
2. chat texto "2 paes e 2 ovos" + profile.slots cafe → 200, reply string, estimate.kcal int, suggested_slot e id do perfil
3. chat "o que e TMB?" → estimate is null, reply nao-vazio
4. sem X-Invite → 401
5. compact=true → 400 neste plano
6. image_b64 gigante → 413

## Fora de escopo

- compact real (S3)
- Android
- Stream
- Apagar estimate/fit
- Guardar memoria no server

## Riscos e controles

- **prompt longo:** prefixo perfil+day+2 digest+12 msgs. Aceito. Sem cache neste plano.
- **modelo inventa slot:** shaping descarta id fora da lista.

## Criterios de aceite

- `/v1/chat` existe e passa os testes acima
- `/v1/estimate` e `/v1/fit` ainda passam
- Luna nao e chamada quando 413/401/400

## Encerramento

Ciclo SDD em `docs/sdd/README.md`.
