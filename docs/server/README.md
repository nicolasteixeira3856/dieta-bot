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
- LLM gpt-6-luna, reasoning.effort=none (`config.REASONING_EFFORT`; mantido pelo avaliador no S11).
- Foto entra no request e some. Nao persiste.
- Cap 16 MB JPEG (`PHOTO_MAX_B64_CHARS = 22_400_000`). Acima disso HTTP 413 `photo_too_large` antes da Luna.
- Shaping do JSON de contrato.
- Compose + Cloudflare Tunnel na torre (dev). Sem porta no roteador.
- `X-Request-Id` em toda resposta. Log de conversa opcional (`CONVERSATION_LOG_PATH`), ligado so no server GCP de dev ([ADR-015](adrs/ADR-015-log-conversa-dev.md)); leitura por `tools/pull-conversations.ps1`.
- Producao: VM e2-micro GCP + Caddy HTTPS ([ADR-013](adrs/ADR-013-gcp-host.md), runbook [deploy-gcp.md](deploy-gcp.md)). `infra/gcp/`, `tools/deploy-gcp.ps1`.

## Fora de escopo

- Teto, eat-back, Room, push, UI — [android](../android/README.md) / [produto](../produto/README.md).
- Conta de user, Stripe, persistencia do dia no server.
- Outro host alem do ADR-013 (VPS, dominio proprio, Cloud Run).

## Fronteiras e dependencias

- Contrato vivo hoje: [api-contract.md](../api-contract.md). Spec de chat: [v1-chat.md](specifications/v1-chat.md).
- Timeout vigente: 60s em server/config.py (`TIMEOUT_SECONDS`). Keep-alive do uvicorn precisa ser >=60s. Docker nao muda neste plano.
- Cap foto: `PHOTO_MAX_BYTES` 16 MB / `PHOTO_MAX_B64_CHARS` 22_400_000.
- ADRs de repo: [001](../decisions/001-monorepo.md), [007](../decisions/007-english-identifiers.md).

## Cobertura documental atual

Fonte HTTP: [api-contract.md](../api-contract.md), [v1-chat.md](specifications/v1-chat.md), `server/tests/test_api.py`, `server/tests/test_photo_cap.py`, `server/tests/test_security.py`, `server/tests/test_chat.py`, `server/tests/test_conversation_log.py`, `server/tests/test_evals.py`.
[S1](plans/completed/s1-timeout-photo-cap.md), [S4](plans/completed/s4-security-hardening.md), [S2](plans/completed/s2-v1-chat.md), [S3](plans/completed/s3-compact.md) e [S5](plans/completed/s5-gcp-deploy.md) e [S10](plans/completed/s10-avaliacao-chat.md) concluidos. [S8](plans/pending_manual_validation/s8-chat-json-slot-consolidado.md), [S11](plans/pending_manual_validation/s11-chat-v2.md) e [S12](plans/pending_manual_validation/s12-slot-nomeado.md) pendentes aprovacao manual.

## Como usar esta documentacao

Segue [docs/sdd/README.md](../sdd/README.md).

1. [matriz](../README.md) e este README
2. [api-contract.md](../api-contract.md)
3. [v1-chat.md](specifications/v1-chat.md)
4. server/main.py, llm.py, shaping.py, config.py

## Estado atual

Contrato /v1/estimate, /v1/fit e /v1/chat no ar. Timeout 60s. `/v1/chat` pede saida estruturada (`json_schema` strict, `suggested_slot` com enum dos ids do perfil) e texto sem JSON vira `reply` ([S8](plans/pending_manual_validation/s8-chat-json-slot-consolidado.md)). Corpo ate 24 MB (`MAX_BODY_BYTES`). `text` e `messages[].text` do `/v1/chat` ate 2000 caracteres ([S9](plans/completed/s9-limite-texto-2000.md)). Cap 16 MB JPEG; `image_b64` acima de 22_400_000 chars → HTTP 413 `photo_too_large` antes da Luna. Chat v2 ([S11](plans/pending_manual_validation/s11-chat-v2.md), ADR-023): `intent`, `meal_text`, memoria em fatos (`facts`), `recent`, `remaining_kcal`, `memory_updates`, `memory_used`; cliente sem `facts` = legado (plano sem card). `compact=true` devolve digest (S3). O client usa o compact desde o [A5b](../android/plans/completed/a5b-ligar-compact.md).

Keep-alive do uvicorn precisa ser >=60s. Docker nao muda neste plano.

