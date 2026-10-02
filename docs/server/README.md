# server

## Content-policy work

[Content policy](../content-policy/README.md) owns the policy. [CP2](../content-policy/plans/completed/cp2-server-content-controls.md) is deployed to dev (2026-09-30, `Concluído`): scope field, fixed refusals, moderation of input and output, fail closed ([ADR-024](../content-policy/adrs/ADR-024-content-safety-boundaries.md), accepted). [CP3](../content-policy/plans/completed/cp3-server-safety-identifier.md) is deployed to dev (2026-10-01): optional `X-Client-Instance-Id`, HMAC `safety_identifier` ([ADR-025](../content-policy/adrs/ADR-025-safety-correlation-audit.md), accepted), `/health` `safety_id: on` since [CP5](../content-policy/plans/completed/cp5-gcp-dev-ingress.md) provisioned the secret, narrowed forwarded-header trust and bounded the logs (2026-10-01; OpenAI budget US$ 10, alert only). In the closed test the ADR-015 dev log stays, with metadata-only records for blocked turns; its retirement in production belongs to [CP9](../content-policy/plans/out_of_scope/cp9-production-audit-and-containment.md). End-to-end correlation verified on 2026-10-01 with the dev 0.0.6 APK on an emulator (CP5). Production server work is subject to the [production gate](../content-policy/production-gate.md).

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

Fonte HTTP: [api-contract.md](../api-contract.md), [v1-chat.md](specifications/v1-chat.md), `server/tests/test_api.py`, `server/tests/test_photo_cap.py`, `server/tests/test_security.py`, `server/tests/test_chat.py`, `server/tests/test_conversation_log.py`, `server/tests/test_evals.py`, `server/tests/test_clarify.py`, `server/tests/test_record.py`.
[S1](plans/completed/s1-timeout-photo-cap.md), [S4](plans/completed/s4-security-hardening.md), [S2](plans/completed/s2-v1-chat.md), [S3](plans/completed/s3-compact.md) e [S5](plans/completed/s5-gcp-deploy.md) e [S10](plans/completed/s10-avaliacao-chat.md) concluidos. [S8](plans/completed/s8-chat-json-slot-consolidado.md), [S11](plans/completed/s11-chat-v2.md) e [S12](plans/completed/s12-slot-nomeado.md) aprovados pelo dono em 30/09/2026. [S13](plans/completed/s13-perguntas-antes-da-estimativa.md) e [S14](plans/completed/s14-registro-autonomo.md) concluidos.

## Como usar esta documentacao

Segue [docs/sdd/README.md](../sdd/README.md).

1. [matriz](../README.md) e este README
2. [api-contract.md](../api-contract.md)
3. [v1-chat.md](specifications/v1-chat.md)
4. server/main.py, llm.py, shaping.py, config.py

## Estado atual

Contrato /v1/estimate, /v1/fit e /v1/chat no ar. Timeout 60s. `/v1/chat` pede saida estruturada (`json_schema` strict, `suggested_slot` com enum dos ids do perfil) e texto sem JSON vira `reply` ([S8](plans/completed/s8-chat-json-slot-consolidado.md)). Corpo ate 24 MB (`MAX_BODY_BYTES`). `text` e `messages[].text` do `/v1/chat` ate 2000 caracteres ([S9](plans/completed/s9-limite-texto-2000.md)). Cap 16 MB JPEG; `image_b64` acima de 22_400_000 chars → HTTP 413 `photo_too_large` antes da Luna. Chat v2 ([S11](plans/completed/s11-chat-v2.md), ADR-023): `intent`, `meal_text`, memoria em fatos (`facts`), `recent`, `remaining_kcal`, `memory_updates`, `memory_used`; cliente sem `facts` = legado (plano sem card). `compact=true` devolve digest (S3). O client usa o compact desde o [A5b](../android/plans/completed/a5b-ligar-compact.md). Perguntas antes da estimativa ([S13](plans/completed/s13-perguntas-antes-da-estimativa.md), ADR-026): com `clarify_rounds` (cliente v3), turno `log` com duvida devolve so `question` (sem estimate); o gate `shaping.clarify_gate` libera a estimativa com 3 rodadas, pergunta repetida ou `force_estimate`; log de conversa ganha `clarify` e `clarify_rounds`. Marca de registro ([S14](plans/completed/s14-registro-autonomo.md), ADR-028): com `clarify_rounds` + `auto_record: true` (cliente v4), o gate `shaping.record_gate` poe `record` (`auto` | `ask` | `none`) e `skip_slot` no topo, e `intent` pode ser `skip`; o server continua sem gravar; log ganha `record`, `record_intent` e `meal_day`.

