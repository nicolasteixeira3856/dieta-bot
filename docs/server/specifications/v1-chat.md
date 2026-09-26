# Especificacao — POST /v1/chat

## Estado

Contrato vigente: GET /health, POST /v1/estimate, POST /v1/fit. Timeout 20s. Sem /v1/chat. Sem digest. Foto sem cap de bytes.

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
3. Prompt do turno = instructions fixas + bloco perfil + memoria + day + digests + messages (≤12) + text atual + imagem opcional.
4. Instructions: estimar se o user registrou comida; responder duvida; nunca gravar sozinho; sugerir slot pelo horario local vs slots do perfil; 1 pergunta se confidence ≠ high.
5. OUT sempre JSON:
   - reply: string (prosa pt-BR)
   - estimate: {kcal,p,c,g,confidence,question,items,suggested_slot} ou null se nao houver comida nesta fala
   - digest: string ou null
   - model: gpt-6-luna
6. suggested_slot = id de um slot do profile.slots. Se hora nao casar, o mais proximo ainda vazio. Nunca inventar id.
7. compact=true: Luna recebe so as messages e devolve digest ≤400 tokens, pt-BR, com kcal/P citados se existirem nas falas. reply pode ser "".
8. Foto: data:image/jpeg;base64. HEIC nao entra no server — client converte.
9. Recusar image_b64 maior que o cap ANTES do LLM. Nao logar o base64.
10. Falha LLM/timeout: reply curto "nao deu pra estimar", estimate=null. Sem stacktrace.

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

- [ADR-012](../produto/adrs/ADR-012-chat-home-perfil.md)
- [api-contract.md](../api-contract.md) ate S2 atualizar

## Planos relacionados

- [S1](../plans/s1-timeout-photo-cap.md)
- [S2](../plans/s2-v1-chat.md)
- [S3](../plans/s3-compact.md)

## Criterios de aceite funcionais

- POST /v1/chat texto sem foto devolve reply + estimate com numeros.
- Sem tap do user o server nao grava nada (sem side effect).
- compact=true devolve digest nao-vazio e messages nao voltam no OUT.
- Foto > cap = 413 e Luna nao e chamada (asserção no teste com transport fake).
