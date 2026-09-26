# Plano — S3 compact digest

- Estado: Aguardando aprovacao
- Data: 25/09/2026
- Contexto proprietario: `server`
- Codigo afetado: `server/`
- Pre-requisitos: [s2-v1-chat.md](s2-v1-chat.md)

## Gate de autorizacao

> Aprovo o plano `docs/server/plans/s3-compact.md`. Implemente o plano aprovado.

## Objetivo

`compact=true` resume `messages` num digest ≤400 tokens. Server stateless: devolve o texto; o client grava no Room (A5/A8).

## Fontes de verdade

- [v1-chat.md](../specifications/v1-chat.md) regras 7 e 3
- [ADR-012](../../produto/adrs/ADR-012-chat-home-perfil.md)

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

## Encerramento

Ciclo SDD em `docs/sdd/README.md`.
