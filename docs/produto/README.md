# produto

Este README roteia. Estado, versão e data ficam no arquivo dono.

## Proposito

Comportamento visivel do Dieta Bot: job, telas, copy, onboarding, slots, o que entra no prompt da IA.

## Tipo e ownership

- Tipo: produto.
- Codigo principal: nenhum. Implementacao mora em android e server.
- Consumidores: [android](../android/README.md), [server](../server/README.md).

## Escopo

- Job: encaixar a proxima refeicao no saldo do dia.
- Telas, copy, onboarding, chips/slots, memoria de produto.
- O que o modelo ve (perfil, memoria, snapshot do dia).

## Fora de escopo

- Schema Room, Compose, push — [android](../android/README.md).
- Rotas HTTP, LLM, host — [server](../server/README.md).
- Política de conteúdo (escopo, recusas, moderação) — [content-policy](../content-policy/README.md). UI de aceite legal ou de idade exige plano de produto próprio e gate Stitch, e está bloqueada pelo [production gate](../content-policy/production-gate.md).

## Fronteiras e dependencias

- Constituicao vigente: [`AGENTS.md`](../../AGENTS.md).
- Visual: [`docs/tokens.md`](../tokens.md) e os golds do Google Stitch ([inventário](../qa/README.md)).
- Nao duplicar contrato HTTP nem schema. Link.

## Como usar esta documentacao

Segue [docs/sdd/README.md](../sdd/README.md).

1. AGENTS.md
2. a spec do assunto (índice abaixo)
3. os ADRs que a spec cita
4. docs/tokens.md e o Design System Stitch (`.stitch/DESIGN.md`) para assuntos visuais

## Indice

### Especificacoes

- [chat](specifications/chat.md) — Chat: composer, registro, recibos, perguntas, memória visível.
- [home-timeline](specifications/home-timeline.md) — Home painel e timeline.
- [perfil-onboarding](specifications/perfil-onboarding.md) — splash e onboarding O1–O4.
- [memoria-push](specifications/memoria-push.md) — memória, foto, Config e push.

### ADRs

Status: a linha de status de cada ADR. Histórico em `docs/decisions/` (ver [matriz](../README.md)).

- [ADR-012](adrs/ADR-012-chat-home-perfil.md) — Chat tela, Home painel, perfil nomeado.
- [ADR-016](adrs/ADR-016-nome-dieta-bot.md) — nome "Dieta Bot".
- [ADR-017](adrs/ADR-017-registro-consolidado.md) — um registro por refeição.
- [ADR-019](adrs/ADR-019-ferramentas-dev.md) — telas de ferramenta só no dev.
- [ADR-020](adrs/ADR-020-estados-novos-chat-home-horario.md) — golds novos `chatA`, `homeW`, `o3t`.
- [ADR-021](adrs/ADR-021-refeicoes-por-dia.md) — refeições por dia da semana.
- [ADR-022](adrs/ADR-022-limite-texto-chat.md) — mensagem do Chat até 2000 caracteres, estado de erro.
- [ADR-023](adrs/ADR-023-chat-v2-memoria-v2.md) — Chat v2 (intenção, texto da refeição, plano) e Memória v2 (permanente + dinâmica).
- [ADR-026](adrs/ADR-026-perguntas-antes-da-estimativa.md) — perguntas antes da estimativa, até 3 rodadas, Forçar estimativa.
- [ADR-028](adrs/ADR-028-registro-autonomo.md) — registro autônomo no Chat com recibo reversível.
- [ADR-029](adrs/ADR-029-fatos-temporarios-compactacao.md) — fatos temporários (3 dias), compactação que mantém o fim aberto, dia da refeição.
- [ADR-032](adrs/ADR-032-acrescimos-e-correcoes-de-refeicoes.md) — explicit meal additions, revisions and destination semantics.

### Planos

This context has no `plans/`. Deliveries belong to [Android](../android/README.md), [server](../server/README.md) and [design](../design/README.md); frozen Stitch history is routed by its [README](../stitch/README.md).

Meal updates: [D9](../design/plans/d9-chat-meal-updates.md) and [A47](../android/plans/a47-chat-meal-updates.md).
