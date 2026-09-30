# Plano — S10 Avaliação do Chat com casos reais

- Estado: Aguardando aprovação
- Data: 30/09/2026
- Contexto proprietário: `server`
- Código afetado: `server/evals/` (novo), `server/tests/` (teste do avaliador)
- Pré-requisitos: Nenhum. Executa a decisão 6 do [ADR-023](../../produto/adrs/ADR-023-chat-v2-memoria-v2.md). **Primeiro plano da série Chat v2.**

## Gate de autorização

Este plano é exclusivamente documental. A implementação só começa após aprovação explícita que identifique este arquivo:

> Aprovo o plano `docs/server/plans/s10-avaliacao-chat.md`. Implemente o plano aprovado.

Se a implementação revelar decisão não coberta, pare, atualize os artefatos e peça nova aprovação.

## Objetivo

Um avaliador que roda o prompt do Chat contra o modelo real, com casos escritos a partir do uso real, e mede taxa de acerto, latência e tokens por `reasoning.effort`. Este plano entrega o avaliador, os casos e a **linha de base** do prompt atual. A comparação `none` × `low` com o prompt novo é do [S11](s11-chat-v2.md).

## Fontes de verdade

- [ADR-023](../../produto/adrs/ADR-023-chat-v2-memoria-v2.md), [ADR-015](../adrs/ADR-015-log-conversa-dev.md).
- [v1-chat](../specifications/v1-chat.md), [api-contract.md](../../api-contract.md).

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

## Encerramento

Depois da implementação, registre resultados reais e aplique o ciclo de vida em `docs/sdd/README.md`, incluindo a entrega git (§ 6). Sem validação manual: concluído, vai direto para `completed/`.

Só declaração explícita do dono cancelando este plano permite `Cancelado` e a pasta `plans/cancelled/`.
