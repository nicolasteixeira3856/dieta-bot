# Plano — S11 Chat v2 no server: intenção, texto da refeição, memória estruturada e histórico recente

- Estado: Pendente aprovação manual
- Data: 30/09/2026
- Contexto proprietário: `server`
- Código afetado: `server/llm.py`, `server/main.py`, `server/shaping.py`, `server/config.py`, `server/evals/cases/`, `server/tests/`
- Pré-requisitos: [S10](../completed/s10-avaliacao-chat.md) concluído (avaliador e linha de base). Aceita o [ADR-023](../../../produto/adrs/ADR-023-chat-v2-memoria-v2.md) (decisões 1, 2, 3, 4, 5 e 6, parte server).

## Gate de autorização

Este plano é exclusivamente documental. A implementação só começa após aprovação explícita que identifique este arquivo:

> Aprovo o plano `docs/server/plans/s11-chat-v2.md`. Implemente o plano aprovado.

Se a implementação revelar decisão não coberta, pare, atualize os artefatos e peça nova aprovação.

## Objetivo

O `/v1/chat` passa a classificar a intenção, devolver o texto consolidado da refeição, receber a memória em fatos e o histórico dos últimos 7 dias, propor mudanças de memória e dizer quais fatos usou. Tudo aditivo: o APK 0.0.3 continua funcionando. No fim, o `reasoning.effort` é decidido pelo avaliador.

## Fontes de verdade

- [ADR-023](../../../produto/adrs/ADR-023-chat-v2-memoria-v2.md), [ADR-017](../../../produto/adrs/ADR-017-registro-consolidado.md), [ADR-015](../../adrs/ADR-015-log-conversa-dev.md).
- [v1-chat](../../specifications/v1-chat.md), [api-contract.md](../../../api-contract.md).

## Escopo de implementação

### 1. Entrada nova (`ChatIn`, todos opcionais)

| Campo | Tipo | Limites |
|---|---|---|
| `facts` | lista de `{id, kind, category, key, text, slot, days_seen, last_seen}` ou ausente | ≤ 70 itens; `id` `[PD][0-9]{1,4}`; `kind` `permanent`/`dynamic`; `category` `preference`/`portion`/`routine`; `key` ≤ 40; `text` ≤ 160; `slot` id do perfil ou null; `days_seen` ≥ 0; `last_seen` data ISO ou null |
| `recent` | lista de `{date, slot_id, slot_name, text, kcal, p, c, g}` | ≤ 42 itens; `text` ≤ 240; `slot_id` null = "Outros" |
| `day.remaining_kcal` | inteiro ou null | calculado pelo app: teto efetivo − comido (pode ser negativo) |

- **Cliente v2** = request com `facts` presente (mesmo vazio). **Cliente legado** = sem `facts`: `memory` (texto) continua aceito e vai para o prompt como hoje.
- Campo fora dos limites → 422, como os limites atuais.

### 2. Prompt do turno (`_chat_text`)

Ordem: `PROFILE`, `MEMORY`, `DAY`, `RECENT`, `DIGESTS`, `HISTORY`, mensagem atual. As instruções fixas continuam em `instructions` (prefixo estável para o cache).

```text
MEMORY: permanent 3/30, dynamic 5/40
P1 preference leite: Leite semidesnatado
D2 routine slot=1 cafe: 2 ovos mexidos, 1 pão francês c/ manteiga, 200 ml leite, café (seen 5 days, last 2026-09-30)
DAY: date=…, local_time=…, remaining_kcal=640, eaten_kcal=…, …, slots=[…]
RECENT:
2026-09-29 1 Café: "2 ovos mexidos, 1 pão francês c/ manteiga, 200 ml leite, café" 440kcal 25P 38C 22G
```

Cliente legado: `MEMORY: <texto>` como hoje. `RECENT` e `remaining_kcal` entram sempre que vierem, em qualquer cliente (o [A27](../../../android/plans/a27-chat-v2-texto-intencao.md) manda os dois antes da memória v2).

