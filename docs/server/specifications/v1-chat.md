# Especificacao — POST /v1/chat

## Proposed content-policy overlay

The target [content policy](../../content-policy/specifications/content-policy.md) and [identity/audit contract](../../content-policy/specifications/identity-and-audit.md) are awaiting the named CP plan approvals. They cover moderation, semantic scope, fallback checks, optional installation header and minimal audit; none is claimed as implemented by this planning update. CP2/CP3 must reconcile this live specification and the HTTP contract during delivery.

## Estado

Vigente: GET /health, POST /v1/estimate, POST /v1/fit, POST /v1/chat ([S2](../plans/completed/s2-v1-chat.md)) com `compact=true` ([S3](../plans/completed/s3-compact.md)). Timeout 60s. Cap 16 MB JPEG (22_400_000 chars de image_b64).

Desde o [S8](../plans/completed/s8-chat-json-slot-consolidado.md) (29/09/2026): saída estruturada (`json_schema` strict, `suggested_slot` com enum dos ids do perfil), texto sem JSON vira `reply`, refeição consolidada ([ADR-017](../../produto/adrs/ADR-017-registro-consolidado.md)), total sem comida sem estimate, `MAX_BODY_BYTES` 24 MB.

Desde o [S9](../plans/completed/s9-limite-texto-2000.md) (29/09/2026): `text` e `messages[].text` até 2000 caracteres ([ADR-022](../../produto/adrs/ADR-022-limite-texto-chat.md)).

Desde o [S11](../plans/completed/s11-chat-v2.md) (30/09/2026, [ADR-023](../../produto/adrs/ADR-023-chat-v2-memoria-v2.md)): intenção (`log` | `plan` | `question`), `estimate.meal_text`, memória em fatos (`facts`), `recent` (7 dias), `day.remaining_kcal`, `memory_updates` e `memory_used`. Tudo aditivo: cliente legado (sem `facts`) continua funcionando. `reasoning.effort` fica `none`, decidido pelo avaliador `server/evals/` ([S10](../plans/completed/s10-avaliacao-chat.md)).

Desde o [S12](../plans/completed/s12-slot-nomeado.md) (30/09/2026): o `suggested_slot` segue a refeição nomeada pelo usuário (na mensagem ou na fala que ela responde), mesmo com comida igual à de um slot gravado; a refeição consolidada só vale quando a mensagem se refere à refeição gravada.

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
2. Modelo gpt-6-luna, reasoning.effort=none (`config.REASONING_EFFORT`; `low` só se ganhar ≥ 10 p.p. no avaliador com p95 ≤ 20 s — ADR-023). store=false.
3. Prompt do turno = instructions fixas + `PROFILE` + `MEMORY` + `DAY` + `RECENT` + `DIGESTS` + `HISTORY` (≤12) + mensagem atual + imagem opcional. As instructions vêm primeiro e são idênticas entre turnos (cache de prompt). O `day` leva, por slot gravado, `kcal`, P/C/G e `text`, e `remaining_kcal` quando o app manda.
   - Cliente v2 (`facts` presente, mesmo vazio): `MEMORY: permanent {n}/30, dynamic {n}/40` e uma linha por fato: `{id} {category}[ slot={slot}] {key}: {text} (seen {n} days[, last {data}])`. O texto `memory` é ignorado.
   - Cliente legado (sem `facts`): `MEMORY: {memory}` como antes.
   - `RECENT` (qualquer cliente, quando vier): uma linha por refeição, `{data} {dia da semana} {slot_id} {slot_name}: "{text}" {kcal}kcal {p}P {c}C {g}G`; `slot_id` null = `Outros`.
