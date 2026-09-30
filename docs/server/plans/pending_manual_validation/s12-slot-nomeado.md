# Plano — S12 Slot da refeição: o nome vence a semelhança com o registro

- Estado: Pendente aprovação manual (aprovado e implementado em 30/09/2026)
- Data: 30/09/2026
- Contexto proprietário: `server`
- Código afetado: `server/llm.py` (`_CHAT_INSTRUCTIONS`), `server/evals/cases/`
- Pré-requisitos: [S11](s11-chat-v2.md) no ar no server de dev (instruções e avaliador atuais). Sem mudança de contrato, schema ou client.

## Gate de autorização

Este plano é exclusivamente documental. A implementação só começa após aprovação explícita que identifique este arquivo:

> Aprovo o plano `docs/server/plans/s12-slot-nomeado.md`. Implemente o plano aprovado.

Se a implementação revelar decisão não coberta, pare, atualize os artefatos e peça nova aprovação.

## Objetivo

O `suggested_slot` segue o que o usuário disse da refeição, com os nomes e horários do perfil dele, mesmo que a comida seja parecida com uma refeição já gravada no dia. A regra de refeição consolidada ([ADR-017](../../../produto/adrs/ADR-017-registro-consolidado.md)) só vale quando a mensagem se refere à refeição gravada.

## Diagnóstico (30/09/2026, validação do A27)

No emulador, o Almoço tinha "almocei 2 ovos mexidos e 1 pao frances" gravado. Depois: "cafe igual ao de ontem" → "O que você comeu?" → "2 ovos mexidos, 1 pao frances e 200 ml de leite". A IA respondeu "O almoço fica em cerca de 530 kcal…, substituindo o almoço já registrado", com `suggested_slot` = Almoço.

Requests reais no server de dev (`X-Request-Id: slotprobe-*`):

| Cenário | Slot devolvido |
|---|---|
| "Cafe" às 20:00, 15:55, sem histórico do almoço (conteúdo do Almoço igual ou diferente) | Cafe 6/6 |
| Mensagem que nomeia ("no café comi …"), Cafe às 20:00 | Cafe 3/3 |
| Noturno (Jantar 03:00, Café 14:00, Almoço 20:00), 03:10, sem nomear / "jantei" | Jantar 3/3 e 3/3 |
| Noturno, 14:10, sem nomear | Café 3/3 |
| Histórico completo do dia (conversa do almoço + pergunta do café), Cafe às 20:00 | Cafe 5/5 |
| **Histórico completo do dia, Cafe às 07:30** | **Almoço 3/5** |

Conclusão: o horário do slot não é a causa, e perfis fora do padrão (jantar de madrugada) já funcionam. A falha é de prioridade entre duas regras das instruções:

- `suggested_slot`: "a refeição que a mensagem nomeia; senão, a da mensagem anterior do usuário no HISTORY; senão, a do horário".
- Refeição consolidada: "se a mensagem acrescenta, remove ou corrige comida de uma refeição cujo slot está `eaten`, devolva a refeição inteira com aquele slot e diga que substitui".

Quando a comida da resposta repete a de um slot gravado e a conversa daquele slot está no HISTORY, o modelo às vezes aplica a segunda por semelhança e ignora que o usuário nomeou o café na mensagem anterior.

## Fontes de verdade

- [v1-chat](../../specifications/v1-chat.md) regras 4 e 6, [ADR-017](../../../produto/adrs/ADR-017-registro-consolidado.md), [chat](../../../produto/specifications/chat.md) regra 4.
- [S10](../completed/s10-avaliacao-chat.md) (avaliador), [S11](s11-chat-v2.md) (instruções atuais).

## Escopo de implementação

### 0. Confirmar com o log real

Antes de mudar o prompt: ler no log de conversa do dev ([S6](s6-log-conversa-dev.md)) o turno de 30/09/2026 ~15:55 do emulador e conferir se o prompt enviado bate com o cenário reproduzido (histórico do almoço + pergunta do café). Se não bater, parar e reportar antes de seguir.

### 1. Instruções (`_CHAT_INSTRUCTIONS`, em inglês)

1. **Ordem do `suggested_slot`**, escrita como prioridade explícita:
   1. a refeição nomeada na mensagem atual (nome do slot do PROFILE ou palavra comum: café, almoço, jantar, lanche, ceia, "jantei", "almocei");
   2. senão, a refeição nomeada na fala do usuário que esta mensagem responde ou continua (ex.: "cafe igual ao de ontem" → pergunta → resposta);
   3. senão, correção de uma refeição gravada (regra 2 abaixo);
   4. senão, o slot do horário local pelos horários do PROFILE, ou o vazio mais próximo.
   - Os nomes e horários vêm só do PROFILE. Nunca supor horário "normal" de café, almoço ou jantar: um Jantar às 03:00 é o Jantar.
