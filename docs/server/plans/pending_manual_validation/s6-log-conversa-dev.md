# Plano — S6 Log de conversa no server de dev

- Estado: Pendente aprovação manual (passo 8.4: dono reproduz o "nao deu para estimar")
- Data: 28/09/2026
- Contexto proprietário: `server`
- Código afetado: `server/` (`config.py`, `main.py`, `llm.py`, módulo novo `conversation_log.py`, testes), `infra/gcp/compose.yml`, `tools/pull-conversations.ps1` (novo), `.gitignore`, docs.
- Pré-requisitos: Nenhum. É independente do A9/A10/A11 e pode rodar antes deles. [ADR-015](../../adrs/ADR-015-log-conversa-dev.md) aceito junto com a aprovação.

## Gate de autorização

Este plano é exclusivamente documental. A implementação só começa após aprovação explícita que identifique este arquivo:

> Aprovo o plano `docs/server/plans/s6-log-conversa-dev.md`. Implemente o plano aprovado.

A aprovação aceita o ADR-015. Se a implementação revelar decisão não coberta, pare, atualize os artefatos e peça nova aprovação.

## Objetivo

Cada chamada ao LLM no server de dev deixa uma linha JSON com entrada, saída crua do modelo, erro e resposta final. Assim o agente consegue explicar casos como o "nao deu pra estimar" que o dono recebeu.

## Fontes de verdade

- [ADR-015](../../adrs/ADR-015-log-conversa-dev.md), [api-contract.md](../../../api-contract.md), [v1-chat.md](../../specifications/v1-chat.md).
- Código: `server/main.py` (rotas e `_configure_logging`), `server/llm.py` (`_complete`, `_parse_json_object`), `server/shaping.py` (`shape_chat`, `fail_chat`).

## Diagnóstico que motiva o plano

"nao deu pra estimar" sai do server em três caminhos: `reply` ausente ou vazio (`shape_chat`), `_parse_json_object` falhando (texto sem JSON) ou exceção na chamada (timeout, erro da OpenAI) → `fail_chat`. Hoje só `type(exc).__name__` vai para o log, e a saída crua do modelo se perde.

## Escopo de implementação

### 1. Config (`server/config.py`)

- `CONVERSATION_LOG_PATH` do ambiente (default `""` = desligado). `CONVERSATION_LOG_MAX_BYTES = 20 MB`, `CONVERSATION_LOG_BACKUPS = 5`.

### 2. Captura da saída crua (`server/llm.py`)

- `_complete` e os métodos públicos recebem um `trace: dict | None = None` opcional e preenchem `trace["instructions"]` (nome da instrução: `estimate|fit|chat|digest`), `trace["input_text"]` e `trace["raw_output"]` (o `response.output_text`, antes do parse).
- A foto nunca entra no trace: só `has_photo` e `photo_b64_chars`.
- O comportamento e o retorno atuais não mudam.

### 3. Request id e registro (`server/main.py` + `server/conversation_log.py`)

- Middleware: lê `X-Request-Id` (válido se `^[A-Za-z0-9-]{1,64}$`), senão gera `uuid4`. Guarda em `request.state` e devolve no header da resposta, em **todas** as rotas.
- `conversation_log.py`: `ConversationLog(path)` com `RotatingFileHandler` próprio (logger isolado, `propagate=False`), escrevendo `json.dumps(record, ensure_ascii=False)`. Com `path` vazio, vira no-op.
- Nas quatro rotas LLM: um `record` com `ts` (America/Sao_Paulo, ISO), `request_id`, `route`, `app_version` e `app_env` (headers opcionais `X-App-Version`/`X-App-Env`, só registrados), `input_text`, `has_photo`, `photo_b64_chars`, `raw_output`, `error` (`{type, message}` com a chave redigida), `response` (o dict devolvido), `fallback` (true quando saiu `fail_*` ou `reply == "nao deu pra estimar"`), `latency_ms`.
- Escrita em `finally`. Uma falha ao escrever o log **nunca** muda a resposta: vira um `warning` e segue.
- `INVITE_CODE` e `X-Invite` nunca entram no record.

### 4. Testes (`server/tests/test_conversation_log.py`)

1. Desligado: nenhuma escrita; resposta idêntica à de hoje.
2. Ligado + sucesso: 1 linha com `raw_output`, `response`, `fallback=false`.
3. Ligado + saída sem JSON: `raw_output` preenchido, `error.type="ValueError"`, `fallback=true`, resposta `nao deu pra estimar`.
4. Ligado + exceção de transporte: `error` preenchido, `raw_output=null`, `fallback=true`.
5. Foto: `has_photo=true`, `photo_b64_chars` > 0 e nenhum trecho do base64 no arquivo.
6. `X-Request-Id` enviado → mesmo id no header de resposta e no log; id inválido/ausente → UUID gerado.
7. O invite e a chave falsa dos testes não aparecem no arquivo.
8. Um erro de I/O no log (path num diretório inexistente) não quebra a rota.

### 5. Infra dev (`infra/gcp/compose.yml`)

- `api.environment.CONVERSATION_LOG_PATH=/data/conversations.jsonl` e o volume `/opt/nutri/logs:/data`. O `startup.sh` passa a criar `/opt/nutri/logs`.
- `server/docker-compose.yml` (torre) não muda: o log continua desligado.

### 6. Leitura (`tools/pull-conversations.ps1`)

- `-Tail N` (padrão 20): imprime as últimas N linhas via `gcloud compute ssh --tunnel-through-iap`.
- `-Download`: copia os `conversations.jsonl*` para `logs/` na raiz (nova entrada `/logs/` no `.gitignore`).
- `-RequestId <id>`: filtra uma chamada.