3a. Intenção: `log` (comeu ou está comendo, ou responde a pergunta sobre essa refeição), `plan` (vai comer, quer montar, pede quantidades, pergunta se cabe) ou `question` (sem comida a estimar). Dúvida: passado = `log`; futuro, condicional ou pedido de quantidade = `plan`. Estimate em `log` e `plan`; null em `question`.
3b. `meal_text`: a refeição inteira em pt-BR com as correções da conversa, sem comentário, ≤ 160 caracteres. Nunca a resposta do usuário sozinha.
3c. Plano: `reply` com gramas por item, preparo em até 3 linhas se receita, total `kcal · P · C · G`; cabe em `remaining_kcal` quando possível, senão diz quanto passa. Não faz a conta do dia. Não pergunta: assume e diz o que assumiu (`question` null).
3d. Histórico: "o mesmo de ontem", "igual ao almoço de segunda" → usa a linha de `RECENT` daquele dia e slot. Sem registro que case → estimate null e pergunta o que foi.
3e. Memória: fato que responde a incerteza é usado, não vira pergunta, e o id vai em `memory_used`. `memory_updates` (≤ 5 por turno): frase explícita de hábito → `add` permanente (ou `replace` do fato de mesma `key`); "esquece X" → `remove`; marca/tipo/porção num `log` → `reinforce` ou `add` dinâmico; refeição que casa rotina → `reinforce`; hábito novo → `add` dinâmico `routine` com slot. Nunca refeição avulsa, números do dia ou saúde. Permanente 30/30 + frase nova → sem `add`; o `reply` pergunta "Minha memória fixa está cheia. Esqueço {fato menos visto}?".
3f. O `reply` nunca diz que registrou: quem registra é o usuário, no app.
4. Instructions: estimar se o user registrou comida; responder duvida; nunca gravar sozinho; sugerir slot pelo horario local vs slots do perfil; 1 pergunta específica (porção, tamanho, preparo) se confidence ≠ high, e só uma por refeição: respondida, confidence high e sem pergunta nova. kcal/P/C/G são o total da refeição inteira (soma dos itens). A refeição da fala, em ordem de prioridade: (1) a nomeada na mensagem atual (nome do slot do perfil ou palavra comum: café, almoço, jantar, lanche, ceia, "jantei", "almocei"); (2) senão, a nomeada na fala do user que esta mensagem responde ou continua ("cafe igual ao de ontem" → pergunta → resposta = café); (3) senão, a refeição gravada que a mensagem corrige; (4) senão, a do horário local. Refeição consolidada ([ADR-017](../../produto/adrs/ADR-017-registro-consolidado.md)) só quando a mensagem se refere à refeição gravada: nomeia aquele slot, ou é acréscimo/correção explícito ("também", "faltou", "esqueci", "na verdade", "tirando", "era X e não Y") sem nomear outra refeição. Aí, slot `eaten` → estimate da **refeição inteira** (itens gravados + mudança, a comida acrescentada sempre entra, com porção assumida se houver pergunta) com aquele slot, e o reply diz que substitui. Comida igual ou parecida à gravada não basta: com outra refeição nomeada, é refeição nova daquele slot e o reply não fala em substituir. Resposta a pergunta → reestima a mesma refeição, mesmo slot. Outro dia ("ontem") → estima se pedido, e o reply avisa que grava hoje.
4a. Total de kcal sem comida ("comi 1220 kcal"), mesmo com o slot gravado → `estimate: null` e o reply pergunta o que foi comido. Nunca estimate com p, c e g todos zero; P/C/G coerentes com kcal; sem conseguir estimar → null, nunca zeros.
5. OUT sempre JSON. O server pede ao modelo `text.format` `json_schema` strict (`chat_turn`: todo campo obrigatório, nulo explícito; compact usa o schema `digest`):
   - reply: string (prosa pt-BR)
   - intent: `log` | `plan` | `question`
   - estimate: {kcal,p,c,g,confidence,question,items,suggested_slot,meal_text} ou null (`question`)
   - memory_updates: lista de {op,id,kind,category,key,text,slot}; `id` e `memory_used` com enum dos ids de `facts` (sem fatos: `id` só null, `memory_used` string livre)
   - memory_used: lista de ids de fatos
   - digest: string ou null
   - model: gpt-6-luna
5a. Shaping (S11): `intent` inválido → deduzido (estimate = `log`, senão `question`); `question` com estimate → estimate descartado; `plan` → `question` null; `meal_text` vazio → itens (`{name} {g} g`, vírgulas), acima de 160 corta na última vírgula. `memory_updates`: descarta op/kind/category inválidos, `id` desconhecido em `reinforce`/`replace`/`remove`, `add` com `id`, `routine` sem slot válido, `key` ou `text` vazio; corta `text` 160 e `key` 40; no máximo 5. `memory_used`: só ids de `facts`, sem repetição, no máximo 10.
5b. Cliente legado (sem `facts`): `memory_updates` e `memory_used` vazios; `intent: plan` → `estimate: null` (sem card no APK ≤ 0.0.3; gramas e total no `reply`).
6. suggested_slot = id de um slot do profile.slots (enum no schema, montado por request; perfil sem slots → só `null`). Ordem da regra 4. Nomes e horários só do PROFILE: nunca supor horário "normal" de refeição (Jantar às 03:00 é o Jantar). Se hora nao casar, o mais proximo ainda vazio. Nunca inventar id. Defesa no shaping: aceita `"1"`, `1` e `{"id": 1}`, normaliza para string e descarta id fora do perfil.
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

Campos opcionais do S11 (ADR-023), limites → 422:

- `facts`: ≤ 70 itens `{id, kind, category, key, text, slot, days_seen, last_seen}`; `id` `[PD][0-9]{1,4}`; `kind` `permanent` | `dynamic`; `category` `preference` | `portion` | `routine`; `key` ≤ 40; `text` ≤ 160; `slot` id do perfil ou null; `days_seen` ≥ 0; `last_seen` data ISO ou null. Presente (mesmo vazio) = cliente v2.
- `recent`: ≤ 42 itens `{date, slot_id, slot_name, text, kcal, p, c, g}`; `text` ≤ 240; `date` ISO; `slot_id` null = "Outros".
- `day.remaining_kcal`: inteiro ou null (teto efetivo − comido, pode ser negativo).

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
- [ADR-023](../../produto/adrs/ADR-023-chat-v2-memoria-v2.md)
- [api-contract.md](../../api-contract.md) ate S2 atualizar

## Planos relacionados

- [S1](../plans/completed/s1-timeout-photo-cap.md)
- [S2](../plans/completed/s2-v1-chat.md)
- [S3 (Concluido)](../plans/completed/s3-compact.md)
- [S8 (Concluído)](../plans/completed/s8-chat-json-slot-consolidado.md)
- [S10 (Concluído)](../plans/completed/s10-avaliacao-chat.md) — avaliador `server/evals/`
- [S11 (Concluído)](../plans/completed/s11-chat-v2.md) — Chat v2
- [S12 (Concluído)](../plans/completed/s12-slot-nomeado.md) — slot nomeado vence a semelhança

## Criterios de aceite funcionais

- POST /v1/chat texto sem foto devolve reply + estimate com numeros.
- Sem tap do user o server nao grava nada (sem side effect).
- compact=true devolve digest nao-vazio e messages nao voltam no OUT.
- Foto > cap = 413 e Luna nao e chamada (asserção no teste com transport fake).
