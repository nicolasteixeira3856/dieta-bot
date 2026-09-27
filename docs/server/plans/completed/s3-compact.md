# Plano — S3 compact digest (Concluido)

- Estado: Concluido
- Data: 25/09/2026
- Data de conclusao: 27/09/2026
- Contexto proprietario: `server`
- Codigo afetado: `server/`
- Pre-requisitos: [s2-v1-chat.md](s2-v1-chat.md)

## Gate de autorizacao

> Aprovado pelo usuario via comando:
> `/goal Aprovo o plano docs/server/plans/s3-compact.md. Analise e implemente o plano aprovado.`

## Objetivo

`compact=true` resume `messages` num digest ≤400 tokens. Server stateless: devolve o texto; o client grava no Room (A5/A8).

## Fontes de verdade

- [v1-chat.md](../../specifications/v1-chat.md) regras 7 e 3
- [ADR-012](../../../produto/adrs/ADR-012-chat-home-perfil.md)

## Escopo de implementacao

### 1. ramo compact

- `compact=true` deixa de ser 400
- Se `messages` vazio: 422
- Instructions de resumo: pt-BR, fatos (kcal, P, slot, pulou), sem conselho
- OUT.digest string nao-vazia. OUT.reply "". OUT.estimate null
- Foto em compact e ignorada (nao enviar image ao LLM)

### 2. ramo normal

- Inalterado. Client manda `digests[]` no IN.

### 3. testes

- compact de 12 msgs fake → digest len > 20
- compact messages=[] → 422
- chat normal ainda 200

## Arquivos e areas afetadas

- `server/llm.py`
- `server/main.py`
- `server/shaping.py`
- `server/config.py`
- `server/tests/test_chat.py`
- `docs/api-contract.md` (compact)

## Validacao planejada

1. `pytest server/tests -q`
2. compact=true com 4 pares user/assistant contendo "450 kcal" → digest menciona 450
3. Sem side effect de disco

## Fora de escopo

- Terceiro digest
- Empilhar 36 raw
- Client Room
- Substituicao de digest velho (regra de A5)

## Riscos e controles

- **resumo perde numero:** DAY snapshot no proximo turno cobre kcal. Digest e prosa.

## Criterios de aceite

- compact=true → 200 + digest
- compact=false → comportamento S2
- Server nao cria arquivo

## Resultado da implementacao

### Entregue

- `main.py`: `compact=true` vai para `_compact`: descarta `image_b64`, `messages` vazio → 422 `compact_needs_messages` (sem chamar a Luna), senao `llm.digest_json` com so as messages (`role: text` por linha).
- `llm.py`: `_DIGEST_INSTRUCTIONS` (pt-BR, ≤400 tokens, prosa, fatos: comida, kcal/P como citados, slot, pulos, confirmacoes; sem conselho, sem numero novo) e `wrap_history` (delimitadores `### CHAT_HISTORY_START/END` + aviso anti-injecao). Sem imagem, `store=false`, mesmo modelo e `reasoning.effort=none`.
- `shaping.py`: `shape_digest` → `{reply: "", estimate: null, digest, model}`; digest vazio/ausente = falha. `fail_digest` → 200 com `digest: null`.
- `config.py`: `DIGEST_MAX_CHARS = 1600` (~400 tokens).
- Ramo `compact=false` inalterado.

### Decisoes tomadas na implementacao

- **Falha em compact = 200 com `digest: null`** (fail-soft, regra 10 da spec). O client (A5) so grava digest se vier texto; senao mantem as raw.
- **Corte do digest em 1600 chars** como teto duro dos 400 tokens.
- **O client segue com o compact desligado** (`PromptBuilder.COMPACT_ENABLED = false`, A5). Ligar e trabalho de `apps/android/` — fora deste `/goal` (1 goal = 1 pasta). Pendente para um plano android — resolvido no [A5b](../../../android/plans/completed/a5b-ligar-compact.md).

## Validacao executada

1. `server/.venv/Scripts/python -m pytest server/tests -q`: 43 passed, 19 subtests. Novos em `test_chat.py`: 12 msgs → digest > 20 chars, reply "", estimate null, prompt so com o historico (sem `PROFILE:`), instructions de resumo; "450 kcal" chega ao prompt e ao digest; `messages=[]` → 422 sem chamar o modelo; foto ignorada (nem sentinela nem `input_image` no request) e nada em disco; digest de 5000 chars cortado em 1600; falha de rede / digest vazio / sem digest → 200 `digest: null`; `compact=false` mantem S2 (PROFILE + DIGESTS no prompt, instructions de chat). O teste antigo `compact_true_returns_400` saiu.
2. Chamada real (gpt-6-luna, `.env` local, via ASGI) com 4 pares contendo "450 kcal": HTTP 200, `reply ""`, `estimate null`, digest "No cafe da manha, foram registrados 2 paes e 2 ovos: 450 kcal e 22 g de proteina. O almoco foi pulado. O jantar foi planejado como leve." — menciona 450.
3. Sem side effect: nenhum arquivo novo em `server/` (fora de `.venv`/`__pycache__`) apos a chamada real; teste `_assert_not_on_disk` com a foto sentinela.

## Encerramento

Ciclo SDD.