### 7. Docs

- `docs/api-contract.md`: header opcional `X-Request-Id` (request e response) e `X-App-Version`/`X-App-Env` opcionais. O JSON não muda.
- `docs/server/README.md`, `docs/server/deploy-gcp.md` (seção "Log de conversa").

### 8. Deploy e diagnóstico (AGENTE)

1. `pytest` verde localmente.
2. `./tools/deploy-gcp.ps1`.
3. Uma chamada real de `/v1/chat` com `X-Request-Id: s6-smoke` → `pull-conversations.ps1 -RequestId s6-smoke` mostra a linha.
4. **[DONO]** Repetir no app o pedido que deu "nao deu para estimar". **[AGENTE]** Ler as últimas linhas, explicar a causa e registrar neste plano. Uma correção de prompt ou shaping que saia daí vira plano próprio.

## Arquivos e áreas afetadas

- `server/config.py`, `server/llm.py`, `server/main.py`, `server/conversation_log.py` (novo), `server/tests/test_conversation_log.py` (novo).
- `infra/gcp/compose.yml`, `infra/gcp/startup.sh`, `tools/pull-conversations.ps1` (novo), `.gitignore`.
- `docs/api-contract.md`, `docs/server/README.md`, `docs/server/deploy-gcp.md`.

## Validação planejada

1. `server/.venv` `pytest -q`: todos verdes, incluindo os 8 novos.
2. Deploy + smoke do passo 8.3.
3. `curl -i /health` mostra `X-Request-Id`.
4. Na VM: `ls -l /opt/nutri/logs` e um `grep` pelo convite no arquivo com zero ocorrências (o valor é lido do `.env` da VM dentro do comando, sem imprimir).
5. Manual: passo 8.4.

## Fora de escopo

- Qualquer mudança no JSON do contrato, no prompt ou no shaping.
- Envio do `X-Request-Id` pelo app (vem no [A11](../../../android/plans/pending_manual_validation/a11-firebase-dev.md)); até lá, o server gera o id.
- Cloud Logging, Firestore, retenção por tempo, dashboard.

## Riscos e controles

- **Dado pessoal em disco:** só na VM dev, rotação de ~100 MB, `.env` e logs só acessíveis por root/containers. Aceito pelo dono (ADR-015).
- **Foto vazar para o log:** o trace nunca recebe a imagem; o teste 5 garante.
- **Log quebrar a API:** escrita em `try/except` + teste 8.
- **Mudança acidental de comportamento:** a suíte existente (43 testes) precisa continuar verde sem ajuste.

## Critérios de aceite

- Log ligado só no server GCP dev, uma linha por chamada LLM, com a saída crua.
- `X-Request-Id` em toda resposta.
- O caso "nao deu para estimar" do dono explicado a partir do log.

## Registro de execução

- 28/09/2026 — ADR-015 aceito com a aprovação.
- `config.py`: `CONVERSATION_LOG_PATH`, `CONVERSATION_LOG_MAX_BYTES`, `CONVERSATION_LOG_BACKUPS`.
- `conversation_log.py` (novo): `RotatingFileHandler` com `delay=True` num logger isolado; `handleError` vira `warning` (não imprime traceback); redação do convite e da chave na linha serializada; timestamp com offset fixo `-03:00` (sem depender de `tzdata`).
- `llm.py`: `trace` opcional preenche `prompt` (`estimate|fit|chat|digest`), `input_text` e `raw_output` antes do parse.
- `main.py`: middleware `X-Request-Id` registrado por último (o mais externo, então vale para 401/413/429); helper `_llm_call` unifica as quatro rotas (a mesma mensagem de `warning` de antes, o mesmo fallback). O 413 de foto não chama o modelo e não gera linha.
- `shaping.py`: constante `CHAT_FALLBACK_REPLY` (mesmo texto; só evita o literal duplicado).
- `infra/gcp/compose.yml`: env + volume `/opt/nutri/logs:/data`; `startup.sh` e `deploy-gcp.ps1` criam `/opt/nutri/logs` com `chmod 700`; metadata `startup-script` da VM atualizado pelo MCP.
- `tools/pull-conversations.ps1`: `-Tail`, `-RequestId`, `-Download`. Ajuste: o PS 5.1 perde aspas duplas ao chamar programa nativo, então o filtro por id é um `grep -F` simples na VM mais a comparação exata do `request_id` local.
- `.gitignore`: `/logs/`.

### Resultados da validação

1. `pytest -q` → **53 passed**, 19 subtests (43 antigos sem ajuste + 10 novos em `test_conversation_log.py`: desligado, sucesso, saída sem JSON, `reply` vazio, erro de transporte, foto, request id, redação, estimate/fit/compact, path inválido). ✅
2. `tools/deploy-gcp.ps1` → `/health` 200. ✅
3. `/health` devolve `X-Request-Id` (UUID). `POST /v1/chat` com `X-Request-Id: s6-smoke` → 200, mesmo id no header; `pull-conversations.ps1 -RequestId s6-smoke` → `route=chat`, `prompt=chat`, `app_env=smoke`, `fallback=false`, `raw_output` preenchido, 4397 ms. ✅
4. VM: `/opt/nutri/logs` = `drwx------ root`; `grep -c` do convite (lido do `.env` da VM) no log = **0**. `-Download` copia para `logs/`, ignorado pelo git. ✅
5. Manual (passo 8.4): pendente. ⏳

## Encerramento

Depois da implementação, registre resultados reais e aplique o ciclo de vida em `docs/sdd/README.md`.

Só declaração explícita do dono cancelando este plano permite `Cancelado` e a pasta `plans/cancelled/`.
