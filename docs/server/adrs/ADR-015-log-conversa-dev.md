# ADR-015 — Log de conversa no server de dev

- Estado: Aceito (aprovação do plano S6, 2026-09-28)
- Data: 2026-09-28
- Contexto: `server`
- Substitui: Não se aplica. Complementa [ADR-013](ADR-013-gcp-host.md).

## Contexto

O dono recebeu "nao deu pra estimar" num pedido simples. Esse texto é o fallback do server (`shape_chat`/`fail_chat`): `reply` vazio, JSON inválido do modelo ou exceção (timeout, erro da OpenAI). Hoje o server só loga o **nome** da exceção. Não há como saber o que o modelo respondeu nem o prompt montado. O server GCP é ambiente de dev ([ADR-013](ADR-013-gcp-host.md)).

## Decisão

- O server ganha um log de conversa **opcional**, ligado por `CONVERSATION_LOG_PATH`. Vazio = desligado (default, e o que os testes e a torre usam).
- Uma linha JSON por chamada a `/v1/estimate`, `/v1/fit` e `/v1/chat` (incluindo `compact`), com: horário (America/Sao_Paulo), `request_id`, rota, versão/ambiente do app (headers opcionais), texto de entrada enviado ao LLM, presença e tamanho da foto, saída crua do modelo, erro de parse/exceção (tipo + mensagem), resposta final, flag `fallback` e latência.
- **Nunca** entram no log: a imagem, o `INVITE_CODE` e a `OPENAI_API_KEY` (o filtro de redação existente continua valendo).
- `X-Request-Id`: se o client mandar (≤ 64 chars `[A-Za-z0-9-]`), o server reusa; senão gera um UUID. Sempre devolve no header da resposta. O JSON do contrato não muda.
- Rotação por tamanho: 20 MB × 5 arquivos.
- Só o server GCP de dev liga o log (via `infra/gcp/compose.yml`, volume `/opt/nutri/logs`). O agente lê por `tools/pull-conversations.ps1`.

## Motivação

- Só o server vê a saída crua do modelo e o prompt completo, que são exatamente o que explica um fallback.
- Ser opt-in por variável mantém a regra "foto entra e some" e deixa qualquer prod futuro desligado por padrão.

## Consequências

### Positivas

- Qualquer "nao deu" vira um caso analisável: o `request_id` do app (Crashlytics) aponta para a linha do log.

### Negativas

- Dados pessoais (perfil, memória, refeições) ficam em disco na VM de dev, com retenção de ~100 MB por rotação. É aceito pelo dono para dev.
- A escrita síncrona de uma linha por request é custo desprezível para 1 usuário.

## Alternativas consideradas

### Cloud Logging (stdout do container)

Exige o agente de logging na VM e consulta pelo console ou `gcloud logging`, além de ter custo e cota. Com 1 usuário, um JSONL local é mais simples de ler e baixar.

### Cópia no Firestore pelo app

Não tem a saída crua nem o prompt montado. Rejeitada ([ADR-014](../../android/adrs/ADR-014-flavors-firebase-dev.md)).

## Relações

- Plano: [S6](../plans/completed/s6-log-conversa-dev.md).
- Especificações afetadas: [api-contract.md](../../api-contract.md) (header opcional `X-Request-Id`).
- ADRs relacionados: [ADR-013](ADR-013-gcp-host.md), [ADR-014](../../android/adrs/ADR-014-flavors-firebase-dev.md).

Depois de aceito, este ADR não se edita. Mudança posterior exige ADR novo que declare a substituição.