2. **Refeição consolidada mais estrita:** só quando a mensagem se refere à refeição gravada: nomeia aquele slot, ou é um acréscimo/correção explícito ("também", "faltou", "esqueci", "na verdade", "tirando", "era X e não Y") sem nomear outra refeição. Comida igual ou parecida à gravada **não** basta: com outra refeição nomeada (itens 1–2 acima), é uma refeição nova daquele slot, e o `reply` não diz que substitui.
3. Resto das instruções sem mudança (intenção, `meal_text`, plano, memória, histórico).

### 2. Avaliador (`server/evals/cases/`, tag `slot`)

Casos novos, todos cliente v2 (`facts: []`), com `suggested_slot` esperado:

| id | Cenário | Esperado |
|---|---|---|
| `slot-cafe-repete-almoco` | Caso que falhou: Cafe 07:30, Almoço gravado com os mesmos itens, histórico completo, resposta "2 ovos mexidos, 1 pão francês e 200 ml de leite" às 15:55 | Cafe, `reply_not: ["substitu"]` |
| `slot-cafe-20h` | O mesmo com Cafe às 20:00 | Cafe |
| `slot-jantei-igual-almoco` | 20:10, Almoço gravado "2 ovos mexidos e 1 pão francês", "jantei 2 ovos mexidos e 1 pão francês" | Jantar |
| `slot-noturno-3h-sem-nome` | Jantar 03:00, Café 14:00, Almoço 20:00; 03:10; "comi um prato de macarrão com carne moída" | Jantar |
| `slot-noturno-jantei` | O mesmo perfil, "jantei …" | Jantar |
| `slot-noturno-cafe-14h` | O mesmo perfil, 14:10, Jantar gravado, "2 ovos mexidos e 1 pão francês" | Café |
| `slot-correcao-faltou` | Controle da regra consolidada: Almoço gravado "arroz, feijão e bife", 13:30, "faltou dizer que tinha farofa" | Almoço, `meal_text_has: ["bife", "farofa"]` |

As expectativas usadas (`suggested_slot`, `reply_not`, `meal_text_has`) já existem no `checks.py`: o avaliador não muda.

### 3. Spec e contrato

- [v1-chat](../../specifications/v1-chat.md) regras 4 e 6: a ordem de prioridade acima e "nomes e horários só do PROFILE".
- `docs/api-contract.md`: sem mudança de campo. Só a frase do `suggested_slot` se ela citar a ordem.

## Arquivos e áreas afetadas

- `server/llm.py`.
- `server/evals/cases/slot-*.json` (7 casos).
- `docs/server/specifications/v1-chat.md`, `docs/server/README.md`, este plano.

## Validação planejada

1. `pytest -q` verde.
2. `python -m evals.run --effort none --tag slot --repeat 5`: cada caso novo 5/5.
3. `python -m evals.run --effort none --repeat 3`: suíte inteira (22 casos atuais + 7) sem regressão; os 22 atuais continuam aprovados.
4. `./tools/deploy-gcp.ps1`, `/health` 200; repetir o caso `slot-cafe-repete-almoco` 5× no server de dev com `X-Request-Id: s12-*`.
5. Manual (dono): no APK atual, repetir a sequência do emulador (almoço gravado com ovos e pão → "café igual ao de ontem" → resposta com os mesmos itens) → a barra mostra "Gravar café" (ou o nome do slot do café dele).

## Fora de escopo

- **Virada do dia para quem trabalha à noite.** O dia do app vira à meia-noite de America/Sao_Paulo (`AGENTS.md`, Formulas): o jantar das 03:00 conta no teto do dia seguinte. Mudar isso (horário de virada por perfil) é decisão de produto, com ADR próprio. Não entra aqui.
- Mudança no client: o app já usa o `suggested_slot` só como sugestão ("Gravar {slot}" + Trocar).
- Schema, contrato, `reasoning.effort`, troca de modelo.

## Riscos e controles

- **Regra estrita quebrar a consolidação do ADR-017** ("também tomei suco" deixar de somar no slot gravado): o caso de controle `slot-correcao-faltou` e os casos atuais `ceia-completa-suco` e `almoco-resposta-peso` precisam continuar aprovados.
- **Palavras comuns (café) casarem com a bebida e não com a refeição** ("tomei um café" às 16:00): a regra 1 fala em refeição nomeada; se o avaliador mostrar confusão, caso novo e ajuste antes do deploy, registrado aqui.
- **Prompt maior:** poucas linhas nas instruções fixas, que ficam em cache.

