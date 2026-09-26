# produto

## Proposito

Comportamento visivel do Nutri: job, telas, copy, onboarding, slots, o que entra no prompt da IA.

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
- Visual: [`docs/tokens.md`](../tokens.md), [`wires/nutri-wires-expressive.html`](../../wires/nutri-wires-expressive.html), gold em `docs/qa/wire/`.
- Nao duplicar contrato HTTP nem schema. Link.

## Cobertura documental atual

Fonte hoje: AGENTS.md, tokens, wires, gold PNGs. Sem especificacao viva. Sem plano ativo. Sem ADR local.

## Como usar esta documentacao

Segue [docs/sdd/README.md](../sdd/README.md).

1. AGENTS.md
2. docs/tokens.md e os wires
3. este README
4. [matriz](../README.md)

## Estado atual

App no ar: splash, O1, O2, T0, T1 sheet, T2, T3. Chat-como-tela e Config ainda nao tem spec neste contexto.

## Indice

### Especificacoes

Nenhuma especificacao criada ate o momento.

### ADRs

Nenhum ADR local. Historico em `docs/decisions/` (ver [matriz](../README.md)).

### Planos e validacao

Nenhum plano criado ate o momento.