### 3. Instruções (`_CHAT_INSTRUCTIONS`, reescritas em inglês)

Mantém todas as regras do S8 (refeição consolidada, total sem comida, 4/4/9, resposta a pergunta, outro dia, pergunta específica) e acrescenta:

1. **Intenção:** `intent` é `log` (comeu ou está comendo; passado, "comi", "tomei", "foi o mesmo de ontem", foto de prato comido), `plan` (vai comer, quer montar, pede quantidades, pergunta se cabe) ou `question` (sem comida para estimar). Na dúvida entre `log` e `plan`: passado = `log`; futuro, condicional ou pedido de quantidade = `plan`.
2. **Estimativa:** presente em `log` e `plan`, null em `question`.
3. **`meal_text`:** a refeição inteira em pt-BR, alimentos e quantidades já corrigidos pela conversa, sem comentário, ≤ 160 caracteres. Nunca a resposta do usuário sozinha, nunca uma frase ("Sempre uso…").
4. **Plano:** o `reply` traz as gramas de cada item, o preparo em até 3 linhas quando for receita e o total do prato (`kcal · P · C · G`). O prato cabe em `remaining_kcal` quando possível; se não couber, diz quanto passa. Não faz a conta do dia no texto (o app mostra). Plano não pergunta: assume e diz o que assumiu. `confidence` pode ser `medium`, com `question` null.
5. **Histórico:** "o mesmo de ontem", "igual ao almoço de segunda" → usar `RECENT` daquele slot e dia. Sem registro que case, perguntar o que foi.
6. **Memória — uso:** se um fato de `MEMORY` responde uma incerteza (tipo de leite, marca, porção), usar o fato, **não perguntar** sobre isso, e listar o `id` em `memory_used`. Só ids que existem em `MEMORY`.
7. **Memória — mudanças (`memory_updates`):**
   - Frase explícita de hábito ou preferência ("sempre uso", "lembra que", "não uso mais", "agora uso") → `add` com `kind: permanent`; se já existe fato com a mesma `key`, `replace` com o `id` dele.
   - "Esquece X" → `remove` com o `id` do fato.
   - Marca, tipo ou porção específica que aparece num registro (`log`) → `reinforce` do fato de mesma `key`, ou `add` `dynamic` se não existe.
   - Refeição `log` que corresponde a uma rotina existente do slot → `reinforce` com o `id` da rotina. Refeição que parece hábito novo ("todo dia", ou igual a um registro de `RECENT` no mesmo slot) → `add` `dynamic` `routine` com o `slot`.
   - Nunca: refeição avulsa, números do dia, condição de saúde, nada que não seja hábito alimentar.
   - Permanente com 30/30 e frase explícita nova: não emitir `add`; o `reply` pergunta "Minha memória fixa está cheia. Esqueço {fato permanente com menos dias vistos}?". Com a resposta, `remove` + `add`.
   - No máximo 5 mudanças por turno.
8. `reply` nunca diz que registrou: quem registra é o usuário, no app.

### 4. Saída (`chat_turn`, `json_schema` strict)

```json
{
  "reply": "string",
  "intent": "log | plan | question",
  "estimate": {
    "kcal": 0, "p": 0, "c": 0, "g": 0,
    "confidence": "high | medium | low",
    "question": "string | null",
    "items": [{"name": "string", "g": 0, "kcal": 0}],
    "suggested_slot": "enum dos ids do perfil | null",
    "meal_text": "string"
  },
  "memory_updates": [{
    "op": "add | reinforce | replace | remove",
    "id": "enum dos ids de facts | null",
    "kind": "permanent | dynamic",
    "category": "preference | portion | routine",
    "key": "string",
    "text": "string",
    "slot": "enum dos ids do perfil | null"
  }],
  "memory_used": ["enum dos ids de facts"],
  "digest": null
}
```

