# server

## Proposito

API HTTP do Nutri. Estima refeicao e devolve prato que cabe. Nao calcula teto. Nao guarda o dia.

## Tipo e ownership

- Tipo: contrato HTTP.
- Codigo principal: `server/`.
- Consumidor: [android](../android/README.md).
- Infra S0 indexada aqui: `server/docker-compose.yml`, `infra/`, [MIGRACAO-VPS.md](../MIGRACAO-VPS.md).

## Escopo

- GET /health, POST /v1/estimate, POST /v1/fit, POST /v1/chat.
- Auth X-Invite.
- LLM gpt-6-luna, reasoning.effort=none.
- Foto entra no request e some. Nao persiste.
- Cap 16 MB JPEG (`PHOTO_MAX_B64_CHARS = 22_400_000`). Acima disso HTTP 413 `photo_too_large` antes da Luna.
- Shaping do JSON de contrato.
- Compose + Cloudflare Tunnel. Sem porta no roteador.

## Fora de escopo

- Teto, eat-back, Room, push, UI — [android](../android/README.md) / [produto](../produto/README.md).
- Conta de user, Stripe, persistencia do dia no server.
- VPS agora. Texto de migracao em [MIGRACAO-VPS.md](../MIGRACAO-VPS.md).

## Fronteiras e dependencias

- Contrato vivo hoje: [api-contract.md](../api-contract.md). Spec de chat: [v1-chat.md](specifications/v1-chat.md).
- Timeout vigente: 60s em server/config.py (`TIMEOUT_SECONDS`). Keep-alive do uvicorn precisa ser >=60s. Docker nao muda neste plano.
- Cap foto: `PHOTO_MAX_BYTES` 16 MB / `PHOTO_MAX_B64_CHARS` 22_400_000.
- ADRs de repo: [001](../decisions/001-monorepo.md), [007](../decisions/007-english-identifiers.md).

## Cobertura documental atual

Fonte HTTP: [api-contract.md](../api-contract.md), [v1-chat.md](specifications/v1-chat.md), `server/tests/test_api.py`, `server/tests/test_photo_cap.py`, `server/tests/test_security.py`, `server/tests/test_chat.py`.
[S1](plans/completed/s1-timeout-photo-cap.md), [S4](plans/completed/s4-security-hardening.md), [S2](plans/completed/s2-v1-chat.md) e [S3](plans/completed/s3-compact.md) concluidos.

## Como usar esta documentacao

Segue [docs/sdd/README.md](../sdd/README.md).

1. [matriz](../README.md) e este README
2. [api-contract.md](../api-contract.md)
3. [v1-chat.md](specifications/v1-chat.md)
4. server/main.py, llm.py, shaping.py, config.py

## Estado atual

Contrato /v1/estimate, /v1/fit e /v1/chat no ar. Timeout 60s. Cap 16 MB JPEG; `image_b64` acima de 22_400_000 chars → HTTP 413 `photo_too_large` antes da Luna. `compact=true` devolve digest (S3). O client ainda tem o compact desligado (`PromptBuilder.COMPACT_ENABLED = false`) ate um plano android ligar.

Keep-alive do uvicorn precisa ser >=60s. Docker nao muda neste plano.

## Indice

### Especificacoes

- [v1-chat.md](specifications/v1-chat.md) — timeout/cap vigentes; rota `/v1/chat` no ar.
- Contrato HTTP: [api-contract.md](../api-contract.md).

### ADRs

Nenhum ADR local. Historico: [001](../decisions/001-monorepo.md), [007](../decisions/007-english-identifiers.md).

### Planos e validacao

- [S1 concluido](plans/completed/s1-timeout-photo-cap.md) — timeout 60s + cap 16 MB.
- [S4 concluido](plans/completed/s4-security-hardening.md) — hardening de seguranca (rate limiting, constant time auth, payload limit).
- [S2 concluido](plans/completed/s2-v1-chat.md) — POST /v1/chat.
- [S3 concluido](plans/completed/s3-compact.md) — compact digest.
- Testes: `server/tests/test_api.py`, `server/tests/test_photo_cap.py`, `server/tests/test_security.py`, `server/tests/test_chat.py`.
