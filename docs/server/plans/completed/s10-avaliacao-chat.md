# Plano — S10 Avaliação do Chat com casos reais

- Estado: Concluído
- Data: 30/09/2026
- Contexto proprietário: `server`
- Código afetado: `server/evals/` (novo), `server/tests/` (teste do avaliador)
- Pré-requisitos: Nenhum. Executa a decisão 6 do [ADR-023](../../../produto/adrs/ADR-023-chat-v2-memoria-v2.md). **Primeiro plano da série Chat v2.**

## Gate de autorização

Este plano é exclusivamente documental. A implementação só começa após aprovação explícita que identifique este arquivo:

> Aprovo o plano `docs/server/plans/s10-avaliacao-chat.md`. Implemente o plano aprovado.

Se a implementação revelar decisão não coberta, pare, atualize os artefatos e peça nova aprovação.

## Objetivo

Um avaliador que roda o prompt do Chat contra o modelo real, com casos escritos a partir do uso real, e mede taxa de acerto, latência e tokens por `reasoning.effort`. Este plano entrega o avaliador, os casos e a **linha de base** do prompt atual. A comparação `none` × `low` com o prompt novo é do [S11](s11-chat-v2.md).

## Fontes de verdade

- [ADR-023](../../../produto/adrs/ADR-023-chat-v2-memoria-v2.md), [ADR-015](../../adrs/ADR-015-log-conversa-dev.md).
- [v1-chat](../../specifications/v1-chat.md), [api-contract.md](../../../api-contract.md).

## Escopo de implementação

### 1. Coleta

- `./tools/pull-conversations.ps1 -Download` baixa o log do server de dev para `logs/conversations.jsonl` (fora do git). Inclui o café com a pergunta do leite (30/09) e a pizza de pão sírio (29/09).
- O agente lê o log e escolhe os casos. **O log bruto nunca entra no git.** Os casos são reescritos à mão: mesma situação, texto próprio, sem nome, e-mail ou dado de tester. Nenhum texto de tester é copiado.

### 2. Casos (`server/evals/cases/*.json`)

Um arquivo por caso, no formato:

```json
{
  "id": "cafe-resposta-leite",
  "since": "v1",
  "tags": ["log", "meal_text", "memory"],
  "request": { "...": "corpo ChatIn completo, como o app manda" },
  "expect": {
    "intent": "log",
    "estimate": "present",
    "suggested_slot": "1",
    "kcal_range": [380, 520],
    "meal_text_has": ["ovo", "pão", "leite"],
    "meal_text_not": ["Sempre uso"],
    "question": "absent",
    "memory_updates_has": [{"op": "add", "kind": "permanent", "key": "leite"}],
    "memory_used_has": [],
    "reply_not": ["registrei"]
  }
}
```

- `since: "v1"`: o contrato de hoje já permite avaliar (intenção deduzida de `estimate`). `since: "v2"`: precisa dos campos do S11 (`intent`, `meal_text`, `memory_updates`, `memory_used`, `facts`, `recent`). Na linha de base, os casos `v2` rodam, e as expectativas que o contrato atual não tem aparecem como `n/a`, não como falha.
- Cada expectativa é opcional. `kcal_range` é largo de propósito: mede se a estimativa é plausível, não o número exato.

Conjunto inicial (mínimo 20 casos), cobrindo:

| Grupo | Casos |
|---|---|
| Texto da refeição | café "igual ao de ontem" + resposta sobre o leite; almoço com pergunta de peso respondida; completar refeição gravada (suco na ceia) |
| Intenção | pizza de pão sírio "vou fazer, quantas gramas?"; "o que como na janta?"; "cabe um açaí?"; "comi uma pizza de pão sírio"; dúvida sem comida; saudação |
| Histórico | "mesmo café de ontem" com `recent`; "igual ao almoço de segunda" |
| Memória | "sempre uso leite semidesnatado"; "agora uso leite integral" (contradição); "esquece o pão francês"; memória cheia (30 permanentes); iogurte Nuv visto de novo (reforço); fato conhecido evita pergunta (tipo de leite já na memória) |
| Travas do S8 | total sem comida ("comi 1220 kcal"); "ontem" grava hoje; foto marcada `[foto]` no histórico |

### 3. Avaliador (`server/evals/run.py`)