## Avaliacao do Chat (S10)

Roda na maquina do dono, nunca no server. Chave `OPENAI_API_KEY` do `.env` da raiz; nunca impressa.

```bash
cd server
.venv/Scripts/python -m evals.run --effort none low --repeat 3
.venv/Scripts/python -m evals.run --effort none --repeat 1 --only cafe-resposta-leite,cabe-acai
.venv/Scripts/python -m evals.run --tag memory
```

- Casos: `server/evals/cases/<id>.json` (`id`, `since` v1/v2, `tags`, `request` = corpo `ChatIn`, `expect`). Expectativas em `server/evals/checks.py`; campo v2 ausente na saida = `n/a`.
- Mesmo codigo da rota (`_chat_text`, `LlmClient.chat_json`, `shape_chat`), sem HTTP. No maximo 3 chamadas simultaneas.
- Aprovado: todas as expectativas aplicaveis em pelo menos 2 de 3 repeticoes.
- Relatorio no terminal e em `logs/evals/<data-hora>-<effort>.json` (fora do git).
- Caso novo: situacao do log (`./tools/pull-conversations.ps1 -Download`), texto reescrito a mao. Log bruto e texto de tester nunca entram no git.

## Indice

### Especificacoes

- [v1-chat.md](specifications/v1-chat.md) — timeout/cap vigentes; rota `/v1/chat` no ar.
- Contrato HTTP: [api-contract.md](../api-contract.md).

### ADRs

- [ADR-013](adrs/ADR-013-gcp-host.md) — host GCP e2-micro (aceito).
- [ADR-015](adrs/ADR-015-log-conversa-dev.md) — log de conversa no server de dev (aceito).
- Historico: [001](../decisions/001-monorepo.md), [007](../decisions/007-english-identifiers.md).

### Planos e validacao

- [S10 concluido](plans/completed/s10-avaliacao-chat.md) — avaliador do Chat com casos reais (`server/evals/`); linha de base `none` 52,4% (11/21) ([ADR-023](../produto/adrs/ADR-023-chat-v2-memoria-v2.md)).
- [S12 pendente aprovacao manual](plans/pending_manual_validation/s12-slot-nomeado.md) — slot da refeicao: o nome dito pelo usuario vence a semelhanca com uma refeicao ja gravada; avaliador 29/29 (7 casos `slot` 5/5); no ar no dev; falta o dono repetir o "cafe igual ao de ontem" no APK.
- [S11 pendente aprovacao manual](plans/pending_manual_validation/s11-chat-v2.md) — Chat v2: intencao, `meal_text`, memoria em fatos, `recent`, `remaining_kcal`; avaliador 22/22, effort `none` mantido; no ar no dev; falta o dono ver o plano sem card no APK 0.0.3.
- [S9 concluido](plans/completed/s9-limite-texto-2000.md) — `/v1/chat` aceita `text` e `messages[].text` ate 2000 caracteres ([ADR-022](../produto/adrs/ADR-022-limite-texto-chat.md)); no ar no dev.
- [S8 pendente aprovacao manual](plans/pending_manual_validation/s8-chat-json-slot-consolidado.md) — saida estruturada (`json_schema` strict), slot sugerido, refeicao consolidada, total sem comida sem estimate, corpo 24 MB; no ar no dev; falta o dono ver "Gravar cafe" no APK 0.0.2.
- [S7 concluido](plans/completed/s7-rename-prompt.md) — "Dieta Bot" no prompt do Chat.
- [S6 pendente aprovacao manual](plans/pending_manual_validation/s6-log-conversa-dev.md) — log de conversa no server de dev + `X-Request-Id`; o "nao deu" ja foi explicado pelo log (29/09); falta o dono aprovar.
- [S5 concluido](plans/completed/s5-gcp-deploy.md) — deploy GCP e2-micro + Caddy.
- [S1 concluido](plans/completed/s1-timeout-photo-cap.md) — timeout 60s + cap 16 MB.
- [S4 concluido](plans/completed/s4-security-hardening.md) — hardening de seguranca (rate limiting, constant time auth, payload limit).
- [S2 concluido](plans/completed/s2-v1-chat.md) — POST /v1/chat.
- [S3 concluido](plans/completed/s3-compact.md) — compact digest.
- Testes: `server/tests/test_api.py`, `server/tests/test_photo_cap.py`, `server/tests/test_security.py`, `server/tests/test_chat.py`, `server/tests/test_conversation_log.py`.
