# Plano — S8 Chat: JSON garantido, slot sugerido e refeição consolidada

- Estado: Pendente aprovação manual
- Data: 29/09/2026 (aprovado e implementado em 29/09/2026)
- Contexto proprietário: `server`
- Código afetado: `server/llm.py`, `server/shaping.py`, `server/main.py`, `server/config.py`, `server/tests/`
- Pré-requisitos: Nenhum. Aceita o [ADR-017](../../../produto/adrs/ADR-017-registro-consolidado.md) (regras de IA) e a parte server do [ADR-018](../../../android/adrs/ADR-018-foto-2048.md).

## Gate de autorização

Este plano é exclusivamente documental. A implementação só começa após aprovação explícita que identifique este arquivo:

> Aprovo o plano `docs/server/plans/s8-chat-json-slot-consolidado.md`. Implemente o plano aprovado.

Se a implementação revelar decisão não coberta, pare, atualize os artefatos e peça nova aprovação.

## Objetivo

Acabar com o "nao deu pra estimar" quando a IA respondeu, fazer o "Gravar {slot}" aparecer, reestimar a refeição inteira quando o usuário completa uma refeição já gravada e nunca estimar a partir de um total de kcal sem comida.

## Diagnóstico (log do server de dev, 28–29/09, 23 chamadas)

| Sintoma relatado | Causa no log |
|---|---|
| "não deu para estimar" depois de "tamanho pequeno a lasanha…" (`dada7406`) | A IA respondeu em texto livre, sem JSON ("Entendi: a lasanha era um pedaço pequeno… Quer que eu corrija a estimativa anterior?"). `_parse_json_object` → `ValueError("no json")` → `fail_chat()`. O server não pede saída estruturada ao modelo. |
| "Gravar café" nunca aparece, só "Trocar" | Em **17 de 17** estimativas o modelo mandou o slot certo, mas como número (`"suggested_slot":1`) ou objeto (`{"id":2,...}`). `shape_chat` só aceita `str` → `suggested_slot: null` sempre. |
| Ceia com 1220 kcal e P/C/G zerados (`corrija, na minha ceia eu consumi aproximadamente 1220kcal`) | O prompt não proíbe estimar a partir de um total informado. Saiu `{kcal:1220, p:0, c:0, g:0, confidence:"high"}`. |
| Suco virou um segundo registro na Ceia | O prompt não manda reestimar a refeição inteira quando o slot já está `eaten`. A IA estimou só o suco (220 kcal). |
| Pergunta "descreve em 1 linha" depois de uma descrição completa (5 de 17) | O modelo deu `confidence` média e `question: null`. `shape_chat` troca pelo `FALLBACK_QUESTION` genérico. |
| (latente) foto de ~15–16 MB | `MAX_BODY_BYTES` (20 MB) < cap do `image_b64` (22,4 M chars): 413 `payload_too_large` antes de chegar no cap certo. |

## Fontes de verdade

- [v1-chat](../../specifications/v1-chat.md), [api-contract.md](../../../api-contract.md), [chat](../../../produto/specifications/chat.md).
- [ADR-017](../../../produto/adrs/ADR-017-registro-consolidado.md), [ADR-018](../../../android/adrs/ADR-018-foto-2048.md), [ADR-015](../../adrs/ADR-015-log-conversa-dev.md).

## Escopo de implementação

### 1. Saída estruturada no `/v1/chat` (`llm.py`)

- `chat_json` passa `text={"format": {"type": "json_schema", "name": "chat_turn", "strict": true, "schema": …}}` na Responses API.
- Schema (strict: todo campo obrigatório, nulo explícito):
  - `reply`: string
  - `estimate`: objeto ou null, com `kcal`, `p`, `c`, `g` (number), `confidence` (`"high" | "medium" | "low"`), `question` (string ou null), `items` (lista de `{name: string, g: number, kcal: number}`) e `suggested_slot` (string ou null, com **`enum` dos ids de `profile.slots`** montado por request).
  - `digest`: null
- `digest_json` (compact) ganha o seu schema `{digest: string}`.
- `/v1/estimate` e `/v1/fit` não mudam (o client não usa).

### 2. Shaping tolerante (`shaping.py`)

- `suggested_slot`: aceita `str`, `int` e `{"id": …}`; normaliza para `str` e valida contra os ids do perfil. É uma defesa, caso o schema falhe.
- Texto sem JSON, mas não vazio: vira `reply` com `estimate: null`, em vez de "nao deu pra estimar". O log marca `fallback: "text_only"`. O fallback fixo fica só para saída vazia, exceção ou timeout.
- Chat com `confidence` ≠ high e sem `question`: a pergunta genérica vira `"Alguma porção foi diferente do que considerei?"` (nova constante `CHAT_FALLBACK_QUESTION`). `FALLBACK_QUESTION` do `/v1/estimate` não muda.

### 3. Instruções do chat (`_CHAT_INSTRUCTIONS`)

Acrescentar, em inglês, no bloco fixo (prefixo estável, bom para o cache automático de prompt):

