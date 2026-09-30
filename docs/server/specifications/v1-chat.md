# Especificacao — POST /v1/chat

## Estado

Vigente: GET /health, POST /v1/estimate, POST /v1/fit, POST /v1/chat ([S2](../plans/completed/s2-v1-chat.md)) com `compact=true` ([S3](../plans/completed/s3-compact.md)). Timeout 60s. Cap 16 MB JPEG (22_400_000 chars de image_b64).

Desde o [S8](../plans/pending_manual_validation/s8-chat-json-slot-consolidado.md) (29/09/2026): saída estruturada (`json_schema` strict, `suggested_slot` com enum dos ids do perfil), texto sem JSON vira `reply`, refeição consolidada ([ADR-017](../../produto/adrs/ADR-017-registro-consolidado.md)), total sem comida sem estimate, `MAX_BODY_BYTES` 24 MB.

Desde o [S9](../plans/completed/s9-limite-texto-2000.md) (29/09/2026): `text` e `messages[].text` até 2000 caracteres ([ADR-022](../../produto/adrs/ADR-022-limite-texto-chat.md)).

## Mudanças decididas, ainda não vigentes (ADR-023)

O [S11](../plans/s11-chat-v2.md) implementa o [ADR-023](../../produto/adrs/ADR-023-chat-v2-memoria-v2.md). Até lá, valem as regras numeradas.

- Entrada opcional: `facts` (memória em fatos; presente = cliente v2), `recent` (refeições dos últimos 7 dias), `day.remaining_kcal`.
- Saída: `intent` (`log` | `plan` | `question`), `estimate.meal_text`, `memory_updates`, `memory_used`.
- Cliente legado (sem `facts`): `memory` em texto como hoje; `plan` volta com `estimate: null`.
- `reasoning.effort` decidido pelo avaliador `server/evals/` ([S10](../plans/s10-avaliacao-chat.md)): `low` só se ganhar ≥ 10 p.p. com p95 ≤ 20 s.

## Contexto e objetivo

O client deixa de usar o wizard T1/T2/T3 como caminho principal. O Chat manda perfil + snapshot do dia + ate 12 msgs + foto opcional. O server devolve prosa + estimate estruturado + slot sugerido. Stateless. Nao grava o dia.

## Escopo

- POST /v1/chat (X-Invite, JSON).
- Timeout 60s em config + httpx + OpenAI.
- Cap 16 MB no JPEG decodificado (ou 22_400_000 chars de image_b64). HTTP 413. Antes de chamar Luna.
- compact=false: uma resposta de chat.
- compact=true: resume `messages` e devolve `digest`. Nao empilha raw.
- estimate/fit continuam no ar neste corte (client para de chama-los no A5).

## Fora de escopo

- Persistencia de foto, user, dia, memoria.
- Stream.
- Gemini / Grok flagship.
- Multipart (MVP continua image_b64).
- Calcular teto no server.

## Regras funcionais

1. Auth igual estimate. 401 se X-Invite falhar.
2. Modelo gpt-6-luna, reasoning.effort=none. store=false.
3. Prompt do turno = instructions fixas + bloco perfil + memoria + day + digests + messages (≤12) + text atual + imagem opcional. As instructions vêm primeiro e são idênticas entre turnos (cache de prompt). O `day` leva, por slot gravado, `kcal`, P/C/G e `text`.
4. Instructions: estimar se o user registrou comida; responder duvida; nunca gravar sozinho; sugerir slot pelo horario local vs slots do perfil; 1 pergunta específica (porção, tamanho, preparo) se confidence ≠ high. A refeição da fala é a que ela nomeia, senão a da fala anterior do user, senão a do horário. Completar, corrigir ou remover comida de um slot `eaten` → estimate da **refeição inteira** (itens gravados + mudança) com aquele slot, e o reply diz que substitui. Resposta a pergunta → reestima a mesma refeição, mesmo slot. Outro dia ("ontem") → estima se pedido, e o reply avisa que grava hoje.
4a. Total de kcal sem comida ("comi 1220 kcal"), mesmo com o slot gravado → `estimate: null` e o reply pergunta o que foi comido. Nunca estimate com p, c e g todos zero; P/C/G coerentes com kcal; sem conseguir estimar → null, nunca zeros.
5. OUT sempre JSON. O server pede ao modelo `text.format` `json_schema` strict (`chat_turn`: todo campo obrigatório, nulo explícito; compact usa o schema `digest`):
   - reply: string (prosa pt-BR)
   - estimate: {kcal,p,c,g,confidence,question,items,suggested_slot} ou null se nao houver comida nesta fala
   - digest: string ou null
   - model: gpt-6-luna