- `python -m evals.run --effort none low --repeat 3 [--only <id,...>] [--tag <tag>]`, rodado dentro de `server/` com a `.venv`.
- Monta o prompt com o **mesmo código da rota** (`_chat_text`, `LlmClient.chat_json`, `shape_chat`), sem HTTP: o que é avaliado é exatamente o que vai para o modelo em produção. `reasoning.effort` passa a ser parâmetro de `LlmClient` (padrão `none`, sem mudar o comportamento da rota).
- Chave: `OPENAI_API_KEY` do `.env` da raiz, lida por `config.load_settings()`. Nunca impressa.
- Por caso e por effort: aprovado se **todas** as expectativas aplicáveis passarem em pelo menos 2 das 3 repetições.
- Relatório no terminal e em `logs/evals/<data>-<effort>.json` (fora do git): taxa de aprovação total e por tag, expectativas que falharam por caso (com o `raw_output`), latência p50/p95, tokens de entrada, saída e raciocínio (`response.usage`) e custo estimado.
- Sem paralelismo agressivo: no máximo 3 chamadas simultâneas, para não esbarrar em rate limit.

### 4. Teste do avaliador (`server/tests/test_evals.py`)

- Com transport fake (como os testes do chat): um caso que passa, um que falha em cada tipo de expectativa, `n/a` para campo `v2` na saída `v1`, e a regra "2 de 3".
- Todo arquivo em `server/evals/cases/` é JSON válido, tem `id` único e um `request` aceito pelo modelo `ChatIn`.

### 5. Linha de base

- `python -m evals.run --effort none --repeat 3` com o prompt atual. O resultado vai para a seção Resultados deste plano: taxa total, taxa por tag, casos que falham. É a régua do S11.

## Arquivos e áreas afetadas

- `server/evals/__init__.py`, `server/evals/run.py`, `server/evals/checks.py`, `server/evals/cases/*.json`.
- `server/llm.py`: só o parâmetro `effort` no `LlmClient` (padrão `none`).
- `server/tests/test_evals.py`.
- `docs/server/README.md` (como rodar), este plano.

## Validação planejada

1. `server/.venv/Scripts/python -m pytest -q` verde (suíte atual + `test_evals.py`).
2. Linha de base executada e registrada.
3. `git grep` sem texto de tester nem trecho literal do log em `server/evals/`.

## Fora de escopo

- Mudança de prompt, de contrato ou do effort da rota: [S11](s11-chat-v2.md).
- Deploy: o avaliador roda na máquina do dono, não no server.
- Avaliação de `/v1/estimate` e `/v1/fit`.

## Riscos e controles

- **Custo:** 20+ casos × 3 repetições × 2 efforts ≈ 120+ chamadas por rodada. `--only` e `--tag` permitem rodar só o que mudou. O relatório mostra o custo.
- **Expectativa frágil (número exato):** faixas largas e termos-chave, nunca texto exato do `reply`.
- **Dado pessoal no git:** casos reescritos, checagem da validação 3.

## Critérios de aceite

- `python -m evals.run` roda os casos com `none` e `low` e gera o relatório.
- Pelo menos 20 casos, cobrindo todos os grupos da tabela.
- Linha de base registrada neste plano.

## Resultados (30/09/2026)

### Entregue

- `server/evals/run.py`, `server/evals/checks.py`, `server/evals/__init__.py`; `LlmClient(effort=...)` com padrão `none` (rota inalterada, coberta por teste).
- 21 casos em `server/evals/cases/`, cobrindo os 5 grupos da tabela: texto da refeição (3), intenção (6), histórico (2), memória (7), travas do S8 (3). 7 `v1`, 14 `v2`.
- Expectativas: as do formato acima mais `memory_updates_not` (mudança que não pode aparecer: refeição avulsa, contradição, memória cheia) e `reply_has` (termo-chave, nunca frase). Termos comparados sem caixa e sem acento.
- Tokens lidos de `response.usage` por um transport que só observa a resposta. Custo com a tabela padrão do gpt-6-luna (US$ 0,10 entrada, 0,01 entrada em cache, 0,50 saída por 1M tokens; raciocínio cobra como saída), em constantes de `run.py`.
- Relatório com data e hora no nome (`logs/evals/<AAAA-MM-DD-HHMMSS>-<effort>.json`), para uma rodada parcial não sobrescrever outra.
- Coleta: `./tools/pull-conversations.ps1 -Download` (log de 28/09 a 30/09, em `logs/`, fora do git). Situações do café com a resposta do leite, da pizza de pão sírio (plano e registro), do suco na ceia, do total sem comida, do "ontem", da foto e do iogurte Nuv, reescritas à mão.

