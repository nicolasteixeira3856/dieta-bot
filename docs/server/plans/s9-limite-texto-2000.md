# Plano — S9 Texto do Chat até 2000 caracteres

- Estado: Aguardando aprovação
- Data: 29/09/2026
- Contexto proprietário: `server`
- Código afetado: `server/main.py`, `server/tests/`
- Pré-requisitos: Nenhum. Executa o [ADR-022](../../produto/adrs/ADR-022-limite-texto-chat.md) (regra 4).

## Gate de autorização

Este plano é exclusivamente documental. A implementação só começa após aprovação explícita que identifique este arquivo:

> Aprovo o plano `docs/server/plans/s9-limite-texto-2000.md`. Implemente o plano aprovado.

Se a implementação revelar decisão não coberta, pare, atualize os artefatos e peça nova aprovação.

## Objetivo

O `POST /v1/chat` aceita `text` e `messages[].text` até 2000 caracteres. Vai ao ar no dev antes do [A25](../../android/plans/a25-limite-texto-composer.md), para o client nunca mandar algo que o server recusa.

## Fontes de verdade

- [ADR-022](../../produto/adrs/ADR-022-limite-texto-chat.md), [v1-chat](../specifications/v1-chat.md), [api-contract](../../api-contract.md).

## Escopo de implementação

### 1. Contrato (`server/main.py`)

- `ChatIn.text`: `max_length` 1000 → 2000.
- `ChatMessageIn.text`: `max_length` 1000 → 2000.
- `/v1/estimate` e `/v1/fit` (`text` ≤ 1000) não mudam.
- Nenhuma outra mudança de prompt, schema ou limite de corpo (`MAX_BODY_BYTES` 24 MB já cobre).

### 2. Testes (`server/tests/test_chat.py`)

- `test_chat_pydantic_validation_limits`: 2000 caracteres → 200; 2001 → 422, para `text` e para `messages[].text`.
- 2000 caracteres com emoji (code points, não bytes) → 200.

### 3. Docs

- `docs/api-contract.md`: `text` e `messages[].text` max 2000 no `/v1/chat`.
- `docs/server/specifications/v1-chat.md`: linha de estado com o S9.

### 4. Deploy

- `tools/deploy-gcp.ps1` no dev (runbook `docs/server/deploy-gcp.md`).

## Arquivos e áreas afetadas

- `server/main.py`, `server/tests/test_chat.py`, `docs/api-contract.md`, `docs/server/specifications/v1-chat.md`.

## Validação planejada

1. Suíte do server verde.
2. Depois do deploy: `GET /health` 200; `POST /v1/chat` com `text` de 2000 caracteres → 200; com 2001 → 422.
3. Log de conversa (S6) sem erro novo.

## Fora de escopo

- `/v1/estimate`, `/v1/fit`.
- Qualquer mudança no client: [A25](../../android/plans/a25-limite-texto-composer.md).

## Riscos e controles

- **Turno maior no pior caso (12 × 2000):** ~24 k caracteres de histórico, aceito no ADR-022. O timeout de 60 s continua; o log do S6 mostra a latência.
- **Client antigo:** manda ≤ 1000, continua válido.

## Critérios de aceite

- `/v1/chat` aceita 2000 e recusa 2001 em `text` e em `messages[].text`, no dev.

## Encerramento

Depois da implementação, registre resultados reais e aplique o ciclo de vida em `docs/sdd/README.md`, com a entrega git (§ 6).

Só declaração explícita do dono cancelando este plano permite `Cancelado` e a pasta `plans/cancelled/`.
