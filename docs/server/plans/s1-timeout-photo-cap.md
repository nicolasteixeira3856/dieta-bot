# Plano — S1 timeout 60s + cap 16MB

- Estado: Aguardando aprovacao
- Data: 25/09/2026
- Contexto proprietario: `server`
- Codigo afetado: `server/`
- Pre-requisitos: Nenhum

## Gate de autorizacao

Este plano e exclusivamente documental. A implementacao so comeca apos aprovacao explicita que identifique este arquivo:

> Aprovo o plano `docs/server/plans/s1-timeout-photo-cap.md`. Implemente o plano aprovado.

Se a implementacao revelar decisao nao coberta, pare, atualize os artefatos e peca nova aprovacao.

## Objetivo

estimate e fit sobrevivem. Timeout passa a 60s. Foto acima do cap e recusada com 413 antes da Luna.

## Fontes de verdade

- [v1-chat.md](../specifications/v1-chat.md) regras 8-10
- [api-contract.md](../../api-contract.md)

## Escopo de implementacao

### 1. config

- `TIMEOUT_SECONDS = 60.0`
- `PHOTO_MAX_BYTES = 16 * 1024 * 1024`
- `PHOTO_MAX_B64_CHARS = 22_400_000`

### 2. recusa

- Funcao pura `reject_photo(image_b64) -> None | "too_large"`
- Chamar em `/v1/estimate` e `/v1/fit` ANTES de `llm.*`
- HTTP 413 `{"detail":"photo_too_large"}`
- Nao logar `image_b64`. Zerar a ref no `finally` como hoje.

### 3. llm

- `httpx2.Timeout` e OpenAI timeout ja leem `TIMEOUT_SECONDS`. So o config muda.
- Teste de regressao: estimate texto ainda 200.

### 4. contrato

- `docs/api-contract.md`: Timeout 60s. Cap 16 MB JPEG. 413.
- `docs/server/README.md`: estado timeout 60s.

## Arquivos e areas afetadas

- `server/config.py`
- `server/main.py`
- `server/tests/test_api.py`
- `server/tests/test_photo_cap.py` (novo)
- `docs/api-contract.md`
- `docs/server/README.md`

## Validacao planejada

1. `pytest server/tests -q`
2. Teste 413 com `image_b64` de len > `PHOTO_MAX_B64_CHARS` (string fake)
3. Teste `/v1/estimate` texto "2 paes" ainda 200
4. `TIMEOUT_SECONDS == 60`

## Fora de escopo

- `/v1/chat`
- compact
- Android OkHttp (A6)
- HEIC, Multipart, shaping

## Riscos e controles

- **uvicorn keep-alive curto:** README do server nota timeout ≥60. Sem mudar Docker neste plano.
- **4G lento:** aceito. Foto falha. Fail-soft.

## Criterios de aceite

- `TIMEOUT_SECONDS` e 60
- POST `/v1/estimate` com `image_b64` gigante → 413 e o fake transport do LLM nao e chamado
- POST `/v1/estimate` texto "2 paes" → 200 com kcal
- `/v1/fit` e `/health` intactos

## Encerramento

Ciclo SDD em `docs/sdd/README.md`. Cancelado so com frase explicita do dono.
