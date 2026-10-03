# server

## Proposito

API HTTP do Dieta Bot. Estima refeicao e devolve prato que cabe. Nao calcula teto. Nao guarda o dia.

## Tipo e ownership

- Tipo: contrato HTTP.
- Codigo principal: `server/`.
- Consumidor: [android](../android/README.md).
- Infra indexada aqui: `server/docker-compose.yml`, `infra/`, `infra/gcp/`, [deploy-gcp.md](deploy-gcp.md).

## Escopo

- GET /health, POST /v1/estimate, POST /v1/fit, POST /v1/chat.
- Auth X-Invite.
- LLM gpt-6-luna, reasoning.effort=none (`config.REASONING_EFFORT`; escolha feita pelo [avaliador](#avaliacao-do-chat)).
- Foto entra no request e some. Nao persiste.
- Cap 16 MB JPEG (`PHOTO_MAX_B64_CHARS = 22_400_000`). Acima disso HTTP 413 `photo_too_large` antes da Luna.
- Shaping do JSON de contrato.
- `X-Request-Id` em toda resposta. Log de conversa opcional (`CONVERSATION_LOG_PATH`), ligado so no server GCP de dev ([ADR-015](adrs/ADR-015-log-conversa-dev.md)); leitura por `tools/pull-conversations.ps1`.
- Host: VM e2-micro GCP + Caddy HTTPS ([ADR-013](adrs/ADR-013-gcp-host.md), runbook [deploy-gcp.md](deploy-gcp.md)). `infra/gcp/`, `tools/deploy-gcp.ps1`. Servidor de produção: bloqueado pelo [production gate](../content-policy/production-gate.md).

## Fora de escopo

- Teto, eat-back, Room, push, UI — [android](../android/README.md) / [produto](../produto/README.md).
- Conta de user, Stripe, persistencia do dia no server.
- Outro host alem do ADR-013 (VPS, dominio proprio, Cloud Run).

## Fronteiras e dependencias

- Contrato vivo hoje: [api-contract.md](../api-contract.md). Spec de chat: [v1-chat.md](specifications/v1-chat.md).
- Timeout vigente: 60s em server/config.py (`TIMEOUT_SECONDS`). Keep-alive do uvicorn precisa ser >=60s.
- Cap foto: `PHOTO_MAX_BYTES` 16 MB / `PHOTO_MAX_B64_CHARS` 22_400_000.
- ADRs de repo: [001](../decisions/001-monorepo.md), [007](../decisions/007-english-identifiers.md).
- Controles de conteúdo e correlação de segurança: política em [content-policy](../content-policy/README.md); regras do server em [v1-chat](specifications/v1-chat.md) (regras 12–16) e no [contrato](../api-contract.md).

## Cobertura documental atual

Fonte HTTP: [api-contract.md](../api-contract.md), [v1-chat.md](specifications/v1-chat.md), `server/tests/test_api.py`, `server/tests/test_photo_cap.py`, `server/tests/test_security.py`, `server/tests/test_chat.py`, `server/tests/test_conversation_log.py`, `server/tests/test_evals.py`, `server/tests/test_clarify.py`, `server/tests/test_record.py`.

## Como usar esta documentacao

Segue [docs/sdd/README.md](../sdd/README.md).

1. [matriz](../README.md) e este README
2. [api-contract.md](../api-contract.md)
3. [v1-chat.md](specifications/v1-chat.md)
4. server/main.py, llm.py, shaping.py, config.py

## Avaliacao do Chat

Roda na maquina do dono, nunca no server. Chave `OPENAI_API_KEY` do `.env` da raiz; nunca impressa.

```bash
cd server
.venv/Scripts/python -m evals.run --effort none low --repeat 3
.venv/Scripts/python -m evals.run --effort none --repeat 1 --only cafe-resposta-leite,cabe-acai
.venv/Scripts/python -m evals.run --tag memory
```

- Casos: `server/evals/cases/<id>.json` (`id`, `since` v1/v2/v3/v4/cp2, `tags`, `request` = corpo `ChatIn`, `expect`). Expectativas em `server/evals/checks.py`; campo v2 ausente na saida = `n/a`.
- Mesmo codigo da rota (`_chat_text`, `LlmClient.chat_json`, `shape_chat_turn` = `shape_chat` + gate do S13 + gate do S14), sem HTTP. Casos v3 levam `clarify_rounds` no request; casos v4 levam tambem `auto_record: true`, e `record` (valor ou lista de aceitos) e `skip_slot` olham a marca (`n/a` sem ela); `top_question` (`present` | `absent` | lista de termos) e `top_question_not` olham a `question` do topo (`n/a` sem ela). No maximo 3 chamadas simultaneas.
- Aprovado: todas as expectativas aplicaveis em pelo menos 2 de 3 repeticoes.
- Relatorio no terminal e em `logs/evals/<data-hora>-<effort>.json` (fora do git).
- Caso novo: situacao do log (`./tools/pull-conversations.ps1 -Download`), texto reescrito a mao. Log bruto e texto de tester nunca entram no git.

## Indice

### Especificacoes

- [v1-chat.md](specifications/v1-chat.md) — regras da rota `/v1/chat`.
- Contrato HTTP: [api-contract.md](../api-contract.md).

### ADRs

Status: a linha de status de cada ADR.

- [ADR-013](adrs/ADR-013-gcp-host.md) — host GCP e2-micro.
- [ADR-015](adrs/ADR-015-log-conversa-dev.md) — log de conversa no server de dev.
- Historico: [001](../decisions/001-monorepo.md), [007](../decisions/007-english-identifiers.md).

### Planos e validacao

- Ativos: arquivos na raiz de [`plans/`](plans/).
- Histórico (com os números de avaliação de cada entrega): [`plans/completed/`](plans/completed/).
- Testes: `server/tests/test_api.py`, `server/tests/test_photo_cap.py`, `server/tests/test_security.py`, `server/tests/test_chat.py`, `server/tests/test_conversation_log.py`, `server/tests/test_evals.py`, `server/tests/test_clarify.py`, `server/tests/test_record.py`.
