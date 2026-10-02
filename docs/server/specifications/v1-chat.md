# Especificacao — POST /v1/chat

## Controles de conteúdo (CP2)

Vigente desde o [CP2](../../content-policy/plans/completed/cp2-server-content-controls.md) (30/09/2026, [ADR-024](../../content-policy/adrs/ADR-024-content-safety-boundaries.md)), regras na [política de conteúdo](../../content-policy/specifications/content-policy.md). Header de instalação opcional `X-Client-Instance-Id` e `safety_identifier` desde o [CP3](../../content-policy/plans/completed/cp3-server-safety-identifier.md) (01/10/2026, [identity/audit](../../content-policy/specifications/identity-and-audit.md#closed-test-profile), [contrato](../../api-contract.md)); auditoria segue no CP9.

## Estado

Vigente: GET /health, POST /v1/estimate, POST /v1/fit, POST /v1/chat ([S2](../plans/completed/s2-v1-chat.md)) com `compact=true` ([S3](../plans/completed/s3-compact.md)). Timeout 60s. Cap 16 MB JPEG (22_400_000 chars de image_b64).

Desde o [S8](../plans/completed/s8-chat-json-slot-consolidado.md) (29/09/2026): saída estruturada (`json_schema` strict, `suggested_slot` com enum dos ids do perfil), texto sem JSON vira `reply`, refeição consolidada ([ADR-017](../../produto/adrs/ADR-017-registro-consolidado.md)), total sem comida sem estimate, `MAX_BODY_BYTES` 24 MB.

Desde o [S9](../plans/completed/s9-limite-texto-2000.md) (29/09/2026): `text` e `messages[].text` até 2000 caracteres ([ADR-022](../../produto/adrs/ADR-022-limite-texto-chat.md)).

Desde o [S11](../plans/completed/s11-chat-v2.md) (30/09/2026, [ADR-023](../../produto/adrs/ADR-023-chat-v2-memoria-v2.md)): intenção (`log` | `plan` | `question`), `estimate.meal_text`, memória em fatos (`facts`), `recent` (7 dias), `day.remaining_kcal`, `memory_updates` e `memory_used`. Tudo aditivo: cliente legado (sem `facts`) continua funcionando. `reasoning.effort` fica `none`, decidido pelo avaliador `server/evals/` ([S10](../plans/completed/s10-avaliacao-chat.md)).

Desde o [S12](../plans/completed/s12-slot-nomeado.md) (30/09/2026): o `suggested_slot` segue a refeição nomeada pelo usuário (na mensagem ou na fala que ela responde), mesmo com comida igual à de um slot gravado; a refeição consolidada só vale quando a mensagem se refere à refeição gravada.

Desde o [S13](../plans/completed/s13-perguntas-antes-da-estimativa.md) (30/09/2026, [ADR-026](../../produto/adrs/ADR-026-perguntas-antes-da-estimativa.md)): perguntas antes da estimativa. Com `clarify_rounds` no request (cliente v3), um turno `log` com dúvida devolve só a pergunta (`estimate: null`, `question` no topo); o server libera a estimativa no código após 3 rodadas, em pergunta repetida ou com `force_estimate`. Sem `clarify_rounds`, a resposta é a mesma de antes do S13.

Desde o [S14](../plans/completed/s14-registro-autonomo.md) (02/10/2026, [ADR-028](../../produto/adrs/ADR-028-registro-autonomo.md), proposto): marca de registro. Com `clarify_rounds` e `auto_record: true` (cliente v4), o OUT ganha `record` (`auto` | `ask` | `none`) e `skip_slot`, e a intenção `skip` ("pulei o café"); o modelo devolve `record_intent` e `meal_day` (internos); outro dia nunca é registrado. O server continua sem gravar nada. Sem cliente v4, a resposta é a mesma de antes do S14.

Desde o [S15](../plans/completed/s15-registro-casos-dificeis.md) (02/10/2026): refeição pendente ("ainda não almocei") nunca é `skip`; pulo firme avisado antes ("hoje não vou jantar") é `skip`, pulo com dúvida ("acho que não vou jantar") não; foto de comida com pergunta sobre ela é `log` com `record_intent: unsure` (`record: ask`). Só as instructions mudaram: schema, gates e formato da resposta iguais aos do S14.

Desde o [CP2](../../content-policy/plans/completed/cp2-server-content-controls.md) (30/09/2026): escopo do produto nas instructions, campo `scope` obrigatório no schema, resposta fixa fora do escopo, moderação OpenAI da entrada e da saída com falha fechada (regras 12–16).

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
3a. Intenção: `log` (comeu ou está comendo, ou responde a pergunta sobre essa refeição; comida citada sozinha, sem verbo nem pergunta, também é `log`), `plan` (vai comer, quer montar, pede quantidades, pergunta se cabe), `question` (sem comida a estimar: saudação, dúvida sobre o app, pergunta de nutrição sobre comida) ou `skip` (uma refeição de hoje não aconteceu: "pulei o café", "hoje não almocei", sem "ainda"; ou o usuário diz com firmeza que não vai acontecer hoje: "hoje não vou jantar", "vou pular o almoço hoje"; `skip_slot` = id do slot do perfil cujo nome é essa refeição, ou null quando nenhum slot tem esse nome: "merenda", "sobremesa"). Refeição pendente não é `skip` ("ainda não almocei", "não jantei ainda", "ainda vou almoçar"): `question`, ou `plan` se pede o que comer, `skip_slot: null`. Pulo com dúvida não é `skip` ("acho que não vou jantar", "talvez eu pule a janta", "não sei se vou almoçar"): `question`, `skip_slot: null` (S15). Pedido fora do escopo nunca é respondido (regra 12). Dúvida: passado = `log`; futuro, condicional ou pedido de quantidade = `plan`. Estimate em `log` e `plan`; null em `question` e `skip`. `skip` só chega ao cliente v4; antes dele vira `question` com o mesmo `reply` (regra 5d).
3b. `meal_text`: a refeição inteira em pt-BR com as correções da conversa, sem comentário, ≤ 160 caracteres. Nunca a resposta do usuário sozinha.
3c. Plano: `reply` com gramas por item, preparo em até 3 linhas se receita, total `kcal · P · C · G`; cabe em `remaining_kcal` quando possível, senão diz quanto passa. Não faz a conta do dia. Não pergunta: assume e diz o que assumiu (`question` null).
3d. Histórico: "o mesmo de ontem", "igual ao almoço de segunda" → usa a linha de `RECENT` daquele dia e slot. Sem registro que case → estimate null e pergunta o que foi.
3e. Memória: fato que responde a incerteza é usado, não vira pergunta, e o id vai em `memory_used`. `memory_updates` (≤ 5 por turno): frase explícita de hábito → `add` permanente (ou `replace` do fato de mesma `key`); "esquece X" → `remove`; marca/tipo/porção num `log` → `reinforce` ou `add` dinâmico; refeição que casa rotina → `reinforce`; hábito novo → `add` dinâmico `routine` com slot. Nunca refeição avulsa, números do dia ou saúde. Permanente 30/30 + frase nova → sem `add`; o `reply` pergunta "Minha memória fixa está cheia. Esqueço {fato menos visto}?".
3f. O `reply` nunca diz que registrou ou pulou uma refeição: quem registra é o app, que mostra o recibo ([ADR-028](../../produto/adrs/ADR-028-registro-autonomo.md)). Vale para todo cliente.
3g. Marca de registro (todo cliente, usada só pelo v4): `record_intent` `clear` = o usuário diz que comeu (ou pulou) e espera que conte: passado ou presente de comer ("Na janta comi…", "almocei um PF"), pedido explícito ("registra", "anota", "marca": "Registra aí, comi tal e tal"), nome de refeição seguido de comida ("Lanche da tarde: 200g de…"), foto de prato sem texto ou com texto dizendo que comeu ("almocei isso"), resposta a pergunta sobre a refeição, "Pode estimar assim."; `unsure` = comida sem sinal de ter sido comida ("pudim de leite com calda"), dúvida misturada com comida, hipótese, foto de comida com pergunta sobre ela ("isso tem muita caloria?", "quanto tem isso?": `log` com estimate, `record: ask`; S15); `plan` e `question` levam `unsure`. `meal_day` `other` = a mensagem fala de refeição de outro dia ("ontem", "anteontem", dia da semana passado, "ontem à noite"); antes das 05:00 locais, "a janta" sem outra data é `today` só se o perfil tem esse slot hoje, num horário antes das 05:00 (ex.: Jantar 03:00); senão é a janta de ontem, `other`. Resto = `today`; comida comida agora ("agora") é sempre `today`. `skip`: reply de uma linha curta e neutra, sem conselho.
4. Instructions: estimar se o user registrou comida; responder duvida; o modelo nunca grava (só marca, regra 3g); sugerir slot pelo horario local vs slots do perfil; se confidence ≠ high, `question` lista todas as dúvidas da refeição numa mensagem só (no máximo 3 perguntas curtas, cada uma específica: porção, tamanho, preparo, ingrediente); depois de uma resposta, pergunta de novo só sobre comida que ficou sem porção nenhuma e muda a estimativa de forma material (quantidade de molho, óleo, creme, queijo ou tempero é assumida); senão confidence high e `question` null; nunca repete pergunta já feita no `HISTORY`; com confidence ≠ high, o `reply` diz numa linha curta o que assumiu. kcal/P/C/G são o total da refeição inteira (soma dos itens). A refeição da fala, em ordem de prioridade: (1) a nomeada na mensagem atual (nome do slot do perfil ou palavra comum: café, almoço, jantar, lanche, ceia, "jantei", "almocei"); (2) senão, a nomeada na fala do user que esta mensagem responde ou continua ("cafe igual ao de ontem" → pergunta → resposta = café); (3) senão, a refeição gravada que a mensagem corrige; (4) senão, a do horário local. Refeição consolidada ([ADR-017](../../produto/adrs/ADR-017-registro-consolidado.md)) só quando a mensagem se refere à refeição gravada: nomeia aquele slot, ou é acréscimo/correção explícito ("também", "faltou", "esqueci", "na verdade", "tirando", "era X e não Y") sem nomear outra refeição. Aí, slot `eaten` → estimate da **refeição inteira** (itens gravados + mudança, a comida acrescentada sempre entra, com porção assumida se houver pergunta) com aquele slot, e o reply diz que substitui. Comida igual ou parecida à gravada não basta: com outra refeição nomeada, é refeição nova daquele slot e o reply não fala em substituir. Resposta a pergunta → reestima a mesma refeição, mesmo slot. Outro dia ("ontem") → `meal_day: other`, estima se pedido, e o reply diz numa linha curta que o Chat registra só as refeições de hoje.
4a. Total de kcal sem comida ("comi 1220 kcal"), mesmo com o slot gravado → `estimate: null` e o reply pergunta o que foi comido. Nunca estimate com p, c e g todos zero; P/C/G coerentes com kcal; sem conseguir estimar → null, nunca zeros.
5. OUT sempre JSON. O server pede ao modelo `text.format` `json_schema` strict (`chat_turn`: todo campo obrigatório, nulo explícito; compact usa o schema `digest`):
   - reply: string (prosa pt-BR)
   - intent: `log` | `plan` | `question` | `skip`
   - estimate: {kcal,p,c,g,confidence,question,items,suggested_slot,meal_text} ou null (`question`, `skip`)
   - record_intent: `clear` | `unsure`; meal_day: `today` | `other`; skip_slot: enum dos ids do perfil + null (S14, regra 3g; internos: só o cliente v4 recebe `skip_slot`, pela regra 5d)
   - memory_updates: lista de {op,id,kind,category,key,text,slot}; `id` e `memory_used` com enum dos ids de `facts` (sem fatos: `id` só null, `memory_used` string livre)
   - memory_used: lista de ids de fatos
   - digest: string ou null
   - scope: `in_scope` | `out_of_scope` | `policy_blocked` | `safety_support` (último campo do schema; só interno, nunca vai ao client)
   - model: gpt-6-luna
5a. Shaping (S11): `intent` inválido → deduzido (estimate = `log`, senão `question`); `question` com estimate → estimate descartado; `plan` → `question` null; `meal_text` vazio → itens (`{name} {g} g`, vírgulas), acima de 160 corta na última vírgula. `memory_updates`: descarta op/kind/category inválidos, `id` desconhecido em `reinforce`/`replace`/`remove`, `add` com `id`, `routine` sem slot válido, `key` ou `text` vazio; corta `text` 160 e `key` 40; no máximo 5. `memory_used`: só ids de `facts`, sem repetição, no máximo 10.
5c. Cliente v3 (`clarify_rounds` presente, [ADR-026](../../produto/adrs/ADR-026-perguntas-antes-da-estimativa.md)): o OUT ganha `question` no topo (string ou null) e `estimate.question` é sempre null. Para `intent: log` com estimate, o gate de liberação (`shaping.clarify_gate`), em ordem: (1) `force_estimate` → libera (`released_force`); (2) confidence high ou pergunta vazia → libera (`released_confident`); (3) `clarify_rounds ≥ 3` → libera (`released_cap`); (4) a pergunta repete uma pergunta de um turno `assistant` de `messages` → libera (`released_repeat`); (5) senão pergunta (`asked`). Liberar = estimate com `question: null` e a confidence do modelo; `question` do topo null. Perguntar = `estimate: null`, `question` = a pergunta do modelo, `reply` = `Entendi: {meal_text}.` + quebra de linha + a pergunta (sem `meal_text`, só a pergunta), texto que o app guarda e devolve no histórico; `memory_updates` e `memory_used` passam iguais. Repetição: só as frases terminadas em `?` do turno contam; texto em minúsculas, sem acento, tokens `[a-z0-9]`, sem stop words pt-BR de uma lista fixa curta; repete com Jaccard ≥ 0,6 (`CLARIFY_REPEAT_JACCARD`) ou um conjunto contido no outro. `plan`, `question` e estimate null passam sem gate. Falha ou texto sem JSON: `question: null`.
5d. Cliente v4 (`clarify_rounds` presente e `auto_record: true`, [ADR-028](../../produto/adrs/ADR-028-registro-autonomo.md)): depois do gate da regra 5c e da política de conteúdo, o gate de registro (`shaping.record_gate`) põe no topo `record` (`auto` | `ask` | `none`) e `skip_slot` (string ou null). Em ordem, a primeira linha vale (valor no log entre parênteses): (1) `scope` ≠ `in_scope` ou flag de moderação (resposta fixa, inclusive texto sem JSON) → `none` (`none_policy`); (2) `intent` `question` ou `plan`, turno só de pergunta, `log` sem estimate ou falha (`fallback: "error"`) → `none` (`none_intent`); (3) `meal_day: other` → `none` (`none_other_day`); (4) `skip` com `skip_slot` id do perfil → `auto`, `skip_slot` = esse id (`auto_skip`); (5) `skip` sem slot válido → `none` e `intent: question` (`none_skip_slot`); (6) `log` com estimate liberado e `suggested_slot` null → `ask` (`ask_no_slot`); (7) `log` com estimate liberado e `record_intent: clear` → `auto` (`auto_log`); (8) senão `ask` (`ask_unsure`). `force_estimate: true` e foto sem texto contam como `clear`. `meal_day` ausente ou inválido = `today`; `record_intent` ausente ou inválido = `unsure`. `skip_slot` só não é null na linha 4. O cliente aplica a marca e rebaixa `auto` para `ask` se uma guarda dele falhar. Sem cliente v4 (`auto_record` ausente ou `false`, ou sem `clarify_rounds`): `skip` vira `question` com o mesmo `reply`, `record` e `skip_slot` não existem no OUT, e a resposta é byte a byte a de antes do S14.
5b. Cliente legado (sem `facts`): `memory_updates` e `memory_used` vazios; `intent: plan` → `estimate: null` (sem card no APK ≤ 0.0.3; gramas e total no `reply`).
6. suggested_slot = id de um slot do profile.slots (enum no schema, montado por request; perfil sem slots → só `null`). Ordem da regra 4. Nomes e horários só do PROFILE: nunca supor horário "normal" de refeição (Jantar às 03:00 é o Jantar). Se hora nao casar, o mais proximo ainda vazio. Nunca inventar id. Defesa no shaping: aceita `"1"`, `1` e `{"id": 1}`, normaliza para string e descarta id fora do perfil.
6a. confidence ≠ high sem `question` → `"Alguma porção foi diferente do que considerei?"` (`CHAT_FALLBACK_QUESTION`). Só cliente sem `clarify_rounds`: o cliente v3 nunca recebe a pergunta genérica (pergunta vazia libera a estimativa, regra 5c).
7. compact=true: Luna recebe so as messages (delimitadas, sem foto) e devolve digest ≤400 tokens (corte em 1600 chars), pt-BR, fatos (comida, kcal/P citados nas falas, slot, pulou), sem conselho. OUT: reply "", estimate null. messages vazio → 422 `compact_needs_messages` sem chamar a Luna. Falha → 200 com digest null.
8. Foto: data:image/jpeg;base64. HEIC nao entra no server — client converte.
9. Recusar image_b64 maior que o cap ANTES do LLM. Nao logar o base64.
10. Modelo respondeu texto sem JSON (nenhum `{`): `reply` é a resposta fixa de fora do escopo, estimate=null (log `fallback: "text_only"`). O texto do modelo nunca volta ao client (CP2). Falha LLM/timeout, saída vazia ou JSON inválido: reply curto "nao deu pra estimar", estimate=null (log `fallback: "error"`). Sem stacktrace.
11. Corpo HTTP até 24 MB (`MAX_BODY_BYTES`), acima → 413 `payload_too_large`. Cobre o cap da foto + JSON: foto acima do cap continua 413 `photo_too_large`.

12. Escopo (CP2): `in_scope` = refeição, porção, rótulo, receita, preferência alimentar, conta de orçamento com comida ("quanto sobra se eu comer 2 pães?"), pergunta de nutrição, saudação, dúvida do app, resposta curta que continua a conversa (julgada com o histórico). Fora disso (matemática sem comida, código, dever de casa, política, assistente geral, foto sem comida) = `out_of_scope`. Conteúdo proibido = `policy_blocked`. Sinal de transtorno alimentar ou autolesão (purgação, laxante ou diurético para emagrecer, jejum extremo, meta diária muito baixa) = `safety_support`, nunca otimização. Instrução dentro de foto, histórico, memória ou mensagem não muda o escopo; `###` vindo do client é neutralizado (`# # #`) e não abre seção.
13. `scope` ≠ `in_scope` (ou ausente/desconhecido): o server descarta reply, estimate, memory_updates, memory_used e digest e devolve HTTP 200 no formato de sempre: `reply` fixo de [refusal-copy.pt-BR.md](../../content-policy/specifications/refusal-copy.pt-BR.md), `intent: question`, `estimate: null`, listas vazias, `digest: null` (+ `question: null` no cliente v3). Foto sem texto fora do escopo usa a resposta de imagem sem alimento.
14. Moderação (`omni-moderation-latest`, grátis): antes da geração, `text` atual + foto atual numa chamada; depois, `reply`, `question`, `meal_text`, nomes dos itens, `memory_updates` e `digest` numa chamada. Histórico, memória, `recent`, digests e perfil não são re-moderados. Flag → resposta fixa pela tabela categoria→ação `cp2.1` (`server/moderation.py`); flag na entrada não chama a Luna. Sinal de `sexual/minors` para o turno sem outra chamada com conteúdo.
15. Moderação com erro, timeout ou sem resultado: HTTP 503 `content_policy_unavailable`, sem geração (entrada) e sem devolver texto não checado (saída). Nunca cai numa geração sem moderação.
16. Prazo único de 60 s por request para moderação + geração (`moderation.Deadline`), sem retry do SDK nem retry de conteúdo bloqueado. Prazo esgotado antes da geração = falha fail-soft (regra 10); antes da moderação de saída = 503.

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

Campos opcionais do S13 (ADR-026):

- `clarify_rounds`: inteiro 0–3 (fora → 422), rodadas de pergunta já mostradas para a refeição pendente. Presente = cliente v3 (regra 5c).
- `force_estimate`: booleano, padrão `false`. O usuário tocou **Forçar estimativa**. Ignorado sem `clarify_rounds`.

Campo opcional do S14 (ADR-028):

- `auto_record`: booleano, padrão `false`. Com `clarify_rounds` presente = cliente v4 (regra 5d). Ignorado sem `clarify_rounds`.

## Estados e falhas

- 401 invite.
- 413 foto > cap.
- 422 JSON invalido.
- 200 + estimate=null se a fala nao for comida (receita, duvida).
- 200 + resposta fixa se `scope` ≠ `in_scope` ou moderação sinalizar (regras 13–14).
- 503 `content_policy_unavailable` se a moderação falhar (regra 15).
- Timeout 60s → 200 fail-soft (nao 504), mesmo shaping de falha.

## Fronteiras e ownership

- Dono: `server/`.
- Client monta profile/day/messages. Server nao consulta Room.

## Localizacao e observabilidade

- Nao logar image_b64 nem OPENAI_API_KEY.
- Log de conversa dev, campo `policy` (CP2): `null` ou `{stage, code, severe, categories, table}` (`stage` `input` | `scope` | `output` | `text_only`; `code` interno; só categorias verdadeiras). Turno `policy_blocked` ou com sinal severo: só metadados (prompt, input, saída e resposta `null`), parcialmente substituindo o [ADR-015](../adrs/ADR-015-log-conversa-dev.md). `error` leva só tipo e status HTTP, nunca o texto do provedor.
- Log de conversa dev ([ADR-015](../adrs/ADR-015-log-conversa-dev.md)), rota `chat`: `clarify` (`none` | `asked` | `released_force` | `released_confident` | `released_cap` | `released_repeat`) e `clarify_rounds` (número ou null). S14: `record` (`none_policy` | `none_intent` | `none_other_day` | `auto_skip` | `none_skip_slot` | `ask_no_slot` | `auto_log` | `ask_unsure`, ou null fora do cliente v4), `record_intent` (`clear` | `unsure` | null) e `meal_day` (`today` | `other` | null), estes dois do modelo, para todo cliente (null se o modelo não devolveu ou se o turno foi recusado antes do shaping). Sem texto do usuário nesses campos.

## Decisoes relacionadas

- [ADR-012](../../produto/adrs/ADR-012-chat-home-perfil.md)
- [ADR-017](../../produto/adrs/ADR-017-registro-consolidado.md)
- [ADR-015](../adrs/ADR-015-log-conversa-dev.md)
- [ADR-023](../../produto/adrs/ADR-023-chat-v2-memoria-v2.md)
- [ADR-028](../../produto/adrs/ADR-028-registro-autonomo.md) (proposto; o S14 executa as decisões 1, 2, 4, 7 e 8)
- [api-contract.md](../../api-contract.md) ate S2 atualizar

## Planos relacionados

- [S1](../plans/completed/s1-timeout-photo-cap.md)
- [S2](../plans/completed/s2-v1-chat.md)
- [S3 (Concluido)](../plans/completed/s3-compact.md)
- [S8 (Concluído)](../plans/completed/s8-chat-json-slot-consolidado.md)
- [S10 (Concluído)](../plans/completed/s10-avaliacao-chat.md) — avaliador `server/evals/`
- [S11 (Concluído)](../plans/completed/s11-chat-v2.md) — Chat v2
- [S12 (Concluído)](../plans/completed/s12-slot-nomeado.md) — slot nomeado vence a semelhança
- [S13 (Concluído)](../plans/completed/s13-perguntas-antes-da-estimativa.md) — perguntas antes da estimativa, limite de 3 rodadas
- [S14 (Concluído)](../plans/completed/s14-registro-autonomo.md) — marca de registro (`record`) e pulo por texto (`skip`)
- [S15 (Concluído)](../plans/completed/s15-registro-casos-dificeis.md) — refeição pendente não é pulo, pulo firme avisado antes é, foto com pergunta é `ask`

## Criterios de aceite funcionais

- POST /v1/chat texto sem foto devolve reply + estimate com numeros.
- O server nunca grava: só marca `record` (sem side effect).
- Cliente v4: `log` claro de hoje com slot → `record: auto`; `log` sem clareza → `ask`; `plan`, `question`, turno só de pergunta, outro dia ou recusa → `none`; "pulei o café" → `intent: skip`, `skip_slot` do café, `record: auto`. "ainda não almocei" e "acho que não vou jantar" → `record: none`, nunca `skip`; "hoje não vou jantar" → `skip`, `record: auto`; foto de comida com pergunta → `record: ask` com estimate; foto sem texto → `auto` (S15).
- Request sem `auto_record` devolve o mesmo formato de antes do S14.
- compact=true devolve digest nao-vazio e messages nao voltam no OUT.
- Foto > cap = 413 e Luna nao e chamada (asserção no teste com transport fake).
