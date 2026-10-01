# produto

## Planned content-policy work

[Content policy](../content-policy/README.md) proposes scope refusals, safety handling and privacy/incident procedures. [ADR-024](../content-policy/adrs/ADR-024-content-safety-boundaries.md) is proposed, not accepted; current Chat behavior below remains deployed. The [plan sequence](../content-policy/plans/README.md) adds no screen and preserves meal formulas/photos. Public legal publication/acceptance or age-related UX requires separate product planning and any applicable Stitch gate, and is blocked until the [production gate](../content-policy/production-gate.md) allows it.

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
- Rotas HTTP, LLM, tunnel — [server](../server/README.md).

## Fronteiras e dependencias

- Constituicao vigente: [`AGENTS.md`](../../AGENTS.md).
- Visual: [`docs/tokens.md`](../tokens.md), Google Stitch (`docs/qa/stitch/{dark,light}/`), wires apenas como referência de criação de layout.
- Nao duplicar contrato HTTP nem schema. Link.

## Cobertura documental atual

Fonte hoje: AGENTS.md, tokens, Stitch gold PNGs (`docs/qa/stitch/`). Sem especificacao viva. Sem plano ativo. Sem ADR local.

## Como usar esta documentacao

Segue [docs/sdd/README.md](../sdd/README.md).

1. AGENTS.md
2. docs/tokens.md e o Design System Stitch (`.stitch/DESIGN.md`)
3. este README
4. [matriz](../README.md)

## Estado atual

App no ar (telas do ADR-012): splash, O1–O4, Home (painel), Chat ([chat](specifications/chat.md)), Config ([memoria-push](specifications/memoria-push.md)) e Push. O fluxo antigo T0–T3 foi removido no [A12](../android/plans/completed/a12-remover-legado-t123.md).

## Indice

### Especificacoes

Nenhuma especificacao criada ate o momento.

### ADRs

- [ADR-012](adrs/ADR-012-chat-home-perfil.md) — Chat tela, Home painel, perfil nomeado.
- [ADR-016](adrs/ADR-016-nome-dieta-bot.md) — nome "Dieta Bot".
- [ADR-017](adrs/ADR-017-registro-consolidado.md) — um registro por refeição (aceito).
- [ADR-019](adrs/ADR-019-ferramentas-dev.md) — telas de ferramenta só no dev (aceito com o A23).
- [ADR-020](adrs/ADR-020-estados-novos-chat-home-horario.md) — golds novos `chatA`, `homeW`, `o3t` (aceito).
- [ADR-021](adrs/ADR-021-refeicoes-por-dia.md) — refeições por dia da semana (aceito).
- [ADR-022](adrs/ADR-022-limite-texto-chat.md) — mensagem do Chat até 2000 caracteres, estado de erro (aceito).
- [ADR-023](adrs/ADR-023-chat-v2-memoria-v2.md) — Chat v2 (intenção, texto da refeição, plano) e Memória v2 (permanente + dinâmica) (aceito).
- [ADR-026](adrs/ADR-026-perguntas-antes-da-estimativa.md) — perguntas antes da estimativa, até 3 rodadas, Forçar estimativa (aceito com o A30; planos [S13](../server/plans/completed/s13-perguntas-antes-da-estimativa.md), [ST7](../stitch/plans/completed/st7-pergunta-antes-da-estimativa.md), [A30](../android/plans/completed/a30-perguntas-antes-da-estimativa.md)).

Historico em `docs/decisions/` (ver [matriz](../README.md)).

### Planos e validacao

Nenhum plano criado ate o momento.