6. suggested_slot = id de um slot do profile.slots (enum no schema, montado por request; perfil sem slots → só `null`). Se hora nao casar, o mais proximo ainda vazio. Nunca inventar id. Defesa no shaping: aceita `"1"`, `1` e `{"id": 1}`, normaliza para string e descarta id fora do perfil.
6a. confidence ≠ high sem `question` → `"Alguma porção foi diferente do que considerei?"` (`CHAT_FALLBACK_QUESTION`).
7. compact=true: Luna recebe so as messages (delimitadas, sem foto) e devolve digest ≤400 tokens (corte em 1600 chars), pt-BR, fatos (comida, kcal/P citados nas falas, slot, pulou), sem conselho. OUT: reply "", estimate null. messages vazio → 422 `compact_needs_messages` sem chamar a Luna. Falha → 200 com digest null.
8. Foto: data:image/jpeg;base64. HEIC nao entra no server — client converte.
9. Recusar image_b64 maior que o cap ANTES do LLM. Nao logar o base64.
10. Modelo respondeu texto sem JSON (nenhum `{`): o texto vira `reply`, estimate=null (log `fallback: "text_only"`). Falha LLM/timeout, saída vazia ou JSON inválido: reply curto "nao deu pra estimar", estimate=null (log `fallback: "error"`). Sem stacktrace.
11. Corpo HTTP até 24 MB (`MAX_BODY_BYTES`), acima → 413 `payload_too_large`. Cobre o cap da foto + JSON: foto acima do cap continua 413 `photo_too_large`.

## IN

```json
{
  "local_time": "2026-09-25T21:10:00-03:00",
  "profile": {
    "ceiling_kcal": 2000,
    "p_target": 160,
    "c_target": 200,
    "g_target": 67,
    "eat_back": "zero",
    "slots": [{"id": "cafe", "name": "Cafe da manha", "time": "08:00"}]
  },
  "memory": "",
  "day": {
    "date": "2026-09-25",
    "eaten_kcal": 0,
    "eaten_p": 0,
    "eaten_c": 0,
    "eaten_g": 0,
    "workout_kcal": null,
    "slots": [{"id": "cafe", "status": "empty"}]
  },
  "digests": [],
  "messages": [],
  "text": "2 paes e 2 ovos",
  "image_b64": null,
  "compact": false
}
```

status do slot: `empty` | `eaten` | `skipped`.
messages[].role: `user` | `assistant`. Sem system.

## Estados e falhas

- 401 invite.
- 413 foto > cap.
- 422 JSON invalido.
- 200 + estimate=null se a fala nao for comida (receita, duvida).
- Timeout 60s → 200 fail-soft (nao 504), mesmo shaping de falha.

## Fronteiras e ownership

- Dono: `server/`.
- Client monta profile/day/messages. Server nao consulta Room.

## Localizacao e observabilidade

- Nao logar image_b64 nem OPENAI_API_KEY.

## Decisoes relacionadas

- [ADR-012](../../produto/adrs/ADR-012-chat-home-perfil.md)
- [ADR-017](../../produto/adrs/ADR-017-registro-consolidado.md)
- [ADR-015](../adrs/ADR-015-log-conversa-dev.md)
- [api-contract.md](../../api-contract.md) ate S2 atualizar

## Planos relacionados

- [S1](../plans/completed/s1-timeout-photo-cap.md)
- [S2](../plans/completed/s2-v1-chat.md)
- [S3 (Concluido)](../plans/completed/s3-compact.md)
- [S8 (Pendente aprovação manual)](../plans/pending_manual_validation/s8-chat-json-slot-consolidado.md)

## Criterios de aceite funcionais

- POST /v1/chat texto sem foto devolve reply + estimate com numeros.
- Sem tap do user o server nao grava nada (sem side effect).
- compact=true devolve digest nao-vazio e messages nao voltam no OUT.
- Foto > cap = 413 e Luna nao e chamada (asserção no teste com transport fake).