1. Refeição consolidada: "If the message adds, removes or corrects food of a meal whose slot in DAY is `eaten`, return the estimate of the **whole meal** (the foods already recorded in that slot's text plus the change) and set `suggested_slot` to that slot. Say in `reply` that it replaces the recorded meal."
2. Total sem comida: "If the user gives only a calorie total without saying what was eaten, `estimate` is null and `reply` asks what was eaten. Never return an estimate with p, c and g all zero for real food."
3. Resposta a pergunta: "When the user answers your clarifying question, re-estimate the same meal with the answer; keep the same `suggested_slot`."
4. Outro dia: "The app records only today. If the user refers to another day (e.g. ontem), estimate if asked but say in `reply` that it will be recorded today."
5. Pergunta: "When confidence is not high, `question` is one specific question about the biggest uncertainty (portion, size, preparation). Never generic."

### 4. Limite do corpo (`config.py`)

- `MAX_BODY_BYTES = 24 * 1024 * 1024`: cobre `PHOTO_MAX_B64_CHARS` (22,4 M) + JSON. O cap da foto continua sendo o 413 `photo_too_large`.

### 5. Log (ADR-015)

- `fallback` passa a ser `false | "error" | "text_only"` (antes, booleano). `tools/pull-conversations.ps1` não muda.

### 6. Testes (`server/tests/test_chat.py`, `test_conversation_log.py`)

1. O schema enviado ao transport fake tem o `enum` dos ids do perfil em `suggested_slot`.
2. `suggested_slot` `1`, `"1"` e `{"id": 1}` → `"1"`; id fora do perfil → null.
3. Saída em texto livre → `reply` = o texto, `estimate` null, log `fallback: "text_only"`.
4. Saída vazia / exceção → "nao deu pra estimar" (como hoje).
5. `confidence: medium` + `question: null` → `CHAT_FALLBACK_QUESTION`.
6. Corpo de 21 MB com `image_b64` dentro do cap → não é 413 `payload_too_large`.
7. Suíte atual (53) verde, com ajuste só onde o formato do `fallback` do log mudou.

### 7. Deploy e verificação real

1. `pytest -q` verde.
2. `./tools/deploy-gcp.ps1`.
3. Replay dos casos do log com `X-Request-Id: s8-*`, usando os mesmos `input` do log baixado (`logs/conversations.jsonl`, fora do git):
   - `dada7406` (lasanha) → reply útil, sem fallback.
   - `bfbf194d` (almoço) → `suggested_slot: "3"`.
   - `d111a3dd` + suco com a Ceia `eaten` → estimate da ceia inteira (esfihas + suco), slot `"4"`.
   - "corrija, na minha ceia eu consumi aproximadamente 1220kcal" → `estimate: null`, pergunta o que comeu.
4. Resultado de cada replay registrado neste plano.

## Arquivos e áreas afetadas

- `server/llm.py`, `server/shaping.py`, `server/main.py`, `server/config.py`, `server/conversation_log.py` (só o formato de `fallback`).
- `server/tests/test_chat.py`, `server/tests/test_conversation_log.py`.
- `docs/server/specifications/v1-chat.md`, `docs/api-contract.md` (regras 4–6, 10 e limite de corpo), `docs/server/README.md`.

## Validação planejada

1. `pytest -q` (seção 6).
2. Replays da seção 7.3 no server de dev.
3. Manual (dono): o fluxo "Café da manhã: 2 pães e 2 ovos" mostra "Gravar café" no app atual (0.0.2), sem update do client.

## Fora de escopo

- UI do client (pergunta em bolha, confirmação de substituir): [A18](../../../android/plans/a18-chat-registro-foto.md) e [A19](../../../android/plans/a19-chat-visual.md).
- `/v1/estimate` e `/v1/fit`.
- Troca de modelo ou de `reasoning.effort`.

## Riscos e controles

- **O modelo não suportar `json_schema` strict:** o teste com transport fake não prova isso. O deploy + replay prova. Se a API recusar, vale `{"type": "json_object"}` + o shaping tolerante, registrado aqui.
- **Enum dinâmico com perfil sem slots:** o `enum` vira `[null]`, e o schema continua válido. Há teste para isso.
- **O modelo juntar os itens errado ao consolidar:** o client confirma antes de substituir (ADR-017). O replay do suco mede isso.

## Critérios de aceite

- Nenhum "nao deu pra estimar" quando o modelo devolveu texto.
- `suggested_slot` preenchido nas estimativas com slot do perfil.
- Total sem comida → sem estimate.
- Completar uma refeição gravada → estimate da refeição inteira.

## Sugestões de economia de token (registro, não implementar aqui)

- A consolidação usa o snapshot `day.slots[]` que já vai em todo turno. Não precisa reenviar histórico.
- A instrução fixa vem primeiro e é idêntica entre turnos: o cache automático de prompt da OpenAI (prefixo ≥ 1024 tokens) já pega. Não pôr nada variável antes dela.
- Saída estruturada elimina a volta extra do usuário depois de um fallback, que hoje custa um turno inteiro.
- Linhas `Respondeu "…": …` da memória repetem o que já está no histórico do dia. Candidato a sair num plano futuro.