- `id` e `memory_used` usam `enum` dos ids recebidos em `facts`, montado por request (como o `suggested_slot` do S8). Sem fatos, `memory_used` fica `{"type": "array", "items": {"type": "string"}}` e o shaping filtra.

### 5. Shaping (`shape_chat`)

- `intent` fora do conjunto → deduzido: estimate presente = `log`, senão `question`.
- `intent: question` com estimate → estimate descartado.
- `meal_text` vazio → montado dos `items` (`"{name} {g} g"`, separados por vírgula, corte em 160). Acima de 160, corte na última vírgula antes do limite.
- `memory_updates`: descarta op inválida, `id` desconhecido (`reinforce`/`replace`/`remove`), `add` com `id`, `routine` sem `slot` válido, `key` ou `text` vazio; corta `text` em 160 e `key` em 40; no máximo 5.
- `memory_used`: só ids de `facts`, sem repetição, no máximo 10.
- **Cliente legado:** `memory_updates` e `memory_used` vazios; `intent: plan` → `estimate: null` (sem card no 0.0.3; gramas e total ficam no texto).

### 6. Avaliação e `reasoning.effort`

1. Casos `v2` do S10 com as expectativas completas. Casos novos para o que surgir na implementação.
2. `python -m evals.run --effort none low --repeat 3`.
3. Regra do ADR-023: **`low`** se ganhar pelo menos 10 pontos percentuais de aprovação e tiver p95 ≤ 20 s; senão **`none`**. O vencedor vira o padrão de `LlmClient` (`config.REASONING_EFFORT`).
4. Se `low` vencer: linha LLM do `AGENTS.md` (`reasoning.effort=low`), [v1-chat](../../specifications/v1-chat.md) regra 2 e `docs/server/README.md`.
5. Meta: todos os casos dos grupos "Texto da refeição" e "Intenção" aprovados no effort escolhido. Caso que continuar falhando fica listado em Resultados, com o `raw_output`, e é reportado ao dono antes do deploy.

### 7. Testes (`server/tests/test_chat.py`)

1. Schema com `enum` de `id`/`memory_used` = ids de `facts`; sem fatos, array de string.
2. Prompt: `MEMORY` com contagem e linhas de fato, `RECENT`, `remaining_kcal`; cliente legado igual a hoje.
3. Shaping: intent inválido/deduzido; question com estimate; `meal_text` de fallback; cada descarte de `memory_updates`; filtro de `memory_used`.
4. Cliente legado: plan → `estimate: null`, sem campos de memória na saída.
5. Limites novos → 422.
6. Suíte atual verde.

### 8. Deploy e verificação real

1. `pytest -q` verde.
2. `./tools/deploy-gcp.ps1`, `/health` 200.
3. Contra o server de dev, com `X-Request-Id: s11-*`: um request **legado** (corpo do 0.0.3, sem `facts`) e um **v2** para café com resposta sobre o leite e para a pizza de pão sírio. Resultado registrado aqui.

### 9. Contrato e specs

- `docs/api-contract.md` (§ POST /v1/chat): campos de entrada e saída novos, limites, cliente legado.
- [v1-chat](../../specifications/v1-chat.md): regras 3–6 e a seção "Mudanças aprovadas (ADR-023)" vira regra vigente.

## Arquivos e áreas afetadas

- `server/llm.py` (instruções, schema, effort), `server/main.py` (`ChatIn`, `_chat_text`), `server/shaping.py`, `server/config.py` (limites, `REASONING_EFFORT`).
- `server/evals/cases/*.json`, `server/tests/test_chat.py`.
- `docs/api-contract.md`, `docs/server/specifications/v1-chat.md`, `docs/server/README.md`, `AGENTS.md` (só se `low` vencer).

## Validação planejada

1. `pytest -q`.
2. Avaliador `none` × `low` (seção 6), números registrados.
3. Deploy + requests reais (seção 8).
4. Manual (dono), no APK 0.0.3 sem update: perguntar "vou fazer pizza de pão sírio, quantas gramas de cada item?" → resposta com gramas e total, **sem card**.