### Linha de base — prompt atual, `effort=none`, 3 repetições (30/09/2026 12:03)

`python -m evals.run --effort none --repeat 3`

- **Total: 11/21 aprovados (52,4%)**, 0 `n/a`.
- Por tag: `consolidated` 2/2, `s8` 3/3, `meal_text` 3/4, `log` 7/11, `intent` 3/6, `question` 3/6, `memory` 3/8, `history` 0/2, `plan` 0/3.
- Latência p50 2,2 s, p95 4,5 s. Tokens por chamada: entrada 1086 (575 em cache), saída 114, raciocínio 0. Custo da rodada (63 chamadas): US$ 0,0072.

| Caso | Resultado | Motivo |
|---|---|---|
| `cabe-acai` | falha 0/3 | estimate presente sem intenção: vira card de registro (plano tratado como `log`) |
| `pizza-pao-sirio-plano` | falha 0/3 | mesmo defeito do plano; o `reply` não traz o total em kcal |
| `janta-o-que-como` | falha 0/3 | sugere prato sem estimate e sem kcal no `reply` |
| `cafe-resposta-leite` | falha 0/3 | depois da resposta sobre o leite, pergunta de novo (manteiga) |
| `memoria-evita-pergunta-leite` | falha 0/3 | pergunta a manteiga; o fato do leite não chega ao prompt (v1 ignora `facts`) |
| `mesmo-cafe-ontem-recent` | falha 0/3 | sem `recent` no prompt: não sabe o café de ontem e pede a refeição |
| `igual-almoco-segunda` | falha 0/3 | idem, almoço de segunda |
| `memoria-contradicao-leite` | falha 1/3 | "agora uso leite integral" vira reestimativa do café gravado |
| `memoria-esquece-pao` | falha 0/3 | "esquece o pão" vira reestimativa do café gravado |
| `memoria-cheia` | falha 0/3 | não existe memória fixa cheia no v1 |

Aprovados: `almoco-resposta-peso`, `ceia-completa-suco`, `comi-pizza-pao-sirio`, `duvida-whey`, `foto-no-historico`, `memoria-refeicao-avulsa`, `memoria-reforco-iogurte`, `memoria-sempre-leite`, `ontem-grava-hoje`, `saudacao`, `total-sem-comida`. Nos casos `v2`, `meal_text_*`, `memory_updates_*` e `memory_used_has` ficaram `n/a` (campos do S11), então o número mede só o que o contrato v1 mostra.

Variação entre rodadas iguais: duas rodadas anteriores deram 10/21; `memoria-sempre-leite` oscila (em uma rodada o modelo estimou um café). O S11 compara `none` × `low` nas mesmas 3 repetições.

Smoke `--effort none low --repeat 1 --only saudacao,cabe-acai`: as duas rodaram; `low` usou 122 tokens de raciocínio por chamada, p95 6,8 s.

Ajuste de caso durante a linha de base: `memoria-reforco-iogurte` tinha faixa 120–260 kcal; 110 kcal (1 iogurte Nuv + 120 g de morango) é plausível, a faixa foi para 90–260 antes da rodada registrada.

### Validação

1. `server/.venv/Scripts/python -m pytest -q`: 76 passed, 46 subtests (suíte anterior + `test_evals.py`: aprovado, falha em cada uma das 12 expectativas, `n/a` de v2 em saída v1, intenção deduzida, regra 2 de 3, casos válidos no `ChatIn`, rodada com transport fake incluindo tokens, custo, `effort=low` enviado, erro sem vazar a chave, rota ainda em `none`).
2. Linha de base executada e registrada acima.
3. Nenhum texto de tester nem trecho do log: `git grep -i -E "nicolas|icaro|@gmail|teixeira" -- server/evals` vazio; nenhuma janela de 30 caracteres dos textos dos casos aparece no `input_text`/`raw_output` do log (zero ocorrências, depois de reescrever frases genéricas de comida que coincidiam).

Sem validação manual prevista: `Concluído`.

## Encerramento

Depois da implementação, registre resultados reais e aplique o ciclo de vida em `docs/sdd/README.md`, incluindo a entrega git (§ 6). Sem validação manual: concluído, vai direto para `completed/`.

Só declaração explícita do dono cancelando este plano permite `Cancelado` e a pasta `plans/cancelled/`.