Keep-alive do uvicorn precisa ser >=60s. Docker nao muda neste plano.

## Avaliacao do Chat (S10)

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

- [v1-chat.md](specifications/v1-chat.md) — timeout/cap vigentes; rota `/v1/chat` no ar.
- Contrato HTTP: [api-contract.md](../api-contract.md).

### ADRs

- [ADR-013](adrs/ADR-013-gcp-host.md) — host GCP e2-micro (aceito).
- [ADR-015](adrs/ADR-015-log-conversa-dev.md) — log de conversa no server de dev (aceito; parcialmente substituído pelo [ADR-024](../content-policy/adrs/ADR-024-content-safety-boundaries.md): turno bloqueado só com metadados).
- Historico: [001](../decisions/001-monorepo.md), [007](../decisions/007-english-identifiers.md).

### Planos e validacao

- [S15 aguardando aprovacao](plans/s15-registro-casos-dificeis.md) — "ainda não almocei" nao e `skip`; foto com pergunta = `record: ask`; 23 casos `record-hard` no avaliador; effort `none` mantido pela comparacao `none` x `low` de 02/10/2026 (98,9% x 95,5%, p95 4,2 s x 5,9 s).
- [S14 concluido](plans/completed/s14-registro-autonomo.md) — marca `record` (`auto` | `ask` | `none`), intencao `skip`, `record_intent` e `meal_day` para o cliente v4 (`auto_record`) ([ADR-028](../produto/adrs/ADR-028-registro-autonomo.md), proposto); opt-in, cliente legado e v3 byte a byte inalterados; avaliador 66/68 (16/16 `record`); no ar no dev em 02/10/2026; libera o [A34](../android/plans/a34-registro-autonomo.md).
- [S13 concluido](plans/completed/s13-perguntas-antes-da-estimativa.md) — perguntas antes da estimativa (`clarify_rounds`, `force_estimate`, `question` no topo), trava de 3 rodadas no codigo ([ADR-026](../produto/adrs/ADR-026-perguntas-antes-da-estimativa.md)); opt-in, cliente legado inalterado; avaliador 35/35; no ar no dev em 30/09/2026.
- [S10 concluido](plans/completed/s10-avaliacao-chat.md) — avaliador do Chat com casos reais (`server/evals/`); linha de base `none` 52,4% (11/21) ([ADR-023](../produto/adrs/ADR-023-chat-v2-memoria-v2.md)).
- [S12 concluido](plans/completed/s12-slot-nomeado.md) — slot da refeicao: o nome dito pelo usuario vence a semelhanca com uma refeicao ja gravada; avaliador 29/29 (7 casos `slot` 5/5); no ar no dev; aprovado pelo dono em 30/09/2026.
- [S11 concluido](plans/completed/s11-chat-v2.md) — Chat v2: intencao, `meal_text`, memoria em fatos, `recent`, `remaining_kcal`; avaliador 22/22, effort `none` mantido; no ar no dev; aprovado pelo dono em 30/09/2026.
- [S9 concluido](plans/completed/s9-limite-texto-2000.md) — `/v1/chat` aceita `text` e `messages[].text` ate 2000 caracteres ([ADR-022](../produto/adrs/ADR-022-limite-texto-chat.md)); no ar no dev.
- [S8 concluido](plans/completed/s8-chat-json-slot-consolidado.md) — saida estruturada (`json_schema` strict), slot sugerido, refeicao consolidada, total sem comida sem estimate, corpo 24 MB; no ar no dev; aprovado pelo dono em 30/09/2026.
- [S7 concluido](plans/completed/s7-rename-prompt.md) — "Dieta Bot" no prompt do Chat.
- [S6 concluido](plans/completed/s6-log-conversa-dev.md) — log de conversa no server de dev + `X-Request-Id`; o "nao deu" ja foi explicado pelo log (29/09); aprovado pelo dono em 30/09/2026.
- [S5 concluido](plans/completed/s5-gcp-deploy.md) — deploy GCP e2-micro + Caddy.
- [S1 concluido](plans/completed/s1-timeout-photo-cap.md) — timeout 60s + cap 16 MB.
- [S4 concluido](plans/completed/s4-security-hardening.md) — hardening de seguranca (rate limiting, constant time auth, payload limit).
- [S2 concluido](plans/completed/s2-v1-chat.md) — POST /v1/chat.
- [S3 concluido](plans/completed/s3-compact.md) — compact digest.
- Testes: `server/tests/test_api.py`, `server/tests/test_photo_cap.py`, `server/tests/test_security.py`, `server/tests/test_chat.py`, `server/tests/test_conversation_log.py`, `server/tests/test_evals.py`, `server/tests/test_clarify.py`, `server/tests/test_record.py`.