## Fora de escopo

- Client: [A27](../../../android/plans/a27-chat-v2-texto-intencao.md), [A28](../../../android/plans/a28-memoria-v2.md), [A29](../../../android/plans/a29-chat-v2-interface.md).
- Aplicar a memória, promoção, expiração e limites: são do client (o server é stateless).
- Troca de modelo. `/v1/estimate` e `/v1/fit`.

## Riscos e controles

- **Schema grande demais para o strict:** o avaliador e o deploy provam. Plano B: `memory_updates` com `id` string livre e validação só no shaping, registrado aqui.
- **Prompt maior (fatos + recent):** o relatório do S10 mostra tokens de entrada antes e depois. Custo aceito no ADR-023.
- **A IA propor memória demais:** limite de 5 por turno, casos negativos no avaliador ("refeição avulsa não vira memória").
- **Quebrar o 0.0.3:** request legado na verificação e teste do cliente legado.

## Critérios de aceite

- Café + resposta sobre o leite → `meal_text` com a refeição, não com a resposta.
- Pizza "vou fazer" → `intent: plan`, gramas e total no `reply`.
- "Sempre uso leite semidesnatado" → `add` permanente `leite`. Com esse fato na memória, o café não pergunta o tipo de leite e `memory_used` tem o id.
- APK 0.0.3 continua funcionando e para de mostrar card em plano.
- Effort escolhido pelo avaliador e registrado.

## Resultados (30/09/2026)

### Entregue

- `ChatIn`: `facts` (≤ 70, presente = cliente v2), `recent` (≤ 42), `day.remaining_kcal`, com os limites da seção 1 (422). `slot` de fato fora do perfil → 422.
- `_chat_text`: `MEMORY` com contagem e linhas de fato (v2) ou texto (legado), `remaining_kcal` no `DAY`, `RECENT` entre `DAY` e `DIGESTS`. Cliente legado sem `recent`/`remaining_kcal` gera o prompt de antes, byte a byte.
- `_CHAT_INSTRUCTIONS` reescritas em inglês com todas as regras do S8 e as 8 da seção 3.
- `chat_turn` strict com `intent`, `meal_text`, `memory_updates` e `memory_used`; enum de ids montado por request.
- `shape_chat` com as regras da seção 5 e do cliente legado. `fail_chat` e `text_only_chat` devolvem os campos novos vazios (`intent: question`).
- `config.REASONING_EFFORT = "none"` como padrão do `LlmClient`.
- Casos: os `v2` passam a mandar `facts: []` (cliente v2); novo `legado-plano-sem-card` (corpo do 0.0.3, plano → sem card). 22 casos.

### Decisões de implementação (cobertas pelo plano, registradas aqui)

- **Dia da semana no `RECENT`** (`2026-09-28 segunda 3 Almoço: …`): com `none`, "igual ao almoço de segunda" falhava 0/3 por não converter data em dia da semana. Com o dia na linha, 3/3.
- **Duas regras a mais no prompt**, das falhas do avaliador: kcal/P/C/G são o total da refeição inteira, soma dos itens (a ceia com suco voltava 900 ou 103 kcal com itens somando 1035); uma pergunta por refeição: respondida, `confidence` high e sem pergunta nova (regra "One question" do `AGENTS.md`).
- **Plano não pergunta:** o shaping zera `question` em `plan`, sem o `CHAT_FALLBACK_QUESTION`.
- **Avaliador:** expectativa nova `question_not` (termo que não pode estar na pergunta). `memoria-evita-pergunta-leite` usa `question_not: ["leite"]` no lugar de `question: absent`: o caso mede se o fato do leite evita a pergunta do leite; uma pergunta sobre a manteiga é legítima.
- Schema strict no pior caso real: 70 fatos (enum de 70 ids) + 42 `recent` aceitos pela API, 4,3 s, `memory_used: ["P1"]`. O plano B não foi necessário.