## Critérios de aceite

- A refeição nomeada pelo usuário (na mensagem ou na fala que ela responde) decide o slot, mesmo com comida igual à de um slot gravado.
- Perfis com horários fora do padrão (Jantar 03:00, Café 14:00) recebem o slot certo com e sem o nome.
- Correção explícita de uma refeição gravada continua devolvendo a refeição inteira naquele slot.
- Suíte do avaliador sem regressão.

## Encerramento

Depois da implementação, registre resultados reais e aplique o ciclo de vida em `docs/sdd/README.md`, incluindo a entrega git (§ 6).

Só declaração explícita do dono cancelando este plano permite `Cancelado` e a pasta `plans/cancelled/`.

## Resultados (30/09/2026)

### 0. Log real

`./tools/pull-conversations.ps1 -Download`, request `9fc7f37a-5ddd-46b1-9b0d-113fad55af0d` (0.0.3-dev, 15:55:25): HISTORY com a conversa do almoço ("almocei 2 ovos mexidos e 1 pao frances" → "com manteiga") e "cafe igual ao de ontem" → "O que você comeu ou bebeu?"; mensagem "2 ovos mexidos, 1 pao frances e 200 ml de leite"; DAY com o slot 1 (Almoço) `eaten` com o mesmo texto. Saída: `suggested_slot` "1" e "substituindo o almoço já registrado". Bate com o cenário. Diferença: no perfil real o Cafe é às **20:00** (não 07:30), e o erro aconteceu assim mesmo: o caso `slot-cafe-20h` reproduz o prompt real; o `slot-cafe-repete-almoco` fica com 07:30, como previsto.

### 1. Instruções

`_CHAT_INSTRUCTIONS` (`server/llm.py`): `suggested_slot` com prioridade 1–4 explícita e "nomes e horários só do PROFILE"; regra consolidada estrita ("refere-se à refeição gravada" = nomeia o slot ou acréscimo/correção explícito sem nomear outra refeição; comida parecida não basta, e o reply não fala em substituir).

Dois ajustes antes do deploy, previstos em Riscos:

- `slot-correcao-faltou` saiu 3/5: o slot estava certo, mas quando o modelo perguntava a porção da farofa, ele a deixava fora de `items`/`meal_text`. A instrução antiga também falhava (4/5 no mesmo teste). Frase nova: "An added food always enters items, kcal and meal_text, with an assumed portion even if you ask about it." → 5/5.
- `ceia-completa-suco` caiu para 1/3 na suíte ("Na ceia também tomei suco" virou refeição nova só com o suco). A regra estrita passou a dar o exemplo: nomear o slot gravado é referir-se a ele ("na ceia também tomei suco, with Ceia eaten"). → 5/5 e 3/3 na suíte.

### 2. Avaliador

7 casos `slot` em `server/evals/cases/slot-*.json` (cliente v2, `facts: []`), como na tabela. `slot-cafe-*` também exigem `meal_text_has: ["ovo", "leite"]`; `slot-jantei-igual-almoco` também `reply_not: ["substitu"]`. `checks.py` sem mudança.

### Validação executada

1. `pytest -q` (em `server/`) → **95 passed**, 70 subtests. ✅
2. `python -m evals.run --effort none --tag slot --repeat 5` → **7/7, cada caso 5/5** (`logs/evals/2026-09-30-162936-none.json`). ✅
3. `python -m evals.run --effort none --repeat 3` → **29/29 (100%)**, os 22 antigos aprovados, `ceia-completa-suco` e `almoco-resposta-peso` 3/3; p50 2,7 s, p95 4,0 s, ~2,4k tokens de entrada por chamada (2,2k em cache) (`logs/evals/2026-09-30-163059-none.json`). ✅
4. `./tools/deploy-gcp.ps1` → `/health` 200 (`gpt-6-luna`). `slot-cafe-repete-almoco` 5× no server de dev (`X-Request-Id: s12-cafe-1..5`) → `suggested_slot` "3" (Cafe) **5/5**, nenhum reply com "substitu". ✅
5. Manual (dono): **pendente**. No APK atual, repetir almoço gravado com ovos e pão → "café igual ao de ontem" → resposta com os mesmos itens → a barra mostra "Gravar Cafe".

Não testado no avaliador: "tomei um café" às 16:00 (café bebida × refeição). Nenhum caso atual mostrou confusão; fica para um caso novo se aparecer no log.

`docs/api-contract.md` não cita a ordem do `suggested_slot`: sem mudança.