## Resultados (29/09/2026)

### Implementado

- `llm.py`: `chat_json` manda `text.format` `json_schema` strict (`chat_turn`), com `enum` de `suggested_slot` = ids de `profile.slots` + `null` (perfil sem slots → `[null]`). `digest_json` manda o schema `digest`. `/v1/estimate` e `/v1/fit` sem mudança.
- `llm.py`: saída sem nenhum `{` levanta `TextOnlyOutput`; JSON truncado ou inválido continua erro.
- `shaping.py`: `_slot_id` aceita `"1"`, `1` e `{"id": 1}` e valida contra o perfil (bool → null). `text_only_chat` devolve o texto como `reply`, `estimate: null`. `CHAT_FALLBACK_QUESTION` no chat; `FALLBACK_QUESTION` do `/v1/estimate` intacto.
- `main.py`: `_llm_call` grava `fallback` `false | "error" | "text_only"`. A rota do chat passa os ids do perfil para o schema.
- `config.py`: `MAX_BODY_BYTES = 24 MB`, `CHAT_FALLBACK_QUESTION`.
- `conversation_log.py` não precisou mudar: o valor de `fallback` sai de `main.py`.

### Ajustes dentro do escopo (seção 3), descobertos no replay

O primeiro replay local com o modelo real, só com as 5 regras do plano, falhou em 3 de 4 casos: a lasanha voltou com estimate zerado, o suco foi somado ao **almoço** (slot 3), e o "1220 kcal" copiou o total com as macros do suco. Correções, todas no bloco fixo de instruções ou no snapshot que já ia no prompt:

- Snapshot do dia no prompt: `DAY slots` agora leva, por slot gravado, `kcal`, `P/C/G` e `text` (o client já mandava esses campos — ADR-017, "Custo de token"; antes o server só repassava `kcal`). Sem isso a IA não vê os itens da refeição gravada.
- Regra de referência: a refeição da fala é a que ela nomeia; senão a da fala anterior do usuário; senão a do horário.
- Total sem comida: vale mesmo com o slot já gravado; nunca copiar um total digitado para `kcal`.
- Macros coerentes com kcal (4/4/9). Sem conseguir estimar: `estimate` null, nunca zeros.
- Resposta a pergunta: reestimar incluindo todos os alimentos daquela refeição no HISTORY.

### Validação automatizada

- `server/.venv/Scripts/python -m pytest -q` → **64 passed, 25 subtests** (eram 54). Novos: schema com enum (e `[null]` sem slots), schema do compact, slot `1`/`"1"`/`{"id":1}`/fora do perfil/bool, texto livre → reply, saída vazia → fallback fixo, medium sem pergunta → `CHAT_FALLBACK_QUESTION`, snapshot com texto gravado + regras no prompt, corpo de 21 MB com foto dentro do cap → 200. Log: texto livre → `fallback: "text_only"` sem `error`; JSON truncado, saída vazia e erro de transporte → `"error"`.
- A API aceitou o `json_schema` strict no `gpt-6-luna` (replay local e no dev). O plano B (`json_object`) não foi necessário.

### Deploy e replay (seção 7)

`./tools/deploy-gcp.ps1` → `/health` 200. Replay no server de dev (`https://35-231-53-42.sslip.io`), mesmos `input` do log baixado, perfil com slots `1` Café, `2` Lanche, `3` Almoço, `4` Ceia. Log do server: `fallback: false` nos 4.

| Caso | Request id | Resultado | Aceite |
|---|---|---|---|
| `bfbf194d` almoço | `s8-almoco-dev` | 1040 kcal · 49P · 112C · 43G, `suggested_slot: "3"`, pergunta sobre o peso da lasanha e da costela | OK |
| `dada7406` lasanha ("tamanho pequeno… almoço de ontem") | `s8-lasanha-dev` | reply útil, reestima o almoço inteiro (890 kcal, 5 itens, lasanha pequena), slot `"3"`, avisa que grava hoje | OK |
| `d111a3dd` + suco com a Ceia `eaten` | `s8-suco-dev` | ceia inteira: 4 esfihas + suco = 1225 kcal · 41P · 172C · 47G, slot `"4"`, "substituir a estimativa registrada" | OK |
| "corrija, na minha ceia eu consumi aproximadamente 1220kcal" | `s8-1220-dev` | `estimate: null`, "O que você comeu na ceia?" | OK |

### Pendente (manual, dono)

- Validação 3: no app atual (0.0.2), "Café da manhã: 2 pães e 2 ovos" mostra "Gravar café", sem update do client. Aprovado → estado `Concluído` e o plano vai para `completed/`.

## Encerramento

Depois da implementação, registre resultados reais e aplique o ciclo de vida em `docs/sdd/README.md`, incluindo a entrega git (§ 6).

Só declaração explícita do dono cancelando este plano permite `Cancelado` e a pasta `plans/cancelled/`.