### Avaliação `none` × `low` (seção 6)

`python -m evals.run --effort none low --repeat 3`, 22 casos, prompt final (30/09/2026 12:27):

| effort | aprovados | p50 | p95 | tokens/chamada (entrada · cache · saída · raciocínio) | custo da rodada |
|---|---|---|---|---|---|
| `none` | **22/22 (100%)** | 2,5 s | 3,5 s | 2236 · 1888 · 168 · 0 | US$ 0,0091 |
| `low` | 22/22 (100%) | 3,0 s | 5,2 s | 2236 · 1919 · 235 · 66 | US$ 0,0111 |

- Linha de base do S10 (prompt antigo, `none`): 11/21 (52,4%).
- **Decisão: `none`.** `low` não ganha 10 p.p. (0 p.p.). `REASONING_EFFORT` fica `none`; `AGENTS.md` sem mudança.
- Rodadas intermediárias (mesmo conjunto): 1ª `none` 20/22, `low` 21/22 (falhas: ceia com kcal ≠ soma, pergunta repetida no café, pergunta da manteiga); 2ª `none` 20/22, `low` 21/22 (falhas: pergunta repetida no café, "segunda" sem data). As correções acima levaram as duas a 22/22.
- Meta da seção 6.5: todos os casos de "Texto da refeição" e "Intenção" aprovados com `none`. Nenhum caso falhando para reportar.
- Tokens de entrada: 1086 → 2236 por chamada (fatos, `recent` e instruções maiores), ~85% em cache. Custo aceito no ADR-023.

### Deploy e verificação real (seção 8)

- `pytest -q`: 95 passed, 63 subtests.
- `./tools/deploy-gcp.ps1`: `/health` 200 `{"ok": true, "model": "gpt-6-luna"}`.
- Requests reais no server de dev (`X-App-Env: s11-check`):
  - `s11-legacy-pizza-plano` (corpo do 0.0.3, sem `facts` nem `remaining_kcal`): 200, `intent: plan`, **`estimate: null`**, `memory_updates: []`, `memory_used: []`; `reply` com gramas, preparo e "Total aproximado: 490 kcal · P 48 g · C 39 g · G 14 g".
  - `s11-v2-cafe-leite` (café igual ao de ontem + "Uso sempre leite semidesnatado"): 200, `intent: log`, `meal_text` "pão francês com manteiga, 2 ovos mexidos, 200 ml de leite semidesnatado, 100 ml de café sem açúcar", 430 kcal, slot 1, `question` null, `memory_updates` `add permanent preference leite`.
  - `s11-v2-pizza-plano`: 200, `intent: plan`, estimate 495 kcal slot 5 sem `question`, `reply` com gramas, preparo e "Total estimado: 495 kcal · P 47 g · C 39 g · G 15 g", cabendo nos 735 kcal restantes.

### Contrato e specs

- `docs/api-contract.md` § POST /v1/chat: campos de entrada e saída, limites, cliente legado. Corrigida a linha antiga "text do /v1/chat ≤ 1000" (vale 2000 desde o S9).
- [v1-chat](../../specifications/v1-chat.md): regras 2–5b vigentes; a seção "Mudanças decididas" virou estado vigente.

### Pendente (manual, dono)

- Validação 4: no APK 0.0.3, sem update, perguntar "vou fazer pizza de pão sírio, quantas gramas de cada item?" → resposta com gramas e total, **sem card**. Depois disso: `Concluído` e `completed/`.
- O [ADR-023](../../../produto/adrs/ADR-023-chat-v2-memoria-v2.md) continua `Proposto` (contexto `produto`); este plano não muda o estado dele.

## Encerramento

Depois da implementação, registre resultados reais e aplique o ciclo de vida em `docs/sdd/README.md`, incluindo a entrega git (§ 6).

Só declaração explícita do dono cancelando este plano permite `Cancelado` e a pasta `plans/cancelled/`.
