# server

## Proposito

API HTTP do Nutri. Estima refeicao e devolve prato que cabe. Nao calcula teto. Nao guarda o dia.

## Tipo e ownership

- Tipo: contrato HTTP.
- Codigo principal: `server/`.
- Consumidor: [android](../android/README.md).
- Infra S0 indexada aqui: `server/docker-compose.yml`, `infra/`, [MIGRACAO-VPS.md](../MIGRACAO-VPS.md).

## Escopo

- GET /health, POST /v1/estimate, POST /v1/fit.
- Auth X-Invite.
- LLM gpt-6-luna, reasoning.effort=none.
- Foto entra no request e some. Nao persiste.
- Shaping do JSON de contrato.
- Compose + Cloudflare Tunnel. Sem porta no roteador.

## Fora de escopo

- Teto, eat-back, Room, push, UI — [android](../android/README.md) / [produto](../produto/README.md).
- Conta de user, Stripe, persistencia do dia no server.
- VPS agora. Texto de migracao em [MIGRACAO-VPS.md](../MIGRACAO-VPS.md).

## Fronteiras e dependencias

- Contrato vivo hoje: [api-contract.md](../api-contract.md). Vira spec em docs/server/specifications/ quando o contrato mudar.
- Timeout vigente: server/config.py (TIMEOUT_SECONDS).
- ADRs de repo: [001](../decisions/001-monorepo.md), [007](../decisions/007-english-identifiers.md).

## Cobertura documental atual

Fonte HTTP: docs/api-contract.md + server/tests/test_api.py. Sem spec neste contexto. Sem plano ativo.

## Como usar esta documentacao

Segue [docs/sdd/README.md](../sdd/README.md).

1. [matriz](../README.md) e este README
2. [api-contract.md](../api-contract.md)
3. server/main.py, llm.py, shaping.py, config.py

## Estado atual

Contrato /v1/estimate + /v1/fit no ar. Sem /v1/chat. Sem digest. Timeout 20s.

## Indice

### Especificacoes

Nenhuma spec neste contexto. Fonte = [api-contract.md](../api-contract.md).

### ADRs

Nenhum ADR local. Historico: [001](../decisions/001-monorepo.md), [007](../decisions/007-english-identifiers.md).

### Planos e validacao

Nenhum plano criado ate o momento.
